package com.siliconlabs.bledemo.Browser.Fragment

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.siliconlabs.bledemo.Bluetooth.DataTypes.*
import com.siliconlabs.bledemo.Bluetooth.Parsing.Common
import com.siliconlabs.bledemo.Bluetooth.Parsing.Engine
import com.siliconlabs.bledemo.Bluetooth.Services.BluetoothService
import com.siliconlabs.bledemo.Browser.Activities.DeviceServicesActivity
import com.siliconlabs.bledemo.Browser.Dialogs.CharacteristicWriteDialog
import com.siliconlabs.bledemo.Browser.Utils.FieldViewHelper
import com.siliconlabs.bledemo.Browser.Views.*
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.utils.Converters
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList

open class FragmentCharacteristicDetail : Fragment() {

    var isRemote: Boolean = true
    var writeType = WriteType.REMOTE_WRITE
    private var currRefreshInterval = REFRESH_INTERVAL

    private val rawValueViews = ArrayList<EditText>()
    private val hidableViews = ArrayList<View>()
    private var rawValueData = ArrayList<String>()
    private var handler = Handler()

    private var isRawValue = false
    private var parseProblem = false
    var notificationsEnabled = false
    var indicationsEnabled = false
    var displayWriteDialog = false

    private val postLoadValueViews = Runnable { loadValueViews() }
    private val postDisplayValues = Runnable { displayValues() }

    private lateinit var parsingProblemInfo: String
    private var previousValue: ByteArray = ByteArray(0)

    var mBluetoothCharact: BluetoothGattCharacteristic? = null
    private var mCharact: Characteristic? = null
    private var mGattService: BluetoothGattService? = null

    private var value: ByteArray = ByteArray(0)
    private var offset = 0

    private lateinit var valuesLayout: LinearLayout
    private var editableFieldsDialog: CharacteristicWriteDialog? = null

    private var fieldViewHelper: FieldViewHelper? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_characteristic_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("Characteristic = ${mBluetoothCharact?.uuid.toString()}, instanceId = ${mBluetoothCharact?.instanceId}")

        valuesLayout = view.findViewById(R.id.values_layout)

        mCharact = Engine.getCharacteristic(mBluetoothCharact?.uuid)
        fieldViewHelper = FieldViewHelper(mCharact)

        if (mCharact == null || mCharact?.fields == null || mCharact?.name == REG_CERT_DATA_LIST_NAME) {
            isRawValue = true
        }

        setupRefreshInterval()

        if (!isRawValue) {
            prepareValueData()
        }
        loadValueViews()

        if (displayWriteDialog) {
            createWriteDialog(writeType)
        }
    }

    // Builds activity UI based on characteristic content
    private fun loadValueViews() {
        if (!isRawValue) {
            if (parseProblem || !addNormalValue()) {
                addInvalidValue()
            }
        } else {
            RawValueView(context, value).createViewForRead(true, viewHandler)
        }
    }

    fun createWriteDialog(writeType: WriteType) {
        if (!isRawValue && value.isEmpty()) prepareValueData()

        context?.let { editableFieldsDialog = CharacteristicWriteDialog(
                it,
                writeDialogListener,
                writeType,
                mCharact,
                value
        ) }

        editableFieldsDialog?.apply {
            fillViewsWithValues(
                    mGattService,
                    mBluetoothCharact,
                    mCharact,
                    writeType
            )
            displayWriteMethods(
                    Common.isSetProperty(Common.PropertyType.WRITE, mBluetoothCharact!!.properties),
                    Common.isSetProperty(Common.PropertyType.WRITE_NO_RESPONSE, mBluetoothCharact!!.properties)
            )
            loadValueViews(isRawValue, parseProblem)
        }

        showCharacteristicWriteDialog()
    }

    fun onActionDataWrite(uuid: String, status: Int) {
        if (mBluetoothCharact?.uuid.toString() != uuid) {
            return
        }
        activity?.runOnUiThread {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Toast.makeText(activity, getText(R.string.characteristic_write_success), Toast.LENGTH_SHORT).show()
                editableFieldsDialog?.dismiss()
            } else {
                Toast.makeText(
                        activity,
                        getText(R.string.characteristic_write_fail),
                        Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun onActionDataAvailable(uuidCharacteristic: String, withNotification: Boolean) {
        if (uuidCharacteristic == mBluetoothCharact?.uuid.toString()) {
            updateValueView(withNotification)
        }
    }

    private fun updateValueView(withNotification: Boolean) {
        activity?.runOnUiThread {

            if (withNotification) {
                offset = 0
                value = mBluetoothCharact?.value?.clone() ?: byteArrayOf()
                valuesLayout.removeAllViews()
                loadValueViews()
            }
            else {
                if (currRefreshInterval >= REFRESH_INTERVAL) {
                    currRefreshInterval = 0
                    offset = 0
                    value = mBluetoothCharact?.value?.clone() ?: byteArrayOf()

                    if (value.contentEquals(previousValue)) {
                        hideValues()
                        handler.removeCallbacks(postDisplayValues)
                        handler.postDelayed(postDisplayValues, 50)
                    } else {
                        valuesLayout.removeAllViews()
                        handler.removeCallbacks(postLoadValueViews)
                        handler.postDelayed(postLoadValueViews, 50)
                    }
                }
            }
            if (value.isNotEmpty()) previousValue = value.clone()
        }
    }

    fun setmBluetoothCharact(mBluetoothCharact: BluetoothGattCharacteristic?) {
        this.mBluetoothCharact = mBluetoothCharact
    }

    fun setmService(service: BluetoothGattService?) {
        mGattService = service
    }

    private fun saveValueInCharacteristic(newValue: ByteArray) {
        Timber.d("New value to set = ${newValue.contentToString()}")
        value = newValue
        mBluetoothCharact?.value = newValue

        if (isRemote) {
            mBluetoothCharact?.let {
                it.writeType = editableFieldsDialog?.getChosenWriteMethod() ?: BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                (activity as DeviceServicesActivity).bluetoothGatt?.writeCharacteristic(it)
            }
        } else {
            updateValueView(false)
        }

        editableFieldsDialog?.dismiss()
    }

    private fun notifyClients(characteristic: BluetoothGattCharacteristic, confirm: Boolean) {
        (activity as DeviceServicesActivity).bluetoothService?.let {
            getClients(it, characteristic, confirm).forEach { device ->
                it.bluetoothGattServer?.notifyCharacteristicChanged(
                        device,
                        characteristic,
                        confirm)
            }
        }
    }

    private fun getClients(service: BluetoothService,
                           characteristic: BluetoothGattCharacteristic,
                           confirm: Boolean
    ) : Collection<BluetoothDevice> {
        return if (confirm)
            service.getClientsToIndicate(characteristic.uuid)
        else service.getClientsToNotify(characteristic.uuid)
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

    // Count time that is used to preventing from very fast refreshing view
    private fun setupRefreshInterval() {
        val timer = Timer()
        val updateRefreshInterval = object : TimerTask() {
            override fun run() {
                currRefreshInterval += REFRESH_INTERVAL
            }
        }
        timer.scheduleAtFixedRate(updateRefreshInterval, 0, REFRESH_INTERVAL.toLong())
    }

    // Builds activity UI in tree steps:
    // a) add views based on characteristic content without setting values
    // b) add problem info view
    // c) add raw views (hex, ASCII, decimal) with setting values
    private fun addInvalidValue() {
        valuesLayout.removeAllViews()
        addNormalValue()
        addProblemInfoView()
        RawValueView(context, value).createViewForRead(true, viewHandler)
    }

    // Only called when characteristic is standard Bluetooth characteristic
    // Build activity UI based on characteristic content and also take account
    // of field requirements
    private fun addNormalValue(): Boolean {
        mCharact?.fields?.forEach {
            try {
                addField(it)
            } catch (ex: Exception) {
                ex.printStackTrace()
                parsingProblemInfo = prepareParsingProblemInfo(mCharact)
                parseProblem = true
                return false
            }
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
                expectedBytes += Engine.getFormat(field.format)
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

    private val viewHandler = object : ValueView.ViewHandler {
        override fun handleFieldView(fieldViewContainer: View, fieldValueView: View) {
            valuesLayout.addView(fieldViewContainer)
            if (!parseProblem) {
                hidableViews.add(fieldValueView)
            }
        }

        override fun handleRawValueViews(rawValuesContainer: View, rawValueViews: ArrayList<EditText>) {
            valuesLayout.addView(rawValuesContainer)
            this@FragmentCharacteristicDetail.rawValueViews.addAll(rawValueViews)
        }
    }

    // Add single field
    private fun addField(field: Field) {
        if (isFieldPresent(field)) {
            if (field.referenceFields?.size!! > 0) {
                for (subField in field.referenceFields!!) {
                    addField(subField)
                }
            } else {
                if (field.reference == null) {
                    val currentValue = value
                    val currentOffset = offset
                    var currentRange = currentValue.copyOfRange(currentOffset, currentOffset + field.getSizeInBytes())

                    if (field.bitfield != null) {
                        BitFieldView(context, field, currentRange).createViewForRead(!parseProblem, viewHandler)
                        offset += field.getSizeInBytes()
                    }
                    else if (field.enumerations != null && field.enumerations?.size!! > 0) {
                        if (field.isNibbleFormat()) {
                            handleNibbleRead(field, currentRange[0])
                        } else {
                            EnumerationView(context, field, currentRange).createViewForRead(!parseProblem, viewHandler)
                            offset += field.getSizeInBytes()
                        }
                    }
                    else {
                        var offsetShift = field.getSizeInBytes()
                        if (offsetShift == 0) {
                            offsetShift = value.size - currentOffset // for "variable", "utf8", "utf16" format types
                            currentRange = currentValue.copyOfRange(currentOffset, currentOffset + offsetShift)
                        }
                        NormalValueView(context, field, currentRange).createViewForRead(!parseProblem, viewHandler)
                        offset += offsetShift
                    }
                }
            }
        } else {
            offset += field.getSizeInBytes()
        }
    }

    private fun handleNibbleRead(field: Field, data: Byte) {
        val firstNibble = Converters.byteToUnsignedInt(data) shr 4
        val secondNibble = Converters.byteToUnsignedInt(data) and 0x0f

        if (fieldViewHelper?.isFirstNibbleInByte(field) == true) {
            EnumerationView(context, field, byteArrayOf(firstNibble.toByte())).createViewForRead(!parseProblem, viewHandler)
        } else {
            EnumerationView(context, field, byteArrayOf(secondNibble.toByte())).createViewForRead(!parseProblem, viewHandler)
            offset += field.getSizeInBytes()
        }
    }

    // Initializes byte array with empty characteristic content
    private fun prepareValueData() {
        val size = mCharact.size()
        if (size != 0) {
            value = ByteArray(size)
        }
    }

    // Returns characteristic size in bytes
    private fun Characteristic?.size(): Int {
        return this?.fields.orEmpty().sumBy { it.getSizeInBytes() }
    }

    // Checks if field is present based on it's requirements and bitfield settings
    private fun isFieldPresent(field: Field): Boolean {
        if (parseProblem) {
            return true
        }

        return fieldViewHelper?.isFieldPresent(field, value) ?: false
    }

    private fun showCharacteristicWriteDialog() {
        val propertiesContainer = editableFieldsDialog?.findViewById<LinearLayout>(R.id.picker_dialog_properties_container)
        propertiesContainer?.let { initPropertiesForEditableFieldsDialog(it) }

        editableFieldsDialog?.show()
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

    // Adds TextView with error info
    // Called when characteristic parsing error occurred
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

    private val writeDialogListener = object : CharacteristicWriteDialog.WriteDialogListener {
        override fun onNewValueSet(newValue: ByteArray, writeType: WriteType) {
            saveValueInCharacteristic(newValue)
            when (writeType) {
                WriteType.LOCAL_INDICATE -> notifyClients(mBluetoothCharact!!, true)
                WriteType.LOCAL_NOTIFY -> notifyClients(mBluetoothCharact!!, false)
                else -> { }
            }
        }
    }

    enum class WriteType {
        REMOTE_WRITE,
        LOCAL_WRITE,
        LOCAL_INDICATE, /* Write locally and and indicate clients */
        LOCAL_NOTIFY /* Write locally and notify clients */
    }

    companion object {
        private const val REFRESH_INTERVAL = 500
        private const val REG_CERT_DATA_LIST_NAME = "IEEE 11073-20601 Regulatory Certification Data List"

    }
}
