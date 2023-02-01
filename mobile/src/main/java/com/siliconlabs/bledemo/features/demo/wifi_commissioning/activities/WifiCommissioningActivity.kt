package com.siliconlabs.bledemo.features.demo.wifi_commissioning.activities

import android.app.AlertDialog
import android.app.ProgressDialog
import android.bluetooth.*
import android.content.*
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import com.siliconlabs.bledemo.bluetooth.ble.GattCharacteristic
import com.siliconlabs.bledemo.bluetooth.ble.GattService
import com.siliconlabs.bledemo.bluetooth.ble.TimeoutGattCallback
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.base.activities.BaseDemoActivity
import com.siliconlabs.bledemo.databinding.ActivityWifiCommissioningBinding
import com.siliconlabs.bledemo.utils.Converters
import com.siliconlabs.bledemo.features.demo.wifi_commissioning.adapters.AccessPointsAdapter
import com.siliconlabs.bledemo.features.demo.wifi_commissioning.models.AccessPoint
import com.siliconlabs.bledemo.features.demo.wifi_commissioning.models.BoardCommand
import com.siliconlabs.bledemo.features.demo.wifi_commissioning.models.SecurityMode
import timber.log.Timber
import java.util.*

/**
 * Created by harika on 18-04-2016.
 */
class WifiCommissioningActivity : BaseDemoActivity() {

    private lateinit var _binding: ActivityWifiCommissioningBinding
    private var progressDialog: ProgressDialog? = null

    private var accessPointsAdapter: AccessPointsAdapter? = null
    private val accessPoints = ArrayList<AccessPoint>()
    private var isItemClicked = false
    private var clickedAccessPoint: AccessPoint? = null
    private var connectedAccessPoint: AccessPoint? = null

    private var characteristicWrite: BluetoothGattCharacteristic? = null
    private var characteristicRead: BluetoothGattCharacteristic? = null
    private var characteristicNotification: BluetoothGattCharacteristic? = null
    private val clientCharacteristicConfig = "00002902-0000-1000-8000-00805f9b34fb"

    private val unprintableCharsRegex = Regex("[\\x00-\\x1F]")

    private var sendCommand: BoardCommand.Send = BoardCommand.Send.UNKNOWN
    private var readCommand: BoardCommand.Response = BoardCommand.Response.UNKNOWN

    private val sleepForWrite: Long = 500
    private val sleepForRead: Long = 500


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityWifiCommissioningBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        setupRecyclerView()
        setupUiListeners()
    }


    override fun onBluetoothServiceBound() {
        service?.also {
            it.registerGattCallback(mBluetoothGattCallback)
            it.connectedGatt?.discoverServices()
        }
        showProgressDialog(getString(R.string.ble_detail_device_connection))
    }

    private fun setupRecyclerView() {
        accessPointsAdapter = AccessPointsAdapter(accessPoints, object : AccessPointsAdapter.OnItemClickListener {
            override fun onItemClick(itemView: View?, position: Int) {
                onAccessPointClicked(position)
            }
        })
        _binding.wifiAccessPtsList.apply {
            adapter = accessPointsAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupUiListeners() {
        _binding.disconnectBtn.setOnClickListener { showDisconnectionDialog() }
    }

    private fun showProgressDialog(message: String) {
        runOnUiThread {
            progressDialog?.dismiss()
            progressDialog = ProgressDialog.show(this, getString(R.string.empty_description), message)
        }
    }

    private fun dismissProgressDialog() {
        runOnUiThread { progressDialog?.dismiss() }
    }

    override fun onBackPressed() {
        service?.clearConnectedGatt()
        super.onBackPressed()
    }

    private fun onAccessPointClicked(position: Int) {
        isItemClicked = true
        clickedAccessPoint = accessPointsAdapter?.getItem(position)
        showProgressDialog(getString(R.string.check_for_status))
        writeCommand(BoardCommand.Send.CHECK_CONNECTION)
    }

    private fun showToastOnUi(message: String) {
        runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
    }

    fun onAccessPointScanned(accessPoint: AccessPoint) {
        dismissProgressDialog()
        accessPoints.add(accessPoint)
        runOnUiThread { accessPointsAdapter?.notifyDataSetChanged() }
    }

    fun onAccessPointConnection(isSuccessful: Boolean) {
        dismissProgressDialog()
        if (isSuccessful) {
            showToastOnUi(getString(R.string.ap_connect))
            connectedAccessPoint = clickedAccessPoint
            connectedAccessPoint?.status = true

            runOnUiThread { accessPointsAdapter?.notifyDataSetChanged() }
        } else {
            showToastOnUi(getString(R.string.ap_connect_fail))
        }
    }

    fun onAccessPointDisconnection(isSuccessful: Boolean) {
        dismissProgressDialog()
        if (isSuccessful) {
            connectedAccessPoint = null
            showToastOnUi(getString(R.string.ap_disconnect_success))
            scanForAccessPoints()
            toggleMainView(isAccessPointConnected = false)
        } else {
            showToastOnUi(getString(R.string.ap_disconnect_fail))
        }
    }

    fun isAccessPointConnected(isConnected: Boolean) {
        dismissProgressDialog()
        if (isConnected) {
            if (isItemClicked) { /* Board already connected when clicking on item */
                showDisconnectionDialog()
            } else { /* Board connected when entering the app */
                toggleMainView(isAccessPointConnected = true)
            }
        } else {
            clickedAccessPoint?.let { /* No board connected when clicking on item */
                if (it.securityMode != SecurityMode.OPEN) showPasswordDialog()
                else {
                    it.password = getString(R.string.empty_description)
                    writeCommand(BoardCommand.Send.SSID, it.name)
                    showProgressDialog(getString(R.string.config_AP))
                }
            } ?: scanForAccessPoints() /* No board connected when entering the app */
        }
    }

    fun onFirmwareVersionReceived(firmwareVersion: String) {
        runOnUiThread { _binding.firmwareVersionTv.text = firmwareVersion }
        writeCommand(BoardCommand.Send.CHECK_CONNECTION)
    }

    private fun toggleMainView(isAccessPointConnected: Boolean) {
        runOnUiThread {
            _binding.apConnectedLayout.visibility = if (isAccessPointConnected) View.VISIBLE else View.GONE
            _binding.wifiAccessPtsList.visibility = if (isAccessPointConnected) View.GONE else View.VISIBLE
        }
    }

    private fun showDisconnectionDialog() {
        val dialogMessage =
                if (connectedAccessPoint?.name == clickedAccessPoint?.name) getString(R.string.disconnect_title)
                else getString(R.string.another_ap_connected)

        AlertDialog.Builder(this).apply {
            setCancelable(false)
            setMessage(dialogMessage)
            setPositiveButton(getString(R.string.yes)) { dialog: DialogInterface, _: Int ->
                showProgressDialog(getString(R.string.disconnect_ap))
                writeCommand(BoardCommand.Send.DISCONNECTION)
                dialog.cancel()
            }
            setNegativeButton(getString(R.string.no)) { dialog: DialogInterface, _: Int ->
                isItemClicked = false
                dialog.cancel()
            }
            runOnUiThread {
                create()
                show()
            }
        }
    }

    private fun showPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.secured_ap_connect_dialog, null)
        val password = dialogView.findViewById<EditText>(R.id.password_et)

        AlertDialog.Builder(this).apply {
            setView(dialogView)
            setTitle(clickedAccessPoint?.name)
            setPositiveButton(getString(R.string.connect_btn_txt)) { dialog: DialogInterface?, _: Int ->
                if (password.text.toString().isNotEmpty()) {
                    dialog?.dismiss()
                    showProgressDialog(getString(R.string.config_AP))
                    clickedAccessPoint?.let {
                        it.password = password.text.toString()
                        writeCommand(BoardCommand.Send.SSID, it.name)
                    }
                } else {
                    showToastOnUi(getString(R.string.invalid_password))
                    dialog?.dismiss()
                }
            }
            setNegativeButton(getString(R.string.button_cancel)) { dialog: DialogInterface?, _: Int ->
                dialog?.dismiss()
            }
            runOnUiThread {
                create()
                show()
            }
        }
    }

    private fun scanForAccessPoints() {
        clickedAccessPoint = null
        accessPoints.clear()
        runOnUiThread { accessPointsAdapter?.notifyDataSetChanged() }
        showProgressDialog(getString(R.string.scanning_for_access_points))
        writeCommand(BoardCommand.Send.SCAN)
    }


    private val mBluetoothGattCallback: TimeoutGattCallback = object : TimeoutGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Timber.d("onConnectionStateChange; status = $status, newState = $newState")
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                onDeviceDisconnected()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Timber.d("onServicesDiscovered; status = $status")

            gatt.getService(GattService.WifiCommissioningService.number)?.let {
                characteristicWrite = it.getCharacteristic(GattCharacteristic.WifiCommissioningWrite.uuid)
                characteristicRead = it.getCharacteristic(GattCharacteristic.WifiCommissioningRead.uuid)
                characteristicNotification = it.getCharacteristic(GattCharacteristic.WifiCommissioningNotify.uuid)
            }

            if (gatt.setCharacteristicNotification(characteristicNotification, true)) {
                Timber.d("Notifications enabled for ${characteristicNotification?.uuid}")
                characteristicNotification?.getDescriptor(UUID.fromString(clientCharacteristicConfig))?.let {
                    it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(it)
                }
            }

            writeCommand(BoardCommand.Send.GET_FIRMWARE_VERSION)
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt,
                                           characteristic: BluetoothGattCharacteristic,
                                           status: Int) {
            Timber.d("onCharacteristicWrite; characteristic = ${characteristic.uuid}, sendCommand = $sendCommand, status = $status")
            Timber.d("Raw data = ${Arrays.toString(characteristic.value)}")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (sendCommand) {
                    BoardCommand.Send.SSID -> writeCommand(
                            BoardCommand.Send.SECURITY_TYPE, clickedAccessPoint?.securityMode?.value?.toString()!!)
                    BoardCommand.Send.DISCONNECTION -> readOperation()
                    BoardCommand.Send.SECURITY_TYPE -> {
                        if (clickedAccessPoint?.password!!.isNotEmpty()) {
                            writeCommand(BoardCommand.Send.PASSWORD, clickedAccessPoint?.password!!)
                        } else {
                            sendCommand = BoardCommand.Send.SSID
                            readOperation()
                        }
                    }
                    BoardCommand.Send.PASSWORD -> {
                        sendCommand = BoardCommand.Send.SSID
                        readOperation()
                    }
                    BoardCommand.Send.CHECK_CONNECTION -> {
                        sendCommand = BoardCommand.Send.CHECK_CONNECTION
                        readOperation()
                    }
                    BoardCommand.Send.GET_FIRMWARE_VERSION -> {
                        sendCommand = BoardCommand.Send.GET_FIRMWARE_VERSION
                        readOperation()
                    }
                    else -> { }
                }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt,
                                          characteristic: BluetoothGattCharacteristic,
                                          status: Int) {
            Timber.d("onCharacteristicRead; characteristic = ${characteristic.uuid}, readCommand = $readCommand, status = $status")
            Timber.d("Raw data = ${Arrays.toString(characteristic.value)}")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                readCommand = BoardCommand.Response.fromInt(
                        characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                )
                val statusBit = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1)

                if (readCommand.value != sendCommand.value) {
                    readOperation()
                } else {
                    when (readCommand) {
                        BoardCommand.Response.CONNECTION -> {
                            if (statusBit == 1) {
                                clickedAccessPoint?.macAddress = convertMacAddressToString(characteristic.value.copyOfRange(3, 9))
                                clickedAccessPoint?.ipAddress = convertIpAddressToString(characteristic.value.copyOfRange(10, 14))
                                onAccessPointConnection(true)
                            } else {
                                onAccessPointConnection(false)
                            }
                        }
                        BoardCommand.Response.DISCONNECTION -> onAccessPointDisconnection(statusBit == 1)
                        BoardCommand.Response.CHECK_CONNECTION -> isAccessPointConnected(statusBit == 1)
                        BoardCommand.Response.FIRMWARE_VERSION -> {
                            if (statusBit > 0) {
                                val rawString = characteristic.getStringValue(2)
                                onFirmwareVersionReceived(rawString.replace(unprintableCharsRegex, ""))
                            }
                        }
                        BoardCommand.Response.UNKNOWN -> {
                            if (sendCommand != BoardCommand.Send.SSID) readOperation()
                        }
                    }
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            Timber.d("onCharacteristicChanged; characteristic = ${characteristic.uuid}")
            Timber.d("Raw data = ${Arrays.toString(characteristic.value)}")

            if (characteristic.uuid == GattCharacteristic.WifiCommissioningNotify.uuid) {
                /* Scanned access point info. */
                val rawName = characteristic.getStringValue(2)
                val rawSecurityMode = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                Timber.d("Raw access point name = $rawName")

                onAccessPointScanned(AccessPoint(
                        name = rawName.replace(unprintableCharsRegex, ""),
                        securityMode =  SecurityMode.fromInt(rawSecurityMode)
                ))
            }
        }
    }

    private fun writeCommand(command: BoardCommand.Send) {
        sendCommand = command
        writeUntilSuccess(sendCommand.value.toString())
    }

    private fun writeCommand(command: BoardCommand.Send, additionalData: String) {
        sendCommand = command

        val stringToWrite = StringBuilder().apply {
            append(sendCommand.value)
            if (additionalData.length < 10) append('0')
            append(additionalData.length)
            append(additionalData)
        }.toString()

        writeUntilSuccess(stringToWrite)
    }

    private fun writeUntilSuccess(dataToWrite: String) {
        Thread {
            var writeStatus = false
            while (!writeStatus) {
                writeStatus = writeToCharacteristic(service?.connectedGatt, characteristicWrite, dataToWrite)
                Thread.sleep(sleepForWrite)
            }
            Timber.d("Command $sendCommand written successfully")
        }.start()
    }

    private fun writeToCharacteristic(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, data: String): Boolean {
        return if (gatt != null && characteristic != null) {
            Timber.d("Data to write = $data")
            if (data.length > 100) {
                Timber.d("Attempt to write to characteristic with more then 64 bytes")
                return false
            }
            characteristic.setValue(data)
            gatt.writeCharacteristic(characteristic)
        } else {
            Timber.d("Fail to write: null references")
            false
        }
    }

    private fun readOperation() {
        Thread {
            service?.connectedGatt?.readCharacteristic(characteristicRead)
            Thread.sleep(sleepForRead)
        }.start()
    }

    private fun convertMacAddressToString(rawData: ByteArray) : String {
        return StringBuilder().apply {
            rawData.forEachIndexed { index, byte ->
                val hexValue = Integer.toHexString(Converters.getIntFromTwosComplement(byte))

                append(hexValue)
                if (hexValue.length == 1) insert(length - 1, '0')
                if (index != rawData.size - 1) append(':')
            }
        }.toString()
    }

    private fun convertIpAddressToString(rawData: ByteArray) : String {
        return StringBuilder().apply {
            rawData.forEachIndexed { index, byte ->
                append(Converters.getIntFromTwosComplement(byte))
                if (index != rawData.size - 1) append('.')
            }
        }.toString()
    }

}