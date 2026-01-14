package com.siliconlabs.bledemo.features.demo.matter_demo.evse.data

import com.siliconlabs.bledemo.features.demo.matter_demo.evse.domain.entity.ChargingMode
import com.siliconlabs.bledemo.features.demo.matter_demo.evse.domain.entity.EVStatus
import com.siliconlabs.bledemo.features.demo.matter_demo.evse.domain.repo.EVRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min

class EVRepositoryImpl : EVRepository {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val state = MutableStateFlow(
        EVStatus(
            batteryPercent = 26,
            isConnected = true,
            mode = ChargingMode.Manual,
            remainingSeconds = 45 * 60 // demo 45 min
        )
    )

    init {
        // Fake battery/timer ticker
        scope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(1000)
                state.update {
                    val nextSec = (it.remainingSeconds - 1).coerceAtLeast(0)
                    val nextPct = min(100, it.batteryPercent + if (it.isConnected) 1 else 0)
                    it.copy(batteryPercent = nextPct, remainingSeconds = nextSec)
                }
            }
        }
    }

    override fun observeStatus() = state.asStateFlow()

    override suspend fun setMode(mode: ChargingMode) {
        state.update { it.copy(mode = mode) }
    }
}
