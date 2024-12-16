package com.siliconlabs.bledemo.features.configure.advertiser.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.siliconlabs.bledemo.features.configure.advertiser.utils.AdvertiserStorage
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.DialogInfoOkCancelBinding

//import kotlinx.android.synthetic.main.dialog_info_ok_cancel.view.*

class RemoveAdvertiserDialog(val callback: Callback) : BaseDialogFragment() {
    private lateinit var binding: DialogInfoOkCancelBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogInfoOkCancelBinding.inflate(inflater, container, false).apply {

            tvDialogTitle.text =
                requireContext().getString(R.string.advertiser_title_remove_advertiser)
            tvDialogContent.text =
                requireContext().getString(R.string.advertiser_note_remove_advertiser)

            btnOk.setOnClickListener {
                if (binding.cbDontShowAgain.isChecked) AdvertiserStorage(requireContext()).setShouldDisplayRemoveAdvertiserDialog(
                    false
                )
                callback.onOkClicked()
                dismiss()
            }
            btnCancel.setOnClickListener { dismiss() }
        }
        return binding.root
    }

    interface Callback {
        fun onOkClicked()
    }
}