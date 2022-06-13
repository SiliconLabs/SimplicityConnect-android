package com.siliconlabs.bledemo.thunderboard.base

import android.animation.AnimatorInflater
import android.app.AlertDialog
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NavUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.siliconlabs.bledemo.Bluetooth.BLE.GattCharacteristic
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.thunderboard.model.ThunderBoardDevice
import kotlinx.android.synthetic.main.fragment_device_status.view.*

class StatusFragment : Fragment() {

    lateinit var viewModel: StatusViewModel

    private lateinit var rootView: View
    private var isConnecting = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        viewModel = ViewModelProvider(this).get(StatusViewModel::class.java)

        setupDataListeners()

        rootView = inflater.inflate(R.layout.fragment_device_status, container, false)
        rootView.battery_indicator.visibility = View.INVISIBLE
        rootView.battery_indicator.setBatteryValue(ThunderBoardDevice.PowerSource.UNKNOWN, 0)
        return rootView
    }

    fun handleBaseCharacteristic(characteristic: BluetoothGattCharacteristic) {
        when (characteristic.uuid) {
            GattCharacteristic.DeviceName.uuid ->
                viewModel.setDeviceName(characteristic.getStringValue(0))
            GattCharacteristic.ModelNumberString.uuid ->
                viewModel.setModelNumber(characteristic.getStringValue(0))
            GattCharacteristic.BatteryLevel.uuid -> {
                viewModel.setBatteryLevel(characteristic.value[0].toInt())
            }
            GattCharacteristic.PowerSource.uuid -> {
                val powerSource = ThunderBoardDevice.PowerSource.fromInt(characteristic.value[0].toInt())
                viewModel.setPowerSource(powerSource)
            }
            GattCharacteristic.FirmwareRevision.uuid ->
                viewModel.setFirmwareVersion(characteristic.getStringValue(0))
            else -> { }
        }
    }

    private fun setupDataListeners() {
        viewModel.thunderboardDevice.observe(viewLifecycleOwner, Observer {
            updateStatusViews(it)
        })
        viewModel.state.observe(viewLifecycleOwner, Observer {
            showDeviceStatus(it)
        })
    }

    private fun updateStatusViews(device: ThunderBoardDevice) {
        rootView.apply {
            device_name.text = device.name
            device_firmware.text =
                    if (device.firmwareVersion == null || device.firmwareVersion?.isEmpty()!!) {
                        getString(R.string.status_no_firmware_version)
                    } else device.firmwareVersion
            battery_indicator.setBatteryValue(device.powerSource, device.batteryLevel)
        }
    }

    private fun showDeviceStatus(state: Int) {
        val resourceId: Int
        when (state) {
            BluetoothProfile.STATE_CONNECTED -> {
                resourceId = R.string.status_connected
                isConnecting = false
                rootView.battery_indicator.visibility = View.VISIBLE
                rootView.progress_bar.visibility = View.INVISIBLE
            }
            BluetoothProfile.STATE_CONNECTING -> {
                resourceId = R.string.status_connecting
                isConnecting = true
                rootView.battery_indicator.visibility = View.INVISIBLE
                rootView.progress_bar.visibility = View.VISIBLE
            }
            BluetoothProfile.STATE_DISCONNECTING -> {
                resourceId = BluetoothProfile.STATE_DISCONNECTING
                isConnecting = false
                rootView.battery_indicator.visibility = View.INVISIBLE
                rootView.progress_bar.visibility = View.VISIBLE
            }
            else -> {
                val titleId: Int
                val messageId: Int
                if (isConnecting) {
                    titleId = R.string.status_unable_to_connect
                    messageId = R.string.status_unable_to_connect_long
                } else {
                    titleId = R.string.status_connection_lost
                    messageId = R.string.status_connection_lost_long
                }
                resourceId = R.string.status_disconnected
                isConnecting = false
                rootView.battery_indicator.visibility = View.INVISIBLE
                rootView.progress_bar.visibility = View.VISIBLE
                animateDown()
                showNotConnectedDialog(viewModel.thunderboardDevice.value?.name, titleId, messageId)
            }
        }
        rootView.device_status.text = getString(resourceId)
    }


    private fun showNotConnectedDialog(deviceName: String?, titleId: Int, messageId: Int) {
        val builder = AlertDialog.Builder(activity)
        builder.setCancelable(false)
                .setMessage(String.format(getString(messageId), deviceName))
                .setTitle(titleId)
                .setPositiveButton(R.string.ok) { dialogInterface, which ->
                    val intent = NavUtils.getParentActivityIntent(activity as ThunderboardActivity)
                    intent!!.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    NavUtils.navigateUpTo(activity as ThunderboardActivity, intent)
                }
        val dialog = builder.create()
        dialog.show()
    }

    private fun animateDown() {
        val animator = AnimatorInflater.loadAnimator(activity, R.animator.animator_down)
        animator.setTarget(rootView)
        animator.start()
    }
}