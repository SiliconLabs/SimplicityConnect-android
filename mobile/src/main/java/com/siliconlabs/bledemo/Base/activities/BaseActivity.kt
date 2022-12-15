package com.siliconlabs.bledemo.base.activities

import android.app.Activity
import android.content.DialogInterface
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.base.dialogs.ProgressDialogWithSpinner

abstract class BaseActivity : AppCompatActivity() {
    companion object {
        protected const val DISMISS_DIALOG_DELAY_MS = 1000
    }

    enum class ConnectionStatus {
        CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED, READING_DEVICE_STATE
    }

    private var connectionStatusModalDialog: ProgressDialogWithSpinner? = null

    @JvmOverloads
    fun showModalDialog(connStat: ConnectionStatus? = null, cancelListener: DialogInterface.OnCancelListener? = null) {
        dismissModalDialog()
        runOnUiThread {
            // note that the dialog state is never shown when disconnecting from a device
            connectionStatusModalDialog = when (connStat) {
                ConnectionStatus.CONNECTING -> ProgressDialogWithSpinner(this@BaseActivity, "Connecting...", true, -1)
                ConnectionStatus.CONNECTED -> ProgressDialogWithSpinner(this@BaseActivity, "Connection Successful!", false, R.drawable.ic_check)
                ConnectionStatus.DISCONNECTING -> ProgressDialogWithSpinner(this@BaseActivity, "Disconnecting...", false, R.drawable.ic_check)
                ConnectionStatus.DISCONNECTED -> ProgressDialogWithSpinner(this@BaseActivity, "Device Disconnected", false, R.drawable.ic_check)
                ConnectionStatus.READING_DEVICE_STATE -> ProgressDialogWithSpinner(this@BaseActivity, "Reading device state...", true, -1)
                null -> ProgressDialogWithSpinner(this@BaseActivity, "Loading...", true, -1)
            }
            if (!this@BaseActivity.isFinishing) {
                if (connStat == ConnectionStatus.CONNECTED || connStat == ConnectionStatus.DISCONNECTED) {
                    connectionStatusModalDialog?.show(DISMISS_DIALOG_DELAY_MS.toLong())
                } else {
                    connectionStatusModalDialog?.show()
                }
                connectionStatusModalDialog?.setOnCancelListener(cancelListener)
            }
        }
    }

    fun setModalDialogMessage(message: String) {
        runOnUiThread { connectionStatusModalDialog?.setCaption(message) }
    }

    fun dismissModalDialog() {
        runOnUiThread {
            if (connectionStatusModalDialog != null && connectionStatusModalDialog?.isShowing!!) {
                connectionStatusModalDialog?.dismiss()
                connectionStatusModalDialog?.clearAnimation()
                connectionStatusModalDialog = null
            }
        }
    }

    fun showMessage(message: String) {
        runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
    }

    fun showMessage(stringResId: Int) {
        runOnUiThread { Toast.makeText(this, stringResId, Toast.LENGTH_SHORT).show() }
    }

    fun showLongMessage(message: String) {
        runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
    }

    fun hideKeyboard() {
        val imm: InputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = this.currentFocus
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) view = View(this)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}