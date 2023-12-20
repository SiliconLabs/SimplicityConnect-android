package com.siliconlabs.bledemo.features.demo.matter_demo.model

import android.os.Parcel
import android.os.Parcelable
import chip.setuppayload.DiscoveryCapability
import chip.setuppayload.SetupPayload
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CHIPDeviceInfo(
    val version: Int = 0,
    val vendorId: Int = 0,
    val productId: Int = 0,
    val discriminator: Int = 0,
    val setupPinCode: Long = 0L,
    var commissioningFlow: Int = 0,
    val optionalQrCodeInfoMap: Map<Int, QrCodeInfo> = mapOf(),
    val discoveryCapabilities: Set<DiscoveryCapability> = setOf(),
    val isShortDiscriminator: Boolean = false,
    val ipAddress: String? = null,
    var networkType: String? = null,

    ) : Parcelable {
    companion object {
        fun fromSetupPayload(
            setupPayload: SetupPayload,
            isShortDiscriminator: Boolean = false
        ): CHIPDeviceInfo {
            return CHIPDeviceInfo(
                setupPayload.version,
                setupPayload.vendorId,
                setupPayload.productId,
                setupPayload.discriminator,
                setupPayload.setupPinCode,
                setupPayload.commissioningFlow,
                setupPayload.optionalQRCodeInfo.mapValues { (_, info) ->
                    QrCodeInfo(
                        info.tag,
                        info.type,
                        info.data,
                        info.int32
                    )
                },
                setupPayload.discoveryCapabilities,
                isShortDiscriminator
            )
        }
    }


}
