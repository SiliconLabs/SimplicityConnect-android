package com.siliconlabs.bledemo.Browser.Dialogs

import android.app.Dialog
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.siliconlabs.bledemo.Bluetooth.DataTypes.Characteristic
import com.siliconlabs.bledemo.Bluetooth.DataTypes.Field
import com.siliconlabs.bledemo.Bluetooth.Parsing.Common
import com.siliconlabs.bledemo.Browser.Fragment.FragmentCharacteristicDetail
import com.siliconlabs.bledemo.Browser.Sig.GlucoseManagement
import com.siliconlabs.bledemo.Browser.Utils.FieldViewHelper
import com.siliconlabs.bledemo.Browser.Views.*
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.utils.Converters
import com.siliconlabs.bledemo.utils.UuidUtils
import java.util.*
import kotlin.collections.ArrayList

class CharacteristicWriteDialog(
        context: Context,
        private val listener: WriteDialogListener,
        private val writeType: FragmentCharacteristicDetail.WriteType,
        private val characteristic: Characteristic?,
        private var value: ByteArray
) : Dialog(context) {

    private lateinit var serviceNameTextView: TextView
    private lateinit var characteristicNameTextView: TextView
    private lateinit var uuidTextView: TextView

    private lateinit var writableFieldsContainer: LinearLayout
    private lateinit var writeMethodRadioGroup: RadioGroup
    private lateinit var writeWithResponseMethod: RadioButton
    private lateinit var writeWithoutResponseMethod: RadioButton

    private lateinit var btnSave: Button
    private lateinit var btnClear: Button
    private lateinit var ivClose: ImageView

    var fieldViewHelper = FieldViewHelper(characteristic)

    val fieldsValidMap = mutableMapOf<Field, Boolean>()
    val fieldsInRangeMap = mutableMapOf<Field, Boolean>()

    private val editableFields = ArrayList<EditText>()

    private var isRawValue = false

    var offset = 0

    private fun setWindow() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_characteristic_write)
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        val width = (context.resources.displayMetrics.widthPixels * 0.9).toInt()
        window?.setLayout(width, LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    private fun findViews() {
        serviceNameTextView = findViewById(R.id.characteristic_dialog_service_name)
        characteristicNameTextView = findViewById(R.id.characteristic_dialog_characteristic_name)
        uuidTextView = findViewById(R.id.characteristic_dialog_characteristic_uuid)

        writableFieldsContainer = findViewById(R.id.characteristic_writable_fields_container)
        writeMethodRadioGroup = findViewById(R.id.write_method_radio_group)
        writeWithResponseMethod = findViewById(R.id.write_with_resp_radio_button)
        writeWithoutResponseMethod = findViewById(R.id.write_without_resp_radio_button)

        btnSave = findViewById(R.id.save_btn)
        btnClear = findViewById(R.id.clear_btn)
        ivClose = findViewById(R.id.image_view_close)

        btnSave.apply {
            isEnabled = true
            isClickable = true
        }
    }

    private fun setListeners() {
        btnSave.setOnClickListener {
            if (isInputValid()) {
                val valueToSet =
                        if (GlucoseManagement.isRecordAccessControlPoint(characteristic))
                            GlucoseManagement.updateValueToWrite(value)
                        else value
                listener.onNewValueSet(valueToSet, writeType)
            }
        }
        btnClear.setOnClickListener {
            editableFields.forEach {
                it.setText("")
            }
        }
        ivClose.setOnClickListener { dismiss() }
    }

    private fun isInputValid() : Boolean {
        var isValid = false

        if (!isAnyWriteFieldEmpty()) {
            if (isRawValue) {
                isValid = true
            } else {
                if (!isAnyInputInvalid()) isValid = true
            }
        }

        return isValid
    }

    fun fillViewsWithValues(
            mGattService: BluetoothGattService?,
            mGattCharacteristic: BluetoothGattCharacteristic?,
            mCharacteristic: Characteristic?,
            writeType: FragmentCharacteristicDetail.WriteType
    ) {
        val serviceName = Common.getServiceName(mGattService?.uuid!!, context)
        val characteristicName = Common.getCharacteristicName(mGattCharacteristic?.uuid, context)

        val characteristicUuid =
                if (mCharacteristic != null) UuidUtils.getUuidText(mCharacteristic.uuid!!)
                else UuidUtils.getUuidText(mGattCharacteristic?.uuid!!)

        serviceNameTextView.text = serviceName
        characteristicNameTextView.text = characteristicName
        uuidTextView.text = characteristicUuid

        writeType.let {
            if (it != FragmentCharacteristicDetail.WriteType.REMOTE_WRITE) {
                writeMethodRadioGroup.visibility = View.GONE
                btnSave.text = when (it) {
                    FragmentCharacteristicDetail.WriteType.LOCAL_WRITE -> context.getString(R.string.button_save)
                    FragmentCharacteristicDetail.WriteType.LOCAL_INDICATE -> context.getString(R.string.button_indicate)
                    FragmentCharacteristicDetail.WriteType.LOCAL_NOTIFY -> context.getString(R.string.button_notify)
                    else -> { context.getString(R.string.button_save)}
                }
            } else {
                writeMethodRadioGroup.visibility = View.VISIBLE
            }

        }
    }

    fun getChosenWriteMethod() : Int {
        return if (writeWithResponseMethod.isChecked) {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        }
    }

    fun displayWriteMethods(isWritable: Boolean, isWritableWithoutResponse: Boolean) {
        writeWithResponseMethod.isEnabled = isWritable
        writeWithoutResponseMethod.isEnabled = isWritableWithoutResponse

        if (writeWithResponseMethod.isEnabled) {
            writeWithResponseMethod.isChecked = true
        } else if (writeWithoutResponseMethod.isEnabled) {
            writeWithoutResponseMethod.isChecked = true
        }
    }

    private val valueListener = object : ValueView.ValueListener {
        override fun onValueChanged(field: Field, newValue: ByteArray, fieldOffset: Int) {
            if (field.isNibbleFormat()) {
                handleNibbleWrite(field, newValue[0], fieldOffset)
            } else {
                newValue.copyInto(value, fieldOffset)
            }
        }

        override fun onRawValueChanged(newValue: ByteArray) {
            value = newValue
        }

        override fun onFieldsChanged() {
            resetDialogFieldViews()
        }

        override fun handleFieldView(fieldViewContainer: View) {
            writableFieldsContainer.addView(fieldViewContainer)
        }

        override fun addEditTexts(editTexts: ArrayList<EditText>) {
            editableFields.addAll(editTexts)
        }

        override fun addInRangeCheck(pair: Pair<Field, Boolean>) {
            fieldsInRangeMap[pair.first] = pair.second
        }

        override fun addValidityCheck(pair: Pair<Field, Boolean>) {
            fieldsValidMap[pair.first] = pair.second
        }
    }

    private fun isAnyInputInvalid() : Boolean {
        var validField: Boolean
        for ((_, value1) in fieldsValidMap) {
            validField = value1
            if (!validField) {
                Toast.makeText(context, R.string.characteristic_dialog_invalid_input, Toast.LENGTH_SHORT).show()
                return true
            }
        }

        var entryInRange: Boolean
        for ((_, value1) in fieldsInRangeMap) {
            entryInRange = value1
            if (!entryInRange) {
                Toast.makeText(context, R.string.characteristic_dialog_invalid_out_of_range, Toast.LENGTH_SHORT).show()
                return true
            }
        }

        return false
    }

    fun loadValueViews(isRawValue: Boolean, parseProblem: Boolean) {
        if (isRawValue || parseProblem) {
            this.isRawValue = isRawValue
            RawValueView(context, value).apply {
                onEditorActionListener = this@CharacteristicWriteDialog.onEditorActionListener
                createViewForWrite(0, this@CharacteristicWriteDialog.valueListener)
            }
        } else {
            addNormalValue()
        }
    }

    private fun addNormalValue() {
        characteristic?.fields?.forEach {
            addField(it)
            if (GlucoseManagement.isCgmSpecificOpsControlPoint(characteristic) && it.name == "Operand") {
                return
            }
        }
    }

    private fun addField(field: Field) {
        if (fieldViewHelper.isFieldPresent(field, value)) {
            if (field.referenceFields?.size!! > 0) {
                for (subField in field.referenceFields!!) {
                    addField(subField)
                }
            } else {
                if (field.reference == null) {
                    val currentValue = value
                    val currentOffset = offset
                    val fieldSize = calculateFieldSize(field)
                    val currentRange = currentValue.copyOfRange(currentOffset, currentOffset + fieldSize)

                    if (field.bitfield != null) {
                        BitFieldView(context, field, currentRange).createViewForWrite(currentOffset, valueListener)
                        offset += field.getSizeInBytes()

                    }
                    else if (field.enumerations != null && field.enumerations?.size!! > 0) {
                        if (field.isNibbleFormat()) {
                            handleNibbleRead(field, currentRange[0])
                        } else {
                            EnumerationView(context, field, currentRange).createViewForWrite(currentOffset, valueListener)
                            offset += field.getSizeInBytes()
                        }
                    }
                    else {
                        NormalValueView(context, field, currentRange).createViewForWrite(currentOffset, valueListener)
                        offset += fieldSize
                    }
                }
            }
        } else {
            offset += field.getSizeInBytes()
        }
    }

    private fun calculateFieldSize(field: Field) : Int {
        return if (field.getSizeInBytes() != 0) {
            field.getSizeInBytes()
        }
        else when (field.format) {
            "utf8s", "utf16s" -> value.size - offset
            "variable" -> field.getVariableFieldLength(characteristic, value)
            else -> 0
        }
    }

    private fun handleNibbleRead(field: Field, data: Byte) {
        val firstNibble = Converters.byteToUnsignedInt(data) shr 4
        val secondNibble = Converters.byteToUnsignedInt(data) and 0x0f

        if (fieldViewHelper.isFirstNibbleInByte(field)) {
            EnumerationView(context, field, byteArrayOf(firstNibble.toByte())).createViewForWrite(offset, valueListener)
        } else {
            EnumerationView(context, field, byteArrayOf(secondNibble.toByte())).createViewForWrite(offset, valueListener)
            offset += field.getSizeInBytes()
        }
    }

    private fun handleNibbleWrite(field: Field, newValue: Byte, fieldOffset: Int) {
        var nibbleByte = value[fieldOffset].toInt() and 0xff

        if (fieldViewHelper.isFirstNibbleInByte(field)) {
                val firstNibble = newValue.toInt() and 0x0f shl 4
                nibbleByte = nibbleByte and 0x0f
                nibbleByte = nibbleByte or firstNibble
        } else {
                val secondNibble = newValue.toInt() and 0x0f
                nibbleByte = nibbleByte and 0xf0
                nibbleByte = nibbleByte or secondNibble
        }
        byteArrayOf(nibbleByte.toByte()).copyInto(value, fieldOffset)
    }

    private fun isAnyWriteFieldEmpty(): Boolean {
        editableFields.forEach {
            if (it.text.toString().isEmpty()) {
                Toast.makeText(context, R.string.You_cannot_send_empty_value_to_charac, Toast.LENGTH_SHORT).show()
                return true
            }
        }
        return false
    }

    private val onEditorActionListener = object : TextView.OnEditorActionListener {
        override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (isInputValid()) listener.onNewValueSet(value, writeType)
                return true
            }
            return false
        }
    }

    private fun resetDialogFieldViews() {
        offset = 0
        writableFieldsContainer.removeAllViews()
        loadValueViews(isRawValue = false, parseProblem = false)
    }


    interface WriteDialogListener {
        fun onNewValueSet(newValue: ByteArray, writeType: FragmentCharacteristicDetail.WriteType)
    }


    init {
        setWindow()
        findViews()
        setListeners()
    }
}