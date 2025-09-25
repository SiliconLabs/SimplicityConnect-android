package com.siliconlabs.bledemo.features.demo.matter_demo.activities

import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import chip.devicecontroller.ChipDeviceController
import chip.devicecontroller.GetConnectedDeviceCallbackJni
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.ActivityMatterDemoBinding
import com.siliconlabs.bledemo.features.demo.matter_demo.dishwasher_demo.view.MatterDishwasherFragment
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterDoorFragment
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterLightFragment
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterOccupancySensorFragment
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannerFragment
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterThermostatFragment
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterWifiInputDialogFragment
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterWindowCoverFragment
import com.siliconlabs.bledemo.features.demo.matter_demo.model.MatterScannedResultModel
import com.siliconlabs.bledemo.features.demo.matter_demo.model.ProvisionNetworkType
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.ChipClient
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.CustomProgressDialog
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.SharedPrefsUtils
import com.siliconlabs.bledemo.utils.AppUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@AndroidEntryPoint
class MatterDemoActivity : AppCompatActivity(),
    MatterScannerFragment.CallBack,
    MatterScannedResultFragment.Callback,
    MatterThermostatFragment.CallBackHandler,
    MatterLightFragment.CallBackHandler,
    MatterWifiInputDialogFragment.CallBackHandler,
    MatterDoorFragment.CallBackHandler,
    MatterWindowCoverFragment.CallBackHandler,
    MatterOccupancySensorFragment.CallBackHandler {


    private var currentFragment: Fragment? = null
    private lateinit var binding: ActivityMatterDemoBinding
    private var scannedDeviceList = ArrayList<MatterScannedResultModel>()
    private lateinit var mPrefs: SharedPreferences
    private lateinit var deviceController: ChipDeviceController
    private var customProgressDialog: CustomProgressDialog? = null
    private var networkType: ProvisionNetworkType? = null
    private var shouldContinueOperation = true
    private var count: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.tag(TAG).e(" onCreate")
        binding = ActivityMatterDemoBinding.inflate(layoutInflater)
        mPrefs = this.getSharedPreferences("your_preference_name", MODE_PRIVATE)
        setContentView(binding.root)
        AppUtil.setEdgeToEdge(window,this)
        setSupportActionBar(binding.toolbar)
        deviceController = ChipClient.getDeviceController(this)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.matter_back)
            actionBar.setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.matter_demo_title)
        }
        binding.scanQRCode.setOnClickListener {
            val fragment = MatterScannerFragment.newInstance()
            showFragment(
                fragment, fragment::class.java.simpleName
            )
        }
        binding.refresh.setOnClickListener {
            count = 0
            if (SharedPrefsUtils.retrieveSavedDevices(mPrefs).size > 0) {
                showMatterProgressDialog(getString(R.string.matter_device_status))
                CoroutineScope(Dispatchers.Default).launch {
                    val resultInfo = performLongRunningOperation()
                    if (resultInfo) {
                        println("Operation was successful")
                        removeProgress()
                        scannedDeviceList = SharedPrefsUtils.retrieveSavedDevices(mPrefs)
                        prepareList(scannedDeviceList)

                    }
                }
            }
        }
        startPeriodictFunction()
        sendToScanResultFragment()
    }

    private fun startPeriodictFunction() {
        CoroutineScope(Dispatchers.Default).launch {
            while (shouldContinueOperation) {
                performLongRunningOperation() // Call your long-running operation
                delay(DELAY_TIMEOUT)
            }
        }
    }

    private fun stopPeriodicOperation() {
        shouldContinueOperation = false
    }

    private fun prepareList(scannedDeviceListPrep: ArrayList<MatterScannedResultModel>) {
        val bundle = Bundle()
        bundle.putParcelableArrayList(ARG_DEVICE_LIST, scannedDeviceListPrep)
        val fragment = MatterScannedResultFragment.newInstance()
        fragment.arguments = bundle
        supportFragmentManager.beginTransaction().replace(
            binding.matterContainer.id,
            fragment,
            fragment.javaClass.simpleName
        ).addToBackStack(null).commit()
    }

    private fun removeProgress() {
        if (customProgressDialog?.isShowing == true) {
            customProgressDialog?.dismiss()
        }
    }

    private fun showMatterProgressDialog(message: String) {
        customProgressDialog = CustomProgressDialog(this)
        customProgressDialog!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        customProgressDialog!!.setMessage(message)
        customProgressDialog!!.setCanceledOnTouchOutside(false)
        customProgressDialog!!.show()

    }

    override fun onResume() {
        super.onResume()
        Timber.tag(TAG).e(" onResume")
        ChipClient.startDnssd(this)
        count = 0
    }

    override fun onPause() {
        super.onPause()
        Timber.tag(TAG).e(" onPause")
        ChipClient.stopDnssd(this)
    }

    override fun onStart() {
        super.onStart()
        Timber.tag(TAG).e(" onStart")

    }


    private fun performLongRunningOperation(): Boolean {
        CoroutineScope(Dispatchers.IO).launch {
            val savedDevices = SharedPrefsUtils.retrieveSavedDevices(mPrefs)
            for (item in savedDevices) {
                getStatus(item.deviceId)
            }
        }
        return false
    }

    private suspend fun getStatus(deviceId: Long) = withContext(Dispatchers.Default) {
        val savedDevices = SharedPrefsUtils.retrieveSavedDevices(mPrefs)
        val savedDeviceCount = savedDevices.size

        println("Matter Total device found $savedDeviceCount")
        try {
            //  suspendCoroutine<Unit> { continuation ->
            suspendCoroutine { continuation ->

                deviceController.getConnectedDevicePointer(deviceId,
                    object : GetConnectedDeviceCallbackJni.GetConnectedDeviceCallback {
                        override fun onDeviceConnected(devicePointer: Long) {
                            Timber.tag(TAG).e("devicePointer $devicePointer")
                            SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, true)
                            if (currentFragment != null) {
                                when (currentFragment) {
                                    is MatterScannedResultFragment -> {
                                        prepareList(SharedPrefsUtils.retrieveSavedDevices(mPrefs))
                                    }
                                }
                            }
                            count++
                            println("Matter Success count$count")
                            if (count >= savedDeviceCount) {
                                removeProgress()
                            }
                            // continuation.resume(Unit)
                        }

                        override fun onConnectionFailure(
                            nodeId: Long,
                            error: java.lang.Exception?
                        ) {
                            Timber.tag(TAG).e("nodeId $nodeId  Error $error")
                            SharedPrefsUtils.updateDeviceByDeviceId(mPrefs, deviceId, false)
                            currentFragment =
                                supportFragmentManager.findFragmentById(R.id.matter_container)
                            if (currentFragment != null) {
                                when (currentFragment) {
                                    is MatterScannedResultFragment -> {
                                        prepareList(SharedPrefsUtils.retrieveSavedDevices(mPrefs))
                                    }
                                }

                            }

                            continuation.resumeWithException(IllegalStateException(error))
                            count++
                            println("Matter disconnect count$count")
                            if (count >= savedDeviceCount) {
                                removeProgress()
                            }
                        }

                    })
            }
        } catch (ex: Exception) {
            Timber.tag(TAG).e("Exception $ex")
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        stopPeriodicOperation()
    }

    fun hideQRScanner() {
        binding.scanQRCode.visibility = View.GONE
        binding.refresh.visibility = View.GONE
    }

    fun showQRScanner() {
        binding.scanQRCode.visibility = View.VISIBLE
        binding.refresh.visibility = View.VISIBLE
    }

    private fun showFragment(
        fragment: Fragment, tag: String? = null,
    ) {
        val fManager = supportFragmentManager
        val fTransaction = fManager.beginTransaction()

        fTransaction.replace(binding.matterContainer.id, fragment, tag)
            .addToBackStack(null)
            .commit()

    }


    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(MatterScannerFragment.ARG_PROVISION_NETWORK_TYPE, networkType?.name)

        super.onSaveInstanceState(outState)
    }


    override fun onCommissionCompleteLoadData(matterScannedResultModel: MatterScannedResultModel) {
        Timber.tag(TAG).e("on commission complete add $matterScannedResultModel ")
        scannedDeviceList.add(matterScannedResultModel)
        SharedPrefsUtils.saveDevicesToPref(mPrefs, scannedDeviceList)
        sendToScanResultFragment()

    }

    private fun sendToScanResultFragment() {
        val bundle = Bundle()
        scannedDeviceList = SharedPrefsUtils.retrieveSavedDevices(mPrefs)
        bundle.putParcelableArrayList(ARG_DEVICE_LIST, scannedDeviceList)
        val fragment = MatterScannedResultFragment.newInstance()
        fragment.arguments = bundle
        supportFragmentManager.beginTransaction().replace(
            binding.matterContainer.id,
            fragment,
            fragment.javaClass.simpleName
        ).commit()
    }

    override fun navigateToDemo(
        fragment: Fragment,
        model: MatterScannedResultModel
    ) {
        val bundle = Bundle()
        bundle.putParcelable(ARG_DEVICE_MODEL, model)
        fragment.arguments = bundle

        supportFragmentManager.beginTransaction().replace(
            binding.matterContainer.id,
            fragment,
            fragment.javaClass.simpleName
        ).addToBackStack(null).commit()
    }


    private fun backHandler() {
        val bundle = Bundle()
        scannedDeviceList = SharedPrefsUtils.retrieveSavedDevices(mPrefs)
        bundle.putParcelableArrayList(ARG_DEVICE_LIST, scannedDeviceList)

        val fragment = MatterScannedResultFragment.newInstance()
        fragment.arguments = bundle
        supportFragmentManager.beginTransaction().replace(
            binding.matterContainer.id,
            fragment,
            fragment.javaClass.simpleName
        ).addToBackStack(null).commit()
    }


    override fun onBackHandler() {
        backHandler()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {
            android.R.id.home -> {
                currentFragment = supportFragmentManager.findFragmentById(R.id.matter_container)
                if (currentFragment != null && currentFragment is MatterScannedResultFragment) {

                    this.finish()

                }else if (currentFragment != null && currentFragment is MatterDishwasherFragment) {

                    val fragment = supportFragmentManager.findFragmentById(R.id.matter_container) as? MatterDishwasherFragment

                    fragment?.let {
                        // You can access the DataBinding object here
                        // Example: Change the text of the TextView inside the fragment when back is pressed
                        it.handleDishwasherBackNavigationUI()
                    }

                }
                else if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    this.finish()
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val TAG = "MatterDemoActivity"
        private const val ARG_DEVICE_LIST = "device_list"
        private const val ARG_DEVICE_MODEL = "device_model"
        private const val DELAY_TIMEOUT = 8000L
        const val MATTER_PREF = "your_preference_name"
    }
}
