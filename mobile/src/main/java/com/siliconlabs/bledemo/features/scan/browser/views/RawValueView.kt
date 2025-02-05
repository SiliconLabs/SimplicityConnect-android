package com.siliconlabs.bledemo.features.scan.browser.views

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.*
import com.siliconlabs.bledemo.utils.Converters
import com.siliconlabs.bledemo.utils.StringUtils

open class RawValueView(
    context: Context?,
    fieldValue: ByteArray
) : ValueView(context, fieldValue) {

    private lateinit var _writableFieldsBinding: CharacteristicRawValuesWriteModeBinding

    var onEditorActionListener: TextView.OnEditorActionListener? = null
    var valueListener: ValueListener? = null


    override fun createViewForRead(isParsedSuccessfully: Boolean, viewHandler: ViewHandler) {
        CharacteristicRawValuesReadModeBinding.inflate(LayoutInflater.from(context)).apply {
            hexReadonly.etCharValueReadMode.apply {
                println("HEX VALUE ${Converters.bytesToHexWhitespaceDelimited(fieldValue)}")
                setText(Converters.bytesToHexWhitespaceDelimited(fieldValue))
                hint = context?.getString(R.string.hex)
                isEnabled = false
            }
            asciiReadonly.etCharValueReadMode.apply {
                println("ASCII VALUE ${Converters.getAsciiValue(fieldValue)}")

                setText(Converters.getAsciiValue(fieldValue))
                hint = context?.getString(R.string.ascii)
                isEnabled = false
            }
            decimalReadonly.etCharValueReadMode.apply {
                println("DECIMAL VALUE ${Converters.getDecimalValue(fieldValue)}")
                setText(Converters.getDecimalValue(fieldValue))
                hint = context?.getString(R.string.decimal)
                isEnabled = false
            }

            setCopyListener(hexReadonly)
            setCopyListener(asciiReadonly)
            setCopyListener(decimalReadonly)

            viewHandler.handleRawValueViews(
                root, arrayListOf(
                    hexReadonly.etCharValueReadMode,
                    asciiReadonly.etCharValueReadMode,
                    decimalReadonly.etCharValueReadMode
                )
            )
        }
    }

    override fun createViewForWrite(fieldOffset: Int, valueListener: ValueListener) {
        this.valueListener = valueListener

        _writableFieldsBinding =
            CharacteristicRawValuesWriteModeBinding.inflate(LayoutInflater.from(context)).apply {

                hexWrite.etCharValueWriteMode.apply {
                    onFocusChangeListener = hexFocusChangeListener
                    setOnEditorActionListener(onEditorActionListener)
                    addTextChangedListener(hexTextWatcher)
                    keyListener = DigitsKeyListener.getInstance("0123456789ABCDEFabcdef")
                    setRawInputType(InputType.TYPE_CLASS_TEXT)
                    hint = context?.getString(R.string.hex)
                    isEnabled = true

                }
                asciiWrite.etCharValueWriteMode.apply {
                    setOnEditorActionListener(onEditorActionListener)
                    addTextChangedListener(asciiTextWatcher)
                    hint = context?.getString(R.string.ascii)
                    isEnabled = true

                }
                decimalWrite.etCharValueWriteMode.apply {
                    setOnEditorActionListener(onEditorActionListener)
                    addTextChangedListener(decTextWatcher)
                    keyListener = DigitsKeyListener.getInstance("0123456789 ")
                    setRawInputType(InputType.TYPE_CLASS_TEXT)
                    hint = context?.getString(R.string.decimal)
                    isEnabled = true
                }

                setPasteListener(hexWrite, HEX_ID)
                setPasteListener(asciiWrite, ASCII_ID)
                setPasteListener(decimalWrite, DECIMAL_ID)

                valueListener.addEditTexts(
                    arrayListOf(
                        hexWrite.etCharValueWriteMode,
                        asciiWrite.etCharValueWriteMode,
                        decimalWrite.etCharValueWriteMode
                    )
                )
                valueListener.handleFieldView(root)
            }
    }

    private fun setCopyListener(binding: CharacteristicRawValueReadModeBinding) {
        binding.ivCopyCharValueReadMode.setOnClickListener {
            val clipboardManager = context?.getSystemService(ClipboardManager::class.java)
            val clip = ClipData.newPlainText(
                "characteristic-value", binding.etCharValueReadMode.text.toString()
            )
            if (clipboardManager != null) {
                clipboardManager.setPrimaryClip(clip)
                Toast.makeText(context, R.string.Copied_to_clipboard, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val hexTextWatcher: TextWatcher
        get() = object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                _writableFieldsBinding.apply {
                    val hexEdit = hexWrite.etCharValueWriteMode

                    if (hexEdit.hasFocus()) {
                        val textLength = hexEdit.text.toString().length
                        val newValue: ByteArray
                        if (textLength % 2 == 1) {
                            var temp = hexEdit.text.toString()
                            temp = temp.substring(0, textLength - 1) + "0" + temp[textLength - 1]
                            newValue = Converters.hexToByteArray(temp.replace("\\s+".toRegex(), ""))
                        } else {
                            newValue = Converters.hexToByteArray(
                                hexEdit.text.toString().replace("\\s+".toRegex(), "")
                            )
                        }
                        asciiWrite.etCharValueWriteMode.setText(Converters.getAsciiValue(newValue))
                        decimalWrite.etCharValueWriteMode.setText(
                            Converters.getDecimalValue(
                                newValue
                            )
                        )

                        valueListener?.onRawValueChanged(newValue)
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable) {}
        }

    private val decTextWatcher: TextWatcher
        get() = object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                _writableFieldsBinding.apply {
                    val decimalEdit = decimalWrite.etCharValueWriteMode

                    if (decimalEdit.hasFocus()) {
                        if (isDecValueValid(decimalEdit.text.toString())) {
                            val newValue = Converters.decToByteArray(decimalEdit.text.toString())
                            hexWrite.etCharValueWriteMode.setText(
                                Converters.bytesToHexWhitespaceDelimited(
                                    newValue
                                )
                            )
                            asciiWrite.etCharValueWriteMode.setText(
                                Converters.getAsciiValue(
                                    newValue
                                )
                            )
                            valueListener?.onRawValueChanged(newValue)
                        } else {
                            decimalEdit.setText(
                                decimalEdit.text.toString().substring(
                                    0,
                                    decimalEdit.text.length - 1
                                )
                            )
                            decimalEdit.setSelection(decimalEdit.text.length)
                            Toast.makeText(context, R.string.invalid_dec_value, Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable) {}
        }

    private val asciiTextWatcher: TextWatcher
        get() = object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                _writableFieldsBinding.apply {
                    val asciiEdit = asciiWrite.etCharValueWriteMode

                    if (asciiEdit.hasFocus()) {
                        val newValue = asciiEdit.text.toString().toByteArray()
                        hexWrite.etCharValueWriteMode.setText(
                            Converters.bytesToHexWhitespaceDelimited(
                                newValue
                            )
                        )
                        decimalWrite.etCharValueWriteMode.setText(
                            Converters.getDecimalValue(
                                newValue
                            )
                        )

                        valueListener?.onRawValueChanged(newValue)
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable) {}
        }

    private val hexFocusChangeListener: View.OnFocusChangeListener
        get() = View.OnFocusChangeListener { _, hasFocus ->
            _writableFieldsBinding.apply {
                val hexEdit = hexWrite.etCharValueWriteMode

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
        }

    private fun setPasteListener(
        _binding: CharacteristicRawValueWriteModeBinding,
        expectedPasteType: String
    ) {
        _binding.ivPasteCharValue.setOnClickListener {
            val clipboardManager = context?.getSystemService(ClipboardManager::class.java)
            if (clipboardManager != null && clipboardManager.primaryClip != null) {
                val clip = clipboardManager.primaryClip
                var text = clip?.getItemAt(0)?.text.toString()
                _binding.etCharValueWriteMode.requestFocus()
                when (expectedPasteType) {
                    HEX_ID -> {
                        text = StringUtils.getStringWithoutWhitespaces(text)
                        if (isHexStringCorrect(text)) {
                            _binding.etCharValueWriteMode.setText(text)
                        } else Toast.makeText(
                            context,
                            R.string.Incorrect_data_format,
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    ASCII_ID -> _binding.etCharValueWriteMode.setText(text)
                    DECIMAL_ID -> if (isDecimalCorrect(text.trim())) {
                        _binding.etCharValueWriteMode.setText(text)
                    } else Toast.makeText(
                        context,
                        R.string.Incorrect_data_format,
                        Toast.LENGTH_SHORT
                    ).show()
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