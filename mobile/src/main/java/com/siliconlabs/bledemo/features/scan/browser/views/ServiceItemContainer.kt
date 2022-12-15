package com.siliconlabs.bledemo.features.scan.browser.views

import android.bluetooth.BluetoothGattService
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.LinearLayout
import com.siliconlabs.bledemo.bluetooth.parsing.Common
import com.siliconlabs.bledemo.bluetooth.parsing.Engine
import com.siliconlabs.bledemo.features.scan.browser.models.Mapping
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.ServiceContainerBinding
import com.siliconlabs.bledemo.utils.UuidUtils
import java.util.*

class ServiceItemContainer(
        context: Context,
        private val callback: Callback,
        val service: BluetoothGattService,
        private val isMandatorySystemService: Boolean,
        private val serviceDictionaryUuids: Map<String, Mapping>
) : LinearLayout(context) {

    private var nameType = NameType.UNKNOWN
    private val _binding: ServiceContainerBinding = ServiceContainerBinding.inflate(LayoutInflater.from(context), this, true)

    companion object {
        private const val ANIMATION_DURATION_FOR_EXPAND_AND_COLLAPSE = 333L
    }

    init {
        initViews()
        setupUiListeners()
    }

    private fun initViews() {
        _binding.apply {
            serviceCharacteristicsContainer.apply {
                visibility = View.GONE
                removeAllViews()
            }
            serviceTitle.text = getServiceName()
            tvRenameService.visibility = when (nameType) {
                NameType.ENGINE, NameType.CUSTOM -> View.GONE
                NameType.USER, NameType.UNKNOWN -> View.VISIBLE
            }
            serviceUuid.text = UuidUtils.getUuidText(service.uuid)


            if (service.characteristics.isEmpty()) {
                serviceInfoCardView.setBackgroundColor(Color.LTGRAY)
            }
        }
    }

    private fun setupUiListeners() {
        _binding.apply {
            tvRenameService.setOnClickListener { callback.onRenameClicked(this@ServiceItemContainer) }
            expandArrow.setOnClickListener {
                if (serviceCharacteristicsContainer.visibility == View.VISIBLE) {
                    tvMoreInfo.text = resources.getString(R.string.more_info)
                    serviceCharacteristicsContainer.visibility = View.GONE
                    expandArrow.setState(shouldShowDetails = false)
                } else {
                    tvMoreInfo.text = resources.getString(R.string.Less_Info)
                    expandArrow.setState(shouldShowDetails = true)
                    animateCharacteristicExpansion()
                }
            }
        }
    }

    fun getServiceName() : String {
        return Engine.getService(service.uuid)?.let { serv ->
            nameType = NameType.ENGINE

            if (isMandatorySystemService) markAsSystemService(serv.name)
            else serv.name
        } ?: run {
            Common.getCustomServiceName(service.uuid, context)?.let { name ->
                nameType = NameType.CUSTOM
                name
            }
        } ?: run {
            serviceDictionaryUuids[UuidUtils.getUuidText(service.uuid)]?.let { mapping ->
                nameType = NameType.USER
                mapping.name
            }
        } ?: run {
            nameType = NameType.UNKNOWN
            context.getString(R.string.unknown_service)
        }
    }

    fun setServiceName(newName: String) {
        nameType = NameType.USER
        _binding.serviceTitle.text = newName
    }

    private fun animateCharacteristicExpansion() {
        val container = _binding.serviceCharacteristicsContainer.apply {
            measure(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)
            layoutParams?.height = 1 // smooths animation
            visibility = View.VISIBLE
        }
        val targetHeight = container.measuredHeight

        val animation: Animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                container.layoutParams?.height =
                        if (interpolatedTime == 1f) ViewGroup.LayoutParams.WRAP_CONTENT
                        else (targetHeight * interpolatedTime).toInt()
                container.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }
        animation.duration = ANIMATION_DURATION_FOR_EXPAND_AND_COLLAPSE
        container.startAnimation(animation)
    }

    fun addCharacteristicContainer(container: CharacteristicItemContainer) {
        _binding.serviceCharacteristicsContainer.addView(container)
    }

    private fun markAsSystemService(name: String?) : String? {
        return name?.let {
            StringBuilder().apply {
                append(name)
                append(" (System)")
            }.toString()
        }
    }

    fun setMargins(position: Int) {
        val verticalSpacing = context.resources.getDimensionPixelSize(
                R.dimen.recycler_view_card_view_vertical_separation)
        val horizontalMargin = context.resources.getDimensionPixelSize(
                R.dimen.recycler_view_card_view_horizontal_margin)

        _binding.serviceInfoCardView.layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        ).apply {
            when (position) {
                0 -> setMargins(horizontalMargin, verticalSpacing, horizontalMargin, verticalSpacing)
                else -> setMargins(horizontalMargin, 0, horizontalMargin, verticalSpacing)
            }
        }
    }

    interface Callback {
        fun onRenameClicked(container: ServiceItemContainer)
    }

    enum class NameType {
        ENGINE,
        CUSTOM,
        USER,
        UNKNOWN
    }

}
