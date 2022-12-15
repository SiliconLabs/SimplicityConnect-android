package com.siliconlabs.bledemo.home_screen.dialogs

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.ScanFilter
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.home_screen.adapters.ScannedDevicesAdapter
import com.siliconlabs.bledemo.base.activities.BaseActivity
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.home_screen.viewmodels.SelectDeviceViewModel
import com.siliconlabs.bledemo.bluetooth.ble.*
import com.siliconlabs.bledemo.bluetooth.ble.GattService
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService.GattConnectType
import com.siliconlabs.bledemo.features.demo.connected_lighting.activities.ConnectedLightingActivity
import com.siliconlabs.bledemo.features.demo.health_thermometer.activities.HealthThermometerActivity
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.features.demo.blinky.activities.BlinkyActivity
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.blinky_thunderboard.activities.BlinkyThunderboardActivity
import com.siliconlabs.bledemo.databinding.DialogSelectDeviceBinding
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.environment.activities.EnvironmentActivity
import com.siliconlabs.bledemo.features.iop_test.activities.IOPTestActivity
import com.siliconlabs.bledemo.features.iop_test.models.IOPTest
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.motion.activities.MotionActivity
import com.siliconlabs.bledemo.home_screen.activities.MainActivity
import com.siliconlabs.bledemo.features.demo.throughput.activities.ThroughputActivity
import com.siliconlabs.bledemo.features.demo.throughput.utils.PeripheralManager
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.base.models.ThunderBoardDevice
import com.siliconlabs.bledemo.features.demo.wifi_commissioning.activities.WifiCommissioningActivity
import timber.log.Timber
import java.util.*

class SelectDeviceDialog(
        private var bluetoothService: BluetoothService?
) : BaseDialogFragment(
        hasCustomWidth = true,
        isCanceledOnTouchOutside = true
),
    ScannedDevicesAdapter.DemoDeviceCallback,
    BluetoothService.ScanListener {

    private lateinit var viewBinding: DialogSelectDeviceBinding
    private lateinit var viewModel: SelectDeviceViewModel

    private lateinit var adapter: ScannedDevicesAdapter

    private var currentDeviceInfo: BluetoothDeviceInfo? = null
    private var connectType: GattConnectType? = null

    private var cachedBoardType: String? = null

    private var rangeTestCallback: RangeTestCallback? = null

    private val handler = Handler(Looper.getMainLooper())

    private val timeoutGattCallback = object : TimeoutGattCallback() {

        override fun onTimeout() {
            handleDisconnection(R.string.toast_connection_timed_out)
        }

        override fun onMaxRetriesExceeded(gatt: BluetoothGatt) {
            handleDisconnection(R.string.connection_failed)
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    if (status == BluetoothGatt.GATT_SUCCESS) handleSuccessfulConnection(gatt)
                }
                BluetoothGatt.STATE_DISCONNECTED -> {
                    when (status) {
                        133 -> showReconnectionMessage()
                        else -> handleDisconnection(R.string.connection_failed)
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            gatt?.readCharacteristic(getModelNumberCharacteristic(gatt))
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?,
                                          characteristic: BluetoothGattCharacteristic?,
                                          status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (characteristic?.uuid == GattCharacteristic.ModelNumberString.uuid) {
                when (connectType) {
                    GattConnectType.MOTION -> launchDemo(characteristic.getStringValue(0))
                    GattConnectType.BLINKY -> {
                        when (characteristic.getStringValue(0)) {
                            ThunderBoardDevice.THUNDERBOARD_MODEL_SENSE,
                            ThunderBoardDevice.THUNDERBOARD_MODEL_DEV_KIT_V1,
                            ThunderBoardDevice.THUNDERBOARD_MODEL_DEV_KIT_V2 -> {
                                connectType = GattConnectType.BLINKY_THUNDERBOARD
                                cachedBoardType = characteristic.getStringValue(0)
                                gatt?.readCharacteristic(getPowerSourceCharacteristic(gatt))
                            }
                            ThunderBoardDevice.THUNDERBOARD_MODEL_BLUE_V1,
                            ThunderBoardDevice.THUNDERBOARD_MODEL_BLUE_V2 -> {
                                launchDemo(characteristic.getStringValue(0))
                            }
                            else -> { Timber.d("Unknown model")}
                        }
                    }
                    else -> { }
                }
            } else if (characteristic?.uuid == GattCharacteristic.PowerSource.uuid) {
                launchDemo(cachedBoardType, characteristic.getIntValue(GattCharacteristic.PowerSource.format, 0))
            }
        }
    }

    private fun getModelNumberCharacteristic(gatt: BluetoothGatt?): BluetoothGattCharacteristic? {
        return gatt?.getService(GattService.DeviceInformation.number)?.getCharacteristic(GattCharacteristic.ModelNumberString.uuid)
    }

    private fun getPowerSourceCharacteristic(gatt: BluetoothGatt?): BluetoothGattCharacteristic? {
        return gatt?.getService(GattService.PowerSource.number)?.getCharacteristic(GattCharacteristic.PowerSource.uuid)
    }

    private fun handleSuccessfulConnection(gatt: BluetoothGatt) {
        when (connectType) {
            GattConnectType.MOTION -> {
                (activity as BaseActivity).setModalDialogMessage(getString(R.string.reading_board_type))
                gatt.discoverServices()
            }
            GattConnectType.BLINKY -> {
                if (gatt.device.name == "Blinky Example") launchDemo()
                else {
                    (activity as BaseActivity).setModalDialogMessage(getString(R.string.reading_board_type))
                    gatt.discoverServices()
                }
            }
            else -> launchDemo()
        }
        bluetoothService?.isNotificationEnabled = true
    }

    private fun launchDemo(boardType: String? = null, powerSource: Int? = null) {
        getIntent(connectType)?.let { intent ->
            boardType?.let { intent.putExtra(MODEL_TYPE_EXTRA, it) }
            powerSource?.let { intent.putExtra(POWER_SOURCE_EXTRA, it) }
            startActivity(intent)
        }
        (activity as BaseActivity).dismissModalDialog()
        dismiss()
    }

    private fun showReconnectionMessage() {
        (activity as BaseActivity).apply {
            showMessage(R.string.connection_failed_reconnecting)
        }
    }

    private fun handleDisconnection(@StringRes message: Int) {
        viewModel.clearDevices()
        (activity as BaseActivity).apply {
            dismissModalDialog()
            dismiss()
            showMessage(message)
        }
        bluetoothService?.isNotificationEnabled = true
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)

        viewModel = ViewModelProvider(this).get(SelectDeviceViewModel::class.java)
        adapter = ScannedDevicesAdapter(mutableListOf(),
                this
        ).also { it.setHasStableIds(true) }

        if (bluetoothService == null) {
            bluetoothService = (activity as MainActivity).bluetoothService
        }
    }

    override fun onDemoDeviceClicked(deviceInfo: BluetoothDeviceInfo) {
        when (connectType) {
            GattConnectType.RANGE_TEST -> {
                dismiss()
                rangeTestCallback?.getBluetoothDeviceInfo(deviceInfo)
            }
            GattConnectType.IOP_TEST -> {
                dismiss()
                IOPTest.createDataTest(deviceInfo.name)
                getIntent(connectType)?.let {
                    activity?.startActivity(it)
                }
            }
            else -> connect(deviceInfo)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        (parentFragment as? DialogInterface.OnDismissListener)?.onDismiss(dialog)
    }

    override fun onDetach() {
        stopDiscovery()
        super.onDetach()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let { args ->
            connectType = GattConnectType.values()[args.getInt(CONN_TYPE_INFO, 0)]
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewBinding.buttonCancel.setOnClickListener {
            dialog?.dismiss()
            rangeTestCallback?.onCancel()
        }
        initializeRecyclerView()
        observeChanges()
        initDemoDescription()
    }

    override fun onCancel(dialog: DialogInterface) {
        rangeTestCallback?.onCancel()
        super.onCancel(dialog)
    }

    private fun initializeRecyclerView() {
        viewBinding.list.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = this@SelectDeviceDialog.adapter
        }
    }

    private fun observeChanges() {
        viewModel.apply {
            isScanningOn.observe(viewLifecycleOwner, Observer {
                toggleScanning(it)
                toggleRefreshInfoRunnable(it)
            })
            isAnyDeviceDiscovered.observe(viewLifecycleOwner, Observer {
                toggleListView(it)
            })
            numberOfDevices.observe(viewLifecycleOwner, Observer {
                viewBinding.devicesNumber.text = getString(R.string.DEVICE_LIST, it)
            })
            deviceToInsert.observe(viewLifecycleOwner, Observer {
                adapter.addNewDevice(it)
            })
        }
    }

    private fun initDemoDescription() {
        viewBinding.dialogTextInfo.apply {
            text = when (connectType) {
                GattConnectType.THERMOMETER -> getString(R.string.soc_must_be_connected,
                        getString(R.string.demo_firmware_name_health_thermometer))
                GattConnectType.LIGHT -> getString(R.string.soc_must_be_connected,
                        getString(R.string.demo_firmware_name_connected_lighting))
                GattConnectType.RANGE_TEST -> getString(R.string.soc_must_be_connected,
                        getString(R.string.demo_firmware_name_range_test))
                GattConnectType.BLINKY -> getString(R.string.soc_blinky_must_be_connected)
                GattConnectType.THROUGHPUT_TEST -> getString(R.string.soc_must_be_connected,
                        getString(R.string.demo_firmware_name_throughput))
                GattConnectType.MOTION -> getString(R.string.soc_thunderboard_must_be_connected)
                GattConnectType.ENVIRONMENT -> getString(R.string.soc_thunderboard_must_be_connected)
                GattConnectType.IOP_TEST -> getString(R.string.soc_must_be_connected,
                        getString(R.string.demo_firmware_name_iop))
                GattConnectType.WIFI_COMMISSIONING -> {
                    Html.fromHtml(getString(R.string.soc_wifi_commissioning_must_be_connected))
                }
                else -> getString(R.string.empty_description)
            }
            if (connectType == GattConnectType.WIFI_COMMISSIONING) {
                movementMethod = LinkMovementMethod.getInstance() // react to clicking the link
            }
        }
    }

    private fun toggleScanning(isOn: Boolean) {
        if (isOn) {
            startDiscovery()
        } else {
            stopDiscovery()
        }
    }


    private fun startDiscovery() {
        bluetoothService?.let {
            it.removeListener(this)
            it.addListener(this)
            it.startDiscovery(applyDemoFilters())
        }
    }

    private fun stopDiscovery() {
        bluetoothService?.let {
            it.removeListener(this)
            it.stopDiscovery()
        }
    }

    private fun toggleListView(isAnyDeviceDiscovered: Boolean) {
        viewBinding.apply {
            if (isAnyDeviceDiscovered) {
                list.visibility = View.VISIBLE
                noDevicesFound.visibility = View.GONE
            } else {
                list.visibility = View.GONE
                noDevicesFound.visibility = View.VISIBLE
            }
        }
    }

    private fun toggleRefreshInfoRunnable(isOn: Boolean) {
        handler.let {
            if (isOn) it.postDelayed(updateScanInfoRunnable, SCAN_UPDATE_PERIOD)
            else it.removeCallbacks(updateScanInfoRunnable)
        }
    }

    private val updateScanInfoRunnable = object : Runnable {
        override fun run() {
            adapter.updateList(viewModel.getScannedDevicesList().toMutableList())
            handler.postDelayed(this, SCAN_UPDATE_PERIOD)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = DialogSelectDeviceBinding.inflate(inflater)
        return viewBinding.root
    }

    override fun onResume() {
        super.onResume()
        if (dialog?.window != null) {
            viewModel.setIsScanningOn(true)
        }
    }

    private fun applyDemoFilters() : List<ScanFilter> {
        return mutableListOf<ScanFilter>().apply {
            when (connectType) {
                GattConnectType.THERMOMETER -> {
                    add(buildFilter(GattService.HealthThermometer))
                }
                GattConnectType.LIGHT -> {
                    add(buildFilter(GattService.ProprietaryLightService))
                    add(buildFilter(GattService.ZigbeeLightService))
                    add(buildFilter(GattService.ConnectLightService))
                    add(buildFilter(GattService.ThreadLightService))
                }
                GattConnectType.RANGE_TEST -> {
                    add(buildFilter(GattService.RangeTestService))
                }
                GattConnectType.BLINKY -> {
                    add(buildFilter("Blinky Example"))
                    add(buildFilter(ManufacturerDataFilter(
                            id = 71,
                            data = byteArrayOf(2, 0)
                    )))
                }
                GattConnectType.THROUGHPUT_TEST -> {
                    add(buildFilter("Throughput Test"))
                }
                GattConnectType.WIFI_COMMISSIONING -> {
                    add(buildFilter("BLE_CONFIGURATOR"))
                }
                GattConnectType.IOP_TEST -> {
                    add(buildFilter("IOP Test"))
                    add(buildFilter("IOP Test Update"))
                    add(buildFilter("IOP_Test_1"))
                    add(buildFilter("IOP_Test_2"))
                }
                GattConnectType.MOTION -> {
                    add(buildFilter(ManufacturerDataFilter(
                            id = 71,
                            data = byteArrayOf(2, 0)
                    )))
                }
                GattConnectType.ENVIRONMENT -> {
                    add(buildFilter(ManufacturerDataFilter(
                            id = 71,
                            data = byteArrayOf(2, 0)
                    )))
                }
                else -> { }
            }
        }
    }

    private fun buildFilter(name: String) : ScanFilter {
        return ScanFilter.Builder().apply {
            setDeviceName(name)
        }.build()
    }

    private fun buildFilter(service: GattService) : ScanFilter {
        return ScanFilter.Builder().apply {
            setServiceUuid(ParcelUuid(service.number), ParcelUuid(GattService.UUID_MASK))
        }.build()
    }

    private fun buildFilter(manufacturerData: ManufacturerDataFilter) : ScanFilter {
        return ScanFilter.Builder().apply {
            setManufacturerData(manufacturerData.id, manufacturerData.data, manufacturerData.mask)
        }.build()
    }

    override fun onPause() {
        super.onPause()
        viewModel.setIsScanningOn(false)
    }

    private fun connect(deviceInfo: BluetoothDeviceInfo) {
        currentDeviceInfo = deviceInfo

        if (connectType == GattConnectType.THROUGHPUT_TEST) {
            PeripheralManager.advertiseThroughputServer(bluetoothService)
        }

        bluetoothService?.let { service ->
            (activity as BaseActivity).showModalDialog(BaseActivity.ConnectionStatus.CONNECTING, DialogInterface.OnCancelListener {
                service.clearConnectedGatt()
            })
            service.isNotificationEnabled = false
            service.connectGatt(deviceInfo.device, false, timeoutGattCallback)
        }
    }

    private fun getIntent(connectType: GattConnectType?): Intent? {
        return when (connectType) {
            GattConnectType.THERMOMETER -> {
                Intent(activity, HealthThermometerActivity::class.java)
            }
            GattConnectType.LIGHT -> {
                Intent(activity, ConnectedLightingActivity::class.java)
            }
            GattConnectType.BLINKY -> {
                Intent(activity, BlinkyActivity::class.java)
            }
            GattConnectType.BLINKY_THUNDERBOARD -> {
                Intent(activity, BlinkyThunderboardActivity::class.java)
            }
            GattConnectType.THROUGHPUT_TEST -> {
                Intent(activity, ThroughputActivity::class.java)
            }
            GattConnectType.WIFI_COMMISSIONING -> {
                Intent(activity, WifiCommissioningActivity::class.java)
            }
            GattConnectType.IOP_TEST -> {
                Intent(activity, IOPTestActivity::class.java)
            }
            GattConnectType.MOTION -> {
                Intent(activity, MotionActivity::class.java)
            }
            GattConnectType.ENVIRONMENT -> {
                Intent(activity, EnvironmentActivity::class.java)
            }
            else -> null
        }
    }

    interface RangeTestCallback {
        fun onCancel()
        fun getBluetoothDeviceInfo(info: BluetoothDeviceInfo?)
    }

    fun setCallback(rangeTestCallback: RangeTestCallback) {
        this.rangeTestCallback = rangeTestCallback
    }

    companion object {
        private const val CONN_TYPE_INFO = "_conn_type_info_"

        const val MODEL_TYPE_EXTRA = "model_type"
        const val POWER_SOURCE_EXTRA = "power_source"
        private const val SCAN_UPDATE_PERIOD = 2000L //ms

        fun newDialog(connectType: GattConnectType?, service: BluetoothService? = null) : SelectDeviceDialog {
            return SelectDeviceDialog(service).apply {
                arguments = Bundle().apply {
                    connectType?.let { putInt(CONN_TYPE_INFO, connectType.ordinal) }
                }
            }
        }
    }

    override fun handleScanResult(scanResult: ScanResultCompat) {
        viewModel.handleScanResult(scanResult)
    }

    override fun onDiscoveryFailed() {
        viewModel.setIsScanningOn(false)
        dismiss()
    }

}