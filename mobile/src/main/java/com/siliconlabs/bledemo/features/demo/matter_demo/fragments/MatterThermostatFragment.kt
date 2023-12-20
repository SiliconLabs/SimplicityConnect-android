package com.siliconlabs.bledemo.features.demo.matter_demo.fragments

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
import com.siliconlabs.bledemo.databinding.FragmentMatterThermostatBinding
import com.siliconlabs.bledemo.features.demo.matter_demo.activities.MatterDemoActivity
import com.siliconlabs.bledemo.features.demo.matter_demo.controller.GenericChipDeviceListener
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterConnectFragment.Companion.SPACE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterLightFragment.Companion.ARG_DEVICE_INFO
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterLightFragment.Companion.ARG_DEVICE_MODEL
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterLightFragment.Companion.INIT
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterLightFragment.Companion.ON_OFF_CLUSTER_ENDPOINT
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterTemperatureSensorFragment.Companion.TEMPERATURE_UNIT
import com.siliconlabs.bledemo.features.demo.matter_demo.model.CHIPDeviceInfo
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
import java.text.DecimalFormat


class MatterThermostatFragment : Fragment() {
    private lateinit var dialog: MessageDialogFragment
    val dialogTag = "MessageDialog"
    private lateinit var mPrefs: SharedPreferences
    private val deviceController: ChipDeviceController
        get() = ChipClient.getDeviceController(requireContext())

    private lateinit var scope: CoroutineScope
    private lateinit var binding: FragmentMatterThermostatBinding
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
            Timber.tag(TAG).e( "deviceID: " + model)
        }
        if (deviceId != null) {

            showMatterProgressDialog(getString(R.string.please_wait))

            // retrieveSavedDevices()
            GlobalScope.launch {
                // This code will run asynchronously

                val resultq = checkForDeviceStatus()
                if (resultq) {
                    println("Operation was successful")
                    removeProgress()
                    // prepareList()

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

    override fun onAttach(@NotNull context: Context) {
        super.onAttach(context)
    }

    override fun onDetach() {
        super.onDetach()
    }
    private fun showMessageDialog() {
        try {
            if (isAdded() && requireActivity()!=null && !requireActivity().isFinishing){
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
                                    this@MatterThermostatFragment,
                                    MatterDoorFragment.CallBackHandler::class.java
                                )
                                    .onBackHandler()
                            }
                        }
                        val transaction: FragmentTransaction =
                            requireActivity().supportFragmentManager.beginTransaction()

                        dialog.show(transaction, dialogTag)
                    }
                }
            }else{
                Timber.e("device offline")
            }
        }catch (e:Exception){
            Timber.e("device offline"+e)
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

        binding = FragmentMatterThermostatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MatterDemoActivity).hideQRScanner()
        scope = viewLifecycleOwner.lifecycleScope
        deviceController.setCompletionListener(DoorChipControllerCallback())

        binding.btnReadValue.setLongClickable(false);
        binding.btnReadValue.setText(requireContext().getText(R.string.matter_temp_refresh))

        binding.btnReadValue.setOnClickListener {
            scope.launch { sendReadValueCommandClick() }
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {

            if (requireActivity().supportFragmentManager.getBackStackEntryCount() > 0) {
                requireActivity().supportFragmentManager.popBackStack();
            } else {
                FragmentUtils.getHost(this@MatterThermostatFragment, CallBackHandler::class.java)
                    .onBackHandler()
            }


        }
    }

    private suspend fun getThermostatClusterForDevice(): ChipClusters.ThermostatCluster {
        return ChipClusters.ThermostatCluster(
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

    private suspend fun sendReadValueCommandClick() {
        getThermostatClusterForDevice().readLocalTemperatureAttribute(object :
            ChipClusters.ThermostatCluster.LocalTemperatureAttributeCallback {
            override fun onSuccess(value: Int?) {
                SharedPrefsUtils.updateDeviceByDeviceId(mPrefs,deviceId,true)
                requireActivity().runOnUiThread(Runnable {

                    val resValue = roundDoubleToInt(addDecimalPointToInteger(value))
                    Log.e(
                        TAG,
                        "read local temp command Success: " + addDecimalPointToInteger(value)
                    )
                    binding.txtValue.setText(SPACE + (resValue) + TEMPERATURE_UNIT)
                })


            }

            override fun onError(ex: Exception?) {
               // removeProgress()
                SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, false)
                showMessageDialog()
                Timber.tag(TAG).e( "read local temp command failure", ex)
            }
        })
    }


    private fun showMessage(msg: String) {
        requireActivity().runOnUiThread {
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }


    inner class DoorChipControllerCallback : GenericChipDeviceListener() {
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
        private val TAG = MatterThermostatFragment.javaClass.simpleName.toString()

        fun newInstance(): MatterThermostatFragment = MatterThermostatFragment()
    }


}