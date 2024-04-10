package com.siliconlabs.bledemo.features.demo.matter_demo.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.camera.core.impl.utils.ContextUtil.getApplicationContext
import chip.devicecontroller.ChipDeviceController
import chip.devicecontroller.ControllerParams
import chip.devicecontroller.GetConnectedDeviceCallbackJni
import chip.platform.AndroidBleManager
import chip.platform.AndroidChipPlatform
import chip.platform.ChipMdnsCallbackImpl
import chip.platform.DiagnosticDataProviderImpl
import chip.platform.NsdManagerServiceBrowser
import chip.platform.NsdManagerServiceResolver
import chip.platform.PreferencesConfigurationManager
import chip.platform.PreferencesKeyValueStoreManager
import com.siliconlabs.bledemo.features.demo.matter_demo.attestation.ExampleAttestationTrustStoreDelegate
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


object ChipClient {
    private val TAG = ChipClient::class.java.classes.toString()
    private lateinit var chipDeviceController: ChipDeviceController
    private lateinit var androidPlatform: AndroidChipPlatform
    private const val VENDOR_ID = 0xFFF4

    fun getDeviceController(context: Context): ChipDeviceController {
        getAndroidChipPlatform(context)
        if (!this::chipDeviceController.isInitialized) {
            chipDeviceController = ChipDeviceController(
                ControllerParams.newBuilder()
                    .setControllerVendorId(VENDOR_ID)
                    .build()
            )
            chipDeviceController.setAttestationTrustStoreDelegate(
                ExampleAttestationTrustStoreDelegate(chipDeviceController)
            )
        }
        return chipDeviceController
    }

    fun getAndroidChipPlatform(context: Context?): AndroidChipPlatform {
        if (!this::androidPlatform.isInitialized && context != null) {
            ChipDeviceController.loadJni()
            androidPlatform = AndroidChipPlatform(
                AndroidBleManager(),
                PreferencesKeyValueStoreManager(context),
                PreferencesConfigurationManager(context),
                NsdManagerServiceResolver(context),
                NsdManagerServiceBrowser(context),
                ChipMdnsCallbackImpl(), DiagnosticDataProviderImpl(context)
            )
        }
        return androidPlatform
    }

    private fun showMessage(msg: String, context: Context) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    suspend fun getConnectedDevicePointer(context: Context, nodeId: Long): Long {
        // TODO (#21539) This is a memory leak because we currently never call releaseConnectedDevicePointer
        // once we are done with the returned device pointer. Memory leak was introduced since the refactor
        // that introduced it was very large in order to fix a use after free, which was considered to be
        // worse than the memory leak that was introduced.
        return suspendCoroutine { continuation ->
            getDeviceController(context).getConnectedDevicePointer(
                nodeId,
                object : GetConnectedDeviceCallbackJni.GetConnectedDeviceCallback {
                    override fun onDeviceConnected(devicePointer: Long) {
                        Log.d(TAG, "Got connected device pointer")
                        continuation.resume(devicePointer)
                    }

                    override fun onConnectionFailure(nodeId: Long, error: Exception) {
                        //  showMessage("Device is offline",context)
                        val errorMessage = "Unable to get connected device with nodeId $nodeId"
                        Log.e(TAG, errorMessage, error)
                        continuation.resumeWithException(IllegalStateException(errorMessage))
                    }
                })
        }
    }
}