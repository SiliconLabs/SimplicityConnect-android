package com.siliconlabs.bledemo.features.demo.channel_sounding.utils

import android.content.Context
import android.content.pm.PackageManager
import timber.log.Timber

object ChannelSoundingDetectorUtils {
    private val POSSIBLE_FEATURE_KEYS = listOf(
        // Preferred/finalized names (adjust if your SDK exposes constants):
        "android.hardware.bluetooth.le.channel_sounding",
        // Fallback / legacy candidates seen in previews/vendor trees:
        "android.hardware.bluetooth.channel_sounding",
        "android.software.bluetooth.le.channel_sounding",
        // Last-ditch experimental key some OEMs used:
        "com.android.feature.BLUETOOTH_LE_CHANNEL_SOUNDING"
    )

    /**
     * Fast probe using PackageManager system features.
     * Safe on all API levels.
     */
    fun hasFeatureFlag(pm: PackageManager): Boolean {
        // 1) direct check via hasSystemFeature on each candidate key
        POSSIBLE_FEATURE_KEYS.forEach { key ->
            try {
                if (pm.hasSystemFeature(key)) return true
            } catch (_: Throwable) {
                // ignore and keep trying
            }
        }

        // 2) fallback: enumerate all available features and string-match
        return try {
            val feats = pm.systemAvailableFeatures ?: return false
            feats.any { it?.name?.contains("channel", ignoreCase = true) == true &&
                    it.name?.contains("sounding", ignoreCase = true) == true }
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Reflection-based probe of Android's unified Ranging APIs.
     *
     * Why reflection? Names may differ across previews/forks,
     * and this keeps your app compatible without compile-time deps.
     *
     * Returns true if the runtime says BLE Channel Sounding is supported.
     */
    fun hasRangingRuntimeCapability(context: Context): Boolean {
        return try {
            // android.ranging.RangingManager (Android 15/16+ in many builds)
            val rmClass = Class.forName("android.ranging.RangingManager")
            val getService = Context::class.java.getMethod("getSystemService", Class::class.java)
            val rmInstance = getService.invoke(context, rmClass) ?: return false

            // capabilities = rangingManager.getCapabilities()
            // Some SDKs expose getCapabilities(); others use register*Callback.
            // Try direct getter first:
            val getCaps = runCatching { rmClass.getMethod("getCapabilities") }.getOrNull()
            val capsObj = if (getCaps != null) {
                getCaps.invoke(rmInstance)
            } else {
                // Try a synchronous “capabilities” property via getXxx if present
                val capsField = runCatching { rmClass.getDeclaredField("capabilities") }.getOrNull()
                capsField?.isAccessible = true
                capsField?.get(rmInstance)
            } ?: return false

            // Inspect capabilities for supported technologies.
            // Look for an enum or bitmask mentioning BLUETOOTH / CHANNEL_SOUNDING.
            val capsClass = capsObj::class.java

            // 1) Try boolean style: isBluetoothLeChannelSoundingSupported()
            runCatching {
                val m = capsClass.getMethod("isBluetoothLeChannelSoundingSupported")
                return (m.invoke(capsObj) as? Boolean) == true
            }

            // 2) Try list/bitmask style: getSupportedTechnologies()
            val getTechs = runCatching { capsClass.getMethod("getSupportedTechnologies") }.getOrNull()
            if (getTechs != null) {
                val techs = getTechs.invoke(capsObj) as? Collection<*>
                if (techs != null && techs.any { it?.toString()?.contains("CHANNEL_SOUNDING", true) == true }) {
                    return true
                }
            }

            // 3) Try a generic flags field: capabilities.flags bit & CHANNEL_SOUNDING
            val flagsField = runCatching { capsClass.getDeclaredField("flags") }.getOrNull()
            val flagsValue = flagsField?.let {
                it.isAccessible = true
                (it.get(capsObj) as? Int) ?: 0
            } ?: 0

            // If there is a companion/const named e.g. FLAG_BLUETOOTH_CHANNEL_SOUNDING, try to fetch it:
            val companionFlag = runCatching {
                capsClass.getDeclaredField("FLAG_BLUETOOTH_CHANNEL_SOUNDING").apply { isAccessible = true }.getInt(null)
            }.getOrElse { 0 }

            if (companionFlag != 0 && (flagsValue and companionFlag) == companionFlag) {
                return true
            }

            false
        } catch (e: Throwable) {
            Timber.tag(TAG).d( "Ranging runtime capability probe failed: ${e.message}")
            false
        }
    }

    /**
     * Single call your app can use.
     * - Checks a PackageManager feature flag
     * - Falls back to Ranging runtime capability (if available)
     *
     * Note: On some devices the runtime capability may be more reliable
     * than the declared feature; you can flip the order if that’s better
     * for your fleet.
     */
    fun isBleChannelSoundingSupported(context: Context): Boolean {
        val pm = context.packageManager

        // Prefer runtime truth if you target newer Android and your app
        // requests the RANGING permission. Otherwise, use flags first.
        val flagYes = runCatching { hasFeatureFlag(pm) }.getOrDefault(false)
        val runtimeYes = runCatching { hasRangingRuntimeCapability(context) }.getOrDefault(false)

        // If either says yes, consider it supported.
        return flagYes || runtimeYes
    }

     val TAG = ChannelSoundingDetectorUtils::class.java.simpleName

}