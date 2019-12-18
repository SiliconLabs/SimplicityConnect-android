package com.siliconlabs.bledemo.activity;

import android.bluetooth.*;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;
import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.ble.*;
import com.siliconlabs.bledemo.rangetest.RangeTestAdvertisementHandler;
import com.siliconlabs.bledemo.rangetest.RangeTestMode;
import com.siliconlabs.bledemo.rangetest.RangeTestValues;
import com.siliconlabs.bledemo.utils.BLEUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Math.abs;

/**
 * @author Comarch S.A.
 */

public class RangeTestActivity extends BaseActivity implements RangeTestPresenter.Controller, BluetoothStateReceiver.BluetoothStateListener {

    private static final int RECONNECTION_RETRIES = 2;
    private static final long RECONNECTION_DELAY_MS = 2000;

    private RangeTestMode mode;
    private RangeTestPresenter presenter = new RangeTestPresenter();

    private BlueToothService service;
    private BlueToothService.Binding binding;
    private BluetoothStateReceiver stateReceiver;

    private boolean timerStarted;

    private GattProcessor processor = new GattProcessor();

    private RangeTestAdvertisementHandler advertisementHandler;
    private boolean reconnecting;
    private boolean testWasRunning;
    private int reconnectionRetry;
    private Handler reconnectionHandler = new Handler(Looper.getMainLooper());
    private Runnable reconnectionRunnable = new Runnable() {

        @Override
        public void run() {
            final BlueToothService service = RangeTestActivity.this.service;
            final BluetoothGatt gatt;
            final BluetoothDevice device;

            if (service == null || (gatt = service.getConnectedGatt()) == null || (device = gatt.getDevice()) == null) {
                handleConnectionError();
                return;
            }

            Toast.makeText(RangeTestActivity.this, R.string.demo_range_toast_reconnecting, Toast.LENGTH_SHORT).show();

            if (!service.connectGatt(device, false, null)) {
                handleConnectionError();
            }
        }
    };

    private CountDownLatch discoveryReadyLatch = new CountDownLatch(1);
    private boolean testRunning;

    private int txSentPackets;
    private CountDownTimer txUpdateTimer = new CountDownTimer(Long.MAX_VALUE, 1000 / 13) {
        @Override
        public void onTick(long millisUntilFinished) {
            ++txSentPackets;
            presenter.onPacketSentUpdated(txSentPackets);
        }

        @Override
        public void onFinish() {
            // nothing
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_range_test);

        binding = new BlueToothService.Binding(this) {

            @Override
            protected void onBound(BlueToothService service) {
                initTest(service);
            }
        };

        txUpdateTimer.cancel();

        stateReceiver = new BluetoothStateReceiver(this);
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(stateReceiver, filter);

        BlueToothService.bind(binding);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (isFinishing()) {
            service.registerGattCallback(false, null);
            binding.unbind();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (stateReceiver != null) {
            unregisterReceiver(stateReceiver);
        }

        if (service != null) {
            service.clearGatt();
        }

        if (advertisementHandler != null) {
            advertisementHandler.stopListening();
            advertisementHandler = null;
        }
    }

    @Override
    public void setView(RangeTestPresenter.RangeTestView view) {
        presenter.setView(view);

        if (view == null) {
            handleTxTimer(false);
        }
    }

    @Override
    public void initTestMode(final RangeTestMode mode) {
        initTestView(mode, true);
    }

    @Override
    public void cancelTestMode() {
        finish();
    }

    @Override
    public void updateTxPower(final int power) {
        withGatt(new SetValueGattAction(GattCharacteristic.RangeTestTxPower, power));
    }

    @Override
    public void updatePayloadLength(final int length) {
        withGatt(new SetValueGattAction(GattCharacteristic.RangeTestPayload, length));
    }

    @Override
    public void updateMaWindowSize(final int size) {
        withGatt(new SetValueGattAction(GattCharacteristic.RangeTestMaSize, size));
    }

    @Override
    public void updateChannel(final int channel) {
        withGatt(new SetValueGattAction(GattCharacteristic.RangeTestChannel, channel));
    }

    @Override
    public void updatePacketCount(final int count) {
        withGatt(new SetValueGattAction(GattCharacteristic.RangeTestPacketsRequired, count));
    }

    @Override
    public void updateRemoteId(final int id) {
        withGatt(new SetValueGattAction(GattCharacteristic.RangeTestDestinationId, id));
    }

    @Override
    public void updateSelfId(final int id) {
        withGatt(new SetValueGattAction(GattCharacteristic.RangeTestSourceId, id));
    }

    @Override
    public void updateUartLogEnabled(final boolean enabled) {
        withGatt(new SetValueGattAction(GattCharacteristic.RangeTestLog, enabled));
    }

    @Override
    public void toggleRunningState() {
        withGatt(new SetValueGattAction(GattCharacteristic.RangeTestIsRunning, !testRunning));
    }

    @Override
    public void updatePhyConfig(final int id) {
        withGatt(new SetValueGattAction(GattCharacteristic.RangePhyConfig, id));
    }

    private void initTestView(final RangeTestMode mode, boolean writeCharacteristic) {
        this.mode = mode;

        if (writeCharacteristic) {
            withGatt(new SetValueGattAction(GattCharacteristic.RangeTestRadioMode, mode.getCode()));
        }

        showRangeTestFragment(mode);
    }

    private void initTest(final BlueToothService service) {
        final BluetoothGatt gatt = service.getConnectedGatt();
        final BluetoothDevice device;

        if (gatt == null || !service.isGattConnected() || (device = gatt.getDevice()) == null) {
            if (!isFinishing()) {
                finish();
            }

            return;
        }

        this.service = service;

        advertisementHandler = new RangeTestAdvertisementHandler(this, device.getAddress()) {

            @Override
            protected void handleAdvertisementRecord(int manufacturerData, int companyId, int structureType, int rssi, int packetCount, int packetReceived) {
                if (mode == RangeTestMode.Rx && structureType == 0) {
                    cancelReconnect();
                    presenter.onTestDataReceived(rssi, packetCount, packetReceived);
                } else if (!reconnecting) {
                    scheduleReconnect();
                }
            }
        };

        service.registerGattCallback(false, processor);
        service.refreshGattServices();
    }

    private void scheduleReconnect() {
        reconnecting = true;

        reconnectionHandler.removeCallbacks(reconnectionRunnable);
        reconnectionHandler.postDelayed(reconnectionRunnable, RECONNECTION_DELAY_MS);
    }

    private void cancelReconnect() {
        reconnecting = false;
        reconnectionHandler.removeCallbacks(reconnectionRunnable);
    }

    private void withGatt(GattAction action) {
        BluetoothGatt gatt;

        if (service == null || !service.isGattConnected() || (gatt = service.getConnectedGatt()) == null) {
            handleConnectionError();
            return;
        }

        if (discoveryReadyLatch.getCount() > 0) {
            try {
                discoveryReadyLatch.await(8, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                handleConnectionError();
                return;
            }
        }

        action.run(gatt);
    }

    private void handleConnectionError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(RangeTestActivity.this, R.string.demo_range_toast_error, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void showModeSelectionDialog() {
        RangeTestModeDialog modeDialog = new RangeTestModeDialog();
        modeDialog.show(getSupportFragmentManager(), "RANGE_TEST_MODE");
    }

    private void showRangeTestFragment(RangeTestMode mode) {
        Bundle arguments = new Bundle();
        arguments.putInt(RangeTestFragment.ARG_MODE, mode.getCode());

        RangeTestFragment fragment = new RangeTestFragment();
        fragment.setArguments(arguments);

        getFragmentManager().beginTransaction()
                .replace(R.id.fragment, fragment)
                .commit();
    }

    private BluetoothGattCharacteristic getGenericAccessCharacteristic(GattCharacteristic characteristic) {
        BluetoothGattService gattService = service.getConnectedGatt().getService(GattService.GenericAccess.number);
        return gattService == null ? null : gattService.getCharacteristic(characteristic.uuid);
    }

    private BluetoothGattCharacteristic getDeviceInformationCharacteristic(GattCharacteristic characteristic) {
        BluetoothGattService gattService = service.getConnectedGatt().getService(GattService.DeviceInformation.number);
        return gattService == null ? null : gattService.getCharacteristic(characteristic.uuid);
    }

    private BluetoothGattCharacteristic getRangeTestCharacteristic(GattCharacteristic characteristic) {
        BluetoothGattService gattService = service.getConnectedGatt().getService(GattService.RangeTestService.number);
        return gattService == null ? null : gattService.getCharacteristic(characteristic.uuid);
    }

    private void handleTxTimer(boolean running) {
        if (running) {
            txSentPackets = 0;
            timerStarted = true;
            txUpdateTimer.cancel();
            txUpdateTimer.start();
        } else {
            timerStarted = false;
            txUpdateTimer.cancel();
        }
    }

    @Override
    public void onBluetoothStateChanged(boolean enabled) {
        if (!enabled) {
            finish();
        }
    }

    private static class GattCommand {

        enum Type {
            Read, Write, Subscribe, ReadDescriptor
        }

        private final Type type;
        private final BluetoothGatt gatt;
        private final BluetoothGattCharacteristic characteristic;
        private BluetoothGattDescriptor descriptor;

        private GattCommand(Type type, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            this.type = type;
            this.gatt = gatt;
            this.characteristic = characteristic;
        }

        private GattCommand(Type type, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor) {
            this.type = type;
            this.gatt = gatt;
            this.characteristic = characteristic;
            this.descriptor = descriptor;
        }
    }

    private class GattProcessor extends TimeoutGattCallback {

        private Queue<GattCommand> commands = new LinkedList<>();
        private Lock lock = new ReentrantLock();
        private boolean processing;

        private void queueRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            queue(new GattCommand(GattCommand.Type.Read, gatt, characteristic));
        }

        private void queueReadDescriptor(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor) {
            queue(new GattCommand(GattCommand.Type.ReadDescriptor, gatt, characteristic, descriptor));
        }

        private void queueWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            queue(new GattCommand(GattCommand.Type.Write, gatt, characteristic));
        }

        private void queueSubscribe(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            queue(new GattCommand(GattCommand.Type.Subscribe, gatt, characteristic));
        }

        private void queue(GattCommand command) {
            lock.lock();
            try {
                commands.add(command);
                if (!processing) {
                    processNextCommand();
                }
            } finally {
                lock.unlock();
            }
        }

        private void clearCommandQueue() {
            lock.lock();
            try {
                commands.clear();
            } finally {
                lock.unlock();
            }
        }

        private void processNextCommand() {
            boolean success = false;

            final GattCommand command = commands.poll();

            if (command != null && command.gatt != null && command.characteristic != null) {
                final BluetoothGatt gatt = command.gatt;
                final BluetoothGattCharacteristic characteristic = command.characteristic;

                switch (command.type) {
                    case Read:
                        success = gatt.readCharacteristic(characteristic);
                        break;
                    case Write:
                        success = gatt.writeCharacteristic(characteristic);
                        break;
                    case Subscribe:
                        GattCharacteristic gattCharacteristic = GattCharacteristic.fromUuid(characteristic.getUuid());
                        GattService gattService = GattService.fromUuid(characteristic.getService().getUuid());
                        success = BLEUtils.SetNotificationForCharacteristic(gatt, gattService, gattCharacteristic, BLEUtils.Notifications.INDICATE);
                        break;
                    case ReadDescriptor:
                        success = gatt.readDescriptor(command.descriptor);
                        break;
                    default:
                        break;
                }
            }

            processing = success;
        }

        private void handleCommandProcessed() {
            lock.lock();
            try {
                if (commands.isEmpty()) {
                    processing = false;
                } else {
                    processNextCommand();
                }
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            testWasRunning = false;

            if (reconnecting && newState != BluetoothProfile.STATE_CONNECTING && newState != BluetoothProfile.STATE_CONNECTED) {
                if (reconnectionRetry >= RECONNECTION_RETRIES) {
                    handleConnectionError();
                } else {
                    ++reconnectionRetry;
                    scheduleReconnect();
                }
            }

            if (mode == RangeTestMode.Rx && newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (isFinishing()) {
                    return;
                }

                synchronized (RangeTestActivity.this) {
                    if (advertisementHandler != null) {
                        advertisementHandler.startListening();
                    } else {
                        handleConnectionError();
                        return;
                    }
                }

                testRunning = true;
                setKeepScrennOn(true);
                presenter.onRunningStateUpdated(true);
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                reconnecting = false;
                reconnectionRetry = 0;
                cancelReconnect();

                if (mode == RangeTestMode.Rx) {
                    synchronized (RangeTestActivity.this) {
                        if (advertisementHandler != null) {
                            advertisementHandler.stopListening();
                        }
                    }

                    testRunning = false;
                    setKeepScrennOn(false);
                    presenter.onRunningStateUpdated(false);
                }

                clearCommandQueue();
                discoveryReadyLatch = new CountDownLatch(1);
                service.refreshGattServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                handleConnectionError();
                return;
            }

            discoveryReadyLatch.countDown();

            queueRead(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestIsRunning));
            queueRead(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestRadioMode));

            queueRead(gatt, getGenericAccessCharacteristic(GattCharacteristic.DeviceName));
            queueRead(gatt, getDeviceInformationCharacteristic(GattCharacteristic.ModelNumberString));

            queueRead(gatt, getRangeTestCharacteristic(GattCharacteristic.RangePhyList));
            queueRead(gatt, getRangeTestCharacteristic(GattCharacteristic.RangePhyConfig));
            queueRead(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestTxPower));
            queueRead(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPacketsSend));
            queueRead(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestDestinationId));
            queueRead(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestSourceId));
            queueRead(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPacketsReceived));
            queueRead(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPacketsCount));
            queueRead(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPacketsRequired));
            queueRead(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPER));
            queueRead(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestMA));
            queueRead(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestChannel));
            queueRead(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPayload));
            queueRead(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestMaSize));
            queueRead(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestLog));

            queueSubscribe(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestIsRunning));
            queueSubscribe(gatt, getRangeTestCharacteristic(GattCharacteristic.RangePhyConfig));
            queueSubscribe(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestTxPower));
            queueSubscribe(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPacketsSend));
            queueSubscribe(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestDestinationId));
            queueSubscribe(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestSourceId));
            queueSubscribe(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPacketsReceived));
            queueSubscribe(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPacketsCount));
            queueSubscribe(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPacketsRequired));
            queueSubscribe(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPER));
            queueSubscribe(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestMA));
            queueSubscribe(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestChannel));
            queueSubscribe(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestRadioMode));
            queueSubscribe(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPayload));
            queueSubscribe(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestMaSize));
            queueSubscribe(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestLog));
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                handleConnectionError();
                return;
            }

            handleCommandProcessed();

            final GattCharacteristic gattCharacteristic = GattCharacteristic.fromUuid(characteristic.getUuid());

            if (gattCharacteristic != null) {
                updatePresenter(characteristic, gattCharacteristic);

                if (gattCharacteristic == GattCharacteristic.RangeTestTxPower
                        || gattCharacteristic == GattCharacteristic.RangeTestPayload
                        || gattCharacteristic == GattCharacteristic.RangeTestMaSize) {
                    List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
                    if (descriptors.size() > 1) {
                        queueReadDescriptor(gatt, characteristic, descriptors.get(descriptors.size() - 1));
                    }
                }
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                handleConnectionError();
                return;
            }

            handleCommandProcessed();

            final GattCharacteristic gattCharacteristic = GattCharacteristic.fromUuid(descriptor.getCharacteristic().getUuid());

            if (gattCharacteristic != null) {
                updatePresenter(descriptor, gattCharacteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                handleConnectionError();
                return;
            }

            handleCommandProcessed();

            GattCharacteristic gattCharacteristic = GattCharacteristic.fromUuid(characteristic.getUuid());

            if (mode == RangeTestMode.Tx && gattCharacteristic != null) {
                switch (gattCharacteristic) {
                    case RangeTestIsRunning:
                        testRunning = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) != 0;
                        setKeepScrennOn(testRunning);
                        handleTxTimer(testRunning);
                        presenter.onRunningStateUpdated(testRunning);
                        break;
                }
            }

            if (gattCharacteristic == GattCharacteristic.RangePhyConfig) {
                queueRead(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPayload));
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                handleConnectionError();
                return;
            }

            handleCommandProcessed();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            GattCharacteristic gattCharacteristic = GattCharacteristic.fromUuid(characteristic.getUuid());

            if (gattCharacteristic != null) {
                updatePresenter(characteristic, gattCharacteristic);
            }
        }

        private void updatePresenter(BluetoothGattCharacteristic characteristic, GattCharacteristic gattCharacteristic) {
            if (gattCharacteristic.format != 0) {
                switch (gattCharacteristic.format) {
                    case BluetoothGattCharacteristic.FORMAT_UINT8:
                    case BluetoothGattCharacteristic.FORMAT_UINT16:
                    case BluetoothGattCharacteristic.FORMAT_UINT32:
                    case BluetoothGattCharacteristic.FORMAT_SINT8:
                    case BluetoothGattCharacteristic.FORMAT_SINT16:
                    case BluetoothGattCharacteristic.FORMAT_SINT32:
                        Log.d("RangeTest", "Update: " + gattCharacteristic.name() + " -> value: " + characteristic.getIntValue(gattCharacteristic.format, 0));
                    default:
                        break;
                }
            }

            switch (gattCharacteristic) {
                case DeviceName:
                    String deviceName = characteristic.getStringValue(0);
                    presenter.onDeviceNameUpdated(deviceName);
                    break;
                case ModelNumberString:
                    String modelNumber = characteristic.getStringValue(0);

                    String patternDuplicateOpn = "opn\\[.*]";
                    modelNumber = modelNumber.replaceFirst(patternDuplicateOpn, "");

                    presenter.onModelNumberUpdated(modelNumber);
                    break;
                case RangeTestDestinationId:
                    int remoteId = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    presenter.onRemoteIdUpdated(remoteId);
                    break;
                case RangeTestSourceId:
                    int selfId = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    presenter.onSelfIdUpdated(selfId);
                    break;
                case RangeTestPacketsReceived:
                    final int uInt16Max = 0xFFFF;
                    int packetsReceived = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                    presenter.onPacketReceivedUpdated(packetsReceived == uInt16Max ? 0 : packetsReceived);
                    break;
                case RangeTestPacketsSend:
                    int packetsSent = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                    txSentPackets = packetsSent;
                    if (testRunning) {
                        if (!timerStarted && packetsSent > 0) {
                            handleTxTimer(true);
                        }
                        presenter.onPacketSentUpdated(packetsSent);
                    } else if (testWasRunning) {
                        testWasRunning = false;
                        presenter.onPacketSentUpdated(packetsSent);
                    }
                    break;
                case RangeTestPacketsCount:
                    int packetsCount = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                    presenter.onPacketCountUpdated(packetsCount);
                    break;
                case RangeTestPacketsRequired:
                    int packetsRequired = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                    boolean repeat = false;
                    if (packetsRequired == RangeTestValues.PACKET_COUNT_REPEAT) {
                        repeat = true;
                    } else {
                        presenter.onPacketRequiredUpdated(packetsRequired);
                    }
                    presenter.onPacketCountRepeatUpdated(repeat);
                    break;
                case RangeTestPER:
                    int per = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                    presenter.onPerUpdated(per / 10.f);
                    break;
                case RangeTestMA:
                    int ma = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                    presenter.onMaUpdated(ma / 10.f);
                    break;
                case RangeTestChannel:
                    int channel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                    presenter.onChannelNumberUpdated(channel);
                    break;
                case RangeTestRadioMode:
                    int radioMode = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    RangeTestMode rangeTestMode = RangeTestMode.fromCode(radioMode);
                    if (mode != null && mode != rangeTestMode) {
                        initTestView(rangeTestMode, false);
                    } else if (mode == null && rangeTestMode != null && testRunning) {
                        initTestView(rangeTestMode, false);
                        presenter.onRunningStateUpdated(testRunning);
                    }
                    break;
                case RangeTestTxPower:
                    int txPower = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, 0);
                    presenter.onTxPowerUpdated(txPower);
                    break;
                case RangeTestPayload:
                    int payloadLength = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    presenter.onPayloadLengthUpdated(payloadLength);
                    break;
                case RangeTestMaSize:
                    int maWindowSize = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    presenter.onMaWindowSizeUpdated(maWindowSize);
                    break;
                case RangeTestLog:
                    int log = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    presenter.onUartLogEnabledUpdated(log != 0);
                    break;
                case RangeTestIsRunning:
                    testRunning = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) != 0;
                    if (mode == null && !testRunning) {
                        showModeSelectionDialog();
                    } else if (mode == RangeTestMode.Tx) {
                        handleTxTimer(testRunning);
                        if (!testRunning && service != null) {
                            BluetoothGatt gatt = service.getConnectedGatt();
                            if (gatt != null) {
                                testWasRunning = true;
                                queueRead(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPacketsSend));
                            }
                        }
                        presenter.onRunningStateUpdated(testRunning);
                    }
                    break;
                case RangePhyConfig:
                    int phyConfig = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    presenter.onPhyConfigUpdated(phyConfig);

                    BluetoothGatt gatt = service.getConnectedGatt();
                    if (gatt != null) {
                        queueRead(gatt, getRangeTestCharacteristic(GattCharacteristic.RangeTestPayload));
                    }

                    break;
                case RangePhyList:
                    LinkedHashMap<Integer, String> phyConfigs = new LinkedHashMap<>();

                    String phyConfigsList = characteristic.getStringValue(0);
                    String[] phyConfigsListSplit = phyConfigsList.split(",");

                    for (String phyConfigPair : phyConfigsListSplit) {
                        String[] phyConfigPairSplit = phyConfigPair.split(":");
                        if (phyConfigPairSplit.length == 2) {
                            int id = Integer.valueOf(phyConfigPairSplit[0]);
                            String name = phyConfigPairSplit[1];
                            phyConfigs.put(id, name);
                        }
                    }

                    presenter.onPhyMapUpdated(phyConfigs);
                    break;
            }
        }

        private void updatePresenter(BluetoothGattDescriptor descriptor, GattCharacteristic gattCharacteristic) {
            if (gattCharacteristic.format != 0) {
                switch (gattCharacteristic.format) {
                    case BluetoothGattCharacteristic.FORMAT_UINT8:
                    case BluetoothGattCharacteristic.FORMAT_UINT16:
                    case BluetoothGattCharacteristic.FORMAT_UINT32:
                    case BluetoothGattCharacteristic.FORMAT_SINT8:
                    case BluetoothGattCharacteristic.FORMAT_SINT16:
                    case BluetoothGattCharacteristic.FORMAT_SINT32:
                        Log.d("RangeTest", "Update: " + gattCharacteristic.name() + " -> descriptor: " + descriptor.toString());
                    default:
                        break;
                }
            }

            byte[] descriptorValues = descriptor.getValue();

            if (descriptorValues.length % 2 != 0) {
                return;
            }

            byte[] firstValueArray = Arrays.copyOfRange(descriptorValues, 0, descriptorValues.length / 2);
            byte[] secondValueArray = Arrays.copyOfRange(descriptorValues, descriptorValues.length / 2, descriptorValues.length);
            ByteBuffer wrappedFirstValue = ByteBuffer.wrap(firstValueArray).order(ByteOrder.LITTLE_ENDIAN);
            ByteBuffer wrappedSecondValue = ByteBuffer.wrap(secondValueArray).order(ByteOrder.LITTLE_ENDIAN);
            int rangeFrom;
            int rangeTo;

            switch (gattCharacteristic) {
                case RangeTestTxPower:
                    rangeFrom = wrappedFirstValue.getShort();
                    rangeTo = wrappedSecondValue.getShort();
                    presenter.onTxPowerRangeUpdated(rangeFrom, rangeTo);
                    break;
                case RangeTestPayload:
                    rangeFrom = firstValueArray[0];
                    rangeTo = secondValueArray[0];
                    presenter.onPayloadLengthRangeUpdated(rangeFrom, rangeTo);
                    break;
                case RangeTestMaSize:
                    rangeFrom = abs(firstValueArray[0]);
                    rangeTo = abs(secondValueArray[0]);
                    presenter.onMaWindowSizeRangeUpdated(rangeFrom, rangeTo);
                    break;
            }
        }
    }

    private void setKeepScrennOn(final boolean enabled) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (enabled) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
        });
    }

    private interface GattAction {

        void run(BluetoothGatt gatt);
    }

    private class SetValueGattAction implements GattAction {

        private final GattCharacteristic characteristic;
        private final int value;

        SetValueGattAction(GattCharacteristic characteristic, boolean value) {
            this.characteristic = characteristic;
            this.value = value ? 1 : 0;
        }

        SetValueGattAction(GattCharacteristic characteristic, int value) {
            this.characteristic = characteristic;
            this.value = value;
        }

        @Override
        public void run(final BluetoothGatt gatt) {
            final BluetoothGattCharacteristic characteristic = getRangeTestCharacteristic(this.characteristic);

            if (characteristic == null) {
                return;
            }

            final Integer currentValue = getCurrentValueOf(characteristic);

            if (currentValue == null || currentValue == this.value) {
                return;
            }

            writeValueFor(gatt, characteristic);
        }

        private Integer getCurrentValueOf(final BluetoothGattCharacteristic characteristic) {
            final byte[] value = characteristic.getValue();

            if (value == null || value.length == 0) {
                return null;
            }

            return characteristic.getIntValue(this.characteristic.format, 0);
        }

        private void writeValueFor(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            characteristic.setValue(this.value, this.characteristic.format, 0);
            processor.queueWrite(gatt, characteristic);
        }
    }
}
