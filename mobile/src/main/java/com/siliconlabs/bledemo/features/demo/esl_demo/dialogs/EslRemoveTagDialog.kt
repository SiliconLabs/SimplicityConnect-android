package com.siliconlabs.bledemo.features.demo.esl_demo.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import by.kirich1409.viewbindingdelegate.viewBinding
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.DialogEslRemoveTagBinding

class EslRemoveTagDialog(
    private val onConfirmClicked: () -> Unit,
) : DialogFragment(R.layout.dialog_esl_remove_tag) {
    private val binding by viewBinding(DialogEslRemoveTagBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        setButtonListeners(onConfirmClicked)
    }

    private fun setButtonListeners(onConfirmClicked: () -> Unit) {
        binding.apply {
            eslRemoveConfirm.setOnClickListener {
                onConfirmClicked()
                dismiss()
            }

            eslRemoveCancel.setOnClickListener { dismiss() }
        }
    }
}