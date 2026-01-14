package com.siliconlabs.bledemo.base.fragments

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import com.pranavpandey.android.dynamic.toasts.DynamicToast


open class BaseDialogFragment(
    private val hasCustomWidth: Boolean? = null,
    private val isCanceledOnTouchOutside: Boolean? = null
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (hasCustomWidth == true) {
            dialog?.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.85f).toInt(),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        isCanceledOnTouchOutside?.let {
            dialog?.setCanceledOnTouchOutside(isCanceledOnTouchOutside)
        }
    }

    protected fun showMessage(@StringRes message: Int) {
        activity?.runOnUiThread {
            DynamicToast.make(requireContext(),getString(message),5000).show()
        }
    }

    fun hideKeyboard() {
        val imm: InputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (imm.isActive) imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    fun isShowing() = dialog?.isShowing ?: false
}