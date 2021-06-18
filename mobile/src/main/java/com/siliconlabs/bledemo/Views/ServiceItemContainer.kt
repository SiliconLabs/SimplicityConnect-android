package com.siliconlabs.bledemo.views

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.utils.BLEUtils.Notifications

class ServiceItemContainer(context: Context) : LinearLayout(context) {
    private var characteristicNotificationStates: MutableMap<String, Notifications> = HashMap()

    private val btnExpandToShowCharacteristics: Button
    val cvServiceInfo: CardView
    val tvServiceTitle: TextView
    val ivEditServiceName: ImageView
    val llServiceEditName: LinearLayout
    val tvServiceUuid: TextView
    val llGroupOfCharacteristicsForService: LinearLayout
    val llLastItemDivider: LinearLayout

    companion object {
        private const val ANIMATION_DURATION_FOR_EXPAND_AND_COLLAPSE = 333
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.list_item_debug_mode_service, this)

        cvServiceInfo = findViewById(R.id.service_info_card_view)
        btnExpandToShowCharacteristics = findViewById(R.id.btn_expand_to_show_characteristics)
        tvServiceTitle = findViewById(R.id.service_title)
        ivEditServiceName = findViewById(R.id.image_view_edit_service)
        llServiceEditName = findViewById(R.id.linear_layout_edit_service_name)
        tvServiceUuid = findViewById(R.id.sevice_uuid)
        llGroupOfCharacteristicsForService = findViewById(R.id.container_of_characteristics_for_service)
        llLastItemDivider = findViewById(R.id.last_item_divider)

        btnExpandToShowCharacteristics.setOnClickListener {
            if (llGroupOfCharacteristicsForService.visibility == View.VISIBLE) {
                btnExpandToShowCharacteristics.text = resources.getString(R.string.More_Info)
                llGroupOfCharacteristicsForService.visibility = View.GONE
            } else {
                btnExpandToShowCharacteristics.text = resources.getString(R.string.Less_Info)
                animateCharacteristicExpansion()
            }
        }
        cvServiceInfo.setOnClickListener {
            if (llGroupOfCharacteristicsForService.visibility == View.VISIBLE) {
                btnExpandToShowCharacteristics.text = resources.getString(R.string.More_Info)
                llGroupOfCharacteristicsForService.visibility = View.GONE
            } else {
                btnExpandToShowCharacteristics.text = resources.getString(R.string.Less_Info)
                animateCharacteristicExpansion()
            }
        }
    }

    private fun animateCharacteristicExpansion() {
        val characteristicsExpansion: View? = llGroupOfCharacteristicsForService
        characteristicsExpansion?.measure(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)
        val targetHeight = characteristicsExpansion?.measuredHeight

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        characteristicsExpansion?.layoutParams?.height = 1
        characteristicsExpansion?.visibility = View.VISIBLE
        val animation: Animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                characteristicsExpansion?.layoutParams?.height = if (interpolatedTime == 1f) ViewGroup.LayoutParams.WRAP_CONTENT else (targetHeight!! * interpolatedTime).toInt()
                characteristicsExpansion?.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }
        animation.duration = ANIMATION_DURATION_FOR_EXPAND_AND_COLLAPSE.toLong()
        characteristicsExpansion?.startAnimation(animation)
    }

    fun setCharacteristicNotificationState(characteristicName: String, state: Notifications) {
        characteristicNotificationStates[characteristicName] = state
    }

}