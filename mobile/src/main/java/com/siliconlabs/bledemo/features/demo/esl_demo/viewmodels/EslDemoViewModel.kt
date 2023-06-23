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
import kotlinx.coroutines.CoroutineStart
import com.siliconlabs.bledemo.utils.indexOrNull
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.time.Duration.Companion.seconds

class EslDemoViewModel : ViewModel() {

    private var oldTagsInfo = listOf<TagViewInfo>()
    private val _tagsInfo = MutableStateFlow<List<TagViewInfo>>(emptyList())
    val tagsInfo get() = _tagsInfo.asStateFlow()

    private val _viewState: MutableLiveData<ViewState> = MutableLiveData()
    val viewState: LiveData<ViewState> = _viewState
    private val _actionState: SingleLiveEvent<ActionState> = SingleLiveEvent()
    val actionState: SingleLiveEvent<ActionState> = _actionState
    val imageUploadData: MutableLiveData<ImageUploadData?> = MutableLiveData()

    private val _groupLedBtnToggled = MutableStateFlow(false)

    private val removedEslIdChannel = Channel<Int>(CONFLATED)

    private var pendingConnectionAddress: String? = null

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
        _tagsInfo.value[tagInfoIndex].isViewExpanded = isViewExpanded
    }

    fun getImageArray(tagIndex: Int): Array<Uri?> {
        return _tagsInfo.value[tagIndex].slotImages
    }

    fun getTagIndex(tag: TagViewInfo): Int? = tagsInfo.value.indexOrNull(tag)

    fun connectTag(qrCodeData: QrCodeData) {
        if (_tagsInfo.value.find { it.tagInfo.bleAddress.uppercase() == qrCodeData.address.uppercase() } != null) {
            _actionState.postValue(ActionState.TagAlreadyExists)
        } else {
            pendingConnectionAddress = qrCodeData.address
            eslCommandManager?.connectTagByQr(qrCodeData)
            _viewState.postValue(ViewState.LoadingState(EslCommand.CONNECT))
        }
    }

    fun connectExistingTag(tagIndex: Int) {
        _tagsInfo.value[tagIndex].let {
            pendingConnectionAddress = it.tagInfo.bleAddress
            eslCommandManager?.connectTagById(it.tagInfo.eslId)
            _viewState.postValue(ViewState.LoadingState(EslCommand.CONNECT))
        }
    }

    fun configureTag() {
        eslCommandManager?.configureTag()
        _viewState.postValue(ViewState.LoadingState(EslCommand.CONFIGURE))
    }

    fun disconnectTag() {
        eslCommandManager?.disconnectTag()
        pendingConnectionAddress = null
        _viewState.postValue(ViewState.LoadingState(EslCommand.DISCONNECT))
    }

    fun loadTagsInfo() {
        clearList()

        eslCommandManager?.loadTagsInfo()
        _viewState.postValue(ViewState.LoadingState(EslCommand.LOAD_INFO))
    }

    fun removeTag(index: Int) {
        viewModelScope.launch {
            val eslId = _tagsInfo.value[index].tagInfo.eslId
            eslCommandManager?.removeTag(eslId)
            removedEslIdChannel.send(eslId)
            _viewState.postValue(ViewState.LoadingState(EslCommand.REMOVE))
        }
    }

    fun pingTag(index: Int) {
        val eslId = _tagsInfo.value[index].tagInfo.eslId
        eslCommandManager?.pingTag(eslId)
        _viewState.postValue(ViewState.LoadingState(EslCommand.PING))
    }

    fun toggleLed(tagIndex: Int) {
        eslCommandManager?.toggleLed(
            shouldSubmitStateOn = !_tagsInfo.value[tagIndex].tagInfo.isLedOn,
            eslId = _tagsInfo.value[tagIndex].tagInfo.eslId,
        )
    }

    fun toggleAllLeds() {
        eslCommandManager?.toggleAllLeds(!_groupLedBtnToggled.value)
    }

    fun updateTagLedImage(imageIndex: Int, imageFilepath: String) {
        eslCommandManager?.updateTagLedImage(imageIndex, imageFilepath)
    }

    fun displayTagLedImage(tagIndex: Int, imageIndex: Int) {
        eslCommandManager?.displayTagLedImage(
            _tagsInfo.value[tagIndex].tagInfo.eslId,
            imageIndex,
            displayIndex = 0,
        )
    }

    fun displayAllTagsImage(imageIndex: Int) {
        eslCommandManager?.displayAllTagsImage(imageIndex, displayIndex = 0)
    }

    private fun cleanupImageUpload() {
        imageUploadData.postValue(null)
    }

    fun handleCharacteristicChanged(char: BluetoothGattCharacteristic?) {
        clearTimeouts()

        char?.let {
            if (it.uuid == GattCharacteristic.EslControlPoint.uuid) {
                handleControlCharacteristicChanged(it)
            } else if (it.uuid == GattCharacteristic.EslTransferImage.uuid) {
                handleImageTransferCharacteristicChanged(it)
            }
        }
    }

    private fun clearList() {
        oldTagsInfo = _tagsInfo.value
        _tagsInfo.value = emptyList()
    }

    private fun handleControlCharacteristicChanged(char: BluetoothGattCharacteristic) {
        val executedCommand = EslCommand.fromCode(char.value[0].toInt())
        val commandStatus = char.value[1].toInt()

        if (commandStatus == ESL_COMMAND_STATUS_FAILURE) {
            _viewState.postValue(ViewState.IdleState(null))
            _actionState.postValue(ActionState.CommandError(executedCommand))
            cleanupImageUpload()
        } else if (commandStatus == ESL_COMMAND_STATUS_SUCCESS) { when (executedCommand) {
            EslCommand.CONNECT -> {
                if (_tagsInfo.value.find { it.tagInfo.bleAddress.uppercase() == pendingConnectionAddress?.uppercase() } == null ) {
                    // new tag, proceed to configure
                    _viewState.postValue(ViewState.IdleState(DialogQuery.CONFIGURE_TAG))
                } else {
                    // connecting to existing tag, to upload image
                    imageUploadData.value?.let {
                        updateTagLedImage(it.slotIndex, it.filename)
                    }
                }
            }
            EslCommand.DISCONNECT -> {
                _viewState.postValue(ViewState.IdleState(null))
                _actionState.postValue(ActionState.CommandSuccess(executedCommand))
                imageUploadData.value?.let {
                    if(it.displayAfterUpload) {
                        displayTagLedImage(it.tagIndex, it.slotIndex)
                    }
                    cleanupImageUpload()
                }
            }
            EslCommand.CONFIGURE -> {
                pendingConnectionAddress?.let {
                    val tagInfo = TagInfo.parse(char.value.copyOfRange(2, char.value.size))
                    val tagViewInfo = TagViewInfo(tagInfo, arrayOfNulls(tagInfo.maxImageIndex + 1))

                    _tagsInfo.value = _tagsInfo.value + tagViewInfo
                    _actionState.postValue(ActionState.TagConfigured(tagViewInfo))
                }

                pendingConnectionAddress = null
                _viewState.postValue(ViewState.IdleState(null))
            }
            EslCommand.TOGGLE_LED -> {
                val eslId = char.value[2].toInt()

                if (isGroupEslId(eslId)) {
                    _groupLedBtnToggled.apply { value = !value }

                    _tagsInfo.value = _tagsInfo.value.map {
                        val newTagInfo = it.tagInfo.copy(isLedOn = _groupLedBtnToggled.value)
                        it.copy(tagInfo = newTagInfo)
                    }

                    _actionState.postValue(ActionState.GroupLedStateToggled(_groupLedBtnToggled.value))
                } else findTagIndexById(eslId)?.let { tagIndex ->
                    val updatedList = _tagsInfo.value.mapIndexed { index, tagViewInfo ->
                        val newTagInfo = if (index == tagIndex) {
                            val isLedOn = tagViewInfo.tagInfo.isLedOn
                            tagViewInfo.tagInfo.copy(isLedOn = !isLedOn)
                        } else tagViewInfo.tagInfo

                        tagViewInfo.copy(tagInfo = newTagInfo)
                    }

                    _tagsInfo.value = updatedList
                }
            }
            EslCommand.LOAD_INFO -> {
                val tagListItem = TagListItem.parse(char.value.copyOfRange(2, char.value.size))
                tagListItem.tagInfo?.let { info ->
                    var tagViewInfo = TagViewInfo(info, arrayOfNulls(info.maxImageIndex + 1))

                    oldTagsInfo.find { it.tagInfo.eslId == tagViewInfo.tagInfo.eslId }?.let {
                        tagViewInfo = tagViewInfo.copy(
                            tagInfo = it.tagInfo,
                            slotImages = it.slotImages,
                            isViewExpanded = it.isViewExpanded,
                        )
                    }

                    _tagsInfo.value = _tagsInfo.value + tagViewInfo
                }

                if (tagListItem.isLast) {
                    oldTagsInfo = emptyList()
                    _viewState.postValue(ViewState.IdleState(null))
                }
            }
            EslCommand.UPDATE_IMAGE -> {
                //apparently there's no additional info here besides command type and success msg
                imageUploadData.value?.let {
                    val updatedList = _tagsInfo.value.toList().apply {
                        this[it.tagIndex].slotImages[it.slotIndex] = it.uri
                    }

                    _tagsInfo.value = updatedList
                }
                _actionState.postValue(ActionState.CommandSuccess(executedCommand))
                _viewState.postValue(ViewState.IdleState(null))
                disconnectTag()
                if(imageUploadData.value?.displayAfterUpload == false) {
                    cleanupImageUpload()
                }
            }
            EslCommand.DISPLAY_IMAGE -> {
                if (char.value.last().toInt() == IMAGE_UPLOAD_NO_IMAGE_ERROR) {
                    _actionState.postValue(ActionState.ImageNotAvailable)
                } else {
                    _actionState.postValue(ActionState.CommandSuccess(executedCommand))
                }
            }
            EslCommand.PING -> {
                val pingInfo = PingInfo.parse(char.value.copyOfRange(2, char.value.size))

                val actionStateValue = when(pingInfo.tlvResponseBasicState) {
                    CORRECT_TLV_RESP_BASIC_STATE -> ActionState.TagPinged(pingInfo)
                    else -> ActionState.CommandError(executedCommand)
                }

                if (pingInfo.tlvResponseBasicState == CORRECT_TLV_RESP_BASIC_STATE) {
                    val ledState = pingInfo.activeLed
                    val pingedTagIndex = tagsInfo.value.indexOrNull { it.tagInfo.eslId == pingInfo.eslId }

                    pingedTagIndex?.let {
                        val newTagsInfo = _tagsInfo.value.toMutableList()
                        val pingedTag = newTagsInfo[it]
                        val pingedTagInfo = pingedTag.tagInfo

                        newTagsInfo[it] = pingedTag.copy(tagInfo = pingedTagInfo.copy(isLedOn = ledState))
                        _tagsInfo.value = newTagsInfo
                    }
                }

                _actionState.postValue(actionStateValue)
                _viewState.postValue(ViewState.IdleState(null))
            }

            EslCommand.REMOVE -> viewModelScope.launch {
                val eslId = removedEslIdChannel.receive()
                _tagsInfo.value.indexOrNull { it.tagInfo.eslId == eslId }?.let {
                    val updatedList = _tagsInfo.value.toMutableList().apply { removeAt(it) }
                    _tagsInfo.value = updatedList
                }

                _actionState.postValue(ActionState.TagRemoved(eslId))
                _viewState.postValue(ViewState.IdleState(null))
            }

            else -> Unit
        }
        }
    }

    private fun handleImageTransferCharacteristicChanged(char: BluetoothGattCharacteristic) {
        val header = char.value.firstOrNull()
        val expectedHeaderValue = 0xef.toByte()
        if (header == expectedHeaderValue) {
            val rawOffset = char.value.sliceArray(IntRange(1, 4))
            val offset = ByteBuffer.wrap(rawOffset).order(ByteOrder.LITTLE_ENDIAN).int

            startTimeoutCount()
            imageUploadData.value?.let {
                val chunkLength = it.packetSize - 1
                val dataRange =
                    IntRange(offset, it.data.size.coerceAtMost(offset + chunkLength) - 1)
                eslCommandManager?.sendImageWrite(
                    data = it.data.sliceArray(dataRange),
                    lastChunk = (it.data.size - offset <= chunkLength)
                )

                val percentage = (offset * 100) / it.data.size
                _viewState.postValue(ViewState.LoadingState(EslCommand.UPDATE_IMAGE, percentage))
                // TODO improvement: proper progress dialog
            }
        } else {
            _viewState.postValue(ViewState.IdleState(null))
            _actionState.postValue(ActionState.CommandError(EslCommand.UPDATE_IMAGE))
            cleanupImageUpload()
            disconnectTag()
            //TODO more handling?
        }
    }

    private fun isGroupEslId(rawValue: Int) = rawValue == -1

    private fun findTagIndexById(eslId: Int) : Int? =
        _tagsInfo.value.indexOrNull { it.tagInfo.eslId == eslId }

    fun startTimeoutCount() {
        if (!timeoutJob.isActive) {
            timeoutJob = createTimeoutJob()
        }

        timeoutJob.start()
    }

    private fun clearTimeouts() {
        timeoutJob.cancel()
    }

    private fun createTimeoutJob(): Job = viewModelScope.launch(start = CoroutineStart.LAZY) {
        delay(TIMEOUT)
        _actionState.postValue(ActionState.Timeout)
    }

    data class TagViewInfo(
        val tagInfo: TagInfo,
        val slotImages: Array<Uri?>,
        var isViewExpanded: Boolean = false
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as TagViewInfo

            if (tagInfo != other.tagInfo) return false
            if (!slotImages.contentEquals(other.slotImages)) return false
            if (isViewExpanded != other.isViewExpanded) return false

            return true
        }

        override fun hashCode(): Int {
            var result = tagInfo.hashCode()
            result = 31 * result + slotImages.contentHashCode()
            result = 31 * result + isViewExpanded.hashCode()
            return result
        }
    }

    sealed class ViewState {
        data class IdleState(val dialogQuery: DialogQuery?) : ViewState()
        data class LoadingState(val commandBeingExecuted: EslCommand, val arg: Int? = null) : ViewState()
    }

    sealed class ActionState {
        data class CommandSuccess(val commandExecuted: EslCommand) : ActionState()
        data class CommandError(val failedCommand: EslCommand?) : ActionState()
        data class TagConfigured(val configuredTag: TagViewInfo) : ActionState()
        object TagAlreadyExists : ActionState()
        data class GroupLedStateToggled(val isGroupLedOn: Boolean) : ActionState()
        data class TagPinged(val pingInfo: PingInfo) : ActionState()
        data class TagRemoved(val eslId: Int) : ActionState()
        object ImageNotAvailable : ActionState()
        object Timeout : ActionState()
    }

    enum class DialogQuery {
        CONFIGURE_TAG,
    }


    companion object {
        private const val ESL_COMMAND_STATUS_SUCCESS = 0x00
        private const val ESL_COMMAND_STATUS_FAILURE = 0x01

        private const val IMAGE_UPLOAD_NO_IMAGE_ERROR = 0x05

        private val TIMEOUT = 10.seconds
    }

}