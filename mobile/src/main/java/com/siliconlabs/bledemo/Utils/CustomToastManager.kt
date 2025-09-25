package com.siliconlabs.bledemo.utils


import android.annotation.SuppressLint
import android.content.Context
import com.pranavpandey.android.dynamic.toasts.DynamicToast


object CustomToastManager {
    @SuppressLint("StaticFieldLeak")
    fun showError(context: Context, message: String, duration: Long = 5000) {
        DynamicToast.makeError(context, message, duration.toInt()).show()
    }

    @SuppressLint("StaticFieldLeak")
    fun show(context: Context, message: String, duration: Long = 5000) {
        DynamicToast.make(context, message, duration.toInt()).show()
    }

    fun showSuccess(context: Context, message: String, duration: Long = 5000) {
        DynamicToast.makeSuccess(context, message, duration.toInt()).show()
    }

}
