package com.siliconlabs.bledemo.features.demo.matter_demo.evse.domain.usecase

import com.siliconlabs.bledemo.features.demo.matter_demo.evse.domain.entity.ChargingMode
import com.siliconlabs.bledemo.features.demo.matter_demo.evse.domain.repo.EVRepository
import javax.inject.Inject

class SetChargingModeUseCase @Inject constructor(
    private val repo: EVRepository
) {
    suspend operator fun invoke(mode: ChargingMode) = repo.setMode(mode)
}
