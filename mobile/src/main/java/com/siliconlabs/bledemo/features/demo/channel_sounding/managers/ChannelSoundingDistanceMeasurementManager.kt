package com.siliconlabs.bledemo.features.demo.channel_sounding.managers

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.CancellationSignal
import android.ranging.RangingCapabilities
import android.ranging.RangingData
import android.ranging.RangingDevice
import android.ranging.RangingManager
import android.ranging.RangingSession
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.siliconlabs.bledemo.features.demo.channel_sounding.interfaces.BleConnection
import com.siliconlabs.bledemo.features.demo.channel_sounding.utils.ChannelSoundingConfigureParameters
import com.siliconlabs.bledemo.features.demo.channel_sounding.utils.ChannelSoundingRangingParameters
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import com.siliconlabs.bledemo.R
import java.util.concurrent.atomic.AtomicReference

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
class ChannelSoundingDistanceMeasurementManager(
    val activity: Activity,
    val bleConnection: BleConnection,
    val isResponder: Boolean,
    cb: Callback
) {
    private var rangingManager: RangingManager =
        activity.getSystemService(RangingManager::class.java) as RangingManager
    private var executor: Executor = Executors.newSingleThreadExecutor()
    private var callback: Callback = cb
    private var alertDialog: androidx.appcompat.app.AlertDialog? = null
    private val rangingCapabilities = AtomicReference<RangingCapabilities?>()
    private val cancellationSignal = AtomicReference<CancellationSignal?>(null)
    private val capabilitiesCountDownLatch = CountDownLatch(1)
    private var targetDevice: BluetoothDevice? = null
    private var session: RangingSession? = null

    private val PERM_RANGING = "android.permission.RANGING"

    init {
        rangingManager.registerCapabilitiesCallback(
            executor,
            { capabilities ->
                // Store capabilities and release latch
                rangingCapabilities.set(capabilities)
                capabilitiesCountDownLatch.countDown()
                Timber.tag(TAG)
                    .d("ChannelSoundingDistanceMeasurementManager capabilities: $capabilities")
            })
    }

    // --- Readiness / permission helpers -----------------------------------------------------
    private fun hasPermission(permission: String): Boolean =
        ActivityCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED

    private fun hasRangingPermission(): Boolean = hasPermission(PERM_RANGING)

    private fun ensureRangingPermission(): Boolean {
        if (!hasRangingPermission()) {
            Timber.tag(TAG).e("Missing android.permission.RANGING – cannot start ranging session")
            return false
        }
        return true
    }

    private fun hasBtConnectPermission(): Boolean =
        hasPermission(Manifest.permission.BLUETOOTH_CONNECT)

    private fun isTargetBondedIfRequired(tech: ChannelSoundingRangingParameters.Technology): Boolean {
        if (tech == ChannelSoundingRangingParameters.Technology.BLE_CS ||
            tech == ChannelSoundingRangingParameters.Technology.OOB
        ) {
            val bonded = if (hasBtConnectPermission()) {
                try {
                    targetDevice?.bondState == BluetoothDevice.BOND_BONDED
                } catch (se: SecurityException) {
                    Timber.tag(TAG).e("SecurityException reading bond state: ${se.message}")
                    false
                }
            } else {
                false
            }
            if (!bonded) {
                Timber.tag(TAG).d("CS Device not bonded but bonding is required for $tech")
                if (hasBtConnectPermission()) {
                    //showBondAlertDialog()
                    try {
                        targetDevice?.createBond()?.let {
                            if (!it) {
                                Timber.tag(TAG)
                                    .d("Failed to initiate bond with ${targetDevice!!.name}")
                            }
                        }
                    } catch (e: SecurityException) {
                        Timber.tag(TAG).e("showBondAlertDialog SecurityException: ${e.message}")
                    }
                } else {
                    Timber.tag(TAG)
                        .e("Cannot show bond dialog without BLUETOOTH_CONNECT permission")
                }
            }
            return bonded
        }
        return true
    }

    data class Readiness(
        val hasRangingPermission: Boolean,
        val hasBluetoothConnectPermission: Boolean,
        val targetDeviceSet: Boolean,
        val targetBondedIfNeeded: Boolean,
        val capabilitiesAvailable: Boolean,
        val selectedTechnologySupported: Boolean,
        val ok: Boolean,
        val missingReasons: List<String>
    )

    private fun evaluateReadiness(selectedTech: ChannelSoundingRangingParameters.Technology): Readiness {
        val missing = mutableListOf<String>()
        val hasRangingPerm = ensureRangingPermission()
        if (!hasRangingPerm) missing += "RANGING permission not granted"
        val hasBtConn = hasBtConnectPermission()
        if (!hasBtConn) missing += "BLUETOOTH_CONNECT permission not granted"
        val deviceSet = targetDevice != null
        if (!deviceSet) missing += "Target device not set"
        val bondedIfNeeded = if (deviceSet) isTargetBondedIfRequired(selectedTech) else false
        if (deviceSet && !bondedIfNeeded) missing += "Device not bonded"
        // Wait (briefly) for capabilities if not already present
        if (rangingCapabilities.get() == null) {
            try {
                capabilitiesCountDownLatch.await(1, TimeUnit.SECONDS)
            } catch (_: InterruptedException) {
            }
        }
        val capsAvail = rangingCapabilities.get() != null
        if (!capsAvail) missing += "Ranging capabilities unavailable"
        var techSupported = false
        if (capsAvail) {
            val availability = rangingCapabilities.get()!!.technologyAvailability
            techSupported =
                availability.any { (id, avail) -> avail == RangingCapabilities.ENABLED && id == selectedTech.tech }
            if (!techSupported && selectedTech != ChannelSoundingRangingParameters.Technology.OOB) {
                missing += "Selected technology ${selectedTech} not supported / not enabled"
            } else if (selectedTech == ChannelSoundingRangingParameters.Technology.OOB) {
                techSupported = true // OOB always allowed as per original code
            }
        }
        val ok = missing.isEmpty()
        return Readiness(
            hasRangingPermission = hasRangingPerm,
            hasBluetoothConnectPermission = hasBtConn,
            targetDeviceSet = deviceSet,
            targetBondedIfNeeded = bondedIfNeeded,
            capabilitiesAvailable = capsAvail,
            selectedTechnologySupported = techSupported,
            ok = ok,
            missingReasons = missing
        )
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun setTargetDevice(device: BluetoothDevice) {
        targetDevice = device
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    fun stop() {
        if (cancellationSignal.get() != null) {
            Timber.tag(TAG).d("CS Stopping ranging session")
            Timber.tag(TAG).d("CS Stop ranging with device: ${targetDevice?.name}")
            try {
                cancellationSignal.get()?.cancel()
            } catch (e: Exception) {
                Timber.tag(TAG).e("CS Error cancelling ranging session: ${e.message}")
            }
            cancellationSignal.set(null)
        }
        if (alertDialog != null) {
            try {
                if (alertDialog!!.isShowing) alertDialog?.dismiss()
            } catch (e: IllegalArgumentException) {
                Timber.tag(TAG).e("Dialog dismiss error: ${e.message}")
            }
        }
        session = null
    }

    private fun getRangingTechnologyName(technology: Int): String {
        for (tech in ChannelSoundingRangingParameters.Technology.entries) {
            if (tech.tech == technology) return tech.toString()
        }
        throw IllegalArgumentException("unknown technology $technology")
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    @SuppressLint("MissingPermission")
    fun getSupportedTechnologies(): ArrayList<String> {
        val techs = ArrayList<String>()
        try {
            capabilitiesCountDownLatch.await(2, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Timber.tag(TAG).e("getSupportedTechnologies InterruptedException: ${e.message}")
        }
        if (rangingCapabilities.get() == null) {
            Timber.tag(TAG).e("getSupportedTechnologies rangingCapabilities is null")
            return techs
        }
        val technologyAvailability = rangingCapabilities.get()!!.technologyAvailability
        val dbgMessage = StringBuilder("getRangingTechnologies: ")
        for ((techId, availability) in technologyAvailability) {
            if (availability == RangingCapabilities.ENABLED) {
                val techName = getRangingTechnologyName(techId)
                dbgMessage.append(techName).append(", ")
                techs.add(techName)
            }
        }

        // Always add OOB
        val techName = ChannelSoundingRangingParameters.Technology.OOB.toString()
        dbgMessage.append(techName).append(", ")
        techs.add(techName)
        Timber.tag(TAG).d("Tech $dbgMessage")
        return techs
    }

    fun getMeasurementFrequencies(): List<String> = listOf(
        ChannelSoundingRangingParameters.Freq.HIGH.toString(),
        ChannelSoundingRangingParameters.Freq.MEDIUM.toString(),
        ChannelSoundingRangingParameters.Freq.LOW.toString()
    )

    fun getMeasureDurationsInIntervalRounds(): List<String> =
        listOf(
            activity.getString(R.string.channel_sounding_duration_10000),
            activity.getString(R.string.channel_sounding_duration_1000),
            activity.getString(R.string.channel_sounding_duration_100),
            activity.getString(R.string.channel_sounding_duration_10),
            activity.getString(R.string.channel_sounding_duration_5),
            activity.getString(R.string.channel_sounding_duration_0)
        )

    fun getSensorFusionEnable(): List<String> = listOf(
        activity.getString(R.string.channel_sounding_sensor_fusion_enable_true),
        activity.getString(R.string.channel_sounding_sensor_fusion_enable_false)
    )

    fun getLocationTypes(): List<String> =
        listOf(
            activity.getString(R.string.channel_sounding_location_type_unknown),
            activity.getString(R.string.channel_sounding_location_type_indoor),
            activity.getString(R.string.channel_sounding_location_type_outdoor)
        )

    fun getSightType(): List<String> =
        listOf(
            activity.getString(R.string.channel_sounding_sight_type_unknown),
            activity.getString(R.string.channel_sounding_sight_type_in_sight),
            activity.getString(R.string.channel_sounding_sight_type_out_of_sight)
        )

    private fun showBondAlertDialog() {
        try {

            val onDismissListener = DialogInterface.OnDismissListener {
                alertDialog?.dismiss()
            }
            val onClickListener = DialogInterface.OnClickListener { dialog, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        // Dismiss the current dialog
                        dialog.dismiss()

                        // Show alert dialog "Waiting for the device to pair"
                        MaterialAlertDialogBuilder(activity)
                            .setTitle(activity.getString(R.string.channel_sounding_waiting_for_pairing))
                            .setMessage(activity.getString(R.string.channel_sounding_waiting_for_pairing))
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok) { waitingDialog, _ ->
                                waitingDialog.dismiss()
                            }
                            .show()

                        Timber.tag(TAG).d("Initiating bond with ${targetDevice?.name}")
                        targetDevice?.createBond()?.let {
                            if (!it) {
                                Timber.tag(TAG)
                                    .d("Failed to initiate bond with ${targetDevice!!.name}")
                            }
                        }

                    }

                    else -> alertDialog?.dismiss()
                }
            }

            alertDialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.channel_sounding_alert_title)
                .setMessage(R.string.channel_sounding_alert_bond_message)
                .setCancelable(false)
                .setOnDismissListener(onDismissListener)
                .setNegativeButton(
                    R.string.channel_sounding_alert_bond_ignore, onClickListener
                ).setPositiveButton(R.string.channel_sounding_alert_bond_create, onClickListener)
                .show()

        } catch (e: SecurityException) {
            Timber.tag(TAG).e("showBondAlertDialog SecurityException: ${e.message}")
        }
    }


    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    @SuppressLint("MissingPermission")
    fun start(
        rangingTechnologyName: String,
        freqName: String,
        duration: Int
    ): Boolean {
        val tech = ChannelSoundingRangingParameters.Technology.fromName(rangingTechnologyName)
        val readiness = evaluateReadiness(tech)
        if (!readiness.ok) {
            Timber.tag(TAG).e("Cannot start ranging. Readiness failed: ${readiness.missingReasons}")
            callback.onStartFail(readiness.missingReasons)
            return false
        }
        if (!isTargetBondedIfRequired(tech)) {
            callback.onStartFail(listOf("CS Device not bonded"))
            return false
        }

        Timber.tag(TAG).d("CS Start ranging with device: ${targetDevice!!.name}")
        session = rangingManager.createRangingSession(
            Executors.newSingleThreadExecutor(), rangeSessionCallback
        )
        executor.execute {
            val rangingPreference = if (isResponder) {
                ChannelSoundingRangingParameters
                    .createResponderRangingPreference(
                        activity.application,
                        bleConnection,
                        rangingTechnologyName,
                        freqName,
                        ChannelSoundingConfigureParameters.restoreInstance(
                            activity.application, isResponder
                        ),
                        duration,
                        targetDevice!!
                    )
            } else {
                ChannelSoundingRangingParameters
                    .createInitiatorRangingPreference(
                        activity.application,
                        bleConnection,
                        rangingTechnologyName,
                        freqName,
                        ChannelSoundingConfigureParameters.restoreInstance(
                            activity.application, isResponder
                        ),
                        duration,
                        targetDevice!!
                    )
            }
            if (rangingPreference == null) {
                Timber.tag(TAG).d("CS Failed to start ranging session (rangingPreference is null)")
                callback.onStartFail(listOf("Ranging preference was null"))
                return@execute
            }

            cancellationSignal.set(session?.start(rangingPreference))

        }
        return true
    }


    private val rangeSessionCallback = @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    object : RangingSession.Callback {
        override fun onClosed(reason: Int) {
            Timber.tag(TAG).e("DistanceMeasurementManager onClosed! $reason")
            callback.onStop()
        }

        override fun onOpenFailed(reason: Int) {
            Timber.tag(TAG).e("DistanceMeasurementManager onOpenFailed! $reason")
            callback.onStartFail(listOf("Ranging session open failed reason=$reason"))
        }

        override fun onOpened() {
            Timber.tag(TAG).d("CS DistanceMeasurementManager onOpened! ")
        }

        override fun onResults(
            peer: RangingDevice,
            data: RangingData
        ) {
            Timber.tag(TAG)
                .d("DistanceMeasurementManager onResults ! $peer:$data")
            val measurement = data.distance?.measurement
            val confidence = data.distance?.confidence
            if (measurement == null) {
                Timber.tag(TAG).d("Distance measurement null in results")
                return
            }
            callback.onDistanceResult(measurement, confidence as Integer?)
        }

        override fun onStarted(peer: RangingDevice, technology: Int) {
            Timber.tag(TAG)
                .d("DistanceMeasurementManager onStarted ! ")
            callback.onStartSuccess()
        }

        override fun onStopped(peer: RangingDevice, technology: Int) {
            Timber.tag(TAG)
                .e("DistanceMeasurementManager onStopped!  $technology")
            callback.onStop()
        }

    }

    interface Callback {
        fun onStartSuccess()
        fun onStartFail(reasons: List<String> = emptyList())
        fun onStop()
        fun onDistanceResult(distanceMeter: Double?, confidence: Integer?)
    }

    companion object {
        private val TAG = ChannelSoundingDistanceMeasurementManager::class.java.toString()

        // Helper for readable logging / potential external checks
        fun readinessSummary(r: Readiness): String =
            "ok=${r.ok}; perms(ranging=${r.hasRangingPermission}, btConnect=${r.hasBluetoothConnectPermission}); " +
                    "targetSet=${r.targetDeviceSet}; bonded=${r.targetBondedIfNeeded}; caps=${r.capabilitiesAvailable}; techSupported=${r.selectedTechnologySupported}; missing=${r.missingReasons}"

        @JvmStatic
        fun restoreInstance() {
        }

        @JvmStatic
        fun resetInstance() {
        }
    }
}
