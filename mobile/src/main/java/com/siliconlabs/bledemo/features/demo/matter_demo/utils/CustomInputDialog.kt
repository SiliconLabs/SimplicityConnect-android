package com.siliconlabs.bledemo.features.demo.matter_demo.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import com.siliconlabs.bledemo.databinding.CustomInputDialogBinding
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast

import androidx.fragment.app.DialogFragment
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.features.demo.matter_demo.fragments.MatterOTBRInputDialogFragment


class CustomInputDialog : DialogFragment() {
    private lateinit var binding: CustomInputDialogBinding
    private var onButtonClickListener: ((String) -> Unit)? = null
    private var titleText: String = "OK"
    private var subTitleText: String = "OK"

    companion object {
        const val DEVICE_KEY = "device_key"
        const val TITLE_TEXT_KEY = "title_text_key"
        const val SUBTITLE_TEXT_KEY = "subtitle_key"

        fun newInstance(
            context: Context,
            device: String,
            title: String,
            subtitle: String
        ): CustomInputDialog {
            val fragment = CustomInputDialog()
            val args = Bundle()
            args.putString(DEVICE_KEY, device)
            args.putString(TITLE_TEXT_KEY, title)
            args.putString(SUBTITLE_TEXT_KEY, subtitle)
            fragment.arguments = args
            return fragment
        }
    }

    @SuppressLint("UseGetLayoutInflater")
    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val device = arguments?.getString(DEVICE_KEY) ?: ""
        titleText = arguments?.getString(TITLE_TEXT_KEY) ?: "OK"
        subTitleText = arguments?.getString(SUBTITLE_TEXT_KEY) ?: "OK"

        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())

        binding = CustomInputDialogBinding.inflate(LayoutInflater.from(requireContext()))
        builder.setView(binding.root)
        binding.tvTitle.text = titleText
        binding.tvSubTitle.text = subTitleText
        binding.editText.setText(device)
        binding.button.text = requireContext().getString(R.string.matter_custom_alert_add)
        binding.button.setOnClickListener {
            onOkButtonClick()
        }

        return builder.create()
    }

    override fun onStart() {
        super.onStart()
        val dialog: Dialog? = dialog
        if (dialog != null) {
//            if (dialog != null && dialog!!.window != null) {
//                dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//                dialog!!.window!!.requestFeature(Window.FEATURE_NO_TITLE);
//
//            }
            dialog.setCanceledOnTouchOutside(false)
            dialog.window!!
                .setLayout(
                    (getScreenWidth(requireActivity()) * MatterOTBRInputDialogFragment.WINDOW_SIZE).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
        }
    }

    private fun getScreenWidth(activity: Activity): Int {
        val size = Point()

        activity.windowManager.defaultDisplay.getSize(size)
        return size.x
    }

    private fun onOkButtonClick() {
        val enteredText = binding.editText.text.toString().trim()

        if (enteredText.isNotEmpty() && !enteredText.startsWith(".")) {
            onButtonClickListener?.invoke(enteredText)
            dismiss()
        } else {
            if (enteredText.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.please_enter_device_name), Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.please_enter_valid_device_name), Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun setOnButtonClickListener(listener: (String) -> Unit) {
        this.onButtonClickListener = listener
    }
}


