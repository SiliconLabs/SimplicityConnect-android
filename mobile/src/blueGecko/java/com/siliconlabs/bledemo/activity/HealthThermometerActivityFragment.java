package com.siliconlabs.bledemo.activity;

import android.graphics.Typeface;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.SwitchCompat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.models.TemperatureReading;
import com.siliconlabs.bledemo.Views.TemperatureDisplay;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class HealthThermometerActivityFragment extends Fragment {
    private TemperatureReading currentReading;
    private TemperatureReading.Type currentType;

    @InjectView(R.id.thermo_large_temperature)
    TemperatureDisplay largeTemperatureDisplay;
    @InjectView(R.id.thermo_type_value_text)
    TextView thermoTypeText;
    @InjectView(R.id.thermo_large_time_text)
    TextView thermoTimeText;
    @InjectView(R.id.type_switch)
    SwitchCompat typeSwitch;
    @InjectView(R.id.thermometer_device_name)
    TextView deviceNameText;

    public HealthThermometerActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_health_thermometer, container, false);
        ButterKnife.inject(this, v);
        setHasOptionsMenu(true);
        largeTemperatureDisplay.setFontFamily("sans-serif-thin", Typeface.NORMAL);
        typeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onTabClick(!isChecked);
            }
        });
        return v;
    }

    private void onTabClick(boolean fahrenheit) {
        currentType = fahrenheit ? TemperatureReading.Type.FAHRENHEIT : TemperatureReading.Type.CELSIUS;
        largeTemperatureDisplay.setCurrentType(currentType);
    }

    public void setCurrentReading(TemperatureReading temperatureReading) {
        this.currentReading = temperatureReading;
        refreshUi();
    }

    public void setDeviceName(String deviceName) {
        deviceNameText.setText(deviceName);
    }

    private void refreshUi() {
        if (currentReading != null && isAdded()) {
            largeTemperatureDisplay.setTemperature(currentReading);
            thermoTypeText.setText(getString(currentReading.getHtmType().getNameResId()));
            thermoTimeText.setText(currentReading.getFormattedTime());
        }
    }

}
