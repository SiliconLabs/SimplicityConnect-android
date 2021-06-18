package com.siliconlabs.bledemo.advertiser.presenters

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.os.Build
import android.os.Handler
import com.siliconlabs.bledemo.advertiser.activities.IAdvertiserActivityView
import com.siliconlabs.bledemo.advertiser.models.Advertiser
import com.siliconlabs.bledemo.advertiser.models.BluetoothInfo
import com.siliconlabs.bledemo.advertiser.utils.AdvertiserStorage
import com.siliconlabs.bledemo.bluetooth.ble.ErrorCodes

class AdvertiserActivityPresenter(private val view: IAdvertiserActivityView, private val storage: AdvertiserStorage, private val advertiserItems: ArrayList<Advertiser>) : IAdvertiserActivityPresenter {
    private val handler = Handler()

    private fun hideDetailsView(list: List<Advertiser>) {
        for (item in list) item.displayDetailsView = false
    }

    override fun populateAdvertiserAdapter() {
        hideDetailsView(advertiserItems)
        view.onAdvertiserPopulated(advertiserItems)
    }

    override fun copyItem(item: Advertiser) {
        advertiserItems.add(item.deepCopy())
        view.onCopyClicked(advertiserItems.size - 1)
    }

    override fun createNewItem() {
        advertiserItems.add(Advertiser())
        view.onItemCreated(advertiserItems.size - 1)
    }

    override fun switchItemOn(position: Int) {
        val item = advertiserItems[position]

        if (!item.isRunning) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                item.start(object : AdvertisingSetCallback() {
                    override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet?, txPower: Int, status: Int) {
                        if (status == ADVERTISE_SUCCESS) {
                            item.isRunning = true
                            item.runnable = getAdvertiserRunnable(item)
                            if (item.data.limitType.isTimeLimit() || item.data.limitType.isEventLimit()) handler.postDelayed(item.runnable, item.data.getAdvertisingTime())
                            if (item.data.getAdvertisingTime() > 1000 || item.data.limitType.isNoLimit()) view.startAdvertiserService()
                            item.data.txPower = txPower
                        } else {
                            view.showMessage(ErrorCodes.getAdvertiserErrorMessage(status))
                        }
                        view.refreshItem(position)
                    }

                }, object : Advertiser.ErrorCallback {
                    override fun onErrorHandled(message: String) {
                        view.showMessage("Error: ".plus(message))
                        view.refreshItem(position)
                    }
                })
            } else {
                item.startLowApi(object : AdvertiseCallback() {
                    override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                        item.isRunning = true
                        item.runnable = getAdvertiserRunnable(item)
                        if (item.data.limitType.isTimeLimit()) handler.postDelayed(item.runnable, item.data.getAdvertisingTime())
                        if (item.data.getAdvertisingTime() > 1000 || item.data.limitType.isNoLimit()) view.startAdvertiserService()
                        item.data.setEffectiveTxPowerLowApi(settingsInEffect?.txPowerLevel)
                        view.refreshItem(position)
                    }

                    override fun onStartFailure(errorCode: Int) {
                        super.onStartFailure(errorCode)
                        view.showMessage(ErrorCodes.getAdvertiserErrorMessage(errorCode))
                        view.refreshItem(position)
                    }
                }, object : Advertiser.ErrorCallback {
                    override fun onErrorHandled(message: String) {
                        view.showMessage("Error: ".plus(message))
                        view.refreshItem(position)
                    }
                })
            }
        }
    }

    override fun editItem(position: Int) {
        stopAdvertiserItem(position)
        view.refreshItem(position)
        view.showEditIntent(advertiserItems[position])
    }

    override fun removeItem(position: Int) {
        stopAdvertiserItem(position)
        advertiserItems.remove(advertiserItems[position])
        view.onItemRemoved(position)
    }

    override fun switchItemOff(position: Int) {
        stopAdvertiserItem(position)
        view.refreshItem(position)
    }

    override fun switchAllItemsOff() {
        for (item in advertiserItems) {
            if (item.isRunning) {
                item.stop()
                if (item.isRunnableInitialized()) handler.removeCallbacks(item.runnable)
                view.refreshItem(advertiserItems.indexOf(item))
            }
        }
        view.stopAdvertiserService()
    }

    override fun persistData() {
        storage.storeAdvertiserList(advertiserItems)
    }

    override fun checkExtendedAdvertisingSupported() {
        if (!storage.isAdvertisingBluetoothInfoChecked()) {
            val bluetoothInfo = BluetoothInfo()
            storage.setAdvertisingExtensionSupported(bluetoothInfo.isExtendedAdvertisingSupported())
            storage.setLe2MPhySupported(bluetoothInfo.isLe2MPhySupported())
            storage.setLeCodedPhySupported(bluetoothInfo.isLeCodedPhySupported())
            storage.setLeMaximumDataLength(bluetoothInfo.getLeMaximumAdvertisingDataLength())
        }
    }

    private fun stopAdvertiserItem(position: Int) {
        val item = advertiserItems[position]
        item.stop()
        if (item.isRunnableInitialized()) handler.removeCallbacks(item.runnable)
        if (!isAnyAdvertiserRunning()) view.stopAdvertiserService()
    }

    private fun isAnyAdvertiserRunning(): Boolean {
        for (item in advertiserItems) if (item.isRunning) return true
        return false
    }

    private fun getAdvertiserRunnable(item: Advertiser): Runnable {
        return Runnable {
            item.stop()
            if (item.isRunnableInitialized()) handler.removeCallbacks(item.runnable)
            if (!isAnyAdvertiserRunning()) view.stopAdvertiserService()
            persistData()
            view.refreshItem(advertiserItems.indexOf(item))
        }
    }
}