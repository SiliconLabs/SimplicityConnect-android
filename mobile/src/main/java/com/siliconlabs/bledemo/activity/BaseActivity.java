package com.siliconlabs.bledemo.activity;

import android.content.DialogInterface;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.siliconlabs.bledemo.R;

import com.siliconlabs.bledemo.dialogs.ProgressDialogWithSpinner;

public abstract class BaseActivity extends AppCompatActivity {
    protected static final int MILLIS = 1000;
    private static boolean showSpinnerDialog = true;

    public enum ConnectionStatus {
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        DISCONNECTED
    }

    private ProgressDialogWithSpinner connectionStatusModalDialog;

    public static boolean isSpinnerDialogVisible() {
        return showSpinnerDialog;
    }

    public static void setShowSpinnerDialogVisibility(boolean visible) {
        BaseActivity.showSpinnerDialog = visible;
    }

    public void showModalDialog(ConnectionStatus connStat) {
        showModalDialog(connStat, null);
    }

    public void showModalDialog(final ConnectionStatus connStat, @Nullable final DialogInterface.OnCancelListener cancelListener) {
        dismissModalDialog();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (connStat) {
                    case CONNECTING:
                        connectionStatusModalDialog = new ProgressDialogWithSpinner(BaseActivity.this, "Connecting...", true, -1);
                        break;
                    case CONNECTED:
                        connectionStatusModalDialog = new ProgressDialogWithSpinner(BaseActivity.this, "Connection Successful!", false, R.drawable.ic_check);
                        break;
                    case DISCONNECTING:
                        // note that the dialog state is never shown when disconnecting from a device
                        connectionStatusModalDialog = new ProgressDialogWithSpinner(BaseActivity.this, "Disconnecting...", false, R.drawable.ic_check);
                        break;
                    case DISCONNECTED:
                        connectionStatusModalDialog = new ProgressDialogWithSpinner(BaseActivity.this, "Device Disconnected", false, R.drawable.ic_check);
                }

                if (!BaseActivity.this.isFinishing()) {
                    if (connStat == ConnectionStatus.CONNECTED || connStat == ConnectionStatus.DISCONNECTED) {
                        connectionStatusModalDialog.show(MILLIS);
                    } else {
                        connectionStatusModalDialog.show();
                    }

                    connectionStatusModalDialog.setOnCancelListener(cancelListener);
                }
            }
        });
    }

    public void dismissModalDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (connectionStatusModalDialog != null && connectionStatusModalDialog.isShowing()) {
                    connectionStatusModalDialog.dismiss();
                    connectionStatusModalDialog.clearAnimation();
                    connectionStatusModalDialog = null;
                }
            }
        });
    }
}
