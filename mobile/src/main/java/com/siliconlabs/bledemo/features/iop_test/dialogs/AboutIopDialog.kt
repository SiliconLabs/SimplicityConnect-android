package com.siliconlabs.bledemo.features.iop_test.dialogs

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.DialogInfoIopBinding


class AboutIopDialog : BaseDialogFragment(
    hasCustomWidth = true,
    isCanceledOnTouchOutside = true
) {
    private lateinit var binding: DialogInfoIopBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogInfoIopBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleClickEvents()
    }

    private fun handleClickEvents() {
        binding.btnOk.setOnClickListener { dismiss() }

        binding.btnInteroperabilityTest.setOnClickListener {
            handleClickOnInteroperabilityTest()
        }
    }


    private fun handleClickOnInteroperabilityTest() {
        val url = Uri.parse(IOP_LINK)
        val launchBrowser = Intent(Intent.ACTION_VIEW, url)
        startActivity(launchBrowser)
    }

    companion object {
        private const val IOP_LINK =
            "https://www.silabs.com/documents/public/application-notes/an1346-running-ble-iop-test.pdf"
    }
}