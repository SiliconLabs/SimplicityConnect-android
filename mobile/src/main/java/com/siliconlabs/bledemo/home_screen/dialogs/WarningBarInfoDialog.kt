package com.siliconlabs.bledemo.home_screen.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.R
import kotlinx.android.synthetic.main.dialog_location_info.*

class WarningBarInfoDialog(private val type: Type) : BaseDialogFragment(
        hasCustomWidth = true,
        isCanceledOnTouchOutside = true
) {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_location_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btn_ok.setOnClickListener { dismiss() }
        initTexts()
    }

    private fun initTexts() {
        info_dialog_title.text = getString( when(type) {
            Type.LOCATION -> R.string.Location_service
            Type.LOCATION_PERMISSION -> R.string.location_permission
            Type.BLUETOOTH_PERMISSIONS -> R.string.bluetooth_permissions
        })
        info_dialog_description.text = getString( when(type) {
            Type.LOCATION -> R.string.location_service_info
            Type.LOCATION_PERMISSION -> R.string.location_permission_info
            Type.BLUETOOTH_PERMISSIONS -> R.string.bluetooth_permissions_info
        })
    }

    enum class Type {
        LOCATION,
        LOCATION_PERMISSION,
        BLUETOOTH_PERMISSIONS
    }

}