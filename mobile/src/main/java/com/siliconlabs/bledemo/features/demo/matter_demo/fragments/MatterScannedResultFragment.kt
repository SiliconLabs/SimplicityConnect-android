package com.siliconlabs.bledemo.features.demo.matter_demo.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context.MODE_PRIVATE
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import chip.devicecontroller.ChipDeviceController
import chip.devicecontroller.UnpairDeviceCallback
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.FragmentMatterScannedResultsBinding
import com.siliconlabs.bledemo.features.demo.matter_demo.activities.MatterDemoActivity
import com.siliconlabs.bledemo.features.demo.matter_demo.adapters.MatterScannedResultAdapter
import com.siliconlabs.bledemo.features.demo.matter_demo.model.MatterScannedResultModel
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.ChipClient
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.FragmentUtils
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.RecyclerViewMargin
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.SharedPrefsUtils
import com.siliconlabs.bledemo.utils.CustomToastManager
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber


class MatterScannedResultFragment : Fragment() {
    private val deviceController: ChipDeviceController
        get() = ChipClient.getDeviceController(requireContext())
    private lateinit var scope: CoroutineScope
    private lateinit var deviceList: ArrayList<MatterScannedResultModel>
    private lateinit var binding: FragmentMatterScannedResultsBinding
    private var deviceId: Long = 0
    private var deviceType: Int = 0
    private var pos: Int = 0
    private lateinit var matterAdapter: MatterScannedResultAdapter
    private lateinit var mPrefs: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPrefs = requireContext().getSharedPreferences("your_preference_name", MODE_PRIVATE)

        deviceList = requireArguments().getParcelableArrayList(ARG_DEVICE_LIST)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        scope = viewLifecycleOwner.lifecycleScope
        binding = FragmentMatterScannedResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    inner class ChipUnpairDeviceCallback : UnpairDeviceCallback {
        override fun onError(status: Int, remoteDeviceId: Long) {
            Timber.tag(TAG).e("onError : $remoteDeviceId, $status")
        }

        override fun onSuccess(remoteDeviceId: Long) {
            Timber.tag(TAG).d("onSuccess : $remoteDeviceId")
            showMessages(R.string.matter_device_unpair)
        }
    }

    private fun showMessages(msgId: Int) {
        requireActivity().runOnUiThread {
            val resString = requireContext().getString(msgId)
           // Toast.makeText(requireContext(), resString, Toast.LENGTH_SHORT).show()
            CustomToastManager.show(
                requireContext(),resString,5000
            )
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as MatterDemoActivity).showQRScanner()


        val quickStartGuide = getString(R.string.matter_quick_starter_guide_link)
        val spannableString = SpannableString(quickStartGuide)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                // Define the action to be taken when the link is clicked
                val uri = Uri.parse(MATTER_URL)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
        }
        spannableString.setSpan(
            clickableSpan,
            HYPERLINK_START,
            HYPERLINK_END,
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Set the SpannableString as the text of the TextView
        binding.tvQuickStartHyperlink.text = spannableString

        // Make the TextView clickable
        binding.tvQuickStartHyperlink.movementMethod = LinkMovementMethod.getInstance()

        setListDisplay()
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )

    }


    private fun setListDisplay() {
        deviceList = SharedPrefsUtils.retrieveSavedDevices(mPrefs)
        if (deviceList.size !== 0) {
            Timber.tag(TAG).e("+ deviceList $deviceList")
            matterAdapter = MatterScannedResultAdapter(deviceList)
            binding.recyclerViewScannedDevices.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            binding.recyclerViewScannedDevices.adapter = matterAdapter
            binding.recyclerViewScannedDevices.addItemDecoration(
                RecyclerViewMargin(resources.getDimensionPixelSize(R.dimen.matter_margin))
            )

            matterAdapter.setOnClickListener(object : MatterScannedResultAdapter.OnClickListener {
                override suspend fun onClick(position: Int, model: MatterScannedResultModel) {
                    deviceId = model.deviceId
                    navigateToDemos(model)
                }

                override suspend fun onDelete(position: Int, model: MatterScannedResultModel) {
                    Timber.tag(TAG).d("Unpair")
                    deviceId = model.deviceId
                    pos = position
                    val builder = AlertDialog.Builder(context, R.style.AlertDialogTheme)
                    val alertMessageStart =
                        requireContext().getString(R.string.matter_delete_message)
                    val alertTitle = requireContext().getString(R.string.matter_delete_alert_title)
                    val cancel = requireContext().getString(R.string.matter_cancel)
                    val ok = requireContext().getString(R.string.matter_alert_ok)

                    builder.setTitle(alertTitle)
                    builder.setMessage(alertMessageStart)
                        .setPositiveButton(ok, dialogClickListener)
                        .setNegativeButton(cancel, dialogClickListener).show()
                }
            })
        } else {
            binding.placeholder.visibility = View.VISIBLE
            binding.tvQuickStartHyperlink.visibility = View.VISIBLE
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    var dialogClickListener =
        DialogInterface.OnClickListener { dialog, which ->

            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {

                    Timber.tag(TAG).d("deviceList   $deviceList")
                    deviceList.removeAt(pos)
                    binding.recyclerViewScannedDevices.adapter?.notifyItemRemoved(pos)
                    Timber.tag(TAG).d("deviceList removed elem $deviceList")

                    deviceController.unpairDeviceCallback(deviceId, ChipUnpairDeviceCallback())
                    matterAdapter.notifyDataSetChanged()
                    SharedPrefsUtils.saveDevicesToPref(mPrefs, deviceList)
                    if (deviceList.isEmpty()) {
                        requireActivity().finish()
                    }
                    setListDisplay()
                }

                DialogInterface.BUTTON_NEGATIVE -> {
                    dialog.dismiss()
                }
            }
        }


    private fun navigateToDemos(model: MatterScannedResultModel) {
        deviceType = model.deviceType
        when (deviceType) {

            THERMOSTAT_TYPE -> {
                val matterThermostatFragment = MatterThermostatFragment.newInstance()
                FragmentUtils.getHost(
                    this@MatterScannedResultFragment,
                    Callback::class.java
                ).navigateToDemo(matterThermostatFragment, model)
            }

            DIMMABLE_LIGHT_TYPE, ENHANCED_COLOR_LIGHT_TYPE, ON_OFF_LIGHT_TYPE, COLOR_TEMPERATURE_LIGHT_TYPE -> {
                val matterLightFragment = MatterLightFragment.newInstance()
                FragmentUtils.getHost(
                    this@MatterScannedResultFragment,
                    Callback::class.java
                ).navigateToDemo(matterLightFragment, model)
            }

            WINDOW_COVERING_TYPE -> {
                val matterWindowCoverFragment = MatterWindowCoverFragment.newInstance()
                FragmentUtils.getHost(
                    this@MatterScannedResultFragment,
                    Callback::class.java
                ).navigateToDemo(matterWindowCoverFragment, model)
            }

            DOOR_LOCK_TYPE -> {
                val matterDoorFragment = MatterDoorFragment.newInstance()
                FragmentUtils.getHost(
                    this@MatterScannedResultFragment,
                    Callback::class.java
                ).navigateToDemo(matterDoorFragment, model)
            }

            OCCUPANCY_SENSOR_TYPE -> {
                val matterOccupancySensorFragment = MatterOccupancySensorFragment.newInstance()
                FragmentUtils.getHost(
                    this@MatterScannedResultFragment,
                    Callback::class.java
                ).navigateToDemo(matterOccupancySensorFragment, model)
            }

            CONTACT_SENSOR_TYPE -> {
                val matterContactSensorFragment = MatterContactSensorFragment.newInstance()
                FragmentUtils.getHost(
                    this@MatterScannedResultFragment,
                    Callback::class.java
                ).navigateToDemo(matterContactSensorFragment, model)
            }

            TEMPERATURE_SENSOR_TYPE -> {
                val matterTemperatureSensorFragment = MatterTemperatureSensorFragment.newInstance()
                FragmentUtils.getHost(
                    this@MatterScannedResultFragment,
                    Callback::class.java
                ).navigateToDemo(matterTemperatureSensorFragment, model)
            }

            DIMMABLE_PLUG_IN_UNIT_TYPE -> {
                val matterPlugFragment = MatterPlugFragment.newInstance()
                FragmentUtils.getHost(
                    this@MatterScannedResultFragment,
                    Callback::class.java
                ).navigateToDemo(matterPlugFragment, model)
            }

            DISHWASHER_TYPE -> {
                val matterDishwasherFragment = MatterDishwasherFragment.newInstance()
                FragmentUtils.getHost(
                    this@MatterScannedResultFragment,
                    Callback::class.java
                ).navigateToDemo(matterDishwasherFragment, model)
            }

            AIR_QUALITY_SENSOR_TYPE ->{
                val matterAirQualitySensorFragment = MatterAirQualitySensorFragment.newInstance()
                FragmentUtils.getHost(
                    this@MatterScannedResultFragment,
                    Callback::class.java
                ).navigateToDemo(matterAirQualitySensorFragment, model)
            }


            else -> {
                println("Unhandled Operation....")
            }
        }

    }


    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            requireActivity().finish()
        }
    }


    interface Callback {
        fun navigateToDemo(
            matterDemoFragment: Fragment,
            model: MatterScannedResultModel
        )

    }

    companion object {
        private const val ARG_DEVICE_LIST = "device_list"
        private const val TAG = "MatterScannedResultFragment"
        private const val MATTER_URL = "https://docs.silabs.com/matter/2.1.0/matter-overview"


        //Lighting Type
        const val ON_OFF_LIGHT_TYPE = 256
        const val DIMMABLE_LIGHT_TYPE = 257
        const val COLOR_TEMPERATURE_LIGHT_TYPE = 268
        const val ENHANCED_COLOR_LIGHT_TYPE = 269

        //smart plugs/outlets and other actuators
        const val ON_OFF_PLUG_IN_UNIT_TYPE = 266
        const val DIMMABLE_PLUG_IN_UNIT_TYPE = 267
        const val PUMP = 771

        //switches and controls
        const val ON_OFF_LIGHT_SWITCH = 259
        const val DIMMER_SWITCH = 260
        const val COLOR_DIMMER_SWITCH = 261
        const val CONTROL_BRIDGE = 2112
        const val PUMP_CONTROLLER = 772
        const val GENERIC_SWITCH = 15

        //sensors
        const val CONTACT_SENSOR_TYPE = 21
        const val LIGHT_SENSOR_TYPE = 262
        const val OCCUPANCY_SENSOR_TYPE = 263
        const val TEMPERATURE_SENSOR_TYPE = 770
        const val PRESSURE_SENSOR_TYPE = 773
        const val FLOW_SENSOR_TYPE = 774
        const val HUMIDITY_SENSOR_TYPE = 775
        const val ON_OFF_SENSOR_TYPE = 2128
        const val SMOKE_CO_ALARM_TYPE = 118

        //closures
        const val DOOR_LOCK_TYPE = 10
        const val DOOR_LOCK_CONTROLLER_TYPE = 11
        const val WINDOW_COVERING_TYPE = 514
        const val WINDOW_COVERING_CONTROLLER_TYPE = 515

        //HVAC
        const val HEATING_COOLING_UNIT_TYPE = 768
        const val THERMOSTAT_TYPE = 769
        const val FAN_TYPE = 43
        const val AIR_PURIFIER_TYPE = 45
        const val AIR_QUALITY_SENSOR_TYPE = 44

        //media
        const val BASIC_VIDEO_PLAYER_TYPE = 40
        const val CASTING_VIDEO_PLAYER_TYPE = 35
        const val SPEAKER_TYPE = 34
        const val CONTENT_APP_TYPE = 36
        const val CASTING_VIDEO_CLIENT_TYPE = 41
        const val VIDEO_REMOTE_CONTROL_TYPE = 42

        //Generic
        const val MODE_SELECT_TYPE = 39

        //robotic devices
        const val ROBOTIC_VACUUM_CLEANER_TYPE = 116

        //appliances
        const val REFRIGERATOR_TYPE = 112
        const val TEMPERATURE_CONTROLLED_CABINET_TYPE = 113
        const val ROOM_AIR_CONDITIONER_TYPE =114
        const val LAUNDRY_WASHER_TYPE = 115
        const val DISHWASHER_TYPE = 117

        //Hyperlink const
        private const val HYPERLINK_START = 89
        private const val HYPERLINK_END = 107

        fun newInstance(): MatterScannedResultFragment {
            return MatterScannedResultFragment()
        }
    }
}