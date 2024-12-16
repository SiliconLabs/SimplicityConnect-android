package com.siliconlabs.bledemo.features.configure.advertiser.viewmodels

import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.*
import com.siliconlabs.bledemo.features.configure.advertiser.models.Advertiser
import com.siliconlabs.bledemo.features.configure.advertiser.models.AdvertiserData
import com.siliconlabs.bledemo.features.configure.advertiser.models.BluetoothInfo
import com.siliconlabs.bledemo.features.configure.advertiser.utils.AdvertiserStorage
import com.siliconlabs.bledemo.bluetooth.ble.ErrorCodes

class AdvertiserViewModel(private val advertiserStorage: AdvertiserStorage) : ViewModel() {

    private val _advertisers: MutableLiveData<ArrayList<Advertiser>> = MutableLiveData(advertiserStorage.loadAdvertiserList())
    val advertisers: LiveData<ArrayList<Advertiser>> = _advertisers

    private val _insertedPosition: MutableLiveData<Int> = MutableLiveData()
    val insertedPosition: LiveData<Int> = _insertedPosition
    private val _removedPosition: MutableLiveData<Int> = MutableLiveData()
    val removedPosition: LiveData<Int> = _removedPosition
    private val _changedPosition: MutableLiveData<Int> = MutableLiveData()
    val changedPosition = _changedPosition

    private val _areAnyAdvertisers: MutableLiveData<Boolean> = MutableLiveData()
    val areAnyAdvertisers: LiveData<Boolean> = _areAnyAdvertisers
    private val _areAnyAdvertisersOn: MutableLiveData<Boolean> = MutableLiveData()
    val areAnyAdvertisersOn: LiveData<Boolean> = _areAnyAdvertisersOn

    private val _errorMessage: MutableLiveData<String> = MutableLiveData()
    val errorMessage: LiveData<String> = _errorMessage

    init {
        areAnyAdvertisers()
        checkExtendedAdvertisingSupported()
    }

    private fun areAnyAdvertisers() {
        _areAnyAdvertisers.value = _advertisers.value?.size!! > 0
    }

    fun createAdvertiser() {
        _advertisers.value?.apply {
            add(Advertiser())
            _insertedPosition.value = size - 1
        }
        areAnyAdvertisers()
        saveAdvertiserList()
    }

    fun copyAdvertiser(advertiser: Advertiser) {
        _advertisers.value?.apply {
            add(advertiser)
            _insertedPosition.value = size - 1
        }
        saveAdvertiserList()
    }

    fun removeAdvertiserAt(position: Int) {
        _advertisers.value?.apply {
            stopAdvertiserItem(position)
            removeAt(position)
            _removedPosition.value = position
        }
        areAnyAdvertisers()
        saveAdvertiserList()
    }

    fun updateAdvertiser(position: Int, newData: AdvertiserData) {
        _advertisers.value?.get(position)?.apply {
            data = newData
            _changedPosition.value = position
        }
        saveAdvertiserList()
    }

    fun switchItemOn(position: Int) {
        _advertisers.value?.get(position)?.apply {
            if (!isRunning) {
                start(object : AdvertisingSetCallback() {
                    override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet?, txPower: Int, status: Int) {
                        if (status == ADVERTISE_SUCCESS) {
                            isRunning = true
                            runnable = getAdvertiserRunnable(position)
                            data.txPower = txPower

                            if (data.limitType.isTimeLimit() || data.limitType.isEventLimit()) {
                                Handler(Looper.getMainLooper()).postDelayed(runnable, data.getAdvertisingTime())
                            }
                            if (data.getAdvertisingTime() > 1000 || data.limitType.isNoLimit()) {
                                _areAnyAdvertisersOn.value = true
                            }

                            saveAdvertiserList()
                        } else {
                            _errorMessage.value = ErrorCodes.getAdvertiserErrorMessage(status)
                        }
                        _changedPosition.value = position
                    }

                }, object : Advertiser.ErrorCallback {
                    override fun onErrorHandled(message: String) {
                        _errorMessage.value = "Error: ".plus(message)
                        _changedPosition.value = position
                    }
                })
            }
        }
    }

    fun switchItemOff(position: Int) {
        stopAdvertiserItem(position)
        saveAdvertiserList()
    }

    fun switchAllItemsOff() {
        _advertisers.value?.forEachIndexed { index, advertiser ->
            //if (advertiser.isRunning) {
                stopAdvertiserItem(index)
            //}
        }
        saveAdvertiserList()
    }

    private fun stopAdvertiserItem(position: Int) {
        _advertisers.value?.get(position)?.apply {
            stop()
            if (isRunnableInitialized()) Handler(Looper.getMainLooper()).removeCallbacks(runnable)
            _changedPosition.value = position
            _areAnyAdvertisersOn.value = _advertisers.value?.any { it.isRunning } ?: false
        }
    }

    private fun getAdvertiserRunnable(position: Int): Runnable {
        return Runnable {
            stopAdvertiserItem(position)
            saveAdvertiserList()
            _changedPosition.value = position
        }
    }

    private fun checkExtendedAdvertisingSupported() {
        if (!advertiserStorage.isAdvertisingBluetoothInfoChecked()) {
            val bluetoothInfo = BluetoothInfo()
            advertiserStorage.setAdvertisingExtensionSupported(bluetoothInfo.isExtendedAdvertisingSupported())
            advertiserStorage.setLe2MPhySupported(bluetoothInfo.isLe2MPhySupported())
            advertiserStorage.setLeCodedPhySupported(bluetoothInfo.isLeCodedPhySupported())
            advertiserStorage.setLeMaximumDataLength(bluetoothInfo.getLeMaximumAdvertisingDataLength())
        }
    }


    fun saveAdvertiserList() {
        _advertisers.value?.apply {
            advertiserStorage.storeAdvertiserList(this)
        }
    }


    class Factory(private val advertiserStorage: AdvertiserStorage) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AdvertiserViewModel(advertiserStorage) as T
        }
    }
}