package com.siliconlabs.bledemo.features.scan.browser.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.DialogLoadingBinding

class OtaLoadingDialog(
        private val currentMessage: String,
        private val header: String? = null
) : BaseDialogFragment(
        hasCustomWidth = true,
        isCanceledOnTouchOutside = false
) {

    private var _binding: DialogLoadingBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = DialogLoadingBinding.inflate(inflater)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateMessage(currentMessage)
        _binding?.loadingHeader?.text = header ?: getString(R.string.preparing_for_upload)
    }

    fun updateMessage(message: String) {
        _binding?.loadingLog?.text = message
    }

    fun toggleLoadingSpinner(isVisible: Boolean) {
        _binding?.connectingSpinner?.visibility =
                if (isVisible) View.VISIBLE
                else View.GONE
    }

}