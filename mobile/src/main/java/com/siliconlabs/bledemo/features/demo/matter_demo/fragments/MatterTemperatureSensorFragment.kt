package com.siliconlabs.bledemo.features.demo.matter_demo.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import chip.devicecontroller.ChipClusters
import chip.devicecontroller.ChipDeviceController
import chip.devicecontroller.GetConnectedDeviceCallbackJni
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.bluetooth.beacon_utils.eddystone.Constants
import com.siliconlabs.bledemo.databinding.FragmentMatterTemperatureSensorBinding
import com.siliconlabs.bledemo.features.demo.matter_demo.activities.MatterDemoActivity
import com.siliconlabs.bledemo.features.demo.matter_demo.controller.GenericChipDeviceListener
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterLightFragment.Companion.ARG_DEVICE_MODEL
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterLightFragment.Companion.INIT
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterLightFragment.Companion.ON_OFF_CLUSTER_ENDPOINT
import com.siliconlabs.bledemo.features.demo.matter_demo.model.MatterScannedResultModel
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.ChipClient
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.CustomProgressDialog
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.FragmentUtils
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.MessageDialogFragment
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.SharedPrefsUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.NotNull
import timber.log.Timber


class MatterTemperatureSensorFragment : Fragment() {
    private lateinit var dialog: MessageDialogFragment
    val dialogTag = "MessageDialog"
    private lateinit var mPrefs: SharedPreferences
    private val deviceController: ChipDeviceController
        get() = ChipClient.getDeviceController(requireContext())

    private lateinit var scope: CoroutineScope
    private lateinit var binding: FragmentMatterTemperatureSensorBinding
    private var deviceId: Long = INIT
    private var endpointId: Int = ON_OFF_CLUSTER_ENDPOINT
    private lateinit var model: MatterScannedResultModel
    private var customProgressDialog: CustomProgressDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPrefs = requireContext().getSharedPreferences(
            "your_preference_name",
            AppCompatActivity.MODE_PRIVATE
        )
        model = requireArguments().getParcelable(ARG_DEVICE_MODEL)!!
        deviceId = model.deviceId

        if (deviceId != null) {

            showMatterProgressDialog(getString(R.string.please_wait))

            // retrieveSavedDevices()
            GlobalScope.launch {
                // This code will run asynchronously

                val resultq = checkForDeviceStatus()
                if (resultq) {
                    println("Operation was successful")
                    removeProgress()
                }
            }
        }

        //back handling
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private suspend fun checkForDeviceStatus(): Boolean {
        return withContext(Dispatchers.Default) {
            // Simulate a time-consuming task

            deviceController.getConnectedDevicePointer(deviceId, object :
                GetConnectedDeviceCallbackJni.GetConnectedDeviceCallback {
                override fun onDeviceConnected(devicePointer: Long) {
                    model.isDeviceOnline = true
                    SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, true)
                }

                override fun onConnectionFailure(nodeId: Long, error: Exception?) {
                    model.isDeviceOnline = false
                    removeProgress()
                    SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, false)

                    // showMessageDialog(getString(R.string.matter_device_offline_text))
                }
            })

            delay(Constants.SCAN_TIMER * 500)


            // Return the result (true or false based on the actual result)
            true
        }
    }

    override fun onAttach(@NotNull context: Context) {
        super.onAttach(context)
    }

    override fun onDetach() {
        super.onDetach()
    }

    private fun showMessageDialog() {
        try {
            if (isAdded() && requireActivity() != null && !requireActivity().isFinishing) {
                requireActivity().runOnUiThread {
                    if (!MessageDialogFragment.isDialogShowing()) {
                        dialog = MessageDialogFragment()
                        dialog.setMessage(getString(R.string.matter_device_offline_text))
                        dialog.setOnDismissListener {
                            removeProgress()
                            if (requireActivity().supportFragmentManager.getBackStackEntryCount() > 0) {
                                requireActivity().supportFragmentManager.popBackStack();
                            } else {
                                FragmentUtils.getHost(
                                    this@MatterTemperatureSensorFragment,
                                    CallBackHandler::class.java
                                )
                                    .onBackHandler()
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
            Timber.e("" + e)
        }


    }

    private fun removeProgress() {
        if (customProgressDialog?.isShowing() == true) {
            customProgressDialog?.dismiss()
        }
    }

    private fun showMatterProgressDialog(message: String) {
        customProgressDialog = CustomProgressDialog(requireContext())
        customProgressDialog!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        customProgressDialog!!.setMessage(message)
        customProgressDialog!!.show()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentMatterTemperatureSensorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MatterDemoActivity).hideQRScanner()
        scope = viewLifecycleOwner.lifecycleScope
        deviceController.setCompletionListener(TempSensorChipControllerCallback())
        binding.btnReadValue.setLongClickable(false);
        binding.btnReadValue.setText(requireContext().getText(R.string.matter_temp_refresh))
        scope.launch {
            readValueCommand()
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {

            if (requireActivity().supportFragmentManager.getBackStackEntryCount() > 0) {
                requireActivity().supportFragmentManager.popBackStack();
            } else {
                FragmentUtils.getHost(
                    this@MatterTemperatureSensorFragment,
                    CallBackHandler::class.java
                ).onBackHandler()
            }


        }
    }

    private suspend fun getTempSensorClusterForDevice(): ChipClusters
    .TemperatureMeasurementCluster {
        return ChipClusters.TemperatureMeasurementCluster(
            ChipClient.getConnectedDevicePointer(requireContext(), deviceId),
            endpointId
        )
    }

    fun roundDoubleToInt(value: Double): Int {
        val roundedValue = (value + 0.5).toInt()
        return roundedValue
    }

    fun addDecimalPointToInteger(input: Int?): Double {
        /*   // Convert the integer to a string
           var inputString = input.toString()

           if (inputString.length <= 1) {
               inputString = "0000"
           }

           // Insert a decimal point after the first two digits
           val resultString = inputString.substring(0, 2) + "." + inputString.substring(2)

           // Convert the result string to a double*/

        if (input == null) {
            // Handle the case when the input is null
            return 0.0
        }

        // Convert the integer to a string
        var inputString = input.toString()

        // Ensure the string has at least two characters
        while (inputString.length < 2) {
            inputString = "0$inputString"
        }

        // Check if the integer is a two-digit number
        if (inputString.length == 2) {
            return inputString.toDouble()
        }

        // Insert a decimal point after the first two absolute digits for negative input
        val resultString =
            if (input < 0) {
                "-${inputString.substring(1, inputString.length - 2)}.${
                    inputString.substring(
                        inputString.length - 2
                    )
                }"
            } else {
                // If the input is positive, insert the decimal point after the first two digits
                "${inputString.substring(0, inputString.length - 2)}.${
                    inputString.substring(
                        inputString.length - 2
                    )
                }"
            }

        return resultString.toDouble()
    }

    private suspend fun readValueCommand() {
        getTempSensorClusterForDevice().subscribeMeasuredValueAttribute(object :
            ChipClusters.TemperatureMeasurementCluster.MeasuredValueAttributeCallback {
            @SuppressLint("SetTextI18n")
            override fun onSuccess(value: Int?) {
                SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, true)
                requireActivity().runOnUiThread(Runnable {

                    val resValue = (addDecimalPointToInteger(value))

                    binding.txtValue.text = SPACE + (resValue) + TEMPERATURE_UNIT
                })
            }

            override fun onError(ex: Exception?) {
                Timber.e(TAG, "read local temp command failure", ex)
                // removeProgress()
                SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, false)
                showMessageDialog()
            }
        }, MIN_REFRESH_PERIOD_S, MAX_REFRESH_PERIOD_S)
    }

    private suspend fun sendReadValueCommandClick() {
        getTempSensorClusterForDevice().readMeasuredValueAttribute(object :
            ChipClusters.TemperatureMeasurementCluster.MeasuredValueAttributeCallback {
            @SuppressLint("SetTextI18n")
            override fun onSuccess(value: Int?) {
                // removeProgress()
                requireActivity().runOnUiThread(Runnable {

                    val resValue = addDecimalPointToInteger(value)
                    binding.txtValue.text = SPACE + (resValue) + TEMPERATURE_UNIT
                })
            }

            override fun onError(ex: java.lang.Exception?) {
                SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, false)
                Timber.tag(TAG).e( "read local temp command failure"+ ex)
                showMessageDialog()

            }
        })


    }


    private fun showMessage(msg: String) {
        requireActivity().runOnUiThread {
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }


    inner class TempSensorChipControllerCallback : GenericChipDeviceListener() {
        override fun onConnectDeviceComplete() {}

        override fun onCommissioningComplete(nodeId: Long, errorCode: Int) {
            Timber.d(TAG, "onCommissioningComplete for nodeId $nodeId: $errorCode")
            //showMessage("Address update complete for nodeId $nodeId with code $errorCode")
        }

        override fun onNotifyChipConnectionClosed() {
            Timber.d(TAG, "onNotifyChipConnectionClosed")
        }

        override fun onCloseBleComplete() {
            Timber.d(TAG, "onCloseBleComplete")
        }

        override fun onError(error: Throwable) {
            super.onError(error)
            Timber.d(TAG, "onError : $error")
        }
    }

    interface CallBackHandler {
        fun onBackHandler()
    }

    companion object {
        const val MIN_REFRESH_PERIOD_S = 2
        const val MAX_REFRESH_PERIOD_S = 10
        const val TEMPERATURE_UNIT = " \u2103"
        const val SPACE = ""

        private val TAG = MatterTemperatureSensorFragment.javaClass.simpleName.toString()

        fun newInstance(): MatterTemperatureSensorFragment = MatterTemperatureSensorFragment()
    }


}