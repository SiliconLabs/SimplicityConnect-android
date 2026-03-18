package com.siliconlabs.bledemo.features.demo.channel_sounding.models

import android.content.Context
import androidx.core.content.edit
import com.siliconlabs.bledemo.features.demo.channel_sounding.filters.DistanceKalmanFilter

/**
 * Filtering level presets that balance latency vs outlier rejection.
 * Filtering is always enabled - these presets adjust the aggressiveness.
 */
enum class FilteringLevel(val displayName: String) {
    LIGHT("Light - Fast Response"),
    MEDIUM("Medium - Balanced (Default)"),
    HEAVY("High - Maximum Smoothing")
}

/**
 * Data class representing Kalman filter configuration parameters.
 * Updated to work with the new DistanceKalmanFilter implementation.
 * Filtering is always enabled - use filteringLevel to adjust aggressiveness.
 */
data class KalmanFilterConfig(
    val filteringLevel: FilteringLevel = FilteringLevel.MEDIUM,
    val enabled: Boolean = true, // Deprecated - kept for backward compatibility, always true
    val outlierDetectionEnabled: Boolean = true,
    val adaptiveFilteringEnabled: Boolean = true,
    val medianPreFilterEnabled: Boolean = true,
    val rateLimiterEnabled: Boolean = true,
    val emaEnabled: Boolean = true,
    val calibrationEnabled: Boolean = false,
    val processNoise: Double = DistanceKalmanFilter.Defaults.processNoise,
    val measurementNoise: Double = DistanceKalmanFilter.Defaults.measurementNoise,
    val outlierThreshold: Double = DistanceKalmanFilter.Defaults.outlierThresholdPerSecond,
    val distanceOffset: Double = DistanceKalmanFilter.Defaults.distanceOffset,
    val distanceScale: Double = DistanceKalmanFilter.Defaults.distanceScale
) {
    companion object {
        private const val PREFS_NAME = "KalmanFilterPrefs"
        private const val KEY_FILTERING_LEVEL = "kalman_filtering_level"
        private const val KEY_ENABLED = "kalman_filter_enabled" // Legacy - kept for migration
        private const val KEY_OUTLIER_DETECTION = "kalman_outlier_detection_enabled"
        private const val KEY_ADAPTIVE_FILTERING = "kalman_adaptive_filtering_enabled"
        private const val KEY_MEDIAN_PRE_FILTER = "kalman_median_pre_filter_enabled"
        private const val KEY_RATE_LIMITER = "kalman_rate_limiter_enabled"
        private const val KEY_EMA = "kalman_ema_enabled"
        private const val KEY_CALIBRATION = "kalman_calibration_enabled"
        private const val KEY_PROCESS_NOISE = "kalman_process_noise"
        private const val KEY_MEASUREMENT_NOISE = "kalman_measurement_noise"
        private const val KEY_OUTLIER_THRESHOLD = "kalman_outlier_threshold"
        private const val KEY_DISTANCE_OFFSET = "kalman_distance_offset"
        private const val KEY_DISTANCE_SCALE = "kalman_distance_scale"

        // Available options for spinners - tuned for aggressive outlier rejection
        val PROCESS_NOISE_OPTIONS = listOf(
            0.004,  // Very smooth, slowest response
            0.006,  // Default - aggressive outlier rejection (from DistanceKalmanFilter.Defaults)
            0.008,  // Smooth
            0.012,  // Responsive (previous default)
            0.020,  // Very responsive
            0.035   // Most responsive, more noise
        )

        val MEASUREMENT_NOISE_OPTIONS = listOf(
            0.02,   // Trust measurements highly
            0.03,   // High trust
            0.04,   // Moderate trust (previous default)
            0.06,   // Lower trust
            0.08,   // Default - aggressive outlier rejection (from DistanceKalmanFilter.Defaults)
            0.10    // Low trust, smoothest output
        )

        val OUTLIER_THRESHOLD_OPTIONS = listOf(
            0.30,   // Strict - rejects more measurements
            0.40,   // Moderate strict
            0.50,   // Default (from DistanceKalmanFilter.Defaults)
            0.70,   // Lenient
            1.00    // Very lenient - allows large variations
        )

        /**
         * Get preset parameters for a filtering level
         */
        fun getPresetParams(level: FilteringLevel): PresetParams {
            return when (level) {
                FilteringLevel.LIGHT -> PresetParams(
                    processNoise = 0.012,
                    measurementNoise = 0.04,
                    outlierSigma = 3.0,
                    outlierNoiseMultiplier = 9.0,
                    medianWindowSize = 3
                )
                FilteringLevel.MEDIUM -> PresetParams(
                    processNoise = 0.006,
                    measurementNoise = 0.08,
                    outlierSigma = 2.0,
                    outlierNoiseMultiplier = 25.0,
                    medianWindowSize = 5
                )
                FilteringLevel.HEAVY -> PresetParams(
                    processNoise = 0.004,
                    measurementNoise = 0.10,
                    outlierSigma = 1.5,
                    outlierNoiseMultiplier = 50.0,
                    medianWindowSize = 7
                )
            }
        }

        /**
         * Preset parameters for a filtering level
         */
        data class PresetParams(
            val processNoise: Double,
            val measurementNoise: Double,
            val outlierSigma: Double,
            val outlierNoiseMultiplier: Double,
            val medianWindowSize: Int
        )

        /**
         * Load configuration from SharedPreferences
         * Also considers legacy SharedPreferences for migration
         */
        fun load(context: Context): KalmanFilterConfig {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            // Load filtering level (new approach)
            val levelOrdinal = prefs.getInt(KEY_FILTERING_LEVEL, -1)
            val filteringLevel = if (levelOrdinal >= 0 && levelOrdinal < FilteringLevel.values().size) {
                FilteringLevel.values()[levelOrdinal]
            } else {
                // Migration: if old enabled flag exists, use it to determine level
                val hasOldConfig = prefs.contains(KEY_ENABLED) || 
                    context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE).contains("kalman_filter_enabled")
                if (hasOldConfig) {
                    val wasEnabled = prefs.getBoolean(KEY_ENABLED, true) || 
                        context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                            .getBoolean("kalman_filter_enabled", true)
                    if (wasEnabled) FilteringLevel.MEDIUM else FilteringLevel.MEDIUM // Always enabled now
                } else {
                    FilteringLevel.MEDIUM // Default
                }
            }

            // Apply preset if using a preset level, otherwise use saved individual params
            val presetParams = getPresetParams(filteringLevel)
            val usePreset = !prefs.contains(KEY_PROCESS_NOISE) // If no custom params saved, use preset

            return KalmanFilterConfig(
                filteringLevel = filteringLevel,
                enabled = true, // Always enabled now
                outlierDetectionEnabled = prefs.getBoolean(KEY_OUTLIER_DETECTION, true),
                adaptiveFilteringEnabled = prefs.getBoolean(KEY_ADAPTIVE_FILTERING, true),
                medianPreFilterEnabled = prefs.getBoolean(KEY_MEDIAN_PRE_FILTER, true),
                rateLimiterEnabled = prefs.getBoolean(KEY_RATE_LIMITER, true),
                emaEnabled = prefs.getBoolean(KEY_EMA, true),
                calibrationEnabled = prefs.getBoolean(KEY_CALIBRATION, false),
                processNoise = if (usePreset) presetParams.processNoise else 
                    prefs.getFloat(KEY_PROCESS_NOISE, presetParams.processNoise.toFloat()).toDouble(),
                measurementNoise = if (usePreset) presetParams.measurementNoise else 
                    prefs.getFloat(KEY_MEASUREMENT_NOISE, presetParams.measurementNoise.toFloat()).toDouble(),
                outlierThreshold = prefs.getFloat(KEY_OUTLIER_THRESHOLD, DistanceKalmanFilter.Defaults.outlierThresholdPerSecond.toFloat()).toDouble(),
                distanceOffset = prefs.getFloat(KEY_DISTANCE_OFFSET, DistanceKalmanFilter.Defaults.distanceOffset.toFloat()).toDouble(),
                distanceScale = prefs.getFloat(KEY_DISTANCE_SCALE, DistanceKalmanFilter.Defaults.distanceScale.toFloat()).toDouble()
            )
        }

        /**
         * Get default configuration
         */
        fun getDefault(): KalmanFilterConfig = KalmanFilterConfig()

        /**
         * Get display string for process noise value
         */
        fun getProcessNoiseDisplayString(value: Double): String {
            return when {
                value <= 0.004 -> "0.004 (Very Smooth)"
                value <= 0.006 -> "0.006 (Default)"
                value <= 0.008 -> "0.008 (Smooth)"
                value <= 0.012 -> "0.012 (Responsive)"
                value <= 0.020 -> "0.020 (Very Responsive)"
                else -> "0.035 (Most Responsive)"
            }
        }

        /**
         * Get display string for measurement noise value
         */
        fun getMeasurementNoiseDisplayString(value: Double): String {
            return when {
                value <= 0.02 -> "0.02 (High Trust)"
                value <= 0.03 -> "0.03 (Trust)"
                value <= 0.04 -> "0.04 (Moderate)"
                value <= 0.06 -> "0.06 (Smooth)"
                value <= 0.08 -> "0.08 (Default)"
                else -> "0.10 (Very Smooth)"
            }
        }

        /**
         * Get display string for outlier threshold value
         */
        fun getOutlierThresholdDisplayString(value: Double): String {
            return when {
                value <= 0.30 -> "0.30 m/s (Strict)"
                value <= 0.40 -> "0.40 m/s (Moderate)"
                value <= 0.50 -> "0.50 m/s (Default)"
                value <= 0.70 -> "0.70 m/s (Lenient)"
                else -> "1.00 m/s (Very Lenient)"
            }
        }

        /**
         * Get list of display strings for process noise spinner
         */
        fun getProcessNoiseDisplayStrings(): List<String> {
            return PROCESS_NOISE_OPTIONS.map { getProcessNoiseDisplayString(it) }
        }

        /**
         * Get list of display strings for measurement noise spinner
         */
        fun getMeasurementNoiseDisplayStrings(): List<String> {
            return MEASUREMENT_NOISE_OPTIONS.map { getMeasurementNoiseDisplayString(it) }
        }

        /**
         * Get list of display strings for outlier threshold spinner
         */
        fun getOutlierThresholdDisplayStrings(): List<String> {
            return OUTLIER_THRESHOLD_OPTIONS.map { getOutlierThresholdDisplayString(it) }
        }

        /**
         * Get list of filtering level display strings
         */
        fun getFilteringLevelDisplayStrings(): List<String> {
            return FilteringLevel.values().map { it.displayName }
        }
    }

    /**
     * Save configuration to SharedPreferences
     */
    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putInt(KEY_FILTERING_LEVEL, filteringLevel.ordinal)
            putBoolean(KEY_ENABLED, true) // Always true for backward compatibility
            putBoolean(KEY_OUTLIER_DETECTION, outlierDetectionEnabled)
            putBoolean(KEY_ADAPTIVE_FILTERING, adaptiveFilteringEnabled)
            putBoolean(KEY_MEDIAN_PRE_FILTER, medianPreFilterEnabled)
            putBoolean(KEY_RATE_LIMITER, rateLimiterEnabled)
            putBoolean(KEY_EMA, emaEnabled)
            putBoolean(KEY_CALIBRATION, calibrationEnabled)
            putFloat(KEY_PROCESS_NOISE, processNoise.toFloat())
            putFloat(KEY_MEASUREMENT_NOISE, measurementNoise.toFloat())
            putFloat(KEY_OUTLIER_THRESHOLD, outlierThreshold.toFloat())
            putFloat(KEY_DISTANCE_OFFSET, distanceOffset.toFloat())
            putFloat(KEY_DISTANCE_SCALE, distanceScale.toFloat())
        }

        // Also sync enabled state to legacy SharedPreferences for backward compatibility
        val legacyPrefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        legacyPrefs.edit {
            putBoolean("kalman_filter_enabled", enabled)
        }
    }

    /**
     * Reset to default values and save
     */
    fun resetToDefault(context: Context): KalmanFilterConfig {
        val default = getDefault()
        default.save(context)
        return default
    }
}
