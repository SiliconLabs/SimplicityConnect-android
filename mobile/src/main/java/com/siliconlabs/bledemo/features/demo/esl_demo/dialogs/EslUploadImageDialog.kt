package com.siliconlabs.bledemo.features.demo.esl_demo.dialogs

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.siliconlabs.bledemo.R

class EslUploadImageDialog(
    imageArray: Array<Uri?>,
    callback: Callback
) : EslLoadedImagesDialog(imageArray, callback) {

    private var imageLoaderRequest = 
    registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { result ->
        result?.let { updateSelectedImage(it) }
    }

    private var imageToUpload: Uri? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cbDisplayAfterUpload.isChecked = true
        binding.cbDisplayAfterUpload.visibility = View.VISIBLE
    }

    private fun updateSelectedImage(uri: Uri) {
        imageToUpload = uri
        this.context?.let {
            Glide.with(it)
                .load(uri)
                .centerCrop()
                .into(binding.eslImageSelected)
            binding.eslSelectedImageMessage.visibility = View.VISIBLE
            binding.eslImageSelected.visibility = View.VISIBLE
            binding.btnPositiveAction.isEnabled = true
        }
    }

    override fun getDialogTitle() : String {
        return getString(R.string.dialog_upload_image_title)
    }

    override fun getDialogMessage(): String {
        return getString(R.string.dialog_upload_image_message)
    }

    override fun getDialogNote(): String {
        return getString(R.string.dialog_upload_image_note)
    }

    override fun getPositiveButtonText(): String {
        return getString(R.string.dialog_btn_upload)
    }

    override fun onPositiveButtonClicked() {
        super.onPositiveButtonClicked()
        slotIndex?.let {slot ->
            imageToUpload?.let {uri ->
                callback.onUploadButtonClicked(slot, uri, binding.cbDisplayAfterUpload.isChecked)
            }
        }
    }

    override fun onSlotClicked(index: Int) {
        super.onSlotClicked(index)
        imageLoaderRequest.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

}