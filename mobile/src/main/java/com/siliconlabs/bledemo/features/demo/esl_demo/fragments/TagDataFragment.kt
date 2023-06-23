package com.siliconlabs.bledemo.features.demo.esl_demo.fragments

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.MimeTypeMap
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import by.kirich1409.viewbindingdelegate.viewBinding
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.base.activities.BaseActivity
import com.siliconlabs.bledemo.bluetooth.ble.TimeoutGattCallback
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService
import com.siliconlabs.bledemo.common.other.CardViewListDecoration
import com.siliconlabs.bledemo.databinding.FragmentEslTagDataBinding
import com.siliconlabs.bledemo.features.demo.esl_demo.activities.EslDemoActivity
import com.siliconlabs.bledemo.features.demo.esl_demo.adapters.TagInfoAdapter
import com.siliconlabs.bledemo.features.demo.esl_demo.dialogs.EslDisplayImageDialog
import com.siliconlabs.bledemo.features.demo.esl_demo.dialogs.EslLoadedImagesDialog
import com.siliconlabs.bledemo.features.demo.esl_demo.dialogs.EslLoadingDialog
import com.siliconlabs.bledemo.features.demo.esl_demo.dialogs.EslPingInfoDialog
import com.siliconlabs.bledemo.features.demo.esl_demo.dialogs.EslPromptDialog
import com.siliconlabs.bledemo.features.demo.esl_demo.dialogs.EslRemoveTagDialog
import com.siliconlabs.bledemo.features.demo.esl_demo.dialogs.EslUploadImageDialog
import com.siliconlabs.bledemo.features.demo.esl_demo.model.EslCommand
import com.siliconlabs.bledemo.features.demo.esl_demo.model.EslCommandManager
import com.siliconlabs.bledemo.features.demo.esl_demo.model.ImageUploadData
import com.siliconlabs.bledemo.features.demo.esl_demo.model.PingInfo
import com.siliconlabs.bledemo.features.demo.esl_demo.model.QrCodeData
import com.siliconlabs.bledemo.features.demo.esl_demo.viewmodels.EslDemoViewModel
import com.siliconlabs.bledemo.utils.Constants
import com.siliconlabs.bledemo.utils.showOnce
import kotlinx.coroutines.launch
import timber.log.Timber

class TagDataFragment : Fragment(R.layout.fragment_esl_tag_data) {

    private val binding by viewBinding(FragmentEslTagDataBinding::bind)
    private val viewModel by viewModels<EslDemoViewModel>()

    private lateinit var tagInfoAdapter: TagInfoAdapter

    private var eslLoadingDialog: EslLoadingDialog? = null
    private var chunkSize: Int = with(Constants) { MIN_ALLOWED_MTU - ATT_HEADER_SIZE }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initNoTagsContainer()
        setupUiListeners()
        setupRecyclerView()
        setupDataObservers()
    }

    private fun setupUiListeners() {
        binding.apply {
            ibEslGroupLed.setOnClickListener {
                viewModel.toggleAllLeds()
            }
            ibEslUploadImage.setOnClickListener {
                showImageUploadDialog(
                    arrayOfNulls(GROUP_DISPLAY_SLOTS_COUNT),
                    loadedImagesDialogGroupCallback,
                )
            }
            ibEslDisplayImage.setOnClickListener {
                showImageDisplayDialog(
                    arrayOfNulls(GROUP_DISPLAY_SLOTS_COUNT),
                    loadedImagesDialogGroupCallback,
                )
            }
        }
    }

    private fun setupRecyclerView() {
        tagInfoAdapter = TagInfoAdapter(adapterListener)

        binding.rvTagInfoList.apply {
            adapter = tagInfoAdapter
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            addItemDecoration(CardViewListDecoration())

            val itemAnimator = itemAnimator as SimpleItemAnimator
            itemAnimator.supportsChangeAnimations = false
        }
    }

    private fun setupDataObservers() {
        viewModel.viewState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is EslDemoViewModel.ViewState.IdleState -> {
                    dismissLoadingDialog()
                    state.dialogQuery?.let {
                        EslPromptDialog(it, eslPromptDialogCallback).showOnce(childFragmentManager, ESL_DIALOG_TAG)
                    }
                }
                is EslDemoViewModel.ViewState.LoadingState -> {
                    showLoadingDialog(state.commandBeingExecuted, state.arg)
                }
            }
        }

        viewModel.actionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is EslDemoViewModel.ActionState.CommandSuccess -> {
                    (requireActivity() as BaseActivity).showMessage(getSuccessMessage(state.commandExecuted))
                }
                is EslDemoViewModel.ActionState.CommandError -> {
                    (requireActivity() as BaseActivity).showMessage(getErrorMessage(state.failedCommand))
                }
                is EslDemoViewModel.ActionState.TagAlreadyExists -> {
                    (requireActivity() as BaseActivity).showMessage(getString(R.string.tag_already_provisioned_message))
                }
                is EslDemoViewModel.ActionState.TagConfigured -> {
                    toggleMainContainer(isAnyTagConfigured = true)

                    viewModel.getTagIndex(state.configuredTag)?.let {
                        showImageUploadDialog(
                            viewModel.getImageArray(it),
                            uploadImageAfterTagConfigureCallback(it)
                        )
                    }
                }
                is EslDemoViewModel.ActionState.GroupLedStateToggled -> {
                    toggleGroupLedImage(state.isGroupLedOn)
                }
                is EslDemoViewModel.ActionState.TagPinged -> showPingDialog(state.pingInfo)
                is EslDemoViewModel.ActionState.TagRemoved -> handleTagRemove()
                is EslDemoViewModel.ActionState.ImageNotAvailable -> {
                    (requireActivity() as BaseActivity).showMessage(getString(R.string.no_image_to_display))
                }
                is EslDemoViewModel.ActionState.Timeout -> handleTimeout()
                else -> Unit
            }
        }

        lifecycleScope.launch {
            viewModel.tagsInfo.collect {
                toggleMainContainer(it.isNotEmpty())
                tagInfoAdapter.submitList(it)
            }
        }
    }

    fun prepareGatt(service: BluetoothService, gatt: BluetoothGatt?) {
        service.registerGattCallback(false, eslGattCallback)
        gatt?.discoverServices()
    }

    fun connectTag(qrCodeData: QrCodeData) {
        viewModel.connectTag(qrCodeData)
    }

    fun loadTags() = viewModel.loadTagsInfo()

    private fun initNoTagsContainer() {
        binding.containerNoTagsConfigured.apply {
            image.setImageResource(R.drawable.ic_esl_network)
            textPrimary.text = getString(R.string.no_tags_primary_text)
            textSecondary.text = getString(R.string.no_tags_secondary_text)
        }
    }

    private fun toggleMainContainer(isAnyTagConfigured: Boolean) {
        binding.apply {
            containerNoTagsConfigured.root.visibility = if (isAnyTagConfigured) View.GONE else View.VISIBLE
            containerTagsConfigured.visibility = if (isAnyTagConfigured) View.VISIBLE else View.GONE
        }
    }

    private fun toggleGroupLedImage(isGroupLedOn: Boolean) {
        context?.let {
            binding.ibEslGroupLed.iconTint = ColorStateList.valueOf(it.getColor(
                if (isGroupLedOn) R.color.esl_led_on
                else R.color.esl_led_off
            ))
        }
    }

    private fun showLoadingDialog(command: EslCommand, customTextData: Int? = null) {
        val customText = customTextData?.let { getCustomText(command, it) }

        eslLoadingDialog?.setText(command, customText) ?: run {
            eslLoadingDialog = EslLoadingDialog(command, customText).also {
                it.showOnce(childFragmentManager, EslLoadingDialog.FRAGMENT_NAME)
            }
        }
    }

    private fun getCustomText(command: EslCommand, arg: Int): String? = when(command) {
        EslCommand.UPDATE_IMAGE -> R.string.image_update_progress
        else -> null
    }?.let { getString(it, arg) }

    private fun dismissLoadingDialog() {
        eslLoadingDialog?.dismiss()
        eslLoadingDialog = null
    }

    private fun handleTagRemove() {
        (requireActivity() as BaseActivity).showMessage(getSuccessMessage(EslCommand.REMOVE))
    }

    private fun handleTimeout() {
        dismissLoadingDialog()
        (requireActivity() as BaseActivity).showMessage(getString(R.string.esl_timeout))

        viewModel.viewState.value.let {
            if (it is EslDemoViewModel.ViewState.LoadingState
                && it.commandBeingExecuted !in listOf(EslCommand.CONNECT, EslCommand.DISCONNECT)
            ) {
                viewModel.disconnectTag()
            }
            else dismissLoadingDialog()
        }
    }


    private fun getSuccessMessage(command: EslCommand) : String {
        return getString( when (command) {
            EslCommand.DISCONNECT -> R.string.disconnect_tag_success
            EslCommand.UPDATE_IMAGE -> R.string.image_update_success
            EslCommand.DISPLAY_IMAGE -> R.string.image_display_success
            EslCommand.REMOVE -> R.string.remove_tag_success
            else -> R.string.unknown_state
        })
    }

    private fun getErrorMessage(command: EslCommand?) : String {
        return getString( when (command) {
            EslCommand.CONNECT -> R.string.connect_tag_error
            EslCommand.CONFIGURE -> R.string.configure_tag_error
            EslCommand.PING -> R.string.ping_error
            EslCommand.DISCONNECT -> R.string.disconnect_tag_error
            EslCommand.REMOVE -> R.string.remove_tag_error
            EslCommand.UPDATE_IMAGE -> R.string.image_update_error
            EslCommand.DISPLAY_IMAGE -> R.string.image_display_error
            else -> R.string.unknown_state
        })
    }

    private val loadedImagesDialogGroupCallback = object : EslLoadedImagesDialog.Callback {
        override fun onUploadButtonClicked(slotIndex: Int, uri: Uri, displayAfterUpload: Boolean) {
            Timber.d("HERE; upload clicked")
            //TODO: handle if group upload becomes possible
        }

        override fun onDisplayButtonClicked(slotIndex: Int) {
            viewModel.displayAllTagsImage(slotIndex)
        }

        override fun onCancelButtonClicked() = Unit
    }

    private fun getTagLoadedImagesDialogCallback(tagIndex: Int) = object : EslLoadedImagesDialog.Callback {
        override fun onUploadButtonClicked(slotIndex: Int, uri: Uri, displayAfterUpload: Boolean) {
            startFileUpload(uri, slotIndex, tagIndex, displayAfterUpload)
        }

        override fun onDisplayButtonClicked(slotIndex: Int) {
            viewModel.displayTagLedImage(tagIndex, slotIndex)
        }

        override fun onCancelButtonClicked() = Unit
    }

    private fun uploadImageAfterTagConfigureCallback(tagIndex: Int) = object : EslLoadedImagesDialog.Callback {
        override fun onUploadButtonClicked(slotIndex: Int, uri: Uri, displayAfterUpload: Boolean) {
            startFileUploadAfterConfigure(uri, slotIndex, tagIndex, displayAfterUpload)
        }

        override fun onDisplayButtonClicked(slotIndex: Int) {
            viewModel.displayTagLedImage(tagIndex, slotIndex)
        }

        override fun onCancelButtonClicked() {
            viewModel.disconnectTag()
        }
    }

    private fun startFileUpload(uri: Uri, slotIndex: Int, tagIndex: Int, displayAfterUpload: Boolean) {
        val imageFileData = readImage(uri)

        imageFileData?.let {
            val imageUploadData = prepareImageUploadData(uri, it, slotIndex, tagIndex, displayAfterUpload)
            viewModel.imageUploadData.value = imageUploadData

            lifecycleScope.launch {
                viewModel.connectExistingTag(tagIndex)
                // connect handler calls image update when imageUploadData is present
            }
        }
    }

    private fun startFileUploadAfterConfigure(uri: Uri, slotIndex: Int, tagIndex: Int, displayAfterUpload: Boolean) {
        val imageFileData = readImage(uri)

        imageFileData?.let {
            val imageUploadData = prepareImageUploadData(uri, it, slotIndex, tagIndex, displayAfterUpload)
            viewModel.imageUploadData.value = imageUploadData

            lifecycleScope.launch {
                viewModel.updateTagLedImage(imageUploadData.slotIndex, imageUploadData.filename)
            }
        }
    }

    private fun prepareImageUploadData(
        uri: Uri,
        imageFileData: ByteArray,
        slotIndex: Int,
        tagIndex: Int,
        displayAfterUpload: Boolean,
    ): ImageUploadData {
        val mimetype = requireContext().contentResolver.getType(uri)
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimetype)
        val filename = if (extension != null) "image.$extension" else "image"

        return ImageUploadData(
            uri,
            filename,
            imageFileData,
            slotIndex,
            tagIndex,
            displayAfterUpload,
            chunkSize,
        )
    }

    private fun readImage(uri: Uri) : ByteArray? {
        return try {
            val imageInputStream = this.requireContext().contentResolver.openInputStream(uri)

            val size = imageInputStream!!.available()

            val temp = ByteArray(size)
            imageInputStream.read(temp)
            imageInputStream.close()
            temp
        } catch (e: Exception) {
            Timber.d(e, "Couldn't open image")
            null
        }
    }

    private fun showImageDisplayDialog(
        imageArray: Array<Uri?>,
        callback: EslLoadedImagesDialog.Callback,
    ) {
        val dialog = EslDisplayImageDialog(
            imageArray,
            callback,
        )

        showDialogFragment(dialog)
    }

    private fun showImageUploadDialog(
        imageArray: Array<Uri?>,
        callback: EslLoadedImagesDialog.Callback,
    ) {
        val dialog = EslUploadImageDialog(
            imageArray,
            callback,
        )

        showDialogFragment(dialog)
    }

    private fun showPingDialog(
        pingInfo: PingInfo,
    ) {
        val dialog = EslPingInfoDialog(pingInfo)
        showDialogFragment(dialog)
    }

    private fun showRemoveDialog(index: Int) {
        val dialog = EslRemoveTagDialog {
            viewModel.removeTag(index)
        }
        showDialogFragment(dialog)
    }

    private fun showDialogFragment(dialog: DialogFragment, tag: String = ESL_DIALOG_TAG) {
        dialog.showOnce(childFragmentManager, tag)
    }

    private val eslPromptDialogCallback = object : EslPromptDialog.EslDialogCallback {
        override fun handlePositiveDialogAction(dialogQuery: EslDemoViewModel.DialogQuery) {
            when (dialogQuery) {
                EslDemoViewModel.DialogQuery.CONFIGURE_TAG -> viewModel.configureTag()
            }
        }

        override fun handleNegativeDialogAction(dialogQuery: EslDemoViewModel.DialogQuery) {
            when (dialogQuery) {
                EslDemoViewModel.DialogQuery.CONFIGURE_TAG -> viewModel.disconnectTag()
            }
        }
    }

    private val adapterListener: TagInfoAdapter.Listener = object : TagInfoAdapter.Listener {
        override fun onExpandArrowClicked(index: Int, isViewExpanded: Boolean) {
            viewModel.toggleIsViewExpanded(index, isViewExpanded)
        }

        override fun onLedButtonClicked(index: Int) {
            viewModel.toggleLed(index)
        }

        override fun onUploadImageClicked(index: Int) {
            showImageUploadDialog(
                viewModel.getImageArray(index),
                getTagLoadedImagesDialogCallback(index),
            )
        }

        override fun onDisplayImageClicked(index: Int) {
            showImageDisplayDialog(
                viewModel.getImageArray(index),
                getTagLoadedImagesDialogCallback(index),
            )
        }

        override fun onPingButtonClicked(index: Int) {
            viewModel.pingTag(index)
        }

        override fun onRemoveButtonClicked(index: Int) {
            showRemoveDialog(index)
        }
    }


    private val eslGattCallback = object : TimeoutGattCallback() {

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)

            if (status == BluetoothGatt.GATT_SUCCESS) {
                chunkSize = mtu - Constants.ATT_HEADER_SIZE
                viewModel.imageUploadData.value?.packetSize = chunkSize // if mtu changed during upload
            }

            Timber.d("onMtuChanged(): chunkSize = $chunkSize")

            viewModel.setEslCommandManager(EslCommandManager(gatt, viewModel::startTimeoutCount))
            viewModel.handleCharacteristicSubscription()
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            val activity = activity as? EslDemoActivity

            if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> activity?.handleOnDeviceDisconnected()
                    else -> {
                        activity?.showMessage(getString(R.string.device_terminated_connection, status))
                        activity?.finish()
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            gatt?.requestMtu(Constants.MAX_ALLOWED_MTU)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            viewModel.handleGattCommandProcessed()
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            viewModel.handleCharacteristicChanged(characteristic)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            viewModel.handleGattCommandProcessed()
        }
    }
    companion object {
        private const val GROUP_DISPLAY_SLOTS_COUNT = 2
        private const val ESL_DIALOG_TAG = "esl_dialog_tag"
    }
}