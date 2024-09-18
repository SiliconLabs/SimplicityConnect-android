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
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber


class MatterScannedResultFragment : Fragment() {
    private val deviceController: ChipDeviceController
        get() = ChipClient.getDeviceController(requireContext())
    private lateinit var scope: CoroutineScope
    private lateinit var deviceList: ArrayList<MatterScannedResultModel>
    private lateinit var binding: FragmentMatterScannedResultsBinding
    private var deviceId: Long = 0;
    private var deviceType: Int = 0;
    private var pos: Int = 0;
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
    ): View? {
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
            Toast.makeText(requireContext(), resString, Toast.LENGTH_SHORT).show()
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
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

    }


    private fun setListDisplay() {
        deviceList = SharedPrefsUtils.retrieveSavedDevices(mPrefs)
        if (deviceList.size !== 0) {
            Timber.tag(TAG).e("+ deviceList $deviceList")
            matterAdapter = MatterScannedResultAdapter(deviceList)
            binding.recyclerViewScannedDevices.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
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
        deviceType = model.deviceType!!
        when (deviceType) {

            THERMOSTAT_TYPE -> {
                val matterThermostatFragment = MatterThermostatFragment.newInstance()
                FragmentUtils.getHost(
                    this@MatterScannedResultFragment,
                    Callback::class.java
                ).navigateToDemo(matterThermostatFragment, model)
            }

            LIGHTNING_TYPE, ENHANCED_COLOR_LIGHT_TYPE, ONOFF_LIGHT_TYPE, TEMPERATURE_COLOR_LIGHT_TYPE -> {
                val matterLightFragment = MatterLightFragment.newInstance()
                FragmentUtils.getHost(
                    this@MatterScannedResultFragment,
                    Callback::class.java
                ).navigateToDemo(matterLightFragment, model)
            }

            WINDOW_TYPE -> {
                val matterWindowCoverFragment = MatterWindowCoverFragment.newInstance()
                FragmentUtils.getHost(
                    this@MatterScannedResultFragment,
                    Callback::class.java
                ).navigateToDemo(matterWindowCoverFragment, model)
            }

            LOCK_TYPE -> {
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

            PLUG_TYPE -> {
                val matterPlugFragment = MatterPlugFragment.newInstance()
                FragmentUtils.getHost(
                    this@MatterScannedResultFragment,
                    Callback::
                    class.java
                ).navigateToDemo(matterPlugFragment, model)
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

    override fun onDestroy() {
        super.onDestroy()

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
        public const val THERMOSTAT_TYPE = 769
        public const val LIGHTNING_TYPE = 257
        public const val WINDOW_TYPE = 514
        public const val LOCK_TYPE = 10
        public const val ENHANCED_COLOR_LIGHT_TYPE = 269
        public const val ONOFF_LIGHT_TYPE = 256
        public const val TEMPERATURE_COLOR_LIGHT_TYPE = 268
       // public const val SWITCH_TYPE = 259
        public const val OCCUPANCY_SENSOR_TYPE = 263
        public const val TEMPERATURE_SENSOR_TYPE = 770
        public const val CONTACT_SENSOR_TYPE = 21
        public const val PLUG_TYPE = 267
        private const val HYPERLINK_START = 89
        private const val HYPERLINK_END = 107

        fun newInstance(): MatterScannedResultFragment {
            return MatterScannedResultFragment()
        }
    }
}