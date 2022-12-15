package com.siliconlabs.bledemo.features.scan.browser.dialogs

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.databinding.DialogPermissionMissingBinding

class PermissionMissingDialog: BaseDialogFragment(
        hasCustomWidth = true,
        isCanceledOnTouchOutside = true
) {

    private lateinit var _binding: DialogPermissionMissingBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = DialogPermissionMissingBinding.inflate(inflater)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUiListeners()
    }

    private fun setupUiListeners() {
        _binding.apply {
            btnSettings.setOnClickListener { context?.let { context ->
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .apply { data = Uri.fromParts("package", context.packageName, null) }
                        .also { context.startActivity(it) }
            } }
            btnOk.setOnClickListener { dismiss() }
        }
    }


}