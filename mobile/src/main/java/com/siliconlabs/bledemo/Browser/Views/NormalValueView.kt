package com.siliconlabs.bledemo.Browser.Views

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.common.math.IntMath.pow
import com.siliconlabs.bledemo.Advertiser.Utils.HtmlCompat
import com.siliconlabs.bledemo.Bluetooth.DataTypes.Field
import com.siliconlabs.bledemo.Bluetooth.Parsing.Common
import com.siliconlabs.bledemo.Bluetooth.Parsing.Engine
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.utils.Converters
import java.math.BigInteger
import java.util.*
import kotlin.math.*

class NormalValueView(context: Context?,
                      field: Field,
                      fieldValue: ByteArray
) : FieldView(context, field, fieldValue) {

    override fun createViewForRead(isParsedSuccessfully: Boolean, viewHandler: ViewHandler) {
        val readFieldContainer = LayoutInflater.from(context).inflate(
                R.layout.characteristic_field_read_mode, null, false)
        val fieldValueText = readFieldContainer.findViewById<TextView>(R.id.characteristic_field_value)
        val fieldNameText = readFieldContainer.findViewById<TextView>(R.id.characteristic_field_name)

        val valueUnit = getValueUnit()
        fieldNameText.text = field.name!!

        if (isParsedSuccessfully) {
            val currentValue = showValue()

            fieldValueText.text = StringBuilder().apply {
                append(currentValue)
                append(valueUnit)
            }.toString()
        }

        viewHandler.handleFieldView(readFieldContainer, fieldValueText)
    }

    override fun createViewForWrite(fieldOffset: Int, valueListener: ValueListener) {
        val writeFieldContainer = LayoutInflater.from(context).inflate(R.layout
                .characteristic_field_normal_value_write_mode, null, false)
        val fieldName = writeFieldContainer.findViewById<TextView>(R.id.characteristic_field_name)
        val valueEdit = writeFieldContainer.findViewById<EditText>(R.id.characteristic_field_value_edit)

        fieldName.text = field.name!!
        var newValue: ByteArray

        valueEdit.apply {
            setText(showValue())
            if (field.isNumberFormat()) {
                setRawInputType(
                        InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED)
            }
            addTextChangedListener(object : TextWatcher {
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    var currentInput = s.toString()

                    if (field.isStringFormat()) {
                        if (!isFieldSizeExceeded(currentInput.length)) {
                            newValue = ByteArray(fieldValue.size)
                            currentInput.toByteArray().copyInto(newValue, 0)
                            valueListener.onValueChanged(field, newValue, fieldOffset)
                        } else {
                            setText(text.toString().substring(0, text.length - 1))
                            setSelection(text.length)
                        }
                    } else {
                        currentInput = handleModifiers(currentInput)

                        val convertedInput = Converters.convertStringTo(currentInput, field.format)
                        valueListener.addInRangeCheck(Pair(field, convertedInput.second ?: false))
                        val validityCheck =
                                if (field.isNumberFormat()) Pair(field, isNumeric(currentInput))
                                else Pair(field, true)
                        valueListener.addValidityCheck(validityCheck)

                        newValue = convertedInput.first ?: ByteArray(fieldValue.size)

                        if (!isFieldSizeExceeded(newValue.size)) {
                            valueListener.onValueChanged(field, newValue, fieldOffset)
                        }
                    }
                }

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun afterTextChanged(s: Editable) {}
            })
        }

        valueListener.addEditTexts(arrayListOf(valueEdit))
        valueListener.handleFieldView(writeFieldContainer)
    }

    private fun isFieldSizeExceeded(inputLength: Int) : Boolean {
        return if (inputLength > fieldValue.size) {
            Toast.makeText(context, R.string.characteristic_dialog_field_size_exceeded, Toast.LENGTH_SHORT).show()
            true
        } else false
    }

    private fun showValue() : String {
        var currentValue = readValue()
        currentValue = if (field.isStringFormat()) {
            currentValue.replace("\u0000", "")
        } else {
            calculateModifiers(currentValue)
        }
        return currentValue
    }

    private fun getValueUnit(): String {
        val unit = Engine.getUnit(field.unit)
        if (unit != null) {
            return if (!TextUtils.isEmpty(unit.symbol)) {
                if (unit.fullName.toLowerCase(Locale.getDefault()) == "celsius"
                        || unit.fullName.toLowerCase(Locale.getDefault()) == "fahrenheit") {
                    // displays degrees symbol correctly
                    context!!.getString(R.string.unit_with_whitespace, HtmlCompat.fromHtml(unit.symbol))
                } else {
                    context!!.getString(R.string.unit_with_whitespace, unit.symbol)
                }
            } else {
                context!!.getString(R.string.unit_with_whitespace, unit.fullName)
            }
        }
        return ""
    }

    private fun calculateModifiers(currentValue: String) : String {
        var valueAsNumber = currentValue.toDouble()
        val exponentModifier = 10.0.pow(abs(field.decimalExponent.toInt()))

        valueAsNumber *= field.multiplier
        valueAsNumber =
                if (field.decimalExponent >= 0) valueAsNumber * exponentModifier
                else valueAsNumber / exponentModifier
        return valueAsNumber.toString()
    }

    private fun handleModifiers(currentInput: String) : String {
        return try {
            var currentInputAsValue = currentInput.toDouble()
            val exponentModifier = 10.0.pow(abs(field.decimalExponent.toInt()))

            currentInputAsValue /= field.multiplier
            currentInputAsValue =
                    if (field.decimalExponent >= 0) currentInputAsValue / exponentModifier
                    else currentInputAsValue * exponentModifier
            currentInputAsValue = round(currentInputAsValue * 1000000) / 1000000 // in case of inaccurate Double calculations

            var modifiedInput = currentInputAsValue.toString()
            if (isInputInteger(modifiedInput)) {
                modifiedInput = modifiedInput.substring(0, modifiedInput.indexOf('.'))
            }
            modifiedInput
        } catch (ex: NumberFormatException) {
            ex.printStackTrace()
            return ""
        }

    }

    private fun isInputInteger(input: String) : Boolean {
        return floor(input.toDouble()) == ceil(input.toDouble())
    }

    private fun isNumeric(string: String): Boolean {
        return try {
            string.toDouble()
            true
        } catch (e: NumberFormatException) {
            false
        }
    }

    private fun readValue(): String {
        if (fieldValue.isEmpty()) {
            return ""
        }
        val format = field.format!!
        val formatLength = Engine.getFormat(format)

        if (formatLength == 0) {
            return if (format.toLowerCase(Locale.getDefault()) == "reg-cert-data-list") {
                val result = StringBuilder(
                        "0x" + Converters.bytesToHexWhitespaceDelimited(fieldValue))
                StringBuilder(result.toString().replace(" ", "")).toString()
            } else {
                StringBuilder(String(fieldValue)).toString()
            }
        } else {
            return when {
                field.isFullByteSintFormat() -> convertSintToString()
                field.isFullByteUintFormat() -> convertUintToString(formatLength)
                field.isFloatFormat() -> {
                    val fValue = readFloat(format, formatLength)
                    StringBuilder(String.format(Locale.US, "%.1f", fValue)).toString()
                }
                else -> {
                    val result = StringBuilder()
                    for (element in fieldValue) {
                        result.append((element.toInt().and(0xff)))
                    }
                    result.toString()
                }
            }
        }
    }

    private fun convertSintToString(): String {
        val builder = StringBuilder()
        val reversedArray = fieldValue.reversedArray()
        for (i in reversedArray.indices) {
            if (reversedArray[i] < 0 ) {
                reversedArray[i] = (reversedArray[i] + 256).toByte()
            }
            builder.append(Converters.getHexValue(reversedArray[i]))
        }

        var result = builder.toString().toInt(16)
        if (result >= (pow(256, fieldValue.size) / 2)) {
            result -=pow(256, fieldValue.size)
        }

        return result.toString()
    }

    private fun convertUintToString(formatLength: Int): String {
        return try {
            if (formatLength < 9) {
                var uintAsLong = 0L
                for (i in 0 until formatLength) {
                    uintAsLong = uintAsLong shl 8
                    val byteAsInt: Int = fieldValue[formatLength - 1 - i].toInt().and(0xff)
                    uintAsLong = uintAsLong or byteAsInt.toLong()
                }
                uintAsLong.toString()
            } else { // uint128
                val binaryString = StringBuilder()
                for (element in fieldValue) {
                    binaryString.append(
                            String.format("%8s", Integer.toBinaryString(element.toInt().and(0xFF)))
                                    .replace(' ', '0')
                    )
                }
                BigInteger("0$binaryString", 2).toString(16)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            ""
        }
    }

    private fun readFloat(format: String, formatLength: Int): Double {
        var result = 0.0
        when (format) {
            TYPE_SFLOAT -> result = Common.readSfloat(fieldValue).toDouble()
            TYPE_FLOAT -> result = Common.readFloat(fieldValue, 0, formatLength - 1).toDouble()
            TYPE_FLOAT_32 -> result = Common.readFloat32(fieldValue).toDouble()
            TYPE_FLOAT_64 -> result = Common.readFloat64(fieldValue)
        }
        return result
    }

    companion object {
        private const val TYPE_FLOAT = "FLOAT"
        private const val TYPE_SFLOAT = "SFLOAT"
        private const val TYPE_FLOAT_32 = "float32"
        private const val TYPE_FLOAT_64 = "float64"
    }

}