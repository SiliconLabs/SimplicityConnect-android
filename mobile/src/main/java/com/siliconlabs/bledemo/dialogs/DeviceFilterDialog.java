package com.siliconlabs.bledemo.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.siliconlabs.bledemo.R;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class DeviceFilterDialog extends Dialog {
    @NonNull
    private final OnSubmitListener listener;

    @InjectView(R.id.dialog_device_filter_root)
    ViewGroup rootView;
    @InjectView(R.id.dialog_device_filter_search_container)
    ViewGroup nameSearchContainer;
    @InjectView(R.id.dialog_device_filter_search_input)
    SearchView nameSearchView;
    @InjectView(R.id.dialog_device_filter_rssi_seekbar)
    SeekBar rssiSeekBar;
    @InjectView(R.id.dialog_device_filter_rssi_value)
    TextView rssiValueText;
    @InjectView(R.id.dialog_device_filter_rssi_bottom_space)
    View rssiBottomSpace;
    @InjectView(R.id.dialog_device_filter_reset_button)
    View resetButton;
    @InjectView(R.id.dialog_device_filter_apply_button)
    View applyButton;
    @InjectView(R.id.dialog_device_filter_cancel_button)
    View cancelButton;

    private boolean filterRssi;

    public DeviceFilterDialog(@NonNull Context context, @NonNull OnSubmitListener listener) {
        this(context, 0, listener);
    }

    public DeviceFilterDialog(@NonNull Context context, @StyleRes int theme, @NonNull OnSubmitListener listener) {
        super(context, theme);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_device_filter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        ButterKnife.inject(this);
        resetUi();
        rssiSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                filterRssi = true;
                int rssi = getRssiValue(progress);
                rssiValueText.setText(getContext().getString(R.string.dialog_device_filter_rssi_value, rssi));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetUi();
            }
        });
        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit();
                dismiss();
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        nameSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                submit();
                dismiss();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
    }

    private int getRssiValue(int progress) {
        return progress - 100;
    }

    private void resetUi() {
        nameSearchView.setQuery("", false);
        rssiSeekBar.setProgress(0);
        rssiValueText.setText(R.string.dialog_device_filter_rssi_not_set);
        filterRssi = false;
    }

    private void submit() {
        String name = nameSearchView.getQuery().toString();
        int rssi = getRssiValue(rssiSeekBar.getProgress());
        listener.onSubmit(name, !name.isEmpty(), rssi, filterRssi);
    }

    public interface OnSubmitListener {
        void onSubmit(String name,
                      boolean filterName,
                      int rssi,
                      boolean filterRssi);
    }
}
