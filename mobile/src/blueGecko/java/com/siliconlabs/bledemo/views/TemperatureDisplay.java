package com.siliconlabs.bledemo.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.models.TemperatureReading;

public class TemperatureDisplay extends LinearLayout {
    TextView mainTempText;
    TextView decimalText;
    private TextView degreeSymbol;
    private float defaultTextSize;
    private float largeTextSize;
    private float smallTextSize;
    Double temp;
    TemperatureReading.Type currentType;
    TemperatureReading currentReading;

    public TemperatureDisplay(Context context) {
        super(context);
        init(null);
    }

    public TemperatureDisplay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public TemperatureDisplay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TemperatureDisplay(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    public void init(AttributeSet attrs) {
        defaultTextSize = isInEditMode() ? 15 : getContext().getResources().getDimension(R.dimen.thermo_graph_time_text_size);
        if (attrs != null) {
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.TemperatureDisplay);
            largeTextSize = typedArray.getDimension(R.styleable.TemperatureDisplay_large_text_size, defaultTextSize);
            smallTextSize = typedArray.getDimension(R.styleable.TemperatureDisplay_small_text_size, defaultTextSize);
            typedArray.recycle();
        } else {
            smallTextSize = largeTextSize = defaultTextSize;
        }
        currentType = TemperatureReading.Type.FAHRENHEIT;
        View.inflate(getContext(), R.layout.temperature_display, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mainTempText = (TextView) findViewById(R.id.temp_display_primary);
        mainTempText.setTextSize(TypedValue.COMPLEX_UNIT_PX, largeTextSize);
        degreeSymbol = (TextView) findViewById(R.id.temp_degree_symbol);
        degreeSymbol.setTextSize(TypedValue.COMPLEX_UNIT_PX, largeTextSize);
        decimalText = (TextView) findViewById(R.id.temp_display_secondary);
        decimalText.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallTextSize);
        if (temp != null) {
            setTemperature(temp);
        }
    }

    private void setTemperature(double temperature) {
        temp = temperature;
        if (mainTempText != null && decimalText != null) {
            mainTempText.setText(Integer.toString((int) temperature) + ".");
            decimalText.setText(Integer.toString((int) (temperature * 10) % 10));
        }
    }

    public void setFontFamily(String familyName, int style) {
        Typeface typeface = Typeface.create(familyName, style);
        mainTempText.setTypeface(typeface);
        decimalText.setTypeface(typeface);
        degreeSymbol.setTypeface(typeface);
    }

    public void setTemperature(TemperatureReading reading) {
        if (reading != null) {
            this.currentReading = reading;
            setTemperature(reading.getTemperature(currentType));
        }
    }

    public void setCurrentType(TemperatureReading.Type currentType) {
        this.currentType = currentType;
        setTemperature(currentReading);
    }
}
