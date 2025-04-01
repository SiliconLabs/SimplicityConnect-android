package com.siliconlabs.bledemo.features.demo.esl_demo.dialogs

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import coil.load
import coil.size.Scale
import coil.transform.RoundedCornersTransformation
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.features.demo.esl_demo.model.ImageSize
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

class EslUploadImageDialog(
    imageArray: Array<Uri?>,
    callback: Callback,
) : EslLoadedImagesDialog(imageArray, callback) {
    private val maxImageSizeKB = 100 // Maximum allowed image size in KB
    private var imageLoaderRequest =
    registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { result ->
        Log.e("ESIUPLOADDIALOG",""+result.toString())
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
            binding.eslImageSelected.load(uri) {
                scale(Scale.FILL)
                transformations(RoundedCornersTransformation())
            }

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
                Log.e("ESL_DEM0_DIALOG","URI:-"+uri)
                val imageSize = getImageSize(uri)
                if (imageSize.sizeInKB > maxImageSizeKB) {

                    Log.e("ImageHelper", "Image size exceeds the limit: ${imageSize.sizeInKB} KB")
                    showImageSizeExceededDialog(uri = uri,slot)
                } else {
                    Log.d("ImageHelper", "Image Size: Width = ${imageSize.width}, Height = ${imageSize.height}, Size = ${imageSize.sizeInKB} KB")
                    // Process the image here, as it's within the allowed size
                    // You can use the imageSize.width and imageSize.height here
                    // to update your UI or perform other operations.
                    callback.onUploadButtonClicked(slot, uri, binding.cbDisplayAfterUpload.isChecked)
                }

            }
        }
    }

    private fun showImageSizeExceededDialog(uri: Uri, slot: Int) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.alert))
            .setMessage(getString(R.string.big_size_image_content))
            .setPositiveButton(getString(R.string.yes)) { dialog, _ ->
                // User wants to continue uploading
                Log.d("ImageHelper", "User chose to continue uploading large image.")
                // Process the image here
                //binding  = viewBinding(DialogEslLoadedImagesBinding::bind)
                callback.onUploadButtonClicked(slot, uri, true)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.button_cancel)) { dialog, _ ->
                // User canceled the upload
                Log.d("ImageHelper", "User canceled uploading large image.")
                dialog.dismiss()
                callback.onShowingUploadDialogAgain()
            }
            .setCancelable(false) // Prevent dismissing the dialog by tapping outside
            .show()


    }

    override fun onSlotClicked(index: Int) {
        super.onSlotClicked(index)
        imageLoaderRequest.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun getImageSize(uri: Uri): ImageSize {
        val contentResolver: ContentResolver = requireActivity().contentResolver
        var inputStream: InputStream? = null
        try {
            inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeStream(inputStream, null, options)

                // Calculate the file size
                val fileSizeInBytes = contentResolver.openAssetFileDescriptor(uri, "r")?.length ?: 0
                val fileSizeInKB = fileSizeInBytes / 1024

                return ImageSize(options.outWidth, options.outHeight, fileSizeInKB)
            } else {
                Log.e("ImageHelper", "Failed to open input stream for URI: $uri")
            }
        } catch (e: FileNotFoundException) {
            Log.e("ImageHelper", "File not found for URI: $uri", e)
        } catch (e: IOException) {
            Log.e("ImageHelper", "IO Exception for URI: $uri", e)
        } finally {
            try {
                inputStream?.close()
            } catch (e: IOException) {
                Log.e("ImageHelper", "Error closing input stream", e)
            }
        }
        return ImageSize(0, 0, 0)
    }

}