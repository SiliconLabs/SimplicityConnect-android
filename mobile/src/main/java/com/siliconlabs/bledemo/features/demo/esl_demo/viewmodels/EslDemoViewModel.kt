package com.siliconlabs.bledemo.features.demo.esl_demo.viewmodels

import android.bluetooth.BluetoothGattCharacteristic
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siliconlabs.bledemo.bluetooth.ble.GattCharacteristic
import com.siliconlabs.bledemo.features.demo.esl_demo.model.EslCommand
import com.siliconlabs.bledemo.features.demo.esl_demo.model.EslCommandManager
import com.siliconlabs.bledemo.features.demo.esl_demo.model.ImageUploadData
import com.siliconlabs.bledemo.features.demo.esl_demo.model.PingInfo
import com.siliconlabs.bledemo.features.demo.esl_demo.model.PingInfo.Companion.CORRECT_TLV_RESP_BASIC_STATE
import com.siliconlabs.bledemo.features.demo.esl_demo.model.QrCodeData
import com.siliconlabs.bledemo.features.demo.esl_demo.model.TagInfo
import com.siliconlabs.bledemo.features.demo.esl_demo.model.TagListItem
import com.siliconlabs.bledemo.utils.SingleLiveEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.time.Duration.Companion.seconds

class EslDemoViewModel : ViewModel() {

    private val tagsInfo: MutableList<TagViewInfo> = mutableListOf()

    private val _viewState: MutableLiveData<ViewState> = MutableLiveData()
    val viewState: LiveData<ViewState> = _viewState
    private val _actionState: SingleLiveEvent<ActionState> = SingleLiveEvent()
    val actionState: SingleLiveEvent<ActionState> = _actionState
    val imageUploadData: MutableLiveData<ImageUploadData?> = MutableLiveData()

    private var pendingConnectionData: QrCodeData? = null

    private var eslCommandManager: EslCommandManager? = null
    private var timeoutJob: Job = createTimeoutJob()

    fun setEslCommandManager(manager: EslCommandManager) {
        eslCommandManager = manager
    }

    fun handleCharacteristicSubscription() {
        eslCommandManager?.subscribeToCharacteristic()
    }

    fun handleGattCommandProcessed() {
        eslCommandManager?.handleGattCommandProcessed()
    }

    fun toggleIsViewExpanded(tagInfoIndex: Int, isViewExpanded: Boolean) {
        tagsInfo[tagInfoIndex].isViewExpanded = isViewExpanded
    }

    fun getImageArray(tagIndex: Int) : Array<Uri?> {
        return tagsInfo[tagIndex].slotImages
    }

    fun connectTag(qrCodeData: QrCodeData) {
        if (tagsInfo.find { it.connectionData.address == qrCodeData.address } != null) {
            _actionState.postValue(ActionState.TagAlreadyExists)
        } else {
            pendingConnectionData = qrCodeData
            eslCommandManager?.connectTag(qrCodeData)
            _viewState.postValue(ViewState.LoadingState(EslCommand.CONNECT))
        }
    }

    fun connectExistingTag(tagIndex: Int) {
        tagsInfo[tagIndex].connectionData.let {
            pendingConnectionData = it
            eslCommandManager?.connectTag(it)
            _viewState.postValue(ViewState.LoadingState(EslCommand.CONNECT))
        }
    }

    fun configureTag() {
        eslCommandManager?.configureTag()
        _viewState.postValue(ViewState.LoadingState(EslCommand.CONFIGURE))
    }

    fun disconnectTag() {
        eslCommandManager?.disconnectTag()
        pendingConnectionData = null
        _viewState.postValue(ViewState.LoadingState(EslCommand.DISCONNECT))
    }

    fun loadTagsInfo() {
        eslCommandManager?.loadTagsInfo()
        _viewState.postValue(ViewState.LoadingState(EslCommand.LOAD_INFO))
    }

    fun deleteTag(address: String) {
        eslCommandManager?.deleteTag(address)
    }

    fun pingTag(index: Int) {
        val eslId = tagsInfo[index].tagInfo.eslId
        eslCommandManager?.pingTag(eslId)
        _viewState.postValue(ViewState.LoadingState(EslCommand.PING))
    }

    fun toggleLed(tagIndex: Int) {
        eslCommandManager?.toggleLed(!tagsInfo[tagIndex].tagInfo.isLedOn, tagsInfo[tagIndex].tagInfo.eslId)
    }

    fun toggleAllLeds() {
        eslCommandManager?.toggleAllLeds(!getGroupLedState())
    }

    fun updateTagLedImage(imageIndex: Int, imageFilepath: String) {
        eslCommandManager?.updateTagLedImage(imageIndex, imageFilepath)
    }

    fun displayTagLedImage(tagIndex: Int, imageIndex: Int) {
        eslCommandManager?.displayTagLedImage(tagsInfo[tagIndex].tagInfo.eslId, imageIndex, displayIndex = 0)
    }

    fun displayAllTagsImage(imageIndex: Int) {
        eslCommandManager?.displayAllTagsImage(imageIndex, displayIndex = 0)
    }

    fun clearTagsInfo() = tagsInfo.clear()

    private fun cleanupImageUpload() {
        imageUploadData.postValue(null)
    }

    fun handleCharacteristicChanged(char: BluetoothGattCharacteristic?) {
        clearTimeouts()

        char?.let {
            if(it.uuid == GattCharacteristic.EslControlPoint.uuid) {
                handleControlCharacteristicChanged(it)
            } else if (it.uuid == GattCharacteristic.EslTransferImage.uuid) {
                handleImageTransferCharacteristicChanged(it)
            }
        }
    }

    private fun handleControlCharacteristicChanged(char: BluetoothGattCharacteristic) {
        val executedCommand = EslCommand.fromCode(char.value[0].toInt())
        val commandStatus = char.value[1].toInt()

        if (commandStatus == ESL_COMMAND_STATUS_FAILURE) {
            _viewState.postValue(ViewState.IdleState(isTagListEmpty(), null))
            _actionState.postValue(ActionState.CommandError(executedCommand))
            cleanupImageUpload()
        } else if (commandStatus == ESL_COMMAND_STATUS_SUCCESS) { when (executedCommand) {
            EslCommand.CONNECT -> {
                if (tagsInfo.find { it.connectionData == pendingConnectionData } == null ) {
                    // new tag, proceed to configure
                    _viewState.postValue(ViewState.IdleState(isTagListEmpty(), DialogQuery.CONFIGURE_TAG))
                } else {
                    // connecting to existing tag, to upload image
                    imageUploadData.value?.let {
                        updateTagLedImage(it.slotIndex, it.filename)
                    }
                }
            }
            EslCommand.DISCONNECT -> {
                _viewState.postValue(ViewState.IdleState(isTagListEmpty(), null))
                _actionState.postValue(ActionState.CommandSuccess(executedCommand))
                imageUploadData.value?.let {
                    if(it.displayAfterUpload) {
                        displayTagLedImage(it.tagIndex, it.slotIndex)
                    }
                    cleanupImageUpload()
                }
            }
            EslCommand.CONFIGURE -> {
                pendingConnectionData?.let {
                    val tagInfo = TagInfo.parse(char.value.copyOfRange(2, char.value.size))
                    val tagViewInfo = TagViewInfo(tagInfo, arrayOfNulls(tagInfo.maxImageIndex + 1), it)
                    tagsInfo.add(tagViewInfo)
                    _actionState.postValue(ActionState.TagConfigured(tagViewInfo))
                }

                pendingConnectionData = null
                _viewState.postValue(ViewState.IdleState(isTagListEmpty(), null))
                disconnectTag()
            }
            EslCommand.TOGGLE_LED -> {
                val eslId = char.value[2].toInt()

                if (isGroupEslId(eslId)) {
                    val groupLedState = getGroupLedState()
                    tagsInfo.forEach { info -> info.tagInfo.isLedOn = !groupLedState }
                    _actionState.postValue(ActionState.GroupLedStateToggled(!groupLedState))
                } else findTagIndexById(eslId)?.let { tagIndex ->
                    val changedTag = tagsInfo[tagIndex]
                    changedTag.tagInfo.isLedOn = !changedTag.tagInfo.isLedOn
                    _actionState.postValue(ActionState.LedStateToggled
                        (tagIndex, changedTag.tagInfo.isLedOn, getGroupLedState()))
                }
            }
            EslCommand.LOAD_INFO -> {
                val tagListItem = TagListItem.parse(char.value.copyOfRange(2, char.value.size))
                tagListItem.tagInfo?.let { info ->
                    val connectionData = QrCodeData(EslCommand.CONNECT.message, info.bleAddress)
                    val tagViewInfo = TagViewInfo(info, arrayOfNulls(info.maxImageIndex + 1), connectionData)
                    tagsInfo.add(tagViewInfo)
                    _actionState.postValue(ActionState.TagConfigured(tagViewInfo))
                }

                if (tagListItem.isLast) {
                    _viewState.postValue(ViewState.IdleState(isTagListEmpty(), null))
                }
            }
            EslCommand.UPDATE_IMAGE -> {
                //apparently there's no additional info here besides command type and success msg
                imageUploadData.value?.let {
                    tagsInfo[it.tagIndex].slotImages[it.slotIndex] = it.uri
                }
                _actionState.postValue(ActionState.CommandSuccess(executedCommand))
                _viewState.postValue(ViewState.IdleState(isTagListEmpty(), null))
                disconnectTag()
                if(imageUploadData.value?.displayAfterUpload == false) {
                    cleanupImageUpload()
                }
            }
            EslCommand.DISPLAY_IMAGE -> {
                _actionState.postValue(ActionState.CommandSuccess(executedCommand))
            }
            EslCommand.PING -> {
                val pingInfo = PingInfo.parse(char.value.copyOfRange(2, char.value.size))

                val actionStateValue = when(pingInfo.tlvResponseBasicState) {
                    CORRECT_TLV_RESP_BASIC_STATE -> ActionState.TagPinged(pingInfo)
                    else -> ActionState.CommandError(executedCommand)
                }

                _actionState.postValue(actionStateValue)
                _viewState.postValue(ViewState.IdleState(isTagListEmpty(), null))
            }
            EslCommand.DELETE -> { }
            else -> Unit
        } }
    }

    private fun handleImageTransferCharacteristicChanged(char: BluetoothGattCharacteristic) {
        val header = char.value.firstOrNull()
        val expectedHeaderValue = 0xef.toByte()
        if (header == expectedHeaderValue) {
            val rawOffset = char.value.sliceArray(IntRange(1,4))
            val offset = ByteBuffer.wrap(rawOffset).order(ByteOrder.LITTLE_ENDIAN).int

            startTimeoutCount()
            imageUploadData.value?.let {
                val chunkLength = it.packetSize - 1
                eslCommandManager?.sendImageWrite(
                        data = it.data.sliceArray(IntRange(offset, it.data.size.coerceAtMost(offset + chunkLength) - 1)),
                        lastChunk = (it.data.size - offset <= chunkLength)
                )

                val percentage = (offset * 100) / it.data.size
                _viewState.postValue(ViewState.LoadingState(
                        EslCommand.UPDATE_IMAGE, "Transferring image to the tag: $percentage% done"))
                // TODO improvement: proper progress dialog
            }
        } else {
            _viewState.postValue(ViewState.IdleState(isTagListEmpty(), null))
            _actionState.postValue(ActionState.CommandError(EslCommand.UPDATE_IMAGE))
            cleanupImageUpload()
            disconnectTag()
            //TODO more handling?
        }
    }

    private fun isGroupEslId(rawValue: Int) = rawValue == -1

    private fun getGroupLedState() : Boolean {
        /* If there's at least on led 'on', group led will be presented as 'on' */
        return tagsInfo.any { it.tagInfo.isLedOn }
    }

    private fun findTagIndexById(eslId: Int) : Int? {
        val index = tagsInfo.indexOfFirst { it.tagInfo.eslId == eslId }
        return if (index != -1) index else null
    }

    fun startTimeoutCount() {
        if (!timeoutJob.isActive) {
            timeoutJob = createTimeoutJob()
        }
        timeoutJob.start()

    }

    private fun clearTimeouts() {
        timeoutJob.cancel()
    }

    private fun createTimeoutJob(): Job = viewModelScope.launch {
        delay(TIMEOUT)
        _actionState.postValue(ActionState.Timeout)
    }

    data class TagViewInfo(
        val tagInfo: TagInfo,
        val slotImages: Array<Uri?>,
        var connectionData: QrCodeData,
        var isViewExpanded: Boolean = false
    )

    private fun isTagListEmpty() = tagsInfo.isEmpty()

    sealed class ViewState {
        data class IdleState(val isTagListEmpty: Boolean, val dialogQuery: DialogQuery?) : ViewState()
        data class LoadingState(val commandBeingExecuted: EslCommand, val customText: String? = null) : ViewState()
    }

    sealed class ActionState {
        data class CommandSuccess(val commandExecuted: EslCommand) : ActionState()
        data class CommandError(val failedCommand: EslCommand?) : ActionState()
        data class TagConfigured(val configuredTag: TagViewInfo) : ActionState()
        object TagAlreadyExists : ActionState()
        data class TagPinged(val pingInfo: PingInfo) : ActionState()
        data class LedStateToggled(
            val tagIndex: Int,
            val isLedOn: Boolean,
            val isGroupLedOn: Boolean
        ) : ActionState()
        data class GroupLedStateToggled(val isGroupLedOn: Boolean) : ActionState()
        object Timeout : ActionState()
    }

    enum class DialogQuery {
        CONFIGURE_TAG
    }


    companion object {
        private const val ESL_COMMAND_STATUS_SUCCESS = 0x00
        private const val ESL_COMMAND_STATUS_FAILURE = 0x01

        private val TIMEOUT = 10.seconds
    }

}