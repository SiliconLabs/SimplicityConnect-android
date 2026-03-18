package com.siliconlabs.bledemo.features.demo.channel_sounding.fragments

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.ranging.ble.cs.BleCsRangingCapabilities
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.FragmentChannelSoundingConfigureLayoutBinding
import com.siliconlabs.bledemo.features.demo.channel_sounding.activities.ChannelSoundingActivity
import com.siliconlabs.bledemo.features.demo.channel_sounding.activities.ChannelSoundingActivity.Companion.HIGH_FREQUENCY

import com.siliconlabs.bledemo.features.demo.channel_sounding.models.FilteringLevel
import com.siliconlabs.bledemo.features.demo.channel_sounding.models.KalmanFilterConfig
import com.siliconlabs.bledemo.features.demo.channel_sounding.utils.ChannelSoundingConfigureParameters
import com.siliconlabs.bledemo.features.demo.channel_sounding.viewmodels.ChannelSoundingBleConnectViewModel
import com.siliconlabs.bledemo.features.demo.channel_sounding.viewmodels.ChannelSoundingDistanceMeasurementViewModel
import com.siliconlabs.bledemo.features.demo.channel_sounding.viewmodels.SharedViewModel
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.FragmentUtils
import com.siliconlabs.bledemo.utils.CustomToastManager
import com.skydoves.balloon.Balloon
import java.util.concurrent.atomic.AtomicReference


class ChannelSoundingConfigureFragment : Fragment() {
    private lateinit var binding: FragmentChannelSoundingConfigureLayoutBinding
    private lateinit var viewModelBle: ChannelSoundingBleConnectViewModel
    private lateinit var viewModelDM: ChannelSoundingDistanceMeasurementViewModel
    private lateinit var viewModel: SharedViewModel
    private lateinit var spinnerFrequencyAdapter: ArrayAdapter<String>
    private lateinit var spinnerDurationAdapter: ArrayAdapter<String>
    private lateinit var spinnerSensorEnabledAdapter: ArrayAdapter<String>
    private lateinit var spinnerSecurityLevelAdapter: ArrayAdapter<String>
    private lateinit var spinnerLocationTypeSAdapter: ArrayAdapter<String>
    private lateinit var spinnerSightTypeAdapter: ArrayAdapter<String>
    private lateinit var spinnerFilteringLevelAdapter: ArrayAdapter<String>
    private lateinit var spinnerProcessNoiseAdapter: ArrayAdapter<String>
    private lateinit var spinnerMeasurementNoiseAdapter: ArrayAdapter<String>
    private lateinit var spinnerOutlierThresholdAdapter: ArrayAdapter<String>
    private val configurationParameters = AtomicReference<ChannelSoundingConfigureParameters?>(null)
    private var currentKalmanConfig: KalmanFilterConfig = KalmanFilterConfig.getDefault()


    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModelBle =
            ViewModelProvider(
                requireActivity(),
                ChannelSoundingBleConnectViewModel.Factory(requireActivity().application)
            )[ChannelSoundingBleConnectViewModel::class.java]
        viewModelDM =
            ViewModelProvider(
                requireActivity(),
                ChannelSoundingDistanceMeasurementViewModel.Factory(requireActivity(), viewModelBle)
            )[ChannelSoundingDistanceMeasurementViewModel::class.java]
        viewModel =
            ViewModelProvider(
                requireActivity()
            )[SharedViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentChannelSoundingConfigureLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        requireActivity().onBackPressedDispatcher.addCallback(
            requireActivity(), onBackPressedCallback
        )
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun initViews() {

        val tooltipFreq = Balloon.Builder(requireContext())
            .setText(requireContext().getString(R.string.channel_sounding_tooltip_freq))
            .setArrowSize(10)
            .setWidthRatio(0.5f)
            .setHeight(100)
            .setCornerRadius(4f)
            .setAutoDismissDuration(TIME_OUT)
            .setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue_teal))
            .setTextColor(ContextCompat.getColor(requireContext(),R.color.black))
            .setTextSize(14f)
            .build()
        val tooltipDuration = Balloon.Builder(requireContext())
            .setText(requireContext().getString(R.string.channel_sounding_tooltip_duration))
            .setArrowSize(10)
            .setWidthRatio(0.5f)
            .setHeight(100)
            .setCornerRadius(4f)
            .setAutoDismissDuration(TIME_OUT)
            .setBackgroundColor(ContextCompat.getColor(requireContext(),R.color.blue_teal))
            .setTextColor(ContextCompat.getColor(requireContext(),R.color.black))
            .setTextSize(14f)
            .build()

        val tooltipSesor = Balloon.Builder(requireContext())
            .setText(requireContext().getString(R.string.channel_sounding_tooltip_sensor))
            .setArrowSize(10)
            .setWidthRatio(0.5f)
            .setHeight(100)
            .setCornerRadius(4f)
            .setAutoDismissDuration(TIME_OUT)
            .setBackgroundColor(ContextCompat.getColor(requireContext(),R.color.blue_teal))
            .setTextColor(ContextCompat.getColor(requireContext(),R.color.black))
            .setTextSize(14f)
            .build()
        val tooltipLocatoin = Balloon.Builder(requireContext())
            .setText(requireContext().getString(R.string.channel_sounding_tooltip_location))
            .setArrowSize(10)
            .setWidthRatio(0.5f)
            .setHeight(100)
            .setCornerRadius(4f)
            .setAutoDismissDuration(TIME_OUT)
            .setBackgroundColor(ContextCompat.getColor(requireContext(),R.color.blue_teal))
            .setTextColor(ContextCompat.getColor(requireContext(),R.color.black))
            .setTextSize(14f)
            .build()
        val tooltipSight = Balloon.Builder(requireContext())
            .setText(requireContext().getString(R.string.channel_sounding_tooltip_sight))
            .setArrowSize(10)
            .setWidthRatio(0.5f)
            .setHeight(100)
            .setCornerRadius(4f)
            .setAutoDismissDuration(TIME_OUT)
            .setBackgroundColor(ContextCompat.getColor(requireContext(),R.color.blue_teal))
            .setTextColor(ContextCompat.getColor(requireContext(),R.color.black))
            .setTextSize(14f)
            .build()
        val tooltipSecurity = Balloon.Builder(requireContext())
            .setText(requireContext().getString(R.string.channel_sounding_tooltip_duration))
            .setArrowSize(10)
            .setWidthRatio(0.5f)
            .setHeight(100)
            .setCornerRadius(4f)
            .setAutoDismissDuration(TIME_OUT)
            .setBackgroundColor(ContextCompat.getColor(requireContext(),R.color.blue_teal))
            .setTextColor(ContextCompat.getColor(requireContext(),R.color.black))
            .setTextSize(14f)
            .build()
        val tooltipKalmanFilter = Balloon.Builder(requireContext())
            .setText(requireContext().getString(R.string.channel_sounding_tooltip_kalman_filter))
            .setArrowSize(10)
            .setWidthRatio(0.5f)
            .setHeight(100)
            .setCornerRadius(4f)
            .setAutoDismissDuration(TIME_OUT)
            .setBackgroundColor(ContextCompat.getColor(requireContext(),R.color.blue_teal))
            .setTextColor(ContextCompat.getColor(requireContext(),R.color.black))
            .setTextSize(14f)
            .build()

        binding.ivInfoFreq.setOnClickListener {
            tooltipFreq.showAlignTop(binding.ivInfoFreq)
        }

        binding.ivInfoDuration.setOnClickListener {
            tooltipDuration.showAlignTop(binding.ivInfoDuration)
        }
        binding.ivInfoSensor.setOnClickListener {
            tooltipSesor.showAlignTop(binding.ivInfoSensor)
        }
        binding.ivInfoLocatoin.setOnClickListener {
            tooltipLocatoin.showAlignTop(binding.ivInfoLocatoin)
        }
        binding.ivInfoSighty.setOnClickListener {
            tooltipSight.showAlignTop(binding.ivInfoSighty)
        }
        binding.ivInfoSecuity.setOnClickListener {
            tooltipSecurity.showAlignTop(binding.ivInfoSecuity)
        }
        binding.ivInfoKalmanFilter.setOnClickListener {
            tooltipKalmanFilter.showAlignTop(binding.ivInfoKalmanFilter)
        }

        // Load Kalman filter configuration first
        currentKalmanConfig = KalmanFilterConfig.load(requireContext())

        // Initialize filtering level spinner (replaces on/off toggle)
        spinnerFilteringLevelAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            KalmanFilterConfig.getFilteringLevelDisplayStrings()
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerFilteringLevel.adapter = spinnerFilteringLevelAdapter
        
        // Set current filtering level
        val currentLevelIndex = FilteringLevel.values().indexOf(currentKalmanConfig.filteringLevel)
            .takeIf { it >= 0 } ?: FilteringLevel.MEDIUM.ordinal
        binding.spinnerFilteringLevel.setSelection(currentLevelIndex)
        
        // Apply filtering level when changed
        binding.spinnerFilteringLevel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedLevel = FilteringLevel.values()[position]
                currentKalmanConfig = currentKalmanConfig.copy(filteringLevel = selectedLevel)
                viewModelDM.setFilteringLevel(selectedLevel)
                // Save config
                currentKalmanConfig.save(requireContext())
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // Hide the old toggle switch (filtering is always enabled now)
        binding.switchKalmanFilter.visibility = android.view.View.GONE
        
        // Apply the initial filtering level to the filter
        viewModelDM.setFilteringLevel(currentKalmanConfig.filteringLevel)


        // Initialize Kalman filter settings section
        initializeKalmanFilterSettings()


        // Frequency Spinner
        spinnerFrequencyAdapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, ArrayList<String>()
        )
        spinnerFrequencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFrequency.adapter = spinnerFrequencyAdapter
        spinnerFrequencyAdapter.addAll(viewModelDM.getMeasurementFrequencies())

        // Duration Spinner now uses Int values directly
        spinnerDurationAdapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, ArrayList<String>()
        )
        spinnerDurationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDuration.adapter = spinnerDurationAdapter
        spinnerDurationAdapter.addAll(viewModelDM.getMeasurementDurations())

        //Sensor Enabled
        spinnerSensorEnabledAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item, ArrayList<String>()
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerSensorEnabled.adapter = spinnerSensorEnabledAdapter
        spinnerSensorEnabledAdapter.addAll(viewModelDM.getSensorFusionEnable())

        //Security Level
        val securityLeveLList = listOf(
            BleCsRangingCapabilities.CS_SECURITY_LEVEL_ONE.toString()/*,
            BleCsRangingCapabilities.CS_SECURITY_LEVEL_FOUR.toString()*/
        )

        spinnerSecurityLevelAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item, securityLeveLList
        )

        spinnerSecurityLevelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSecurityLevel.adapter = spinnerSecurityLevelAdapter
        binding.spinnerSecurityLevel.visibility = View.GONE
        binding.securityLevelParent.visibility = View.GONE

        //Location Type
        spinnerLocationTypeSAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item, ArrayList<String>()
        )
        binding.spinnerLocationType.adapter = spinnerLocationTypeSAdapter
        spinnerLocationTypeSAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLocationTypeSAdapter.addAll(viewModelDM.getLocationTypes())

        //Sight Type

        spinnerSightTypeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item, ArrayList<String>()
        )

        spinnerSightTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSightType.adapter = spinnerSightTypeAdapter
        spinnerSightTypeAdapter.addAll(viewModelDM.getSightType())
        configurationParameters
            .set(
                ChannelSoundingConfigureParameters
                    .restoreInstance(requireContext(), false)
            )
        populateUI()
        //Freq selection
        binding.spinnerFrequency.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    id: Long
                ) {
                    saveFrequency(
                        requireContext(),
                        binding.spinnerFrequency.selectedItem.toString()
                    )
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                }
            }
        //Duration selection
        binding.spinnerDuration.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    id: Long
                ) {
                    // No immediate action; saved on Save button click
                    saveDuration(
                        requireContext(),
                        binding.spinnerDuration.selectedItem.toString().toInt()
                    )
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                }
            }
        //Sensor Enabled selection
        binding.spinnerSensorEnabled.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    id: Long
                ) {
                    val selectedString = binding.spinnerSensorEnabled.selectedItem.toString()
                    val booleanValue =
                        selectedString == requireContext().getString(R.string.channel_sounding_sensor_fusion_enable_true)
                    configurationParameters.get()?.global?.sensorFusionEnabled = booleanValue
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {

                }
            }
        //Security Level selection
        binding.spinnerSecurityLevel.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    id: Long
                ) {
                    val selected = binding.spinnerSecurityLevel.selectedItem.toString()
                    configurationParameters.get()?.bleCs?.securityLevel =
                        binding.spinnerSecurityLevel.getItemAtPosition(pos).toString().toInt()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {

                }
            }

        //Location Type selection
        binding.spinnerLocationType.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    id: Long
                ) {
                    // val selected = binding.spinnerLocationType.selectedItem
                    configurationParameters.get()?.bleCs?.locationType = pos

                }

                override fun onNothingSelected(p0: AdapterView<*>?) {

                }
            }

        //Sight Type selection
        binding.spinnerSightType.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    id: Long
                ) {
                    val selected = binding.spinnerSightType.selectedItem.toString()
                    configurationParameters.get()?.bleCs?.sightType = pos

                }

                override fun onNothingSelected(p0: AdapterView<*>?) {

                }
            }

        //Save
        binding.saveBtn.setOnClickListener {
            saveFrequency(requireContext(), binding.spinnerFrequency.selectedItem.toString())
            val durationSelected = binding.spinnerDuration.selectedItem
            val durationInt = when (durationSelected) {
                is Int -> durationSelected
                is String -> durationSelected.toIntOrNull()
                else -> null
            }
            if (durationInt == null) {
                CustomToastManager.show(requireContext(), "Invalid duration selected")
                return@setOnClickListener
            }
            saveDuration(requireContext(), durationInt)
            configurationParameters.get()?.saveInstance(requireContext())

            // Save Kalman filter configuration
            currentKalmanConfig.save(requireContext())
            viewModelDM.applyKalmanFilterConfig(currentKalmanConfig)

            viewModel.message.value = "Configuration Saved"
            CustomToastManager.show(requireContext(), "Configuration Saved")
            requireActivity().supportFragmentManager.popBackStack()
        }
        //Reset
        binding.resetBtn.setOnClickListener {
            configurationParameters.set(
                ChannelSoundingConfigureParameters.resetInstance(
                    requireContext(),
                    false
                )
            )
            saveFrequency(requireContext(), HIGH_FREQUENCY)
            saveDuration(requireContext(), 0)

            // Reset Kalman filter configuration
            currentKalmanConfig = currentKalmanConfig.resetToDefault(requireContext())
            populateKalmanFilterUI()

            // Use configureKalmanFilter for quick noise parameter reset
            viewModelDM.configureKalmanFilter(
                currentKalmanConfig.processNoise,
                currentKalmanConfig.measurementNoise
            )
            // Apply full configuration for all other settings
            viewModelDM.applyKalmanFilterConfig(currentKalmanConfig)

            populateDefaultUI()
        }
    }

    /**
     * Initialize Kalman filter settings section with spinners and switches
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun initializeKalmanFilterSettings() {
        // Create tooltips for Kalman filter settings
        val tooltipOutlierDetection = Balloon.Builder(requireContext())
            .setText(requireContext().getString(R.string.channel_sounding_tooltip_outlier_detection))
            .setArrowSize(10)
            .setWidthRatio(0.5f)
            .setHeight(100)
            .setCornerRadius(4f)
            .setAutoDismissDuration(TIME_OUT)
            .setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue_teal))
            .setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            .setTextSize(14f)
            .build()

        val tooltipAdaptiveFiltering = Balloon.Builder(requireContext())
            .setText(requireContext().getString(R.string.channel_sounding_tooltip_adaptive_filtering))
            .setArrowSize(10)
            .setWidthRatio(0.5f)
            .setHeight(100)
            .setCornerRadius(4f)
            .setAutoDismissDuration(TIME_OUT)
            .setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue_teal))
            .setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            .setTextSize(14f)
            .build()

        val tooltipProcessNoise = Balloon.Builder(requireContext())
            .setText(requireContext().getString(R.string.channel_sounding_tooltip_process_noise))
            .setArrowSize(10)
            .setWidthRatio(0.5f)
            .setHeight(100)
            .setCornerRadius(4f)
            .setAutoDismissDuration(TIME_OUT)
            .setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue_teal))
            .setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            .setTextSize(14f)
            .build()

        val tooltipMeasurementNoise = Balloon.Builder(requireContext())
            .setText(requireContext().getString(R.string.channel_sounding_tooltip_measurement_noise))
            .setArrowSize(10)
            .setWidthRatio(0.5f)
            .setHeight(100)
            .setCornerRadius(4f)
            .setAutoDismissDuration(TIME_OUT)
            .setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue_teal))
            .setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            .setTextSize(14f)
            .build()

        val tooltipOutlierThreshold = Balloon.Builder(requireContext())
            .setText(requireContext().getString(R.string.channel_sounding_tooltip_outlier_threshold))
            .setArrowSize(10)
            .setWidthRatio(0.5f)
            .setHeight(100)
            .setCornerRadius(4f)
            .setAutoDismissDuration(TIME_OUT)
            .setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue_teal))
            .setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            .setTextSize(14f)
            .build()

        // Set up tooltip click listeners
        binding.ivInfoOutlierDetection.setOnClickListener {
            tooltipOutlierDetection.showAlignTop(binding.ivInfoOutlierDetection)
        }
        binding.ivInfoAdaptiveFiltering.setOnClickListener {
            tooltipAdaptiveFiltering.showAlignTop(binding.ivInfoAdaptiveFiltering)
        }
        binding.ivInfoProcessNoise.setOnClickListener {
            tooltipProcessNoise.showAlignTop(binding.ivInfoProcessNoise)
        }
        binding.ivInfoMeasurementNoise.setOnClickListener {
            tooltipMeasurementNoise.showAlignTop(binding.ivInfoMeasurementNoise)
        }
        binding.ivInfoOutlierThreshold.setOnClickListener {
            tooltipOutlierThreshold.showAlignTop(binding.ivInfoOutlierThreshold)
        }

        // Initialize Outlier Detection switch
        binding.switchOutlierDetection.isChecked = currentKalmanConfig.outlierDetectionEnabled
        binding.switchOutlierDetection.setOnCheckedChangeListener { _, isChecked ->
            currentKalmanConfig = currentKalmanConfig.copy(outlierDetectionEnabled = isChecked)
            // Apply immediately for real-time feedback
            viewModelDM.setOutlierDetectionEnabled(isChecked)
        }

        // Initialize Adaptive Filtering switch
        binding.switchAdaptiveFiltering.isChecked = currentKalmanConfig.adaptiveFilteringEnabled
        binding.switchAdaptiveFiltering.setOnCheckedChangeListener { _, isChecked ->
            currentKalmanConfig = currentKalmanConfig.copy(adaptiveFilteringEnabled = isChecked)
            // Apply immediately for real-time feedback
            viewModelDM.setAdaptiveFilteringEnabled(isChecked)
        }

        // Initialize Process Noise Spinner
        spinnerProcessNoiseAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            KalmanFilterConfig.getProcessNoiseDisplayStrings()
        )
        spinnerProcessNoiseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerProcessNoise.adapter = spinnerProcessNoiseAdapter
        binding.spinnerProcessNoise.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedNoise = KalmanFilterConfig.PROCESS_NOISE_OPTIONS[position]
                currentKalmanConfig = currentKalmanConfig.copy(processNoise = selectedNoise)
                // Apply immediately for real-time feedback
                viewModelDM.setProcessNoise(selectedNoise)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Initialize Measurement Noise Spinner
        spinnerMeasurementNoiseAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            KalmanFilterConfig.getMeasurementNoiseDisplayStrings()
        )
        spinnerMeasurementNoiseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMeasurementNoise.adapter = spinnerMeasurementNoiseAdapter
        binding.spinnerMeasurementNoise.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedNoise = KalmanFilterConfig.MEASUREMENT_NOISE_OPTIONS[position]
                currentKalmanConfig = currentKalmanConfig.copy(measurementNoise = selectedNoise)
                // Apply immediately for real-time feedback
                viewModelDM.setMeasurementNoise(selectedNoise)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Initialize Outlier Threshold Spinner
        spinnerOutlierThresholdAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            KalmanFilterConfig.getOutlierThresholdDisplayStrings()
        )
        spinnerOutlierThresholdAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerOutlierThreshold.adapter = spinnerOutlierThresholdAdapter
        binding.spinnerOutlierThreshold.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedThreshold = KalmanFilterConfig.OUTLIER_THRESHOLD_OPTIONS[position]
                currentKalmanConfig = currentKalmanConfig.copy(outlierThreshold = selectedThreshold)
                // Apply immediately for real-time feedback
                viewModelDM.setOutlierThreshold(selectedThreshold)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Populate UI with current configuration
        populateKalmanFilterUI()

        // Filtering is always enabled now, so settings section is always visible
        updateKalmanSettingsSectionVisibility(true)
    }

    /**
     * Update the visibility of the Kalman filter settings section.
     * Filtering is always enabled now, so this always shows the section.
     */
    private fun updateKalmanSettingsSectionVisibility(isEnabled: Boolean) {
        // Filtering is always enabled, so section is always visible
        binding.kalmanSettingsSection.visibility = View.GONE
    }

    /**
     * Populate the Kalman filter UI elements with current configuration values.
     * Uses the ViewModel's getKalmanFilterConfig() to ensure synchronization with actual filter state.
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun populateKalmanFilterUI() {
        // Get the actual filter configuration from ViewModel for synchronization
        val actualConfig = viewModelDM.getKalmanFilterConfig()

        // Update local config to match actual filter state
        currentKalmanConfig = actualConfig

        // Set filtering level spinner
        val currentLevel = viewModelDM.getFilteringLevel()
        val levelIndex = FilteringLevel.values().indexOf(currentLevel)
            .takeIf { it >= 0 } ?: FilteringLevel.MEDIUM.ordinal
        binding.spinnerFilteringLevel.setSelection(levelIndex)
        
        // Set other switch states using ViewModel's query methods for verification
        binding.switchOutlierDetection.isChecked = viewModelDM.isOutlierDetectionEnabled()
        binding.switchAdaptiveFiltering.isChecked = viewModelDM.isAdaptiveFilteringEnabled()

        // Set spinner positions based on actual filter configuration
        val processNoiseIndex = KalmanFilterConfig.PROCESS_NOISE_OPTIONS.indexOfFirst {
            it == actualConfig.processNoise
        }.takeIf { it >= 0 } ?: 2 // Default to index 2 (0.12)
        binding.spinnerProcessNoise.setSelection(processNoiseIndex)

        val measurementNoiseIndex = KalmanFilterConfig.MEASUREMENT_NOISE_OPTIONS.indexOfFirst {
            it == actualConfig.measurementNoise
        }.takeIf { it >= 0 } ?: 2 // Default to index 2 (0.05)
        binding.spinnerMeasurementNoise.setSelection(measurementNoiseIndex)

        val outlierThresholdIndex = KalmanFilterConfig.OUTLIER_THRESHOLD_OPTIONS.indexOfFirst {
            it == actualConfig.outlierThreshold
        }.takeIf { it >= 0 } ?: 2 // Default to index 2 (2.5)
        binding.spinnerOutlierThreshold.setSelection(outlierThresholdIndex)
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun populateDefaultUI() {
        binding.spinnerFrequency.setSelection(
            spinnerFrequencyAdapter.getPosition(getDefaultFrequency(requireContext()))
        )
        val storedDuration = getDefaultDuration(requireContext())
        if (storedDuration != null) {
            val idx = spinnerDurationAdapter.getPosition(storedDuration)
            if (idx >= 0) binding.spinnerDuration.setSelection(idx)
        }
        val sensorFusionEnabled =
            configurationParameters.get()?.global?.sensorFusionEnabled ?: false
        val sensorFusionString = if (sensorFusionEnabled) {
            requireContext().getString(R.string.channel_sounding_sensor_fusion_enable_true)
        } else {
            requireContext().getString(R.string.channel_sounding_sensor_fusion_enable_false)
        }
        binding.spinnerSensorEnabled.setSelection(
            spinnerSensorEnabledAdapter.getPosition(sensorFusionString)
        )
        binding.spinnerSecurityLevel.setSelection(
            spinnerSecurityLevelAdapter.getPosition(
                configurationParameters.get()?.bleCs?.securityLevel.toString()
            )
        )
        val posLoc = configurationParameters.get()?.bleCs?.locationType
        binding.spinnerLocationType.setSelection(
            posLoc ?: 0
        )
        val posSight = configurationParameters.get()?.bleCs?.sightType
        binding.spinnerSightType.setSelection(
            posSight ?: 0
        )
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun populateUI() {
        val config = configurationParameters.get() ?: return
        binding.spinnerFrequency.setSelection(
            spinnerFrequencyAdapter.getPosition(getFrequency(requireContext()))
        )
        val storedDuration = getDuration(requireContext())
        if (storedDuration != null) {
            val idx = spinnerDurationAdapter.getPosition(storedDuration)
            if (idx >= 0) binding.spinnerDuration.setSelection(idx)
        }
        val sensorFusionEnabled =
            configurationParameters.get()?.global?.sensorFusionEnabled ?: false
        val sensorFusionString = if (sensorFusionEnabled) {
            requireContext().getString(R.string.channel_sounding_sensor_fusion_enable_true)
        } else {
            requireContext().getString(R.string.channel_sounding_sensor_fusion_enable_false)
        }
        binding.spinnerSensorEnabled.setSelection(
            spinnerSensorEnabledAdapter.getPosition(sensorFusionString)
        )
        binding.spinnerSecurityLevel.setSelection(
            spinnerSecurityLevelAdapter.getPosition(
                configurationParameters.get()?.bleCs?.securityLevel.toString()
            )
        )
        val posLoc = configurationParameters.get()?.bleCs?.locationType
        binding.spinnerLocationType.setSelection(
            posLoc ?: 0
        )
        val posSight = configurationParameters.get()?.bleCs?.sightType
        binding.spinnerSightType.setSelection(
            posSight ?: 0
        )
    }

    interface CallBackHandler {
        fun onBackHandler()
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (isAdded) {
                if (requireActivity().supportFragmentManager.backStackEntryCount > 0) {
                    requireActivity().supportFragmentManager.popBackStack()
                    FragmentUtils.getHost(
                        this@ChannelSoundingConfigureFragment,
                        CallBackHandler::class.java
                    ).onBackHandler()
                } else {
                    FragmentUtils.getHost(
                        this@ChannelSoundingConfigureFragment,
                        CallBackHandler::class.java
                    ).onBackHandler()
                }
            }
        }
    }

    companion object {
        private val TIME_OUT = 35000L
        fun newInstance(): ChannelSoundingConfigureFragment {
            return ChannelSoundingConfigureFragment()
        }

        fun saveFrequency(context: Context, frequency: String) {
            val sharedPref = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            sharedPref.edit {
                putString("frequency", frequency)
            }
        }

        fun getFrequency(context: Context): String? {
            val sharedPref = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            return sharedPref.getString("frequency", HIGH_FREQUENCY)
        }

        fun saveDuration(context: Context, duration: Int) {
            val sharedPref = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            sharedPref.edit {
                putInt("duration", duration)
            }
        }

        fun getDuration(context: Context): String? {
            val sharedPref = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            return sharedPref.getInt("duration", 0).toString()
        }

        fun getDefaultFrequency(context: Context): String? {
            saveFrequency(context, HIGH_FREQUENCY)
            val sharedPref = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            return sharedPref.getString("frequency", HIGH_FREQUENCY)
        }

        fun getDefaultDuration(context: Context): String? {
            saveDuration(context, 0)
            val sharedPref = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            return sharedPref.getInt("duration", 0).toString()
        }

        fun saveKalmanFilterEnabled(context: Context, enabled: Boolean) {
            val sharedPref = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            sharedPref.edit {
                putBoolean("kalman_filter_enabled", enabled)
            }
        }

        fun getKalmanFilterEnabled(context: Context): Boolean {
            val sharedPref = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            return sharedPref.getBoolean("kalman_filter_enabled", true) // Enabled by default
        }
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    override fun onPause() {
        super.onPause()
        (activity as ChannelSoundingActivity).showConfigureIcon()
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    override fun onResume() {
        super.onResume()
        (activity as ChannelSoundingActivity).hideConfigureIcon()
    }
}