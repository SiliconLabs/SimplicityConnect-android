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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import chip.devicecontroller.ChipClusters
import chip.devicecontroller.ChipDeviceController
import chip.devicecontroller.GetConnectedDeviceCallbackJni
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.bluetooth.beacon_utils.eddystone.Constants
import com.siliconlabs.bledemo.databinding.FragmentMatterPlugBinding
import com.siliconlabs.bledemo.features.demo.matter_demo.activities.MatterDemoActivity
import com.siliconlabs.bledemo.features.demo.matter_demo.controller.GenericChipDeviceListener
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


class MatterPlugFragment : Fragment() {
    private lateinit var dialog: MessageDialogFragment
    val dialogTag = "MessageDialog"
    private lateinit var mPrefs: SharedPreferences
    private val deviceController: ChipDeviceController
        get() = ChipClient.getDeviceController(requireContext())
    private var customProgressDialog: CustomProgressDialog? = null
    private lateinit var scope: CoroutineScope
    private lateinit var binding: FragmentMatterPlugBinding
    private var deviceId: Long = 0
    private var endpointId: Int = ON_OFF_CLUSTER_ENDPOINT
    private lateinit var model: MatterScannedResultModel


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
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMatterPlugBinding.inflate(inflater, container, false)
        return binding.root
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

                override fun onConnectionFailure(nodeId: Long, error: java.lang.Exception?) {
                    model.isDeviceOnline = false
                    removeProgress()
                    SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, false)
                    showMessageDialog()
                    requireActivity().runOnUiThread(Runnable {
                        binding.btnMatterDeviceState.setImageResource(R.drawable.matter_plug_off)
                        binding.btnMatterDeviceState.setColorFilter(
                            ContextCompat.getColor(requireContext(), R.color.silabs_grey),
                            android.graphics.PorterDuff.Mode.SRC_IN
                        )
                    })
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
                                    this@MatterPlugFragment,
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scope = viewLifecycleOwner.lifecycleScope
        deviceController.setCompletionListener(LightChipControllerCallback())
        binding.btnMatterDeviceState.setImageResource(R.drawable.matter_plug_off)
        binding.btnOn.setLongClickable(false);
        binding.btnOff.setLongClickable(false);
        binding.btnOff.text = requireContext().getText(R.string.matter_light_off_status)
        binding.btnOn.text = requireContext().getText(R.string.matter_light_on_status)

        binding.btnOn.setOnClickListener {
            scope.launch { sendOnCommandClick() }
        }

        binding.btnOff.setOnClickListener {
            scope.launch { sendOffCommandClick() }
        }
        (activity as MatterDemoActivity).hideQRScanner()

        //back handling
        requireActivity().onBackPressedDispatcher.addCallback(
            requireActivity(),
            onBackPressedCallback
        )
    }


    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (requireActivity().supportFragmentManager.getBackStackEntryCount() > 0) {
                requireActivity().supportFragmentManager.popBackStack();
            } else {
                FragmentUtils.getHost(this@MatterPlugFragment, CallBackHandler::class.java)
                    .onBackHandler()
            }

        }
    }

    private suspend fun getPlugCluster(): ChipClusters.OnOffCluster {
        return ChipClusters.OnOffCluster(
            ChipClient.getConnectedDevicePointer(requireContext(), deviceId),
            endpointId
        )
    }

    private suspend fun sendOnCommandClick() {
        getPlugCluster().on(object : ChipClusters.DefaultClusterCallback {
            override fun onSuccess() {
                SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, true)
                requireActivity().runOnUiThread(Runnable {
                    binding.btnMatterDeviceState.setImageResource(R.drawable.matter_plug_on)
                })

            }

            override fun onError(ex: Exception) {
                removeProgress()
                SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, false)
                Timber.tag(TAG).e( "ON command failure", ex)
                showMessageDialog()
            }

        })
    }

    private suspend fun sendOffCommandClick() {
        getPlugCluster().off(object : ChipClusters.DefaultClusterCallback {
            override fun onSuccess() {
                SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, true)
                requireActivity().runOnUiThread(Runnable {
                    binding.btnMatterDeviceState.setImageResource(R.drawable.matter_plug_off)
                })

            }

            override fun onError(ex: Exception) {
                removeProgress()
                SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, false)
                Timber.tag(TAG).e( "OFF command failure", ex)
                showMessageDialog()
            }
        })
    }

    private fun showMessage(msg: String) {
        requireActivity().runOnUiThread {
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        model.isDeviceOnline = false
    }


    inner class LightChipControllerCallback : GenericChipDeviceListener() {
        override fun onConnectDeviceComplete() {}

        override fun onCommissioningComplete(nodeId: Long, errorCode: Int) {
             Timber.tag(TAG).d( "onCommissioningComplete for nodeId $nodeId: $errorCode")
            //showMessage("Address update complete for nodeId $nodeId with code $errorCode")
        }

        override fun onNotifyChipConnectionClosed() {
             Timber.tag(TAG).d( "onNotifyChipConnectionClosed")
        }

        override fun onCloseBleComplete() {
             Timber.tag(TAG).d( "onCloseBleComplete")
        }

        override fun onError(error: Throwable) {
            super.onError(error)
             Timber.tag(TAG).d( "onError: $error")
        }

    }

    interface CallBackHandler {
        fun onBackHandler()
    }

    companion object {
        private const val TAG = "MatterPlugFragment"
        public const val ARG_DEVICE_MODEL = "device_model"
        fun newInstance(): MatterPlugFragment = MatterPlugFragment()
    }
}