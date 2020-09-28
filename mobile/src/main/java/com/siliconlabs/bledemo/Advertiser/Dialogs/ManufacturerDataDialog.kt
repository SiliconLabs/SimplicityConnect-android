package com.siliconlabs.bledemo.Advertiser.Dialogs

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.siliconlabs.bledemo.Advertiser.Models.Manufacturer
import com.siliconlabs.bledemo.Advertiser.Utils.Converter
import com.siliconlabs.bledemo.Advertiser.Utils.Validator
import com.siliconlabs.bledemo.Base.BaseDialogFragment
import com.siliconlabs.bledemo.R
import kotlinx.android.synthetic.main.dialog_data_manufacturer.view.*
import java.lang.Exception

class ManufacturerDataDialog(private val manufacturers: List<Manufacturer>, val callback: Callback) : BaseDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_data_manufacturer, container, false).apply {

            btn_clear.setOnClickListener {
                et_company_identifier.setText("")
                et_data_in_hex_format.setText("")
            }

            btn_cancel.setOnClickListener {
                dismiss()
            }

            btn_save.setOnClickListener {
                handleSave(et_company_identifier, et_data_in_hex_format)
                dismiss()
            }

            verifyDataCorrectness(et_company_identifier, et_data_in_hex_format, btn_save, tv_id_already_exists)
            handleClickOnCompanyIdentifiers(btn_company_identifiers)

        }
    }

    private fun identifierCurrentlyExists(currentId: String): Boolean {
        try {
            val identifier = currentId.toInt(16)
            for (manufacturer in manufacturers) if (manufacturer.identifier == identifier) return true
            return false
        } catch (e: Exception) {
            return false
        }
    }

    private fun verifyDataCorrectness(etIdentifier: EditText, etData: EditText, btnSave: Button, tvNote: TextView) {
        etIdentifier.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val id = etIdentifier.text.toString()
                val data = etData.text.toString()
                tvNote.visibility = if(identifierCurrentlyExists(id)) View.VISIBLE else View.GONE
                btnSave.isEnabled = (Validator.isCompanyIdentifierValid(id) && Validator.isCompanyDataValid(data) && !identifierCurrentlyExists(id))
            }
        })

        etData.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val id = etIdentifier.text.toString()
                val data = etData.text.toString()
                tvNote.visibility = if(identifierCurrentlyExists(id)) View.VISIBLE else View.GONE
                btnSave.isEnabled = (Validator.isCompanyIdentifierValid(id) && Validator.isCompanyDataValid(data) && !identifierCurrentlyExists(id))
            }
        })
    }

    private fun handleClickOnCompanyIdentifiers(btn: Button) {
        btn.setOnClickListener {
            val uriUrl = Uri.parse("https://" + getString(R.string.advertiser_url_company_identifiers))
            val launchBrowser = Intent(Intent.ACTION_VIEW, uriUrl)
            startActivity(launchBrowser)
        }
    }

    private fun handleSave(etIdentifier: EditText, etData: EditText) {
        val identifier = etIdentifier.text.toString().toInt(16)
        val data = Converter.getHexStringAsByteArray(etData.text.toString())
        val manufacturer = Manufacturer(identifier, data)
        callback.onSave(manufacturer)
    }

    interface Callback {
        fun onSave(manufacturer: Manufacturer)
    }

}