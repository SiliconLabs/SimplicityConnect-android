package com.siliconlabs.bledemo.base.dialogs

import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import by.kirich1409.viewbindingdelegate.viewBinding
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.DialogWithProgressSpinnerBinding

class ProgressDialogWithSpinner(
    cancelable: Boolean = true,
    @StringRes private val caption: Int,
    private val onCancelAction: () -> Unit = {},
) : DialogFragment(R.layout.dialog_with_progress_spinner) {
    private val binding by viewBinding(DialogWithProgressSpinnerBinding::bind)
    private var handler: Handler = Handler(Looper.getMainLooper())

    private val autoDismiss = Runnable {
        if (isVisible) {
            dismiss()
        }
    }

    init {
        isCancelable = cancelable
    }

    fun setCaption(@StringRes caption: Int) =
        getString(caption).also { binding.dialogText.text = it }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setCaption(caption)
    }

    override fun dismiss() {
        super.dismiss()
        handler.removeCallbacks(autoDismiss)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        onCancelAction()
    }
}