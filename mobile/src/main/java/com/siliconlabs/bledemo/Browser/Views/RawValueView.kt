package com.siliconlabs.bledemo.Browser.Views

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.utils.Converters
import com.siliconlabs.bledemo.utils.StringUtils

open class RawValueView(context: Context?,
                        fieldValue: ByteArray
) : ValueView(context, fieldValue) {

    private val writableFieldsForDialog = View.inflate(context, R.layout.characteristic_value, null)

    private val hexEdit = writableFieldsForDialog.findViewById<EditText>(R.id.hex_edit)
    private val asciiEdit = writableFieldsForDialog.findViewById<EditText>(R.id.ascii_edit)
    private val decimalEdit = writableFieldsForDialog.findViewById<EditText>(R.id.decimal_edit)

    var onEditorActionListener: TextView.OnEditorActionListener? = null
    var valueListener: ValueListener? = null


    override fun createViewForRead(isParsedSuccessfully: Boolean, viewHandler: ViewHandler) {
        val readableFieldsForInline = View.inflate(context, R.layout.characteristic_value_read_only, null)
        val hex = readableFieldsForInline.findViewById<EditText>(R.id.hex_readonly)
        val ascii = readableFieldsForInline.findViewById<EditText>(R.id.ascii_readonly)
        val decimal = readableFieldsForInline.findViewById<EditText>(R.id.decimal_readonly)

        val hexCopyIV = readableFieldsForInline.findViewById<ImageView>(R.id.hex_copy)
        val asciiCopyIV = readableFieldsForInline.findViewById<ImageView>(R.id.ascii_copy)
        val decimalCopyIV = readableFieldsForInline.findViewById<ImageView>(R.id.decimal_copy)

        hex.setText(Converters.bytesToHexWhitespaceDelimited(fieldValue))
        ascii.setText(Converters.getAsciiValue(fieldValue))
        decimal.setText(Converters.getDecimalValue(fieldValue))

        setCopyListener(hex, hexCopyIV)
        setCopyListener(ascii, asciiCopyIV)
        setCopyListener(decimal, decimalCopyIV)

        viewHandler.handleRawValueViews(readableFieldsForInline, arrayListOf(hex, ascii, decimal))
    }

    override fun createViewForWrite(fieldOffset: Int, valueListener: ValueListener) {
        this.valueListener = valueListener

        val hexPasteIV = writableFieldsForDialog.findViewById<ImageView>(R.id.hex_paste)
        val asciiPasteIV = writableFieldsForDialog.findViewById<ImageView>(R.id.ascii_paste)
        val decimalPasteIV = writableFieldsForDialog.findViewById<ImageView>(R.id.decimal_paste)

        valueListener.addEditTexts(arrayListOf(hexEdit, asciiEdit, decimalEdit))

        val hexWatcher = hexTextWatcher
        val decWatcher = decTextWatcher
        val asciiWatcher = asciiTextWatcher

        val hexListener = hexFocusChangeListener
        hexEdit.onFocusChangeListener = hexListener

        hexEdit.setOnEditorActionListener(onEditorActionListener)
        asciiEdit.setOnEditorActionListener(onEditorActionListener)
        decimalEdit.setOnEditorActionListener(onEditorActionListener)
        hexEdit.addTextChangedListener(hexWatcher)
        asciiEdit.addTextChangedListener(asciiWatcher)
        decimalEdit.addTextChangedListener(decWatcher)

        setPasteListener(hexEdit, hexPasteIV, HEX_ID)
        setPasteListener(asciiEdit, asciiPasteIV, ASCII_ID)
        setPasteListener(decimalEdit, decimalPasteIV, DECIMAL_ID)

        valueListener.handleFieldView(writableFieldsForDialog)
    }

    private fun setCopyListener(copyFromET: EditText?, copyIV: ImageView) {
        copyIV.setOnClickListener {
            val clipboardManager = context?.getSystemService(ClipboardManager::class.java)
            val clip = ClipData.newPlainText("characteristic-value", copyFromET?.text.toString())
            if (clipboardManager != null) {
                clipboardManager.setPrimaryClip(clip)
                Toast.makeText(context, R.string.Copied_to_clipboard, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val hexTextWatcher: TextWatcher
        get() = object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (hexEdit.hasFocus()) {
                    val textLength = hexEdit.text.toString().length
                    val newValue: ByteArray
                    if (textLength % 2 == 1) {
                        var temp = hexEdit.text.toString()
                        temp = temp.substring(0, textLength - 1) + "0" + temp[textLength - 1]
                        newValue = Converters.hexToByteArray(temp.replace("\\s+".toRegex(), ""))
                    } else {
                        newValue = Converters.hexToByteArray(hexEdit.text.toString().replace("\\s+".toRegex(), ""))
                    }
                    asciiEdit.setText(Converters.getAsciiValue(newValue))
                    decimalEdit.setText(Converters.getDecimalValue(newValue))

                    valueListener?.onRawValueChanged(newValue)
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable) {}
        }

    private val decTextWatcher: TextWatcher
        get() = object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (decimalEdit.hasFocus()) {
                    if (isDecValueValid(decimalEdit.text.toString())) {
                        val newValue = Converters.decToByteArray(decimalEdit.text.toString())
                        hexEdit.setText(Converters.bytesToHexWhitespaceDelimited(newValue))
                        asciiEdit.setText(Converters.getAsciiValue(newValue))
                        valueListener?.onRawValueChanged(newValue)
                    } else {
                        decimalEdit.setText(decimalEdit.text.toString().substring(0,
                                decimalEdit.text.length - 1))
                        decimalEdit.setSelection(decimalEdit.text.length)
                        Toast.makeText(context, R.string.invalid_dec_value, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable) {}
        }

    private val asciiTextWatcher: TextWatcher
        get() = object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (asciiEdit.hasFocus()) {
                    val newValue = asciiEdit.text.toString().toByteArray()
                    hexEdit.setText(Converters.bytesToHexWhitespaceDelimited(newValue))
                    decimalEdit.setText(Converters.getDecimalValue(newValue))

                    valueListener?.onRawValueChanged(newValue)
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable) {}
        }

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
                val value = Converters.hexToByteArray(hexValue)
                hexEdit.setText(Converters.bytesToHexWhitespaceDelimited(value))
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
                        if (isHexStringCorrect(text)) {
                            pasteToET?.setText(text)
                        } else Toast.makeText(context, R.string.Incorrect_data_format, Toast.LENGTH_SHORT).show()
                    }
                    ASCII_ID -> pasteToET?.setText(text)
                    DECIMAL_ID -> if (isDecimalCorrect(text.trim())) {
                        pasteToET?.setText(text)
                    } else Toast.makeText(context, R.string.Incorrect_data_format, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun isDecValueValid(decValue: String): Boolean {
        val value = decValue.toCharArray()
        val valLength = value.size
        return if (decValue.length < 4) {
            true
        } else {
            value[valLength - 1] == ' ' || value[valLength - 2] == ' ' || value[valLength - 3] == ' ' || value[valLength - 4] == ' '
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

    companion object {
        private const val HEX_ID = "HEX"
        private const val ASCII_ID = "ASCII"
        private const val DECIMAL_ID = "DECIMAL"
    }


}