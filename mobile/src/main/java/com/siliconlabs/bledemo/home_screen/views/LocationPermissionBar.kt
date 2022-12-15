package com.siliconlabs.bledemo.home_screen.views

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.siliconlabs.bledemo.databinding.LocationPermissionBarBinding

class LocationPermissionBar(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private val _binding = LocationPermissionBarBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        _binding.btnSettings.setOnClickListener {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .apply { data = Uri.fromParts("package", context.packageName, null) }
                    .also { context.startActivity(it) }
        }
    }

}