package com.siliconlabs.bledemo.Browser.Dialogs

import android.app.Dialog
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.view.View
import android.view.Window
import android.widget.*
import com.siliconlabs.bledemo.Bluetooth.DataTypes.Characteristic
import com.siliconlabs.bledemo.Bluetooth.Parsing.Common
import com.siliconlabs.bledemo.Browser.Fragment.FragmentCharacteristicDetail
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.utils.UuidUtils

class CharacteristicWriteDialog(
        context: Context,
        private val listener: ButtonClickListener,
        private val writeType: FragmentCharacteristicDetail.WriteType
) : Dialog(context) {

    private var serviceNameTextView: TextView? = null
    private var characteristicNameTextView: TextView? = null
    private var uuidTextView: TextView? = null

    var writableFieldsContainer: LinearLayout? = null
    private var writeMethodRadioGroup: RadioGroup? = null

    var btnSave: Button? = null
    var btnClear: Button? = null
    var ivClose: ImageView? = null


    init {
        setWindow()
        findViews()
        setListeners()
    }

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

        btnSave = findViewById(R.id.save_btn)
        btnClear = findViewById(R.id.clear_btn)
        ivClose = findViewById(R.id.image_view_close)
    }

    private fun setListeners() {
        btnSave?.setOnClickListener { listener.onSaveClicked(writeType) }
        btnClear?.setOnClickListener { listener.onClearClicked() }
        ivClose?.setOnClickListener { dismiss() }
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

        serviceNameTextView?.text = serviceName
        characteristicNameTextView?.text = characteristicName
        uuidTextView?.text = characteristicUuid

        writeType.let {
            if (it != FragmentCharacteristicDetail.WriteType.REMOTE_WRITE) {
                writeMethodRadioGroup?.visibility = View.GONE
                btnSave?.text = when (it) {
                    FragmentCharacteristicDetail.WriteType.LOCAL_WRITE -> context.getString(R.string.button_save)
                    FragmentCharacteristicDetail.WriteType.LOCAL_INDICATE -> context.getString(R.string.button_indicate)
                    FragmentCharacteristicDetail.WriteType.LOCAL_NOTIFY -> context.getString(R.string.button_notify)
                    else -> { context.getString(R.string.button_save)}
                }
            } else {
                writeMethodRadioGroup?.visibility = View.VISIBLE
            }

        }
    }


    interface ButtonClickListener {
        fun onSaveClicked(writeType: FragmentCharacteristicDetail.WriteType)
        fun onClearClicked()
    }
}