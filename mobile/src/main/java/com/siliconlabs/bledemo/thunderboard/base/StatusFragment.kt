package com.siliconlabs.bledemo.thunderboard.base

import android.animation.AnimatorInflater
import android.app.AlertDialog
import android.app.Fragment
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.NavUtils
import butterknife.BindView
import butterknife.ButterKnife
import com.siliconlabs.bledemo.Bluetooth.Services.BluetoothService
import com.siliconlabs.bledemo.Bluetooth.Services.ThunderboardActivityCallback
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.thunderboard.model.ThunderBoardDevice
import com.siliconlabs.bledemo.thunderboard.ui.BatteryIndicator
import timber.log.Timber
import javax.inject.Inject

class StatusFragment : Fragment(), StatusViewListener, ThunderboardActivityCallback {
    @Inject
    lateinit var presenter: StatusPresenter

    @BindView(R.id.battery_indicator)
    lateinit var batteryIndicator: BatteryIndicator

    @BindView(R.id.device_status)
    lateinit var deviceStatus: TextView

    @BindView(R.id.device_name)
    lateinit var deviceName: TextView

    @BindView(R.id.device_firmware)
    lateinit var deviceFirmware: TextView

    @BindView(R.id.progress_bar)
    lateinit var progressBar: ProgressBar


    private lateinit var rootView: View
    private var bluetoothService: BluetoothService? = null
    private var isConnecting = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        presenter = StatusPresenter(bluetoothService)
        rootView = inflater.inflate(R.layout.fragment_device_status, container, false)
        ButterKnife.bind(this, rootView)
        batteryIndicator.visibility = View.INVISIBLE
        batteryIndicator.setBatteryValue(ThunderBoardDevice.PowerSource.UNKNOWN, 0)
        return rootView
    }

    override fun onPause() {
        presenter.clearViewListener()
        super.onPause()
    }

    fun setBluetoothService(service: BluetoothService?) {
        bluetoothService = service
    }

    override fun onPrepared() {
        presenter.setBluetoothService(bluetoothService)
        presenter.setViewListener(this)
    }

    fun disableHeartbeatTimer() {
        presenter.disableHeartbeatTimer()
    }

    // StatusViewListener
    override fun onData(device: ThunderBoardDevice?) {
        Timber.d("name: %s, state: %d", device?.name, device?.state)
        deviceName.text = device?.name
        if (device?.firmwareVersion == null || device.firmwareVersion?.isEmpty()!!) {
            deviceFirmware.text = getString(R.string.status_no_firmware_version)
        } else {
            deviceFirmware.setText(device.firmwareVersion)
        }
        val resourceId: Int
        when (device?.state) {
            BluetoothProfile.STATE_CONNECTED -> {
                resourceId = R.string.status_connected
                isConnecting = false
                batteryIndicator.visibility = View.VISIBLE
                progressBar.visibility = View.INVISIBLE
            }
            BluetoothProfile.STATE_CONNECTING -> {
                resourceId = R.string.status_connecting
                isConnecting = true
                batteryIndicator.visibility = View.INVISIBLE
                progressBar.visibility = View.VISIBLE
            }
            BluetoothProfile.STATE_DISCONNECTING -> {
                resourceId = BluetoothProfile.STATE_DISCONNECTING
                isConnecting = false
                batteryIndicator.visibility = View.INVISIBLE
                progressBar.visibility = View.VISIBLE
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
                batteryIndicator.visibility = View.INVISIBLE
                progressBar.visibility = View.VISIBLE
                animateDown()
                showNotConnectedDialog(device?.name, titleId, messageId)
                (activity as BaseActivity).onDisconnected()
            }
        }
        deviceStatus.text = getString(resourceId)
        batteryIndicator.setBatteryValue(device?.powerSource!!, device.batteryLevel)
    }

    private fun showNotConnectedDialog(deviceName: String?, titleId: Int, messageId: Int) {
        val builder = AlertDialog.Builder(activity)
        builder.setCancelable(false)
                .setMessage(String.format(getString(messageId), deviceName))
                .setTitle(titleId)
                .setPositiveButton(R.string.ok) { dialogInterface, which ->
                    val intent = NavUtils.getParentActivityIntent(activity)
                    intent!!.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    NavUtils.navigateUpTo(activity, intent)
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