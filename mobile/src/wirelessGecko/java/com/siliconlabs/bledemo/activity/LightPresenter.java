package com.siliconlabs.bledemo.activity;

import com.siliconlabs.bledemo.ble.GattService;

import java.util.Timer;
import java.util.TimerTask;

class LightPresenter {

    private static final int FAST_TOGGLING_READ_DELAY = 800;

    public interface View {

        void showLightState(boolean lightOn);

        void showTriggerSourceIcon(TriggerSource source);

        void showTriggerSourceAddress(String sourceAddress, TriggerSource source);

        void showDeviceDisconnectedDialog();

    }

    public interface BluetoothController {

        boolean setLightValue(boolean lightOn);

        void setPresenter(LightPresenter presenter);

        boolean getLightValue();

        void leaveDemo();

    }

    private boolean lightOn;
    private TriggerSource triggerSource = TriggerSource.UNKNOWN;
    private String sourceAddress;
    private LightPresenter.View view;
    private final BluetoothController bluetoothController;
    private Timer periodicReadTimer;
    private GattService gattService;

    LightPresenter(View view, BluetoothController bluetoothController) {
        this.view = view;
        this.bluetoothController = bluetoothController;
        //light_on will be set according to what we read from bluetooth, just hardcoding to start with off for now
        lightOn = false;
        sourceAddress = "";
        view.showLightState(lightOn);
        view.showTriggerSourceIcon(triggerSource);
        view.showTriggerSourceAddress(sourceAddress, triggerSource);
        bluetoothController.setPresenter(this);
        periodicReadTimer = new Timer();
    }

    private void updateLight() {
        bluetoothController.getLightValue();
    }

    void onLightClicked() {
        bluetoothController.setLightValue(!lightOn);
    }

    void onLightUpdated(boolean isLightOn) {
        lightOn = isLightOn;
        view.showLightState(lightOn);
    }

    void onSourceUpdated(TriggerSource value) {
        triggerSource = value;
        if (gattService == GattService.ProprietaryLightService && value.equals(TriggerSource.ZIGBEE)) {
            triggerSource = TriggerSource.PROPRIETARY;
        }
        view.showTriggerSourceIcon(triggerSource);
    }

    void onSourceAddressUpdated(String sourceAddress) {
        this.sourceAddress = sourceAddress;
        view.showTriggerSourceAddress(sourceAddress, triggerSource);
    }

    void cancelPeriodicReads() {
        periodicReadTimer.cancel();
    }

    void getLightValueDelayed()
    {
            periodicReadTimer.cancel();
            periodicReadTimer.purge();
            periodicReadTimer = new Timer();
            periodicReadTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    updateLight();
                }
            },FAST_TOGGLING_READ_DELAY);
    }

    void setGattService(GattService gattService) {
        this.gattService = gattService;
    }

    GattService getGattService() {
        return gattService;
    }

    void showDeviceDisconnectedDialog() {
        view.showDeviceDisconnectedDialog();
    }

    void leaveDemo() {
        bluetoothController.leaveDemo();
    }
}
