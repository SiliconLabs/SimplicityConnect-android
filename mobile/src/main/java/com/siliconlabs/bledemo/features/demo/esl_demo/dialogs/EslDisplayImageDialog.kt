package com.siliconlabs.bledemo.features.demo.esl_demo.dialogs

import android.net.Uri
import com.siliconlabs.bledemo.R

class EslDisplayImageDialog(
    imageArray: Array<Uri?>,
    callback: Callback,
    private val blankSlotAllowed: Boolean = true
) : EslLoadedImagesDialog(imageArray, callback) {

    override fun getDialogTitle(): String {
        return getString(R.string.dialog_display_image_title)
    }

    override fun getDialogMessage(): String {
        return getString(R.string.dialog_display_image_message)
    }

    override fun getDialogNote(): String {
        return getString(R.string.dialog_loaded_images_note)
    }

    override fun getPositiveButtonText(): String {
        return getString(R.string.dialog_btn_display)
    }

    override fun onPositiveButtonClicked() {
        super.onPositiveButtonClicked()
        slotIndex?.let { slot ->
            callback.onDisplayButtonClicked(slot)
        }
    }

    override fun onSlotClicked(index: Int) {
        if (blankSlotAllowed || loadedImagesAdapter.isChosenSlotNotEmpty()) {
            super.onSlotClicked(index)
            binding.btnPositiveAction.isEnabled = true
        } else {
            binding.btnPositiveAction.isEnabled = false
            showMessage(R.string.no_image_to_display)
        }
    }

}