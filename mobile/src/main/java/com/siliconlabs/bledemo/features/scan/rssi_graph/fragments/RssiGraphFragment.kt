package com.siliconlabs.bledemo.features.scan.rssi_graph.fragments

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_OPEN_DOCUMENT_TREE
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.home_screen.viewmodels.ScanFragmentViewModel
import com.siliconlabs.bledemo.features.scan.browser.view_states.GraphFragmentViewState
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.FragmentGraphBinding
import com.siliconlabs.bledemo.home_screen.base.BaseServiceDependentMainMenuFragment
import com.siliconlabs.bledemo.home_screen.fragments.ScanFragment
import com.siliconlabs.bledemo.home_screen.base.ViewPagerFragment
import com.siliconlabs.bledemo.features.scan.rssi_graph.adapters.GraphLabelAdapter
import com.siliconlabs.bledemo.features.scan.rssi_graph.utils.GraphDataExporter
import com.siliconlabs.bledemo.features.scan.rssi_graph.views.ChartView
import com.siliconlabs.bledemo.home_screen.base.BluetoothDependent
import com.siliconlabs.bledemo.home_screen.base.LocationDependent
import java.text.DateFormat
import java.util.*
import java.util.concurrent.ScheduledThreadPoolExecutor

class RssiGraphFragment : BaseServiceDependentMainMenuFragment() {

    private lateinit var viewModel: ScanFragmentViewModel
    private lateinit var _binding: FragmentGraphBinding

    private lateinit var chartView: ChartView
    private lateinit var labelAdapter: GraphLabelAdapter

    private var isInitialBluetoothStateObserved = false
    private var isInitialGraphStateLoaded = false // for optimization purposes when entering graph fragment

    private val handler = Handler(Looper.getMainLooper())

    private var executor: ScheduledThreadPoolExecutor? = null


    private fun getScanFragment() = (parentFragment as ViewPagerFragment).getScanFragment()


    override val bluetoothDependent = object : BluetoothDependent {

        override fun onBluetoothStateChanged(isBluetoothOn: Boolean) {
            toggleBluetoothBar(isBluetoothOn, _binding.bluetoothBar)
            _binding.rssiGraphBtnScanning.isEnabled = isBluetoothOperationPossible()
            if (isInitialBluetoothStateObserved) {
                /* LiveData sends its initial value to an observer which starts scan when creating
                 this fragment - even if global scanning state is false. This flag prevents that. */
                viewModel.setIsScanningOn(isBluetoothOperationPossible())
            }

            if (!isBluetoothOn) {
                viewModel.reset()
                chartView.reset()
            }
            isInitialBluetoothStateObserved = true
        }

        override fun onBluetoothPermissionsStateChanged(arePermissionsGranted: Boolean) {
            toggleBluetoothPermissionsBar(arePermissionsGranted, _binding.bluetoothPermissionsBar)
            _binding.rssiGraphBtnScanning.isEnabled = isBluetoothOperationPossible()

            if (!arePermissionsGranted) {
                viewModel.reset()
                chartView.reset()
            }
        }

        override fun refreshBluetoothDependentUi(isBluetoothOperationPossible: Boolean) {
            _binding.rssiGraphBtnScanning.isEnabled = isBluetoothOperationPossible
        }

        override fun setupBluetoothPermissionsBarButtons() {
            _binding.bluetoothPermissionsBar.setFragmentManager(childFragmentManager)
        }
    }

    override val locationDependent = object : LocationDependent {

        override fun onLocationStateChanged(isLocationOn: Boolean) {
            toggleLocationBar(isLocationOn, _binding.locationBar)
        }
        override fun onLocationPermissionStateChanged(isPermissionGranted: Boolean) {
            _binding.apply {
                toggleLocationPermissionBar(isPermissionGranted, locationPermissionBar)
                rssiGraphBtnScanning.isEnabled = isPermissionGranted
            }
        }
        override fun setupLocationBarButtons() {
            _binding.locationBar.setFragmentManager(childFragmentManager)
        }

        override fun setupLocationPermissionBarButtons() {
            _binding.locationPermissionBar.setFragmentManager(childFragmentManager)
        }
    }

    private val scanFragmentListener = object : ScanFragment.ScanFragmentListener {
        override fun onScanningStateChanged(isOn: Boolean) {
            if (isOn) chartView.updateTimestamp(viewModel.nanoTimestamp)
            else showGraphArrows()

            toggleRefreshGraphRunnable(isOn)
            toggleScanningViews(isOn)
        }
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = getScanFragment().viewModel
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        setHasOptionsMenu(true)
        _binding = FragmentGraphBinding.inflate(inflater)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = getString(R.string.fragment_scan_label)
        chartView = ChartView(requireContext(), _binding.rssiChart, timeArrowHandler, viewModel.nanoTimestamp)

        setupRecyclerView()
        setupUiObservers()
        setupViewModelObservers()
        chartView.initialize()

        if (viewModel.getIsScanningOn()) {
            handler.postDelayed( { toggleRefreshGraphRunnable(true) }, INITIALIZATION_DELAY)
        }
    }

    private fun setupRecyclerView() {
        labelAdapter = GraphLabelAdapter(viewModel.getLabelViewsState().toMutableList(), legendClickHandler)

        _binding.rssiLegendView.apply {
            adapter = labelAdapter
            layoutManager = LinearLayoutManager(requireContext()).apply {
                orientation = RecyclerView.HORIZONTAL
            }
        }
    }

    private fun setupUiObservers() {
        _binding.apply {
            rssiGraphBtnScanning.setOnClickListener {
                var animationDelay = 0L
                if (!viewModel.getIsScanningOn()) {
                    viewModel.reset()
                    chartView.reset()
                    animationDelay = ANIMATION_DELAY
                }
                handler.postDelayed({ viewModel.toggleScanningState() }, animationDelay)
            }
            rssiGraphTimeArrowEnd.setOnClickListener { chartView.skipToEnd() }
            rssiGraphTimeArrowStart.setOnClickListener { chartView.skipToStart()  }
        }
    }

    private fun setupViewModelObservers() {
        viewModel.apply {
            isAnyDeviceDiscovered.observe(viewLifecycleOwner) {
                toggleLabelView(it, viewModel.getIsScanningOn())
            }
            labelToInsert.observe(viewLifecycleOwner) {
                labelAdapter.addNewDeviceLabel(it)
            }
            activeFiltersDescription.observe(viewLifecycleOwner) {
                toggleFilterDescriptionView(it)
            }
            filteredDevices.observe(viewLifecycleOwner) {
                if (isInitialGraphStateLoaded) {
                    chartView.updateChartData(
                        viewModel.getGraphDevicesState(),
                        viewModel.highlightedLabel.value
                    )
                    labelAdapter.updateLabels(viewModel.getLabelViewsState())
                }
            }
            highlightedLabel.observe(viewLifecycleOwner) {
                if (isInitialGraphStateLoaded) {
                    labelAdapter.updateHighlightedDevice(it)
                    chartView.updateChartData(viewModel.getGraphDevicesState(), it)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        getScanFragment().apply {
            setScanFragmentListener(scanFragmentListener)
            refreshViewState(viewModel.getGraphFragmentViewState())
        }
    }

    private fun refreshViewState(viewState: GraphFragmentViewState) {
        toggleScanningViews(viewState.isScanningOn)
        labelAdapter.apply {
            updateLabels(viewState.labelsInfo)
            updateHighlightedDevice(viewState.highlightedLabel)
        }
        chartView.apply {
            if (viewModel.shouldResetChart) {
                reset()
                viewModel.shouldResetChart = false
            }
            updateTimestamp(viewState.scanTimestamp)
            updateChartData(viewState.graphInfo, viewState.highlightedLabel)
        }
        isInitialGraphStateLoaded = true
    }

    private fun toggleScanningViews(isScanningOn: Boolean) {
        toggleLabelView(viewModel.isAnyDeviceDiscovered.value ?: false, isScanningOn)
        toggleScanningButton(isScanningOn)
    }

    override fun onDestroy() {
        super.onDestroy()
        executor?.shutdown()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_rssi_graph, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.rssi_filter_icon -> {
                getScanFragment().toggleFilterFragment(shouldShowFilterFragment = true)
                true
            }
            R.id.rssi_sort_icon -> {
                viewModel.sortDevices()
                labelAdapter.updateLabels(viewModel.getLabelViewsState())
                _binding.rssiLabelWithDevices.smoothScrollTo(0, 0)
                Toast.makeText(requireContext(), getString(R.string.rssi_labels_sorted_by_descending_rssi),
                        Toast.LENGTH_SHORT).show()
                true
            }
            R.id.rssi_csv_export -> {
                if (viewModel.isAnyDeviceDiscovered.value == true) {
                    startActivityForResult(Intent(ACTION_OPEN_DOCUMENT_TREE), EXPORT_TO_CSV_CODE)
                    showShortToast(R.string.rssi_graph_file_location_chooser)
                } else {
                    showShortToast(R.string.no_graph_data_to_export)
                }
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
            DocumentFile.fromTreeUri(requireContext(), it)?.let { dirLocation ->
                dirLocation.createFile("text/csv", createFileName())?.let { fileLocation ->
                    val fileContent = GraphDataExporter().export(
                            viewModel.getExportDevicesState(),
                            viewModel.miliTimestamp,
                            viewModel.nanoTimestamp)
                    activity?.contentResolver?.openOutputStream(fileLocation.uri)?.let { stream ->
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
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun showShortToast(@StringRes message: Int) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }


    private fun toggleRefreshGraphRunnable(isOn: Boolean) {
        handler.let {
            if (isOn) {
                it.removeCallbacks(drawGraphRunnable)
                it.post(drawGraphRunnable)
            } else {
                it.removeCallbacks(drawGraphRunnable)
            }
        }
    }

    private fun toggleLabelView(isAnyDeviceDiscovered: Boolean, isScanningOn: Boolean) {
        _binding.apply {
            if (isAnyDeviceDiscovered) {
                rssiLabelDescription.visibility = View.GONE
                rssiLabelWithDevices.visibility = View.VISIBLE
            } else {
                rssiLabelDescription.visibility = View.VISIBLE
                rssiLabelWithDevices.visibility = View.GONE
                rssiLabelDescription.text = getString(
                        if (isScanningOn) R.string.device_scanning_background_message
                        else R.string.no_devices_found_title_copy
                )

            }
        }
    }

    private fun toggleFilterDescriptionView(description: String?) {
        _binding.activeFiltersDescription.apply {
            description?.let {
                visibility = View.VISIBLE
                text = it
            } ?: run { visibility = View.GONE }
        }
    }

    private val legendClickHandler = object : GraphLabelAdapter.LegendClickHandler {
        override fun onLegendItemClicked(device: ScanFragmentViewModel.LabelViewState?) {
            viewModel.handleOnLegendItemClick(device)
        }
    }

    private val timeArrowHandler = object : ChartView.TimeArrowsHandler {
        override fun handleVisibility(isStartArrowVisible: Boolean, isEndArrowVisible: Boolean) {
            //runOnUiThread {
                _binding.rssiGraphTimeArrowStart.visibility =
                        if (isStartArrowVisible) View.VISIBLE
                        else View.INVISIBLE
                _binding.rssiGraphTimeArrowEnd.visibility =
                        if (isEndArrowVisible) View.VISIBLE
                        else View.INVISIBLE
            //}
        }
    }

    private fun showGraphArrows() {
        _binding.apply {
            rssiGraphTimeArrowStart.visibility = View.VISIBLE
            rssiGraphTimeArrowEnd.visibility = View.VISIBLE
        }
    }


    private val drawGraphRunnable = object : Runnable {
        override fun run() {
            chartView.updateChartData(viewModel.getGraphDevicesState(), viewModel.highlightedLabel.value)
            handler.postDelayed(this, GRAPH_REFRESH_PERIOD)
        }
    }

    private fun toggleScanningButton(isScanningOn: Boolean) {
        _binding.rssiGraphBtnScanning.apply {
            text = getString(
                    if (isScanningOn) R.string.button_stop_scanning
                    else R.string.button_start_scanning
            )
            setIsActionOn(isScanningOn)
        }
    }

    companion object {
        private const val GRAPH_REFRESH_PERIOD = 250L // ms
        private const val ANIMATION_DELAY = 250L // give user a chance to see what's going on
        private const val INITIALIZATION_DELAY = 750L // smooth user experience when creating fragment
        private const val EXPORT_TO_CSV_CODE = 201
    }
}