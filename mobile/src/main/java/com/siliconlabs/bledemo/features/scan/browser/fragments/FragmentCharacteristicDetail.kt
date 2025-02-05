package com.siliconlabs.bledemo.features.scan.browser.fragments

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock.uptimeMillis
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.siliconlabs.bledemo.bluetooth.data_types.*
import com.siliconlabs.bledemo.bluetooth.parsing.Engine
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService
import com.siliconlabs.bledemo.features.scan.browser.activities.DeviceServicesActivity
import com.siliconlabs.bledemo.features.scan.browser.dialogs.CharacteristicWriteDialog
import com.siliconlabs.bledemo.features.scan.browser.utils.FieldViewHelper
import com.siliconlabs.bledemo.features.scan.browser.views.*
import com.siliconlabs.bledemo.features.scan.browser.utils.GlucoseManagement
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.utils.Converters
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList

@SuppressLint("MissingPermission")
open class FragmentCharacteristicDetail : Fragment() {

    var isRemote: Boolean = true
    var writeType = WriteType.REMOTE_WRITE
    private var currRefreshInterval = REFRESH_INTERVAL
    private var lastNotificationTime = uptimeMillis()

    private val rawValueViews = ArrayList<EditText>()
    private val hidableViews = ArrayList<View>()
    private var rawValueData = ArrayList<String>()
    private var handler = Handler(Looper.getMainLooper())

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


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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

        editableFieldsDialog = CharacteristicWriteDialog(
            writeDialogListener,
            writeType,
            mCharact,
            value,
            mGattService,
            mBluetoothCharact,
            isRawValue,
            parseProblem
        ).also {
            it.show(parentFragmentManager, "dialog_characteristic_write")
        }
    }

    fun onActionDataWrite(uuid: String, status: Int) {
        if (mBluetoothCharact?.uuid.toString() != uuid) {
            return
        }
        activity?.runOnUiThread {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Toast.makeText(
                    activity,
                    getText(R.string.characteristic_write_success),
                    Toast.LENGTH_SHORT
                ).show()
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
        if (withNotification) {
            offset = 0
            value = mBluetoothCharact?.value?.clone() ?: byteArrayOf()
            if (uptimeMillis() - lastNotificationTime >= MIN_NOTIFICATION_UPDATE_INTERVAL) {
                activity?.runOnUiThread {
                    valuesLayout.removeAllViews()
                    loadValueViews()
                    if (value.isNotEmpty()) previousValue = value.clone()
                }
                lastNotificationTime = uptimeMillis()
            } else {
                if (value.isNotEmpty()) previousValue = value.clone()
            }
        } else {
            if (currRefreshInterval >= REFRESH_INTERVAL) {
                currRefreshInterval = 0
                offset = 0
                value = mBluetoothCharact?.value?.clone() ?: byteArrayOf()

                activity?.runOnUiThread {
                    if (value.contentEquals(previousValue)) {
                        hideValues()
                        handler.removeCallbacks(postDisplayValues)
                        handler.postDelayed(postDisplayValues, 50)
                    } else {
                        valuesLayout.removeAllViews()
                        handler.removeCallbacks(postLoadValueViews)
                        handler.postDelayed(postLoadValueViews, 50)
                    }
                    if (value.isNotEmpty()) previousValue = value.clone()
                }
            } else {
                if (value.isNotEmpty()) previousValue = value.clone()
            }
        }
    }

    fun setmBluetoothCharact(mBluetoothCharact: BluetoothGattCharacteristic?) {
        this.mBluetoothCharact = mBluetoothCharact
    }

    fun setmService(service: BluetoothGattService?) {
        mGattService = service
    }

    private fun saveValueInCharacteristic(newValue: ByteArray) {
        value = newValue
        mBluetoothCharact?.value = newValue

        if (isRemote) {
            mBluetoothCharact?.let {
                it.writeType = editableFieldsDialog?.getChosenWriteMethod()
                    ?: BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                (activity as DeviceServicesActivity).bluetoothGatt?.writeCharacteristic(it)
                println("statusPropertyDetail :${it.writeType}")
                if(it.writeType != 1){
                    //UPDATE ONLY FOR WRITE PROPERTY
                    //DO NOT UPDATE FOR WRITE WITHOUT RESPONSE PROPERTY
                    updateValueView(false)
                }


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
                    confirm
                )
            }
        }
    }

    private fun getClients(
        service: BluetoothService,
        characteristic: BluetoothGattCharacteristic,
        confirm: Boolean
    ): Collection<BluetoothDevice> {
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
            Timber.d("HERE, field = ${it.name}")
            try {
                if (GlucoseManagement.isRecordAccessControlPoint(mCharact) && it.name == "Operand") {
                    it.format =
                        if (GlucoseManagement.isNumberOfRecordsResponse(
                                mBluetoothCharact,
                                value
                            )
                        ) "16bit"
                        else "variable"
                }
                addField(it)
                if (GlucoseManagement.isCgmSpecificOpsControlPoint(mCharact) && it.name == "Operand") {
                    return true
                }
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

        override fun handleRawValueViews(
            rawValuesContainer: View,
            rawValueViews: ArrayList<EditText>
        ) {
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
                    val fieldSize = calculateFieldSize(field)
                    val currentRange =
                        currentValue.copyOfRange(currentOffset, currentOffset + fieldSize)

                    if (GlucoseManagement.isNumberOfRecordsResponse(
                            mBluetoothCharact,
                            value
                        ) && field.name == "Operand"
                    ) {
                        handleNumberOfRecordsView(field, fieldSize, currentRange.copyOfRange(0, 1))
                        return
                    }

                    if (field.bitfield != null) {
                        BitFieldView(context, field, currentRange).createViewForRead(
                            !parseProblem,
                            viewHandler
                        )
                        offset += field.getSizeInBytes()
                    } else if (field.enumerations != null && field.enumerations?.size!! > 0) {
                        if (field.isNibbleFormat()) {
                            handleNibbleRead(field, currentRange[0])
                        } else {
                            EnumerationView(
                                context,
                                field,
                                currentRange
                            ).createViewForRead(!parseProblem, viewHandler)
                            offset += field.getSizeInBytes()
                        }
                    } else {
                        NormalValueView(
                            context,
                            field,
                            currentRange
                        ).createViewForRead(!parseProblem, viewHandler)
                        offset += fieldSize
                    }
                }
            }
        } else {
            offset += field.getSizeInBytes()
        }
    }

    private fun handleNumberOfRecordsView(field: Field, fieldSize: Int, currentRange: ByteArray) {
        NormalValueView(context, field, currentRange).createViewForRead(!parseProblem, viewHandler)
        offset += fieldSize
    }

    private fun handleNibbleRead(field: Field, data: Byte) {
        val value =
            if (field.isMostSignificantNibble()) Converters.byteToUnsignedInt(data) shr 4
            else Converters.byteToUnsignedInt(data) and 0x0f
        EnumerationView(
            context,
            field,
            byteArrayOf(value.toByte())
        ).createViewForRead(!parseProblem, viewHandler)

        if (!field.isFirstNibbleInSchema()) offset += field.getSizeInBytes()
    }

    private fun calculateFieldSize(field: Field): Int {
        return if (field.getSizeInBytes() != 0) {
            field.getSizeInBytes()
        } else when (field.format) {
            "utf8s", "utf16s" -> value.size - offset
            "variable" -> field.getVariableFieldLength(mCharact, value)
            else -> 0
        }
    }

    // Initializes byte array with empty characteristic content
    private fun prepareValueData() {
        val size = mCharact.size()
        if (size != 0) {
            value = ByteArray(size)
        }
        if (GlucoseManagement.isRecordAccessControlPoint(mCharact)) {
            value = ByteArray(4)
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


    // Adds TextView with error info
    // Called when characteristic parsing error occurred
    private fun addProblemInfoView() {
        val problemTextView = TextView(context).apply {
            setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(R.dimen.characteristic_list_item_value_text_size)
            )
            context?.let { setBackgroundColor(ContextCompat.getColor(it, R.color.silabs_white)) }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
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
                else -> {}
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
        private const val MIN_NOTIFICATION_UPDATE_INTERVAL = 200
        private const val REG_CERT_DATA_LIST_NAME =
            "IEEE 11073-20601 Regulatory Certification Data List"
        const val CGM_SPECIFIC_OPS_CONTROL_POINT_UUID = "2aac"
    }
}
