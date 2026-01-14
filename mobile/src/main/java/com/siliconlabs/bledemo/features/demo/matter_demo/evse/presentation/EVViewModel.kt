package com.siliconlabs.bledemo.features.demo.matter_demo.evse.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chip.devicecontroller.ChipClusters
import com.siliconlabs.bledemo.features.demo.matter_demo.evse.domain.entity.ChargingMode
import com.siliconlabs.bledemo.features.demo.matter_demo.evse.domain.entity.EVStatus
import com.siliconlabs.bledemo.features.demo.matter_demo.evse.domain.usecase.ObserveEVStatusUseCase
import com.siliconlabs.bledemo.features.demo.matter_demo.evse.domain.usecase.SetChargingModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.system.measureTimeMillis

@HiltViewModel
class EVViewModel @Inject constructor(
    observe: ObserveEVStatusUseCase,
    private val setMode: SetChargingModeUseCase
) : ViewModel() {

    private val _ui = MutableStateFlow(EVUiState())
    val ui: StateFlow<EVUiState> = _ui.asStateFlow()

    // Flag & job tracking for live StateOfCharge observation so we can avoid duplicate loops
    @Volatile private var observingStateOfCharge: Boolean = false
    private var socJob: Job? = null
    @Volatile private var observingChargingState: Boolean = false
    private var chargingStateJob: Job? = null
    private var watchdogJob: Job? = null
    private val pollDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val chargingStateLabels = mapOf(
        0 to "Not Plugged In",
        1 to "Plugged (No Demand)",
        2 to "Plugged (Demand)",
        3 to "Charging",
        4 to "Discharging",
        5 to "Session Ending",
        6 to "Fault"
    )

    // --- Offline detection variables ---
    private val inactivityThresholdMs = 4_000L // mark offline if no successful attribute read within window
    private val consecutiveFailureThreshold = 2 // or earlier if we hit this many consecutive failures quickly

    @Volatile private var lastAnySuccessTimestamp: Long = System.currentTimeMillis()
    @Volatile private var consecutiveFailures: Int = 0
    @Volatile private var offlineMarked: Boolean = false

    init {
        // Collect repository demo status (connectivity / timer / legacy mode). Battery percent is
        // overridden by live SoC once cluster observation begins; until then we use repo value.
        viewModelScope.launch {
            observe().collect { s: EVStatus ->
                _ui.update { current ->
                    current.copy(
                        percent = if (observingStateOfCharge) current.percent else s.batteryPercent,
                        connected = if (observingChargingState) current.connected else s.isConnected,
                        mode = s.mode,
                        remainingSeconds = s.remainingSeconds
                    )
                }
            }
        }
        startWatchdog()
    }

    private fun startWatchdog() {
        if (watchdogJob?.isActive == true) return
        watchdogJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                if (!offlineMarked) {
                    val now = System.currentTimeMillis()
                    val inactivity = now - lastAnySuccessTimestamp
                    if ((inactivity > inactivityThresholdMs && consecutiveFailures > 0) ||
                        (consecutiveFailures >= consecutiveFailureThreshold)) {
                        markOfflineInternal(reason = buildString {
                            append("No response ")
                            append(inactivity).append("ms; failures=").append(consecutiveFailures)
                        })
                    }
                }
                delay(500L)
            }
        }
    }

    fun onChargingModeClick() { _ui.update { it.copy(showModePicker = true) } }
    fun dismissPicker() { _ui.update { it.copy(showModePicker = false) } }
    // Legacy semantic enum selection (kept if other code paths still use it)
    fun selectMode(mode: ChargingMode) { viewModelScope.launch { setMode(mode) }; _ui.update { it.copy(showModePicker = false, mode = mode) } }
    // New numeric mode selection
    fun selectModeId(modeId: Int) { _ui.update { it.copy(currentModeId = modeId, showModePicker = false) } }
    // Called when reading current mode from device (initial / refresh) without altering sheet visibility
    fun updateCurrentModeFromDevice(modeId: Int) { _ui.update { it.copy(currentModeId = modeId) } }
    // Update dynamic labels mapped strictly by numeric device mode IDs.
    fun setSupportedModeLabels(labels: Map<Int, String>) { viewModelScope.launch { _ui.update { cur -> val cid = cur.currentModeId; val adj = if (cid==null || cid !in labels) labels.keys.firstOrNull() else cid; cur.copy(modeLabels = labels, currentModeId = adj) } } }
    // New: update vehicle ID when retrieved from cluster
    fun setVehicleId(vehicleId: String?) { _ui.update { it.copy(vehicleId = vehicleId) } }

    /** Public entry point from Fragment to begin continuous observation of StateOfCharge attribute. */
    fun startObservingStateOfCharge(cluster: ChipClusters.EnergyEvseCluster) {
        if (observingStateOfCharge || offlineMarked) return // already running or offline
        observingStateOfCharge = true
        socJob = viewModelScope.launch(pollDispatcher) {
            Timber.i("Starting StateOfCharge observation loop")
            while (isActive && !offlineMarked) {
                val value = readStateOfChargeOnce(cluster)
                if (value != null) { _ui.update { it.copy(percent = value.coerceIn(0, 100)) }; recordSuccess() } else recordFailure("StateOfCharge")
                delay(1500L)
            }
        }
    }

    /** Public entry point from Fragment to begin continuous observation of ChargingState attribute. */
    fun startObservingChargingState(cluster: ChipClusters.EnergyEvseCluster) {
        if (observingChargingState || offlineMarked) return // already running or offline
        observingChargingState = true
        chargingStateJob = viewModelScope.launch(pollDispatcher) {
            Timber.i("Starting ChargingState observation loop")
            while (isActive && !offlineMarked) {
                val code = readChargingStateOnce(cluster)
                if (code != null) {
                    val label = chargingStateLabels[code] ?: "Unknown ($code)"
                    _ui.update { it.copy(chargingStateCode = code, chargingStateLabel = label, connected = code != 0) }
                    recordSuccess()
                } else recordFailure("ChargingState")
                delay(2000L)
            }
        }
    }

    private fun recordSuccess() {
        lastAnySuccessTimestamp = System.currentTimeMillis()
        consecutiveFailures = 0
        // If we previously marked offline but now succeed (rare before navigation), clear flag.
        if (offlineMarked) {
            // We will not auto-clear UI offline state because Fragment navigates away; retain info.
        }
    }

    private fun recordFailure(attr: String) {
        consecutiveFailures++
        Timber.w("Failure reading attribute $attr; consecutiveFailures=$consecutiveFailures")
        _ui.update { it.copy(lastError = "Fail $attr (${consecutiveFailures})") }
    }

    private fun markOfflineInternal(reason: String?) {
        if (offlineMarked) return
        offlineMarked = true
        Timber.w("Marking EV device offline: $reason")
        _ui.update { it.copy(isOffline = true, lastError = reason ?: it.lastError) }
        // Cancel jobs to avoid further work
        socJob?.cancel()
        chargingStateJob?.cancel()
    }

    /** Attempt a single async read of StateOfCharge attribute, returning null on error. */
    private suspend fun readStateOfChargeOnce(cluster: ChipClusters.EnergyEvseCluster): Int? = withContext(pollDispatcher) {
        suspendCancellableCoroutine { cont ->
            try {
                cluster.readStateOfChargeAttribute(object : ChipClusters.EnergyEvseCluster.StateOfChargeAttributeCallback {
                    override fun onSuccess(value: Int?) { Timber.v("StateOfCharge read success: $value"); if (cont.isActive) cont.resume(value) }
                    override fun onError(error: Exception?) { Timber.e(error, "StateOfCharge read failed"); if (cont.isActive) cont.resume(null) }
                })
            } catch (e: Exception) { Timber.e(e, "Exception invoking readStateOfChargeAttribute"); if (cont.isActive) cont.resume(null) }
        }
    }

    /** Attempt a single async read of ChargingState attribute, returning null on error. */
    private suspend fun readChargingStateOnce(cluster: ChipClusters.EnergyEvseCluster): Int? = withContext(pollDispatcher) {
        suspendCancellableCoroutine { cont ->
            try {
                cluster.readStateAttribute(object : ChipClusters.EnergyEvseCluster.StateAttributeCallback {
                    override fun onSuccess(value: Int?) { Timber.v("ChargingState read success: $value"); if (cont.isActive) cont.resume(value) }
                    override fun onError(error: Exception?) { Timber.e(error, "ChargingState read failed"); if (cont.isActive) cont.resume(null) }
                })
            } catch (e: Exception) { Timber.e(e, "Exception invoking readStateAttribute"); if (cont.isActive) cont.resume(null) }
        }
    }

    override fun onCleared() { super.onCleared(); socJob?.cancel(); chargingStateJob?.cancel(); watchdogJob?.cancel() }
}
