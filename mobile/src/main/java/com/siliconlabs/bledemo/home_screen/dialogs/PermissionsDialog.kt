package com.siliconlabs.bledemo.home_screen.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.databinding.DialogPermissionsBinding

class PermissionsDialog(private val callback: Callback) : BaseDialogFragment(
        hasCustomWidth = true,
        isCanceledOnTouchOutside = false
) {

    private lateinit var _binding: DialogPermissionsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = DialogPermissionsBinding.inflate(inflater)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding.btnUnderstood.setOnClickListener {
            callback.onDismiss()
            dismiss()
        }
    }

    interface Callback {
        fun onDismiss()
    }
}