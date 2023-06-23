package com.siliconlabs.bledemo.features.demo.esl_demo.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import by.kirich1409.viewbindingdelegate.viewBinding
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.databinding.DialogEslPromptBinding
import com.siliconlabs.bledemo.features.demo.esl_demo.viewmodels.EslDemoViewModel

class EslPromptDialog(
    private val dialogQuery: EslDemoViewModel.DialogQuery,
    private val callback: EslDialogCallback
) : BaseDialogFragment(
    hasCustomWidth = true,
    isCanceledOnTouchOutside = false
) {
    private val binding by viewBinding(DialogEslPromptBinding::bind)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.dialog_esl_prompt, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDialogTexts()
        setupUiListeners()
    }

    private fun initDialogTexts() {
        binding.apply {
            eslPromptTitle.text = getDialogTitle()
            eslPromptMessage.text = getDialogMessage()
            btnCancel.text = getNegativeButtonText()
            btnConfigure.text = getPositiveButtonText()
        }
    }

    private fun setupUiListeners() {
        binding.apply {
            btnCancel.setOnClickListener {
                callback.handleNegativeDialogAction(dialogQuery)
                dismiss()
            }
            btnConfigure.setOnClickListener {
                callback.handlePositiveDialogAction(dialogQuery)
                dismiss()
            }
        }
    }

    private fun getDialogTitle() : String? {
        return context?.getString( when (dialogQuery) {
            EslDemoViewModel.DialogQuery.CONFIGURE_TAG -> R.string.dialog_configure_tag_title
        })
    }

    private fun getDialogMessage() : String? {
        return context?.getString( when (dialogQuery) {
            EslDemoViewModel.DialogQuery.CONFIGURE_TAG -> R.string.dialog_configure_tag_message
        })
    }

    private fun getNegativeButtonText() : String? {
        return context?.getString( when (dialogQuery) {
            EslDemoViewModel.DialogQuery.CONFIGURE_TAG -> R.string.button_cancel
        })
    }

    private fun getPositiveButtonText() : String? {
        return context?.getString( when (dialogQuery) {
            EslDemoViewModel.DialogQuery.CONFIGURE_TAG -> R.string.dialog_configure_btn_do_configure
        })
    }

    interface EslDialogCallback {
        fun handlePositiveDialogAction(dialogQuery: EslDemoViewModel.DialogQuery)
        fun handleNegativeDialogAction(dialogQuery: EslDemoViewModel.DialogQuery)
    }
}