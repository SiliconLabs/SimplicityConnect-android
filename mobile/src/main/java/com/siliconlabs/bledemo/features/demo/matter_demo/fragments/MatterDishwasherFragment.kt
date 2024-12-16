package com.siliconlabs.bledemo.features.demo.matter_demo.fragments

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import chip.devicecontroller.ChipClusters
import chip.devicecontroller.ChipClusters.ElectricalEnergyMeasurementCluster
import chip.devicecontroller.ChipClusters.OperationalStateCluster
import chip.devicecontroller.ChipClusters.DeviceEnergyManagementCluster
import chip.devicecontroller.ChipDeviceController
import chip.devicecontroller.ChipStructs
import chip.devicecontroller.GetConnectedDeviceCallbackJni
import com.google.gson.Gson
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.bluetooth.beacon_utils.eddystone.Constants.SCAN_TIMER
import com.siliconlabs.bledemo.databinding.FragmentMatterDishwasherBinding
import com.siliconlabs.bledemo.features.demo.matter_demo.activities.MatterDemoActivity
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterLightFragment.CallBackHandler
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterLightFragment.Companion.ARG_DEVICE_MODEL
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterLightFragment.Companion.INIT
import com.siliconlabs.bledemo.features.demo.matter_demo.model.DishWasherModel
import com.siliconlabs.bledemo.features.demo.matter_demo.model.MatterScannedResultModel
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.ChipClient
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.CustomProgressDialog
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.FragmentUtils
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.MessageDialogFragment
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.SharedPrefsUtils
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.SharedPrefsUtils.ARG_ADDED_DEVICE_INFO_LIST
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class MatterDishwasherFragment : Fragment() {
    private var progress: Int = 0
    private lateinit var dialog: MessageDialogFragment
    private val dialogTag = "MessageDialog"
    private val deviceController: ChipDeviceController
        get() = ChipClient.getDeviceController(requireContext())
    private lateinit var mPrefs: SharedPreferences

    private lateinit var scope: CoroutineScope
    lateinit var binding: FragmentMatterDishwasherBinding
    private var deviceId: Long = INIT
    private var endpointId: Int = DISHWASHER_OPT_ENDPOINT
    private lateinit var model: MatterScannedResultModel
    private var customProgressDialog: CustomProgressDialog? = null
    private var timer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 600000 // 10 minutes in milliseconds
    private var isTimerRunning = false
    private var isPaused = false
    private var currentTotalEnergy: Float = 0.000F
    private var currentInCycleEnergy: Float = 0.000F
    private var cycleCounter: Int? = 0
    private var mCurrentCycleState: Boolean = true
    var mInCurrentCycleGlobalState = 0.000F
    private lateinit var dishwasherPref: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPrefs = requireContext().getSharedPreferences(
            MatterDemoActivity.MATTER_PREF, AppCompatActivity.MODE_PRIVATE
        )
        dishwasherPref = requireContext().getSharedPreferences(
            DISHWASHER_PREF, AppCompatActivity.MODE_PRIVATE
        )
        model = requireArguments().getParcelable(ARG_DEVICE_MODEL)!!
        deviceId = model.deviceId
        Timber.tag(TAG).e("deviceID: $model")

        showMatterProgressDialog(getString(R.string.matter_device_status))
        CoroutineScope(Dispatchers.IO).launch {
            val resultInfo = checkForDeviceStatus()
            withContext(Dispatchers.Main) {
                if (resultInfo) {
                    println("Operation was successful")
                    removeProgress()
                }
            }
        }
    }

    private suspend fun checkForDeviceStatus(): Boolean {
        return withContext(Dispatchers.Default) {
            // Simulate a time-consuming task

            deviceController.getConnectedDevicePointer(
                deviceId,
                object : GetConnectedDeviceCallbackJni.GetConnectedDeviceCallback {
                    override fun onDeviceConnected(devicePointer: Long) {
                        println("----DevicePointer:$devicePointer")
                        model.isDeviceOnline = true
                        SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, true)
                    }

                    override fun onConnectionFailure(nodeId: Long, error: java.lang.Exception?) {
                        model.isDeviceOnline = false
                        removeProgress()
                        println("----nodeId:$nodeId")
                        println("----error:${error!!.message}")
                        SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, false)
                        showMessageDialog()

                    }
                })

            delay(SCAN_TIMER * 1000)
            true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentMatterDishwasherBinding.inflate(inflater, container, false)
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            binding.btnOn.isEnabled = true
            binding.btnOff.isEnabled = false
            binding.btnPause.isEnabled = false
            requireActivity().runOnUiThread {
                imageDishwasherOff()
            }
        }
        return binding.root
    }

    private fun showMessageDialog() {

        try {
            if (isAdded && !requireActivity().isFinishing) {
                requireActivity().runOnUiThread {
                    if (!MessageDialogFragment.isDialogShowing()) {
                        dialog = MessageDialogFragment()
                        dialog.setMessage(getString(R.string.matter_device_offline_text))
                        dialog.setOnDismissListener {
                            removeProgress()
                            if (requireActivity().supportFragmentManager.backStackEntryCount > 0) {
                                requireActivity().supportFragmentManager.popBackStack()
                            } else {
                                FragmentUtils.getHost(
                                    this@MatterDishwasherFragment, CallBackHandler::class.java
                                ).onBackHandler()
                            }
                        }
                        val transaction: FragmentTransaction =
                            requireActivity().supportFragmentManager.beginTransaction()

                        dialog.show(transaction, dialogTag)
                    }
                }
            } else {
                Timber.e("device offline")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e("device offline device offline ${e.message}")
        }

    }

    private fun removeProgress() {
        if (customProgressDialog?.isShowing == true) {
            customProgressDialog?.dismiss()
        }
    }

    private fun showMatterProgressDialog(message: String) {
        customProgressDialog = CustomProgressDialog(requireContext())
        customProgressDialog!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        customProgressDialog!!.setMessage(message)
        customProgressDialog!!.setCanceledOnTouchOutside(false)
        customProgressDialog!!.show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scope = viewLifecycleOwner.lifecycleScope
        updateCompletedCycleTextCounter()
        binding.btnOn.text = requireContext().getString(R.string.matter_dishwasher_on)
        binding.btnOff.text = requireContext().getString(R.string.matter_dishwasher_off)
        binding.btnPause.text = requireContext().getString(R.string.matter_dishwasher_pause)
        (activity as MatterDemoActivity).hideQRScanner()

        binding.btnOn.setOnClickListener {
            DishWasherModel.dishwasherCurrentRunningState = "Started"
            SharedPrefsUtils.saveDishwasherAppliedCycleStates(dishwasherPref, "Started")
            binding.tvCurrentCycleEnergyComp.text = Html.fromHtml(
                "<font color=${context?.getColor(R.color.masala)}>${currentInCycleEnergy}${" kWh"} </font>"

            )
            binding.btnOff.isEnabled = true
            binding.btnPause.isEnabled = true
            binding.btnOn.isEnabled = false
            scope.launch {
                turnOnDishwasher()
                readEnergy()
                startDishwasherTimer()
            }
        }
        binding.btnOff.setOnClickListener {
            currentInCycleEnergy = 0.000F
            binding.tvCurrentCycleEnergyComp.text = Html.fromHtml(
                "<font color=${context?.getColor(R.color.masala)}>${
                    0.000f
                }${" kWh"} </font>"

            )

            SharedPrefsUtils.saveDishwasherInCurrentCycleEnergyConsumed(dishwasherPref, 0.000f)
            DishWasherModel.dishwasherCurrentRunningState = "Stopped"
            SharedPrefsUtils.saveDishwasherAppliedCycleStates(dishwasherPref, "Stopped")
            cycleCounter = cycleCounter!! + 1
            updateCompletedCycleTextCounter()
            binding.btnOn.isEnabled = true
            binding.btnOff.isEnabled = false
            binding.btnPause.isEnabled = false
            mCurrentCycleState = false
            scope.launch {
                //getEleDevMag().subscribeCumulativeEnergyImportedAttribute(null,0,0)
                //forcefully read the Dishwasher Firmware again!
                readEnergy()
                turnOffDishwasher()
                stopDishwasherTimer()
            }
            mInCurrentCycleGlobalState = 0.000F
            cycleCounter?.let {
                SharedPrefsUtils.saveDishwasherCompletedCycleCount(
                    dishwasherPref, it
                )
            }
        }
        binding.btnPause.setOnClickListener {
            binding.btnOn.isEnabled = false
            binding.btnOff.isEnabled = true
            binding.btnPause.isEnabled = true
            if (isPaused) {
                //resume dishwasher operations
                scope.launch {
                    resumeDishwasher()
                    resumeDishwasherTimer()
                }

            } else {
                //pause dishwasher operations
                scope.launch {
                    pauseDishwasher()
                    pauseDishwasherTimer()
                }

            }
            isPaused = !isPaused
            if (isPaused) {
                DishWasherModel.dishwasherCurrentRunningState = "Paused"
                SharedPrefsUtils.saveDishwasherAppliedCycleStates(dishwasherPref, "Paused")
            } else {
                DishWasherModel.dishwasherCurrentRunningState = "Resumed"
                SharedPrefsUtils.saveDishwasherAppliedCycleStates(dishwasherPref, "Resumed")
            }
        }
        updateCountDownTimerText()
        binding.matterProgressBar.max = 100
        binding.matterProgressBar.rotation = 180F
        //back handling
        requireActivity().onBackPressedDispatcher.addCallback(
            requireActivity(), onBackPressedCallback
        )
    }


    private fun showAlertDialogForOtherOperations() {
        val dialog = AlertDialog.Builder(requireContext()).setTitle(getString(R.string.alert))
            .setMessage(getString(R.string.matter_dishwasher_navigating_back))
            .setPositiveButton("OK") { _, _ ->
                // Explicitly pause and navigate back
                binding.btnPause.performClick()
                DishWasherModel.dishwasherCurrentRunningState = "Paused"
                SharedPrefsUtils.saveDishwasherAppliedCycleStates(dishwasherPref, "Paused")
                requireActivity().supportFragmentManager.popBackStack()
            }.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.create()

        dialog.show()
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (isAdded) {
                if (requireActivity().supportFragmentManager.backStackEntryCount > 0) {
                    handleDishwasherBackNavigationUI()
                } else {
                    FragmentUtils.getHost(
                        this@MatterDishwasherFragment,
                        MatterLightFragment.CallBackHandler::class.java
                    ).onBackHandler()
                }
            }
        }
    }

    fun handleDishwasherBackNavigationUI() {
        IS_BACK_BUTTON_CLICKED = true
        if (DishWasherModel.dishwasherCurrentRunningState == "Started" ||
            DishWasherModel.dishwasherCurrentRunningState == "Resumed") {
            showAlertDialogForOtherOperations()
        } else {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    fun showAlertDialogForPausedState() {
        val dialog = AlertDialog.Builder(requireContext()).setTitle(getString(R.string.alert))
            .setMessage(getString(R.string.matter_dishwasher_navigating_back))
            .setPositiveButton("OK") { _, _ ->
                // Navigate back without explicitly pausing
                DishWasherModel.dishwasherCurrentRunningState = "Paused"
                SharedPrefsUtils.saveDishwasherAppliedCycleStates(dishwasherPref, "Paused")
                requireActivity().supportFragmentManager.popBackStack()
            }.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.create()

        dialog.show()
    }


    private fun updateCompletedCycleTextCounter() {
        val htmlString =
            "<font color='#7A7878'>${context?.getString(R.string.matter_completed_cycle)}${" "}</font><b><font color='#000000'>$cycleCounter</font></b>"
        // Set the HTML string to the TextView
        binding.tvCompletedCycleText.text = Html.fromHtml(htmlString, Html.FROM_HTML_MODE_LEGACY)
    }

    private fun resumeDishwasherTimer() {
        isTimerRunning = true
        timer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateCountDownTimerText()
                updateMatterProgressBar()
                SharedPrefsUtils.saveDishwasherTimeLeftFormatted(dishwasherPref, timeLeftInMillis)
            }

            override fun onFinish() {
                updateCompletedCycleTextCounter()
                binding.btnOff.performClick()
                SharedPrefsUtils.saveDishwasherTimeLeftFormatted(dishwasherPref, timeLeftInMillis)
            }
        }.start()
        //isPaused = false
        binding.btnPause.text = getString(R.string.matter_dishwasher_pause)

    }

    private suspend fun resumeDishwasher() {
        getDishwasherClusterForDevice().resume(object :
            OperationalStateCluster.OperationalCommandResponseCallback {
            override fun onError(error: java.lang.Exception?) {
                println("Error: ${error!!.message}")
            }

            override fun onSuccess(commandResponseState: ChipStructs.OperationalStateClusterErrorStateStruct?) {
                if (isAdded) {
                    requireActivity().runOnUiThread {
                        imageDishwasherResume()
                        if (SharedPrefsUtils.getDishwasherAppliedCycleStates(dishwasherPref) == "Resumed") {
                            if (SharedPrefsUtils.getDishwasherCompletedCycleCount(dishwasherPref) <= 0) {
                                currentTotalEnergy =
                                    SharedPrefsUtils.getDishwasherInCurrentCycleEnergyConsumed(
                                        dishwasherPref
                                    )
                            } else {
                                /*currentInCycleEnergy =
                                    SharedPrefsUtils.getDishwasherInCurrentCycleEnergyConsumed(
                                        dishwasherPref
                                    )
                                println("ScccccjustDishwasherInResume $currentInCycleEnergy")*/
                                currentTotalEnergy =
                                    SharedPrefsUtils.getDishwasherTotalEnergyConsumption(
                                        dishwasherPref
                                    )

                            }
                            scope.launch {
                                if (isAdded) {
                                    scope.launch(Dispatchers.Main) {
                                        progress =
                                            SharedPrefsUtils.getDishwasherCompletedCycleProgressBar(
                                                dishwasherPref
                                            )
                                        updateMatterProgressBar()
                                    }
                                }

                                readEnergyWhenInPauseOrResumeState()

                            }
                        }

                    }
                }

            }
        }, TIME_OUT)
    }

    private suspend fun readEnergyWhenInPauseOrResumeState() {
        getEleDevMag().subscribeCumulativeEnergyImportedAttribute(object :
            ElectricalEnergyMeasurementCluster.CumulativeEnergyImportedAttributeCallback {
            override fun onError(error: java.lang.Exception?) {
                if (error != null) {
                    println("Error :${error.message}")
                    Timber.tag(TAG).e("Error :${error.message}")
                    if (isAdded && !requireActivity().isFinishing) {
                        requireActivity().runOnUiThread {
                            Toast.makeText(
                                requireContext(),
                                "Error Occurred:${error.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }


                }
            }

            @SuppressLint("SetTextI18n")
            override fun onSuccess(value: ChipStructs.ElectricalEnergyMeasurementClusterEnergyMeasurementStruct?) {
                if (isAdded && !requireActivity().isFinishing) {
                    requireActivity().runOnUiThread {
                        val energyValue = value?.energy
                        if (null != energyValue) {
                            val str = (energyValue?.toDouble())?.div(1_000_000)
                            val formattedEnergyValue = String.format("%.3f", str)
                            if (formattedEnergyValue.isNotEmpty() || formattedEnergyValue.isNotBlank()) {

                                //currentTotalEnergy = formattedEnergyValue.toFloat()
                                var previousCycle =
                                    SharedPrefsUtils.getDishwasherInCurrentCycleEnergyConsumed(
                                        dishwasherPref
                                    )
                                currentTotalEnergy =
                                    SharedPrefsUtils.getDishwasherTotalEnergyConsumption(
                                        dishwasherPref
                                    )
                                currentInCycleEnergy = (formattedEnergyValue.toFloat() - currentTotalEnergy)+previousCycle

                                //in current cycle
                                if (SharedPrefsUtils.getDishwasherCompletedCycleCount(dishwasherPref) <= 0) {
                                    // when the completed cycle is zero that is during the first cycle, in current cycle
                                    //will be the Total energy coming from the firmware
                                    binding.tvCurrentCycleEnergyComp.text = Html.fromHtml(
                                        "<font color=${context?.getColor(R.color.masala)}>${
                                            String.format(
                                                "%.3f", currentTotalEnergy
                                            )
                                        }${" kWh"} </font>"

                                    )
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        SharedPrefsUtils.saveDishwasherInCurrentCycleEnergyConsumed(
                                            dishwasherPref,
                                            String.format("%.3f", currentTotalEnergy).toFloat()
                                        )
                                    }

                                } else {
                                    //lifecycleScope.launch {
                                        //delay(60000)
                                    if(!DishWasherModel.dishwasherCurrentRunningState.equals(
                                            "Stopped", ignoreCase = true
                                        )){
                                        binding.tvCurrentCycleEnergyComp.text = Html.fromHtml(
                                            "<font color=${context?.getColor(R.color.masala)}>${
                                                String.format(
                                                    "%.3f", currentInCycleEnergy
                                                )
                                            }${" kWh"} </font>"

                                        )
                                        // }

                                        lifecycleScope.launch(Dispatchers.IO) {
                                            SharedPrefsUtils.saveDishwasherInCurrentCycleEnergyConsumed(
                                                dishwasherPref,
                                                String.format("%.3f", currentInCycleEnergy).toFloat()
                                            )
                                        }
                                    }

                                    if(DishWasherModel.dishwasherCurrentRunningState.equals(
                                            "Stopped", ignoreCase = true
                                        )) {
                                        binding.tvCurrentCycleEnergyComp.text = Html.fromHtml(
                                            "<font color=${context?.getColor(R.color.masala)}>${"0.000"}${" kWh"} </font>"

                                        )
                                    }
                                }
                                // in Total
                                if (cycleCounter!! <= 0) {
                                    binding.tvTotalEnergyConsumption.text = Html.fromHtml(
                                        "<font color=${context?.getColor(R.color.masala)}>$formattedEnergyValue${" kWh"} </font>"

                                    )

                                    lifecycleScope.launch(Dispatchers.IO) {
                                        SharedPrefsUtils.saveDishwasherTotalEnergyConsumption(
                                            dishwasherPref, formattedEnergyValue.toFloat()
                                        )
                                    }
                                } else {
                                    binding.tvTotalEnergyConsumption.text = Html.fromHtml(
                                        "<font color=${context?.getColor(R.color.masala)}>$formattedEnergyValue${" kWh"} </font>"

                                    )

                                    binding.tvEnergyMeterReading.text = Html.fromHtml(
                                        "<font color=${context?.getColor(R.color.masala)}>${
                                            String.format(
                                                "%.3f", formattedEnergyValue.toFloat().div(cycleCounter!!)
                                            )
                                        }${" kWh"} </font>"

                                    )

                                    lifecycleScope.launch(Dispatchers.IO) {
                                        SharedPrefsUtils.saveDishwasherTotalEnergyConsumption(
                                            dishwasherPref, formattedEnergyValue.toFloat()
                                        )
                                    }
                                }

                                // average energy per cycle
                                val averageCurrentEnergyPerCycle =
                                    formattedEnergyValue.toFloat().div(cycleCounter!!)
                                if (averageCurrentEnergyPerCycle.isInfinite()) {

                                    binding.tvEnergyMeterReading.text = Html.fromHtml(
                                        "<font color=${context?.getColor(R.color.masala)}>${"0.000"}${" kWh"} </font>"

                                    )
                                }
                                if (cycleCounter!! <= 0) {

                                    binding.tvEnergyMeterReading.text = Html.fromHtml(
                                        "<font color=${context?.getColor(R.color.masala)}>${formattedEnergyValue}${" kWh"} </font>"

                                    )

                                    lifecycleScope.launch(Dispatchers.IO) {
                                        SharedPrefsUtils.saveDishwasherAverageEnergyPerCycle(
                                            dishwasherPref, formattedEnergyValue.toFloat()
                                        )
                                    }
                                } else {
                                    /*if (DishWasherModel.dishwasherCurrentRunningState.equals(
                                            "Stopped", ignoreCase = true
                                        )
                                    ) {*/

                                    binding.tvEnergyMeterReading.text = Html.fromHtml(
                                            "<font color=${context?.getColor(R.color.masala)}>${
                                                String.format(
                                                    "%.3f", averageCurrentEnergyPerCycle
                                                )
                                            }${" kWh"} </font>"

                                        )

                                    lifecycleScope.launch(Dispatchers.IO) {
                                        SharedPrefsUtils.saveDishwasherAverageEnergyPerCycle(
                                            dishwasherPref,
                                            String.format("%.3f", averageCurrentEnergyPerCycle)
                                                .toFloat()
                                        )
                                    }
                                    //}
                                }
                            } else {

                                binding.tvTotalEnergyConsumption.text = Html.fromHtml(
                                    "<font color=${context?.getColor(R.color.masala)}>${"0.000"}${" kWh"} </font>"

                                )
                                binding.tvEnergyMeterReading.text = Html.fromHtml(
                                    "<font color=${context?.getColor(R.color.masala)}>${"0.000"}${" kWh"} </font>"

                                )
                                binding.tvCurrentCycleEnergyComp.text = Html.fromHtml(
                                    "<font color=${context?.getColor(R.color.masala)}>${"0.000"}${" kWh"} </font>"

                                )
                            }
                        } else {
                            binding.tvTotalEnergyConsumption.text = Html.fromHtml(
                                "<font color=${context?.getColor(R.color.masala)}>${"0.000"}${" kWh"} </font>"

                            )
                            binding.tvEnergyMeterReading.text = Html.fromHtml(
                                "<font color=${context?.getColor(R.color.masala)}>${"0.000"}${" kWh"} </font>"

                            )
                            binding.tvCurrentCycleEnergyComp.text = Html.fromHtml(
                                "<font color=${context?.getColor(R.color.masala)}>${"0.000"}${" kWh"} </font>"

                            )
                        }

                    }
                }

            }
        }, 1, 2)
    }


    private fun imageDishwasherResume() {
        if (isAdded) {
            requireActivity().runOnUiThread {
                binding.btnMatterDeviceState.setImageResource(R.drawable.dishwasher_state_on)
                binding.btnMatterDeviceState.setColorFilter(
                    ContextCompat.getColor(requireContext(), R.color.silabs_dark_blue),
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
                binding.txtClusterStatus.text = requireContext().getString(R.string.blinky_tb_on)
            }
        }
    }

    private fun updateCountDownTimerText() {
        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60

        val timeLeftFormatted = String.format("%02d:%02d", minutes, seconds)
        val htmlString =
            "<font color='#7A7878'>${context?.getString(R.string.matter_remaining_time)}${" "}</font><b><font color='#000000'>$timeLeftFormatted</font></b><font color='#7A7878'> ${
                context?.getString(R.string.matter_min)
            }</font>"
        // Set the HTML string to the TextView
        binding.progressMatterText.text = Html.fromHtml(htmlString, Html.FROM_HTML_MODE_LEGACY)

    }

    private fun pauseDishwasherTimer() {
        if (!isTimerRunning) return

        timer?.cancel()
        isTimerRunning = false
        //isPaused = true
        binding.btnPause.text = getString(R.string.matter_resume)
    }

    private fun stopDishwasherTimer() {
        timer?.cancel()
        timeLeftInMillis = 600000 // Reset to 10 minutes
        updateCountDownTimerText()
        binding.matterProgressBar.progress = 0
        isTimerRunning = false
    }

    private fun startDishwasherTimer() {
        if (isTimerRunning) return

        timer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateCountDownTimerText()
                updateMatterProgressBar()
                SharedPrefsUtils.saveDishwasherTimeLeftFormatted(dishwasherPref, timeLeftInMillis)
            }

            override fun onFinish() {
                updateCompletedCycleTextCounter()
                binding.btnOff.performClick()
                SharedPrefsUtils.saveDishwasherTimeLeftFormatted(dishwasherPref, timeLeftInMillis)
            }
        }.start()

        isTimerRunning = true
    }

    private fun updateMatterProgressBar() {
        progress = ((600000 - timeLeftInMillis).toFloat() / 600000 * 100).toInt()
        SharedPrefsUtils.saveDishwasherCompletedCycleProgressBar(dishwasherPref, progress)
        binding.matterProgressBar.progress = progress.toInt()
    }

    private suspend fun getDishwasherDeviceEnergyManagement(): DeviceEnergyManagementCluster {
        return DeviceEnergyManagementCluster(
            ChipClient.getConnectedDevicePointer(requireContext(), deviceId),
            DISHWASHER_DEVICE_ENERGY_MANAGEMENT_ENDPOINT
        )
    }

    private suspend fun getEleDevMag(): ElectricalEnergyMeasurementCluster {
        return ElectricalEnergyMeasurementCluster(
            ChipClient.getConnectedDevicePointer(requireContext(), deviceId),
            DISHWASHER_ELECTRICAL_POWER_MEASUREMENT_ENDPOINT
        )
    }

    private suspend fun getDishwasherClusterForDevice(): OperationalStateCluster {
        return OperationalStateCluster(
            ChipClient.getConnectedDevicePointer(requireContext(), deviceId), endpointId
        )

    }

    private suspend fun readEnergy() {
        getEleDevMag().subscribeCumulativeEnergyImportedAttribute(object :
            ElectricalEnergyMeasurementCluster.CumulativeEnergyImportedAttributeCallback {
            override fun onError(error: java.lang.Exception?) {
                if (error != null) {
                    println("Error :${error.message}")
                    Timber.tag(TAG).e("Error :${error.message}")
                    if (isAdded) {
                        requireActivity().runOnUiThread {
                            Toast.makeText(
                                requireContext(),
                                "Error Occurred:${error.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                }
            }

            @SuppressLint("SetTextI18n")
            override fun onSuccess(value: ChipStructs.ElectricalEnergyMeasurementClusterEnergyMeasurementStruct?) {
                if (isAdded) {
                    requireActivity().runOnUiThread {
                        val energyValue = value?.energy
                        if (null != energyValue) {
                            val str = (energyValue?.toDouble())?.div(1_000_000)
                            val formattedEnergyValue = String.format("%.3f", str)
                            if (formattedEnergyValue.isNotEmpty() || formattedEnergyValue.isNotBlank()) {
                                currentTotalEnergy = formattedEnergyValue.toFloat()
                                if (mInCurrentCycleGlobalState == 0.000F) {
                                    mInCurrentCycleGlobalState = formattedEnergyValue.toFloat()
                                }
                                //currentInCycleEnergy = currentTotalEnergy - currentInCycleEnergy
                                if(cycleCounter!! <= 0){
                                    currentInCycleEnergy =
                                        currentTotalEnergy
                                }else{
                                    currentInCycleEnergy =
                                        currentTotalEnergy - mInCurrentCycleGlobalState
                                }

                                //in current cycle
                                if (mCurrentCycleState) {
                                    mCurrentCycleState = false
                                    binding.tvCurrentCycleEnergyComp.text = Html.fromHtml(
                                        "<font color=${context?.getColor(R.color.masala)}>${"${String.format("%.3f", currentInCycleEnergy)}"}${" kWh"} </font>"

                                    )
                                } else {
                                    if (cycleCounter!! <= 0) {
                                        // when the completed cycle is zero that is during the first cycle, in current cycle
                                        //will be the Total energy coming from the firmware
                                        binding.tvCurrentCycleEnergyComp.text = Html.fromHtml(
                                            "<font color=${context?.getColor(R.color.masala)}>${
                                                String.format(
                                                    "%.3f", currentTotalEnergy
                                                )
                                            }${" kWh"} </font>"

                                        )
                                        SharedPrefsUtils.saveDishwasherInCurrentCycleEnergyConsumed(
                                            dishwasherPref,
                                            String.format("%.3f", currentTotalEnergy).toFloat()
                                        )
                                    } else {
                                        //lifecycleScope.launch {
                                            //delay(60000)
                                        if(!DishWasherModel.dishwasherCurrentRunningState.equals(
                                                "Stopped", ignoreCase = true
                                            )) {
                                            binding.tvCurrentCycleEnergyComp.text = Html.fromHtml(
                                                "<font color=${context?.getColor(R.color.masala)}>${
                                                    String.format(
                                                        "%.3f", currentInCycleEnergy
                                                    )
                                                }${" kWh"} </font>"

                                            )
                                            //}
                                            SharedPrefsUtils.saveDishwasherInCurrentCycleEnergyConsumed(
                                                dishwasherPref,
                                                String.format("%.3f", currentInCycleEnergy).toFloat()
                                            )
                                        }

                                    }
                                    if(DishWasherModel.dishwasherCurrentRunningState.equals(
                                            "Stopped", ignoreCase = true
                                        )) {
                                        binding.tvCurrentCycleEnergyComp.text = Html.fromHtml(
                                            "<font color=${context?.getColor(R.color.masala)}>${"0.000"}${" kWh"} </font>"

                                        )
                                    }
                                }


                                // in Total
                                if (cycleCounter!! <= 0) {
                                    binding.tvTotalEnergyConsumption.text = Html.fromHtml(
                                        "<font color=${context?.getColor(R.color.masala)}>$formattedEnergyValue${" kWh"} </font>"

                                    )
                                    SharedPrefsUtils.saveDishwasherTotalEnergyConsumption(
                                        dishwasherPref, formattedEnergyValue.toFloat()
                                    )
                                } else {
                                    binding.tvTotalEnergyConsumption.text = Html.fromHtml(
                                        "<font color=${context?.getColor(R.color.masala)}>$formattedEnergyValue${" kWh"} </font>"

                                    )
                                    SharedPrefsUtils.saveDishwasherTotalEnergyConsumption(
                                        dishwasherPref, formattedEnergyValue.toFloat()
                                    )
                                }
                                // average energy per cycle
                                val averageCurrentEnergyPerCycle =
                                    currentTotalEnergy.div(cycleCounter!!)
                                if (averageCurrentEnergyPerCycle.isInfinite()) {
                                    binding.tvEnergyMeterReading.text = Html.fromHtml(
                                        "<font color=${context?.getColor(R.color.masala)}>${"0.000"}${" kWh"} </font>"

                                    )
                                }
                                if (cycleCounter!! <= 0) {
                                    binding.tvEnergyMeterReading.text = Html.fromHtml(
                                        "<font color=${context?.getColor(R.color.masala)}>${formattedEnergyValue}${" kWh"} </font>"

                                    )
                                    SharedPrefsUtils.saveDishwasherAverageEnergyPerCycle(
                                        dishwasherPref, formattedEnergyValue.toFloat()
                                    )
                                } else {
                                    /*if (DishWasherModel.dishwasherCurrentRunningState.equals(
                                            "Stopped", ignoreCase = true
                                        )
                                    ) {*/
                                        binding.tvEnergyMeterReading.text = Html.fromHtml(
                                            "<font color=${context?.getColor(R.color.masala)}>${
                                                String.format(
                                                    "%.3f", averageCurrentEnergyPerCycle
                                                )
                                            }${" kWh"} </font>"

                                        )
                                        SharedPrefsUtils.saveDishwasherAverageEnergyPerCycle(
                                            dishwasherPref,
                                            String.format("%.3f", averageCurrentEnergyPerCycle)
                                                .toFloat()
                                        )
                                    //}
                                }
                            } else {
                                binding.tvTotalEnergyConsumption.text = Html.fromHtml(
                                    "<font color=${context?.getColor(R.color.masala)}>${"0.000"}${" kWh"} </font>"

                                )
                                binding.tvEnergyMeterReading.text = Html.fromHtml(
                                    "<font color=${context?.getColor(R.color.masala)}>${"0.000"}${" kWh"} </font>"

                                )
                                binding.tvCurrentCycleEnergyComp.text = Html.fromHtml(
                                    "<font color=${context?.getColor(R.color.masala)}>${"0.000"}${" kWh"} </font>"

                                )
                            }
                        } else {
                            binding.tvTotalEnergyConsumption.text = Html.fromHtml(
                                "<font color=${context?.getColor(R.color.masala)}>${"0.000"}${" kWh"} </font>"

                            )
                            binding.tvEnergyMeterReading.text = Html.fromHtml(
                                "<font color=${context?.getColor(R.color.masala)}>${"0.000"}${" kWh"} </font>"

                            )
                            binding.tvCurrentCycleEnergyComp.text = Html.fromHtml(
                                "<font color=${context?.getColor(R.color.masala)}>${"0.000"}${" kWh"} </font>"

                            )
                        }

                    }
                }

            }
        }, 1, 10)
    }

    private suspend fun pauseDishwasher() {

        getDishwasherClusterForDevice().pause(object :
            OperationalStateCluster.OperationalCommandResponseCallback {
            override fun onError(error: java.lang.Exception?) {
                println("Error: ${error!!.message}")
            }

            override fun onSuccess(commandResponseState: ChipStructs.OperationalStateClusterErrorStateStruct?) {
                imageDishwasherPause()
            }
        }, TIME_OUT)

    }

    private suspend fun turnOffDishwasher() {
        getDishwasherClusterForDevice().stop(object :
            OperationalStateCluster.OperationalCommandResponseCallback {
            override fun onError(error: java.lang.Exception?) {
                println("Error: ${error!!.message}")
            }

            override fun onSuccess(commandResponseState: ChipStructs.OperationalStateClusterErrorStateStruct?) {
                imageDishwasherOff()
            }

        })

    }

    private suspend fun turnOnDishwasher() {
        getDishwasherClusterForDevice().start(object :
            OperationalStateCluster.OperationalCommandResponseCallback {
            override fun onError(error: java.lang.Exception?) {
                println("Error: ${error!!.message}")
            }

            override fun onSuccess(commandResponseState: ChipStructs.OperationalStateClusterErrorStateStruct?) {
                imageDishwasherOn()
            }

        }, TIME_OUT)
    }

    private fun imageDishwasherPause() {
        if (isAdded) {
            requireActivity().runOnUiThread {
                binding.btnMatterDeviceState.setImageResource(R.drawable.ic_dishwasher_icon_stateoff)
                binding.btnMatterDeviceState.setColorFilter(
                    ContextCompat.getColor(requireContext(), R.color.silabs_dark_blue),
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
                binding.txtClusterStatus.text =
                    requireContext().getString(R.string.matter_dishwasher_pause)
            }
        }
    }

    private fun imageDishwasherOff() {
        if (isAdded) {
            requireActivity().runOnUiThread {
                binding.btnMatterDeviceState.setImageResource(R.drawable.ic_dishwasher_icon_stateoff)
                binding.btnMatterDeviceState.setColorFilter(
                    ContextCompat.getColor(requireContext(), R.color.silabs_dark_gray_icon),
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
                binding.txtClusterStatus.text = requireContext().getString(R.string.blinky_tb_off)
                isPaused = false
                binding.btnPause.text = getString(R.string.matter_dishwasher_pause)

            }
        }
    }

    private fun imageDishwasherOn() {
        if (isAdded) {
            requireActivity().runOnUiThread {
                binding.btnMatterDeviceState.setImageResource(R.drawable.dishwasher_state_on)
                binding.btnMatterDeviceState.setColorFilter(
                    ContextCompat.getColor(requireContext(), R.color.silabs_dark_blue),
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
                binding.txtClusterStatus.text = requireContext().getString(R.string.blinky_tb_on)
            }
        }
    }

    interface CallBackHandler {
        fun onBackHandler()
    }

    companion object {
        private val TAG = Companion::class.java.simpleName.toString()
        const val DISHWASHER_PREF = "DISHWASHER_PREF"
        fun newInstance(): MatterDishwasherFragment = MatterDishwasherFragment()

        const val DISHWASHER_OPT_ENDPOINT = 1
        const val DISHWASHER_ELECTRICAL_POWER_MEASUREMENT_ENDPOINT = 2
        const val DISHWASHER_DEVICE_ENERGY_MANAGEMENT_ENDPOINT = 3
        private const val TIME_OUT = 900
        var IS_BACK_BUTTON_CLICKED = false
    }


    override fun onResume() {
        super.onResume()
        if (IS_BACK_BUTTON_CLICKED) {
            IS_BACK_BUTTON_CLICKED = false
            scope.launch {
                //whenever the dishwasher is in pause or resume state,Call the firmware again
                //for throwing the reading again
                readEnergyWhenInPauseOrResumeState()
            }
            try {
                if (isAdded) {
                    scope.launch(Dispatchers.Main) {
                        if (-1 != SharedPrefsUtils.getDishwasherCompletedCycleCount(dishwasherPref)) {
                            cycleCounter =
                                SharedPrefsUtils.getDishwasherCompletedCycleCount(dishwasherPref)
                            updateCompletedCycleTextCounter()
                        }
                    }
                    when (SharedPrefsUtils.getDishwasherAppliedCycleStates(dishwasherPref)) {

                        "Started" -> {
                            dishwasherStartCycle()
                        }

                        "Stopped" -> {
                            dishwasherStopCycle()
                        }

                        "Resumed" -> {
                            dishwasherResumeCycle()
                        }

                        "Paused" -> {
                            dishwasherPauseCycle()

                        }

                        else -> {
                            scope.launch(Dispatchers.Main) {
                                binding.btnOn.isEnabled = true
                                binding.btnOff.isEnabled = false
                                binding.btnPause.isEnabled = false
                                requireActivity().runOnUiThread {
                                    imageDishwasherOff()
                                }
                            }

                        }
                    }
                }
            } catch (e: Exception) {
                println("MatterDishwasherFragment ${e.message}")
            }
        }
    }

    private fun restoreRemainingDishwasgerTime() {
        val timeLeft = SharedPrefsUtils.getDishwasherTimeLeftFormatted(dishwasherPref)

        if (timeLeft != 0L) {
            timeLeftInMillis = timeLeft
            updateCountDownTimerText()
        }
    }

    private fun dishwasherPauseCycle() {
        // Retrieve the time left value from SharedPreferences
        scope.launch(Dispatchers.Main) {
            binding.btnPause.text = getString(R.string.matter_resume)
            binding.btnOn.isEnabled = false
            binding.btnOff.isEnabled = true
            binding.btnPause.isEnabled = true
            restoreRemainingDishwasgerTime()
            progress = SharedPrefsUtils.getDishwasherCompletedCycleProgressBar(dishwasherPref)
            updateMatterProgressBar()
            isPaused = true
            imageDishwasherPause()

            binding.tvTotalEnergyConsumption.text = Html.fromHtml(
                "<font color=${context?.getColor(R.color.masala)}>${
                    SharedPrefsUtils.getDishwasherTotalEnergyConsumption(
                        dishwasherPref
                    )
                }${" kWh"} </font>"

            )
            binding.tvEnergyMeterReading.text = Html.fromHtml(
                "<font color=${context?.getColor(R.color.masala)}>${
                    SharedPrefsUtils.getDishwasherAverageEnergyPerCycle(
                        dishwasherPref
                    )
                }${" kWh"} </font>"

            )
            binding.tvCurrentCycleEnergyComp.text = Html.fromHtml(
                "<font color=${context?.getColor(R.color.masala)}>${
                    SharedPrefsUtils.getDishwasherInCurrentCycleEnergyConsumed(
                        dishwasherPref
                    )
                }${" kWh"} </font>"

            )
        }

    }

    private fun dishwasherResumeCycle() {
        // Retrieve the time left value from SharedPreferences
        scope.launch(Dispatchers.Main) {
            binding.btnPause.text = getString(R.string.matter_dishwasher_pause)
            binding.btnOn.isEnabled = false
            binding.btnOff.isEnabled = true
            binding.btnPause.isEnabled = true
            restoreRemainingDishwasgerTime()
            isPaused = false
            binding.tvTotalEnergyConsumption.text = Html.fromHtml(
                "<font color=${context?.getColor(R.color.masala)}>${
                    SharedPrefsUtils.getDishwasherTotalEnergyConsumption(
                        dishwasherPref
                    )
                }${" kWh"} </font>"

            )
            binding.tvEnergyMeterReading.text = Html.fromHtml(
                "<font color=${context?.getColor(R.color.masala)}>${
                    SharedPrefsUtils.getDishwasherAverageEnergyPerCycle(
                        dishwasherPref
                    )
                }${" kWh"} </font>"

            )
            binding.tvCurrentCycleEnergyComp.text = Html.fromHtml(
                "<font color=${context?.getColor(R.color.masala)}>${
                    SharedPrefsUtils.getDishwasherInCurrentCycleEnergyConsumed(
                        dishwasherPref
                    )
                }${" kWh"} </font>"

            )
            imageDishwasherResume()
        }


    }

    private fun dishwasherStopCycle() {
        scope.launch(Dispatchers.Main) {
            isPaused = false
            binding.btnOn.isEnabled = true
            binding.btnOff.isEnabled = false
            binding.btnPause.isEnabled = false
            imageDishwasherOff()
            stopDishwasherTimer()
            binding.tvTotalEnergyConsumption.text = Html.fromHtml(
                "<font color=${context?.getColor(R.color.masala)}>${
                    SharedPrefsUtils.getDishwasherTotalEnergyConsumption(
                        dishwasherPref
                    )
                }${" kWh"} </font>"

            )
            binding.tvEnergyMeterReading.text = Html.fromHtml(
                "<font color=${context?.getColor(R.color.masala)}>${
                    SharedPrefsUtils.getDishwasherAverageEnergyPerCycle(
                        dishwasherPref
                    )
                }${" kWh"} </font>"

            )
            binding.tvCurrentCycleEnergyComp.text = Html.fromHtml(
                "<font color=${context?.getColor(R.color.masala)}>${
                    SharedPrefsUtils.getDishwasherInCurrentCycleEnergyConsumed(
                        dishwasherPref
                    )
                }${" kWh"} </font>"
            )
        }

    }

    private fun dishwasherStartCycle() {
        scope.launch(Dispatchers.Main) {
            isPaused = false
            binding.btnOff.isEnabled = true
            binding.btnPause.isEnabled = true
            binding.btnOn.isEnabled = false
            binding.tvTotalEnergyConsumption.text = Html.fromHtml(
                "<font color=${context?.getColor(R.color.masala)}>${"0.000"}${" kWh"} </font>"

            )
            binding.tvEnergyMeterReading.text = Html.fromHtml(
                "<font color=${context?.getColor(R.color.masala)}>${"0.000"}${" kWh"} </font>"

            )
            binding.tvCurrentCycleEnergyComp.text = Html.fromHtml(
                "<font color=${context?.getColor(R.color.masala)}>${"0.000"}${" kWh"} </font>"

            )
            imageDishwasherOn()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (timer != null) {
            timer?.cancel()
        }
        if (scope != null) {
            scope.cancel()
        }
    }


}