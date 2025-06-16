package com.siliconlabs.bledemo.features.demo.matter_demo.adapters

import android.content.SharedPreferences
import android.text.BoringLayout
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.COLOR_TEMPERATURE_LIGHT_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.DIMMABLE_Light_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.DIMMER_SWITCH
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.ENHANCED_COLOR_LIGHT_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterScannedResultFragment.Companion.ON_OFF_LIGHT_TYPE
import com.siliconlabs.bledemo.features.demo.matter_demo.model.MatterScannedResultModel
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.SharedPrefsUtils
import com.siliconlabs.bledemo.utils.BLEUtils

class MatterLightSwitchRVAdapter(
    private var deviceList:ArrayList<MatterScannedResultModel>,
    private  var prefs: SharedPreferences,
    private val onItemClick: (MatterScannedResultModel) -> Unit,
    private val onUnbindClick: (MatterScannedResultModel) -> Unit
) : RecyclerView.Adapter<MatterLightSwitchRVAdapter.DeviceViewHolder>()
{

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.iconLight)
        val name: TextView = itemView.findViewById(R.id.deviceName)
        val checkMark: ImageView = itemView.findViewById(R.id.checkmark)
        val unBindButton: Button = itemView.findViewById(R.id.btn_unbind)
        val progressBar:ProgressBar = itemView.findViewById(R.id.progressBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_light_switch_controller, parent, false)
        return DeviceViewHolder(view)
    }

    fun updateList(newList: List<MatterScannedResultModel>) {
        deviceList = newList as ArrayList<MatterScannedResultModel>
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = deviceList[position]
        if (device.deviceType == DIMMER_SWITCH) {
            holder.itemView.visibility = View.GONE
            return // Exit early since this view should be hidden
        }
        // Proceed only if it's a valid device type
        if (isValidDeviceType(device.deviceType)) {
            holder.itemView.visibility = View.VISIBLE // Ensure the item is visible
            holder.name.text = device.matterName
            // Set other views' data here if needed
            holder.checkMark.visibility =
                if (device.isBindingSuccessful && !device.isUnbindingInProgress)
                    View.VISIBLE
                else View.GONE
            /*holder.progressBar.visibility =
                if (device.isBindingInProgress || device.isAclWriteInProgress || device.isUnbindingInProgress) View.VISIBLE else View.GONE*/
            //holder.unBindButton.visibility = if (device.isBindingSuccessful) View.VISIBLE else View.GONE
            holder.unBindButton.isEnabled = (device.matterName == unbindEnabledFor)
            holder.unBindButton.visibility =
                if (device.isBindingSuccessful && !device.isUnbindingInProgress)
                    View.VISIBLE
                else View.GONE
            if(holder.unBindButton.isEnabled){
                Log.e("MATTER_ADAPTER","::${holder.unBindButton.isEnabled}")
            }else{
                Log.e("MATTER_ADAPTER","::${holder.unBindButton.isEnabled}")

            }
            holder.unBindButton.setOnClickListener {
                onUnbindClick(device)
            }
            holder.itemView.setOnClickListener {
                if (device.isBindingInProgress || device.isAclWriteInProgress) return@setOnClickListener
                onItemClick(device)
            }
        } else {
            // Handle cases where the device type is not valid. This could be to hide or show another view
            holder.itemView.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = deviceList.size


   private fun isValidDeviceType(deviceType: Int): Boolean {
       return when (deviceType) {
           DIMMABLE_Light_TYPE,
           ENHANCED_COLOR_LIGHT_TYPE,
           ON_OFF_LIGHT_TYPE,
           COLOR_TEMPERATURE_LIGHT_TYPE -> true
           else -> false
       }
   }

    private var unbindEnabledFor: String? = null

    fun setUnbindEnabledFor(name: String?) {
        unbindEnabledFor = name
        notifyDataSetChanged()
    }
}