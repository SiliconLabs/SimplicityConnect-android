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
import chip.devicecontroller.ChipClusters.IntegerAttributeCallback
import chip.devicecontroller.ChipDeviceController
import chip.devicecontroller.GetConnectedDeviceCallbackJni
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.bluetooth.beacon_utils.eddystone.Constants
import com.siliconlabs.bledemo.databinding.FragmentMatterWindowCoverBinding
import com.siliconlabs.bledemo.features.demo.matter_demo.activities.MatterDemoActivity
import com.siliconlabs.bledemo.features.demo.matter_demo.controller.GenericChipDeviceListener
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterLightFragment.Companion.ARG_DEVICE_MODEL
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
            "your_preference_name",
            AppCompatActivity.MODE_PRIVATE
        )
        if (requireArguments() != null) {
            model = requireArguments().getParcelable(ARG_DEVICE_MODEL)!!
            deviceId = model.deviceId

        }

        if (deviceId != null) {

            showMatterProgressDialog(getString(R.string.matter_device_status))

            // retrieveSavedDevices()
            GlobalScope.launch {
                // This code will run asynchronously

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

            deviceController.getConnectedDevicePointer(deviceId, object :
                GetConnectedDeviceCallbackJni.GetConnectedDeviceCallback {
                override fun onDeviceConnected(devicePointer: Long) {
                    Log.e(TAG, "devicePointer " + devicePointer + " nodeid " + deviceId)
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

            //handleSubscriptionForWindowCover()
            // Return the result (true or false based on the actual result)
            true
        }
    }

    private suspend fun handleSubscriptionForWindowCover() {
        getWindowClusterForDevice().subscribeModeAttribute(
            object : IntegerAttributeCallback {
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
        if (isAdded() && requireActivity() != null && !requireActivity().isFinishing) {
            requireActivity().runOnUiThread {
                if (!MessageDialogFragment.isDialogShowing()) {
                    dialog = MessageDialogFragment()
                    dialog.setMessage(msg)
                    dialog.setOnDismissListener {
                        removeProgress()

                        if (requireActivity().supportFragmentManager.getBackStackEntryCount() > 0) {
                            requireActivity().supportFragmentManager.popBackStack();
                        } else {
                            FragmentUtils.getHost(
                                this@MatterWindowCoverFragment,
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
        customProgressDialog!!.show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentMatterWindowCoverBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scope = viewLifecycleOwner.lifecycleScope
        deviceController.setCompletionListener(WindowCoverChipControllerCallback())
        binding.btnMatterWindowState.setImageResource(R.drawable.window_close)
        binding.btnWindowClose.isLongClickable = false;
        binding.btnWindowOpen.isLongClickable = false;
        binding.btnWindowClose.text = requireContext().getText(R.string.matter_window_close)
        binding.btnWindowOpen.text = requireContext().getText(R.string.matter_window_open)


        binding.btnWindowOpen.setOnClickListener {
            scope.launch {
                sendWindowOpenCommandClick()
            }
        }

        binding.btnWindowClose.setOnClickListener {
            scope.launch {
                sendWindowCloseCommandClick()
            }
        }
        (activity as MatterDemoActivity).hideQRScanner()

        //back handling
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }


    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {

            if (requireActivity().supportFragmentManager.getBackStackEntryCount() > 0) {
                requireActivity().supportFragmentManager.popBackStack();
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
                     Timber.tag(TAG).d( "downOrClose command Success")
                    SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, true)
                    binding.btnMatterWindowState.setImageResource(R.drawable.window_close)
                }

                override fun onError(error: Exception?) {

                    SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, false)
                    showMessageDialog(getString(R.string.matter_device_offline_text))

                    Log.e(TAG, "downOrClose command failure", error)
                }

            }
        )
    }

    private suspend fun sendWindowOpenCommandClick() {
        getWindowClusterForDevice().upOrOpen(
            object : ChipClusters.DefaultClusterCallback {
                override fun onSuccess() {
                    SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, true)
                     Timber.tag(TAG).d( "upOrOpen command Success")
                    binding.btnMatterWindowState.setImageResource(R.drawable.window_open)
                }

                override fun onError(error: Exception?) {
                    SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, false)
                    showMessageDialog(getString(R.string.matter_device_offline_text))
                    // showMessage("Matter device is offline")
                    //  showMessage("upOrOpen command failure $error")
                    Log.e(TAG, "upOrOpen command failure", error)
                }

            }
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
        private const val TAG = "MatterWindowCoverFragment"
        fun newInstance(): MatterWindowCoverFragment = MatterWindowCoverFragment()
    }
}