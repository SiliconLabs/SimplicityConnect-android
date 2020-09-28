package com.siliconlabs.bledemo.Browser.Dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.Utils.SharedPrefUtils;

public class LeaveBrowserDialog extends DialogFragment {

    private Button okBtn;
    private Button cancelBtn;
    private CheckBox dontShowAgainCB;
    private TextView titleTV;
    private TextView contentTV;

    LeaveBrowserCallback callback;

    private Context context;

    public LeaveBrowserDialog(LeaveBrowserCallback callback, Context context) {
        this.callback = callback;
        this.context = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_info_ok_cancel, container, false);

        okBtn = view.findViewById(R.id.btn_ok);
        cancelBtn = view.findViewById(R.id.btn_cancel);
        dontShowAgainCB = view.findViewById(R.id.cb_dont_show_again);
        titleTV = view.findViewById(R.id.tv_dialog_title);
        contentTV = view.findViewById(R.id.tv_dialog_content);

        titleTV.setText(context.getString(R.string.Leave_Browser));
        contentTV.setText(context.getString(R.string.leave_browser_info));

        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(dontShowAgainCB.isChecked()) {
                    // Save to shared prefs not to show again
                    SharedPrefUtils sharedPrefUtils = new SharedPrefUtils(context);
                    sharedPrefUtils.setShouldDisplayLeaveBrowserDialog(false);
                }
                callback.onOkClicked();
                dismiss();
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

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

    public interface LeaveBrowserCallback {
        void onOkClicked();
    }

}
