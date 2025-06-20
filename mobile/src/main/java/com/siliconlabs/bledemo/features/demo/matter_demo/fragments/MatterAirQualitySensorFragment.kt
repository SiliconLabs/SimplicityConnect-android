package com.siliconlabs.bledemo.features.demo.matter_demo.fragments

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
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
import com.siliconlabs.bledemo.databinding.FragmentMatterAirQualitySensorBinding
import com.siliconlabs.bledemo.databinding.FragmentMatterThermostatBinding
import com.siliconlabs.bledemo.features.demo.matter_demo.activities.MatterDemoActivity
import com.siliconlabs.bledemo.features.demo.matter_demo.controller.GenericChipDeviceListener
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterLightFragment.Companion.ARG_DEVICE_MODEL
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterLightFragment.Companion.INIT
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterLightFragment.Companion.ON_OFF_CLUSTER_ENDPOINT
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannerFragment.Companion.AQI
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannerFragment.Companion.SPACE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterTemperatureSensorFragment.Companion.TEMPERATURE_UNIT
import com.siliconlabs.bledemo.features.demo.matter_demo.model.MatterScannedResultModel
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.ChipClient
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.CustomProgressDialog
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.FragmentUtils
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.MessageDialogFragment
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.SharedPrefsUtils
import com.siliconlabs.bledemo.utils.CustomToastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber


class MatterAirQualitySensorFragment : Fragment() {
    private lateinit var dialog: MessageDialogFragment
    val dialogTag = "MessageDialog"
    private lateinit var mPrefs: SharedPreferences
    private val deviceController: ChipDeviceController
        get() = ChipClient.getDeviceController(requireContext())

    private lateinit var scope: CoroutineScope
    private lateinit var binding: FragmentMatterAirQualitySensorBinding
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
        if (requireArguments() != null) {
            model = requireArguments().getParcelable(ARG_DEVICE_MODEL)!!
            deviceId = model.deviceId
            Timber.tag(TAG).e("deviceID: $model")
        }
        if (deviceId != null) {

            showMatterProgressDialog(getString(R.string.matter_device_status))

            // retrieveSavedDevices()
            CoroutineScope(Dispatchers.IO).launch {
                // This code will run asynchronously

                val resInfo = checkForDeviceStatus()
                if (resInfo) {
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

            deviceController.getConnectedDevicePointer(deviceId, object :
                GetConnectedDeviceCallbackJni.GetConnectedDeviceCallback {
                override fun onDeviceConnected(devicePointer: Long) {
                    model.isDeviceOnline = true
                    SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, true)
                }

                override fun onConnectionFailure(nodeId: Long, error: java.lang.Exception?) {
                    model.isDeviceOnline = false
                    removeProgress()
                    SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, false)
                    showMessageDialog()
                }
            })

            delay(Constants.SCAN_TIMER * 500)


            // Return the result (true or false based on the actual result)
            true
        }
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
                                    this@MatterAirQualitySensorFragment,
                                    MatterDoorFragment.CallBackHandler::class.java
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
            Timber.e("device offline $e")
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentMatterAirQualitySensorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MatterDemoActivity).hideQRScanner()
        scope = viewLifecycleOwner.lifecycleScope
        deviceController
            .setCompletionListener(AirQualitySensorChipControllerCallback())

        binding.btnReadValue.isLongClickable = false
        binding.btnReadValue.text = requireContext().getText(R.string.matter_temp_refresh)

        binding.btnReadValue.setOnClickListener {
            scope.launch { sendReadValueCommandClick() }
        }
        scope.launch {
            readSensorValue()
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {

            if (requireActivity().supportFragmentManager.backStackEntryCount > 0) {
                requireActivity().supportFragmentManager.popBackStack()
            } else {
                FragmentUtils.getHost(
                    this@MatterAirQualitySensorFragment,
                    CallBackHandler::class.java
                )
                    .onBackHandler()
            }


        }
    }

    private suspend fun getAirQualitySensorClusterForDevice(): ChipClusters.AirQualityCluster {
        return ChipClusters.AirQualityCluster(
            ChipClient.getConnectedDevicePointer(requireContext(), deviceId),
            endpointId
        )
    }

    fun roundDoubleToInt(value: Double): Int {
        val roundedValue = (value + 0.5).toInt()
        return roundedValue
    }

    fun addDecimalPointToInteger(input: Int?): Double {
        // Convert the integer to a string
        val inputString = input.toString()

        // Check if the integer has exactly 4 digits
        if (inputString.length != 4) {
            throw IllegalArgumentException("Input must be a 4-digit integer.")
        }

        // Insert a decimal point after the first two digits
        val resultString = inputString.substring(0, 2) + "." + inputString.substring(2)

        // Convert the result string to a double
        return resultString.toDouble()
    }

    private suspend fun readSensorValue() {
        getAirQualitySensorClusterForDevice()
            .subscribeAirQualityAttribute(object : ChipClusters.IntegerAttributeCallback {
                override fun onError(error: java.lang.Exception?) {
                    SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, false)
                    showMessageDialog()
                    Timber.tag(TAG).e("read local temp command failure:$error")
                }

                @SuppressLint("SetTextI18n")
                override fun onSuccess(value: Int) {
                    requireActivity().runOnUiThread {
                        displayAQIValue(value)
                    }
                }
            }, 0, 10)
    }

    private fun displayAQIValue(value: Int) {
        println("Sensor VAlue: $value")
        binding.txtValue.setText(AQI + value)
        var status: String = "-"
        when (value) {
            0 -> status = getString(R.string.matter_air_quality_sensor_unknown)//UNKNOWN
            1 -> status = getString(R.string.matter_air_quality_sensor_good) //GOOD
            2 -> status = getString(R.string.matter_air_quality_sensor_fair) //FAIR

            3 -> status = getString(R.string.matter_air_quality_sensor_moderate) //MODERATE
            4 -> status = getString(R.string.matter_air_quality_sensor_poor) //POOR
            5 -> status = getString(R.string.matter_air_quality_sensor_v_poor) //V_POOR
            6 -> status = getString(R.string.matter_air_quality_sensor_e_poor) //E_POOR
        }
        binding.txtStatus.setText(status)
    }

    private suspend fun sendReadValueCommandClick() {
        getAirQualitySensorClusterForDevice().readAirQualityAttribute(object :
            ChipClusters.IntegerAttributeCallback {
            override fun onError(error: java.lang.Exception?) {
                SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, false)
                showMessageDialog()
                Timber.tag(TAG).e("read local temp command failure:$error")
            }

            @SuppressLint("SetTextI18n")
            override fun onSuccess(value: Int) {
                requireActivity().runOnUiThread {
                    displayAQIValue(value)
                }
            }

        })

    }


    private fun showMessage(msg: String) {
        requireActivity().runOnUiThread {
            //Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            CustomToastManager.show(
                requireContext(),msg,5000
            )
        }
    }


    inner class AirQualitySensorChipControllerCallback : GenericChipDeviceListener() {
        override fun onConnectDeviceComplete() {}

        override fun onCommissioningComplete(nodeId: Long, errorCode: Long) {
            Timber.d(TAG, "onCommissioningComplete for nodeId $nodeId: $errorCode")
            showMessage("Address update complete for nodeId $nodeId with code $errorCode")
        }

        override fun onNotifyChipConnectionClosed() {
            Timber.d(TAG, "onNotifyChipConnectionClosed")
        }

        override fun onCloseBleComplete() {
            Timber.d(TAG, "onCloseBleComplete")
        }

        override fun onError(error: Throwable?) {
            super.onError(error)
            Timber.d(TAG, "onError : $error")
        }
    }

    interface CallBackHandler {
        fun onBackHandler()
    }

    companion object {
        private val TAG = Companion::class.java.simpleName.toString()
        fun newInstance(): MatterAirQualitySensorFragment = MatterAirQualitySensorFragment()
    }


}