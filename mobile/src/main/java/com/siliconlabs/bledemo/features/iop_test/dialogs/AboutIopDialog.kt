package com.siliconlabs.bledemo.features.iop_test.dialogs

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.siliconlabs.bledemo.base.fragments.BaseDialogFragment
import com.siliconlabs.bledemo.R
import kotlinx.android.synthetic.main.dialog_info_iop.*

class AboutIopDialog : BaseDialogFragment(
        hasCustomWidth = true,
        isCanceledOnTouchOutside = true
) {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_info_iop, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleClickEvents()
    }

    private fun handleClickEvents() {
        btn_ok.setOnClickListener { dismiss() }

        btn_interoperability_test.setOnClickListener {
            handleClickOnInteroperabilityTest()
        }
    }


    private fun handleClickOnInteroperabilityTest() {
        val url = Uri.parse(IOP_LINK)
        val launchBrowser = Intent(Intent.ACTION_VIEW, url)
        startActivity(launchBrowser)
    }

    companion object {
        private const val IOP_LINK = "https://www.silabs.com/documents/public/application-notes/an1346-running-ble-iop-test.pdf"
    }
}