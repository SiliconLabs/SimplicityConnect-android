package com.siliconlabs.bledemo.thunderboard.base

import android.bluetooth.BluetoothProfile
import android.os.CountDownTimer
import com.siliconlabs.bledemo.Bluetooth.Services.BluetoothService
import com.siliconlabs.bledemo.thunderboard.model.StatusEvent
import com.siliconlabs.bledemo.thunderboard.model.ThunderBoardDevice
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.BehaviorSubject
import timber.log.Timber

class StatusPresenter(private var bluetoothService: BluetoothService?) {
    private var viewListener: StatusViewListener? = null
    private var statusMonitor: BehaviorSubject<StatusEvent>? = null
    private var statusSubscriber: Subscriber<StatusEvent>? = null
    private var isConnectivityLost = false
    private var device: ThunderBoardDevice? = null

    fun setBluetoothService(service: BluetoothService?) {
        bluetoothService = service
    }

    fun setViewListener(viewListener: StatusViewListener?) {
        this.viewListener = viewListener
        subscribe()
    }

    fun clearViewListener() {
        unsubscribe()
        viewListener = null
    }

    private fun subscribe() {
        Timber.d(javaClass.simpleName)
        isConnectivityLost = true
        connectivityHeartbeatTimer.start()
        statusSubscriber = onStatusEvent()
        statusMonitor = bluetoothService!!.selectedDeviceStatusMonitor
        statusMonitor!!
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(statusSubscriber)
    }

    private fun unsubscribe() {
        Timber.d(javaClass.simpleName)
        if (statusSubscriber != null && !statusSubscriber!!.isUnsubscribed) {
            statusSubscriber!!.unsubscribe()
        }
        statusSubscriber = null
        device = null
        connectivityHeartbeatTimer.cancel()
    }

    private fun onStatusEvent(): Subscriber<StatusEvent> {
        return object : Subscriber<StatusEvent>() {
            override fun onCompleted() {
                Timber.d("completed")
            }

            override fun onError(e: Throwable) {
                Timber.d("error: %s", e.message)
            }

            override fun onNext(event: StatusEvent) {
                device = event.device
                val deviceState = device?.state
                Timber.d("device: %s, state: %d", device?.name, deviceState)
                if (BluetoothProfile.STATE_DISCONNECTED == deviceState) {
                    // we are done, notify the UI
                    connectivityHeartbeatTimer.cancel()
                    if (isConnectivityLost) {
                        isConnectivityLost = false
                        viewListener!!.onData(device)
                    }
                } else if (BluetoothProfile.STATE_CONNECTED == deviceState && device!!.isServicesDiscovered == true) {
                    // good stuff, we have a status event and the services
                    isConnectivityLost = false
                    viewListener!!.onData(device)
                } else if (BluetoothProfile.STATE_CONNECTING == deviceState) {
                    viewListener!!.onData(device)
                }
            }
        }
    }

    /*
    The OS Bluetooth connection times out after 30 seconds. Cannot make it shorter.
    We need to timeout after ~10 seconds (which is how often battery change is triggered).
    #1: onResume, start a 10 seconds countdown.
    #2: onFinish check if the connection is present. In our case the connection state is
        determined by the state of the device, the state of discovered services and it is updated
        during onNext of the device state subscriber.
     */
    fun disableHeartbeatTimer() {
        connectivityHeartbeatTimer.cancel()
    }

    private val connectivityHeartbeatTimer: CountDownTimer = object : CountDownTimer(10500, 20000) {
        override fun onTick(millisUntilFinished: Long) {
            // n/a
        }

        override fun onFinish() {
            if (isConnectivityLost) {
                // not good, did not receive a status event for a while
                if (device != null) {
                    device?.state = BluetoothProfile.STATE_DISCONNECTED
                    viewListener!!.onData(device)
                    isConnectivityLost = false
                }
            } else {
                // set another cycle during which the below should be cleared
                isConnectivityLost = true
                start()
            }
        }
    }
}