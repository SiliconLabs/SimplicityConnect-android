package com.siliconlabs.bledemo.features.demo.matter_demo.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import chip.devicecontroller.ChipClusters
import chip.devicecontroller.ChipDeviceController
import chip.devicecontroller.ChipStructs
import chip.devicecontroller.NetworkCredentials
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.FragmentMatterConnectBinding
import com.siliconlabs.bledemo.features.demo.matter_demo.activities.MatterDemoActivity
import com.siliconlabs.bledemo.features.demo.matter_demo.controller.GenericChipDeviceListener
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.CONTACT_SENSOR_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.LIGHTNING_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.LOCK_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.OCCUPANCY_SENSOR_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.PLUG_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.TEMPERATURE_SENSOR_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.THERMOSTAT_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.WINDOW_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannerFragment.Companion.INPUT_NETWORK_THREAD_TYPE_SELECTED
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannerFragment.Companion.INPUT_NETWORK_WIFI_TYPE_SELECTED
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannerFragment.Companion.INPUT_WIFI_REQ_CODE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannerFragment.Companion.WIFI_INPUT_PASSWORD
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannerFragment.Companion.WIFI_INPUT_SSID
import com.siliconlabs.bledemo.features.demo.matter_demo.manager.BluetoothManager
import com.siliconlabs.bledemo.features.demo.matter_demo.model.CHIPDeviceInfo
import com.siliconlabs.bledemo.features.demo.matter_demo.model.MatterScannedResultModel
import com.siliconlabs.bledemo.features.demo.matter_demo.model.NetworkCredentialsParcelable
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.ChipClient
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.CustomInputDialog
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.CustomProgressDialog
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.DeviceIDUtil
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.FragmentUtils
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.MessageDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber


class MatterConnectFragment : Fragment() {
    private val channelInfo: String = "15"
    private val panId: String = "1234"
    private var customProgressDialog: CustomProgressDialog? = null

    private lateinit var extentedPanId: String
    private lateinit var masterKey: String

    private lateinit var binding: FragmentMatterConnectBinding
    private lateinit var deviceController: ChipDeviceController
    private lateinit var scope: CoroutineScope
    private lateinit var deviceInfo: CHIPDeviceInfo
    private var gatt: BluetoothGatt? = null
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var networkCredential: NetworkCredentialsParcelable
    private lateinit var matterScanDevice: BluetoothDevice
    private var deviceId: Long = 0;

    private var dialog: AlertDialog? = null
    private var otbrEntryDialog: MatterOTBRInputDialogFragment? = null
    private var wifiEntryDialog: MatterWifiInputDialogFragment? = null
    private lateinit var typeNtw: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deviceController = ChipClient.getDeviceController(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        scope = viewLifecycleOwner.lifecycleScope
        if (requireArguments() != null) {
            typeNtw = checkNotNull(requireArguments().getString(DIA_INPUT_NTW_TYPE))
            deviceInfo = checkNotNull(requireArguments().getParcelable(ARG_DEVICE_INFO))
        }
        binding = FragmentMatterConnectBinding.inflate(inflater, container, false)
        return binding.root
    }


    @RequiresApi(Build.VERSION_CODES.R)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MatterDemoActivity).showQRScanner()

        val str = requireContext().getString(R.string.matter_commissioning_device)
        showMatterProgressDialog(str)
        Handler().postDelayed(Runnable {
            removeProgress()
            displayInputWindowBasedOnProvisionType()
        }, 1000)
    }

    private fun removeProgress() {
        if (customProgressDialog?.isShowing == true) {
            customProgressDialog?.dismiss()
        }
    }

    private fun displayInputWindowBasedOnProvisionType() {
        when (typeNtw) {
            INPUT_NETWORK_WIFI_TYPE_SELECTED /* ProvisionNetworkType.WIFI */ -> {
                if (wifiEntryDialog == null) {
                    val prev =
                        requireActivity().supportFragmentManager.findFragmentByTag(
                            DIALOG_WIFI_INPUT_TAG
                        )
                    if (prev == null) {
                        wifiEntryDialog = MatterWifiInputDialogFragment.newInstance()
                        wifiEntryDialog!!.setTargetFragment(this, DIALOG_WIFI_FRAGMENT)
                        wifiEntryDialog!!.show(
                            requireActivity().supportFragmentManager,
                            DIALOG_WIFI_INPUT_TAG
                        )
                    }
                }
            }

            INPUT_NETWORK_THREAD_TYPE_SELECTED  /*  ProvisionNetworkType.THREAD*/ -> {
                if (otbrEntryDialog == null) {
                    val prev =
                        requireActivity().supportFragmentManager.findFragmentByTag(
                            DIALOG_THREAD_INPUT_TAG
                        )
                    if (prev == null) {
                        otbrEntryDialog = MatterOTBRInputDialogFragment.newInstance()

                        otbrEntryDialog!!.setTargetFragment(this, DIALOG_THREAD_FRAGMENT)
                        otbrEntryDialog!!.show(
                            requireActivity().supportFragmentManager,
                            DIALOG_THREAD_INPUT_TAG
                        )
                    }
                }
            }

            else -> {
                println("Unhandled....")
            }

        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun validateWiFiInput(wifiSSID: String, wifiPassword: String) {
        networkCredential = NetworkCredentialsParcelable
            .forWiFi(
                NetworkCredentialsParcelable.WiFiCredentials(
                    wifiSSID.toString(), wifiPassword.toString()
                )
            )
        // pairDeviceWithAddress()
        startConnectingToDevice()
    }

    @SuppressLint("NewApi")
    private fun validateThreadInput(inputOTBR: String) {
        if (inputOTBR.isNullOrBlank()) {
            Toast.makeText(requireContext(), "input OTBR is empty", Toast.LENGTH_SHORT).show()
            return
        }
        val operationalDataset = dataFromHexString(inputOTBR.trim())

        networkCredential = NetworkCredentialsParcelable
            .forThread(NetworkCredentialsParcelable.ThreadCredentials(operationalDataset))
        startConnectingToDevice()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun validateInputOld(input: String) {
//        if (input.isNullOrBlank()) {
//            Toast.makeText(requireContext(), "input OTBR is empty", Toast.LENGTH_SHORT).show()
//            return
//        }

        //--------------------------------------------------------
        //Old Setup Supported Code
        extentedPanId = resources.getString(R.string.extPanID)
        masterKey = resources.getString(R.string.matMasterKey)
        if (channelInfo.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Channel is empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (panId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "PAN ID is empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (extentedPanId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "XPAN ID is empty", Toast.LENGTH_SHORT).show()
            return
        }
        val extendedPanIDStr = extentedPanId.toString().filterNot { c -> c == ':' }
        if (extendedPanIDStr.length != NUM_XPANID_BYTES * 2) {
            Toast.makeText(requireContext(), "Extended PAN ID is invalid", Toast.LENGTH_SHORT)
                .show()
            return
        }

        if (masterKey.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Master Key is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val masterKeyStr = masterKey.toString().filterNot { c -> c == ':' }
        if (masterKeyStr.length != NUM_MASTER_KEY_BYTES * 2) {
            Toast.makeText(requireContext(), "Master key is invalid", Toast.LENGTH_SHORT).show()
            return
        }
        val extPID = extendedPanIDStr.hexToByteArray()
        val masKey = masterKeyStr.hexToByteArray()

        val operationalDataset = makeThreadOperationalDataset(
            channelInfo.toInt(),
            panId.toInt(16),
            extPID,
            masKey
        )
        println(
            "OperationDataSet ${operationalDataset.size}"
        )
        //--------------------------------------------------------

        // val operationalDataset = dataFromHexString(input.trim())
        networkCredential = NetworkCredentialsParcelable
            .forThread(NetworkCredentialsParcelable.ThreadCredentials(operationalDataset))
        startConnectingToDevice()
    }


    private fun convertInputToByteArray(strInput: String): ByteArray {
        return strInput.toByteArray()
    }

    private fun makeThreadOperationalDataset(
        channel: Int,
        panId: Int,
        xpanId: ByteArray,
        masterKey: ByteArray
    ): ByteArray {

        // channel
        var dataset = byteArrayOf(TYPE_CHANNEL.toByte(), NUM_CHANNEL_BYTES.toByte())
        dataset += 0x00.toByte() // Channel Page 0.
        dataset += (channel.shr(8) and 0xFF).toByte()
        dataset += (channel and 0xFF).toByte()

        // PAN ID
        dataset += TYPE_PANID.toByte()
        dataset += NUM_PANID_BYTES.toByte()
        dataset += (panId.shr(8) and 0xFF).toByte()
        dataset += (panId and 0xFF).toByte()

        // Extended PAN ID
        dataset += TYPE_XPANID.toByte()
        dataset += NUM_XPANID_BYTES.toByte()
        dataset += xpanId

        // Network Master Key
        dataset += TYPE_MASTER_KEY.toByte()
        dataset += NUM_MASTER_KEY_BYTES.toByte()
        dataset += masterKey
        Timber.tag(TAG).e("dataset " + dataset.decodeToString())

        return dataset
    }


    private fun String.hexToByteArray(): ByteArray {
        return chunked(2).map { byteStr -> byteStr.toUByte(16).toByte() }.toByteArray()
    }


    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("MissingPermission")
    private fun startConnectingToDevice() {
        if (gatt != null) {
            return
        }
        scope.launch {

            bluetoothManager = BluetoothManager()
            val strId = R.string.rendezvous_over_ble_scanning_text
            val devInfo = deviceInfo.discriminator.toString()
            showMessage(strId, devInfo)
            val device = bluetoothManager.getBluetoothDevice(
                requireContext(),
                deviceInfo.discriminator, deviceInfo.isShortDiscriminator
            ) ?: kotlin.run {
                requireActivity().supportFragmentManager.popBackStack()
                requireActivity().supportFragmentManager.popBackStack()

                showMessage(R.string.rendezvous_over_ble_scanning_failed_text)
                return@launch
            }
            matterScanDevice = device
            Timber.tag(TAG).e("Type :" + device.type.toString())
            Timber.tag(TAG).e("UUID :" + device.uuids.toString())
            Timber.tag(TAG).e("Name :" + device.name.toString())
            Timber.tag(TAG).e("Address :" + device.address.toString())
            Timber.tag(TAG).e("Alias :" + device.alias.toString())
            showMatterProgressDialog(getString(R.string.device_commissioning_in_progress) + " " + device.name)


            showMessage(
                R.string.rendezvous_over_ble_connecting_text,
                device.name ?: device.address.toString()
            )
            removeAlert()
            deviceId = DeviceIDUtil.getNextAvailableId(requireContext())
            gatt = bluetoothManager.connect(requireContext(), device)
            deviceController.setCompletionListener(ConnectionCallback())
            val connId = bluetoothManager.connectionId
            var network: NetworkCredentials? = null


            val thread = networkCredential.threadCredentials
            if (thread != null) {
                network =
                    NetworkCredentials.forThread(
                        NetworkCredentials.ThreadCredentials(thread.operationalDataset)
                    )
            }
            val wifi = networkCredential.wiFiCredentials
            if (wifi != null) {
                network =
                    NetworkCredentials.forWiFi(
                        NetworkCredentials.WiFiCredentials(wifi.ssid, wifi.password)
                    )
            }

            setAttestationDelegate()

            deviceController.pairDevice(
                gatt,
                connId,
                deviceId,
                deviceInfo.setupPinCode,
                network
            )
            DeviceIDUtil.setNextAvailableId(requireContext(), deviceId + 1)
        }
    }

    private fun showMatterProgressDialog(message: String) {

        customProgressDialog = CustomProgressDialog(requireContext())
        customProgressDialog!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        customProgressDialog!!.setMessage(message)
        customProgressDialog!!.show()
    }

    private fun setAttestationDelegate() {
        deviceController.setDeviceAttestationDelegate(DEVICE_ATTESTATION_FAILED_TIMEOUT) { devicePtr, _, errorCode ->

            Timber.tag(TAG).e(
                "Device attestation errorCode: $errorCode, " +
                        "Look at 'src/credentials/attestation_verifier/DeviceAttestationVerifier.h' " +
                        "AttestationVerificationResult enum to understand the errors"
            )
            val activity = requireActivity()
            Timber.tag(TAG).e("setAttestationDelegate()--errorCode:" + errorCode)
            if (errorCode == STATUS_PAIRING_SUCCESS) {
                Timber.tag(TAG).e("setAttestationDelegate() In--errorCode:" + errorCode)
                activity.runOnUiThread(Runnable {
                    deviceController.continueCommissioning(devicePtr, true)
                })

                return@setDeviceAttestationDelegate
            }
            activity.runOnUiThread(Runnable {
                if (dialog != null && dialog?.isShowing == true) {
                    Timber.tag(TAG).e("Dialog is already showing...")
                    return@Runnable
                }
                dialog = AlertDialog.Builder(activity)
                    .setPositiveButton("Continue",
                        DialogInterface.OnClickListener { dialog, id ->
                            deviceController.continueCommissioning(devicePtr, true)
                        })
                    .setNegativeButton("No",
                        DialogInterface.OnClickListener { dialog, id ->
                            deviceController.continueCommissioning(devicePtr, false)
                        })
                    .setTitle("Device Attestation")
                    .setMessage("Device Attestation failed for device under commissioning. Do you wish to continue pairing?")
                    .show()
            })

        }
    }

    private fun removeAlert() {
        if (otbrEntryDialog != null) {
            otbrEntryDialog!!.dismiss()
        }
    }

    private fun onCommissionCompleted() {
        ChipClient.getDeviceController(requireContext()).close()
    }

    inner class ConnectionCallback : GenericChipDeviceListener() {
        override fun onConnectDeviceComplete() {
            super.onConnectDeviceComplete()
            Timber.tag(TAG).e("onConnectDeviceComplete")
        }

        override fun onStatusUpdate(status: Int) {
            super.onStatusUpdate(status)
            Timber.tag(TAG).e("onStatusUpdate : $status.toString()")
        }


        @SuppressLint("MissingPermission")
        override fun onCommissioningComplete(nodeId: Long, errorCode: Int) {
            super.onCommissioningComplete(nodeId, errorCode)
            Timber.tag(TAG).e("onCommissioningComplete : NodeID:  $nodeId.toString()")
            Timber.tag(TAG).e("onCommissioningComplete : errorCode: " + errorCode.toString())
            removeAlert()
            if (errorCode == STATUS_PAIRING_SUCCESS) {
                if (customProgressDialog?.isShowing == true) {
                    customProgressDialog?.dismiss()
                }
                Timber.tag(TAG).e("pairing success")
                onCommissionCompleted()

                scope.launch {
                    getDescriptorClusterForDevice().readDeviceTypeListAttribute(object :
                        ChipClusters.DescriptorCluster.DeviceTypeListAttributeCallback {
                        override fun onSuccess(valueList: MutableList<ChipStructs.DescriptorClusterDeviceTypeStruct>?) {

                            Timber.tag(TAG)
                                .e("deviceType  $ valueList?.get(0)?.deviceType?.toInt()!!")
                            val deviceType = valueList?.get(0)?.deviceType?.toInt()!!
                            println("device Type: $deviceType}")
                            println("device Info: ${matterScanDevice.name}  DeviceId: ${deviceId}")
                            var device: String = ""

                            when (deviceType) {
                                LOCK_TYPE ->
                                    device =
                                        requireContext().getString(R.string.matter_lock_list)

                                PLUG_TYPE ->
                                    device =
                                        requireContext().getString(R.string.matter_plug_list)

                                OCCUPANCY_SENSOR_TYPE ->
                                    device =
                                        requireContext().getString(R.string.matter_occupancy_sensor_list)

                                TEMPERATURE_SENSOR_TYPE ->
                                    device =
                                        requireContext().getString(R.string.matter_temperature_sensor_list)

                                CONTACT_SENSOR_TYPE ->
                                    device =
                                        requireContext().getString(R.string.matter_contact_sensor_list)

                                THERMOSTAT_TYPE ->
                                    device =
                                        requireContext().getString(R.string.matter_thermostat_list)

                                LIGHTNING_TYPE ->
                                    device =
                                        requireContext().getString(R.string.matter_light_list)

                                WINDOW_TYPE ->
                                    device =
                                        requireContext().getString(R.string.matter_window_list)

                                else -> device = matterScanDevice.name


                                // else -> device = matterScanDevice.name
                            }

                            val deviceName = device + COLON_WITH_SPACE + deviceId
                            println("device Info: ${deviceName}  DeviceId: ${deviceId}")

                            ShowEditDeviceNameDialog(device, valueList)


                        }

                        private fun ShowEditDeviceNameDialog(
                            device: String,
                            valueList: MutableList<ChipStructs.DescriptorClusterDeviceTypeStruct>
                        ) {

                            val customInputDialog = CustomInputDialog.newInstance(
                                requireContext(), device, getString(R.string.add_device_name),
                                getString(
                                    R.string.add_device_subtitle
                                )
                            )


                            customInputDialog.setOnButtonClickListener { deviceName ->

                                val matterInfo = MatterScannedResultModel(
                                    deviceName,
                                    matterScanDevice.address,
                                    matterScanDevice.type,
                                    deviceId,
                                    valueList?.get(0)?.deviceType?.toInt()!!,
                                    isDeviceOnline = true
                                )
                                FragmentUtils.getHost(
                                    this@MatterConnectFragment,
                                    Callback::
                                    class.java
                                )
                                    .onCommissionCompleteLoadData(matterInfo)

                            }

                            customInputDialog.show(requireFragmentManager(), "CustomInputDialog")


                        }

                        override fun onError(ex: Exception?) {
                            Timber.tag(TAG).e("failed to read readDeviceTypeListAttribute" + ex)
                        }
                    })
                }


            } else {
                if (customProgressDialog?.isShowing() == true) {
                    customProgressDialog?.dismiss()
                }

                /*val strId = R.string.matter_device_offline_text
                showMessages(strId)*/
            }
            onCommissionCompleted()
        }

        override fun onPairingComplete(errorCode: Int) {
            super.onPairingComplete(errorCode)
            Timber.tag(TAG).e("onPairingComplete: $errorCode")
            if (errorCode != STATUS_PAIRING_SUCCESS) {
                showMessage(R.string.rendezvous_over_ble_pairing_failure_text)
                onCommissionCompleted()
            }
        }

        override fun onOpCSRGenerationComplete(csr: ByteArray) {
            super.onOpCSRGenerationComplete(csr)
            Timber.tag(TAG).e(String(csr))
        }

        override fun onPairingDeleted(errorCode: Int) {
            super.onPairingDeleted(errorCode)
            Timber.tag(TAG).e("onPairingDeleted: $errorCode")
        }

        override fun onCloseBleComplete() {
            super.onCloseBleComplete()
            Timber.tag(TAG).e("onCloseBleComplete")
        }

        override fun onError(error: Throwable) {
            super.onError(error)
            Timber.tag(TAG).d("onError: $error")
        }
    }


    private fun showMessages(msgId: Int) {
        requireActivity().runOnUiThread {
            val resString = requireContext().getString(msgId)
            Toast.makeText(requireContext(), resString, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMessage(msgResId: Int, stringArgs: String? = null) {
        requireActivity().runOnUiThread {
            val context = requireContext()
            val msg = context.getString(msgResId, stringArgs)
            Timber.tag(TAG).e("showMessage:$msg")
            Toast.makeText(context, msg, Toast.LENGTH_SHORT)
                .show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onActivityResult(requestCode: Int, resultCode: Int, dataReceived: Intent?) {
        super.onActivityResult(requestCode, resultCode, dataReceived)
        if (resultCode == Activity.RESULT_OK) {
            if (dataReceived != null) {
                val receiveOTBRInfo = dataReceived.getStringExtra(DIALOG_OTBR_INFO)

                println("OTBR data: $receiveOTBRInfo")
                removeAlert()
                validateThreadInput(receiveOTBRInfo!!.trim())
            }
        } else if (resultCode == INPUT_WIFI_REQ_CODE) {
            if (dataReceived != null) {
                val wifiSSID = dataReceived.getStringExtra(WIFI_INPUT_SSID)
                val wifiPassword = dataReceived.getStringExtra(WIFI_INPUT_PASSWORD)
                println("wifiSSID: ${wifiSSID} === wifiPassword:${wifiPassword}")
                if (wifiSSID != null && wifiPassword != null) {
                    validateWiFiInput(wifiSSID, wifiPassword)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        deviceController.close()
    }


    interface Callback {
        fun onCommissionCompleteLoadData(matterScannedResultModel: MatterScannedResultModel)
    }

    private suspend fun getDescriptorClusterForDevice(): ChipClusters.DescriptorCluster {
        return ChipClusters.DescriptorCluster(

            ChipClient.getConnectedDevicePointer(requireContext(), deviceId),
            ON_OFF_CLUSTER_ENDPOINT
        )
    }


    private fun dataFromHexString(string: String): ByteArray {
        var modifiedString = string
        if (string.length % 2 == 1) {
            modifiedString = "0$string"
        }

        val chars = modifiedString.toCharArray()
        var i = 0
        val len = modifiedString.length


        val data = ByteArray(len / 2)
        val byteChars = CharArray(2)


        while (i < len) {
            byteChars[0] = chars[i++]
            byteChars[1] = chars[i++]
            var wholeByte = 0L
            try {
                wholeByte = byteChars.joinToString("").toLong(16)

            } catch (ex: NumberFormatException) {
                var wholeByte = 0L
                Timber.tag(TAG).e("Number Format Exception Occurred..")
            }

            data[(i - 2) / 2] = wholeByte.toByte()
        }

        return data
    }


    companion object {
        private val TAG = "MatterConnectFragment"
        private const val ARG_DEVICE_INFO = "device_info"
        private const val NUM_CHANNEL_BYTES = 3
        private const val NUM_PANID_BYTES = 2
        private const val NUM_XPANID_BYTES = 8
        private const val NUM_MASTER_KEY_BYTES = 16
        private const val TYPE_CHANNEL = 0 // Type of Thread Channel TLV.
        private const val TYPE_PANID = 1 // Type of Thread PAN ID TLV.
        private const val TYPE_XPANID = 2 // Type of Thread Extended PAN ID TLV.
        private const val TYPE_MASTER_KEY = 5 // Type of Thread Network Master Key TLV.
        public const val MATTER_DEVICE_LOCK = "MATTER-3840"
        public const val MATTER_DEVICE_LIGHT = "Silabs-Light"
        public const val MATTER_DEVICE_WINDOW = "Silabs-Window"
        public const val MATTER_DEVICE_SENSOR = "SL-SENSOR"
        private const val STATUS_PAIRING_SUCCESS = 0
        private const val DEVICE_ATTESTATION_FAILED_TIMEOUT = 600
        public const val ON_OFF_CLUSTER_ENDPOINT = 1
        private const val COLON_WITH_SPACE = " - "
        public const val SPACE = " "
        private const val DIALOG_THREAD_FRAGMENT = 999
        private const val DIALOG_WIFI_FRAGMENT = 998
        const val DIA_INPUT_NTW_TYPE = "dia_input_ntw_type"
        const val DIALOG_OTBR_INFO = "Dialog_OTBR_Info"
        private const val DIALOG_THREAD_INPUT_TAG = "MatterThreadDialogFragmentTAG"
        private const val DIALOG_WIFI_INPUT_TAG = "MatterWiFiDialogFragmentTAG"
        fun newInstance(): MatterConnectFragment {
            val args = Bundle()

            val fragment = MatterConnectFragment()
            fragment.arguments = args
            return fragment
        }
    }

}