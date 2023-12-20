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
import com.siliconlabs.bledemo.databinding.FragmentMatterContactSensorBinding
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.NotNull
import timber.log.Timber
import kotlin.Exception


class MatterContactSensorFragment : Fragment() {

    private lateinit var dialog: MessageDialogFragment
    val dialogTag = "MessageDialog"
    private val deviceController: ChipDeviceController
        get() = ChipClient.getDeviceController(requireContext())
    private lateinit var mPrefs: SharedPreferences
    private lateinit var scope: CoroutineScope
    private lateinit var binding: FragmentMatterContactSensorBinding
    private var deviceId: Long = INIT
    private var endpointId: Int = ON_OFF_CLUSTER_ENDPOINT
    private lateinit var model: MatterScannedResultModel
    private var customProgressDialog: CustomProgressDialog? = null
    private val scopeFun = MainScope()
    private var job: Job? = null

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
        startUpdates()
        //back handling
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun removeProgress() {
        if (customProgressDialog?.isShowing() == true) {
            customProgressDialog?.dismiss()
        }
    }

    private suspend fun checkForDeviceStatus(): Boolean {
        return withContext(Dispatchers.Default) {
            // Simulate a time-consuming task

            deviceController.getConnectedDevicePointer(deviceId, object :
                GetConnectedDeviceCallbackJni.GetConnectedDeviceCallback {
                override fun onDeviceConnected(devicePointer: Long) {
                    model.isDeviceOnline = true
                    SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, true)
                          removeProgress()

                }

                override fun onConnectionFailure(nodeId: Long, error: Exception?) {
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

        binding = FragmentMatterContactSensorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MatterDemoActivity).hideQRScanner()
        scope = viewLifecycleOwner.lifecycleScope
        deviceController.setCompletionListener(ContactSensorChipControllerCallback())
        //default state
        binding.imageContactSensorClose.visibility = View.GONE
        binding.imageContactSensorOpen.visibility = View.VISIBLE

    }

    fun startUpdates() {
        stopUpdates()
        job = scopeFun.launch {
            while (true) {
                // getData() // the function that should be ran every second
                // sendReadValueCommandClick()
                sendReadValue()
                delay(2000)
            }
        }
    }

    fun stopUpdates() {
        job?.cancel()
        job = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopUpdates()
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            requireActivity().supportFragmentManager.popBackStack()
            requireActivity().supportFragmentManager.popBackStack()
            FragmentUtils.getHost(
                this@MatterContactSensorFragment,
                MatterLightFragment.CallBackHandler::class.java
            ).onBackHandler()
        }
    }

    private suspend fun getContactSensorClusterForDevice(): ChipClusters.BooleanStateCluster {
        return ChipClusters.BooleanStateCluster(
            ChipClient.getConnectedDevicePointer(requireContext(), deviceId),
            endpointId
        )
    }


    private suspend fun sendReadValue() {
        getContactSensorClusterForDevice().readStateValueAttribute(
            object : ChipClusters.BooleanAttributeCallback {
                override fun onSuccess(value: Boolean) {
                   // removeProgress()
                    println("Contact Value : $value")
                    SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, true)
                    requireActivity().runOnUiThread {
                        if (value) {
                            binding.imageContactSensorOpen.visibility = View.GONE
                            binding.imageContactSensorClose.visibility = View.VISIBLE
                        } else {
                            binding.imageContactSensorOpen.visibility = View.VISIBLE
                            binding.imageContactSensorClose.visibility = View.GONE
                        }
                    }
                }

                override fun onError(error: Exception?) {
                    // removeProgress()
                    SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, false)
                    showMessageDialog()

                    Timber.tag(TAG).e( "error readStateValueAttribute " + error)
                }

            }
        )
    }


    private fun showMessage(msg: String) {
        requireActivity().runOnUiThread {
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
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
                                    this@MatterContactSensorFragment,
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
            } else {
                Timber.e(TAG, "device offline")
            }
        }catch (e:Exception){
            Timber.e(TAG,""+ e)
        }



    }

    inner class ContactSensorChipControllerCallback : GenericChipDeviceListener() {
        override fun onConnectDeviceComplete() {}

        override fun onCommissioningComplete(nodeId: Long, errorCode: Int) {
             Timber.tag(TAG).d( "onCommissioningComplete for nodeId $nodeId: $errorCode")
          //  showMessage("Address update complete for nodeId $nodeId with code $errorCode")
        }

        override fun onNotifyChipConnectionClosed() {
             Timber.tag(TAG).d( "onNotifyChipConnectionClosed")
        }

        override fun onCloseBleComplete() {
             Timber.tag(TAG).d( "onCloseBleComplete")
        }

        override fun onError(error: Throwable) {
            super.onError(error)
             Timber.tag(TAG).d( "onError : $error")
        }
    }

    interface CallBackHandler {
        fun onBackHandler()
    }

    companion object {
        private val TAG = MatterContactSensorFragment.javaClass.simpleName.toString()

        fun newInstance(): MatterContactSensorFragment = MatterContactSensorFragment()
    }
}