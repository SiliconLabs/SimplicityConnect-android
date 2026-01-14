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
    private val configurationParameters = AtomicReference<ChannelSoundingConfigureParameters?>(null)


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
            populateDefaultUI()
        }
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