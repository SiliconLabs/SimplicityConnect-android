package com.siliconlabs.bledemo.features.demo.matter_demo.model

import android.os.Parcelable
import android.util.Log
import kotlinx.android.parcel.Parcelize
import matter.onboardingpayload.OnboardingPayload
import matter.onboardingpayload.OnboardingPayloadException
import matter.onboardingpayload.OptionalQRCodeInfoType
import timber.log.Timber

@Parcelize
data class CHIPDeviceInfo(
    val version: Int = 0,
    val vendorId: Int = 0,
    val productId: Int = 0,
    val discriminator: Int = 0,
    val setupPinCode: Long = 0L,
    var commissioningFlow: Int = 0,
    val optionalQrCodeInfoMap: Map<Int, QrCodeInfo> = mapOf(),
    val discoveryCapabilities: MutableSet<matter.onboardingpayload.DiscoveryCapability> = mutableSetOf(),
    val isShortDiscriminator: Boolean = false,
    val serialNumber: String = "",
    val ipAddress: String? = null,
    var networkType: String? = null,
    val port: Int = 5540
) : Parcelable {

    fun toSetupPayload(): OnboardingPayload {
        val onboardingPayload =
            OnboardingPayload(
                version,
                vendorId,
                productId,
                commissioningFlow,
                discoveryCapabilities,
                discriminator,
                isShortDiscriminator,
                setupPinCode
            )
        if (serialNumber.isNotEmpty()) {
            onboardingPayload.addSerialNumber(serialNumber)
        }
        optionalQrCodeInfoMap.forEach { (_, info) ->
            if (info.type == OptionalQRCodeInfoType.TYPE_STRING && info.data != null) {
                onboardingPayload.addOptionalVendorData(info.tag, info.data)
            } else {
                onboardingPayload.addOptionalVendorData(info.tag, info.intDataValue)
            }
        }
        return onboardingPayload
    }

    companion object {
        private const val TAG = "CHIPDeviceInfo"

        fun fromSetupPayload(
            setupPayload: OnboardingPayload,
            isShortDiscriminator: Boolean = false
        ): CHIPDeviceInfo {
            val serialNumber =
                try {
                    setupPayload.getSerialNumber()
                } catch (e: OnboardingPayloadException) {
                    Timber.tag(TAG).e(e, "serialNumber Exception: ${e.message}")
                    ""
                }
            return CHIPDeviceInfo(
                setupPayload.version,
                setupPayload.vendorId,
                setupPayload.productId,
                setupPayload.getLongDiscriminatorValue(),
                setupPayload.setupPinCode,
                setupPayload.commissioningFlow,
                setupPayload.getAllOptionalVendorData().associate { info ->
                    info.tag to QrCodeInfo(info.tag, info.type, info.data, info.int32)
                },
                setupPayload.discoveryCapabilities,
                setupPayload.hasShortDiscriminator,
                serialNumber
            )
        }
    }
}