package com.siliconlabs.bledemo.features.demo.matter_demo.utils

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.siliconlabs.bledemo.databinding.DialogDeviceInfoLayoutBinding
import com.siliconlabs.bledemo.features.iop_test.utils.DialogDeviceInfoFragment
import kotlinx.android.synthetic.main.adapter_scanned_device.title

class MatterDialogDeviceInfoFragment: DialogFragment() {
    private var title: String? = null
    private var message: String? = null
    private var positiveButtonText: String? = null
    private var negativeButtonText: String? = null
    private var positiveClickListener: DialogInterface.OnClickListener? = null
    private var negativeClickListener: DialogInterface.OnClickListener? = null
    private lateinit var binding: DialogDeviceInfoLayoutBinding

    private var dialogShowing = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogDeviceInfoLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // Set title and message
        binding.dialogTitle.text = title
        binding.dialogMessage.text = message

        // Set positive button click listener
        binding.btnPositive.setOnClickListener {
            positiveClickListener?.onClick(dialog, DialogInterface.BUTTON_POSITIVE)
            dismiss()
        }

        // Set negative button click listener
        binding.btnNegative.setOnClickListener {
            negativeClickListener?.onClick(dialog, DialogInterface.BUTTON_NEGATIVE)
            dismiss()
        }

        // Set button texts
        binding.btnPositive.text = positiveButtonText
        binding.btnNegative.text = negativeButtonText

    }


    // Builder pattern for setting dialog properties
    class Builder {
        private val fragment = DialogDeviceInfoFragment()

        fun setTitle(title: String?): Builder {
            fragment.title = title
            return this
        }

        fun setMessage(message: String?): Builder {
            fragment.message = message
            return this
        }

        fun setPositiveButton(text: String?, listener: DialogInterface.OnClickListener?): Builder {
            fragment.positiveButtonText = text
            fragment.positiveClickListener = listener
            return this
        }

        fun setNegativeButton(text: String?, listener: DialogInterface.OnClickListener?): Builder {
            fragment.negativeButtonText = text
            fragment.negativeClickListener = listener
            return this
        }

        fun build(): DialogDeviceInfoFragment {
            return fragment
        }
    }

    fun isShowing(): Boolean {
        return dialogShowing
    }
}