package com.siliconlabs.bledemo.features.demo.esl_demo.fragments

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.MimeTypeMap
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.siliconlabs.bledemo.features.demo.esl_demo.dialogs.EslUploadImageDialog
import com.siliconlabs.bledemo.features.demo.esl_demo.model.EslCommand
import com.siliconlabs.bledemo.features.demo.esl_demo.model.EslCommandManager
import com.siliconlabs.bledemo.features.demo.esl_demo.model.ImageUploadData
import com.siliconlabs.bledemo.features.demo.esl_demo.model.PingInfo
import com.siliconlabs.bledemo.features.demo.esl_demo.model.QrCodeData
import com.siliconlabs.bledemo.features.demo.esl_demo.viewmodels.EslDemoViewModel
import com.siliconlabs.bledemo.utils.Constants
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
        setupDataObservers()
        setupRecyclerView()
    }

    private fun setupUiListeners() {
        binding.apply {
            ibEslGroupLed.setOnClickListener {
                viewModel.toggleAllLeds()
            }
            ibEslUploadImage.setOnClickListener {
                EslUploadImageDialog(arrayOfNulls(GROUP_DISPLAY_SLOTS_COUNT), loadedImagesDialogCallback)
                    .show(childFragmentManager, "loaded_images_dialog")
            }
            ibEslDisplayImage.setOnClickListener {
                EslDisplayImageDialog(arrayOfNulls(GROUP_DISPLAY_SLOTS_COUNT), loadedImagesDialogCallback)
                    .show(childFragmentManager, "loaded_images_dialog")
            }
        }
    }

    private fun setupRecyclerView() {
        tagInfoAdapter = TagInfoAdapter(adapterListener)

        binding.rvTagInfoList.apply {
            adapter = tagInfoAdapter
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            addItemDecoration(CardViewListDecoration())
        }
    }

    private fun setupDataObservers() {
        viewModel.viewState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is EslDemoViewModel.ViewState.IdleState -> {
                    dismissLoadingDialog()
                    state.dialogQuery?.let {
                        EslPromptDialog(it, eslPromptDialogCallback).show(childFragmentManager, "esl_prompt_dialog")
                    }
                }
                is EslDemoViewModel.ViewState.LoadingState -> {
                    showLoadingDialog(state.commandBeingExecuted, state.customText)
                }
            }
        }
        viewModel.actionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is EslDemoViewModel.ActionState.CommandSuccess -> {
                    (activity as? BaseActivity)?.showMessage(getSuccessMessage(state.commandExecuted))
                }
                is EslDemoViewModel.ActionState.CommandError -> {
                    (activity as? BaseActivity)?.showMessage(getErrorMessage(state.failedCommand))
                }
                is EslDemoViewModel.ActionState.TagAlreadyExists -> {
                    (requireActivity() as BaseActivity).showMessage(getString(R.string.tag_already_provisioned_message))
                }
                is EslDemoViewModel.ActionState.TagConfigured -> {
                    tagInfoAdapter.showNewTag(state.configuredTag)
                    toggleMainContainer(isAnyTagConfigured = true)
                }
                is EslDemoViewModel.ActionState.TagPinged -> {
                    showPingInfoDialog(state.pingInfo)
                }
                is EslDemoViewModel.ActionState.LedStateToggled -> {
                    tagInfoAdapter.toggleLedImage(state.tagIndex, state.isLedOn)
                    toggleGroupLedImage(state.isGroupLedOn)
                }
                is EslDemoViewModel.ActionState.GroupLedStateToggled -> {
                    tagInfoAdapter.toggleAllLedImages(state.isGroupLedOn)
                    toggleGroupLedImage(state.isGroupLedOn)
                }
                is EslDemoViewModel.ActionState.Timeout -> handleTimeout()
                else -> Unit
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

    fun clearAdapterData() {
        tagInfoAdapter.clear()
        viewModel.clearTagsInfo()
    }

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
            binding.ibEslGroupLed.imageTintList = ColorStateList.valueOf(it.getColor(
                if (isGroupLedOn) R.color.esl_led_on
                else R.color.esl_led_off
            ))
        }
    }

    private fun showLoadingDialog(command: EslCommand, customText: String? = null) {
        eslLoadingDialog?.setText(command, customText) ?: run {
            eslLoadingDialog = EslLoadingDialog(command, customText).also {
                it.show(childFragmentManager, EslLoadingDialog.FRAGMENT_NAME)
            }
        }
    }

    private fun dismissLoadingDialog() {
        eslLoadingDialog?.dismiss()
        eslLoadingDialog = null
    }

    private fun showPingInfoDialog(pingInfo: PingInfo) =
        EslPingInfoDialog(pingInfo).show(childFragmentManager, "ping_info_dialog")

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
            else -> R.string.unknown_state
        })
    }

    private fun getErrorMessage(command: EslCommand?) : String {
        return getString( when (command) {
            EslCommand.CONNECT -> R.string.connect_tag_error
            EslCommand.CONFIGURE -> R.string.configure_tag_error
            EslCommand.PING -> R.string.ping_error
            EslCommand.DISCONNECT -> R.string.disconnect_tag_error
            EslCommand.UPDATE_IMAGE -> R.string.image_update_error
            EslCommand.DISPLAY_IMAGE -> R.string.image_display_error
            else -> R.string.unknown_state
        })
    }

    private val loadedImagesDialogCallback = object : EslLoadedImagesDialog.Callback {
        override fun onUploadButtonClicked(slotIndex: Int, uri: Uri, displayAfterUpload: Boolean) {
            Timber.d("HERE; upload clicked")
            //TODO: handle if group upload becomes possible
        }

        override fun onDisplayButtonClicked(slotIndex: Int) {
            viewModel.displayAllTagsImage(slotIndex)
        }
    }

    private fun getTagLoadedImagesDialogCallback(tagIndex: Int): EslLoadedImagesDialog.Callback {
        return object : EslLoadedImagesDialog.Callback {
            override fun onUploadButtonClicked(slotIndex: Int, uri: Uri, displayAfterUpload: Boolean) {
                startFileUpload(uri, slotIndex, tagIndex, displayAfterUpload)
            }

            override fun onDisplayButtonClicked(slotIndex: Int) {
                viewModel.displayTagLedImage(tagIndex, slotIndex)
            }
        }
    }

    private fun startFileUpload(uri: Uri, slotIndex: Int, tagIndex: Int, displayAfterUpload: Boolean) {
        val imageFileData = readImage(uri)

        imageFileData?.let {
            val mimetype = requireContext().contentResolver.getType(uri)
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimetype)
            val filename = if(extension != null) "image.$extension" else "image"

            val imageUploadData = ImageUploadData(uri, filename, it, slotIndex, tagIndex, displayAfterUpload, chunkSize)
            viewModel.imageUploadData.value = imageUploadData

            lifecycleScope.launch {
                viewModel.connectExistingTag(tagIndex)
                // connect handler calls image update when imageUploadData is present
            }
        }
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
            EslUploadImageDialog(
                viewModel.getImageArray(index),
                getTagLoadedImagesDialogCallback(index)
            ).show(childFragmentManager, "loaded_images_dialog")
        }

        override fun onDisplayImageClicked(index: Int) {
            EslDisplayImageDialog(
                viewModel.getImageArray(index),
                getTagLoadedImagesDialogCallback(index)
            ).show(childFragmentManager, "loaded_images_dialog")
        }

        override fun onPingButtonClicked(index: Int) {
            viewModel.pingTag(index)
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
    }
}