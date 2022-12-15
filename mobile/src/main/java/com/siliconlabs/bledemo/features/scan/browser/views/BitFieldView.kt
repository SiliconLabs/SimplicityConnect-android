package com.siliconlabs.bledemo.features.scan.browser.views

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.siliconlabs.bledemo.bluetooth.data_types.Field
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.CharacteristicFieldBitfieldWriteModeBinding
import com.siliconlabs.bledemo.databinding.CharacteristicFieldReadModeBinding

class BitFieldView(context: Context?,
                   field: Field,
                   fieldValue: ByteArray
) : FieldView(context, field, fieldValue) {

    private val bitfieldBuilder = fillStringBuilderWithZeros(field.getSizeInBytes() * 8)

    override fun createViewForRead(isParsedSuccessfully: Boolean, viewHandler: ViewHandler) {
        val fieldBits = getFieldValueAsLsoMsoBitsString()

        for (bit in field.bitfield?.bits!!) {
            val enumerations = ArrayList<String>()
            for (enumeration in bit.enumerations!!) {
                enumerations.add(enumeration.value!!)
            }

            val chosenOption = getValueInStringBitsRange(
                    bit.index,
                    bit.index + bit.size,
                    fieldBits
            )

            CharacteristicFieldReadModeBinding.inflate(LayoutInflater.from(context)).apply {
                characteristicFieldName.text = bit.name
                if (isParsedSuccessfully) {
                    characteristicFieldValue.text = enumerations[chosenOption]
                }

                viewHandler.handleFieldView(root, characteristicFieldValue)
            }
        }
    }

    override fun createViewForWrite(fieldOffset: Int, valueListener: ValueListener) {
        val fieldValueInBits = getFieldValueAsLsoMsoBitsString()

        for (bit in field.bitfield?.bits!!) {
            val enumerations = ArrayList<String>()
            for (enumeration in bit.enumerations!!) {
                enumerations.add(enumeration.value!!)
            }

            CharacteristicFieldBitfieldWriteModeBinding.inflate(LayoutInflater.from(context)).apply {
                val currentOption = getValueInStringBitsRange(
                        bit.index,
                        bit.index + bit.size,
                        fieldValueInBits
                )

                characteristicFieldName.text = bit.name
                characteristicFieldValueSpinner.apply {
                    adapter = ArrayAdapter(
                            context,
                            R.layout.enumeration_spinner_dropdown_item,
                            enumerations
                    )
                    setSelection(currentOption)
                    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                                parent: AdapterView<*>?,
                                view: View,
                                position: Int,
                                id: Long
                        ) {
                            // After each spinner selection bits are prepared for characteristic write - value array is updated with selected value
                            setStringBuilderBitsInRange(
                                    bitfieldBuilder,
                                    bit.index,
                                    bit.index + bit.size,
                                    position
                            )
                            val newValue = bitsStringToByteArray(bitfieldBuilder.toString(),
                                    field.getSizeInBytes())
                            valueListener.onValueChanged(field, newValue, fieldOffset)

                            if (bit.enumerations!![currentOption].requires !=
                                    bit.enumerations!![position].requires) {
                                valueListener.onFieldsChanged()
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
                }
                valueListener.handleFieldView(root)
            }
        }
    }

    private fun getFieldValueAsLsoMsoBitsString(): String {
        val result = StringBuilder()

        fieldValue.forEach {
            var byteValue = it.toInt()
            for (j in 0..7) {
                if (byteValue and 1 == 1) {
                    result.append(1)
                } else {
                    result.append(0)
                }
                byteValue = byteValue shr 1
            }
        }
        return result.toString()
    }

    // Get value from bitsString in range start (inclusive) -> end (exclusive)
    // bits String order must be:
    // least significant (index 0), to most significant (index last)
    private fun getValueInStringBitsRange(start: Int, end: Int, bits: String): Int {
        var startTmp = start
        var result = 0
        while (startTmp < end) {
            result = if (bits[startTmp] == '1') {
                result or 1
            } else {
                result or 0
            }
            if (startTmp + 1 < end) {
                result = result shl 1
            }
            startTmp++
        }
        return result
    }

    private fun fillStringBuilderWithZeros(count: Int): StringBuilder {
        return StringBuilder().apply {
            for (i in 0 until count) append('0')
        }
    }

    private fun setStringBuilderBitsInRange(builder: StringBuilder, startBit: Int, endBit: Int, value: Int) {
        var tmpEndBit = endBit
        var tmpValue = value
        while (startBit < tmpEndBit) {
            val bitValue = if (tmpValue and 1 == 1) '1' else '0'
            builder.setCharAt(tmpEndBit - 1, bitValue)
            tmpValue = tmpValue shr 1
            tmpEndBit--
        }
    }

    // Convert String of bits where bitsString charAt(0) is least significant to byte array
    private fun bitsStringToByteArray(bitsString: String, length: Int): ByteArray {
        val arr = ByteArray(length)
        for (i in 0 until length) {
            var tmp = 0
            for (j in 8 * (i + 1) - 1 downTo i * 8) {
                val bitChar = bitsString[j]
                tmp = if (bitChar == '1') {
                    tmp or 1
                } else {
                    tmp or 0
                }
                if (j > i * 8) {
                    tmp = tmp shl 1
                }
            }
            arr[i] = tmp.toByte()
        }
        return arr
    }



}