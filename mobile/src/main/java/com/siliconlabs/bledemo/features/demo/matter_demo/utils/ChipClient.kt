package com.siliconlabs.bledemo.features.demo.matter_demo.utils

import android.content.Context
import chip.devicecontroller.ChipDeviceController
import chip.devicecontroller.ControllerParams
import chip.devicecontroller.GetConnectedDeviceCallbackJni.GetConnectedDeviceCallback
import chip.devicecontroller.ICDCheckInDelegate
import chip.devicecontroller.ICDClientInfo
import chip.platform.AndroidBleManager
import chip.platform.AndroidChipPlatform
import chip.platform.ChipMdnsCallbackImpl
import chip.platform.DiagnosticDataProviderImpl
import chip.platform.NsdManagerServiceBrowser
import chip.platform.NsdManagerServiceResolver
import chip.platform.PreferencesConfigurationManager
import chip.platform.PreferencesKeyValueStoreManager
import com.siliconlabs.bledemo.features.demo.matter_demo.attestation.ExampleAttestationTrustStoreDelegate
import com.siliconlabs.bledemo.utils.CustomToastManager
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


object ChipClient {
    private val TAG = ChipClient::class.java.classes.toString()
    private lateinit var chipDeviceController: ChipDeviceController
    private lateinit var androidPlatform: AndroidChipPlatform
    private const val VENDOR_ID = 0xFFF4
    private var icdCheckInCallback: ICDCheckInCallback? = null

    fun getDeviceController(context: Context): ChipDeviceController {
        getAndroidChipPlatform(context)

        if (!this::chipDeviceController.isInitialized) {
            chipDeviceController = ChipDeviceController(
                ControllerParams.newBuilder()
                    .setControllerVendorId(VENDOR_ID)
                    .setEnableServerInteractions(true)
                    .build()
            )
            chipDeviceController.setAttestationTrustStoreDelegate(
                ExampleAttestationTrustStoreDelegate(chipDeviceController)
            )
            chipDeviceController.setICDCheckInDelegate(object : ICDCheckInDelegate {
                override fun onCheckInComplete(info: ICDClientInfo) {
                    Timber.tag(TAG).d("onCheckInComplete : $info")
                    icdCheckInCallback?.notifyCheckInMessage(info)
                }

                override fun onKeyRefreshNeeded(info: ICDClientInfo): ByteArray? {
                    Timber.tag(TAG).d("onKeyRefreshNeeded : $info")
                    return null
                }

                override fun onKeyRefreshDone(errorCode: Long) {
                    Timber.tag(TAG).d("onKeyRefreshDone : $errorCode")
                }

            })
        }
        return chipDeviceController
    }

    fun setICDCheckInCallback(callback: ICDCheckInCallback) {
        icdCheckInCallback = callback
    }


    fun startDnssd(context: Context) {
        if (!this::chipDeviceController.isInitialized) {
            getDeviceController(context)
        } else {
            chipDeviceController.startDnssd()
        }
    }

    fun stopDnssd(context: Context) {
        if (!this::chipDeviceController.isInitialized) {
            getDeviceController(context)
        }
        chipDeviceController.stopDnssd()
    }

    fun getAndroidChipPlatform(context: Context?): AndroidChipPlatform {
        if (!this::androidPlatform.isInitialized && context != null) {
            ChipDeviceController.loadJni()
            androidPlatform = AndroidChipPlatform(
                AndroidBleManager(),
                PreferencesKeyValueStoreManager(context),
                PreferencesConfigurationManager(context),
                NsdManagerServiceResolver(
                    context,
                    NsdManagerServiceResolver.NsdManagerResolverAvailState()
                ),
                NsdManagerServiceBrowser(context),
                ChipMdnsCallbackImpl(), DiagnosticDataProviderImpl(context)
            )
        }
        return androidPlatform
    }

    private fun showMessage(msg: String, context: Context) {
        //  Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        CustomToastManager.show(
            context, msg, 5000
        )
    }

    /* suspend fun getConnectedDevicePointer(context: Context, nodeId: Long): Long {
         // TODO (#21539) This is a memory leak because we currently never call releaseConnectedDevicePointer
         // once we are done with the returned device pointer. Memory leak was introduced since the refactor
         // that introduced it was very large in order to fix a use after free, which was considered to be
         // worse than the memory leak that was introduced.

         return suspendCancellableCoroutine { continuation ->
             getDeviceController(context).getConnectedDevicePointer(
                 nodeId,
                 object : GetConnectedDeviceCallbackJni.GetConnectedDeviceCallback {
                     override fun onDeviceConnected(devicePointer: Long) {
                         Timber.tag(TAG).d("Got connected device pointer")
                         continuation.resume(devicePointer)
                     }

                     override fun onConnectionFailure(nodeId: Long, error: Exception) {
                         val errorMessage = "Unable to get connected device with nodeId $nodeId"
                         Timber.tag(TAG).e(error, errorMessage)
                         continuation.resumeWithException(IllegalStateException(errorMessage))
                     }
                 }
             )

         }
     }*/

    suspend fun getConnectedDevicePointer(context: Context, nodeId: Long): Long {
        return suspendCancellableCoroutine { continuation ->
            try {
                getDeviceController(context).getConnectedDevicePointer(
                    nodeId,
                    object : GetConnectedDeviceCallback {
                        override fun onDeviceConnected(devicePointer: Long) {
                            Timber.tag(TAG).d("Got connected device pointer")
                            continuation.resume(devicePointer)
                        }

                        override fun onConnectionFailure(nodeId: Long, error: Exception) {
                            val errorMessage = "Unable to get connected device with nodeId $nodeId"
                            Timber.tag(TAG).e(error, errorMessage)
                            if (continuation.isActive) {
                                continuation.resumeWithException(IllegalStateException(errorMessage))
                            }
                            // continuation.resumeWithException(IllegalStateException(errorMessage))
                        }
                    }
                )
            } catch (e: Exception) {
                val errorMessage = "Failed to initiate connection for nodeId $nodeId"
                Timber.tag(TAG).e(e, errorMessage)
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }
}