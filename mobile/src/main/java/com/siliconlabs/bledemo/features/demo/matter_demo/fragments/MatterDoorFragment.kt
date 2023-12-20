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
import com.siliconlabs.bledemo.bluetooth.beacon_utils.eddystone.Constants.SCAN_TIMER
import com.siliconlabs.bledemo.databinding.FragmentMatterDoorLightBinding
import com.siliconlabs.bledemo.features.demo.matter_demo.activities.MatterDemoActivity
import com.siliconlabs.bledemo.features.demo.matter_demo.controller.GenericChipDeviceListener
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterLightFragment.Companion.ARG_DEVICE_MODEL
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterLightFragment.Companion.INIT
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterLightFragment.Companion.ON_OFF_CLUSTER_ENDPOINT
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterLightFragment.Companion.TIME_OUT
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


class MatterDoorFragment : Fragment() {

    private lateinit var dialog: MessageDialogFragment
    private val dialogTag = "MessageDialog"
    private val deviceController: ChipDeviceController
        get() = ChipClient.getDeviceController(requireContext())
    private lateinit var mPrefs: SharedPreferences
    private var scannedDeviceList = ArrayList<MatterScannedResultModel>()
    private lateinit var scope: CoroutineScope
    private lateinit var binding: FragmentMatterDoorLightBinding
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
            Timber.tag(TAG).e("deviceID: " + model)
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

    }


    private suspend fun checkForDeviceStatus(): Boolean {
        return withContext(Dispatchers.Default) {
            // Simulate a time-consuming task

            deviceController.getConnectedDevicePointer(deviceId,
                object : GetConnectedDeviceCallbackJni.GetConnectedDeviceCallback {
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

            delay(SCAN_TIMER * 1000)
            // handleSubscription()
            true
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
        binding = FragmentMatterDoorLightBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MatterDemoActivity).hideQRScanner()
        scope = viewLifecycleOwner.lifecycleScope
        deviceController.setCompletionListener(DoorChipControllerCallback())
        binding.btnMatterDeviceState.setImageResource(R.drawable.door_lock)
        binding.txtClusterName.text = requireContext().getText(R.string.matter_door_title)
        binding.btnOn.setLongClickable(false);
        binding.btnOff.setLongClickable(false);

        binding.btnOn.text = requireContext().getText(R.string.matter_locked_status)
        binding.btnOff.text = requireContext().getText(R.string.matter_unlock_status)
        binding.btnToggle.visibility = View.GONE
        binding.btnOn.setOnClickListener {
            scope.launch {
                // showMatterProgressDialog(getString(R.string.please_wait))
                sendLockCommandClick()
            }
        }

        binding.btnOff.setOnClickListener {
            scope.launch {
                sendUnlockCommandClick()
            }
        }
        //back handling
        requireActivity().onBackPressedDispatcher.addCallback(
            requireActivity(),
            onBackPressedCallback
        )
    }

    override fun onAttach(@NotNull context: Context) {
        super.onAttach(context)
    }

    override fun onDetach() {
        super.onDetach()
    }

    private fun showMessageDialog() {

        try {
            if (isAdded && requireActivity() != null && !requireActivity().isFinishing) {
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
                                    this@MatterDoorFragment,
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
            Timber.e("device offline " + e)
        }

    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (requireActivity().supportFragmentManager.getBackStackEntryCount() > 0) {
                requireActivity().supportFragmentManager.popBackStack();
            } else {
                FragmentUtils.getHost(this@MatterDoorFragment, CallBackHandler::class.java)
                    .onBackHandler()
            }
        }
    }

    private suspend fun getLockUnlockClusterForDevice(): ChipClusters.DoorLockCluster {
        return ChipClusters.DoorLockCluster(
            ChipClient.getConnectedDevicePointer(requireContext(), deviceId),
            endpointId
        )
    }

    private suspend fun handleSubscription() {
        getLockUnlockClusterForDevice().subscribeLockStateAttribute(
            object : ChipClusters.DoorLockCluster.LockStateAttributeCallback {
                override fun onSuccess(value: Int?) {
                    if (isAdded && !requireActivity().isFinishing) {
                        when (value) {
                            1 -> {
                                binding.btnMatterDeviceState.setImageResource(R.drawable.door_lock)
                            }

                            2 -> {
                                binding.btnMatterDeviceState.setImageResource(R.drawable.door_unlock)
                            }

                            else -> println("Unhandled Event....")
                        }
                    }
                }

                override fun onError(ex: java.lang.Exception?) {
                    Timber.tag(TAG).e("LockStateAttributeCallback Error $ex")
                }

            },
            MatterTemperatureSensorFragment.MIN_REFRESH_PERIOD_S,
            MatterTemperatureSensorFragment.MAX_REFRESH_PERIOD_S
        )
    }

    private suspend fun sendLockCommandClick() {

        getLockUnlockClusterForDevice().lockDoor(
            object : ChipClusters.DefaultClusterCallback {
                override fun onSuccess() {
                    removeProgress()
                    Timber.tag(TAG).e("lock command Success")
                    binding.btnMatterDeviceState.setImageResource(R.drawable.door_lock)
                }

                override fun onError(error: Exception?) {
                    removeProgress()
                    SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, false)
                    showMessageDialog()

                }

            }, null,
            TIME_OUT
        )
    }

    private suspend fun sendUnlockCommandClick() {
        getLockUnlockClusterForDevice().unlockDoor(
            object : ChipClusters.DefaultClusterCallback {
                override fun onSuccess() {
                    Timber.tag(TAG).e("Unlock command Success")
                    SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, true)
                    binding.btnMatterDeviceState.setImageResource(R.drawable.door_unlock)
                }

                @SuppressLint("TimberArgCount")
                override fun onError(error: Exception?) {
                    SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, false)
                    Timber.tag(TAG).e("Unlock command failure: $error")
                    showMessageDialog()
                }

            }, null,
            TIME_OUT
        )
    }

    private fun showMessage(msg: String) {
        requireActivity().runOnUiThread {
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }


    inner class DoorChipControllerCallback : GenericChipDeviceListener() {
        override fun onConnectDeviceComplete() {}

        override fun onCommissioningComplete(nodeId: Long, errorCode: Int) {
            Timber.tag(TAG).d("onCommissioningComplete for nodeId $nodeId: $errorCode")
            // showMessage("Address update complete for nodeId $nodeId with code $errorCode")
        }

        override fun onNotifyChipConnectionClosed() {
            Timber.tag(TAG).d("onNotifyChipConnectionClosed")
        }

        override fun onCloseBleComplete() {
            Timber.tag(TAG).d("onCloseBleComplete")
        }

        override fun onError(error: Throwable) {
            super.onError(error)
            Timber.tag(TAG).d("onError : $error")
        }
    }

    interface CallBackHandler {
        fun onBackHandler()
    }

    companion object {
        private val TAG = MatterDoorFragment.javaClass.simpleName.toString()

        fun newInstance(): MatterDoorFragment = MatterDoorFragment()
    }

}


