package com.siliconlabs.bledemo.features.demo.matter_demo.viewholder

import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.MatterScannedListItemBinding
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.AIR_QUALITY_SENSOR_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.COLOR_DIMMER_SWITCH
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.CONTACT_SENSOR_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.DISHWASHER_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.ENHANCED_COLOR_LIGHT_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.DIMMABLE_Light_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.DOOR_LOCK_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.OCCUPANCY_SENSOR_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.ON_OFF_LIGHT_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.DIMMABLE_PLUG_IN_UNIT_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.COLOR_TEMPERATURE_LIGHT_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.DIMMER_SWITCH
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.GENERIC_SWITCH
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.ON_OFF_LIGHT_SWITCH
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.TEMPERATURE_SENSOR_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.THERMOSTAT_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.WINDOW_COVERING_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.model.MatterScannedResultModel

class MatterItemViewModel(
    val binding: MatterScannedListItemBinding,
    val context: Context
) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(get: MatterScannedResultModel) {
        binding.textViewHeader.text = get.matterName
        Log.e("MATTER_LIGHT_DEVICE_TYPE","${get.deviceType}")

        when (get.deviceType) {
            DIMMABLE_Light_TYPE, ENHANCED_COLOR_LIGHT_TYPE, ON_OFF_LIGHT_TYPE, COLOR_TEMPERATURE_LIGHT_TYPE -> binding.imageView.setImageResource(
                R.drawable.matter_light_list
            )

            THERMOSTAT_TYPE -> binding.imageView.setImageResource(R.drawable.matter_thermostat)
            WINDOW_COVERING_TYPE -> binding.imageView.setImageResource(R.drawable.matter_window_close)
            DOOR_LOCK_TYPE -> binding.imageView.setImageResource(R.drawable.matter_door_lock)
            OCCUPANCY_SENSOR_TYPE -> binding.imageView.setImageResource(R.drawable.matter_occupancy_sensor_list)
            CONTACT_SENSOR_TYPE -> binding.imageView.setImageResource(R.drawable.matter_contact_sensor_list)
            TEMPERATURE_SENSOR_TYPE -> binding.imageView.setImageResource(R.drawable.matter_thermometer_list)
            DIMMABLE_PLUG_IN_UNIT_TYPE -> binding.imageView.setImageResource(R.drawable.matter_plug_off)
            DISHWASHER_TYPE -> binding.imageView.setImageResource(R.drawable.matter_dishwasher_list)
            AIR_QUALITY_SENSOR_TYPE -> binding.imageView.setImageResource(R.drawable.matter_air_quality_sensor)
            GENERIC_SWITCH,  COLOR_DIMMER_SWITCH  ->{
                binding.imageView.setImageResource(R.drawable.matter_thermostat)
            }
            ON_OFF_LIGHT_SWITCH,DIMMER_SWITCH, -> binding.imageView.setImageResource(R.drawable.ic_matter_switch_on)

            else -> println("To Be Implemented...")
        }
    }
}
