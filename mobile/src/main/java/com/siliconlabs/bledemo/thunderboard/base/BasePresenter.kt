package com.siliconlabs.bledemo.thunderboard.base

import android.app.FragmentManager
import android.bluetooth.BluetoothProfile
import com.siliconlabs.bledemo.Bluetooth.Services.BluetoothService
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.thunderboard.model.StatusEvent
import com.siliconlabs.bledemo.thunderboard.model.ThunderBoardDevice
import com.siliconlabs.bledemo.thunderboard.sensor.SensorBase
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.BehaviorSubject

abstract class BasePresenter {
    var bluetoothService: BluetoothService? = null
    protected var deviceMonitor: BehaviorSubject<ThunderBoardDevice>? = null
    protected var deviceSubscriber: Subscriber<ThunderBoardDevice>? = null
    protected var sensor: SensorBase? = null
    protected var deviceAvailable = false
    protected var viewListener: BaseViewListener? = null


    fun prepareViewListener(viewListener: BaseViewListener?) {
        this.viewListener = viewListener
        subscribe()
    }

    fun clearViewListener() {
        unsubscribe()
        viewListener = null
    }

    protected open fun subscribe() {
        deviceSubscriber = onDevice()
        deviceMonitor = bluetoothService!!.selectedDeviceMonitor
        deviceMonitor!!
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(deviceSubscriber)
    }

    protected open fun unsubscribe() {
        if (deviceSubscriber != null && !deviceSubscriber!!.isUnsubscribed) {
            deviceSubscriber!!.unsubscribe()
        }
        deviceSubscriber = null
        sensor = null
    }

    fun loadStatusFragment(fragmentManager: FragmentManager) {
        val fragment = fragmentManager.findFragmentById(R.id.bluegecko_status_fragment)
        val thunderboardFragment = fragment as StatusFragment
        thunderboardFragment.setBluetoothService(bluetoothService)
        thunderboardFragment.onPrepared()
    }

    fun showConnectionState() {
        val bgd = bluetoothService!!.thunderboardDevice
        bgd?.state = BluetoothProfile.STATE_CONNECTED
        bluetoothService!!.selectedDeviceMonitor.onNext(bgd)
        bluetoothService!!.selectedDeviceStatusMonitor.onNext(StatusEvent(bgd))
    }

    val boardType: ThunderBoardDevice.Type
        get() = bluetoothService!!.getThunderboardType()

    protected abstract fun onDevice(): Subscriber<ThunderBoardDevice>?
}