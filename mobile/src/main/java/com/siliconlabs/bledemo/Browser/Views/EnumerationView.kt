package com.siliconlabs.bledemo.Browser.Views

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import com.siliconlabs.bledemo.Bluetooth.DataTypes.Field
import com.siliconlabs.bledemo.Bluetooth.Parsing.Engine
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.utils.Converters
import java.util.*
import kotlin.collections.ArrayList

class EnumerationView(
        context: Context?,
        field: Field,
        fieldValue: ByteArray
) : FieldView(context, field, fieldValue) {

    private val enumerations = findEnumerations()

    override fun createViewForRead(isParsedSuccessfully: Boolean, viewHandler: ViewHandler) {
        val readEnumerationContainer = LayoutInflater.from(context).inflate(
                R.layout.characteristic_field_read_mode, null, false)
        val valueTextView = readEnumerationContainer.findViewById<TextView>(R.id.characteristic_field_value)
        val nameTextView = readEnumerationContainer.findViewById<TextView>(R.id.characteristic_field_name)

        val fieldValueAsNumber = calculateFieldValue()
        val position = findPosition(fieldValueAsNumber)

        nameTextView.text = field.name!!
        if (isParsedSuccessfully) {
            valueTextView.text = enumerations[position]
        }
        viewHandler.handleFieldView(readEnumerationContainer, valueTextView)
    }

    override fun createViewForWrite(fieldOffset: Int, valueListener: ValueListener) {
        val writeEnumerationContainer = LayoutInflater.from(context).inflate(
                R.layout.characteristic_field_bitfield_write_mode, null, false)
        val nameTextViewWrite = writeEnumerationContainer.findViewById<TextView>(R.id.characteristic_field_name)
        val spinnerView = writeEnumerationContainer.findViewById<Spinner>(R.id.characteristic_field_value_spinner)

        val fieldValueAsNumber = calculateFieldValue()
        val position = findPosition(fieldValueAsNumber)

        nameTextViewWrite.text = field.name!!
        spinnerView.apply {
            adapter = ArrayAdapter(
                    context,
                    R.layout.enumeration_spinner_dropdown_item,
                    enumerations)
            setSelection(position)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View,
                        position: Int,
                        id: Long
                ) {
                    val key = field.enumerations!![position].key
                    val tmpFormatLength = Engine.getFormat(field.format)
                    val tmpVal = Converters.intToByteArray(key, tmpFormatLength)
                    valueListener.onValueChanged(field, tmpVal, fieldOffset)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
        valueListener.handleFieldView(writeEnumerationContainer)
    }


    private fun findEnumerations() : ArrayList<String> {
        val enumerationArray = ArrayList<String>()
        for (en in field.enumerations!!) {
            enumerationArray.add(en.value!!)
        }
        return enumerationArray
    }

    private fun findPosition(fieldValueAsNumber: Int) : Int {
        val position = 0

        for (en in field.enumerations!!) {
            if (en.key == fieldValueAsNumber) {
                return en.key
            }
        }
        return position
    }


    private fun calculateFieldValue() : Int {
        var calculatedValue: Int

        if (field.format?.toLowerCase(Locale.getDefault()) == "16bit") {
            // for field "Category", last 6 bits of payload are used for sub categories
            if (field.name == "Category") {
                val byte1: Int = fieldValue[0].toInt().and(0xff)
                val byte2: Int = fieldValue[1].toInt().and(0xff)
                calculatedValue = byte2 shl 8 or byte1
                calculatedValue = 0xffc0 and calculatedValue
            } else {
                calculatedValue = fieldValue[0].toInt().and(0xff)
                calculatedValue = calculatedValue shl 8
                calculatedValue = calculatedValue or (fieldValue[1].toInt().and(0xff))
            }
        } else {
            calculatedValue = readInt()
        }

        return calculatedValue
    }

    private fun readInt(): Int {
        var tmpVal = 0
        fieldValue.forEach {
            tmpVal = tmpVal shl 8
            tmpVal = tmpVal or it.toInt()
        }
        return tmpVal
    }
}