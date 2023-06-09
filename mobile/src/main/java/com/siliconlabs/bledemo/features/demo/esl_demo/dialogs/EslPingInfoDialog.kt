package com.siliconlabs.bledemo.features.demo.esl_demo.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import by.kirich1409.viewbindingdelegate.viewBinding
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.DialogEslPingInfoBinding
import com.siliconlabs.bledemo.features.demo.esl_demo.model.PingInfo
import com.siliconlabs.bledemo.utils.addIf

class EslPingInfoDialog(private val pingInfo: PingInfo)
    : DialogFragment(R.layout.dialog_esl_ping_info) {

    private val binding by viewBinding(DialogEslPingInfoBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showBasicStateText()
        setOkButtonListener()
        dialog?.apply {
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCanceledOnTouchOutside(true)
        }
    }

    private fun showBasicStateText() {
        binding.pingMessage.text = getBasicStateText()
    }

    private fun getBasicStateText() = buildList {
        addIf(pingInfo.serviceNeeded) { getString(R.string.ping_dialog_service_needed) }
        addIf(pingInfo.synchronized) { getString(R.string.ping_dialog_synchronized) }
        addIf(pingInfo.activeLed) { getString(R.string.ping_dialog_active_led, pingInfo.ledIndex) }
        addIf(pingInfo.pendingLedUpdate) { getString(R.string.ping_dialog_pending_led_update) }
        addIf(pingInfo.pendingDisplayUpdate) { getString(R.string.ping_dialog_pending_display_update) }

        addIf(isEmpty()) { getString(R.string.ping_dialog_reserved_for_future_use) }
    }.joinToString(NEWLINE_SEPARATOR)

    private fun setOkButtonListener() = binding.okButton.setOnClickListener { dismiss() }

    companion object {
        private const val NEWLINE_SEPARATOR = "\n"
    }
}