package com.siliconlabs.bledemo.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.models.TemperatureReading;

import java.util.ArrayList;

public class ThermoGraph extends LinearLayout {
    private static final int NUM_BARS = 5;

    final int[] barIds = {R.id.bar1, R.id.bar2, R.id.bar3, R.id.bar4, R.id.bar5};
    final int[] timeIds = {R.id.thermo_graph_time1, R.id.thermo_graph_time2,
            R.id.thermo_graph_time3, R.id.thermo_graph_time4, R.id.thermo_graph_time5};
    View[] barViews = new View[NUM_BARS];
    TemperatureDisplay[] tempViews = new TemperatureDisplay[NUM_BARS];
    TextView[] timeViews = new TextView[NUM_BARS];
    ArrayList<TemperatureReading> readings = new ArrayList<>();
    LinearLayout placeholder;

    TemperatureReading.Type displayMode = TemperatureReading.Type.FAHRENHEIT;


    public ThermoGraph(Context context) {
        super(context);
        init();
    }

    public ThermoGraph(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ThermoGraph(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ThermoGraph(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        View.inflate(getContext(), R.layout.thermo_graph, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        //find all views in graph and add them to arrays
        for (int i = 0; i < NUM_BARS; i++) {
            LinearLayout currentBar = (LinearLayout) findViewById(barIds[i]);
            barViews[i] = currentBar.findViewById(R.id.thermo_graph_bar_view);
            //left to right gradient
            barViews[i].setAlpha((float)1.0 - (float)(0.10 * (NUM_BARS - 1 - i)));
            tempViews[i] = (TemperatureDisplay) currentBar.findViewById(R.id.thermo_graph_temp);
            //TODO: temporary
            tempViews[i].setTemperature(new TemperatureReading(TemperatureReading.Type.FAHRENHEIT, 80d, System.currentTimeMillis(), TemperatureReading.HtmType.UNKNOWN));
            timeViews[i] = (TextView) findViewById(timeIds[i]);
            placeholder = (LinearLayout) findViewById(R.id.empty_placeholder);
        }
    }

    public void addReading(TemperatureReading reading) {
        readings.add(reading);
        refreshViews();

    }

    private void refreshViews() {
        int size = readings.size();
        int i = 0;
        for (; i < NUM_BARS && i < size; i++) {
            TemperatureReading currentReading = readings.get((size - 1) - i);
            double currentTemp = currentReading.getNormalizedTemperature();
            TemperatureReading.Type type = currentReading.getType();
            float scaledValue = (float)((currentTemp - type.normalizedMin) / (type.getRange())) * 100;
            int index = NUM_BARS - 1 - i;
            barViews[index].setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, scaledValue));
            toggleViews(true, index);
            fillViews(currentReading, index);
        }

        //make all unfilled views invisible
        for (; i < NUM_BARS; i++) {
            toggleViews(false, NUM_BARS - 1 -i);
        }

        placeholder.setVisibility(size > 0 ? GONE : VISIBLE);
    }

    public void clear() {
        readings.clear();
        refreshViews();
    }

    public void switchDisplayMode(TemperatureReading.Type displayMode, boolean refresh) {
        this.displayMode = displayMode;
        if (refresh) {
            refreshViews();
        }
    }

    private void fillViews(TemperatureReading temperatureReading, int index) {
        tempViews[index].setCurrentType(displayMode);
        tempViews[index].setTemperature(temperatureReading);
        timeViews[index].setText(temperatureReading.getFormattedTime());
    }

    private void toggleViews(boolean show, int index) {
        //use gone for the first one because INVISIBLE wont work on Nexus 10????
        tempViews[index].setVisibility(show ? VISIBLE : GONE);
        timeViews[index].setVisibility(show ? VISIBLE : INVISIBLE);
        barViews[index].setVisibility(show ? VISIBLE : INVISIBLE);
    }

    public TemperatureReading.Type getDisplayMode() {
        return displayMode;
    }
}
