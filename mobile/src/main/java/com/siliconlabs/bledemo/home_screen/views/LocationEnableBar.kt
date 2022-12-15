package com.siliconlabs.bledemo.home_screen.views

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.fragment.app.FragmentManager
import com.siliconlabs.bledemo.home_screen.dialogs.LocationInfoDialog
import com.siliconlabs.bledemo.databinding.LocationEnableBarBinding

class LocationEnableBar(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    private val viewBinding = LocationEnableBarBinding.inflate(LayoutInflater.from(context), this, true)
    private var fragmentManager: FragmentManager? = null

    init {
        viewBinding.apply {
            enableLocation.setOnClickListener { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
            locationInfo.setOnClickListener {
                fragmentManager?.let { LocationInfoDialog().show(it, "location_info_dialog") }
            }
        }

    }

    fun setLocationInfoFragmentManager(fragmentManager: FragmentManager) {
        this.fragmentManager = fragmentManager
    }

}