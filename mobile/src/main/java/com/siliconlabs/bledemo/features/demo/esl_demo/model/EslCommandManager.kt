package com.siliconlabs.bledemo.features.demo.esl_demo.model

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.siliconlabs.bledemo.bluetooth.ble.GattCharacteristic
import com.siliconlabs.bledemo.bluetooth.ble.GattService
import com.siliconlabs.bledemo.utils.GattQueue

class EslCommandManager(
    private val gatt: BluetoothGatt?,
    private val startTimeoutCount: () -> Unit,
) {

    private val gattQueue = GattQueue(gatt)
    private val eslControlCharacteristic = getEslControlPointCharacteristic()
    private val eslTransferImageCharacteristic = getEslTransferImageCharacteristic()

    fun subscribeToCharacteristic() {
        gattQueue.queueNotify(eslControlCharacteristic)
        gattQueue.queueNotify(eslTransferImageCharacteristic)
    }

    fun handleGattCommandProcessed() {
        gattQueue.handleCommandProcessed()
    }

    fun connectTagByQr(qrCodeData: QrCodeData) {
        sendWriteCommand(qrCodeData.fullCommand)
    }

    fun connectTagById(eslId: Int) {
        val message = listOf(EslCommand.CONNECT.message, eslId).joinToString(SPACE_SEPARATOR)
        sendWriteCommand(message)
    }

    fun configureTag() {
        val message = buildString {
            append(EslCommand.CONFIGURE.message).append(SPACE_SEPARATOR)
            append(CONFIG_COMMAND_PARAMETER)
        }
        sendWriteCommand(message)
    }

    fun terminateLoading() {
        val message = buildString {
            //append(EslCommand.CONFIGURE)
            append("0x01")
        }
        sendWriteCommand(message)
    }

    fun loadTagsInfo() {
        val message = EslCommand.LOAD_INFO.message
        sendWriteCommand(message)
    }

    fun pingTag(eslId: Int) {
        val message = listOf(
            EslCommand.PING.message,
            eslId,
        ).joinToString(SPACE_SEPARATOR)
        sendWriteCommand(message)
    }

    fun disconnectTag() {
        val message = listOf(
            EslCommand.DISCONNECT.message,
        ).joinToString(SPACE_SEPARATOR)
        sendWriteCommand(message)
    }

    fun removeTag(eslId: Int) {
        val message = listOf(
            EslCommand.REMOVE.message,
            eslId,
        ).joinToString(SPACE_SEPARATOR)
        sendWriteCommand(message)
    }

    fun toggleLed(shouldSubmitStateOn: Boolean, eslId: Int) {
        val message = buildString {
            append(EslCommand.TOGGLE_LED.message).append(SPACE_SEPARATOR)
            append(if (shouldSubmitStateOn) LED_ON_COMMAND_PARAMETER else LED_OFF_COMMAND_PARAMETER).append(SPACE_SEPARATOR)
            append(eslId)
        }
        sendWriteCommand(message)
    }

    fun toggleAllLeds(shouldSubmitStateOn: Boolean) {
        val message = buildString {
            append(EslCommand.TOGGLE_LED.message).append(SPACE_SEPARATOR)
            append(if (shouldSubmitStateOn) LED_ON_COMMAND_PARAMETER else LED_OFF_COMMAND_PARAMETER).append(SPACE_SEPARATOR)
            append(ALL_COMMAND_PARAMETER)
        }
        sendWriteCommand(message)
    }

    fun updateTagLedImage(imageIndex: Int, imageFilepath: String) {
        val message = buildString {
            append(EslCommand.UPDATE_IMAGE.message).append(SPACE_SEPARATOR)
            append(imageIndex).append(SPACE_SEPARATOR)
            append(imageFilepath)
        }
        sendWriteCommand(message)
    }

    fun displayTagLedImage(eslId: Int, imageIndex: Int, displayIndex: Int) {
        val message = listOf(
            EslCommand.DISPLAY_IMAGE.message,
            eslId,
            imageIndex,
            displayIndex,
        ).joinToString(SPACE_SEPARATOR)
        sendWriteCommand(message)
    }

    fun displayAllTagsImage(imageIndex: Int, displayIndex: Int) {
        val message = listOf(
                EslCommand.DISPLAY_IMAGE.message,
                ALL_COMMAND_PARAMETER,
                imageIndex,
                displayIndex,
            ).joinToString(SPACE_SEPARATOR)
        sendWriteCommand(message)
    }


    private fun sendWriteCommand(stringCommand: String) {
        startTimeoutCount()
        eslControlCharacteristic
                ?.apply { this.value = stringCommand.toByteArray() }
                ?.also { gattQueue.queueWrite(it) }
    }


    fun sendImageWrite(data: ByteArray, lastChunk: Boolean = false) {
        val isLastChunkFlag = (if(lastChunk) 1 else 0).toByte()
        startTimeoutCount()
        eslTransferImageCharacteristic
                ?.apply { this.value = byteArrayOf(isLastChunkFlag) + data }
                ?.also { gattQueue.queueWrite(it) }
    }


    private fun getEslControlPointCharacteristic() : BluetoothGattCharacteristic? {
        return gatt?.getService(GattService.EslDemoService.number)
                ?.getCharacteristic(GattCharacteristic.EslControlPoint.uuid)
    }

    private fun getEslTransferImageCharacteristic() : BluetoothGattCharacteristic? {
        return gatt?.getService(GattService.EslDemoService.number)
                ?.getCharacteristic(GattCharacteristic.EslTransferImage.uuid)
    }

    companion object {
        private const val SPACE_SEPARATOR = " "
        private const val CONFIG_COMMAND_PARAMETER = "full"
        private const val ALL_COMMAND_PARAMETER = "all"
        private const val LED_ON_COMMAND_PARAMETER = "on"
        private const val LED_OFF_COMMAND_PARAMETER = "off"
    }
}