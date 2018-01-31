/*
 * Bluegiga’s Bluetooth Smart Android SW for Bluegiga BLE modules
 * Contact: support@bluegiga.com.
 *
 * This is free software distributed under the terms of the MIT license reproduced below.
 *
 * Copyright (c) 2013, Bluegiga Technologies
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files ("Software")
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF 
 * ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A  PARTICULAR PURPOSE.
 */
package com.siliconlabs.bledemo.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;

// Dialogs - is used to showing dialogs
public class Dialogs {

    // Displays dialog with title, message, positive and negative button
    public static Dialog showAlert(CharSequence title, CharSequence message, Context context,
            CharSequence positiveBtnText, CharSequence negativeBtnText, OnClickListener positiveListener,
            OnClickListener negativeListener) {

        final AlertDialog alert = new AlertDialog.Builder(context).create();
        alert.setCancelable(false);
        alert.setCanceledOnTouchOutside(false);
        if (positiveListener != null && positiveBtnText != null) {
            alert.setButton(AlertDialog.BUTTON_POSITIVE, positiveBtnText, positiveListener);
        }
        if (negativeListener != null && negativeBtnText != null) {
            alert.setButton(AlertDialog.BUTTON_NEGATIVE, negativeBtnText, negativeListener);
        }
        alert.setTitle(title);
        alert.setMessage(message);
        alert.show();
        return alert;
    }

    // Displays progress dialog with title and message
    public static ProgressDialog showProgress(CharSequence title, CharSequence message, Context context,
            OnCancelListener listener) {

        final ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setCancelable(true);
        progressDialog.setCanceledOnTouchOutside(false);

        progressDialog.setOnCancelListener(listener);
        progressDialog.setTitle(title);
        progressDialog.setMessage(message);
        progressDialog.show();
        return progressDialog;
    }
}
