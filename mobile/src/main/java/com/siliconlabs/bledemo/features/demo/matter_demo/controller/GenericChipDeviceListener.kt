package com.siliconlabs.bledemo.features.demo.matter_demo.controller

import chip.devicecontroller.ChipDeviceController

open class GenericChipDeviceListener: ChipDeviceController.CompletionListener {
    override fun onConnectDeviceComplete() {
        // No op
    }

    override fun onStatusUpdate(status: Int) {
        // No op
    }

    override fun onPairingComplete(errorCode: Int) {
        // No op
    }

    override fun onPairingDeleted(errorCode: Int) {
        // No op
    }

    override fun onCommissioningComplete(nodeId: Long, errorCode: Int) {

    }

    override fun onReadCommissioningInfo(
        vendorId: Int,
        productId: Int,
        wifiEndpointId: Int,
        threadEndpointId: Int
    ) {
        // No op
    }

    override fun onCommissioningStatusUpdate(nodeId: Long, stage: String?, errorCode: Int) {
        // No op
    }

    override fun onNotifyChipConnectionClosed() {
        // No op
    }

    override fun onCloseBleComplete() {
        // No op
    }

    override fun onError(error: Throwable) {
        // No op
    }

    override fun onOpCSRGenerationComplete(csr: ByteArray) {
        // No op
    }
}