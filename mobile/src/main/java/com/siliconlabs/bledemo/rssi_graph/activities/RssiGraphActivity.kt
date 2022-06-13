package com.siliconlabs.bledemo.rssi_graph.activities

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_OPEN_DOCUMENT_TREE
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.SystemClock
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.Base.BaseActivity
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.rssi_graph.*
import com.siliconlabs.bledemo.rssi_graph.adapters.DeviceLabelAdapter
import com.siliconlabs.bledemo.rssi_graph.dialog_fragments.FilterDialogFragment
import com.siliconlabs.bledemo.rssi_graph.dialog_fragments.SortDialogFragment
import com.siliconlabs.bledemo.rssi_graph.model.ScannedDevice
import com.siliconlabs.bledemo.rssi_graph.utils.GraphDataExporter
import com.siliconlabs.bledemo.rssi_graph.viewmodels.RssiGraphViewModel
import com.siliconlabs.bledemo.rssi_graph.views.ChartView
import kotlinx.android.synthetic.main.activity_rssi_graph.*
import timber.log.Timber
import java.text.DateFormat
import java.util.*
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class RssiGraphActivity : BaseActivity() {

    private lateinit var bluetoothScanner: BluetoothLeScanner

    private lateinit var viewModel: RssiGraphViewModel

    private lateinit var chartView: ChartView
    private lateinit var labelAdapter: DeviceLabelAdapter

    private var executor: ScheduledThreadPoolExecutor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rssi_graph)
        viewModel = ViewModelProvider(this).get(RssiGraphViewModel::class.java)
        chartView = ChartView(this, window.decorView.rootView, timeArrowHandler)

        bluetoothScanner = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter.bluetoothLeScanner

        prepareToolbar()
        registerReceiver(bluetoothStateChangeListener, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        setupRecyclerView()
        setupUiObservers()
        setupViewModelObservers()
        chartView.initialize()
    }

    private fun prepareToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        findViewById<View>(R.id.iv_go_back).setOnClickListener { onBackPressed() }
    }

    private fun setupRecyclerView() {
        rssi_legend_view?.apply {
            adapter = DeviceLabelAdapter(this@RssiGraphActivity, legendClickHandler)
            layoutManager = LinearLayoutManager(this@RssiGraphActivity).apply {
                orientation = RecyclerView.HORIZONTAL
            }
            labelAdapter = adapter as DeviceLabelAdapter
        }
    }

    private fun setupUiObservers() {
        rssi_graph_btn_scanning.setOnClickListener { viewModel.toggleScanningState() }
        rssi_graph_btn_export.setOnClickListener {
            startActivityForResult(Intent(ACTION_OPEN_DOCUMENT_TREE), EXPORT_TO_CSV_CODE)
            Toast.makeText(this, getString(R.string.rssi_graph_file_location_chooser),
                    Toast.LENGTH_SHORT).show()
        }
        rssi_graph_time_arrow_end.setOnClickListener { chartView.skipToEnd() }
        rssi_graph_time_arrow_start.setOnClickListener { chartView.skipToStart()  }
    }

    private fun setupViewModelObservers() {
        viewModel.apply {
            isScanning.observe(this@RssiGraphActivity, Observer { toggleScanning(it) })
            anyDevicesFound.observe(this@RssiGraphActivity, Observer { showLabel(it) })
            newMatchingDevice.observe(this@RssiGraphActivity, Observer {
                labelAdapter.addNewDeviceLabel(it)
                chartView.addNewChartData(it)
            })
            activeFilters.observe(this@RssiGraphActivity, Observer {
                filterScannedDevices()
                labelAdapter.updateAllLabels(viewModel.filteredDevices)
                chartView.updateChartData(viewModel.filteredDevices, viewModel.highlightedDevice.value)

            })
            activeSortMode.observe(this@RssiGraphActivity, Observer {
                labelAdapter.updateSortMode(it)
                labelAdapter.updateAllLabels(filteredDevices)
            })
            highlightedDevice.observe(this@RssiGraphActivity, Observer {
                labelAdapter.updateHighlightedDevice(it)
                chartView.updateChartData(filteredDevices, it)
            })
        }
    }

    private val bluetoothStateChangeListener: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_OFF) {
                    Toast.makeText(this@RssiGraphActivity,
                            getString(R.string.bluetooth_disabled), Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }


    override fun onStop() {
        super.onStop()
        viewModel.setScanningState(false)
    }
    override fun onDestroy() {
        super.onDestroy()
        executor?.shutdown()
        bluetoothScanner.stopScan(bluetoothScanCallback)
        unregisterReceiver(bluetoothStateChangeListener)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.rssi_graph_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.rssi_filter_icon -> {
                FilterDialogFragment().show(supportFragmentManager, FILTER_FRAGMENT_TAG)
                true
            }
            R.id.rssi_sort_icon -> {
                SortDialogFragment().show(supportFragmentManager, SORT_FRAGMENT_TAG)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                EXPORT_TO_CSV_CODE -> onExportLocationChosen(data)
            }
        }
    }

    private fun onExportLocationChosen(intent: Intent?) {
        intent?.data?.let {
            DocumentFile.fromTreeUri(applicationContext, it)?.let { dirLocation ->
                dirLocation.createFile("text/csv", createFileName())?.let { fileLocation ->
                    val fileContent = GraphDataExporter().export(
                            viewModel.filteredDevices,
                            viewModel.exportTimestamp,
                            chartView.startScanTimestamp)
                    contentResolver.openOutputStream(fileLocation.uri)?.let { stream ->
                        stream.write(fileContent.toByteArray())
                        showShortToast(R.string.gatt_configurator_toast_export_successful)
                    } ?: showShortToast(R.string.gatt_configurator_toast_export_unsuccessful)
                } ?: showLongToast(R.string.rssi_graph_file_creation_unsuccessful)
            } ?: showLongToast(R.string.toast_export_wrong_location_chosen)
        } ?: showLongToast(R.string.toast_export_wrong_location_chosen)
    }

    private fun createFileName() : String {
        val calendar = Calendar.getInstance()
        val dateFormatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).apply {
            timeZone = calendar.timeZone
        }
        return "graph-data-${dateFormatter.format(calendar.time)}.csv"
    }

    private fun showLongToast(@StringRes message: Int) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showShortToast(@StringRes message: Int) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun toggleScanning(isOn: Boolean) {
        if (isOn) {
            val filters = listOf<ScanFilter>()
            val settings = ScanSettings.Builder()
                    .setLegacy(false)
                    .setReportDelay(0)
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()

            chartView.reset()
            viewModel.reset()
            labelAdapter.resetAllLabels()
            scheduleExecutorTasks()

            viewModel.exportTimestamp = System.currentTimeMillis()
            chartView.startScanTimestamp = SystemClock.elapsedRealtimeNanos()
            bluetoothScanner.startScan(filters, settings, bluetoothScanCallback)
            setScanningButtonStop()
            rssi_graph_btn_export.isEnabled = false
            showLabel(false)
        } else {
            executor?.shutdown()
            bluetoothScanner.stopScan(bluetoothScanCallback)
            setScanningButtonStart()
            rssi_graph_btn_export.isEnabled = true
            rssi_graph_time_arrow_start.visibility = View.VISIBLE
            rssi_graph_time_arrow_end.visibility = View.VISIBLE
        }
    }

    private fun scheduleExecutorTasks() {
        executor = ScheduledThreadPoolExecutor(2).also {
            try {
                it.scheduleWithFixedDelay(drawGraphRunnable, 0, GRAPH_REFRESH_PERIOD, TimeUnit.MILLISECONDS)
                it.scheduleWithFixedDelay(refilterGraphRunnable, GRAPH_REFILTER_INITIAL_DELAY,
                        GRAPH_REFILTER_PERIOD, TimeUnit.MILLISECONDS)
            } catch (ex: RejectedExecutionException) {
                Timber.e("Scheduling for executor failed!")
            }
        }
    }

    private fun showLabel(withDevices: Boolean) {
        if (withDevices) {
            rssi_label_description.visibility = View.GONE
            rssi_label_with_devices.visibility = View.VISIBLE
        } else {
            rssi_label_description.visibility = View.VISIBLE
            rssi_label_with_devices.visibility = View.GONE
            rssi_label_description.text = getString(R.string.rssi_graph_no_devices_found_label)
        }
    }

    private val legendClickHandler = object : DeviceLabelAdapter.LegendClickHandler {
        override fun onLegendItemClicked(device: ScannedDevice?) {
            viewModel.handleOnLegendItemClick(device)
        }
    }

    private val timeArrowHandler = object : ChartView.TimeArrowsHandler {
        override fun handleVisibility(isStartArrowVisible: Boolean, isEndArrowVisible: Boolean) {
            runOnUiThread {
                rssi_graph_time_arrow_start.visibility =
                        if (isStartArrowVisible) View.VISIBLE
                        else View.INVISIBLE
                rssi_graph_time_arrow_end.visibility =
                        if (isEndArrowVisible) View.VISIBLE
                        else View.INVISIBLE
            }
        }
    }


    private val drawGraphRunnable = Runnable {
        chartView.updateChartData(viewModel.filteredDevices, viewModel.highlightedDevice.value)
    }

    private val refilterGraphRunnable = Runnable {
        if (viewModel.activeSortMode.value != SortDialogFragment.SortMode.NONE ||
                viewModel.activeFilters.value?.isNotEmpty() == true) {
            viewModel.filterScannedDevices()
            runOnUiThread { labelAdapter.updateAllLabels(viewModel.filteredDevices) }
            chartView.updateChartData(viewModel.filteredDevices, viewModel.highlightedDevice.value)
        }
    }

    private val bluetoothScanCallback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            Thread {
                result?.let { viewModel.handleScanResult(it) }
            }.start()

        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            Thread {
                results?.forEach { viewModel.handleScanResult(it) }
            }.start()
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Timber.e("Scan failed! Error code = $errorCode")
            Toast.makeText(this@RssiGraphActivity, getString(R.string.rssi_graph_scan_failed), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setScanningButtonStart() {
        runOnUiThread {
            rssi_graph_btn_scanning.text = resources.getString(R.string.button_start_scanning)
            rssi_graph_btn_scanning.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.silabs_blue))
        }
    }

    private fun setScanningButtonStop() {
        runOnUiThread {
            rssi_graph_btn_scanning.text = resources.getString(R.string.button_stop_scanning)
            rssi_graph_btn_scanning.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.silabs_red))
        }
    }

    companion object {
        private const val GRAPH_REFRESH_PERIOD = 250L // ms
        private const val GRAPH_REFILTER_PERIOD = 5000L // ms
        private const val GRAPH_REFILTER_INITIAL_DELAY = 5000L // ms
        private const val SORT_FRAGMENT_TAG = "sort_fragment"
        private const val FILTER_FRAGMENT_TAG = "filter_fragment"
        private const val EXPORT_TO_CSV_CODE = 201
    }
}