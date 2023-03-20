package com.siliconlabs.bledemo.features.scan.browser.fragments

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.SeekBar
import androidx.fragment.app.DialogFragment
import com.siliconlabs.bledemo.bluetooth.beacon_utils.BleFormat
import com.siliconlabs.bledemo.home_screen.viewmodels.ScanFragmentViewModel
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.FragmentFilterBinding
import com.siliconlabs.bledemo.home_screen.fragments.ScanFragment
import com.siliconlabs.bledemo.utils.FilterDeviceParams
import kotlinx.android.synthetic.main.fragment_filter.*
import java.util.*

class FilterFragment : DialogFragment() {
    private lateinit var viewBinding: FragmentFilterBinding
    private lateinit var viewModel: ScanFragmentViewModel

    private var rssiFlag = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = (parentFragment as ScanFragment).viewModel
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setHasOptionsMenu(true)
        viewBinding = FragmentFilterBinding.inflate(inflater)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = getString(R.string.button_filter)
        setListeners()
        initSeekbarValues()
        loadFilterValues()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_filter, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.filter_reset -> {
                resetFilters()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initSeekbarValues() {
        viewBinding.seekBarRssi.apply {
            textviewSeekbarMin.text = getString(R.string.filter_seekbar_min)
            textviewSeekbarMax.text = getString(R.string.filter_seekbar_max)
            seekControlBar.max = resources.getInteger(R.integer.rssi_value_range)
            seekControlBar.progress = 0
            seekControlText.text = getString(R.string.filter_rssi_not_set)
        }
    }

    private fun loadFilterValues() {
        viewModel.activeFilters.value?.let {
            viewBinding.apply {
                etSearchDeviceName.setText(it.name)
                if (it.isRssiFlag) {
                    seekBarRssi.seekControlBar.progress = getSeekbarProgress(it.rssiValue)
                    seekBarRssi.seekControlText.text = getString(R.string.n_dBm, it.rssiValue)
                }
                if (it.bleFormats.contains(BleFormat.UNSPECIFIED)) beaconTypeUnspecified.isChecked = true
                if (it.bleFormats.contains(BleFormat.ALT_BEACON)) beaconTypeAltBeacon.isChecked = true
                if (it.bleFormats.contains(BleFormat.I_BEACON)) beaconTypeIBeacon.isChecked = true
                if (it.bleFormats.contains(BleFormat.EDDYSTONE)) beaconTypeEddystone.isChecked = true

                if (it.isOnlyConnectable) cbOnlyConnectable.isChecked = true
                if (it.isOnlyBonded) cbOnlyBonded.isChecked = true
                if (it.isOnlyFavourite) cbOnlyFavourites.isChecked = true
            }
        } ?: resetFilters()
    }

    private fun resetFilters() {
        viewBinding.apply {
            etSearchDeviceName.text.clear()
            seekBarRssi.seekControlBar.progress = 0
            rssiFlag = false
            seekBarRssi.seekControlText.text = getString(R.string.filter_rssi_not_set)
            clearCheckBoxes()
            cbOnlyConnectable.isChecked = false
            cbOnlyBonded.isChecked = false
            cbOnlyFavourites.isChecked = false
        }
    }

    private fun clearCheckBoxes() {
        viewBinding.apply {
            beaconTypeUnspecified.isChecked = false
            beaconTypeAltBeacon.isChecked = false
            beaconTypeIBeacon.isChecked = false
            beaconTypeEddystone.isChecked = false
        }
    }

    private fun getScanFragment() = parentFragment as ScanFragment

    private fun setListeners() {
        viewBinding.apply {
            btnApplyFilters.setOnClickListener {
                viewModel.apply {
                    updateFiltering(prepareFilters())
                    getScanFragment().toggleFilterFragment(shouldShowFilterFragment = false)
                }
            }

            seekBarRssi.seekControlBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    rssiFlag = true
                    val rssi = getRssiValue(progress)
                    seekBarRssi.seekControlText.text = getString(R.string.n_dBm, rssi)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })
        }
    }

    private fun getRssiValue(progress: Int): Int {
        return progress - requireContext().resources.getInteger(R.integer.rssi_value_range)
    }

    private fun getSeekbarProgress(rssiValue: Int) : Int {
        return rssiValue + requireContext().resources.getInteger(R.integer.rssi_value_range)
    }

    private val selectedBeacons: List<BleFormat>
        get() {
            val selectedBeacons: MutableList<BleFormat> = ArrayList()
            viewBinding.apply {
                if (beaconTypeUnspecified.isChecked) selectedBeacons.add(BleFormat.UNSPECIFIED)
                if (beaconTypeIBeacon.isChecked) selectedBeacons.add(BleFormat.I_BEACON)
                if (beaconTypeEddystone.isChecked) selectedBeacons.add(BleFormat.EDDYSTONE)
                if (beaconTypeAltBeacon.isChecked) selectedBeacons.add(BleFormat.ALT_BEACON)
            }
            return selectedBeacons
        }

    private fun prepareFilters(): FilterDeviceParams? {
        val name = viewBinding.etSearchDeviceName.text.toString()
        val rssi = getRssiValue(viewBinding.seekBarRssi.seekControlBar.progress)
        val activeFilter = FilterDeviceParams(
                if (name.isNotBlank()) name else null,
                rssi,
                rssiFlag,
                selectedBeacons,
                cb_only_favourites.isChecked,
                cb_only_connectable.isChecked,
                cb_only_bonded.isChecked
        )
        return if (!activeFilter.isEmpty) activeFilter else null
    }

}
