package com.siliconlabs.bledemo.activity;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import butterknife.InjectView;
import butterknife.InjectViews;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IFillFormatter;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.renderer.YAxisRenderer;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.rangetest.RangeTestMode;
import com.siliconlabs.bledemo.rangetest.RangeTestValues;
import com.siliconlabs.bledemo.rangetest.TxPower;
import com.siliconlabs.bledemo.Views.BlockableSpinner;

import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Comarch S.A.
 */

class RangeTestFragmentView implements RangeTestPresenter.RangeTestView {

    private static final String SERIES_2_REGEX = "BRD41(71|80|81)";

    @InjectView(R.id.range_root_view)
    View rootView;

    @InjectView(R.id.range_device_name)
    TextView deviceNameView;

    @InjectView(R.id.range_model_number)
    TextView modelNumberView;

    @InjectView(R.id.range_test_packet_count)
    TextView testPacketCountView;

    @InjectView(R.id.range_test_rx)
    TextView testRxView;

    @InjectView(R.id.range_test_per)
    TextView testPerView;

    @InjectView(R.id.range_test_rssi)
    TextView testRssiView;

    @InjectView(R.id.range_test_ma)
    TextView testMaView;

    @InjectView(R.id.range_rx_chart)
    LineChart chart;

    @InjectView(R.id.range_spinner_phy_config)
    BlockableSpinner phyConfigSpinner;

    @InjectView(R.id.range_seek_tx_power)
    SeekBar txPowerSeekBar;

    @InjectView(R.id.range_spinner_tx_power)
    BlockableSpinner txPowerSpinner;

    @InjectView(R.id.range_seek_payload_length)
    SeekBar payloadLengthSeekBar;

    @InjectView(R.id.range_spinner_payload_length)
    BlockableSpinner payloadLengthSpinner;

    @InjectView(R.id.range_seek_ma_window_size)
    SeekBar maWindowSizeSeekBar;

    @InjectView(R.id.range_spinner_ma_window_size)
    BlockableSpinner maWindowSizeSpinner;

    @InjectView(R.id.range_spinner_channel_number)
    BlockableSpinner channelSpinner;

    @InjectView(R.id.range_spinner_packet_count)
    BlockableSpinner packetCountSpinner;

    @InjectView(R.id.range_check_packet_repeat)
    CheckBox packetCountRepeatCheckBox;

    @InjectView(R.id.range_spinner_remote_id)
    BlockableSpinner remoteIdSpinner;

    @InjectView(R.id.range_spinner_self_id)
    BlockableSpinner selfIdSpinner;

    @InjectView(R.id.range_check_uart_log)
    CheckBox uartCheckBox;

    @InjectView(R.id.range_test_start_stop)
    Button startStopButton;

    @InjectViews({R.id.range_transmitted_layout, R.id.range_tx_power_layout_1, R.id.range_tx_power_layout_2})
    List<View> txLayouts;

    @InjectViews({R.id.range_rx_data_row_1, R.id.range_rx_data_row_2, R.id.range_rx_chart, R.id.range_rx_chart_label})
    List<View> rxLayouts;

    @InjectViews({R.id.range_seek_tx_power, R.id.range_seek_payload_length, R.id.range_seek_ma_window_size,
            R.id.range_spinner_tx_power, R.id.range_spinner_payload_length, R.id.range_spinner_ma_window_size,
            R.id.range_spinner_remote_id, R.id.range_spinner_self_id, R.id.range_check_uart_log,
            R.id.range_check_packet_repeat, R.id.range_spinner_phy_config})
    List<View> disabledLayouts;

    private final RangeTestPresenter.Controller controller;
    private RangeTestMode mode;
    private boolean series1 = false;

    private int buttonStringIdOff;
    private int buttonStringIdOn;

    private LineDataSet chartDataSet;
    private LineData chartData;

    private SeekBarsListener seekBarsListener;

    private List<TxPower> txPowerValues;
    private List<Integer> payloadLengthValues;
    private List<Integer> maWindowSizeValues;

    private List<Map.Entry<Integer, String>> phyValues;

    RangeTestFragmentView(RangeTestPresenter.Controller controller) {
        this.controller = controller;
    }

    @OnClick(R.id.range_test_start_stop)
    void onStartStopButtonClicked() {
        controller.toggleRunningState();
    }

    @OnCheckedChanged(R.id.range_check_packet_repeat)
    void onPacketCountRepeatCheckBoxChecked(boolean checked) {
        packetCountSpinner.setEnabled(!checked);
        if (checked) {
            controller.updatePacketCount(RangeTestValues.PACKET_COUNT_REPEAT);
        } else {
            int packetCountIndex = packetCountSpinner.getSelectedItemPosition();
            int packetCount = RangeTestValues.PACKET_COUNT_LOOKUP.get(packetCountIndex);
            controller.updatePacketCount(packetCount);
        }
    }

    @OnCheckedChanged(R.id.range_check_uart_log)
    void onUartLogEnabledCheckBoxChanged(boolean checked) {
        controller.updateUartLogEnabled(checked);
    }

    void setup(RangeTestMode mode) {
        this.mode = mode;

        seekBarsListener = new SeekBarsListener();

        if (mode == RangeTestMode.Tx) {
            setVisibility(rxLayouts, View.GONE);

            setupSpinnerValues(txPowerSpinner, Collections.singletonList(""));

            buttonStringIdOff = R.string.range_tx_start;
            buttonStringIdOn = R.string.range_tx_stop;

            setPacketSent(0);
        } else {
            setVisibility(txLayouts, View.GONE);

            setupChartView();

            buttonStringIdOff = R.string.range_rx_start;
            buttonStringIdOn = R.string.range_rx_waiting;

            setRx(0, 0);
            setRssi(0);
            setMa(0);
            setPer(0);
        }

        setupSpinnerValues(payloadLengthSpinner, Collections.singletonList(""));
        setupSpinnerValues(maWindowSizeSpinner, Collections.singletonList(""));

        setupSpinnerValues(channelSpinner, RangeTestValues.CHANNEL_LOOKUP);
        setupSpinnerValues(packetCountSpinner, RangeTestValues.PACKET_COUNT_LOOKUP);
        setupSpinnerValues(remoteIdSpinner, RangeTestValues.ID_LOOKUP);
        setupSpinnerValues(selfIdSpinner, RangeTestValues.ID_LOOKUP);

        channelSpinner.setOnItemSelectedListener(new SpinnerListener() {
            @Override
            protected void onItemSelected(int index) {
                int channel = RangeTestValues.CHANNEL_LOOKUP.get(index);
                controller.updateChannel(channel);
            }
        });
        packetCountSpinner.setOnItemSelectedListener(new SpinnerListener() {
            @Override
            protected void onItemSelected(int index) {
                int packetCount = RangeTestValues.PACKET_COUNT_LOOKUP.get(index);
                controller.updatePacketCount(packetCount);
            }
        });
        remoteIdSpinner.setOnItemSelectedListener(new SpinnerListener() {
            @Override
            protected void onItemSelected(int index) {
                int id = RangeTestValues.ID_LOOKUP.get(index);
                controller.updateRemoteId(id);
            }
        });
        selfIdSpinner.setOnItemSelectedListener(new SpinnerListener() {
            @Override
            protected void onItemSelected(int index) {
                int id = RangeTestValues.ID_LOOKUP.get(index);
                controller.updateSelfId(id);
            }
        });
        phyConfigSpinner.setOnItemSelectedListener(new SpinnerListener() {
            @Override
            protected void onItemSelected(int index) {
                int id = phyValues.get(index).getKey();
                controller.updatePhyConfig(id);
            }
        });

        txPowerSeekBar.setEnabled(false);
        txPowerSpinner.setEnabled(false);
        payloadLengthSeekBar.setEnabled(false);
        payloadLengthSpinner.setEnabled(false);
        maWindowSizeSeekBar.setEnabled(false);
        maWindowSizeSpinner.setEnabled(false);
        phyConfigSpinner.setEnabled(false);

        setupRunning(false);
    }

    private void appendChartData(float rssi) {
        if (chart == null || chartDataSet == null) {
            return;
        }

        int index = chartDataSet.getEntryCount();
        chartDataSet.addEntry(new Entry(index, rssi));

        chart.getXAxis().setAxisMinimum(0);
        chart.getXAxis().setAxisMaximum(Math.max(50, index));
        chart.setVisibleXRange(50, 50);
        chart.moveViewToX(index);

        chartDataSet.notifyDataSetChanged();
        chartData.notifyDataChanged();
        chart.notifyDataSetChanged();

        chart.invalidate();
    }

    private void clearChart() {
        if (chart == null || chartDataSet == null) {
            return;
        }

        chartDataSet.clear();

        chartDataSet.addEntry(new Entry(-2, 0));
        chartDataSet.addEntry(new Entry(-1, 0));

        chartDataSet.notifyDataSetChanged();
        chartData.notifyDataChanged();
        chart.notifyDataSetChanged();

        chart.invalidate();
    }

    // Presenter View

    @Override
    public void runOnUiThread(Runnable runnable) {
        rootView.post(runnable);
    }

    @Override
    public void showDeviceName(String name) {
        deviceNameView.setText(name);
    }

    @Override
    public void showModelNumber(String number) {
        modelNumberView.setText(number);

        Pattern pattern = Pattern.compile(SERIES_2_REGEX, Pattern.CASE_INSENSITIVE);
        series1 = !pattern.matcher(number).find();
        channelSpinner.setEnabled(series1);
    }

    @Override
    public void showTxPower(TxPower power, List<TxPower> values) {
        if (!values.equals(txPowerValues)) {
            setupSpinnerValues(txPowerSpinner, values);
            txPowerSeekBar.setMax(values.size() - 1);
            txPowerSeekBar.setOnSeekBarChangeListener(seekBarsListener);
            txPowerSpinner.setOnItemSelectedListener(new SpinnerSeekBarListener(txPowerSeekBar));
            txPowerValues = values;
        }

        int index = values.indexOf(power);
        if (index != -1) {
            txPowerSeekBar.setProgress(index);
            txPowerSpinner.setSelection(index);

            txPowerSeekBar.setEnabled(true);
            txPowerSpinner.setEnabled(true);
        }
    }

    @Override
    public void showPayloadLength(int length, List<Integer> values) {
        if (!values.equals(payloadLengthValues)) {
            setupSpinnerValues(payloadLengthSpinner, values);
            payloadLengthSeekBar.setMax(values.size() - 1);
            payloadLengthSeekBar.setOnSeekBarChangeListener(seekBarsListener);
            payloadLengthSpinner.setOnItemSelectedListener(new SpinnerSeekBarListener(payloadLengthSeekBar));
            payloadLengthValues = values;
        }

        int index = values.indexOf(length);
        if (index != -1) {
            payloadLengthSeekBar.setProgress(index);
            payloadLengthSpinner.setSelection(index);

            payloadLengthSeekBar.setEnabled(true);
            payloadLengthSpinner.setEnabled(true);
        }
    }

    @Override
    public void showMaWindowSize(int size, List<Integer> values) {
        if (!values.equals(maWindowSizeValues)) {
            setupSpinnerValues(maWindowSizeSpinner, values);
            maWindowSizeSeekBar.setMax(values.size() - 1);
            maWindowSizeSeekBar.setOnSeekBarChangeListener(seekBarsListener);
            maWindowSizeSpinner.setOnItemSelectedListener(new SpinnerSeekBarListener(maWindowSizeSeekBar));
            maWindowSizeValues = values;
        }

        int index = values.indexOf(size);
        if (index != -1) {
            maWindowSizeSeekBar.setProgress(index);
            maWindowSizeSpinner.setSelection(index);

            maWindowSizeSeekBar.setEnabled(true);
            maWindowSizeSpinner.setEnabled(true);
        }
    }

    @Override
    public void showChannelNumber(int number) {
        int index = RangeTestValues.CHANNEL_LOOKUP.indexOf(number);
        channelSpinner.setSelection(index);
    }

    @Override
    public void showPacketCountRepeat(boolean enabled) {
        packetCountRepeatCheckBox.setChecked(enabled);
    }

    @Override
    public void showPacketRequired(int required) {
        int index = RangeTestValues.PACKET_COUNT_LOOKUP.indexOf(required);
        packetCountSpinner.setSelection(index);
    }

    @Override
    public void showPacketSent(int sent) {
        setPacketSent(sent);
    }

    @Override
    public void showPer(float per) {
        setPer(per);
    }

    @Override
    public void showMa(float ma) {
        setMa(ma);
    }

    @Override
    public void showRemoteId(int id) {
        int index = RangeTestValues.ID_LOOKUP.indexOf(id);
        remoteIdSpinner.setSelection(index);
    }

    @Override
    public void showSelfId(int id) {
        int index = RangeTestValues.ID_LOOKUP.indexOf(id);
        selfIdSpinner.setSelection(index);
    }

    @Override
    public void showUartLogEnabled(boolean enabled) {
        uartCheckBox.setChecked(enabled);
    }

    @Override
    public void showRunningState(boolean running) {
        setupRunning(running);
    }

    @Override
    public void showTestRssi(int rssi) {
        setRssi(rssi);
        appendChartData(rssi);
    }

    @Override
    public void showTestRx(int received, int required) {
        setRx(received, required);
    }

    @Override
    public void showPhy(int phy, LinkedHashMap<Integer, String> values) {
        if (!values.containsKey(phy)) {
            return;
        }

        List<Map.Entry<Integer, String>> valuesList = new ArrayList<>(values.entrySet());

        if (!valuesList.equals(phyValues)) {
            setupSpinnerValues(phyConfigSpinner, new ArrayList<>(values.values()));
            phyConfigSpinner.setEnabled(true);
            phyValues = valuesList;
        }

        for (int i = 0; i < valuesList.size(); ++i) {
            Map.Entry<Integer, String> entry = valuesList.get(i);
            if (entry.getKey().equals(phy)) {
                phyConfigSpinner.setSelection(i);
                return;
            }
        }
    }

    @Override
    public void clearTestResults() {
        clearChart();
    }

    private void setPacketSent(int packetSent) {
        testPacketCountView.setText(String.valueOf(packetSent));
    }

    private void setRx(int received, int required) {
        setValue(testRxView, R.string.range_rx_rx, received, required);
    }

    private void setRssi(int rssi) {
        setValue(testRssiView, R.string.range_rx_rssi, rssi);
    }

    private void setMa(float ma) {
        setValue(testMaView, R.string.range_rx_ma, ma);
    }

    private void setPer(float per) {
        setValue(testPerView, R.string.range_rx_per, per);
    }

    private void setValue(final TextView view, final Integer resId, final Object... args) {
        view.setText(view.getContext().getString(resId, args));
    }

    //

    private void updateControllerFromSeekBar(SeekBar seekBar) {
        int progress = seekBar.getProgress();
        if (seekBar == txPowerSeekBar) {
            TxPower power = txPowerValues.get(progress);
            controller.updateTxPower(power.asCharacteristicValue());
        } else if (seekBar == payloadLengthSeekBar) {
            int payloadLength = payloadLengthValues.get(progress);
            controller.updatePayloadLength(payloadLength);
        } else if (seekBar == maWindowSizeSeekBar) {
            int maWindowSize = maWindowSizeValues.get(progress);
            controller.updateMaWindowSize(maWindowSize);
        }
    }

    private void setupRunning(boolean running) {
        if (running) {
            setEnabled(disabledLayouts, false);
            channelSpinner.setEnabled(false);
            packetCountSpinner.setEnabled(false);

            startStopButton.setText(buttonStringIdOn);
            startStopButton.setEnabled(mode != RangeTestMode.Rx);
        } else {
            setEnabled(disabledLayouts, true);
            channelSpinner.setEnabled(series1);
            packetCountSpinner.setEnabled(!packetCountRepeatCheckBox.isChecked());

            startStopButton.setText(buttonStringIdOff);
            startStopButton.setEnabled(true);
        }
    }

    private <T> void setupSpinnerValues(Spinner spinner, List<T> values) {
        ArrayAdapter<T> adapter = new ArrayAdapter<T>(spinner.getContext(), android.R.layout.simple_spinner_item, values) {

            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0, false);
    }

    private void setVisibility(List<View> views, int visibility) {
        for (View view : views) {
            view.setVisibility(visibility);
        }
    }

    private void setEnabled(List<View> views, boolean enabled) {
        for (View view : views) {
            view.setEnabled(enabled);
        }
    }

    private void setupChartView() {
        Resources resources = chart.getResources();
        int textColor = resources.getColor(R.color.silabs_black);
        int axisColor = resources.getColor(R.color.silabs_black);
        int graphColor = resources.getColor(R.color.silabs_red);

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setScaleEnabled(false);
        chart.setHighlightPerTapEnabled(false);
        chart.setHighlightPerDragEnabled(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setValueFormatter(new YAxisValueFormatter());
        chart.getAxisLeft().setTextSize(10f);
        chart.getAxisLeft().setTextColor(textColor);
        chart.getAxisLeft().setAxisMinimum(-100);
        chart.getAxisLeft().setAxisMaximum(25);
        chart.getAxisLeft().setGranularity(25);
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisLeft().setDrawAxisLine(true);
        chart.getAxisLeft().setAxisLineWidth(0.5f);
        chart.getAxisLeft().setAxisLineColor(axisColor);
        chart.setRendererLeftYAxis(new YAxisArrowRenderer(chart.getViewPortHandler(), chart.getAxisLeft(), chart.getRendererLeftYAxis().getTransformer()));
        chart.getXAxis().setDrawGridLines(false);
        chart.getXAxis().setDrawAxisLine(false);
        chart.getXAxis().setAxisMinimum(0);
        chart.getXAxis().setAxisMaximum(50);
        chart.getXAxis().setValueFormatter(new XAxisValueFormatter());
        chart.setMinOffset(0);
        chart.setExtraOffsets(-4f, 0, 4f, 4f);
        chart.getAxisLeft().addLimitLine(createLimitLine(0, axisColor));
        chart.getAxisLeft().addLimitLine(createDashLine(-100, axisColor));
        chart.getAxisLeft().addLimitLine(createDashLine(-75, axisColor));
        chart.getAxisLeft().addLimitLine(createDashLine(-50, axisColor));
        chart.getAxisLeft().addLimitLine(createDashLine(-25, axisColor));
        chart.getAxisLeft().addLimitLine(createDashLine(25, axisColor));

        LineData chartData = createChartData(graphColor);

        chart.setData(chartData);

        chartDataSet.notifyDataSetChanged();
        chartData.notifyDataChanged();
        chart.notifyDataSetChanged();

        chart.invalidate();
    }

    private LineData createChartData(int color) {
        List<Entry> entries = new ArrayList<>(1024);
        entries.add(new Entry(-2, 0));
        entries.add(new Entry(-1, 0));

        chartDataSet = new LineDataSet(entries, null);

        chartDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        chartDataSet.setCubicIntensity(0.1f);
        chartDataSet.setDrawCircles(false);
        chartDataSet.setDrawFilled(true);
        chartDataSet.setLineWidth(0f);
        chartDataSet.setColor(color);
        chartDataSet.setFillColor(color);
        chartDataSet.setFillAlpha(255);
        chartDataSet.setDrawHorizontalHighlightIndicator(false);
        chartDataSet.setDrawVerticalHighlightIndicator(false);
        chartDataSet.setFillFormatter(new CubicLineSampleFillFormatter());

        chartData = new LineData(chartDataSet);

        chartData.setDrawValues(false);

        return chartData;
    }

    private LimitLine createLimitLine(float position, int color) {
        LimitLine line = new ArrowLimitLine(position);

        line.setLineColor(color);
        line.setLineWidth(0.5f);

        return line;
    }

    private LimitLine createDashLine(float position, int color) {
        LimitLine line = new LimitLine(position);

        float lineLength = Utils.convertDpToPixel(4);
        float spaceLength = Utils.convertDpToPixel(2);

        line.setLineColor(color);
        line.setLineWidth(0.5f);
        line.enableDashedLine(lineLength, spaceLength, 0);

        return line;
    }

    private class SeekBarsListener implements SeekBar.OnSeekBarChangeListener {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                if (seekBar == txPowerSeekBar) {
                    txPowerSpinner.setSelection(progress);
                } else if (seekBar == payloadLengthSeekBar) {
                    payloadLengthSpinner.setSelection(progress);
                } else if (seekBar == maWindowSizeSeekBar) {
                    maWindowSizeSpinner.setSelection(progress);
                }
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            updateControllerFromSeekBar(seekBar);
        }
    }

    private class SpinnerSeekBarListener implements Spinner.OnItemSelectedListener {

        private final SeekBar seekBar;

        private SpinnerSeekBarListener(SeekBar seekBar) {
            this.seekBar = seekBar;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            seekBar.setProgress(position);
            updateControllerFromSeekBar(seekBar);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    private abstract class SpinnerListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            onItemSelected(position);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }

        protected abstract void onItemSelected(int index);
    }

    private class YAxisValueFormatter implements IAxisValueFormatter {

        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            return (value > 0 ? "+" + value : value) + " dBm";
        }
    }

    private class XAxisValueFormatter implements IAxisValueFormatter {

        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            return "";
        }
    }

    private class CubicLineSampleFillFormatter implements IFillFormatter {

        @Override
        public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
            return -100;
        }
    }

    private static class YAxisArrowRenderer extends YAxisRenderer {

        private final float ARROW_SIZE = Utils.convertDpToPixel(3.5f);

        private Path arrowPath = new Path();
        private Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private YAxisArrowRenderer(ViewPortHandler viewPortHandler, YAxis yAxis, Transformer trans) {
            super(viewPortHandler, yAxis, trans);
            arrowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        }

        @Override
        public void renderAxisLine(Canvas c) {
            if (!mYAxis.isEnabled() || !mYAxis.isDrawAxisLineEnabled())
                return;

            mAxisLinePaint.setColor(mYAxis.getAxisLineColor());
            mAxisLinePaint.setStrokeWidth(mYAxis.getAxisLineWidth());

            arrowPaint.setColor(mYAxis.getAxisLineColor());

            arrowPath.reset();

            if (mYAxis.getAxisDependency() == YAxis.AxisDependency.LEFT) {
                c.drawLine(mViewPortHandler.contentLeft(), mViewPortHandler.contentTop() - (ARROW_SIZE * 1.5f), mViewPortHandler.contentLeft(),
                        mViewPortHandler.contentBottom(), mAxisLinePaint);

                arrowPath.moveTo(mViewPortHandler.contentLeft(), mViewPortHandler.contentTop() - (ARROW_SIZE * 1.5f));
                arrowPath.lineTo(mViewPortHandler.contentLeft() + (ARROW_SIZE / 2f), mViewPortHandler.contentTop() - (ARROW_SIZE * 0.5f));
                arrowPath.lineTo(mViewPortHandler.contentLeft() - (ARROW_SIZE / 2f), mViewPortHandler.contentTop() - (ARROW_SIZE * 0.5f));
                arrowPath.close();
            } else {
                c.drawLine(mViewPortHandler.contentRight(), mViewPortHandler.contentTop() - (ARROW_SIZE * 1.5f), mViewPortHandler.contentRight(),
                        mViewPortHandler.contentBottom(), mAxisLinePaint);

                arrowPath.moveTo(mViewPortHandler.contentRight(), mViewPortHandler.contentTop() - (ARROW_SIZE * 1.5f));
                arrowPath.lineTo(mViewPortHandler.contentRight() + (ARROW_SIZE / 2f), mViewPortHandler.contentTop() - (ARROW_SIZE * 0.5f));
                arrowPath.lineTo(mViewPortHandler.contentRight() - (ARROW_SIZE / 2f), mViewPortHandler.contentTop() - (ARROW_SIZE * 0.5f));
                arrowPath.close();
            }

            c.drawPath(arrowPath, arrowPaint);
        }

        @Override
        public void renderLimitLines(Canvas c) {
            List<LimitLine> limitLines = mYAxis.getLimitLines();

            if (limitLines == null || limitLines.size() <= 0)
                return;

            float[] pts = mRenderLimitLinesBuffer;
            pts[0] = 0;
            pts[1] = 0;
            Path limitLinePath = mRenderLimitLines;
            limitLinePath.reset();

            for (int i = 0; i < limitLines.size(); i++) {

                LimitLine l = limitLines.get(i);

                if (!l.isEnabled())
                    continue;

                int clipRestoreCount = c.save();
                mLimitLineClippingRect.set(mViewPortHandler.getContentRect());
                mLimitLineClippingRect.inset(0.f, -l.getLineWidth());
                c.clipRect(mLimitLineClippingRect);

                mLimitLinePaint.setStyle(Paint.Style.STROKE);
                mLimitLinePaint.setColor(l.getLineColor());
                mLimitLinePaint.setStrokeWidth(l.getLineWidth());
                mLimitLinePaint.setPathEffect(l.getDashPathEffect());

                pts[1] = l.getLimit();

                mTrans.pointValuesToPixel(pts);

                limitLinePath.moveTo(mViewPortHandler.contentLeft(), pts[1]);
                limitLinePath.lineTo(mViewPortHandler.contentRight(), pts[1]);

                c.drawPath(limitLinePath, mLimitLinePaint);
                limitLinePath.reset();
                // c.drawLines(pts, mLimitLinePaint);

                if (l instanceof ArrowLimitLine) {
                    arrowPaint.setColor(l.getLineColor());

                    arrowPath.reset();
                    arrowPath.moveTo(mViewPortHandler.contentRight(), pts[1]);
                    arrowPath.lineTo(mViewPortHandler.contentRight() - ARROW_SIZE, pts[1] + (ARROW_SIZE / 2f));
                    arrowPath.lineTo(mViewPortHandler.contentRight() - ARROW_SIZE, pts[1] - (ARROW_SIZE / 2f));
                    arrowPath.close();

                    c.drawPath(arrowPath, arrowPaint);
                }

                String label = l.getLabel();

                // if drawing the limit-value label is enabled
                if (label != null && !label.equals("")) {

                    mLimitLinePaint.setStyle(l.getTextStyle());
                    mLimitLinePaint.setPathEffect(null);
                    mLimitLinePaint.setColor(l.getTextColor());
                    mLimitLinePaint.setTypeface(l.getTypeface());
                    mLimitLinePaint.setStrokeWidth(0.5f);
                    mLimitLinePaint.setTextSize(l.getTextSize());

                    final float labelLineHeight = Utils.calcTextHeight(mLimitLinePaint, label);
                    float xOffset = Utils.convertDpToPixel(4f) + l.getXOffset();
                    float yOffset = l.getLineWidth() + labelLineHeight + l.getYOffset();

                    final LimitLine.LimitLabelPosition position = l.getLabelPosition();

                    if (position == LimitLine.LimitLabelPosition.RIGHT_TOP) {

                        mLimitLinePaint.setTextAlign(Paint.Align.RIGHT);
                        c.drawText(label,
                                mViewPortHandler.contentRight() - xOffset,
                                pts[1] - yOffset + labelLineHeight, mLimitLinePaint);
                    } else if (position == LimitLine.LimitLabelPosition.RIGHT_BOTTOM) {

                        mLimitLinePaint.setTextAlign(Paint.Align.RIGHT);
                        c.drawText(label,
                                mViewPortHandler.contentRight() - xOffset,
                                pts[1] + yOffset, mLimitLinePaint);
                    } else if (position == LimitLine.LimitLabelPosition.LEFT_TOP) {

                        mLimitLinePaint.setTextAlign(Paint.Align.LEFT);
                        c.drawText(label,
                                mViewPortHandler.contentLeft() + xOffset,
                                pts[1] - yOffset + labelLineHeight, mLimitLinePaint);
                    } else {

                        mLimitLinePaint.setTextAlign(Paint.Align.LEFT);
                        c.drawText(label,
                                mViewPortHandler.offsetLeft() + xOffset,
                                pts[1] + yOffset, mLimitLinePaint);
                    }
                }

                c.restoreToCount(clipRestoreCount);
            }
        }
    }

    private class ArrowLimitLine extends LimitLine {

        private ArrowLimitLine(float limit) {
            super(limit);
        }
    }
}
