package com.siliconlabs.bledemo.features.demo.energyharvesting.domain.usecase

import com.siliconlabs.bledemo.features.demo.energyharvesting.domain.model.DeviceStatus
import com.siliconlabs.bledemo.features.demo.energyharvesting.domain.repository.EnergyRepository

class GetDeviceStatusUseCase(private val repo: EnergyRepository) {
    suspend operator fun invoke(): DeviceStatus = repo.getDeviceStatus()
}
