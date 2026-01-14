package com.siliconlabs.bledemo.features.demo.energyharvesting.domain.usecase

import com.siliconlabs.bledemo.features.demo.energyharvesting.domain.model.SensorReading
import com.siliconlabs.bledemo.features.demo.energyharvesting.domain.repository.EnergyRepository

class GetVoltageHistoryUseCase(private val repo: EnergyRepository) {
    suspend operator fun invoke(): List<SensorReading> = repo.getVoltageHistory()
}
