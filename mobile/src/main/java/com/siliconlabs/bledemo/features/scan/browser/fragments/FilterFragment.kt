package com.siliconlabs.bledemo.features.scan.browser.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.DialogFragment
import com.siliconlabs.bledemo.bluetooth.beacon_utils.BleFormat
import com.siliconlabs.bledemo.home_screen.viewmodels.ScanFragmentViewModel
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.FragmentFilterBinding
import com.siliconlabs.bledemo.home_screen.fragments.ScanFragment
import com.siliconlabs.bledemo.utils.FilterDeviceParams
//import kotlinx.android.synthetic.main.fragment_filter.*
import java.util.*

class FilterFragment : DialogFragment() {
    private lateinit var viewBinding: FragmentFilterBinding
    private lateinit var viewModel: ScanFragmentViewModel
    // Flag to control if the listener should be active
    private var isListenerEnabled = true

    private var rssiFlag = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = (parentFragment as ScanFragment).viewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        viewBinding = FragmentFilterBinding.inflate(inflater)
        return viewBinding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = getString(R.string.button_filter)
        handleFilterButtonVisibility()
        setListeners()
        initSeekbarValues()
        loadFilterValues()
    }

    private fun handleFilterButtonVisibility() {
        val bottomButtonContainer = viewBinding.btnApplyFilters
        val rootView = viewBinding.root

        // Get the initial bottom margin from layout params (16dp from XML)
        val initialBottomMargin = (bottomButtonContainer.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0

        // IMMEDIATELY apply navigation bar height to ensure it's set at launch
        val navigationBarHeight = getNavigationBarHeight()
        bottomButtonContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = initialBottomMargin + navigationBarHeight
        }

        // Create a flag to track if insets have been applied
        var insetsApplied = false

        // Set up listener for when proper insets become available (to override the manual calculation if needed)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Apply the system bar insets to the bottom button (this will override the manual calculation)
            bottomButtonContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = initialBottomMargin + systemBarsInsets.bottom
            }

            insetsApplied = true

            // Don't consume insets, pass them through
            insets
        }

        // Try to request insets in case they become available
        rootView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (!insetsApplied && rootView.isAttachedToWindow) {
                    ViewCompat.requestApplyInsets(rootView)
                }
            }
        })

        // Also try with immediate post
        rootView.post {
            if (!insetsApplied && rootView.isAttachedToWindow) {
                ViewCompat.requestApplyInsets(rootView)
            }
        }
    }

    private fun getNavigationBarHeight(): Int {
        val resources = requireContext().resources
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_filter, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.filter_reset -> {
                // Toggle the flag to enable/disable the listener
                isListenerEnabled = !isListenerEnabled
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


            // Add a label formatter to show the tooltip text with the slider value
            slider.setLabelFormatter { value ->
                // Format the value to display RSSI values like "0", "-10", "-20", etc.
                value.toInt().toString().plus("dB")
            }
        }
    }

    private fun loadFilterValues() {
        viewModel.activeFilters.value?.let {
            viewBinding.apply {
                etSearchDeviceName.setText(it.name)
                if (it.isRssiFlag) {
                    //seekBarRssi.seekControlBar.progress = getSeekbarProgress(it.rssiValue)
                    // Observe the left and right slider values from the ViewModel
                    viewModel.sliderValues.observe(viewLifecycleOwner, androidx.lifecycle.Observer { sliderValues ->
                        seekBarRssi.slider.setValues(sliderValues.first,sliderValues.second)
                    })

                    viewModel.saveStartEndRSSIRange.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
                        seekBarRssi.seekControlText.text = it
                    })

                }
                if (it.bleFormats.contains(BleFormat.UNSPECIFIED)) beaconTypeUnspecified.isChecked =
                    true
                if (it.bleFormats.contains(BleFormat.ALT_BEACON)) beaconTypeAltBeacon.isChecked =
                    true
                if (it.bleFormats.contains(BleFormat.I_BEACON)) beaconTypeIBeacon.isChecked = true
                if (it.bleFormats.contains(BleFormat.EDDYSTONE)) beaconTypeEddystone.isChecked =
                    true

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
            //SET THE TEXT TO "NOT SET"
            viewModel.saveStartEndRssiRange(getString(R.string.filter_rssi_not_set))
            viewModel.saveStartEndRSSIRange.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
                seekBarRssi.seekControlText.text = it
            })


            clearCheckBoxes()
            cbOnlyConnectable.isChecked = false
            cbOnlyBonded.isChecked = false
            cbOnlyFavourites.isChecked = false
            //UPDATE THE OVER ALL PROGRESS TO ZERO
            viewModel.updateOverallProgress(0f,0f)
            viewModel.saveRSSISlideRValues(-130f,0f)
            viewModel.sliderValues.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
                seekBarRssi.slider.setValues(it.first,it.second)

            })

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



    @SuppressLint("SetTextI18n")
    private fun setListeners() {
        viewBinding.apply {
            btnApplyFilters.setOnClickListener {
                viewModel.apply {
                    updateFiltering(prepareFilters())
                    getScanFragment().toggleFilterFragment(shouldShowFilterFragment = false)
                }
            }


            // Add a listener to update the text as the slider moves
            seekBarRssi.slider.addOnChangeListener { slider, value, fromUser ->
                rssiFlag = true
                if (isListenerEnabled){
                    val format = getString(R.string.n_dBm)
                    seekBarRssi.seekControlText.text = "(${String.format(format, slider.values[0].toInt())}, ${String.format(format, slider.values[1].toInt())})"
                    viewModel.saveStartEndRssiRange("(${String.format(format, slider.values[0].toInt())}, ${String.format(format, slider.values[1].toInt())})")

                    // Update the overallProgress in the ViewModel
                    val values = slider.values
                    val startValue = values[0]
                    val endValue = values[1]

                    println("startValue${startValue}")
                    println("endValue${endValue}")
                    viewModel.saveRSSISlideRValues(startValue,endValue)
                    viewModel.updateOverallProgress(startValue,endValue)
                }

            }

        }
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
        var rssiSliderOverallProgressValue:Pair<Float,Float>? = null
        viewModel.overallProgress.observe(viewLifecycleOwner, androidx.lifecycle.Observer { progress ->
            rssiSliderOverallProgressValue = progress
            println("rssiSliderProgressValue${rssiSliderOverallProgressValue}")
        })
        val name = viewBinding.etSearchDeviceName.text.toString()

        val rssi = rssiSliderOverallProgressValue
        val activeFilter = FilterDeviceParams(
            if (name.isNotBlank()) name else null,
            rssi,
            rssiFlag,
            selectedBeacons,
            viewBinding.cbOnlyFavourites.isChecked,
            viewBinding.cbOnlyConnectable.isChecked,
            viewBinding.cbOnlyBonded.isChecked
        )
        return if (!activeFilter.isEmpty) activeFilter else null
    }

}
