package com.siliconlabs.bledemo.Browser.Utils

import com.siliconlabs.bledemo.Bluetooth.DataTypes.Bit
import com.siliconlabs.bledemo.Bluetooth.DataTypes.Characteristic
import com.siliconlabs.bledemo.Bluetooth.DataTypes.Enumeration
import com.siliconlabs.bledemo.Bluetooth.DataTypes.Field
import com.siliconlabs.bledemo.Bluetooth.Parsing.Consts
import com.siliconlabs.bledemo.Bluetooth.Parsing.Engine
import com.siliconlabs.bledemo.utils.Converters
import kotlin.collections.ArrayList

class FieldViewHelper(
        private val characteristic: Characteristic?
) {

    private var foundField = false

    fun isFieldPresent(field: Field, value: ByteArray) : Boolean {
        if (field.requirements.isEmpty() ||
                (field.requirements.size == 1 && field.requirements[0] == Consts.REQUIREMENT_MANDATORY) ) {
            return true
        } else {
            val requirementsMet = true
            field.requirements.forEach {
                for (bitField in getBitFields()) {
                    for (bit in bitField.bitfield?.bits!!) {
                        for (enumeration in bit.enumerations!!) {
                            if (enumeration.requires != null && it == enumeration.requires) {
                                if (!checkRequirement(value, bitField, enumeration, bit)) {
                                    return false
                                }
                            }
                        }
                    }
                }
            }
            return requirementsMet
        }
    }

    fun isFirstNibbleInByte(field: Field) : Boolean {
        return if (field.format == "nibble") {
            field.name == "Type"
        } else false
    }

    private fun getBitFields(): ArrayList<Field> {
        val bitFields = ArrayList<Field>()
        for (f in characteristic?.fields.orEmpty()) {
            bitFields.addAll(f.getBitField())
        }
        return bitFields
    }

    private fun checkRequirement(value: ByteArray, bitField: Field, enumeration: Enumeration, bit: Bit): Boolean {
        val formatLength = Engine.getFormat(bitField.format)
        val off = getFieldOffset(bitField, value)
        val bitFieldValue = Converters.calculateDecimalValue(
                value.copyOfRange(off, off + formatLength), false)
        val enumVal = readEnumInt(bit.index, bit.size, bitFieldValue)
        return enumVal == enumeration.key
    }

    fun getFieldOffset(searchField: Field, value: ByteArray): Int {
        foundField = false
        var off = 0

        characteristic?.fields?.forEach { field ->
            off += getOffset(field, searchField, value)
        }
        foundField = true
        return off
    }

    private fun getOffset(field: Field, searchField: Field, value: ByteArray): Int {
        var off = 0
        if (field === searchField) {
            foundField = true
            return off
        }
        if (!foundField && isFieldPresent(field, value)) {
            if (field.referenceFields?.size!! > 0) {
                for (subField in field.referenceFields!!) {
                    off += getOffset(subField, searchField, value)
                }
            } else {
                if (field.format != null) {
                    off += Engine.getFormat(field.format)
                }
            }
        }

        return off
    }

    private fun readEnumInt(index: Int, size: Int, tmpVal: Int): Int {
        var result = 0
        for (i in 0 until size) {
            result = result shl 8
            result = result or (tmpVal shr index + i and 0x1)
        }
        return result
    }

}