package com.siliconlabs.bledemo.features.demo.energyharvesting.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.siliconlabs.bledemo.features.demo.energyharvesting.data.PersistentVoltageLogger
import com.siliconlabs.bledemo.features.demo.energyharvesting.domain.usecase.GetDeviceStatusUseCase

class EnergyVmFactory(
    private val getDeviceStatus: GetDeviceStatusUseCase,
    private val voltageLogger: PersistentVoltageLogger
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EnergyViewModel(getDeviceStatus, voltageLogger) as T
    }
}
