package com.siliconlabs.bledemo.gatt_configurator.viewmodels

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.siliconlabs.bledemo.gatt_configurator.models.GattServer
import com.siliconlabs.bledemo.gatt_configurator.utils.GattConfiguratorStorage

class GattConfiguratorViewModel @ViewModelInject constructor(val gattConfiguratorStorage: GattConfiguratorStorage, @Assisted private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val _gattServers: MutableLiveData<ArrayList<GattServer>> = MutableLiveData(gattConfiguratorStorage.loadGattServerList())
    val gattServers: LiveData<ArrayList<GattServer>> = _gattServers
    private val _removedPosition: MutableLiveData<Int> = MutableLiveData()
    val removedPosition: LiveData<Int> = _removedPosition
    private val _insertedPosition: MutableLiveData<Int> = MutableLiveData()
    val insertedPosition: LiveData<Int> = _insertedPosition
    private val _changedPosition: MutableLiveData<Int> = MutableLiveData()
    val changedPosition = _changedPosition
    private val _areAnyGattServers: MutableLiveData<Boolean> = MutableLiveData()
    val areAnyGattServers = _areAnyGattServers
    private val _switchedOffPosition: MutableLiveData<Int> = MutableLiveData()
    val switchedOffPosition: LiveData<Int> = _switchedOffPosition

    init {
        areAnyGattServers()
    }

    private fun areAnyGattServers() {
        _areAnyGattServers.value = _gattServers.value?.size!! > 0
    }

    fun createGattServer() {
        _gattServers.value?.apply {
            add(GattServer("New GATT server"))
            _insertedPosition.value = size - 1
        }
        areAnyGattServers()
    }

    fun copyGattServer(gattServer: GattServer) {
        _gattServers.value?.apply {
            add(gattServer.deepCopy())
            _insertedPosition.value = size - 1
        }
    }

    fun removeGattServerAt(position: Int) {
        _gattServers.value?.apply {
            removeAt(position)
            _removedPosition.value = position
        }
        areAnyGattServers()
    }

    fun replaceGattServerAt(position: Int, gattServer: GattServer) {
        _gattServers.value?.apply {
            set(position, gattServer)
            _changedPosition.value = position
        }
    }

    fun switchGattServerOnAt(position: Int) {
        _gattServers.value?.let { servers ->
            for (i in 0 until servers.size) {
                val gattServer = servers[i]
                if (gattServer.isSwitchedOn) {
                    gattServer.isSwitchedOn = false
                    _switchedOffPosition.value = i
                }
            }
            servers[position].isSwitchedOn = true
            gattConfiguratorStorage.saveActiveGattServer(servers[position])
        }
    }

    fun switchGattServerOffAt(position: Int) {
        _gattServers.value?.let { servers ->
            servers[position].isSwitchedOn = false
            _switchedOffPosition.value = position
            if (!isAnyGattServerSwitchedOn()) {
                gattConfiguratorStorage.saveActiveGattServer(null)
            }
        }
    }

    fun isAnyGattServerSwitchedOn(): Boolean {
        _gattServers.value?.let { servers ->
            return servers.any { it.isSwitchedOn }
        }
        return false
    }

    fun persistGattServers() {
        _gattServers.value?.apply {
            gattConfiguratorStorage.saveGattServerList(this)
        }
    }
}