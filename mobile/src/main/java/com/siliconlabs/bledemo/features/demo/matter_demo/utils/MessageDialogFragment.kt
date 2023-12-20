package com.siliconlabs.bledemo.features.demo.matter_demo.utils
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.siliconlabs.bledemo.R

class MessageDialogFragment : DialogFragment() {

    private var message: String = "Default message"
    private var onDismissListener: (() -> Unit)? = null

    fun setMessage(message: String) {
        this.message = message
    }

    fun setOnDismissListener(listener: () -> Unit) {
        this.onDismissListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())

        builder.setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                onDismissListener?.invoke()
                dismiss()
            }

        return builder.create()
    }

    companion object {
        private var isShowing: Boolean = false

        fun isDialogShowing(): Boolean {
            return isShowing
        }
    }
    override fun onStart() {
        super.onStart()
        isShowing = true
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        isShowing = false
    }
}
