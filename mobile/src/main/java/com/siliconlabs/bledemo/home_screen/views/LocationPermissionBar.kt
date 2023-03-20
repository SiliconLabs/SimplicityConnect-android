package com.siliconlabs.bledemo.home_screen.views

import android.content.Context
import android.util.AttributeSet
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.home_screen.dialogs.WarningBarInfoDialog

class LocationPermissionBar(context: Context, attrs: AttributeSet?) : NoServiceWarningBar(context, attrs) {

    override fun initTexts() {
        _binding.apply { with(context) {
            warningBarMessage.text = getString(R.string.location_permission_denied)
            warningBarActionButton.text = getString(R.string.action_settings)
            warningBarInfoButton.text = getString(R.string.warning_bar_additional_info)
        } }
    }

    override fun initClickListeners() {
        _binding.apply {
            warningBarActionButton.setOnClickListener {
                showAppSettingsScreen()
            }
            warningBarInfoButton.setOnClickListener {
                showInfoDialog(WarningBarInfoDialog.Type.LOCATION_PERMISSION)
            }
        }
    }

}