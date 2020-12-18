package com.siliconlabs.bledemo.Browser.Fragments

import android.app.Dialog
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.siliconlabs.bledemo.Advertiser.Utils.HtmlCompat
import com.siliconlabs.bledemo.Bluetooth.DataTypes.*
import com.siliconlabs.bledemo.Bluetooth.DataTypes.Enumeration
import com.siliconlabs.bledemo.Bluetooth.Parsing.Common
import com.siliconlabs.bledemo.Bluetooth.Parsing.Consts
import com.siliconlabs.bledemo.Bluetooth.Parsing.Engine
import com.siliconlabs.bledemo.Bluetooth.Services.BluetoothLeService
import com.siliconlabs.bledemo.Browser.Activities.DeviceServicesActivity
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.Utils.Converters
import com.siliconlabs.bledemo.Utils.StringUtils
import java.math.BigInteger
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.pow

open class FragmentCharacteristicDetail : Fragment() {

    private var currRefreshInterval = REFRESH_INTERVAL

    lateinit var fieldsInRangeMap: HashMap<Field, Boolean>
    lateinit var fieldsValidMap: HashMap<Field, Boolean>
    private val rawValueViews = ArrayList<EditText>()
    private var editTexts = ArrayList<EditText>()
    private val hidableViews = ArrayList<View>()
    private var rawValueData = ArrayList<String>()
    private var handler = Handler()

    private var readable = false
    private var writeable = false
    private var writeableWithoutResponse = false
    private var notify = false
    private var isRawValue = false
    private var parseProblem = false
    private var writeWithResponse = true
    private var writeString = false
    private var foundField = false
    var notificationsEnabled = false
    var indicationsEnabled = false
    var displayWriteDialog = false

    private lateinit var hexEdit: EditText
    private lateinit var asciiEdit: EditText
    private lateinit var decimalEdit: EditText
    private lateinit var hex: EditText
    private lateinit var ascii: EditText
    private lateinit var decimal: EditText

    private val postLoadValueViews = Runnable { loadValueViews() }
    private val postDisplayValues = Runnable { displayValues() }

    private lateinit var parsingProblemInfo: String
    private var offset = 0
    private var value: ByteArray = ByteArray(0)
    private var previousValue: ByteArray = ByteArray(0)

    private var mBluetoothLeService: BluetoothLeService? = null
    var mBluetoothCharact: BluetoothGattCharacteristic? = null
    private var mCharact: Characteristic? = null
    private var mGattService: BluetoothGattService? = null
    private var mService: Service? = null
    private var mDevice: BluetoothGatt? = null

    private lateinit var valuesLayout: LinearLayout

    var address: String? = null
    private var editableFieldsDialog: Dialog? = null
    private var writableFieldsContainer: LinearLayout? = null
    private var saveValueBtn: Button? = null
    private var btnClear: Button? = null
    private var ivClose: ImageView? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_characteristic_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        valuesLayout = view.findViewById(R.id.values_layout)

        mDevice = (activity as DeviceServicesActivity).bluetoothGatt
        mCharact = Engine.instance?.getCharacteristic(mBluetoothCharact?.uuid)
        mService = Engine.instance?.getService(mBluetoothCharact?.service?.uuid)

        setProperties()
        Log.d("Charac", mBluetoothCharact?.uuid.toString() + " " + mBluetoothCharact?.instanceId)
        mBluetoothCharact?.properties

        configureWriteable()
        updateBall()

        if (!isRawValue) {
            prepareValueData()
        }

        loadValueViews()

        if (displayWriteDialog) {
            showCharacteristicWriteDialog()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mBluetoothLeService = null
    }

    // Builds activity UI based on characteristic content
    private fun loadValueViews() {
        fieldsInRangeMap = HashMap()
        fieldsValidMap = HashMap()
        editTexts.clear()

        if (!isRawValue) {
            if (parseProblem || !addNormalValue()) {
                editTexts.clear()
                addInvalidValue()
            }
        } else {
            addRawValue()
        }
    }

    // Configures characteristic if it is writeable
    private fun configureWriteable() {
        if (writeable || writeableWithoutResponse) {
            initCharacteristicWriteDialog()
        }
    }

    fun onActionDataWrite(uuid: String, status: Int) {
        if (mBluetoothCharact?.uuid.toString() != uuid) {
            return
        }
        activity?.runOnUiThread {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Toast.makeText(activity, getText(R.string.characteristic_write_success), Toast.LENGTH_SHORT).show()
                editableFieldsDialog?.dismiss()
                (activity as DeviceServicesActivity).refreshCharacteristicExpansion()
            } else {
                Toast.makeText(activity, getText(R.string.characteristic_write_fail), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun onActionDataAvailable(uuidCharacteristic: String) {
        if (currRefreshInterval >= REFRESH_INTERVAL) {
            if (uuidCharacteristic == mBluetoothCharact?.uuid.toString()) {
                activity?.runOnUiThread {
                    if (currRefreshInterval >= REFRESH_INTERVAL) {
                        currRefreshInterval = 0
                        offset = 0

                        value = if(mBluetoothCharact?.value == null) {
                            ByteArray(0)
                        } else {
                            mBluetoothCharact?.value!!
                        }


                        if (indicationsEnabled || notificationsEnabled) {
                            valuesLayout.removeAllViews()
                            loadValueViews()
                        } else if (value.contentEquals(previousValue)) {
                            hideValues()
                            handler.removeCallbacks(postDisplayValues)
                            handler.postDelayed(postDisplayValues, 150)
                        } else {
                            valuesLayout.removeAllViews()
                            handler.removeCallbacks(postLoadValueViews)
                            handler.postDelayed(postLoadValueViews, 150)
                        }
                        if (value.isNotEmpty()) previousValue = value.clone()

                    }
                }
            }
        }
    }

    fun setmBluetoothCharact(mBluetoothCharact: BluetoothGattCharacteristic?) {
        this.mBluetoothCharact = mBluetoothCharact
    }

    fun setmService(service: BluetoothGattService?) {
        mGattService = service
    }

    // Sets property members for characteristics
    private fun setProperties() {
        mBluetoothCharact?.let { mBluetoothCharact ->
            if (Common.isSetProperty(Common.PropertyType.READ, mBluetoothCharact.properties)) {
                readable = true
            }
            if (Common.isSetProperty(Common.PropertyType.WRITE, mBluetoothCharact.properties)) {
                writeable = true
            }
            if (Common.isSetProperty(Common.PropertyType.WRITE_NO_RESPONSE, mBluetoothCharact.properties)) {
                writeableWithoutResponse = true
            }
            if (Common.isSetProperty(Common.PropertyType.NOTIFY, mBluetoothCharact.properties)
                    || Common.isSetProperty(Common.PropertyType.INDICATE, mBluetoothCharact.properties)) {
                notify = true
            }
        }

        //Display IEEE characteristic as raw data
        if (mCharact == null || mCharact?.fields == null || mCharact?.name == "IEEE 11073-20601 Regulatory Certification Data List") {
            isRawValue = true
        }
    }

    private fun writeValueToCharacteristic() {
        val hexEdit = editableFieldsDialog?.findViewById<EditText>(R.id.hex_edit)

        if (writeWithResponse) mBluetoothCharact?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        else mBluetoothCharact?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE

        if (hexEdit != null) {
            val hex = hexEdit.text.toString().replace("\\s+".toRegex(), "")
            val newValue = hexToByteArray(hex)
            try {
                Log.d("Name", "" + mDevice?.device?.name)
                Log.d("Address", "" + mDevice?.device?.address)
                Log.d("Service", "" + mBluetoothCharact?.service?.uuid)
                Log.d("Charac", "" + mBluetoothCharact?.uuid)
                mBluetoothCharact?.value = newValue
                Log.d("hex", "" + Converters.bytesToHexWhitespaceDelimited(mBluetoothCharact?.value))
                mDevice?.writeCharacteristic(mBluetoothCharact)
            } catch (e: Exception) {
                Log.e("Service", "null$e")
            }
        } else {
            if (possibleToSave()) {
                mBluetoothCharact?.value = value
                mDevice?.writeCharacteristic(mBluetoothCharact)
                Log.d("write_val", "Standard Value to write (hex): " + Converters.bytesToHexWhitespaceDelimited(value))
            }
        }
    }

    private fun hideValues() {
        for (view in hidableViews) {
            view.visibility = View.GONE
        }

        rawValueData = ArrayList()
        for (et in rawValueViews) {
            rawValueData.add(et.text.toString())
            et.setText("")
        }
    }

    private fun displayValues() {
        for (view in hidableViews) {
            view.visibility = View.VISIBLE
        }

        for ((i, et) in rawValueViews.withIndex()) {
            et.setText(rawValueData[i])
        }
    }

    private fun possibleToSave(): Boolean {
        var validField = true
        for ((_, value1) in fieldsValidMap) {
            validField = value1
            if (!validField) {
                break
            }
        }

        var entryInRange = true
        for ((_, value1) in fieldsInRangeMap) {
            entryInRange = value1
            if (!entryInRange) {
                break
            }
        }

        return if (!validField) {
            Toast.makeText(context, context?.getString(R.string.characteristic_dialog_invalid_input), Toast.LENGTH_SHORT).show()
            false
        } else if (!entryInRange) {
            Toast.makeText(context, context?.getString(R.string.characteristic_dialog_invalid_out_of_range), Toast.LENGTH_SHORT).show()
            false
        } else true
    }

    // Count time that is used to preventing from very fast refreshing view
    private fun updateBall() {
        val timer = Timer()
        val updateBall: TimerTask = object : TimerTask() {
            override fun run() {
                currRefreshInterval += REFRESH_INTERVAL
            }
        }
        timer.scheduleAtFixedRate(updateBall, 0, REFRESH_INTERVAL.toLong())
    }

    fun getmCharact(): Characteristic? {
        return mCharact
    }

    // Builds activity UI in tree steps:
    // a) add views based on characteristic content without setting values
    // b) add problem info view
    // c) add raw views (hex, ASCII, decimal) with setting values
    private fun addInvalidValue() {
        valuesLayout.removeAllViews()
        addNormalValue()
        addProblemInfoView()
        addRawValue()
    }

    // Only called when characteristic is standard Bluetooth characteristic
    // Build activity UI based on characteristic content and also take account
    // of field requirements
    private fun addNormalValue(): Boolean {
        writableFieldsContainer?.removeAllViews()

        for (i in mCharact?.fields?.indices!!) {
            try {
                val field = mCharact?.fields!![i]
                addField(field)
            } catch (ex: Exception) {
                Log.i("CharacteristicUI", i.toString())
                Log.i("Characteristic value", Converters.getDecimalValue(value))
                parsingProblemInfo = prepareParsingProblemInfo(mCharact)
                parseProblem = true
                return false
            }
        }
        for (et in editTexts) {
            et.setText("")
        }

        return true
    }

    private fun prepareParsingProblemInfo(characteristic: Characteristic?): String {
        val builder = StringBuilder()
        builder.append("An error occurred while parsing this characteristic.").append("\n")
        if (value.isEmpty()) return builder.toString()

        var expectedBytes = 0
        val readSize = value.size

        try {
            for (i in characteristic?.fields?.indices!!) {
                val field = characteristic.fields!![i]
                expectedBytes += Engine.instance?.getFormat(field.format)!!
            }
        } catch (ex: NullPointerException) {
            return builder.toString()
        }

        if (expectedBytes != readSize) {
            val expectedBits = expectedBytes * 8
            val readBits = readSize * 8

            builder.append("Reason: expected data length is ")
                    .append(expectedBits)
                    .append("-bit (")
                    .append(expectedBytes)

            if (expectedBytes == 1) {
                builder.append(" byte), ")
            } else {
                builder.append(" bytes), ")
            }

            builder.append("\n")
                    .append("read data length is ")
                    .append(readBits)
                    .append("-bit (")
                    .append(readSize)

            if (readSize == 1) {
                builder.append(" byte).")
            } else {
                builder.append(" bytes).")
            }
        }
        return builder.toString()
    }

    // Add single field
    private fun addField(field: Field) {
        if (isFieldPresent(field)) {
            if (field.referenceFields?.size!! > 0) {
                for (subField in field.referenceFields!!) {
                    addField(subField)
                }
            } else {
                if (field.bitfield != null) addBitfield(field)
                else if (field.enumerations != null && field.enumerations?.size!! > 0) addEnumeration(field)
                else addValue(field)
            }
        }
    }

    // Initializes byte array with empty characteristic content
    private fun prepareValueData() {
        val size = characteristicSize()
        if (size != 0) {
            value = ByteArray(size)
        }
    }

    // Returns characteristic size in bytes
    private fun characteristicSize(): Int {
        var size = 0
        for (field in mCharact?.fields.orEmpty()) {
            size += fieldSize(field)
        }
        return size
    }

    // Returns only one field size in bytes
    private fun fieldSize(field: Field): Int {
        val format = field.format
        return when {
            format != null -> Engine.instance?.getFormat(format)!!
            field.referenceFields?.size!! > 0 -> {
                var subFieldsSize = 0
                for (subField in field.referenceFields!!) {
                    subFieldsSize += fieldSize(subField)
                }
                subFieldsSize
            }
            else -> 0
        }
    }

    // Checks if field is present based on it's requirements and bitfield
    // settings
    private fun isFieldPresent(field: Field): Boolean {
        if (parseProblem) {
            return true
        }

        if (field.requirement == null || field.requirement == Consts.REQUIREMENT_MANDATORY) {
            return true
        } else {
            for (bitField in getBitFields()) {
                for (bit in bitField.bitfield?.bits!!) {
                    for (enumeration in bit.enumerations!!) {
                        if (enumeration.requires != null && field.requirement == enumeration.requires) {
                            return checkRequirement(bitField, enumeration, bit)
                        }
                    }
                }
            }
        }
        return false
    }

    // Checks requirement on exactly given bitfield, enumeration and bit
    private fun checkRequirement(bitField: Field, enumeration: Enumeration, bit: Bit): Boolean {
        val formatLength = Engine.instance?.getFormat(bitField.format)
        val off = getFieldOffset(bitField)
        val tmpVal = readInt(off, formatLength!!)
        val enumVal = readEnumInt(bit.index, bit.size, tmpVal)
        return enumVal == enumeration.key
    }

    // Converts string given in hexadecimal system to byte array
    fun hexToByteArray(hex: String): ByteArray {
        var tmpHex = hex

        if (tmpHex.isNotEmpty() && tmpHex.length % 2 != 0) {
            tmpHex = "0$tmpHex"
        }
        val len = tmpHex.length / 2
        val byteArr = ByteArray(len)
        for (i in byteArr.indices) {
            val init = i * 2
            val end = init + 2
            val temp = tmpHex.substring(init, end).toInt(16)
            byteArr[i] = (temp and 0xFF).toByte()
        }
        return byteArr
    }

    // Converts string given in decimal system to byte array
    private fun decToByteArray(dec: String): ByteArray {
        if (dec.isEmpty()) {
            return byteArrayOf()
        }

        val decArray = dec.split(" ").toTypedArray()
        val byteArr = ByteArray(decArray.size)
        for (i in decArray.indices) {
            try {
                byteArr[i] = decArray[i].toInt().toByte()
            } catch (e: NumberFormatException) {
                return byteArrayOf(0)
            }
        }
        return byteArr
    }


    // Converts int to byte array
    private fun intToByteArray(newVal: Int, formatLength: Int): ByteArray {
        var tmpNewVal = newVal
        val tmpVal = ByteArray(formatLength)
        for (i in 0 until formatLength) {
            tmpVal[i] = (tmpNewVal and 0xff).toByte()
            tmpNewVal = tmpNewVal shr 8
        }
        return tmpVal
    }

    // Checks if decimal input value is valid
    private fun isDecValueValid(decValue: String): Boolean {
        val value = decValue.toCharArray()
        val valLength = value.size
        return if (decValue.length < 4) {
            true
        } else {
            value[valLength - 1] == ' ' || value[valLength - 2] == ' ' || value[valLength - 3] == ' ' || value[valLength - 4] == ' '
        }
    }

    // Reads integer value for given offset and field size
    private fun readInt(offset: Int, size: Int): Int {
        var tmpVal = 0
        for (i in 0 until size) {
            tmpVal = tmpVal shl 8
            tmpVal = tmpVal or value[offset + i].toInt()
        }
        return tmpVal
    }

    private fun getSint16AsString(array: ByteArray): String {
        val builder = StringBuilder()

        for (i in array.indices) {
            if (array[i] < 0) {
                array[i] = (array[i] + 256).toByte()
            }
        }
        for (i in array.indices.reversed()) {
            builder.append(Converters.getHexValue(array[i]))
        }

        var result = builder.toString().toInt(16)
        if (result >= 32768) result -= 65536

        return result.toString()
    }

    // Reads next value for given format
    private fun readNextValue(format: String): String {
        if (value.isEmpty()) {
            return ""
        }
        val formatLength = Engine.instance?.getFormat(format)

        // if format is sint16
        if (format.toLowerCase(Locale.getDefault()) == "sint16") {
            val array = value.copyOfRange(offset, offset + formatLength!!)
            offset += formatLength
            return getSint16AsString(array)
        }

        // binaryString is used for sints, used to fix original bluegiga code ignoring data format type
        val binaryString = StringBuilder()
        try {
            for (i in offset until offset + formatLength!!) {
                binaryString.append(String.format("%8s", Integer.toBinaryString(value[i].toInt().and(0xFF))).replace(' ', '0'))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        var result = StringBuilder()
        // If field length equals 0 then reads from offset to end of characteristic data
        if (formatLength == 0) {
            if (format.toLowerCase(Locale.getDefault()) == "reg-cert-data-list") {
                result = StringBuilder("0x" + Converters.bytesToHexWhitespaceDelimited(value.copyOfRange(offset, value.size)))
                result = StringBuilder(result.toString().replace(" ", ""))
            } else {
                result = StringBuilder(String(value.copyOfRange(offset, value.size)))
            }
            offset += value.size
        } else {
            // If format type is kind of float type then reads float value
            if (format == TYPE_SFLOAT || format == TYPE_FLOAT || format == TYPE_FLOAT_32 || format == TYPE_FLOAT_64) {
                val fValue = readFloat(format, formatLength!!)
                result = StringBuilder(String.format(Locale.US, "%.3f", fValue))
            } else {
                for (i in offset until offset + formatLength!!) {
                    result.append((value[i].toInt().and(0xff)))
                }
            }
            offset += formatLength
        }

        // bluegiga code fix, original source code did not check for sint or uint
        if (format.toLowerCase(Locale.getDefault()).startsWith("sint")) {
            try {
                result = StringBuilder(Converters.getDecimalValueFromTwosComplement(binaryString.toString()))
                return result.toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (format.toLowerCase(Locale.getDefault()).startsWith("uint")) {
            try {
                // note that the (- formatLength) gets the original offset.
                // java uses big endian, payload is little endian
                val bytes = value.copyOfRange(offset - formatLength, offset)
                var uintAsLong = 0L
                for (i in 0 until formatLength) {
                    uintAsLong = uintAsLong shl 8
                    val byteAsInt: Int = bytes[formatLength - 1 - i].toInt().and(0xff)
                    uintAsLong = uintAsLong or byteAsInt.toLong()
                }
                val uintVal = if (formatLength < 9) "" + uintAsLong else BigInteger("0$binaryString", 2).toString(16)
                return "" + uintVal
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return result.toString()
    }

    // Reads float value for given format
    private fun readFloat(format: String, formatLength: Int): Double {
        var result = 0.0
        when (format) {
            TYPE_SFLOAT -> result = Common.readSfloat(value, offset, formatLength - 1).toDouble()
            TYPE_FLOAT -> result = Common.readFloat(value, offset, formatLength - 1).toDouble()
            TYPE_FLOAT_32 -> result = Common.readFloat32(value, offset, formatLength).toDouble()
            TYPE_FLOAT_64 -> result = Common.readFloat64(value, offset, formatLength)
        }
        return result
    }

    // Reads enum for given value
    private fun readEnumInt(index: Int, size: Int, tmpVal: Int): Int {
        var result = 0
        for (i in 0 until size) {
            result = result shl 8
            result = result or (tmpVal shr index + i and 0x1)
        }
        return result
    }

    // Sets value from offset position
    private fun setValue(off: Int, arr: ByteArray) {
        for (i in off until off + arr.size) {
            value[i] = arr[i - off]
        }
    }

    // Gets field offset in bytes
    private fun getFieldOffset(searchField: Field): Int {
        foundField = false
        var off = 0

        for (field in mCharact?.fields.orEmpty()) {
            off += getOffset(field, searchField)
        }
        foundField = true
        return off
    }

    // Gets field offset when field has references to other fields
    private fun getOffset(field: Field, searchField: Field): Int {
        var off = 0
        if (field === searchField) {
            foundField = true
            return off
        }
        if (!foundField && isFieldPresent(field)) {
            if (field.referenceFields?.size!! > 0) {
                for (subField in field.referenceFields!!) {
                    off += getOffset(subField, searchField)
                }
            } else {
                if (field.format != null) {
                    off += Engine.instance?.getFormat(field.format)!!
                }
            }
        }

        return off
    }

    // Gets all bit fields for this characteristic
    private fun getBitFields(): ArrayList<Field> {
        val bitFields = ArrayList<Field>()
        for (f in mCharact?.fields.orEmpty()) {
            bitFields.addAll(getBitField(f))
        }
        return bitFields
    }

    // Gets bit field when field has references to other fields
    private fun getBitField(field: Field): ArrayList<Field> {
        val bitFields = ArrayList<Field>()
        if (field.bitfield != null) {
            bitFields.add(field)
        } else if (field.referenceFields?.size!! > 0) {
            for (subField in field.referenceFields!!) {
                bitFields.addAll(getBitField(subField))
            }
        }
        return bitFields
    }


    // Builds activity UI if characteristic is not standard characteristic (from
    // Bluetooth specifications)
    private fun addRawValue() {
        // read only fields and value display for characteristic (inline)
        //val layoutInflater = context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val readableFieldsForInline = View.inflate(context, R.layout.characteristic_value_read_only, null)
        hex = readableFieldsForInline.findViewById(R.id.hex_readonly)
        ascii = readableFieldsForInline.findViewById(R.id.ascii_readonly)
        decimal = readableFieldsForInline.findViewById(R.id.decimal_readonly)

        val hexCopyIV = readableFieldsForInline.findViewById<ImageView>(R.id.hex_copy)
        val asciiCopyIV = readableFieldsForInline.findViewById<ImageView>(R.id.ascii_copy)
        val decimalCopyIV = readableFieldsForInline.findViewById<ImageView>(R.id.decimal_copy)

        hex.id = EDIT_NOT_CLEAR_ID
        ascii.id = EDIT_NOT_CLEAR_ID
        decimal.id = EDIT_NOT_CLEAR_ID

        hex.keyListener = null
        ascii.keyListener = null
        decimal.keyListener = null

        hex.setText(Converters.bytesToHexWhitespaceDelimited(value))
        ascii.setText(Converters.getAsciiValue(value))
        decimal.setText(Converters.getDecimalValue(value))

        rawValueViews.add(hex)
        rawValueViews.add(ascii)
        rawValueViews.add(decimal)

        setCopyListener(hex, hexCopyIV)
        setCopyListener(ascii, asciiCopyIV)
        setCopyListener(decimal, decimalCopyIV)

        valuesLayout.addView(readableFieldsForInline)

        if (writeable || writeableWithoutResponse) {
            val writableFieldsForDialog = View.inflate(context, R.layout.characteristic_value, null)

            hexEdit = writableFieldsForDialog.findViewById(R.id.hex_edit)
            asciiEdit = writableFieldsForDialog.findViewById(R.id.ascii_edit)
            decimalEdit = writableFieldsForDialog.findViewById(R.id.decimal_edit)

            val hexPasteIV = writableFieldsForDialog.findViewById<ImageView>(R.id.hex_paste)
            val asciiPasteIV = writableFieldsForDialog.findViewById<ImageView>(R.id.ascii_paste)
            val decimalPasteIV = writableFieldsForDialog.findViewById<ImageView>(R.id.decimal_paste)

            editTexts.add(hexEdit)
            editTexts.add(asciiEdit)
            editTexts.add(decimalEdit)

            val hexWatcher = hexTextWatcher
            val decWatcher = decTextWatcher
            val asciiWatcher = asciiTextWatcher

            val hexListener = hexFocusChangeListener
            hexEdit.onFocusChangeListener = hexListener

            val commiter = WriteCharacteristic()
            hexEdit.setOnEditorActionListener(commiter)
            asciiEdit.setOnEditorActionListener(commiter)
            decimalEdit.setOnEditorActionListener(commiter)
            hexEdit.addTextChangedListener(hexWatcher)
            asciiEdit.addTextChangedListener(asciiWatcher)
            decimalEdit.addTextChangedListener(decWatcher)

            setPasteListener(hexEdit, hexPasteIV, HEX_ID)
            setPasteListener(asciiEdit, asciiPasteIV, ASCII_ID)
            setPasteListener(decimalEdit, decimalPasteIV, DECIMAL_ID)

            updateSaveButtonState()
            writableFieldsContainer?.removeAllViews()
            writableFieldsContainer?.addView(writableFieldsForDialog)
        }
    }

    private fun setCopyListener(copyFromET: EditText?, copyIV: ImageView) {
        copyIV.setOnClickListener {
            val clipboardManager = context?.getSystemService(ClipboardManager::class.java)
            val clip = ClipData.newPlainText("characteristic-value", copyFromET?.text.toString())
            if (clipboardManager != null) {
                clipboardManager.setPrimaryClip(clip)
                Toast.makeText(context, getString(R.string.Copied_to_clipboard), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setPasteListener(pasteToET: EditText?, pasteIV: ImageView, expectedPasteType: String) {
        pasteIV.setOnClickListener {
            val clipboardManager = context?.getSystemService(ClipboardManager::class.java)
            if (clipboardManager != null && clipboardManager.primaryClip != null) {
                val clip = clipboardManager.primaryClip
                var text = clip?.getItemAt(0)?.text.toString()
                pasteToET?.requestFocus()
                when (expectedPasteType) {
                    HEX_ID -> {
                        text = StringUtils.getStringWithoutWhitespaces(text)
                        if (isHexStringCorrect(text)) pasteToET?.setText(text) else Toast.makeText(context, getString(R.string.Incorrect_data_format), Toast.LENGTH_SHORT).show()
                    }
                    ASCII_ID -> pasteToET?.setText(text)
                    DECIMAL_ID -> if (isDecimalCorrect(text.trim())) pasteToET?.setText(text) else Toast.makeText(context, getString(R.string.Incorrect_data_format), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun isHexStringCorrect(text: String): Boolean {
        for (element in text) {
            if (!StringUtils.HEX_VALUES.contains(element.toString())) return false
        }
        return true
    }

    private fun isDecimalCorrect(text: String): Boolean {
        val arr = text.split(" ").toTypedArray()
        try {
            for (s in arr) {
                val tmp = s.toInt()
                if (tmp !in 0..255) return false
            }
        } catch (e: Exception) {
            return false
        }
        return true
    }

    private fun initCharacteristicWriteDialog() {
        context?.let { editableFieldsDialog = Dialog(it) }

        editableFieldsDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        editableFieldsDialog?.setContentView(R.layout.dialog_characteristic_write)
        editableFieldsDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        writableFieldsContainer = editableFieldsDialog?.findViewById(R.id.characteristic_writable_fields_container)

        val width = (resources.displayMetrics.widthPixels * 0.9).toInt()

        editableFieldsDialog?.window?.setLayout(width, LinearLayout.LayoutParams.WRAP_CONTENT)
        editableFieldsDialog?.let { initWriteModeView(it) }

        saveValueBtn = editableFieldsDialog?.findViewById(R.id.save_btn)
        btnClear = editableFieldsDialog?.findViewById(R.id.clear_btn)
        ivClose = editableFieldsDialog?.findViewById(R.id.image_view_close)
    }

    private fun isAnyWriteFieldEmpty(): Boolean {
        for (e in editTexts) {
            if (e.id == EDIT_NOT_CLEAR_ID) continue
            if (e.text.toString().isEmpty()) return true
        }
        return false
    }

    fun showCharacteristicWriteDialog() {
        // if any textfields are empty, save btn_rounded_red will be initialized to be disabled
        updateSaveButtonState()
        saveValueBtn?.setOnClickListener {
            if (!isAnyWriteFieldEmpty()) {
                writeValueToCharacteristic()
            } else {
                Toast.makeText(activity, getString(R.string.You_cannot_send_empty_value_to_charac), Toast.LENGTH_SHORT).show()
            }
        }

        btnClear?.setOnClickListener {
            for (et in editTexts) {
                if (et.id != EDIT_NOT_CLEAR_ID) {
                    et.setText("")
                }
            }
        }

        ivClose?.setOnClickListener { editableFieldsDialog?.dismiss() }

        var serviceName = if (mService != null) mService?.name!!.trim { it <= ' ' } else getString(R.string.unknown_service)
        serviceName = Common.checkOTAService(mGattService?.uuid.toString(), serviceName)

        val characteristicName: String = if (mCharact != null) {
            mCharact?.name!!.trim { it <= ' ' }
        } else {
            getOtaSpecificCharacteristicName(mBluetoothCharact?.uuid.toString())
        }

        val characteristicUuid = if (mCharact != null) Common.getUuidText(mCharact?.uuid!!) else Common.getUuidText(mBluetoothCharact?.uuid!!)
        val serviceNameTextView = editableFieldsDialog?.findViewById<TextView>(R.id.picker_dialog_service_name)
        val characteristicTextView = editableFieldsDialog?.findViewById<TextView>(R.id.characteristic_dialog_characteristic_name)
        val uuidTextView = editableFieldsDialog?.findViewById<TextView>(R.id.picker_dialog_characteristic_uuid)

        serviceNameTextView?.text = serviceName
        characteristicTextView?.text = characteristicName
        uuidTextView?.text = characteristicUuid

        val propertiesContainer = editableFieldsDialog?.findViewById<LinearLayout>(R.id.picker_dialog_properties_container)
        propertiesContainer?.let { initPropertiesForEditableFieldsDialog(it) }

        //Clear EditText fields
        for (et in editTexts) {
            if (et.id != EDIT_NOT_CLEAR_ID) {
                et.setText("")
            }
        }

        editableFieldsDialog?.show()
        updateSaveButtonState()
    }

    private fun initPropertiesForEditableFieldsDialog(propertiesContainer: LinearLayout) {
        propertiesContainer.removeAllViews()

        val propertiesString = if (mBluetoothCharact != null) Common.getProperties(requireContext(), mBluetoothCharact?.properties!!) else ""
        val propsExploded = propertiesString.split(",").toTypedArray()

        for (propertyValue in propsExploded) {
            val propertyView = TextView(context)
            var propertyValueTrimmed = propertyValue.trim { it <= ' ' }
            // length 13 is used to cut off property string at length "Write no resp"
            propertyValueTrimmed = if (propertyValue.length > 13) propertyValue.substring(0, 13) else propertyValueTrimmed
            propertyValueTrimmed.toUpperCase(Locale.getDefault())

            propertyView.apply {
                text = propertyValueTrimmed
                append("  ")
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.characteristic_property_text_size))
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                context?.let { propertyView.setTextColor(ContextCompat.getColor(it, R.color.silabs_blue)) }
            }

            val propertyContainer = LinearLayout(context)
            propertyContainer.orientation = LinearLayout.HORIZONTAL
            val propertyIcon = ImageView(context)

            val iconId = when (propertyValue.trim(' ').toUpperCase(Locale.getDefault())) {
                "BROADCAST" -> R.drawable.ic_debug_prop_broadcast
                "READ" -> R.drawable.ic_read_on
                "WRITE NO RESPONSE" -> R.drawable.ic_debug_prop_write_no_resp
                "WRITE" -> R.drawable.ic_edit_on
                "NOTIFY" -> R.drawable.ic_notify_on
                "INDICATE" -> R.drawable.ic_indicate_on
                "SIGNED WRITE" -> R.drawable.ic_debug_prop_signed_write
                "EXTENDED PROPS" -> R.drawable.ic_debug_prop_ext
                else -> R.drawable.ic_debug_prop_ext
            }
            propertyIcon.setBackgroundResource(iconId)

            val paramsText = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            paramsText.gravity = Gravity.CENTER_VERTICAL or Gravity.START

            var paramsIcon = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            paramsIcon.gravity = Gravity.CENTER_VERTICAL or Gravity.END

            if (propertyValue.trim { it <= ' ' }.toUpperCase(Locale.getDefault()) == "WRITE NO RESPONSE") {
                val d = resources.displayMetrics.density
                paramsIcon = LinearLayout.LayoutParams((24 * d).toInt(), (24 * d).toInt())
                paramsIcon.gravity = Gravity.CENTER_VERTICAL or Gravity.END
            }

            propertyContainer.addView(propertyView, paramsText)
            propertyContainer.addView(propertyIcon, paramsIcon)

            val paramsTextAndIconContainer = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            paramsTextAndIconContainer.gravity = Gravity.END

            propertiesContainer.addView(propertyContainer, paramsTextAndIconContainer)
        }
    }

    // Gets text watcher for hex edit view
    private val hexTextWatcher: TextWatcher
        get() = object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (hexEdit.hasFocus()) {
                    val textLength = hexEdit.text.toString().length
                    val newValue: ByteArray
                    if (textLength % 2 == 1) {
                        var temp = hexEdit.text.toString()
                        temp = temp.substring(0, textLength - 1) + "0" + temp[textLength - 1]
                        newValue = hexToByteArray(temp.replace("\\s+".toRegex(), ""))
                    } else {
                        newValue = hexToByteArray(hexEdit.text.toString().replace("\\s+".toRegex(), ""))
                    }
                    asciiEdit.setText(Converters.getAsciiValue(newValue))
                    decimalEdit.setText(Converters.getDecimalValue(newValue))
                }
                updateSaveButtonState()
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                updateSaveButtonState()
            }

            override fun afterTextChanged(s: Editable) {
                updateSaveButtonState()
            }
        }

    // Gets text watcher for decimal edit view
    private val decTextWatcher: TextWatcher
        get() = object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (decimalEdit.hasFocus()) {
                    if (isDecValueValid(decimalEdit.text.toString())) {
                        val newValue = decToByteArray(decimalEdit.text.toString())
                        hexEdit.setText(Converters.bytesToHexWhitespaceDelimited(newValue))
                        asciiEdit.setText(Converters.getAsciiValue(newValue))
                    } else {
                        decimalEdit.setText(decimalEdit.text.toString().substring(0,
                                decimalEdit.text.length - 1))
                        decimalEdit.setSelection(decimalEdit.text.length)
                        Toast.makeText(context, R.string.invalid_dec_value, Toast.LENGTH_SHORT)
                                .show()
                    }
                }
                updateSaveButtonState()
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                updateSaveButtonState()
            }

            override fun afterTextChanged(s: Editable) {
                updateSaveButtonState()
            }
        }

    // Gets text watcher for ascii edit view
    private val asciiTextWatcher: TextWatcher
        get() = object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (asciiEdit.hasFocus()) {
                    val newValue = asciiEdit.text.toString().toByteArray()
                    hexEdit.setText(Converters.bytesToHexWhitespaceDelimited(newValue))
                    decimalEdit.setText(Converters.getDecimalValue(newValue))
                }
                updateSaveButtonState()
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                updateSaveButtonState()
            }

            override fun afterTextChanged(s: Editable) {
                updateSaveButtonState()
            }
        }

    // Gets focus listener for hex edit view
    private val hexFocusChangeListener: View.OnFocusChangeListener
        get() = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                hexEdit.setText(hexEdit.text.toString().replace("\\s+".toRegex(), ""))
            } else {
                val textLength = hexEdit.text.toString().length
                val hexValue: String
                hexValue = if (textLength % 2 == 1) {
                    val temp = hexEdit.text.toString()
                    temp.substring(0, textLength - 1) + "0" + temp[textLength - 1]
                } else {
                    hexEdit.text.toString()
                }
                val value = hexToByteArray(hexValue)
                hexEdit.setText(Converters.bytesToHexWhitespaceDelimited(value))
            }
            updateSaveButtonState()
        }

    // Adds views related to single field value
    private fun addValue(field: Field) {
        val parentLayout = LinearLayout(context)
        parentLayout.orientation = LinearLayout.VERTICAL

        val parentParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        parentParams.setMargins(0, DEFAULT_MARGIN, 0, DEFAULT_MARGIN / 2)
        parentLayout.layoutParams = parentParams

        val valueLayout = addValueLayout()

        val fieldNameView = addValueFieldName(field.name!!, valueLayout.id)
        fieldNameView.gravity = Gravity.CENTER_VERTICAL
        fieldNameView.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.characteristic_list_item_value_label_text_size))

        val fieldUnitView = addValueUnit(field)
        fieldUnitView.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.characteristic_list_item_value_label_text_size))

        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.gravity = Gravity.BOTTOM
        fieldUnitView.layoutParams = layoutParams
        hidableViews.add(fieldUnitView)

        if (!parseProblem && field.reference == null) {
            val format = field.format
            var tmpVal = readNextValue(format!!)

            if (!(format.toLowerCase(Locale.getDefault()) == "utf8s" || format.toLowerCase(Locale.getDefault()) == "utf16s")) {
                val decimalExponentAbs = StrictMath.abs(field.decimalExponent).toInt()
                val divider = 10.0.pow(decimalExponentAbs.toDouble())
                val valDouble = tmpVal.toDouble()
                val valTmp = valDouble / divider
                tmpVal = valTmp.toString()
            } else {
                writeString = true
                tmpVal = tmpVal.replace("\u0000", "")
            }

            if (writeable || writeableWithoutResponse) {
                val fieldValueEdit = addValueEdit(field, tmpVal)
                fieldValueEdit.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.characteristic_list_item_value_text_size))
                fieldValueEdit.gravity = Gravity.CENTER_VERTICAL

                var params = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.5f)
                params.setMargins(8, 0, 0, 0)
                params.gravity = Gravity.CENTER_VERTICAL
                fieldValueEdit.layoutParams = params

                editTexts.add(fieldValueEdit)

                val fieldValue = addFieldName(fieldValueEdit.text.toString()) as TextView
                context?.let { fieldValue.setTextColor(ContextCompat.getColor(it, R.color.silabs_primary_text)) }
                fieldValue.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.characteristic_list_item_value_text_size))
                hidableViews.add(fieldValue)

                valueLayout.addView(fieldValue)

                // dialog field value
                // field name
                val fieldName = addFieldName(field.name!!)
                params = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.5f)
                params.setMargins(0, 0, 8, 0)
                params.gravity = Gravity.CENTER_VERTICAL
                fieldName.layoutParams = params

                // container for editable field value and field name
                val fieldContainer = LinearLayout(context)
                fieldContainer.orientation = LinearLayout.HORIZONTAL

                params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 5, 0, 5)
                fieldContainer.layoutParams = params

                fieldContainer.addView(fieldName)
                fieldContainer.addView(fieldValueEdit)
                fieldContainer.setPadding(0, FIELD_CONTAINER_PADDING_TOP, 0, FIELD_CONTAINER_PADDING_BOTTOM)

                writableFieldsContainer?.addView(fieldContainer)
            } else {
                val fieldValueView = addValueText(tmpVal)
                fieldValueView.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.characteristic_list_item_value_text_size))
                hidableViews.add(fieldValueView)
                valueLayout.addView(fieldValueView)
            }
            updateSaveButtonState()
        }
        valueLayout.addView(fieldUnitView)
        parentLayout.addView(valueLayout)
        parentLayout.addView(fieldNameView)
        valuesLayout.addView(parentLayout)
    }

    // Adds parent layout for normal value
    private fun addValueLayout(): LinearLayout {
        return LinearLayout(context).apply {
            context?.let { setBackgroundColor(ContextCompat.getColor(it, R.color.silabs_white)) }

            val valueLayoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            valueLayoutParams.setMargins(0, 0, DEFAULT_MARGIN, 0)
            valueLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            valueLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL)
            layoutParams = valueLayoutParams

            orientation = LinearLayout.HORIZONTAL
            id = View.generateViewId()
        }
    }

    // Adds unit text view
    private fun addValueUnit(field: Field): TextView {
        val fieldUnitView = TextView(context)
        fieldUnitView.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.characteristic_list_item_value_text_size))
        context?.let { fieldUnitView.setBackgroundColor(ContextCompat.getColor(it, R.color.silabs_white)) }
        val fieldUnitParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        fieldUnitView.layoutParams = fieldUnitParams
        val unit = Engine.instance?.getUnit(field.unit)
        if (unit != null) {
            if (!TextUtils.isEmpty(unit.symbol)) {
                if (unit.fullName.toLowerCase(Locale.getDefault()) == "celsius" || unit.fullName.toLowerCase(Locale.getDefault()) == "fahrenheit") {
                    // this makes sure that the degrees symbol for temperature is displayed correctly
                    fieldUnitView.text = context?.getString(R.string.unit_with_whitespace, HtmlCompat.fromHtml(unit.symbol))
                } else {
                    fieldUnitView.text = context?.getString(R.string.unit_with_whitespace, unit.symbol)
                }
            } else {
                fieldUnitView.text = context?.getString(R.string.unit_with_whitespace, unit.fullName)
            }
        }
        context?.let { fieldUnitView.setTextColor(ContextCompat.getColor(it, R.color.silabs_primary_text)) }
        return fieldUnitView
    }

    private fun isNumberFormat(format: String): Boolean {
        return when (format) {
            "uint8", "uint16", "uint24", "uint32", "uint40", "uint48", "sint8", "sint16", "sint24", "sint32", "sint40", "sint48", "float32", "float64" -> true
            "utf8s", "utf16s" -> false
            else -> false
        }
    }

    // Adds value edit view
    private fun addValueEdit(field: Field, value: String): EditText {
        val fieldValueEdit = EditText(context).apply {
            context?.let { setTextColor(it.getColor(R.color.silabs_primary_text)) }
            setSingleLine()
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.characteristic_list_item_value_text_size))
            setText(value)
            setBackgroundResource(R.drawable.et_custom_color)
            setPadding(0, 0, 0, 0)
        }

        val fieldValueParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        fieldValueParams.gravity = Gravity.CENTER_VERTICAL
        fieldValueParams.leftMargin = FIELD_VALUE_EDIT_LEFT_MARGIN
        fieldValueEdit.layoutParams = fieldValueParams

        var formatLength = Engine.instance?.getFormat(field.format)
        // Bluegiga code had a bug where formatlength = 0 fields were ignored on write
        if (formatLength == 0) {
            formatLength = value.length
        }
        val valArr = ByteArray(formatLength!!)
        if (isNumberFormat(field.format!!)) {
            fieldValueEdit.setRawInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED)
        }
        fieldValueEdit.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (writeString) {
                    val array = fieldValueEdit.text.toString().toByteArray()
                    fillValue(array)
                } else {
                    Arrays.fill(valArr, 0.toByte())
                    val inputVal = fieldValueEdit.text.toString()
                    val decimalExponentAbs = StrictMath.abs(field.decimalExponent).toInt()
                    var inputValMoved = StringBuilder(inputVal)
                    if (decimalExponentAbs != 0) {
                        val index = inputValMoved.indexOf(".")
                        if (index != -1) {
                            when {
                                inputValMoved.length - 1 - index > decimalExponentAbs -> {
                                    inputValMoved = StringBuilder(inputValMoved.toString().replace(".", ""))
                                    inputValMoved = StringBuilder(inputValMoved.substring(0, index + decimalExponentAbs) + "." + inputValMoved.substring(index + decimalExponentAbs))
                                }
                                inputValMoved.length - 1 - index == decimalExponentAbs -> {
                                    inputValMoved = StringBuilder(inputValMoved.toString().replace(".", ""))
                                }
                                else -> {
                                    inputValMoved = StringBuilder(inputValMoved.toString().replace(".", ""))
                                    for (i in inputValMoved.length - index until decimalExponentAbs) {
                                        inputValMoved.append("0")
                                    }
                                }
                            }
                        } else {
                            for (i in 0 until decimalExponentAbs) {
                                inputValMoved.append("0")
                            }
                        }
                    }
                    val pair = Converters.convertStringTo(inputValMoved.toString(), field.format)
                    val newVal = pair.first
                    val inRange = pair.second
                    Log.d("write_val", "Value to write from edittext conversion (hex): " + Converters.bytesToHexWhitespaceDelimited(newVal))
                    for (i in valArr.indices) {
                        if (i < newVal!!.size) {
                            valArr[i] = newVal[i]
                        }
                    }
                    val off = getFieldOffset(field)
                    fieldsInRangeMap[field] = inRange!!
                    if (isNumberFormat(field.format!!)) {
                        fieldsValidMap[field] = isNumeric(inputVal)
                    } else {
                        fieldsValidMap[field] = true
                    }
                    setValue(off, valArr)
                }
                updateSaveButtonState()
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                updateSaveButtonState()
            }

            override fun afterTextChanged(s: Editable) {
                updateSaveButtonState()
            }
        })
        return fieldValueEdit
    }

    private fun fillValue(array: ByteArray) {
        value = ByteArray(array.size)
        System.arraycopy(array, 0, value, 0, array.size)
    }

    private fun updateSaveButtonState() {
        saveValueBtn?.isEnabled = true
        saveValueBtn?.isClickable = true
    }

    // Adds value text view
    private fun addValueText(value: String): TextView {
        return TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.characteristic_list_item_value_text_size))
            text = value

            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)

            context?.let {
                setBackgroundColor(ContextCompat.getColor(it, R.color.silabs_white))
                setTextColor(ContextCompat.getColor(it, R.color.silabs_primary_text))
            }
        }
    }

    // Adds TextView with field name
    private fun addValueFieldName(name: String, leftViewId: Int): TextView {
        return TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.characteristic_list_item_value_label_text_size))
            text = name

            val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                addRule(RelativeLayout.LEFT_OF, leftViewId)
                addRule(RelativeLayout.CENTER_VERTICAL)
                setMargins(0, 0, 0, 15)
            }
            layoutParams = params

            context?.let { it ->
                setBackgroundColor(ContextCompat.getColor(it, R.color.silabs_white))
                setTextColor(ContextCompat.getColor(it, R.color.silabs_subtle_text))
            }
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

    // Get value bits as String from offset to offest+formatLength
    // bits are parsed in order: LSO(least significant octet) ===> MSO (most significant octet)
    // String charAt(0) - least significant bit,
    // String charAt(last) most significant bit.
    private fun getFieldValueAsLsoMsoBitsString(offset: Int, formatLength: Int): String {
        val result = StringBuilder()
        for (i in offset until offset + formatLength) {
            var `val` = value[i].toInt()
            for (j in 0..7) {
                if (`val` and 1 == 1) {
                    result.append(1)
                } else {
                    result.append(0)
                }
                `val` = `val` shr 1
            }
        }
        return result.toString()
    }

    private fun fillStringBuilderWithZeros(count: Int): StringBuilder {
        return StringBuilder().apply {
            for (i in 0 until count) append('0')
        }
    }

    // Convert String of bits where bitsString charAt(0) is least significant
    // to byte array
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

    // Adds views related to bitfield value
    private fun addBitfield(field: Field) {
        if (field.reference == null) {
            val formatLength = Engine.instance?.getFormat(field.format)
            val bitsLength = formatLength!! * 8
            var currentBit = 0
            val valueBits = getFieldValueAsLsoMsoBitsString(offset, formatLength)
            val builder = fillStringBuilderWithZeros(bitsLength)


            // Display read bitfields
            for (bit in field.bitfield?.bits!!) {
                val enumerations = ArrayList<String>()

                // Bits in range startBitIndex to endBitIndex will be replaced with new value for given bitField
                val startBitIndex = currentBit
                val endBitIndex = currentBit + bit.size
                for (enumeration in bit.enumerations!!) {
                    enumerations.add(enumeration.value!!)
                }
                val nameAndValueParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                nameAndValueParams.setMargins(0, DEFAULT_MARGIN, 0, 12 + DEFAULT_MARGIN / 2)
                val nameAndValueContainer = LinearLayout(context)
                nameAndValueContainer.orientation = LinearLayout.VERTICAL
                nameAndValueContainer.layoutParams = nameAndValueParams
                val valueText: View = addValueText(enumerations[getValueInStringBitsRange(startBitIndex, endBitIndex, valueBits)])
                val nameText = addFieldName(bit.name!!)
                nameAndValueContainer.addView(valueText)
                nameAndValueContainer.addView(nameText)
                valuesLayout.addView(nameAndValueContainer)
                hidableViews.add(valueText)
                if (writeable || writeableWithoutResponse) {
                    var params = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.5f)
                    params.gravity = Gravity.CENTER_VERTICAL
                    params.setMargins(8, 0, 0, 0)
                    val spinner = Spinner(context)
                    context?.let { spinner.adapter = ArrayAdapter(it, R.layout.enumeration_spinner_dropdown_item, enumerations) }
                    spinner.layoutParams = params
                    val off = offset
                    spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {

                            // After each spinner selection bits are prepared for characteristic write - value array is updated with selected value
                            setStringBuilderBitsInRange(builder, startBitIndex, endBitIndex, position)
                            val `val` = bitsStringToByteArray(builder.toString(), formatLength)
                            //intToByteArray(Integer.parseInt(builder.toString(), 2), formatLength);
                            setValue(off, `val`)
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
                    val fieldName = addFieldName(bit.name!!)
                    params = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.5f)
                    params.gravity = Gravity.CENTER_VERTICAL
                    params.setMargins(0, 0, 8, 0)
                    fieldName.layoutParams = params
                    val linearLayout = LinearLayout(context)
                    linearLayout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    linearLayout.orientation = LinearLayout.HORIZONTAL
                    linearLayout.addView(fieldName)
                    linearLayout.addView(spinner)
                    writableFieldsContainer?.addView(linearLayout)
                }
                currentBit += bit.size
            }
            offset += formatLength
        }
    }

    private fun initWriteModeView(dialog: Dialog) {
        val writeWithResponseRB = dialog.findViewById<RadioButton>(R.id.write_with_resp_radio_button)
        val writeWithoutResponseRB = dialog.findViewById<RadioButton>(R.id.write_without_resp_radio_button)
        val writeMethodLL = dialog.findViewById<LinearLayout>(R.id.write_method_linear_layout)

        if (writeable) {
            writeWithResponseRB.isChecked = true
            writeWithResponseRB.isChecked = true
            writeWithResponse = true
            writeWithResponseRB.setOnClickListener { writeWithResponse = true }
        } else {
            writeWithResponseRB.isEnabled = false
            writeWithResponseRB.isChecked = false
        }

        if (writeableWithoutResponse) {
            writeWithoutResponseRB.isEnabled = true
            if (!writeWithResponseRB.isChecked) {
                writeWithoutResponseRB.isChecked = true
                writeWithResponse = false
            }
            writeWithoutResponseRB.setOnClickListener { writeWithResponse = false }
        } else {
            writeWithoutResponseRB.isEnabled = false
            writeWithoutResponseRB.isChecked = false
        }

        if (!writeableWithoutResponse && !writeable) {
            writeMethodLL.visibility = View.GONE
        }
    }

    // Adds views related to enumeration value
    // Each enumeration is presented as Spinner view
    private fun addEnumeration(field: Field) {
        if (field.reference == null) {
            val enumerationArray = ArrayList<String>()
            for (en in field.enumerations!!) {
                enumerationArray.add(en.value!!)
            }
            if (!parseProblem) {
                val formatLength = Engine.instance?.getFormat(field.format)
                var pos = 0
                var `val` = 0
                if (field.format?.toLowerCase(Locale.getDefault()) == "16bit") {
                    if (offset == value.size - 1) {
                        // case for when only 8 bits of 16 are sent
                        `val` = value[offset].toInt().and(0xff)
                        `val` = `val` shl 8
                    } else if (offset < value.size - 1) {
                        // for field "Category, last 6 bits of payload are used for sub categories
                        if (field.name == "Category") {
                            val byte1: Int = value[offset].toInt().and(0xff)
                            val byte2: Int = value[offset + 1].toInt().and(0xff)
                            `val` = byte2 shl 8 or byte1
                            `val` = 0xffc0 and `val`
                        } else {
                            // case for when 16 full bits are sent
                            `val` = value[offset].toInt().and(0xff)
                            `val` = `val` shl 8
                            `val` = `val` or (value[offset + 1].toInt().and(0xff))
                        }
                    }
                } else {
                    `val` = readInt(offset, formatLength!!)
                }

                // Bluegiga code was using getFieldOffset() and getting wrong offset
                // this ensures that fields are consistently offset while reading characteristic
                offset += formatLength!!
                if (`val` != 0) {
                    // value was read or notified
                    for (en in field.enumerations!!) {
                        if (en.key == `val`) {
                            break
                        }
                        pos++
                    }
                }
                if (pos >= enumerationArray.size) {
                    pos = 0
                }

                val nameAndValueParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                nameAndValueParams.setMargins(0, DEFAULT_MARGIN, 0, 12 + DEFAULT_MARGIN / 2)

                val nameAndValueContainer = LinearLayout(context)
                nameAndValueContainer.orientation = LinearLayout.VERTICAL
                nameAndValueContainer.layoutParams = nameAndValueParams
                val valueText: View = addValueText(enumerationArray[pos])

                hidableViews.add(valueText)

                val nameText = addFieldName(field.name!!)
                nameAndValueContainer.addView(valueText)
                nameAndValueContainer.addView(nameText)
                valuesLayout.addView(nameAndValueContainer)

                if (writeable || writeableWithoutResponse) {
                    val params = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.5f)
                    params.gravity = Gravity.CENTER_VERTICAL

                    val offset = getFieldOffset(field)
                    val spinner = Spinner(context)
                    context?.let { spinner.adapter = ArrayAdapter(it, R.layout.enumeration_spinner_dropdown_item, enumerationArray) }
                    spinner.layoutParams = params
                    spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                            val key = field.enumerations!![position].key
                            val tmpFormatLength = Engine.instance?.getFormat(field.format)
                            val tmpVal = intToByteArray(key, tmpFormatLength!!)
                            setValue(offset, tmpVal)
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }

                    val fieldName = addFieldName(field.name!!)
                    fieldName.layoutParams = params

                    val linearLayout = LinearLayout(context).apply {
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        orientation = LinearLayout.HORIZONTAL
                        addView(fieldName)
                        addView(spinner)
                    }

                    writableFieldsContainer?.addView(linearLayout)
                }
            }
        }
    }

    // Adds TextView with error info
    // Called when characteristic parsing error occured
    private fun addProblemInfoView() {
        val problemTextView = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.characteristic_list_item_value_text_size))
            context?.let { setBackgroundColor(ContextCompat.getColor(it, R.color.silabs_white)) }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            typeface = Typeface.DEFAULT_BOLD
            text = parsingProblemInfo
            setTextColor(Color.RED)

        }
        valuesLayout.addView(problemTextView)
    }

    // Adds TextView with field name
    private fun addFieldName(name: String): View {
        return TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.characteristic_list_item_value_label_text_size))

            val fieldNameParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            fieldNameParams.gravity = Gravity.CENTER_VERTICAL
            layoutParams = fieldNameParams

            text = name

            context?.let {
                setBackgroundColor(ContextCompat.getColor(it, R.color.silabs_white))
                setTextColor(ContextCompat.getColor(it, R.color.silabs_subtle_text))
            }
        }
    }

    internal inner class WriteCharacteristic : TextView.OnEditorActionListener {
        override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                writeValueToCharacteristic()
                return true
            }
            return false
        }
    }

    private fun isNumeric(string: String): Boolean {
        return try {
            string.toDouble()
            true
        } catch (e: NumberFormatException) {
            false
        }
    }

    private fun getOtaSpecificCharacteristicName(uuid: String): String {
        return when (uuid.toUpperCase(Locale.getDefault())) {
            "F7BF3564-FB6D-4E53-88A4-5E37E0326063" -> "OTA Control Attribute"
            "984227F3-34FC-4045-A5D0-2C581F81A153" -> "OTA Data Attribute"
            "4F4A2368-8CCA-451E-BFFF-CF0E2EE23E9F" -> "AppLoader version"
            "4CC07BCF-0868-4B32-9DAD-BA4CC41E5316" -> "OTA version"
            "25F05C0A-E917-46E9-B2A5-AA2BE1245AFE" -> "Gecko Bootloader version"
            "0D77CC11-4AC1-49F2-BFA9-CD96AC7A92F8" -> "Application version"
            else -> getString(R.string.unknown_characteristic_label)
        }
    }

    companion object {
        private const val FIELD_CONTAINER_PADDING_TOP = 15
        private const val FIELD_CONTAINER_PADDING_BOTTOM = 15
        private const val FIELD_VALUE_EDIT_LEFT_MARGIN = 15
        private const val DEFAULT_MARGIN = 5
        private const val EDIT_NOT_CLEAR_ID = 1000
        private const val REFRESH_INTERVAL = 500
        private const val TYPE_FLOAT = "FLOAT"
        private const val TYPE_SFLOAT = "SFLOAT"
        private const val TYPE_FLOAT_32 = "float32"
        private const val TYPE_FLOAT_64 = "float64"
        private const val HEX_ID = "HEX"
        private const val ASCII_ID = "ASCII"
        private const val DECIMAL_ID = "DECIMAL"
    }
}