package com.siliconlabs.bledemo.home_screen.dialogs

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import by.kirich1409.viewbindingdelegate.viewBinding
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.databinding.DialogPermissionsBinding

class PermissionsDialog(
    private val rationalesToShow: List<String>,
    private val callback: Callback
) : BaseDialogFragment(
        hasCustomWidth = true,
        isCanceledOnTouchOutside = false
) {

    private val binding by viewBinding(DialogPermissionsBinding::bind)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? = inflater.inflate(R.layout.dialog_permissions, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            rationaleText.text = Html.fromHtml(getString(R.string.permissions_rationale_html,
                generateRationalesMessage()), Html.FROM_HTML_MODE_LEGACY)
            btnUnderstood.setOnClickListener {
                callback.onDismiss()
                dismiss()
            }
        }
    }

    private fun generateRationalesMessage() : String {
        return StringBuilder().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isNearbyDeviceRationaleNeeded()) {
                append(getString(R.string.permissions_rationale_nearby_devices_html))
                append("<br>")
            }
            if (rationalesToShow.contains(Manifest.permission.ACCESS_FINE_LOCATION)) {
                append(getString(R.string.permissions_rationale_location_html))
                append("<br>")
            }
            if(rationalesToShow.contains(Manifest.permission.POST_NOTIFICATIONS)){
                append(getString(R.string.permissions_rationale_notification_html))
            }
        }.toString()
    }

    private fun isNearbyDeviceRationaleNeeded() : Boolean {
        /* All 3 Android 12 bluetooth permissions need only 1 runtime permission */
        return rationalesToShow.any {
            it.contains("BLUETOOTH", false)
        }
    }

    interface Callback {
        fun onDismiss()
    }
}