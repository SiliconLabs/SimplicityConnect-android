package com.siliconlabs.bledemo.home_screen.dialogs

import android.annotation.SuppressLint
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.home_screen.adapters.ScannedDevicesAdapter
import com.siliconlabs.bledemo.base.activities.BaseActivity
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.home_screen.viewmodels.SelectDeviceViewModel
import com.siliconlabs.bledemo.bluetooth.ble.*
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService.GattConnectType
import com.siliconlabs.bledemo.features.demo.blinky.activities.BlinkyActivity
import com.siliconlabs.bledemo.features.demo.connected_lighting.activities.ConnectedLightingActivity
import com.siliconlabs.bledemo.features.demo.health_thermometer.activities.HealthThermometerActivity
import com.siliconlabs.bledemo.features.demo.throughput.activities.ThroughputActivity
import com.siliconlabs.bledemo.features.demo.throughput.utils.PeripheralManager
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.base.models.ThunderBoardDevice
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.blinky_thunderboard.activities.BlinkyThunderboardActivity
import com.siliconlabs.bledemo.databinding.DialogSelectDeviceBinding
import com.siliconlabs.bledemo.features.demo.esl_demo.activities.EslDemoActivity
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.environment.activities.EnvironmentActivity
import com.siliconlabs.bledemo.features.demo.thunderboard_demos.demos.motion.activities.MotionActivity
import com.siliconlabs.bledemo.features.demo.wifi_commissioning.activities.WifiCommissioningActivity
import com.siliconlabs.bledemo.features.iop_test.activities.IOPTestActivity
import com.siliconlabs.bledemo.features.iop_test.models.IOPTest
import com.siliconlabs.bledemo.home_screen.activities.MainActivity
import timber.log.Timber

@SuppressLint("MissingPermission")
class SelectDeviceDialog(
    private var bluetoothService: BluetoothService?
) : BaseDialogFragment(
    hasCustomWidth = true,
    isCanceledOnTouchOutside = true,
), ScannedDevicesAdapter.DemoDeviceCallback, BluetoothService.ScanListener {

    private val binding by viewBinding(DialogSelectDeviceBinding::bind)
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

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (characteristic?.uuid == GattCharacteristic.ModelNumberString.uuid) {
                when (connectType) {
                    GattConnectType.MOTION -> launchDemo(characteristic.getStringValue(0))
                    GattConnectType.BLINKY -> {
                        when (characteristic.getStringValue(0)) {
                            ThunderBoardDevice.THUNDERBOARD_MODEL_DEV_KIT_V3,
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

                            else -> {
                                showMessage(R.string.unknown_model)
                                (activity as MainActivity).dismissModalDialog()
                                Timber.d("Unknown model")
                            }
                        }
                    }

                    else -> Unit
                }
            } else if (characteristic?.uuid == GattCharacteristic.PowerSource.uuid) {
                launchDemo(
                    cachedBoardType,
                    characteristic.getIntValue(GattCharacteristic.PowerSource.format, 0)
                )
            }
        }
    }

    private fun getModelNumberCharacteristic(gatt: BluetoothGatt?): BluetoothGattCharacteristic? {
        return gatt?.getService(GattService.DeviceInformation.number)
            ?.getCharacteristic(GattCharacteristic.ModelNumberString.uuid)
    }

    private fun getPowerSourceCharacteristic(gatt: BluetoothGatt?): BluetoothGattCharacteristic? {
        return gatt?.getService(GattService.PowerSource.number)
            ?.getCharacteristic(GattCharacteristic.PowerSource.uuid)
    }

    private fun handleSuccessfulConnection(gatt: BluetoothGatt) {
        when (connectType) {
            GattConnectType.MOTION -> setReadingDialogMsgAndDiscoverServices(gatt)
            GattConnectType.BLINKY -> {
                if (gatt.device.name.startsWith(getString(R.string.blinky_service_name))) launchDemo()
                else setReadingDialogMsgAndDiscoverServices(gatt)
            }

            else -> launchDemo()
        }
        bluetoothService?.isNotificationEnabled = true
    }

    private fun setReadingDialogMsgAndDiscoverServices(gatt: BluetoothGatt) {
        (activity as MainActivity).setModalDialogMessage(R.string.reading_board_type)
        val success = gatt.discoverServices()
        if (!success) {
            // Handle unsuccessful service discovery
            showMessage(R.string.failed_to_discover_services)
            (activity as MainActivity).dismissModalDialog()
        }
    }

    private fun launchDemo(boardType: String? = null, powerSource: Int? = null) {
        getIntent(connectType)?.let { intent ->
            boardType?.let { intent.putExtra(MODEL_TYPE_EXTRA, it) }
            powerSource?.let { intent.putExtra(POWER_SOURCE_EXTRA, it) }
            currentDeviceInfo?.let { intent.putExtra(DEVICE_ADDRESS_EXTRA, it.device.address) }
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

        viewModel = ViewModelProvider(this)[SelectDeviceViewModel::class.java]
        adapter = ScannedDevicesAdapter(
            mutableListOf(),
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
        binding.buttonCancel.setOnClickListener {
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
        binding.list.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = this@SelectDeviceDialog.adapter
        }
    }

    private fun observeChanges() {
        viewModel.apply {
            isScanningOn.observe(viewLifecycleOwner) {
                toggleScanning(it)
                toggleRefreshInfoRunnable(it)
            }
            isAnyDeviceDiscovered.observe(viewLifecycleOwner) {
                toggleListView(it)
            }
            numberOfDevices.observe(viewLifecycleOwner) {
                binding.devicesNumber.text = getString(R.string.DEVICE_LIST, it)
            }
            deviceToInsert.observe(viewLifecycleOwner) {
                adapter.addNewDevice(it)
            }
        }
    }

    @SuppressLint("StringFormatInvalid")
    private fun initDemoDescription() {
        binding.dialogTextInfo.apply {
            text = when (connectType) {
                GattConnectType.THERMOMETER -> getString(
                    R.string.soc_must_be_connected,
                    getString(R.string.demo_firmware_name_health_thermometer)
                )

                GattConnectType.LIGHT -> getString(
                    R.string.soc_must_be_connected,
                    getString(R.string.demo_firmware_name_connected_lighting)
                )

                GattConnectType.RANGE_TEST -> getString(
                    R.string.soc_must_be_connected,
                    getString(R.string.demo_firmware_name_range_test)
                )

                GattConnectType.BLINKY -> getString(R.string.soc_blinky_must_be_connected)
                GattConnectType.THROUGHPUT_TEST -> getString(
                    R.string.soc_must_be_connected,
                    getString(R.string.demo_firmware_name_throughput)
                )

                GattConnectType.MOTION -> getString(R.string.soc_thunderboard_must_be_connected)
                GattConnectType.ENVIRONMENT -> getString(R.string.soc_thunderboard_must_be_connected)
                GattConnectType.IOP_TEST -> getString(
                    R.string.soc_must_be_connected,
                    getString(R.string.demo_firmware_name_iop)
                )

                GattConnectType.WIFI_COMMISSIONING -> {
                    Html.fromHtml(
                        getString(R.string.soc_wifi_commissioning_must_be_connected),
                        Html.FROM_HTML_MODE_LEGACY
                    )
                }

                GattConnectType.ESL_DEMO -> getString(
                    R.string.soc_must_be_connected,
                    getString(R.string.demo_firmware_name_esl)
                )

                GattConnectType.DEV_KIT_SENSOR -> getString(
                    R.string.demo_firmware_name_wifi_commission,
                    getString(R.string.demo_firmware_name_esl)
                )

                GattConnectType.AWS_DEMO -> {
                    getString(R.string.aws_iot_selection_ble_title)
                }

                else -> getString(R.string.empty_description)
            }

            if (connectType == GattConnectType.WIFI_COMMISSIONING || connectType == GattConnectType.AWS_DEMO) {
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
        binding.apply {
            if (isAnyDeviceDiscovered) {
                list.visibility = View.VISIBLE
                noDevicesFound.visibility = View.GONE
                demoScanProgressBar.visibility = View.GONE
            } else {
                list.visibility = View.GONE
                noDevicesFound.visibility = View.VISIBLE
                demoScanProgressBar.visibility = View.INVISIBLE
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? = inflater.inflate(R.layout.dialog_select_device, container, false)

    override fun onResume() {
        super.onResume()
        if (dialog?.window != null) {
            viewModel.setIsScanningOn(true)
        }
    }

    private fun applyDemoFilters() = buildList {
        val manufacturerDataFilter = ManufacturerDataFilter(
            id = 71,
            data = byteArrayOf(2, 0),
        )

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

            /*GattConnectType.BLINKY -> {
                add(buildFilter(BuildFilterName.BLINKY))
                add(buildFilter(manufacturerDataFilter))
            }*/

            GattConnectType.THROUGHPUT_TEST -> {
                add(buildFilter(BuildFilterName.THROUGHPUT_TEST))
            }

            GattConnectType.WIFI_COMMISSIONING -> {
                add(buildFilter(BuildFilterName.BLE_CONFIGURATOR))
            }

            GattConnectType.IOP_TEST -> {
                add(buildFilter(BuildFilterName.IOP_TEST))
                add(buildFilter(BuildFilterName.IOP_TEST_UPDATE))
                add(buildFilter(BuildFilterName.IOP_TEST_NO_1))
                add(buildFilter(BuildFilterName.IOP_TEST_NO_2))
            }

            GattConnectType.MOTION,
            GattConnectType.ENVIRONMENT -> {
                add(buildFilter(manufacturerDataFilter))
            }

            GattConnectType.ESL_DEMO -> {
                add(buildFilter(GattService.EslDemoService))
            }

            GattConnectType.DEV_KIT_SENSOR -> {
                add(buildFilter(BuildFilterName.DEV_KIT_SENSOR))
            }
            GattConnectType.AWS_DEMO -> {
                add(buildFilter(BuildFilterName.BLE_CONFIGURATOR))
            }
            else -> Unit
        }
    }

    private fun buildFilter(name: BuildFilterName) = ScanFilter
        .Builder().apply {
            setDeviceName(name.value)
        }.build()

    private fun buildFilter(service: GattService) = ScanFilter
        .Builder().apply {
            setServiceUuid(ParcelUuid(service.number), ParcelUuid(GattService.UUID_MASK))
        }.build()

    private fun buildFilter(manufacturerData: ManufacturerDataFilter) = ScanFilter
        .Builder().apply {
            manufacturerData.run { setManufacturerData(id, data, mask) }
        }.build()

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
            (activity as BaseActivity).showModalDialog(BaseActivity.ConnectionStatus.CONNECTING) {
                currentDeviceInfo?.let { service.disconnectGatt(it.address) }
            }
            service.isNotificationEnabled = false
            service.connectGatt(deviceInfo.device, false, timeoutGattCallback)
        }
    }

    private fun getIntent(connectType: GattConnectType?): Intent? {
        val clazz = when (connectType) {
            GattConnectType.THERMOMETER -> HealthThermometerActivity::class.java
            GattConnectType.LIGHT -> ConnectedLightingActivity::class.java
            GattConnectType.BLINKY -> BlinkyActivity::class.java
            GattConnectType.BLINKY_THUNDERBOARD -> BlinkyThunderboardActivity::class.java
            GattConnectType.THROUGHPUT_TEST -> ThroughputActivity::class.java
            GattConnectType.WIFI_COMMISSIONING -> WifiCommissioningActivity::class.java
            GattConnectType.IOP_TEST -> IOPTestActivity::class.java
            GattConnectType.MOTION -> MotionActivity::class.java
            GattConnectType.ENVIRONMENT -> EnvironmentActivity::class.java
            GattConnectType.ESL_DEMO -> EslDemoActivity::class.java
            GattConnectType.DEV_KIT_SENSOR -> WifiCommissioningActivity::class.java
            GattConnectType.AWS_DEMO -> WifiCommissioningActivity::class.java

            else -> null
        }

        val intent = clazz?.let { Intent(activity, it) }
        if (connectType == GattConnectType.WIFI_COMMISSIONING || connectType == GattConnectType.DEV_KIT_SENSOR || connectType == GattConnectType.AWS_DEMO)  {
            intent?.putExtra("connectType", connectType)
        }
        return intent
    }

    interface RangeTestCallback {
        fun onCancel()
        fun getBluetoothDeviceInfo(info: BluetoothDeviceInfo?)
    }

    fun setCallback(rangeTestCallback: RangeTestCallback) {
        this.rangeTestCallback = rangeTestCallback
    }

    override fun handleScanResult(scanResult: ScanResultCompat) {
        viewModel.handleScanResult(scanResult, connectType, context)
    }

    override fun onDiscoveryFailed() {
        viewModel.setIsScanningOn(false)
        dismiss()
    }

    override fun onDiscoveryTimeout() {
        /* Scanning through this dialog is always indefinite. */
    }

    companion   object {
        private const val CONN_TYPE_INFO = "_conn_type_info_"

        const val MODEL_TYPE_EXTRA = "model_type"
        const val POWER_SOURCE_EXTRA = "power_source"
        const val DEVICE_ADDRESS_EXTRA = "device_address"
        private const val SCAN_UPDATE_PERIOD = 10L //ms

        fun newDialog(
            connectType: GattConnectType?,
            service: BluetoothService? = null
        ): SelectDeviceDialog {
            return SelectDeviceDialog(service).apply {
                arguments = Bundle().apply {
                    connectType?.let { putInt(CONN_TYPE_INFO, connectType.ordinal) }
                }
            }
        }
    }

    private enum class BuildFilterName(val value: String) {
        BLINKY("Blinky Example"),
        THROUGHPUT_TEST("Throughput Test"),
        BLE_CONFIGURATOR("BLE_CONFIGURATOR"),
        IOP_TEST("IOP Test"),
        IOP_TEST_UPDATE("IOP Test Update"),
        IOP_TEST_NO_1("IOP_Test_1"),
        IOP_TEST_NO_2("IOP_Test_2"),
        DEV_KIT_SENSOR("WIFI_SENSOR")

    }
}