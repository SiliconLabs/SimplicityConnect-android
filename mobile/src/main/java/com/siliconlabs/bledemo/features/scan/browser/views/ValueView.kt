package com.siliconlabs.bledemo.features.scan.browser.views

import android.content.Context
import android.view.View
import android.widget.EditText
import com.siliconlabs.bledemo.bluetooth.data_types.Field

abstract class ValueView(
        protected val context: Context?,
        protected val fieldValue: ByteArray
) {

    abstract fun createViewForRead(isParsedSuccessfully: Boolean, viewHandler: ViewHandler)
    abstract fun createViewForWrite(fieldOffset: Int, valueListener: ValueListener)

    interface ViewHandler {
        fun handleFieldView(fieldViewContainer: View, fieldValueView: View)
        fun handleRawValueViews(rawValuesContainer: View, rawValueViews: ArrayList<EditText>)
    }

    interface ValueListener {
        fun handleFieldView(fieldViewContainer: View)
        fun addEditTexts(editTexts: ArrayList<EditText>)
        fun onValueChanged(field: Field, newValue: ByteArray, fieldOffset: Int)
        fun onRawValueChanged(newValue: ByteArray)
        fun onFieldsChanged()
        fun addInRangeCheck(pair: Pair<Field, Boolean>)
        fun addValidityCheck(pair: Pair<Field, Boolean>)

    }
}