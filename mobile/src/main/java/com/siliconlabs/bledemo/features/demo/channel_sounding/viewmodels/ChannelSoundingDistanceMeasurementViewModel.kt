package com.siliconlabs.bledemo.features.demo.channel_sounding.viewmodels

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.github.mikephil.charting.data.Entry
import com.siliconlabs.bledemo.features.demo.channel_sounding.filters.DistanceKalmanFilter
import com.siliconlabs.bledemo.features.demo.channel_sounding.interfaces.BleConnection
import com.siliconlabs.bledemo.features.demo.channel_sounding.managers.ChannelSoundingDistanceMeasurementManager
import com.siliconlabs.bledemo.features.demo.channel_sounding.models.FilteringLevel
import com.siliconlabs.bledemo.features.demo.channel_sounding.models.KalmanFilterConfig
import com.siliconlabs.bledemo.features.demo.channel_sounding.utils.ChannelSoundingConstant
import com.siliconlabs.bledemo.utils.CustomToastManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
class ChannelSoundingDistanceMeasurementViewModel(
    private val activity: Activity,
    private val bleConnection: BleConnection
) : AndroidViewModel(activity.application), BleConnection {
    data class DistanceResult(
        val distanceMeter: Double,
        val confidence: Int,
        val rawDistanceMeter: Double = distanceMeter,
        val isFiltered: Boolean = false,
        val isOutlier: Boolean = false,
        val kalmanGain: Double = 0.0,
        val mahalanobisDistance: Double = 0.0
    )

    /**
     * Sealed class representing outlier detection events for UI notification
     */
    sealed class OutlierEvent {
        data class OutlierDetected(
            val rawDistance: Double,
            val filteredDistance: Double,
            val mahalanobisDistance: Double
        ) : OutlierEvent()
    }

    /**
     * Data class representing a platform-initiated stop event.
     * Used to signal the Activity to schedule auto-restart.
     */
    data class PlatformStopEvent(
        val reason: Int,
        val isRecoverable: Boolean,
        val reasonName: String
    )

    private val sessionState =
        MutableLiveData(ChannelSoundingConstant.RangeSessionState.STOPPED)

    /**
     * Measurement state for pause/resume functionality.
     * This gates measurement callbacks without terminating the underlying session.
     * 
     * IDLE: No active measurement, ready to start fresh
     * RUNNING: Actively processing measurement callbacks
     * PAUSED: Session alive but callbacks are gated (ignored), ready to resume
     * 
     * Why this exists:
     * The Channel Sounding session is expensive to create (bonding, negotiation).
     * By using a measurement state gate, we can "pause" measurement processing
     * while keeping the session alive, enabling instant resume without reinitializing.
     */
    private val _measurementState = MutableStateFlow(ChannelSoundingConstant.MeasurementState.IDLE)
    //val measurementState: StateFlow<ChannelSoundingConstant.MeasurementState> = _measurementState

    private val distanceResult = MutableStateFlow(DistanceResult(0.0, 0))

    private val startFailureReasons = MutableLiveData<List<String>>(emptyList())

    // SharedFlow for outlier events - UI can observe this to show Toast notifications
    private val _outlierEvents = MutableSharedFlow<OutlierEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )

    // SharedFlow for platform-initiated stop events - Activity observes to schedule auto-restart
    private val _platformStopEvent = MutableSharedFlow<PlatformStopEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    
    /**
     * Heartbeat event emitted for ALL measurements (HIGH, MEDIUM, LOW) to keep Activity responsive.
     * This ensures timing state updates even when only LOW/MEDIUM confidence data is received.
     */
    data class MeasurementHeartbeat(
        val timestamp: Long,
        val confidence: Int,
        val rawDistance: Double
    )
    
    private val _measurementHeartbeat = MutableSharedFlow<MeasurementHeartbeat>(
        replay = 0,
        extraBufferCapacity = 10,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )

    private lateinit var distanceMeasurementManger: ChannelSoundingDistanceMeasurementManager

    // Store current target device for re-initialization
    private var currentTargetDevice: BluetoothDevice? = null

    // Flag to ignore onStop callbacks from old sessions during restart
    @Volatile
    private var ignoreStopCallbacks = false

    // Kalman filter for smoothing distance measurements
    private val kalmanFilter = DistanceKalmanFilter()

    // Filtering level (filtering is always enabled, level controls aggressiveness)
    private val _filteringLevel = MutableStateFlow(FilteringLevel.MEDIUM)
    val filteringLevel: StateFlow<FilteringLevel> = _filteringLevel

    // Deprecated - kept for backward compatibility, always returns true
    private val _kalmanFilterEnabled = MutableStateFlow(true)
    val kalmanFilterEnabled: StateFlow<Boolean> = _kalmanFilterEnabled

    // Stored configuration state for settings that new filter doesn't track directly
    // These are maintained for UI/config purposes but the filter handles outlier rejection internally
    private var _outlierDetectionEnabled = true
    private var _adaptiveFilteringEnabled = true
    private var _medianPreFilterEnabled = true
    private var _rateLimiterEnabled = true
    private var _emaEnabled = true
    private var _calibrationEnabled = false
    private var _distanceOffset = DistanceKalmanFilter.Defaults.distanceOffset
    private var _distanceScale = DistanceKalmanFilter.Defaults.distanceScale
    private var _outlierThreshold = DistanceKalmanFilter.Defaults.outlierThresholdPerSecond

    // ============== CHART DATA FOR RAW vs KALMAN FILTERED DISTANCE ==============
    /**
     * Data class representing a chart data point with both raw and filtered values
     */
    data class ChartDataPoint(
        val timestamp: Float,           // Time in seconds since measurement started
        val rawDistance: Float,         // Raw distance in meters
        val filteredDistance: Float     // Kalman-filtered distance in meters
    )

    // List of chart data points for UI observation (preserves all data from session start)
    private val _chartDataPoints = MutableStateFlow<List<ChartDataPoint>>(emptyList())
    val chartDataPoints: StateFlow<List<ChartDataPoint>> = _chartDataPoints

    // Time when chart data collection started
    private var chartStartTimeMs: Long = 0L

    /**
     * Add a new data point to the chart data
     * @param rawDistance Raw distance measurement in meters
     * @param filteredDistance Kalman-filtered distance in meters
     */
    private fun addChartDataPoint(rawDistance: Float, filteredDistance: Float) {
        if (chartStartTimeMs == 0L) {
            chartStartTimeMs = System.currentTimeMillis()
        }

        val elapsedSeconds = (System.currentTimeMillis() - chartStartTimeMs) / 1000f
        val newPoint = ChartDataPoint(
            timestamp = elapsedSeconds,
            rawDistance = rawDistance,
            filteredDistance = filteredDistance
        )

        val currentPoints = _chartDataPoints.value.toMutableList()
        currentPoints.add(newPoint)

        // Preserve all data points from the session (no limit)
        _chartDataPoints.value = currentPoints

        Timber.tag(TAG).v("CS Chart data point added: t=%.2f s, raw=%.3f m, filtered=%.3f m, total points=%d",
            elapsedSeconds, rawDistance, filteredDistance, _chartDataPoints.value.size)
    }

    /**
     * Clear all chart data points (call when starting a new session)
     */
    fun clearChartData() {
        _chartDataPoints.value = emptyList()
        chartStartTimeMs = 0L
        Timber.tag(TAG).d("CS Chart data cleared")
    }

    /**
     * Get raw distance entries for the chart
     */
    fun getRawDistanceEntries(): List<Entry> {
        return _chartDataPoints.value.map { Entry(it.timestamp, it.rawDistance) }
    }

    /**
     * Get Kalman filtered distance entries for the chart
     */
    fun getFilteredDistanceEntries(): List<Entry> {
        return _chartDataPoints.value.map { Entry(it.timestamp, it.filteredDistance) }
    }
    // ============== END OF CHART DATA ==============

    // ============== 5-MINUTE RAW DATA LOGGING (1 sample/second) ==============
    /**
     * Data class to store a single raw distance measurement log entry
     */
    private data class RawDistanceLogEntry(
        val sampleNumber: Int,                  // Sample number (1-10)
        val timestamp: Long,                    // Time elapsed since logging started (ms)
        val rawDistanceMeter: Double,           // Raw distance in meters
        val rawDistanceMillimeter: Double,      // Raw distance in millimeters
        val confidence: Int                     // Measurement confidence (0=LOW, 1=MEDIUM, 2=HIGH)
    )

    private val rawDistanceLog = mutableListOf<RawDistanceLogEntry>()
    private var loggingStartTime: Long = 0L
    private var lastLoggedSecond: Int = -1
    private var isLogging = false
    private val LOGGING_DURATION_SECONDS = 300 // 5 minutes
    private val SAMPLE_INTERVAL_MS = 1000L // 1 second

    /**
     * Start logging raw distance data for 5 minutes (1 sample per second)
     */
    private fun startDataLogging() {
        rawDistanceLog.clear()
        loggingStartTime = System.currentTimeMillis()
        lastLoggedSecond = -1
        isLogging = true
        Timber.tag(TAG).i("CS ========================================================================")
        Timber.tag(TAG).i("CS ============ STARTING 5-MINUTE RAW DATA LOGGING (1/sec) ==============")
        Timber.tag(TAG).i("CS ========================================================================")
        Timber.tag(TAG).i("CS Data Unit: Raw distance from BLE Channel Sounding API in METERS (m)")
        Timber.tag(TAG).i("CS Sampling: 1 sample per second for 5 minutes (300 samples total)")
        Timber.tag(TAG).i("CS Output: CSV file will be saved to phone storage")
        Timber.tag(TAG).i("CS ========================================================================")
    }

    /**
     * Log raw distance measurement once per second for 5 minutes
     */
    private fun logRawDistanceData(
        rawMeter: Double,
        confidence: Int
    ) {
        if (!isLogging) return

        val elapsed = System.currentTimeMillis() - loggingStartTime
        val currentSecond = (elapsed / SAMPLE_INTERVAL_MS).toInt()

        // Only log once per second and within 10-second window
        if (currentSecond <= LOGGING_DURATION_SECONDS - 1 && currentSecond > lastLoggedSecond) {
            lastLoggedSecond = currentSecond
            val sampleNumber = currentSecond + 1

            // Convert to millimeters
            val rawMillimeter = rawMeter * 1000.0

            val entry = RawDistanceLogEntry(
                sampleNumber = sampleNumber,
                timestamp = elapsed,
                rawDistanceMeter = rawMeter,
                rawDistanceMillimeter = rawMillimeter,
                confidence = confidence
            )
            rawDistanceLog.add(entry)

            // Log each sample in real-time
            val confidenceStr = when (confidence) {
                2 -> "HIGH"
                1 -> "MEDIUM"
                else -> "LOW"
            }

            Timber.tag(TAG).i(
                "CS SAMPLE #%02d | Time: %5dms | RAW: %7.3f m (%7.1f mm) | Confidence: %s",
                sampleNumber, elapsed, rawMeter, rawMillimeter, confidenceStr
            )

            // Check if we've collected all 300 samples (5 minutes)
            if (sampleNumber >= LOGGING_DURATION_SECONDS) {
                isLogging = false
                Timber.tag(TAG).i("CS ========================================================================")
                Timber.tag(TAG).i("CS 300 samples collected (5 minutes). Exporting to CSV file...")
                exportToCsvFile()
            }
        }
    }

    /**
     * Export the logged raw distance data to a CSV file on the phone
     */
    private fun exportToCsvFile() {
        try {
            val context = getApplication<android.app.Application>()
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                .format(java.util.Date())
            val fileName = "cs_raw_distance_$timestamp.csv"

            val file: java.io.File
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 and above, use MediaStore
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let { fileUri ->
                    resolver.openOutputStream(fileUri)?.bufferedWriter()?.use { writer ->
                        // Write CSV header
                        writer.write("sample_number,timestamp_ms,raw_distance_m,raw_distance_mm,confidence,confidence_level")
                        writer.newLine()

                        // Write data rows
                        rawDistanceLog.forEach { entry ->
                            val confidenceStr = when (entry.confidence) {
                                2 -> "HIGH"
                                1 -> "MEDIUM"
                                else -> "LOW"
                            }
                            writer.write(
                                "${entry.sampleNumber},${entry.timestamp},%.4f,%.1f,${entry.confidence},$confidenceStr"
                                    .format(entry.rawDistanceMeter, entry.rawDistanceMillimeter)
                            )
                            writer.newLine()
                        }
                    }

                    Timber.tag(TAG).i("CS ========================================================================")
                    Timber.tag(TAG).i("CS CSV FILE EXPORTED SUCCESSFULLY!")
                    Timber.tag(TAG).i("CS File name: $fileName")
                    Timber.tag(TAG).i("CS File saved to Downloads folder")
                    Timber.tag(TAG).i("CS Total samples: ${rawDistanceLog.size}")
                    Timber.tag(TAG).i("CS ========================================================================")

                    // Show Toast notification on UI thread
                    activity.runOnUiThread {
                        CustomToastManager.show(context, "CSV export complete! File saved to Downloads: $fileName")
                    }
                } ?: throw java.io.IOException("Failed to create file in Downloads")
            } else {
                // For Android 9 and below, use legacy approach
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                file = java.io.File(downloadsDir, fileName)

                file.bufferedWriter().use { writer ->
                    // Write CSV header
                    writer.write("sample_number,timestamp_ms,raw_distance_m,raw_distance_mm,confidence,confidence_level")
                    writer.newLine()

                    // Write data rows
                    rawDistanceLog.forEach { entry ->
                        val confidenceStr = when (entry.confidence) {
                            2 -> "HIGH"
                            1 -> "MEDIUM"
                            else -> "LOW"
                        }
                        writer.write(
                            "${entry.sampleNumber},${entry.timestamp},%.4f,%.1f,${entry.confidence},$confidenceStr"
                                .format(entry.rawDistanceMeter, entry.rawDistanceMillimeter)
                        )
                        writer.newLine()
                    }
                }

                Timber.tag(TAG).i("CS ========================================================================")
                Timber.tag(TAG).i("CS CSV FILE EXPORTED SUCCESSFULLY!")
                Timber.tag(TAG).i("CS File name: $fileName")
                Timber.tag(TAG).i("CS File path: ${file.absolutePath}")
                Timber.tag(TAG).i("CS Total samples: ${rawDistanceLog.size}")
                Timber.tag(TAG).i("CS ========================================================================")

                // Show Toast notification on UI thread
                activity.runOnUiThread {
                    CustomToastManager.show(context, "CSV export complete! File saved to Downloads: $fileName")
                }
            }

            // Print summary statistics
            if (rawDistanceLog.isNotEmpty()) {
                val distances = rawDistanceLog.map { it.rawDistanceMeter }
                Timber.tag(TAG).i("CS --- RAW DATA SUMMARY ---")
                Timber.tag(TAG).i("CS   Min: %.3f m (%.1f mm)", distances.minOrNull() ?: 0.0, (distances.minOrNull() ?: 0.0) * 1000)
                Timber.tag(TAG).i("CS   Max: %.3f m (%.1f mm)", distances.maxOrNull() ?: 0.0, (distances.maxOrNull() ?: 0.0) * 1000)
                Timber.tag(TAG).i("CS   Avg: %.3f m (%.1f mm)", distances.average(), distances.average() * 1000)
                Timber.tag(TAG).i("CS ========================================================================")
            }

            // Also print CSV content to logcat for easy copy
            Timber.tag(TAG).i("CS --- CSV CONTENT (for copy/paste) ---")
            Timber.tag(TAG).i("CS sample_number,timestamp_ms,raw_distance_m,raw_distance_mm,confidence,confidence_level")
            rawDistanceLog.forEach { entry ->
                val confidenceStr = when (entry.confidence) {
                    2 -> "HIGH"
                    1 -> "MEDIUM"
                    else -> "LOW"
                }
                Timber.tag(TAG).i("CS ${entry.sampleNumber},${entry.timestamp},%.4f,%.1f,${entry.confidence},$confidenceStr"
                    .format(entry.rawDistanceMeter, entry.rawDistanceMillimeter))
            }
            Timber.tag(TAG).i("CS ========================================================================")

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "CS Failed to export CSV file")
        }
    }
    // ============== END OF 5-MINUTE RAW DATA LOGGING ==============

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun setTargetDevice(targetDevice: BluetoothDevice?) {
        Timber.tag(TAG).d("CS DistanceMeasurementViewModel setTargetDevice: $targetDevice")
        currentTargetDevice = targetDevice
        if (targetDevice != null) {
            distanceMeasurementManger.setTargetDevice(targetDevice)
        }
    }

    /**
     * Reinitialize the distance measurement manager.
     * This should be called when restarting measurements after a stop to ensure clean state.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun reinitializeManager() {
        Timber.tag(TAG).d("CS DistanceMeasurementManager reinitializing... Filtering level: ${_filteringLevel.value}")
        distanceMeasurementManger = ChannelSoundingDistanceMeasurementManager(
            activity,
            bleConnection, false,
            callback
        )
        // Re-set the target device if we had one
        currentTargetDevice?.let {
            Timber.tag(TAG).d("CS Re-setting target device: ${it.address}")
            distanceMeasurementManger.setTargetDevice(it)
        }
        Timber.tag(TAG).d("CS DistanceMeasurementManager reinitialized successfully")
    }

    fun getSessionState(): LiveData<ChannelSoundingConstant.RangeSessionState> = sessionState

    /**
     * Get the current measurement state for pause/resume tracking.
     */
    fun getMeasurementState(): StateFlow<ChannelSoundingConstant.MeasurementState> = _measurementState

    /**
     * Check if measurements are currently paused.
     * When paused, the session is alive but callbacks are gated.
     */
    fun isMeasurementPaused(): Boolean = 
        _measurementState.value == ChannelSoundingConstant.MeasurementState.PAUSED

    /**
     * Pause measurement processing without terminating the Channel Sounding session.
     * 
     * Why pause instead of stop:
     * - Session and GATT connection remain alive (no teardown overhead on resume)
     * - Kalman filter state is preserved (smoother transition on resume)
     * - Auto-retry logic continues working in background
     * - Resume is instant with no reinitialization needed
     * 
     * This gates measurement callbacks in onDistanceResult() so they are ignored
     * while paused, but the underlying session keeps running.
     */
    fun pauseMeasurements() {
        val currentState = _measurementState.value
        if (currentState == ChannelSoundingConstant.MeasurementState.RUNNING) {
            Timber.tag(TAG).d("CS Pausing measurements - session remains alive, callbacks gated")
            _measurementState.value = ChannelSoundingConstant.MeasurementState.PAUSED
            // NOTE: We do NOT call distanceMeasurementManger.stop() here!
            // The session stays active, only callback processing is paused.
        } else {
            Timber.tag(TAG).w("CS Cannot pause - not in RUNNING state (current: $currentState)")
        }
    }

    /**
     * Resume measurement processing after a pause.
     * 
     * Since the session was kept alive during pause:
     * - No reinitialization needed
     * - Kalman filter state is preserved (continues from where it left off)
     * - Measurements resume instantly
     * 
     * @return true if successfully resumed, false if not in paused state
     */
    fun resumeMeasurements(): Boolean {
        val currentState = _measurementState.value
        return if (currentState == ChannelSoundingConstant.MeasurementState.PAUSED) {
            Timber.tag(TAG).d("CS Resuming measurements - ungating callbacks, filter state preserved")
            _measurementState.value = ChannelSoundingConstant.MeasurementState.RUNNING
            // No reinitialization needed - session was kept alive
            true
        } else {
            Timber.tag(TAG).w("CS Cannot resume - not in PAUSED state (current: $currentState)")
            false
        }
    }

    fun getDistanceResult(): StateFlow<DistanceResult> = distanceResult

    /**
     * Flow of outlier detection events for UI to observe and show Toast notifications
     */
    fun getOutlierEvents(): SharedFlow<OutlierEvent> = _outlierEvents.asSharedFlow()

    /**
     * Flow of platform-initiated stop events for Activity to observe and schedule auto-restart
     */
    fun getPlatformStopEvent(): SharedFlow<PlatformStopEvent> = _platformStopEvent.asSharedFlow()
    
    /**
     * Flow of measurement heartbeats (ALL confidence levels) for Activity timing/liveness tracking
     */
    fun getMeasurementHeartbeat(): SharedFlow<MeasurementHeartbeat> = _measurementHeartbeat.asSharedFlow()


    fun getSupportedTechnologies(): List<String> = distanceMeasurementManger.getSupportedTechnologies()

    fun getMeasurementFrequencies(): List<String> = distanceMeasurementManger.getMeasurementFrequencies()

    fun getMeasurementDurations(): List<String> = distanceMeasurementManger.getMeasureDurationsInIntervalRounds()

    fun getLocationTypes(): List<String> = distanceMeasurementManger.getLocationTypes()

    fun getSightType(): List<String> = distanceMeasurementManger.getSightType()

    fun getSensorFusionEnable(): List<String> = distanceMeasurementManger.getSensorFusionEnable()

    /**
     * Set the filtering level (Light, Medium, Heavy).
     * Filtering is always enabled - this controls the aggressiveness.
     */
    fun setFilteringLevel(level: FilteringLevel) {
        Timber.tag(TAG).d("CS Setting filtering level: $level")
        _filteringLevel.value = level
        
        // Apply preset parameters for this level
        val preset = KalmanFilterConfig.getPresetParams(level)
        kalmanFilter.processNoise = preset.processNoise
        kalmanFilter.measurementNoise = preset.measurementNoise
        kalmanFilter.outlierSigma = preset.outlierSigma
        kalmanFilter.outlierNoiseMultiplier = preset.outlierNoiseMultiplier
        kalmanFilter.medianWindowSize = preset.medianWindowSize
        
        Timber.tag(TAG).d("CS Filtering level applied: Q=${preset.processNoise}, R=${preset.measurementNoise}, " +
                "sigma=${preset.outlierSigma}, multiplier=${preset.outlierNoiseMultiplier}, median=${preset.medianWindowSize}")
    }

    /**
     * Get current filtering level
     */
    fun getFilteringLevel(): FilteringLevel = _filteringLevel.value

    /**
     * @deprecated Use setFilteringLevel() instead. Filtering is always enabled now.
     */
    @Deprecated("Filtering is always enabled. Use setFilteringLevel() instead.")
    fun setKalmanFilterEnabled(enabled: Boolean) {
        Timber.tag(TAG).w("CS setKalmanFilterEnabled() is deprecated. Filtering is always enabled.")
        _kalmanFilterEnabled.value = true // Always true
    }

    /**
     * @deprecated Filtering is always enabled now.
     */
    @Deprecated("Filtering is always enabled. Use getFilteringLevel() instead.")
    fun isKalmanFilterEnabled(): Boolean = true // Always true

    /**
     * Reset the Kalman filter state.
     * Call this when starting a new measurement session.
     */
    fun resetKalmanFilter() {
        Timber.tag(TAG).d("CS Kalman filter reset (level: ${_filteringLevel.value})")
        kalmanFilter.reset()
        clearChartData()
        // Reapply current filtering level after reset
        setFilteringLevel(_filteringLevel.value)
        Timber.tag(TAG).d("CS Kalman filter reset complete")
    }

    /**
     * Configure Kalman filter parameters using setNoises
     * @param processNoise Higher values make filter more responsive but noisier
     * @param measurementNoise Higher values make filter smoother but slower to respond
     */
    fun configureKalmanFilter(processNoise: Double, measurementNoise: Double) {
        Timber.tag(TAG).d("CS Kalman filter configured: processNoise=$processNoise, measurementNoise=$measurementNoise")
        kalmanFilter.setNoises(processNoise, measurementNoise)
    }

    /**
     * Apply a full Kalman filter configuration
     * @param config The configuration to apply
     */
    fun applyKalmanFilterConfig(config: KalmanFilterConfig) {
        Timber.tag(TAG).d("CS Applying Kalman filter config: $config")
        _filteringLevel.value = config.filteringLevel
        _kalmanFilterEnabled.value = true // Always enabled now

        // Apply preset parameters for the filtering level
        val preset = KalmanFilterConfig.getPresetParams(config.filteringLevel)
        kalmanFilter.processNoise = preset.processNoise
        kalmanFilter.measurementNoise = preset.measurementNoise
        kalmanFilter.outlierSigma = preset.outlierSigma
        kalmanFilter.outlierNoiseMultiplier = preset.outlierNoiseMultiplier
        kalmanFilter.medianWindowSize = preset.medianWindowSize

        // Store configuration state for settings the new filter handles internally or doesn't expose
        // The new filter uses outlierSigma-based detection internally, so we store these for UI persistence
        _outlierDetectionEnabled = config.outlierDetectionEnabled
        _adaptiveFilteringEnabled = config.adaptiveFilteringEnabled
        _medianPreFilterEnabled = config.medianPreFilterEnabled
        _rateLimiterEnabled = config.rateLimiterEnabled
        _emaEnabled = config.emaEnabled
        _calibrationEnabled = config.calibrationEnabled

        // Store calibration parameters (not used by new filter but kept for UI persistence)
        _distanceOffset = config.distanceOffset
        _distanceScale = config.distanceScale

        // Store outlier threshold for UI (new filter uses outlierSigma internally)
        _outlierThreshold = config.outlierThreshold

        // Map outlier threshold to outlierSigma (approximate mapping for responsiveness)
        // Lower threshold = stricter outlier detection = lower sigma
        kalmanFilter.outlierSigma = when {
            config.outlierThreshold <= 0.30 -> 2.0  // Strict
            config.outlierThreshold <= 0.50 -> 3.0  // Default
            else -> 4.0                              // Lenient
        }

        Timber.tag(TAG).d("CS Kalman filter config applied successfully (outlierSigma=${kalmanFilter.outlierSigma})")
    }

    /**
     * Get current Kalman filter configuration
     */
    fun getKalmanFilterConfig(): KalmanFilterConfig {
        return KalmanFilterConfig(
            filteringLevel = _filteringLevel.value,
            enabled = true, // Always enabled
            outlierDetectionEnabled = _outlierDetectionEnabled,
            adaptiveFilteringEnabled = _adaptiveFilteringEnabled,
            medianPreFilterEnabled = _medianPreFilterEnabled,
            rateLimiterEnabled = _rateLimiterEnabled,
            emaEnabled = _emaEnabled,
            calibrationEnabled = _calibrationEnabled,
            processNoise = kalmanFilter.processNoise,
            measurementNoise = kalmanFilter.measurementNoise,
            outlierThreshold = _outlierThreshold,
            distanceOffset = _distanceOffset,
            distanceScale = _distanceScale
        )
    }

    /**
     * Set outlier detection enabled/disabled
     */
    fun setOutlierDetectionEnabled(enabled: Boolean) {
        Timber.tag(TAG).d("CS Outlier detection enabled: $enabled")
        _outlierDetectionEnabled = enabled
        // Update filter's outlierSigma based on enabled state
        kalmanFilter.outlierSigma = if (enabled) {
            when {
                _outlierThreshold <= 0.30 -> 2.0  // Strict
                _outlierThreshold <= 0.50 -> 3.0  // Default
                else -> 4.0                        // Lenient
            }
        } else {
            // High sigma effectively disables outlier rejection
            100.0
        }
    }

    /**
     * Check if outlier detection is enabled
     */
    fun isOutlierDetectionEnabled(): Boolean = _outlierDetectionEnabled

    /**
     * Set adaptive filtering enabled/disabled
     */
    fun setAdaptiveFilteringEnabled(enabled: Boolean) {
        Timber.tag(TAG).d("CS Adaptive filtering enabled: $enabled")
        _adaptiveFilteringEnabled = enabled
        // New filter handles adaptation internally via outlierNoiseMultiplier
    }

    /**
     * Check if adaptive filtering is enabled
     */
    fun isAdaptiveFilteringEnabled(): Boolean = _adaptiveFilteringEnabled

    /**
     * Set process noise parameter
     */
    fun setProcessNoise(noise: Double) {
        Timber.tag(TAG).d("CS Process noise set to: $noise")
        kalmanFilter.processNoise = noise
    }

    /**
     * Set measurement noise parameter
     */
    fun setMeasurementNoise(noise: Double) {
        Timber.tag(TAG).d("CS Measurement noise set to: $noise")
        kalmanFilter.measurementNoise = noise
    }

    /**
     * Set outlier threshold
     */
    fun setOutlierThreshold(threshold: Double) {
        Timber.tag(TAG).d("CS Outlier threshold set to: $threshold")
        _outlierThreshold = threshold
        // Map threshold to outlierSigma if outlier detection is enabled
        if (_outlierDetectionEnabled) {
            kalmanFilter.outlierSigma = when {
                threshold <= 0.30 -> 2.0  // Strict
                threshold <= 0.50 -> 3.0  // Default
                else -> 4.0                // Lenient
            }
        }
    }

    class Factory(
        val activity: Activity,
        val bleConnection: BleConnection
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChannelSoundingDistanceMeasurementViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ChannelSoundingDistanceMeasurementViewModel(activity, bleConnection) as T
            }
            throw IllegalArgumentException("Unable to ChannelSoundingDistanceMeasurementViewModel construct viewmodel")
        }
    }

    private val callback = object : ChannelSoundingDistanceMeasurementManager.Callback {
        override fun onStartSuccess() {
            Timber.tag(TAG).d("CS onStartSuccess callback received, Filtering level: ${_filteringLevel.value}")
            // Reset the ignore flag once new session successfully started
            ignoreStopCallbacks = false
            startFailureReasons.postValue(emptyList())
            sessionState.postValue(ChannelSoundingConstant.RangeSessionState.STARTED)
            
            // Set measurement state to RUNNING so callbacks are processed
            // This is set here (not in channelSoundingStart) because we only want to
            // process measurements after the session has successfully started
            _measurementState.value = ChannelSoundingConstant.MeasurementState.RUNNING
            Timber.tag(TAG).d("CS Measurement state set to RUNNING - callbacks are now active")
        }

        override fun onStartFail(reasons: List<String>) {
            Timber.tag(TAG).e("CS Ranging start failed: $reasons")
            ignoreStopCallbacks = false
            startFailureReasons.postValue(reasons)
            sessionState.postValue(ChannelSoundingConstant.RangeSessionState.STOPPED)
        }

        override fun onStop(reason: Int) {
            val isRecoverable = ChannelSoundingDistanceMeasurementManager.StopReason.isRecoverable(reason)
            val reasonName = ChannelSoundingDistanceMeasurementManager.StopReason.toString(reason)
            Timber.tag(TAG).d("CS_DBG_012 onStop callback received, reason=$reason ($reasonName), isRecoverable=$isRecoverable, ignoreStopCallbacks=$ignoreStopCallbacks")
            
            // Ignore stop callbacks from old sessions during restart
            if (ignoreStopCallbacks) {
                Timber.tag(TAG).d("CS Ignoring onStop callback from old session during restart")
                return
            }
            
            sessionState.postValue(ChannelSoundingConstant.RangeSessionState.STOPPED)
            
            // Set measurement state to IDLE since the session has actually stopped.
            // Note: This is only called when the session truly terminates (not on pause).
            // During pause, we keep the session alive and measurement state stays PAUSED.
            _measurementState.value = ChannelSoundingConstant.MeasurementState.IDLE
            Timber.tag(TAG).d("CS Measurement state set to IDLE - session terminated")
            
            // Emit platform stop event for recoverable reasons so Activity can schedule auto-restart
            if (isRecoverable) {
                Timber.tag(TAG).d("CS_DBG_013 Emitting platform stop event for auto-restart consideration")
                viewModelScope.launch {
                    _platformStopEvent.emit(PlatformStopEvent(reason, isRecoverable, reasonName))
                }
            }
        }

        override fun onDistanceResult(distanceMeter: Double?, confidence: Integer?) {
            // === MEASUREMENT STATE GATE ===
            // Skip processing if measurements are paused.
            // This allows the session to stay alive while temporarily ignoring callbacks.
            // When resumed, measurements continue instantly with preserved Kalman filter state.
            val currentMeasurementState = _measurementState.value
            if (currentMeasurementState == ChannelSoundingConstant.MeasurementState.PAUSED) {
                Timber.tag(TAG).v("CS Measurement callback gated - state is PAUSED, ignoring")
                return
            }
            if (currentMeasurementState != ChannelSoundingConstant.MeasurementState.RUNNING) {
                Timber.tag(TAG).v("CS Measurement callback gated - state is $currentMeasurementState (not RUNNING)")
                return
            }
            // === END MEASUREMENT STATE GATE ===

            val rawDistance = distanceMeter ?: 0.0
            val confidenceValue = confidence?.toInt() ?: 0
            
            // === HEARTBEAT UPDATE: Process ALL confidence levels to keep app responsive ===
            // Emit heartbeat for ALL measurements (HIGH, MEDIUM, LOW) to prevent stall
            val currentTimeMs = System.currentTimeMillis()
            _measurementHeartbeat.tryEmit(
                MeasurementHeartbeat(
                    timestamp = currentTimeMs,
                    confidence = confidenceValue,
                    rawDistance = rawDistance
                )
            )
            
            val dt = if (lastDistanceUpdateTimeMs > 0) {
                ((currentTimeMs - lastDistanceUpdateTimeMs) / 1000.0).coerceIn(0.001, 1.0)
            } else {
                1.0 // Default to 1 second if no previous time
            }
            
            // Explicitly reject LOW/MEDIUM confidence measurements (but still advance time)
            if (confidenceValue != 2) {
                Timber.tag(TAG).d("CS Received %s confidence measurement (%.3f m) - rejected (HIGH only)",
                    when(confidenceValue) { 0 -> "LOW"; 1 -> "MEDIUM"; else -> "UNKNOWN" },
                    rawDistance)
                // Update timing even for rejected measurements to prevent stall
                lastDistanceUpdateTimeMs = currentTimeMs
                // Kalman filter: predict-only step (no measurement update)
                // Simply maintain current estimate without incorporating this measurement
                return
            }
            
            // HIGH confidence measurement - proceed with full Kalman filter update
            lastDistanceUpdateTimeMs = currentTimeMs

            // Apply Kalman filter using update() method with complete logging
            val result = kalmanFilter.update(rawDistance, dtSeconds = dt)

            // Log complete filter results
            Timber.tag(TAG).i(
                "CS ==================== FILTER UPDATE ====================")
            Timber.tag(TAG).i(
                "CS Input: raw=%.4f m, dt=%.3f s, confidence=%d (%s)",
                rawDistance, dt, confidenceValue,
                when (confidenceValue) { 2 -> "HIGH"; 1 -> "MEDIUM"; else -> "LOW" }
            )
            Timber.tag(TAG).i(
                "CS Calibration: calibrated=%.4f m (offset=%.2f m, scale=%.2f)",
                result.calibrated,
                _distanceOffset,
                _distanceScale
            )
            Timber.tag(TAG).i(
                "CS Kalman: zUsed=%.4f m (median-filtered), K=%.4f, Q=%.6f, R=%.6f",
                result.zUsed, result.kalmanGain, result.usedQ, result.usedR
            )
            Timber.tag(TAG).i(
                "CS Output: filtered=%.4f m",
                result.output
            )
            Timber.tag(TAG).i(
                "CS Flags: outlier=%b, nearOutlier=%b, spike=%b, rateLimited=%b, filteringLevel=%s",
                result.isOutlierSuspected, result.isNearOutlier, result.isSpike, result.limitedByRate,
                _filteringLevel.value
            )
            Timber.tag(TAG).i(
                "CS ========================================================")

            // Log raw distance data (1 sample per second for 5 minutes, exports to CSV)
            logRawDistanceData(
                rawMeter = rawDistance,
                confidence = confidenceValue
            )

            // Always use filtered output (filtering is always enabled)
            val filteredDistance = result.output

            distanceResult.value = DistanceResult(
                distanceMeter = filteredDistance,
                confidence = confidenceValue,
                rawDistanceMeter = rawDistance,
                isFiltered = true, // Always filtered now
                isOutlier = result.isOutlierSuspected,
                kalmanGain = result.kalmanGain,
                mahalanobisDistance = 0.0 // Not computed in new implementation
            )

            // Add data point to chart (always add both raw and filtered for dual-line display)
            addChartDataPoint(
                rawDistance = rawDistance.toFloat(),
                filteredDistance = result.output.toFloat()
            )

            // Emit outlier event for UI to show Toast notification
            if (result.isOutlierSuspected) {
                _outlierEvents.tryEmit(
                    OutlierEvent.OutlierDetected(
                        rawDistance = rawDistance,
                        filteredDistance = filteredDistance,
                        mahalanobisDistance = 0.0
                    )
                )
            }

            Timber.tag(TAG).d("CS Distance: raw=%.3f m, filtered=%.3f m, confidence=%d, filteringLevel=%s"
                .format(rawDistance, filteredDistance, confidenceValue, _filteringLevel.value))
        }
    }

    // Track last distance update time for dt calculation
    private var lastDistanceUpdateTimeMs: Long = 0

    fun channelSoundingStop(){
        val current = sessionState.value
        // Only stop if not already stopped or stopping
        if (current != ChannelSoundingConstant.RangeSessionState.STOPPED && 
            current != ChannelSoundingConstant.RangeSessionState.STOPPING) {
            sessionState.postValue(ChannelSoundingConstant.RangeSessionState.STOPPING)
            try {
                distanceMeasurementManger.stop()
            } catch (e: SecurityException) {
                Timber.tag(TAG).e("Missing Bluetooth permission: ${e.message}")
                sessionState.postValue(ChannelSoundingConstant.RangeSessionState.STOPPED)
            }
            // State will be updated to STOPPED by onStop() callback
        }
    }

    fun channelSoundingStart(technology: String, freq: String, duration: Int){
        val current = sessionState.value
        Timber.tag(TAG).d("CS channelSoundingStart current state: $current")
        try {
            // Set flag to ignore any pending stop callbacks from old sessions
            ignoreStopCallbacks = true

            // Reset Kalman filter when starting a new session
            resetKalmanFilter()

            // Start 5-minute data logging for analysis
            startDataLogging()

            // Reinitialize the manager to ensure clean state
            reinitializeManager()

            val success: Boolean = distanceMeasurementManger.start(technology, freq, duration)
            if (success) {
                sessionState.postValue(ChannelSoundingConstant.RangeSessionState.STARTING)
            } else {
                ignoreStopCallbacks = false
                sessionState.postValue(ChannelSoundingConstant.RangeSessionState.STOPPED)
            }
        }catch (e: SecurityException) {
            ignoreStopCallbacks = false
            Timber.tag(TAG).e("Missing Bluetooth permission: ${e.message}")
        }
    }

    fun toggleStartStop(technology: String, freq: String, duration: Int) {
        val current = sessionState.value
        Timber.tag(TAG).d("CS toggleStartStop current state: $current, Filtering level: ${_filteringLevel.value}")

        // Prevent duplicate operations
        if (current == ChannelSoundingConstant.RangeSessionState.STARTING) {
            Timber.tag(TAG).w("CS Already starting, ignoring duplicate start request")
            return
        }
        if (current == ChannelSoundingConstant.RangeSessionState.STOPPING) {
            Timber.tag(TAG).w("CS Already stopping, ignoring duplicate stop request")
            return
        }
        
        if (current == ChannelSoundingConstant.RangeSessionState.STOPPED || current == ChannelSoundingConstant.RangeSessionState.STOPPING) {
            Timber.tag(TAG).d("CS Starting distance measurement")
            Timber.tag(TAG).d("CS Technology: $technology, Freq: $freq, Duration: $duration")

            // Set flag to ignore any pending stop callbacks from old sessions
            ignoreStopCallbacks = true

            // Reset Kalman filter when starting a new session
            resetKalmanFilter()

            // Start 5-minute data logging for analysis
            startDataLogging()

            // Reinitialize the manager to ensure clean state when starting a new session
            try {
                reinitializeManager()
            } catch (e: SecurityException) {
                Timber.tag(TAG).e("Missing Bluetooth permission during reinitialize: ${e.message}")
                ignoreStopCallbacks = false
            }

            Timber.tag(TAG).d("CS Calling distanceMeasurementManger.start()")
            val success: Boolean =
                distanceMeasurementManger.start(
                    technology, freq, duration
                )
            Timber.tag(TAG).d("CS distanceMeasurementManger.start() returned: $success")
            if (success) {
                sessionState.postValue(ChannelSoundingConstant.RangeSessionState.STARTING)
            } else {
                Timber.tag(TAG).e("CS Failed to start distance measurement manager")
                ignoreStopCallbacks = false
                sessionState.postValue(ChannelSoundingConstant.RangeSessionState.STOPPED)
            }
        } else if (current == ChannelSoundingConstant.RangeSessionState.STARTED
            || current == ChannelSoundingConstant.RangeSessionState.STARTING) {
            Timber.tag(TAG).d("CS Stopping distance measurement")
            sessionState.postValue(ChannelSoundingConstant.RangeSessionState.STOPPING)
            try {
                distanceMeasurementManger.stop()
            } catch (e: SecurityException) {
                Timber.tag(TAG).e("Missing Bluetooth permission: ${e.message}")
                sessionState.postValue(ChannelSoundingConstant.RangeSessionState.STOPPED)
            }
        }
    }

    init {
        distanceMeasurementManger = ChannelSoundingDistanceMeasurementManager(
            activity,
            bleConnection, false,
            callback
        )

        // Load and apply saved Kalman filter configuration from SharedPreferences
        val savedConfig = KalmanFilterConfig.load(getApplication())
        Timber.tag(TAG).d("CS Loading saved Kalman filter config: $savedConfig")
        _filteringLevel.value = savedConfig.filteringLevel
        applyKalmanFilterConfig(savedConfig)
    }

    companion object {
        private val TAG = "CS_DistanceMeasurementVM"
    }
}