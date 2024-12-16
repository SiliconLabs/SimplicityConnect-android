package com.siliconlabs.bledemo.features.scan.browser.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.DialogOtaMissingCharacteristicBinding


class OtaCharacteristicMissingDialog : BaseDialogFragment(
    hasCustomWidth = true,
    isCanceledOnTouchOutside = true
) {

    private lateinit var binding: DialogOtaMissingCharacteristicBinding//dialog_ota_missing_characteristic
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogOtaMissingCharacteristicBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonOk.setOnClickListener { dismiss() }
    }

}