package com.siliconlabs.bledemo.features.demo.esl_demo.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.common.views.DetailsRow
import com.siliconlabs.bledemo.databinding.AdapterEslTagInfoBinding
import com.siliconlabs.bledemo.features.demo.esl_demo.model.TagInfo
import com.siliconlabs.bledemo.features.demo.esl_demo.viewmodels.EslDemoViewModel
import com.siliconlabs.bledemo.utils.RecyclerViewUtils

class TagInfoAdapter(val listener: Listener) : RecyclerView.Adapter<TagInfoAdapter.TagInfoViewHolder>() {

    private val tagInfoList: MutableList<EslDemoViewModel.TagViewInfo> = mutableListOf()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagInfoViewHolder {
        val binding = AdapterEslTagInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TagInfoViewHolder(binding, listener).apply {
            setupUiListeners()
        }
    }

    override fun getItemCount(): Int {
        return tagInfoList.size
    }

    override fun onBindViewHolder(holder: TagInfoViewHolder, position: Int) {
        holder.bind(tagInfoList[position])
    }

    fun showNewTag(tagViewInfo: EslDemoViewModel.TagViewInfo) {
        tagInfoList.add(tagViewInfo)
        notifyItemInserted(itemCount - 1)
    }

    fun toggleLedImage(index: Int, isLedOn: Boolean) {
        tagInfoList[index].tagInfo.isLedOn = isLedOn
        notifyItemChanged(index, Unit)
    }

    fun toggleAllLedImages(isGroupLedOn: Boolean) {
        tagInfoList.forEach { it.tagInfo.isLedOn = isGroupLedOn }
        notifyDataSetChanged()
    }

    fun clear() {
        val itemCount = tagInfoList.size
        tagInfoList.clear()
        notifyItemRangeRemoved(0, itemCount)
    }

    interface Listener {
        fun onExpandArrowClicked(index: Int, isViewExpanded: Boolean)
        fun onLedButtonClicked(index: Int)
        fun onUploadImageClicked(index: Int)
        fun onDisplayImageClicked(index: Int)
        fun onPingButtonClicked(index: Int)
    }


    inner class TagInfoViewHolder(
        private val _binding: AdapterEslTagInfoBinding,
        private val listener: Listener
    ) : RecyclerView.ViewHolder(_binding.root) {

        fun setupUiListeners() {
            _binding.apply {
                expandArrow.setOnClickListener { RecyclerViewUtils.withProperAdapterPosition(this@TagInfoViewHolder) {
                    val newState = !tagInfoList[it].isViewExpanded
                    tagInfoList[it].isViewExpanded = newState
                    toggleDetailsVisibility(newState)
                    listener.onExpandArrowClicked(it, newState)
                } }
                ibEslLight.setOnClickListener { RecyclerViewUtils.withProperAdapterPosition(this@TagInfoViewHolder) {
                    listener.onLedButtonClicked(it)
                } }
                ibEslUploadImage.setOnClickListener { RecyclerViewUtils.withProperAdapterPosition(this@TagInfoViewHolder) {
                    listener.onUploadImageClicked(it)
                } }
                ibEslDisplayImage.setOnClickListener { RecyclerViewUtils.withProperAdapterPosition(this@TagInfoViewHolder) {
                    listener.onDisplayImageClicked(it)
                } }
                pingTagButton.setOnClickListener { RecyclerViewUtils.withProperAdapterPosition(this@TagInfoViewHolder) {
                    listener.onPingButtonClicked(it)
                } }
            }

        }

        fun bind(tagViewInfo: EslDemoViewModel.TagViewInfo) {
            val context = itemView.context
            val tagInfo = tagViewInfo.tagInfo

            _binding.apply {
                bleAddress.text = tagInfo.bleAddress
                eslTagId.text = context.getString(R.string.esl_tag_id, tagInfo.eslId)
                ibEslLight.imageTintList = ColorStateList.valueOf(context.getColor(
                    if (tagInfo.isLedOn) R.color.esl_led_on
                    else R.color.esl_led_off
                ))

                prepareDetailRows(tagInfo)
                toggleDetailsVisibility(tagViewInfo.isViewExpanded)
            }
        }

        private fun toggleDetailsVisibility(shouldShowDetails: Boolean) {
            _binding.apply {
                expandArrow.setState(shouldShowDetails)
                eslTagDetails.visibility =
                    if (shouldShowDetails) View.VISIBLE
                    else View.GONE
            }
        }

        private fun prepareDetailRows(tagInfo: TagInfo) {
            _binding.eslTagDetails.apply {
                removeAllViews()
                addView(DetailsRow(
                    context,
                    context.getString(R.string.esl_tag_max_image_index),
                    tagInfo.maxImageIndex.toString(),
                ))

                addView(DetailsRow(
                    context,
                    context.getString(R.string.esl_tag_displays_count),
                    tagInfo.displayScreensNumber.toString(),
                ))

                addView(DetailsRow(
                    context,
                    context.getString(R.string.esl_tag_sensors_data),
                    buildSensorInfo(tagInfo.sensorInfo),
                ))
            }
        }

        private fun buildSensorInfo(sensorData: List<Int>): String = buildString {
            sensorData.forEachIndexed { index, data ->
                findSensor(data)?.let { description ->
                    val hexData = data.toString(radix = 16).padStart(SENSOR_HEX_LEN, '0')
                    append("0x$hexData")
                    append(" - ")
                    append(description)
                    if (index != sensorData.lastIndex) append("\n")
                }
            }
        }

        private fun findSensor(data: Int) : String? {
            return when (data) {
                BATTERY_SENSOR_PRESENT -> itemView.context.getString(R.string.battery_sensor_present)
                TEMPERATURE_SENSOR_PRESENT -> itemView.context.getString(R.string.temperature_sensor_present)
                else -> null
            }
        }
    }

    companion object {
        private const val BATTERY_SENSOR_PRESENT = 0x59
        private const val TEMPERATURE_SENSOR_PRESENT = 0x54

        private const val SENSOR_HEX_LEN = 4
    }
}
