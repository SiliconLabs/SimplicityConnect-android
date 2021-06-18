package com.siliconlabs.bledemo.browser.dialogs

import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import com.siliconlabs.bledemo.base.BaseDialogFragment
import com.siliconlabs.bledemo.browser.MappingCallback
import com.siliconlabs.bledemo.browser.models.Mapping
import com.siliconlabs.bledemo.browser.models.MappingType
import com.siliconlabs.bledemo.R
import kotlinx.android.synthetic.main.dialog_characteristic_service_edit.*

class MappingsEditDialog(private val name: String, private val UUID: String, private val callback: MappingCallback, private val type: MappingType) : BaseDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_characteristic_service_edit, container, false)
    }

    override fun onResume() {
        super.onResume()

        val wm = context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val size = Point()

        display.getSize(size)
        val width = (size.x * 0.75).toInt()
        dialog?.window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tv_uuid.text = UUID
        et_name_hint.setText(name)
        et_name_hint.setSelection(et_name_hint.text.length)

        if (type == MappingType.CHARACTERISTIC) tv_mapping_type.text = context?.resources?.getString(R.string.Change_characteristic_name)
        else tv_mapping_type.text = context?.resources?.getString(R.string.Change_service_name)

        btn_cancel.setOnClickListener { dismiss() }
        btn_save.setOnClickListener {
            val newCharacServiceName = et_name_hint.text.toString()
            if (!TextUtils.isEmpty(et_name_hint.text.toString())) {
                val mapping = Mapping(UUID, newCharacServiceName)
                callback.onNameChanged(mapping)
                dismiss()
            } else {
                Toast.makeText(context, context?.resources?.getString(R.string.Name_field_cannot_be_empty), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
