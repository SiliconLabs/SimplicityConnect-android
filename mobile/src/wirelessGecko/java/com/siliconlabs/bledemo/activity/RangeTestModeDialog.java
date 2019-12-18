package com.siliconlabs.bledemo.activity;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.rangetest.RangeTestMode;
import com.siliconlabs.bledemo.rangetest.TxPower;

import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * @author Comarch S.A.
 */

public class RangeTestModeDialog extends DialogFragment implements RangeTestPresenter.RangeTestView {

    RangeTestPresenter.Controller controller;

    @InjectView(R.id.range_mode_device_name)
    TextView deviceNameView;

    @InjectView(R.id.range_mode_device_number)
    TextView deviceNumberView;

    @InjectView(R.id.range_mode_tx_power)
    TextView powerView;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new Dialog(getActivity(), getTheme()) {
            @Override
            public void onBackPressed() {
                controller.cancelTestMode();
                dismiss();
            }
        };
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
        setStyle(STYLE_NO_TITLE, getTheme());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_range_test_mode, container, false);
        ButterKnife.inject(this, view);

        controller = (RangeTestActivity) getActivity();
        controller.setView(this);

        setCancelable(false);

        return view;
    }

    @OnClick(R.id.range_mode_tx)
    void onTxModeButtonClicked() {
        RangeTestPresenter.Controller controller = (RangeTestActivity) getActivity();
        controller.setView(null);

        controller.initTestMode(RangeTestMode.Tx);
        dismiss();
    }

    @OnClick(R.id.range_mode_rx)
    void onRxModeButtonClicked() {
        RangeTestPresenter.Controller controller = (RangeTestActivity) getActivity();
        controller.setView(null);

        controller.initTestMode(RangeTestMode.Rx);
        dismiss();
    }

    @OnClick(R.id.range_mode_cancel)
    void onCancelButtonClicked() {
        controller.cancelTestMode();
        dismiss();
    }

    @Override
    public void runOnUiThread(Runnable runnable) {
        View view = getView();
        if (view != null) {
            view.post(runnable);
        }
    }

    @Override
    public void showDeviceName(String name) {
        deviceNameView.setText(name);
    }

    @Override
    public void showModelNumber(String number) {
        deviceNumberView.setText(number);
    }

    @Override
    public void showTxPower(TxPower power, List<TxPower> values) {
        float value = power.asDisplayValue();
        DecimalFormat formatter = new DecimalFormat("#.##");
        powerView.setText(String.format(Locale.ROOT, "%sdBm", formatter.format(value)));
    }

    @Override
    public void showPayloadLength(int length, List<Integer> values) {
        // not available in this view
    }

    @Override
    public void showMaWindowSize(int size, List<Integer> values) {
        // not available in this view
    }

    @Override
    public void showChannelNumber(int number) {
        // not available in this view
    }

    @Override
    public void showPacketCountRepeat(boolean enabled) {
        // not available in this view
    }

    @Override
    public void showPacketRequired(int required) {
        // not available in this view
    }

    @Override
    public void showPacketSent(int send) {
        // not available in this view
    }

    @Override
    public void showPer(float per) {
        // not available in this view
    }

    @Override
    public void showMa(float ma) {
        // not available in this view
    }

    @Override
    public void showRemoteId(int id) {
        // not available in this view
    }

    @Override
    public void showSelfId(int id) {
        // not available in this view
    }

    @Override
    public void showUartLogEnabled(boolean enabled) {
        // not available in this view
    }

    @Override
    public void showRunningState(boolean running) {
        // not available in this view
    }

    @Override
    public void showTestRssi(int rssi) {
        // not available in this view
    }

    @Override
    public void showTestRx(int received, int required) {
        // not available in this view
    }

    @Override
    public void clearTestResults() {
        // not available in this view
    }

    @Override
    public void showPhy(int phy, LinkedHashMap<Integer, String> values) {
        // not available in this view
    }
}
