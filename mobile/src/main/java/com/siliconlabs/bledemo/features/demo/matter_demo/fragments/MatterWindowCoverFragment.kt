package com.siliconlabs.bledemo.features.demo.matter_demo.fragments

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
import com.siliconlabs.bledemo.databinding.FragmentMatterWindowCoverBinding
import com.siliconlabs.bledemo.features.demo.matter_demo.activities.MatterDemoActivity
import com.siliconlabs.bledemo.features.demo.matter_demo.controller.GenericChipDeviceListener
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterLightFragment.Companion.ARG_DEVICE_MODEL
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber


class MatterWindowCoverFragment : Fragment() {
    private lateinit var dialog: MessageDialogFragment
    val dialogTag = "MessageDialog"
    private lateinit var mPrefs: SharedPreferences
    private val deviceController: ChipDeviceController
        get() = ChipClient.getDeviceController(requireContext())
    private var customProgressDialog: CustomProgressDialog? = null
    private lateinit var scope: CoroutineScope

    private var deviceId: Long = 0
    private var endpointId: Int = ON_OFF_CLUSTER_ENDPOINT
    private lateinit var model: MatterScannedResultModel
    private lateinit var binding: FragmentMatterWindowCoverBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPrefs = requireContext().getSharedPreferences(
            MatterDemoActivity.MATTER_PREF,
            AppCompatActivity.MODE_PRIVATE
        )
        if (requireArguments() != null) {
            model = requireArguments().getParcelable(ARG_DEVICE_MODEL)!!
            deviceId = model.deviceId

        }

        if (deviceId != null) {

            showMatterProgressDialog(getString(R.string.matter_device_status))

            // retrieveSavedDevices()
            CoroutineScope(Dispatchers.IO).launch {
                val resultq = checkForDeviceStatus()
                if (resultq) {
                    removeProgress()
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
                        Timber.tag(TAG)
                            .e("devicePointer :$devicePointer + nodeid :$deviceId")
                        model.isDeviceOnline = true
                        SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, true)
                        // removeProgress()

                    }

                    override fun onConnectionFailure(nodeId: Long, error: Exception?) {
                        model.isDeviceOnline = false
                        removeProgress()
                        SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, false)

                        showMessageDialog(getString(R.string.matter_device_offline_text))
                    }
                })

            delay(Constants.SCAN_TIMER * 500)

            true
        }
    }

    private suspend fun handleSubscriptionForWindowCover() {
        getWindowClusterForDevice().subscribeModeAttribute(
            object : ChipClusters.IntegerAttributeCallback {
                override fun onSuccess(value: Int) {
                    println("value  $value")
                }

                override fun onError(error: java.lang.Exception?) {
                    Timber.tag(TAG).e("handleSubscriptionForWindowCover Error:$error")
                }
            },
            MatterTemperatureSensorFragment.MIN_REFRESH_PERIOD_S,
            MatterTemperatureSensorFragment.MAX_REFRESH_PERIOD_S
        )
    }

    private fun showMessageDialog(msg: String) {
        if (isAdded && !requireActivity().isFinishing) {
            requireActivity().runOnUiThread {
                if (!MessageDialogFragment.isDialogShowing()) {
                    dialog = MessageDialogFragment()
                    dialog.setMessage(msg)
                    dialog.setOnDismissListener {
                        removeProgress()

                        if (requireActivity().supportFragmentManager.backStackEntryCount > 0) {
                            requireActivity().supportFragmentManager.popBackStack()
                        } else {
                            FragmentUtils.getHost(
                                this@MatterWindowCoverFragment,
                                CallBackHandler::class.java
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
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentMatterWindowCoverBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scope = viewLifecycleOwner.lifecycleScope
        deviceController.setCompletionListener(WindowCoverChipControllerCallback())
        scope.launch {
            readTargetPositionLift()
            readTargetPositionTilt()
        }
        binding.btnMatterWindowState.setImageResource(R.drawable.w0)
        binding.overlayImage.setImageResource(R.drawable.w0)

        binding.btnWindowClose.isLongClickable = false
        binding.btnWindowOpen.isLongClickable = false
        binding.btnWindowClose.text = requireContext().getText(R.string.matter_window_close)
        binding.btnWindowOpen.text = requireContext().getText(R.string.matter_window_open)

        binding.btnWindowLift.isLongClickable = false
        binding.btnWindowLift.isLongClickable = false
        binding.btnWindowLift.text = requireContext().getText(R.string.lift)

        binding.btnWindowTilt.isLongClickable = false
        binding.btnWindowTilt.isLongClickable = false
        binding.btnWindowTilt.text = requireContext().getText(R.string.tilt)


        binding.btnWindowOpen.setOnClickListener {
            showMatterProgressDialog(getString(R.string.matter_door_lock_in_progress))
            scope.launch {
                sendWindowOpenCommandClick()
            }
        }

        binding.btnWindowClose.setOnClickListener {
            showMatterProgressDialog(getString(R.string.matter_door_lock_in_progress))
            scope.launch {
                sendWindowCloseCommandClick()
            }
        }

        binding.btnWindowLift.setOnClickListener {
            showMatterProgressDialog(getString(R.string.matter_door_lock_in_progress))
            scope.launch {
                if (!binding.etLift.text.isNullOrEmpty()) {
                    var liftPtage = binding.etLift.text.toString().toIntOrNull()
                    if (liftPtage != null && liftPtage in 1..100) {
                        liftPtage = 100 - liftPtage
                        sendWindowLiftCommandClick(liftPtage)
                    } else {
                        removeProgress()
                        showMessage(getString(R.string.enter_valid_lift_value_error))
                    }
                } else {
                    removeProgress()
                    showMessage(getString(R.string.enter_lift_value_error))
                }

            }
        }
        binding.btnWindowTilt.setOnClickListener {
            showMatterProgressDialog(getString(R.string.matter_door_lock_in_progress))
            scope.launch {
                if (!binding.etTilt.text.isNullOrEmpty()) {
                    var tiltPtage = binding.etTilt.text.toString().toIntOrNull()
                    if (tiltPtage != null && tiltPtage in 1..100) {
                        tiltPtage = 100 - tiltPtage
                        sendWindowTiltCommandClick(tiltPtage)
                    } else {
                        removeProgress()
                        showMessage(getString(R.string.enter_valid_tilt_value_error))
                    }
                } else {
                    removeProgress()
                    showMessage(getString(R.string.enter_tilt_value_error))
                }

            }
        }
        (activity as MatterDemoActivity).hideQRScanner()

        //back handling
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )
    }

    private suspend fun readTargetPositionLift() {
        getWindowClusterForDevice().readTargetPositionLiftPercent100thsAttribute(object :
            ChipClusters.WindowCoveringCluster.TargetPositionLiftPercent100thsAttributeCallback {
            override fun onSuccess(value: Int?) {
                removeProgress()
                //  binding.etLift.setText(value?.div(100).toString())
                when (value?.div(100)) {
                    0 -> {
                        binding.btnMatterWindowState.setImageResource(R.drawable.w0)
                        scope.launch {
                            sendWindowOpenCommandClick()
                        }
                    }

                    in 1..10 -> binding.btnMatterWindowState.setImageResource(R.drawable.w1)
                    in 11..20 -> binding.btnMatterWindowState.setImageResource(R.drawable.w2)
                    in 21..30 -> binding.btnMatterWindowState.setImageResource(R.drawable.w3)
                    in 31..40 -> binding.btnMatterWindowState.setImageResource(R.drawable.w4)
                    in 41..50 -> binding.btnMatterWindowState.setImageResource(R.drawable.w5)
                    in 51..60 -> binding.btnMatterWindowState.setImageResource(R.drawable.w6)
                    in 61..70 -> binding.btnMatterWindowState.setImageResource(R.drawable.w7)
                    in 71..80 -> binding.btnMatterWindowState.setImageResource(R.drawable.w8)
                    in 81..90 -> binding.btnMatterWindowState.setImageResource(R.drawable.w9)
                    in 91..100 -> binding.btnMatterWindowState.setImageResource(R.drawable.w10)
                    else -> null
                }
            }

            override fun onError(ex: java.lang.Exception?) {
                removeProgress()
                Timber.tag(TAG).e("Read LiftPercentage command failure: $ex")
            }
        })
    }

    private suspend fun readTargetPositionTilt() {
        getWindowClusterForDevice().readTargetPositionTiltPercent100thsAttribute(object :
            ChipClusters.WindowCoveringCluster.TargetPositionTiltPercent100thsAttributeCallback {
            override fun onSuccess(value: Int?) {
                removeProgress()
                //  binding.etTilt.setText(value?.div(100).toString())
                when (value?.div(100)) {
                    in 0..10 -> binding.btnMatterWindowState.alpha = 0.1f

                    in 11..20 -> binding.btnMatterWindowState.alpha = 0.15f

                    in 21..30 -> binding.btnMatterWindowState.alpha = 0.25f

                    in 31..40 -> binding.btnMatterWindowState.alpha = 0.35f

                    in 41..50 -> binding.btnMatterWindowState.alpha = 0.45f

                    in 51..60 -> binding.btnMatterWindowState.alpha = 0.55f

                    in 61..70 -> binding.btnMatterWindowState.alpha = 0.65f

                    in 71..80 -> binding.btnMatterWindowState.alpha = 0.75f

                    in 81..90 -> binding.btnMatterWindowState.alpha = 0.85f

                    in 91..100 -> binding.btnMatterWindowState.alpha = 0.95f
                }
            }

            override fun onError(ex: java.lang.Exception?) {
                removeProgress()
                Timber.tag(TAG).e("Read LiftPercentage command failure: $ex")
            }

        })
    }


    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {

            if (requireActivity().supportFragmentManager.backStackEntryCount > 0) {
                requireActivity().supportFragmentManager.popBackStack()
            } else {
                FragmentUtils.getHost(this@MatterWindowCoverFragment, CallBackHandler::class.java)
                    .onBackHandler()
            }


        }
    }

    private suspend fun sendWindowCloseCommandClick() {
        getWindowClusterForDevice().downOrClose(
            object : ChipClusters.DefaultClusterCallback {
                override fun onSuccess() {
                    Timber.tag(TAG).d("downOrClose command Success")
                    SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, true)
                    binding.btnMatterWindowState.alpha = 1.0f
                    binding.btnMatterWindowState.setImageResource(R.drawable.w10)
                    removeProgress()
                }

                override fun onError(error: Exception?) {

                    SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, false)
                    showMessageDialog(getString(R.string.matter_device_offline_text))
                    Timber.tag(TAG).e("downOrClose command failure: $error")
                    removeProgress()
                }

            }
        )
    }

    private suspend fun sendWindowOpenCommandClick() {
        getWindowClusterForDevice().upOrOpen(
            object : ChipClusters.DefaultClusterCallback {
                override fun onSuccess() {
                    SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, true)
                    Timber.tag(TAG).d("upOrOpen command Success")
                    removeProgress()
                    binding.btnMatterWindowState.setImageResource(R.drawable.w0)
                }

                override fun onError(error: Exception?) {
                    SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, false)
                    showMessageDialog(getString(R.string.matter_device_offline_text))
                    removeProgress()
                    Timber.tag(TAG).e("upOrOpen command failure: $error")
                }

            }, TIME_OUT
        )

    }

    private suspend fun sendWindowLiftCommandClick(liftPtage: Int) {

        getWindowClusterForDevice().goToLiftPercentage(
            object : ChipClusters.DefaultClusterCallback {
                override fun onSuccess() {
                    SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, true)
                    Timber.tag(TAG).d("LiftPercentage command Success")
                    scope.launch {
                        readTargetPositionLift()
                        removeProgress()
                    }

                }


                override fun onError(error: Exception?) {
                    SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, false)
                    showMessageDialog(getString(R.string.matter_device_offline_text))
                    removeProgress()
                    Timber.tag(TAG).e("LiftPercentage command failure: $error")
                }

            }, liftPtage * 100
        )

    }

    private suspend fun sendWindowTiltCommandClick(tiltPtage: Int) {
        getWindowClusterForDevice().goToTiltPercentage(
            object : ChipClusters.DefaultClusterCallback {
                override fun onSuccess() {
                    SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, true)
                    Timber.tag(TAG).d("TiltPercentage command Success")
                    scope.launch {
                        readTargetPositionTilt()
                    }
                }


                override fun onError(error: Exception?) {
                    SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, false)
                    showMessageDialog(getString(R.string.matter_device_offline_text))

                    Timber.tag(TAG).e("LiftPercentage command failure: $error")
                }

            }, tiltPtage * 100
        )
    }

    private suspend fun getWindowClusterForDevice(): ChipClusters.WindowCoveringCluster {
        return ChipClusters.WindowCoveringCluster(
            ChipClient.getConnectedDevicePointer(requireContext(), deviceId),
            endpointId
        )

    }

    private fun showMessage(msg: String) {
        requireActivity().runOnUiThread {
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    inner class WindowCoverChipControllerCallback : GenericChipDeviceListener() {
        override fun onConnectDeviceComplete() {}

        override fun onCommissioningComplete(nodeId: Long, errorCode: Long) {
            Timber.tag(TAG).d("onCommissioningComplete for nodeId $nodeId: $errorCode")
            //showMessage("Address update complete for nodeId $nodeId with code $errorCode")
        }

        override fun onNotifyChipConnectionClosed() {
            Timber.tag(TAG).d("onNotifyChipConnectionClosed")
        }

        override fun onCloseBleComplete() {
            Timber.tag(TAG).d("onCloseBleComplete")
        }

        override fun onError(error: Throwable?) {
            super.onError(error)
            Timber.tag(TAG).d("onError: $error")
        }

    }


    interface CallBackHandler {
        fun onBackHandler()
    }

    companion object {
        private const val TAG = "MatterWindowCoverFragment"
        fun newInstance(): MatterWindowCoverFragment = MatterWindowCoverFragment()
    }
}