package com.siliconlabs.bledemo.Browser.Dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.Bluetooth.BLE.ErrorCodes;

public class ErrorDialog extends DialogFragment {
    private Button okBtn;
    private OtaErrorCallback otaErrorCallback;
    private int errorCode;

    public ErrorDialog(int errorCode, OtaErrorCallback otaErrorCallback) {
        this.otaErrorCallback = otaErrorCallback;
        this.errorCode = errorCode;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_error, container, false);

        okBtn = view.findViewById(R.id.ok_btn);

        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                otaErrorCallback.onDismiss();
            }
        });

        TextView errorTitleTV = view.findViewById(R.id.error_title);
        String errorDescriptionText = getString(R.string.Error_colon) + " " + ErrorCodes.getOneOctetErrorCodeHexAsString(errorCode);
        errorTitleTV.setText(errorDescriptionText);

        TextView errorDescriptionTV = view.findViewById(R.id.error_description);
        errorDescriptionTV.setText(Html.fromHtml(ErrorCodes.getATTHTMLFormattedError(errorCode)));

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        return dialog;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        otaErrorCallback.onDismiss();
    }

    public interface OtaErrorCallback {
        void onDismiss();
    }

}

