package com.siliconlabs.bledemo.features.demo.esl_demo.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import by.kirich1409.viewbindingdelegate.viewBinding
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.DialogEslLoadingBinding
import com.siliconlabs.bledemo.features.demo.esl_demo.model.EslCommand

class EslLoadingDialog(private val command: EslCommand, private val customText: String? = null)
    : DialogFragment(R.layout.dialog_esl_loading) {

    private val binding by viewBinding(DialogEslLoadingBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setText(command, customText)
        dialog?.apply {
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCanceledOnTouchOutside(false)
            isCancelable = false
        }
    }

    fun setText(command: EslCommand, customText: String? = null) {
        binding.dialogMessage.text = customText ?: getTextFromCommand(command)
    }

    private fun getTextFromCommand(command: EslCommand) = when (command) {
        EslCommand.CONNECT -> R.string.loading_dialog_connect
        EslCommand.CONFIGURE -> R.string.loading_dialog_configure
        EslCommand.DISCONNECT -> R.string.loading_dialog_disconnect
        EslCommand.LOAD_INFO -> R.string.loading_dialog_load_tags_info
        EslCommand.PING -> R.string.loading_dialog_ping_tag
        else -> R.string.unknown_state
    }.let { getString(it) }

    companion object {
        const val FRAGMENT_NAME = "loading_dialog"
    }
}