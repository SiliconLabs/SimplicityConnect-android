package com.siliconlabs.bledemo.features.demo.matter_demo.viewholder

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.MatterScannedListItemBinding
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.CONTACT_SENSOR_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.ENHANCED_COLOR_LIGHT_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.LIGHTNING_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.LOCK_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.OCCUPANCY_SENSOR_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.ONOFF_LIGHT_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.PLUG_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.TEMPERATURE_COLOR_LIGHT_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.TEMPERATURE_SENSOR_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.THERMOSTAT_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.WINDOW_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.model.MatterScannedResultModel

class MatterItemViewModel(
    val binding: MatterScannedListItemBinding,
    val context: Context
) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(get: MatterScannedResultModel) {
        binding.textViewHeader.text = get.matterName

        when (get.deviceType) {
            LIGHTNING_TYPE, ENHANCED_COLOR_LIGHT_TYPE, ONOFF_LIGHT_TYPE, TEMPERATURE_COLOR_LIGHT_TYPE -> binding.imageview.setImageResource(R.drawable.matter_light_list)
            THERMOSTAT_TYPE -> binding.imageview.setImageResource(R.drawable.matter_thermostat)
            WINDOW_TYPE -> binding.imageview.setImageResource(R.drawable.matter_window_close)
            LOCK_TYPE -> binding.imageview.setImageResource(R.drawable.matter_door_lock)
            OCCUPANCY_SENSOR_TYPE -> binding.imageview.setImageResource(R.drawable.matter_occupancy_sensor_list)
            CONTACT_SENSOR_TYPE -> binding.imageview.setImageResource(R.drawable.matter_contact_sensor_list)
            TEMPERATURE_SENSOR_TYPE -> binding.imageview.setImageResource(R.drawable.matter_thermometer_list)
            PLUG_TYPE -> binding.imageview.setImageResource(R.drawable.matter_plug_off)

            else -> println("To Be Implemented...")
        }
    }
}
