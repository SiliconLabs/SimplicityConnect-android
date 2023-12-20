package com.siliconlabs.bledemo.features.demo.matter_demo.utils

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import com.siliconlabs.bledemo.databinding.CustomProgressDialogBinding

class CustomProgressDialog(context: Context) : Dialog(context) {
    private lateinit var binding: CustomProgressDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
    }

    fun setMessage(message: String) {
        binding = CustomProgressDialogBinding.inflate(LayoutInflater.from(context))

        setContentView(binding.root)

        // Now you can access views through binding
        binding.customMessage.text = message
    }
}
