package com.siliconlabs.bledemo.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import by.kirich1409.viewbindingdelegate.viewBinding
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.databinding.DialogInfoOkCancelBinding


abstract class RemovalDialog(
    @StringRes private val nameRes: Int,
    private val onOkClicked: () -> Unit
) : BaseDialogFragment() {
    //  private val binding by viewBinding(DialogInfoOkCancelBinding::bind)
    private lateinit var binding: DialogInfoOkCancelBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogInfoOkCancelBinding.inflate(inflater, container, false).apply {
            val name = context?.getString(nameRes)
            tvDialogTitle.text = context?.getString(R.string.dialog_title_remove, name)
            tvDialogContent.text =
                context?.getString(R.string.dialog_description_remove, name)

            btnOk.setOnClickListener {
                if (binding.cbDontShowAgain.isChecked) {
                    blockDisplayingRemovalDialog()
                }
                onOkClicked()
                dismiss()
            }
            btnCancel.setOnClickListener { dismiss() }
        }
        return binding.root
    }

    abstract fun blockDisplayingRemovalDialog()
}
