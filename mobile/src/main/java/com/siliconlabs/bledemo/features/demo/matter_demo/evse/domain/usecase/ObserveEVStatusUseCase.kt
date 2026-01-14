package com.siliconlabs.bledemo.features.demo.matter_demo.evse.domain.usecase

import com.siliconlabs.bledemo.features.demo.matter_demo.evse.domain.entity.EVStatus
import com.siliconlabs.bledemo.features.demo.matter_demo.evse.domain.repo.EVRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveEVStatusUseCase @Inject constructor(
    private val repo: EVRepository
) {
    operator fun invoke(): Flow<EVStatus> = repo.observeStatus()
}
