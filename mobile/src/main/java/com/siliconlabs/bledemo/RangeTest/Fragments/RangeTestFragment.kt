package com.siliconlabs.bledemo.RangeTest.Fragments

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.formatter.IFillFormatter
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.renderer.YAxisRenderer
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.RangeTest.Activities.RangeTestActivity
import com.siliconlabs.bledemo.RangeTest.Models.RangeTestMode
import com.siliconlabs.bledemo.RangeTest.Models.RangeTestValues
import com.siliconlabs.bledemo.RangeTest.Models.TxPower
import com.siliconlabs.bledemo.RangeTest.Presenters.RangeTestPresenter
import com.siliconlabs.bledemo.RangeTest.Presenters.RangeTestPresenter.Controller
import kotlinx.android.synthetic.main.fragment_range_test.*
import java.util.*
import java.util.regex.Pattern

/**
 * @author Comarch S.A.
 */
class RangeTestFragment : Fragment(), RangeTestPresenter.RangeTestView {
    private lateinit var controller: Controller

    private var buttonStringIdOff = 0
    private var buttonStringIdOn = 0
    private var series1 = false

    private var mode: RangeTestMode? = null
    private var chartDataSet: LineDataSet? = null

    private var chartData: LineData? = null
    private var seekBarsListener: SeekBarsListener? = null

    private var txPowerValues: List<TxPower>? = null
    private var payloadLengthValues: List<Int>? = null
    private var maWindowSizeValues: List<Int>? = null

    private var phyValues: List<Map.Entry<Int, String>>? = null
    private var txLayouts: ArrayList<View> = ArrayList()
    private var rxLayouts: ArrayList<View> = ArrayList()
    private var disabledLayouts: ArrayList<View> = ArrayList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = activity as RangeTestActivity

        val modeCode = arguments?.getInt(ARG_MODE)!!
        mode = RangeTestMode.fromCode(modeCode)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_range_test, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txLayouts.apply {
            add(range_transmitted_layout)
            add(tv_range_tx_power_layout_1)
            add(range_tx_power_layout_2)
        }

        rxLayouts.apply {
            add(range_rx_data_row_1)
            add(range_rx_data_row_2)
            add(chart)
            add(tv_range_rx_chart_label)
        }

        disabledLayouts.apply {
            add(sb_range_tx_power)
            add(sb_range_payload_length)
            add(range_seek_ma_window_size)
            add(sp_tx_power)
            add(sp_payload_length)
            add(sp_ma_window_size)
            add(sp_remote_id)
            add(sp_self_id)
            add(range_check_uart_log)
            add(range_check_packet_repeat)
            add(sp_phy_config)
        }


        mode?.let { setup(it) }
        controller.setView(this)

        range_test_start_stop.setOnClickListener {
            controller.toggleRunningState()
        }

        range_check_packet_repeat.setOnCheckedChangeListener { _, checked ->
            sp_packet_count.isEnabled = !checked
            if (checked) {
                controller.updatePacketCount(RangeTestValues.PACKET_COUNT_REPEAT)
            } else {
                val packetCountIndex = sp_packet_count.selectedItemPosition
                val packetCount = RangeTestValues.PACKET_COUNT_LOOKUP[packetCountIndex]
                controller.updatePacketCount(packetCount)
            }
        }

        range_check_uart_log.setOnCheckedChangeListener { _, checked ->
            controller.updateUartLogEnabled(checked)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        val controller: Controller = activity as RangeTestActivity
        controller.setView(null)
    }

    private fun clearChart() {
        if (chart == null || chartDataSet == null) {
            return
        }

        chartDataSet?.apply {
            clear()
            addEntry(Entry((-2).toFloat(), 0f))
            addEntry(Entry((-1).toFloat(), 0f))
            notifyDataSetChanged()
        }

        chartData?.notifyDataChanged()
        chart.notifyDataSetChanged()
        chart.invalidate()
    }

    override fun runOnUiThread(runnable: Runnable?) {
        view?.post(runnable)
    }

    override fun showDeviceName(name: String?) {
        tv_range_device_name.text = name
    }

    override fun showModelNumber(number: String?, running: Boolean?) {
        tv_range_model_number.text = number
        val pattern = Pattern.compile(SERIES_2_REGEX, Pattern.CASE_INSENSITIVE)
        series1 = !pattern.matcher(number).find()

        if (running != null && running) sp_channel_number.isEnabled = false
        else sp_channel_number.isEnabled = series1
    }

    override fun showTxPower(power: TxPower?, values: List<TxPower>) {
        if (values != txPowerValues) {
            setupSpinnerValues(sp_tx_power, values)
            sb_range_tx_power.max = values.size - 1
            sb_range_tx_power.setOnSeekBarChangeListener(seekBarsListener)
            sp_tx_power.onItemSelectedListener = SpinnerSeekBarListener(sb_range_tx_power)
            txPowerValues = values
        }
        val index = values.indexOf(power)
        if (index != -1) {
            sb_range_tx_power.progress = index
            sp_tx_power.setSelection(index)
        }
    }

    override fun showPayloadLength(length: Int, values: List<Int>) {
        if (values != payloadLengthValues) {
            setupSpinnerValues(sp_payload_length, values)
            sb_range_payload_length.max = values.size - 1
            sb_range_payload_length.setOnSeekBarChangeListener(seekBarsListener)
            sp_payload_length.onItemSelectedListener = SpinnerSeekBarListener(sb_range_payload_length)
            payloadLengthValues = values
        }
        val index = values.indexOf(length)
        if (index != -1) {
            sb_range_payload_length.progress = index
            sp_payload_length.setSelection(index)
        }
    }

    override fun showMaWindowSize(size: Int, values: List<Int>) {
        if (values != maWindowSizeValues) {
            setupSpinnerValues(sp_ma_window_size, values)
            range_seek_ma_window_size.max = values.size - 1
            range_seek_ma_window_size.setOnSeekBarChangeListener(seekBarsListener)
            sp_ma_window_size.onItemSelectedListener = SpinnerSeekBarListener(range_seek_ma_window_size)
            maWindowSizeValues = values
        }
        val index = values.indexOf(size)
        if (index != -1) {
            range_seek_ma_window_size.progress = index
            sp_ma_window_size.setSelection(index)
        }
    }

    override fun showChannelNumber(number: Int) {
        val index = RangeTestValues.CHANNEL_LOOKUP.indexOf(number)
        sp_channel_number.setSelection(index)
    }

    override fun showPacketCountRepeat(enabled: Boolean) {
        range_check_packet_repeat.isChecked = enabled
    }

    override fun showPacketRequired(required: Int) {
        val index = RangeTestValues.PACKET_COUNT_LOOKUP.indexOf(required)
        sp_packet_count.setSelection(index)
    }

    override fun showPacketSent(sent: Int) {
        setPacketSent(sent)
    }

    override fun showPer(per: Float) {
        setPer(per)
    }

    override fun showMa(ma: Float) {
        setMa(ma)
    }

    override fun showRemoteId(id: Int) {
        val index = RangeTestValues.ID_LOOKUP.indexOf(id)
        sp_remote_id.setSelection(index)
    }

    override fun showSelfId(id: Int) {
        val index = RangeTestValues.ID_LOOKUP.indexOf(id)
        sp_self_id.setSelection(index)
    }

    override fun showUartLogEnabled(enabled: Boolean) {
        range_check_uart_log.isChecked = enabled
    }

    override fun showRunningState(running: Boolean) {
        setupRunning(running)
    }

    override fun showTestRssi(rssi: Int) {
        setRssi(rssi)
        appendChartData(rssi.toFloat())
    }

    override fun showTestRx(received: Int, required: Int) {
        setRx(received, required)
    }

    override fun showPhy(phy: Int, values: LinkedHashMap<Int, String>) {
        if (!values.containsKey(phy)) {
            return
        }

        val valuesList: List<Map.Entry<Int, String>> = ArrayList<Map.Entry<Int, String>>(values.entries)
        if (valuesList != phyValues) {
            setupSpinnerValues(sp_phy_config, ArrayList(values.values))
            phyValues = valuesList
        }

        for (i in valuesList.indices) {
            val entry = valuesList[i]
            if (entry.key == phy) {
                sp_phy_config.setSelection(i)
                return
            }
        }
    }

    override fun clearTestResults() {
        clearChart()
    }

    private fun setup(mode: RangeTestMode) {
        this.mode = mode
        seekBarsListener = SeekBarsListener()
        if (mode == RangeTestMode.Tx) {
            setVisibility(rxLayouts, View.GONE)
            setupSpinnerValues(sp_tx_power, listOf(""))
            buttonStringIdOff = R.string.range_tx_start
            buttonStringIdOn = R.string.range_tx_stop
            setPacketSent(0)
        } else {
            setVisibility(txLayouts, View.GONE)
            setupChartView()
            buttonStringIdOff = R.string.range_rx_start
            buttonStringIdOn = R.string.range_rx_waiting
            setRx(0, 0)
            setRssi(0)
            setMa(0f)
            setPer(0f)
        }

        setupSpinnerValues(sp_payload_length, listOf(""))
        setupSpinnerValues(sp_ma_window_size, listOf(""))
        setupSpinnerValues(sp_channel_number, RangeTestValues.CHANNEL_LOOKUP)
        setupSpinnerValues(sp_packet_count, RangeTestValues.PACKET_COUNT_LOOKUP)
        setupSpinnerValues(sp_remote_id, RangeTestValues.ID_LOOKUP)
        setupSpinnerValues(sp_self_id, RangeTestValues.ID_LOOKUP)

        sp_channel_number.onItemSelectedListener = object : SpinnerListener() {
            override fun onItemSelected(index: Int) {
                val channel = RangeTestValues.CHANNEL_LOOKUP[index]
                controller.updateChannel(channel)
            }
        }

        sp_packet_count.onItemSelectedListener = object : SpinnerListener() {
            override fun onItemSelected(index: Int) {
                val packetCount = RangeTestValues.PACKET_COUNT_LOOKUP[index]
                controller.updatePacketCount(packetCount)
            }
        }

        sp_remote_id.onItemSelectedListener = object : SpinnerListener() {
            override fun onItemSelected(index: Int) {
                val id = RangeTestValues.ID_LOOKUP[index]
                controller.updateRemoteId(id)
            }
        }

        sp_self_id.onItemSelectedListener = object : SpinnerListener() {
            override fun onItemSelected(index: Int) {
                val id = RangeTestValues.ID_LOOKUP[index]
                controller.updateSelfId(id)
            }
        }

        sp_phy_config.onItemSelectedListener = object : SpinnerListener() {
            override fun onItemSelected(index: Int) {
                val id = phyValues!![index].key
                controller.updatePhyConfig(id)
            }
        }

        sb_range_tx_power.isEnabled = false
        sp_tx_power.isEnabled = false
        sb_range_payload_length.isEnabled = false
        sp_payload_length.isEnabled = false
        range_seek_ma_window_size.isEnabled = false
        sp_ma_window_size.isEnabled = false
        sp_phy_config.isEnabled = false
        setupRunning(false)
    }

    private fun appendChartData(rssi: Float) {
        if (chart == null || chartDataSet == null) {
            return
        }
        val index = chartDataSet!!.entryCount
        chartDataSet!!.addEntry(Entry(index.toFloat(), rssi))
        chart.xAxis.axisMinimum = 0f
        chart.xAxis.axisMaximum = Math.max(50, index).toFloat()
        chart.setVisibleXRange(50f, 50f)
        chart.moveViewToX(index.toFloat())
        chartDataSet?.notifyDataSetChanged()
        chartData?.notifyDataChanged()
        chart.notifyDataSetChanged()
        chart.invalidate()
    }


    private fun setPacketSent(packetSent: Int) {
        tv_range_test_packet_count.text = packetSent.toString()
    }

    private fun setRx(received: Int, required: Int) {
        setValue(tv_range_test_rx, R.string.range_rx_rx, received, required)
    }

    private fun setRssi(rssi: Int) {
        setValue(tv_range_test_rssi, R.string.range_rx_rssi, rssi)
    }

    private fun setMa(ma: Float) {
        tv_range_test_ma.text = String.format(Locale.US,"%.1f",ma).plus("%")
    }

    private fun setPer(per: Float) {
        tv_range_test_ma.text = String.format(Locale.US,"%.1f",per).plus("%")
    }

    private fun setValue(view: TextView, resId: Int, vararg args: Any) {
        view.text = view.context.getString(resId, *args)
    }

    private fun updateControllerFromSeekBar(seekBar: SeekBar) {
        val progress = seekBar.progress
        when {
            seekBar === sb_range_tx_power -> {
                val power = txPowerValues!![progress]
                controller.updateTxPower(power.asCharacteristicValue())
            }
            seekBar === sb_range_payload_length -> {
                val payloadLength = payloadLengthValues!![progress]
                controller.updatePayloadLength(payloadLength)
            }
            seekBar === range_seek_ma_window_size -> {
                val maWindowSize = maWindowSizeValues!![progress]
                controller.updateMaWindowSize(maWindowSize)
            }
        }
    }

    private fun setupRunning(running: Boolean) {
        if (running) {
            setEnabled(disabledLayouts, false)
            sp_channel_number.isEnabled = false
            sp_packet_count.isEnabled = false
            range_test_start_stop.setText(buttonStringIdOn)
            range_test_start_stop.isEnabled = mode != RangeTestMode.Rx
        } else {
            setEnabled(disabledLayouts, true)
            sp_channel_number.isEnabled = series1
            sp_packet_count.isEnabled = !range_check_packet_repeat.isChecked
            range_test_start_stop.setText(buttonStringIdOff)
            range_test_start_stop.isEnabled = true
        }
    }

    private fun <T> setupSpinnerValues(spinner: Spinner, values: List<T>) {
        val adapter = ArrayAdapter<T>(spinner.context, R.layout.spinner_item_layout, values)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_layout)
        spinner.adapter = adapter
        spinner.setSelection(0, false)
    }

    private fun setVisibility(views: List<View>, visibility: Int) {
        for (view in views) {
            view.visibility = visibility
        }
    }

    private fun setEnabled(views: List<View>, enabled: Boolean) {
        for (view in views) {
            view.isEnabled = enabled
        }
    }

    private fun setupChartView() {
        val textColor = ContextCompat.getColor(requireContext(), R.color.silabs_dark_gray_text)
        val axisColor = ContextCompat.getColor(requireContext(), R.color.silabs_dark_gray_text)
        val graphColor = ContextCompat.getColor(requireContext(), R.color.silabs_blue)

        chart.apply {
            description?.isEnabled = false
            legend?.isEnabled = false
            setScaleEnabled(false)
            isHighlightPerTapEnabled = false
            isHighlightPerDragEnabled = false
            isDoubleTapToZoomEnabled = false
            axisRight.isEnabled = false
            axisLeft.valueFormatter = YAxisValueFormatter()
            axisLeft.textSize = 10f
            axisLeft.textColor = textColor
            axisLeft.axisMinimum = -100f
            axisLeft.axisMaximum = 25f
            axisLeft.granularity = 25f
            axisLeft.setDrawGridLines(false)
            axisLeft.setDrawAxisLine(true)
            axisLeft.axisLineWidth = 0.5f
            axisLeft.axisLineColor = axisColor
            rendererLeftYAxis = YAxisArrowRenderer(chart.viewPortHandler, chart.axisLeft, chart.rendererLeftYAxis.transformer)
            xAxis.setDrawGridLines(false)
            xAxis.setDrawAxisLine(false)
            xAxis.axisMinimum = 0f
            xAxis.axisMaximum = 50f
            xAxis.valueFormatter = XAxisValueFormatter()
            minOffset = 0f
            setExtraOffsets(-4f, 0f, 4f, 4f)
            axisLeft.addLimitLine(createLimitLine(0f, axisColor))
            axisLeft.addLimitLine(createDashLine(-100f, axisColor))
            axisLeft.addLimitLine(createDashLine(-75f, axisColor))
            axisLeft.addLimitLine(createDashLine(-50f, axisColor))
            axisLeft.addLimitLine(createDashLine(-25f, axisColor))
            axisLeft.addLimitLine(createDashLine(25f, axisColor))
        }

        val chartData = createChartData(graphColor)
        chart.data = chartData
        chartDataSet!!.notifyDataSetChanged()
        chartData.notifyDataChanged()
        chart.notifyDataSetChanged()
        chart.invalidate()
    }

    private fun createChartData(color: Int): LineData {
        val entries: MutableList<Entry> = ArrayList(1024)
        entries.add(Entry((-2).toFloat(), 0f))
        entries.add(Entry((-1).toFloat(), 0f))

        chartDataSet = LineDataSet(entries, null)
        chartDataSet?.apply {
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.1f
            setDrawCircles(false)
            setDrawFilled(true)
            lineWidth = 0f
            this.color = color
            fillColor = color
            fillAlpha = 255
            setDrawHorizontalHighlightIndicator(false)
            setDrawVerticalHighlightIndicator(false)
            fillFormatter = CubicLineSampleFillFormatter()
        }

        chartData = LineData(chartDataSet)
        chartData?.setDrawValues(false)
        return chartData!!
    }

    private fun createLimitLine(position: Float, color: Int): LimitLine {
        return ArrowLimitLine(position).apply {
            lineColor = color
            lineWidth = 0.5f
        }
    }

    private fun createDashLine(position: Float, color: Int): LimitLine {
        val line = LimitLine(position)
        val lineLength = Utils.convertDpToPixel(4f)
        val spaceLength = Utils.convertDpToPixel(2f)
        line.lineColor = color
        line.lineWidth = 0.5f
        line.enableDashedLine(lineLength, spaceLength, 0f)
        return line
    }

    private inner class SeekBarsListener : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser) {
                when {
                    seekBar === sb_range_tx_power -> {
                        sp_tx_power.setSelection(progress)
                    }
                    seekBar === sb_range_payload_length -> {
                        sp_payload_length.setSelection(progress)
                    }
                    seekBar === range_seek_ma_window_size -> {
                        sp_ma_window_size.setSelection(progress)
                    }
                }
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {}
        override fun onStopTrackingTouch(seekBar: SeekBar) {
            updateControllerFromSeekBar(seekBar)
        }
    }

    private inner class SpinnerSeekBarListener(private val seekBar: SeekBar) : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
            seekBar.progress = position
            updateControllerFromSeekBar(seekBar)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {}

    }

    private abstract inner class SpinnerListener : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
            onItemSelected(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {}
        protected abstract fun onItemSelected(index: Int)
    }

    private inner class YAxisValueFormatter : IAxisValueFormatter {
        override fun getFormattedValue(value: Float, axis: AxisBase): String {
            return (if (value > 0) "+$value" else value).toString() + " dBm"
        }
    }

    private inner class XAxisValueFormatter : IAxisValueFormatter {
        override fun getFormattedValue(value: Float, axis: AxisBase): String {
            return ""
        }
    }

    private inner class CubicLineSampleFillFormatter : IFillFormatter {
        override fun getFillLinePosition(dataSet: ILineDataSet, dataProvider: LineDataProvider): Float {
            return (-100).toFloat()
        }
    }

    private class YAxisArrowRenderer(viewPortHandler: ViewPortHandler, yAxis: YAxis, trans: Transformer) : YAxisRenderer(viewPortHandler, yAxis, trans) {
        private val ARROW_SIZE = Utils.convertDpToPixel(3.5f)
        private val arrowPath = Path()
        private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        init {
            arrowPaint.style = Paint.Style.FILL_AND_STROKE
        }

        override fun renderAxisLine(c: Canvas) {
            if (!mYAxis.isEnabled || !mYAxis.isDrawAxisLineEnabled) return
            mAxisLinePaint.color = mYAxis.axisLineColor
            mAxisLinePaint.strokeWidth = mYAxis.axisLineWidth
            arrowPaint.color = mYAxis.axisLineColor
            arrowPath.reset()
            if (mYAxis.axisDependency == YAxis.AxisDependency.LEFT) {
                c.drawLine(mViewPortHandler.contentLeft(), mViewPortHandler.contentTop() - ARROW_SIZE * 1.5f, mViewPortHandler.contentLeft(),
                        mViewPortHandler.contentBottom(), mAxisLinePaint)
                arrowPath.moveTo(mViewPortHandler.contentLeft(), mViewPortHandler.contentTop() - ARROW_SIZE * 1.5f)
                arrowPath.lineTo(mViewPortHandler.contentLeft() + ARROW_SIZE / 2f, mViewPortHandler.contentTop() - ARROW_SIZE * 0.5f)
                arrowPath.lineTo(mViewPortHandler.contentLeft() - ARROW_SIZE / 2f, mViewPortHandler.contentTop() - ARROW_SIZE * 0.5f)
                arrowPath.close()
            } else {
                c.drawLine(mViewPortHandler.contentRight(), mViewPortHandler.contentTop() - ARROW_SIZE * 1.5f, mViewPortHandler.contentRight(),
                        mViewPortHandler.contentBottom(), mAxisLinePaint)
                arrowPath.moveTo(mViewPortHandler.contentRight(), mViewPortHandler.contentTop() - ARROW_SIZE * 1.5f)
                arrowPath.lineTo(mViewPortHandler.contentRight() + ARROW_SIZE / 2f, mViewPortHandler.contentTop() - ARROW_SIZE * 0.5f)
                arrowPath.lineTo(mViewPortHandler.contentRight() - ARROW_SIZE / 2f, mViewPortHandler.contentTop() - ARROW_SIZE * 0.5f)
                arrowPath.close()
            }
            c.drawPath(arrowPath, arrowPaint)
        }

        override fun renderLimitLines(c: Canvas) {
            val limitLines = mYAxis.limitLines
            if (limitLines == null || limitLines.size <= 0) return
            val pts = mRenderLimitLinesBuffer
            pts[0] = 0f
            pts[1] = 0f
            val limitLinePath = mRenderLimitLines
            limitLinePath.reset()
            for (i in limitLines.indices) {
                val l = limitLines[i]
                if (!l.isEnabled) continue
                val clipRestoreCount = c.save()
                mLimitLineClippingRect.set(mViewPortHandler.contentRect)
                mLimitLineClippingRect.inset(0f, -l.lineWidth)
                c.clipRect(mLimitLineClippingRect)
                mLimitLinePaint.style = Paint.Style.STROKE
                mLimitLinePaint.color = l.lineColor
                mLimitLinePaint.strokeWidth = l.lineWidth
                mLimitLinePaint.pathEffect = l.dashPathEffect
                pts[1] = l.limit
                mTrans.pointValuesToPixel(pts)
                limitLinePath.moveTo(mViewPortHandler.contentLeft(), pts[1])
                limitLinePath.lineTo(mViewPortHandler.contentRight(), pts[1])
                c.drawPath(limitLinePath, mLimitLinePaint)
                limitLinePath.reset()
                // c.drawLines(pts, mLimitLinePaint);
                if (l is ArrowLimitLine) {
                    arrowPaint.color = l.getLineColor()
                    arrowPath.reset()
                    arrowPath.moveTo(mViewPortHandler.contentRight(), pts[1])
                    arrowPath.lineTo(mViewPortHandler.contentRight() - ARROW_SIZE, pts[1] + ARROW_SIZE / 2f)
                    arrowPath.lineTo(mViewPortHandler.contentRight() - ARROW_SIZE, pts[1] - ARROW_SIZE / 2f)
                    arrowPath.close()
                    c.drawPath(arrowPath, arrowPaint)
                }
                val label = l.label

                // if drawing the limit-value label is enabled
                if (label != null && label != "") {
                    mLimitLinePaint.style = l.textStyle
                    mLimitLinePaint.pathEffect = null
                    mLimitLinePaint.color = l.textColor
                    mLimitLinePaint.typeface = l.typeface
                    mLimitLinePaint.strokeWidth = 0.5f
                    mLimitLinePaint.textSize = l.textSize
                    val labelLineHeight = Utils.calcTextHeight(mLimitLinePaint, label).toFloat()
                    val xOffset = Utils.convertDpToPixel(4f) + l.xOffset
                    val yOffset = l.lineWidth + labelLineHeight + l.yOffset
                    val position = l.labelPosition
                    if (position == LimitLine.LimitLabelPosition.RIGHT_TOP) {
                        mLimitLinePaint.textAlign = Paint.Align.RIGHT
                        c.drawText(label,
                                mViewPortHandler.contentRight() - xOffset,
                                pts[1] - yOffset + labelLineHeight, mLimitLinePaint)
                    } else if (position == LimitLine.LimitLabelPosition.RIGHT_BOTTOM) {
                        mLimitLinePaint.textAlign = Paint.Align.RIGHT
                        c.drawText(label,
                                mViewPortHandler.contentRight() - xOffset,
                                pts[1] + yOffset, mLimitLinePaint)
                    } else if (position == LimitLine.LimitLabelPosition.LEFT_TOP) {
                        mLimitLinePaint.textAlign = Paint.Align.LEFT
                        c.drawText(label,
                                mViewPortHandler.contentLeft() + xOffset,
                                pts[1] - yOffset + labelLineHeight, mLimitLinePaint)
                    } else {
                        mLimitLinePaint.textAlign = Paint.Align.LEFT
                        c.drawText(label,
                                mViewPortHandler.offsetLeft() + xOffset,
                                pts[1] + yOffset, mLimitLinePaint)
                    }
                }
                c.restoreToCount(clipRestoreCount)
            }
        }
    }

    private inner class ArrowLimitLine(limit: Float) : LimitLine(limit)

    companion object {
        const val ARG_MODE = "ARG_MODE"
        private const val SERIES_2_REGEX = "BRD41(71|80|81)"
    }
}