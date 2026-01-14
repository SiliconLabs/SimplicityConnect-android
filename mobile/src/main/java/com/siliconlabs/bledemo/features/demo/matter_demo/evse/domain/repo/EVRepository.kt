package com.siliconlabs.bledemo.features.demo.matter_demo.evse.domain.repo

import com.siliconlabs.bledemo.features.demo.matter_demo.evse.domain.entity.ChargingMode
import com.siliconlabs.bledemo.features.demo.matter_demo.evse.domain.entity.EVStatus
import kotlinx.coroutines.flow.Flow

interface EVRepository {
    /** Emits live EV status (battery %, connection, mode, remaining time). */
    fun observeStatus(): Flow<EVStatus>

    /** Persist/propagate selected charging mode. */
    suspend fun setMode(mode: ChargingMode)
}
