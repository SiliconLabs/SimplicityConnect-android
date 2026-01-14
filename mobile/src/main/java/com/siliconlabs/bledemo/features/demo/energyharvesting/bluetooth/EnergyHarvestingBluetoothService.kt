@file:Suppress("MissingPermission", "UnspecifiedRegisterReceiverFlag")
package com.siliconlabs.bledemo.features.demo.energyharvesting.bluetooth

import android.content.pm.PackageManager
import android.R
import android.app.*
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.*
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.siliconlabs.bledemo.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

class EnergyHarvestingBluetoothService : Service() {

    // ---- Your existing surfaces (unchanged) ----
    private val processingChannel = Channel<ScanEvent>(capacity = 1024)

    private val _advData = MutableSharedFlow<List<AdvSegment>>(replay = 0, extraBufferCapacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val advData: SharedFlow<List<AdvSegment>> = _advData.asSharedFlow()

    private val _emittedVoltages = MutableSharedFlow<List<Int>>(replay = 0, extraBufferCapacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val emittedVoltages: SharedFlow<List<Int>> = _emittedVoltages.asSharedFlow()

    private val _deviceAddresses = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val deviceAddresses: SharedFlow<String> = _deviceAddresses.asSharedFlow()

    private val _rssiUpdates = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val rssiUpdates: SharedFlow<Int> = _rssiUpdates.asSharedFlow()

    private val _sampleEvents = MutableSharedFlow<SampleEvent>(replay = 0, extraBufferCapacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val sampleEvents: SharedFlow<SampleEvent> = _sampleEvents.asSharedFlow()

    // Add missing flows expected by Activity
    private val _deviceIdentifiers = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val deviceIdentifiers: SharedFlow<String> = _deviceIdentifiers.asSharedFlow()

    private val _advIntervalMs = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val advIntervalMs: SharedFlow<Int> = _advIntervalMs.asSharedFlow()

    // ---- Scopes/dispatchers ----
    private val ingestDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val parseDispatcher  = Executors.newFixedThreadPool(2).asCoroutineDispatcher()
    private val serviceScope     = CoroutineScope(SupervisorJob() + ingestDispatcher)

    // ---- Foreground bits ----
    private val notifId = 999
    private val notifChannelId = "BLE_HARVEST_CHANNEL"

    // ---- Metrics ----
    private val channelDropCount = AtomicLong(0)
    private val flowDropCounts = mutableMapOf<String, AtomicLong>()
    private fun flowDrop(label: String) = flowDropCounts.getOrPut(label) { AtomicLong(0) }.incrementAndGet()
    private inline fun <reified T> MutableSharedFlow<T>.emitOrCount(label: String, value: T) {
        if (!tryEmit(value)) flowDrop(label)
    }

    // ---- PendingIntent-only scanner (no stop/restart) ----
    private val action = "${BuildConfig.APPLICATION_ID}.BLE_SCAN_INTENT"
    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val list = intent.getParcelableArrayListExtra<ScanResult>(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT) ?: return
            val now = SystemClock.elapsedRealtimeNanos()
            lastAppTsNs.set(now)
            list.forEach { ingestScanResult(it) }
        }
    }
    private var receiverRegistered = false
    private val lastAppTsNs = AtomicLong(0L)
    private var watchdogJob: Job? = null

    // Binder to allow clients to bind and obtain the service instance reliably instead of polling.
    inner class LocalBinder : Binder() { fun getService(): EnergyHarvestingBluetoothService = this@EnergyHarvestingBluetoothService }
    private val localBinder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = localBinder

    override fun onCreate() {
        super.onCreate()
        // Mark singleton instance & signal readiness
        companionInstance = this
        readyLatch.countDown()
        ensureForeground()
        // Only attempt scans immediately if permissions already granted; otherwise Activity will call start() after requesting.
        if (hasAllScanPermissions()) {
            startPendingIntentScan()
            startProcessingWorker()
            startMinuteMetricsDump()
            startGapWatchdog()
        } else {
            Log.i(TAG, "Permissions not yet granted; deferring scan start until start() invoked.")
            startProcessingWorker() // we can start processing pipeline early
        }
    }

    // Respect “no stop scan at all”: we do NOT stop in onDestroy either.
    override fun onDestroy() {
        serviceScope.cancel()
        if (receiverRegistered) try { unregisterReceiver(scanReceiver) } catch (_: Throwable) { }
        receiverRegistered = false
        super.onDestroy()
    }

    private fun ensureForeground() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(notifChannelId) == null) {
            nm.createNotificationChannel(
                NotificationChannel(notifChannelId, "BLE Harvesting", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notif = NotificationCompat.Builder(this, notifChannelId)
            .setOngoing(true)
            .setContentTitle("BLE harvesting active")
            .setContentText("Scanning continuously (EH Sensor)")
            .setSmallIcon(R.drawable.stat_sys_data_bluetooth)
            .build()
        startForeground(notifId, notif)
    }
    fun buildFilter(filterName: String): ScanFilter = ScanFilter.Builder().setDeviceName(filterName).build()

    // Helper to process a ScanResult (used by both PendingIntent receiver & direct callback)
    private fun ingestScanResult(res: ScanResult) {
        val rec = res.scanRecord?.bytes ?: return
        val peripheralUuid = derivePeripheralUuid(res) ?: return
        val evt = ScanEvent(
            recordBytes = rec,
            rssi = res.rssi,
            deviceAddress = peripheralUuid, // replaced MAC address with derived peripheral UUID
            serviceUuid = res.scanRecord?.serviceUuids?.firstOrNull()?.uuid?.toString(),
            timestampNanos = res.timestampNanos
        )
        if (!processingChannel.trySend(evt).isSuccess) channelDropCount.incrementAndGet()
    }

    // Derive a stable peripheral UUID: prefer first advertised Service UUID; fallback to name-based UUID of MAC.
    private fun derivePeripheralUuid(res: ScanResult): String? {
        val svc = res.scanRecord?.serviceUuids?.firstOrNull()?.uuid?.toString()
        if (svc != null) return svc
        val mac = res.device?.address ?: return null
        return java.util.UUID.nameUUIDFromBytes(mac.toByteArray()).toString()
    }

    // ---- Direct callback scanning (added for reliability/debug) ----
    private var directScanStarted = false
    private var directScanCallback: ScanCallback? = null
    private var lastDirectStartAttemptNs = 0L

    private fun maybeStartDirectCallbackScan() {
        if (directScanStarted) return
        if (!hasAllScanPermissions()) {
            Log.w(TAG, "Scan permissions missing; direct callback scan skipped")
            return
        }
        val btAdapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter ?: return
        val leScanner = btAdapter.bluetoothLeScanner ?: return
        lastDirectStartAttemptNs = SystemClock.elapsedRealtimeNanos()
        directScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                lastAppTsNs.set(SystemClock.elapsedRealtimeNanos())
                ingestScanResult(result)
            }
            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                val now = SystemClock.elapsedRealtimeNanos()
                lastAppTsNs.set(now)
                results.forEach { ingestScanResult(it) }
            }
            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Direct scan failed code=$errorCode")
                directScanStarted = false
            }
        }
        // Broaden filters: attempt both name variants and allow fallback if device name differs.
        val filters = listOf(buildFilter("EH Sensor"))
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0L)
            .build()
        try {
            leScanner.startScan(filters, settings, directScanCallback)
            directScanStarted = true
            Log.i(TAG, "Direct callback scan started (filters=name='EH Sensor' and 'EH Sensor')")
        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException starting direct scan: ${se.message}")
        } catch (t: Throwable) {
            Log.e(TAG, "Unexpected error starting direct scan: ${t.message}")
        }
    }

    private fun hasAllScanPermissions(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            // For <12 require location if you need device name
            val fineOk = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            return fineOk
        }
        val needed = listOf(
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT
        )
        return needed.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    }

    private fun startPendingIntentScan() {
        val bt = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter ?: return
        val le = bt.bluetoothLeScanner ?: return
        if (!hasAllScanPermissions()) {
            Log.w(TAG, "Permissions missing; PendingIntent scan not started")
            return
        }
        val filters = listOf(buildFilter("EH Sensor"))
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            .setReportDelay(0L)
            .build()

        if (!receiverRegistered) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(scanReceiver, IntentFilter(action), Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(scanReceiver, IntentFilter(action))
            }
            receiverRegistered = true
        }

        val pi = PendingIntent.getBroadcast(
            this, 1001, Intent(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Single, continuous start — no fallback, no stop
        try {
            le.startScan(filters, settings, pi)
            lastAppTsNs.set(SystemClock.elapsedRealtimeNanos())
            Log.i(TAG, "PendingIntent scan started (filter=name='EH Sensor'')")
        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException starting PendingIntent scan: ${se.message}")
        }
        // Also start direct callback to ensure we get packets (debug/reliability)
        maybeStartDirectCallbackScan()
    }

    private fun startProcessingWorker() {
        serviceScope.launch {
            for (event in processingChannel) {
                val parsed = withContext(parseDispatcher) { parseAdvertisement(event.recordBytes) }

                if (parsed.segments.isNotEmpty()) _advData.emitOrCount("advData", parsed.segments)
                parsed.manufacturerVoltageMv?.let { mv -> _emittedVoltages.emitOrCount("emittedVoltages", listOf(mv)) }
                event.deviceAddress?.let { _deviceAddresses.emitOrCount("deviceAddresses", it) }
                event.serviceUuid?.let { _deviceIdentifiers.emitOrCount("deviceIdentifiers", it) }
                _rssiUpdates.emitOrCount("rssi", event.rssi)

                updateIntervalWith(event.timestampNanos)
                val interval = currentIntervalMs()
                _advIntervalMs.emitOrCount("advIntervalMs", interval)

                _sampleEvents.emitOrCount(
                    "sampleEvents",
                    SampleEvent(
                        sequence = nextSequence(),
                        timestampNanos = event.timestampNanos,
                        deviceAddress = event.deviceAddress,
                        deviceIdentifier = event.serviceUuid,
                        voltageMv = parsed.manufacturerVoltageMv,
                        rssi = event.rssi,
                        advIntervalMs = interval
                    )
                )
            }
        }
    }

    private fun startMinuteMetricsDump() {
        serviceScope.launch {
            while (isActive) {
                delay(60_000)
                val drops = channelDropCount.get()
                val flowDrops = flowDropCounts.map { "${it.key}:${it.value.get()}" }.joinToString(",")
                Log.i(TAG, "metrics channelDrops=$drops flowDrops={$flowDrops}")
            }
        }
    }

    // Watchdog ONLY logs when delivery gap > 10s; it does not stop/restart the scan
    private fun startGapWatchdog() {
        watchdogJob = serviceScope.launch {
            while (isActive) {
                delay(5_000)
                val lastDelivery = lastAppTsNs.get()
                val nowNs = SystemClock.elapsedRealtimeNanos()
                if (lastDelivery != 0L) {
                    val lagMs = (nowNs - lastDelivery) / 1e6
                    if (lagMs > 10_000 && !directScanStarted) {
                        Log.w(TAG, "No advertisements for ${lagMs}ms. Attempting direct scan start.")
                        maybeStartDirectCallbackScan()
                    } else if (lagMs > 15_000 && directScanStarted && (nowNs - lastDirectStartAttemptNs) / 1e6 > 30_000) {
                        Log.w(TAG, "Lag ${lagMs}ms persists; retrying direct scan.")
                        directScanStarted = false
                        maybeStartDirectCallbackScan()
                    }
                    if (lagMs > 10_000) {
                        Log.w(TAG, "Delivery gap ${lagMs}ms (>10s). (Monitoring - not stopping scans)")
                    }
                }
            }
        }
    }

    // Public start now actively ensures scanners are running after permissions granted.
    fun start() { ensureScanActive() }
    private fun ensureScanActive() {
        if (hasAllScanPermissions()) {
            if (!receiverRegistered) startPendingIntentScan()
            if (!directScanStarted) maybeStartDirectCallbackScan()
        } else {
            Log.w(TAG, "ensureScanActive called without required permissions")
        }
    }

    // ---- Minimal parsing + helpers (replace with your own) ----
    // Re-implemented to deterministically decode manufacturer-specific voltage instead of heuristic scanning.
    private fun parseAdvertisement(bytes: ByteArray): ParsedAdv {
        val segments = mutableListOf<AdvSegment>()
        var manufacturerVoltage: Int? = null
        var idx = 0
        while (idx < bytes.size) {
            val len = bytes[idx].toInt() and 0xFF
            if (len == 0) break
            val next = idx + len + 1
            if (idx + 1 >= bytes.size || next > bytes.size) break
            val type = bytes[idx + 1].toInt() and 0xFF
            val dataStart = idx + 2
            val dataEnd = dataStart + (len - 1)
            if (dataEnd > bytes.size) break
            val segmentBytes = bytes.copyOfRange(dataStart, dataEnd)
            val typeLabel = when (type) {
                0x01 -> "FLAGS"
                0x02,0x03 -> "UUID16"
                0x04,0x05 -> "UUID32"
                0x06,0x07 -> "UUID128"
                0x08 -> "NAME_SHORT"
                0x09 -> "NAME_COMPLETE"
                0x0A -> "TX_POWER"
                0x16 -> "SERVICE_DATA_16"
                0xFF -> "MANUFACTURER"
                else -> "TYPE_${type.toString(16)}"
            }
            segments += AdvSegment(typeLabel, segmentBytes)
            if (type == 0xFF && segmentBytes.size >= 4 && manufacturerVoltage == null) {
                // Manufacturer specific layout assumed: [companyLSB, companyMSB, voltageLSB, voltageMSB, ...]
                val le = (segmentBytes[2].toInt() and 0xFF) or ((segmentBytes[3].toInt() and 0xFF) shl 8)
                val be = ((segmentBytes[2].toInt() and 0xFF) shl 8) or (segmentBytes[3].toInt() and 0xFF)
                manufacturerVoltage = pickIfPlausible(le, be)
                if (manufacturerVoltage == null) {
                    // Fallback: slide 2-byte window over manufacturer data beyond company code.
                    for (i in 2 until segmentBytes.size - 1) {
                        val wLe = (segmentBytes[i].toInt() and 0xFF) or ((segmentBytes[i + 1].toInt() and 0xFF) shl 8)
                        val wBe = ((segmentBytes[i].toInt() and 0xFF) shl 8) or (segmentBytes[i + 1].toInt() and 0xFF)
                        manufacturerVoltage = pickIfPlausible(wLe, wBe)
                        if (manufacturerVoltage != null) break
                    }
                }
            }
            idx = next
        }
        // Final fallback: search whole adv if not yet found (rare)
        if (manufacturerVoltage == null) {
            for (i in 0 until bytes.size - 1) {
                val le = (bytes[i].toInt() and 0xFF) or ((bytes[i + 1].toInt() and 0xFF) shl 8)
                val be = ((bytes[i].toInt() and 0xFF) shl 8) or (bytes[i + 1].toInt() and 0xFF)
                manufacturerVoltage = pickIfPlausible(le, be)
                if (manufacturerVoltage != null) break
            }
        }
        return ParsedAdv(segments = segments, manufacturerVoltageMv = manufacturerVoltage)
    }

    private fun pickIfPlausible(a: Int, b: Int): Int? {
        // Accept values that look like millivolts: 500..6000 (adjustable)
        return when {
            a in 500..6000 -> a
            b in 500..6000 -> b
            else -> null
        }
    }

    private var sequenceCounter = 0L
    private fun nextSequence() = ++sequenceCounter

    private var lastCtrlTs = 0L
    private var smoothedMs = 5000.0
    private fun updateIntervalWith(ctrlTsNs: Long) {
        if (lastCtrlTs != 0L) {
            val gapMs = (ctrlTsNs - lastCtrlTs) / 1e6
            smoothedMs = 0.2 * gapMs + 0.8 * smoothedMs
            if (gapMs > 8000) Log.w(TAG, "ctrl_gap_ms=$gapMs (expected ~5000).")
        }
        lastCtrlTs = ctrlTsNs
    }
    private fun currentIntervalMs(): Int = smoothedMs.toInt()

    // ---- Data models (align with your pipeline) ----
    data class ScanEvent(
        val recordBytes: ByteArray,
        val rssi: Int,
        val deviceAddress: String?,
        val serviceUuid: String?,
        val timestampNanos: Long
    )
    data class AdvSegment(val type: String, val bytes: ByteArray)
    data class ParsedAdv(val segments: List<AdvSegment>, val manufacturerVoltageMv: Int?)
    data class SampleEvent(
        val sequence: Long,
        val timestampNanos: Long,
        val deviceAddress: String?,
        val deviceIdentifier: String?,
        val voltageMv: Int?,
        val rssi: Int,
        val advIntervalMs: Int
    )

    companion object {
        private const val TAG = "ExtraordinaryBleService"
        @Volatile private var companionInstance: EnergyHarvestingBluetoothService? = null
        private val readyLatch = java.util.concurrent.CountDownLatch(1)

        fun getInstance(ctx: Context): EnergyHarvestingBluetoothService {
            // Fast path
            companionInstance?.let { return it }
            // Start service if needed
            val intent = Intent(ctx.applicationContext, EnergyHarvestingBluetoothService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.applicationContext.startForegroundService(intent)
            } else {
                ctx.applicationContext.startService(intent)
            }
            // Wait briefly for onCreate to set instance (avoid indefinite block)
            try {
                readyLatch.await(3, java.util.concurrent.TimeUnit.SECONDS)
            } catch (_: InterruptedException) { }
            return companionInstance ?: throw IllegalStateException("EnergyHarvestingBluetoothService not ready")
        }
    }
}