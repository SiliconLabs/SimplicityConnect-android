package com.siliconlabs.bledemo.features.configure.gatt_configurator.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.GattServer
import com.siliconlabs.bledemo.features.configure.gatt_configurator.models.Service

class GattServerViewModel : ViewModel() {
    private val _insertedPosition: MutableLiveData<Int> = MutableLiveData()
    val insertedPosition = _insertedPosition
    private val _removedPosition: MutableLiveData<Int> = MutableLiveData()
    val removedPosition: LiveData<Int> = _removedPosition
    private val _changedPosition: MutableLiveData<Int> = MutableLiveData()
    val changedPosition: LiveData<Int> = _changedPosition
    private val _validation: MutableLiveData<Validation> = MutableLiveData()
    val validation: LiveData<Validation> = _validation

    private var gattServer: GattServer? = null
    private var position: Int? = null

    fun init(position: Int, gattServer: GattServer) {
        this.gattServer = gattServer
        this.position = position
    }

    fun getPosition(): Int? {
        return position
    }

    fun getGattServer(): GattServer? {
        return gattServer
    }

    fun getGattServerName(): String? {
        return gattServer?.name
    }

    fun setGattServerName(name: String) {
        gattServer?.name = name
    }

    fun validateGattServer(gattServerName: String) {
        if(gattServerName.isEmpty()) {
            _validation.value = Validation.INVALID_NAME
        } else {
            _validation.value = Validation.VALID
        }
    }

    fun getServiceList(): ArrayList<Service>? {
        return gattServer?.services
    }

    fun addService(service: Service) {
        gattServer?.apply {
            services.add(service)
            _insertedPosition.value = services.size - 1
        }
    }

    fun removeServiceAt(position: Int) {
        gattServer?.apply {
            services.removeAt(position)
            _removedPosition.value = position
        }
    }

    fun copyService(service: Service) {
        gattServer?.apply {
            services.add(service.deepCopy())
            _insertedPosition.value = services.size - 1
        }
    }

    enum class Validation {
        VALID,
        INVALID_NAME
    }
}