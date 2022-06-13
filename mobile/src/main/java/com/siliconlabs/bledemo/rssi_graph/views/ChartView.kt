package com.siliconlabs.bledemo.rssi_graph.views

import android.content.Context
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.utils.Utils
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.rssi_graph.model.GraphPoint
import com.siliconlabs.bledemo.rssi_graph.model.ScannedDevice
import kotlinx.android.synthetic.main.activity_rssi_graph.view.*
import kotlin.math.roundToInt

class ChartView(
        context: Context,
        rootView: View?,
        private val timeArrowsHandler: TimeArrowsHandler?
) : LineChart(context) {

    constructor(context: Context) : this(context, null,null)

    private val chartView: LineChart = rootView?.rssi_chart!!
    var startScanTimestamp: Long = 0L

    private val painter = Painter()

    private val graphAxisColor = ContextCompat.getColor(context, R.color.silabs_dark_gray_text)

    private val horizontalDottedLines = mutableListOf(0f, -20f, -40f, -60f, -80f, -100f)
    private val verticalDottedLines = mutableListOf(5f, 10f, 15f, 20f, 25f, 30f)


    fun initialize() {
        Creator().configure()
    }

    fun reset() {
        chartView.apply {
            fitScreen()
            data?.clearValues()
            notifyDataSetChanged()
            clear()

            xAxis.apply {
                axisMinimum = 0f
                axisMaximum = 30f

            }
            axisLeft.apply {
                axisMinimum = -100f
                axisMaximum = 0f
            }
            data = LineData()
        }
    }

    fun addNewChartData(newData: ScannedDevice) {
        chartView.apply {
            data.addDataSet(
                    LineDataSet(newData.graphData.map {
                        Entry(
                            it.convertTimestampToSeconds(startScanTimestamp),
                            it.rssi.toFloat())
                    }, null)
            )
            notifyDataSetChanged()
            invalidate()
        }
    }

    fun updateChartData(newData: List<ScannedDevice>, highlightedDevice: ScannedDevice?) {
        painter.displayNewData(newData, highlightedDevice)
    }

    fun skipToEnd() {
        chartView.moveViewTo(chartView.xAxis.mAxisMaximum, calculateVerticalCenter(), YAxis.AxisDependency.LEFT)
    }

    fun skipToStart() {
        chartView.moveViewTo(0f, calculateVerticalCenter(), YAxis.AxisDependency.LEFT)
    }

    private fun createDashLine(position: Float): LimitLine {
        val lineLength = Utils.convertDpToPixel(4f)
        val spaceLength = Utils.convertDpToPixel(2f)
        return LimitLine(position).apply {
            lineColor = graphAxisColor
            lineWidth = 0.5f
            enableDashedLine(lineLength, spaceLength, 0f)
        }
    }

    private fun calculateLeftAnchor(isChartOnCurrentMoment: Boolean) : Float {
        chartView.let {
            return if (isChartOnCurrentMoment) it.xAxis.mAxisMaximum - it.visibleXRange
            else {
                if (it.visibleXRange.roundToInt() < 30) { /* Zoom-in on X axis. */
                    it.highestVisibleX - it.visibleXRange /* TODO: should freeze chart on chosen range. */
                } else {
                    it.highestVisibleX - it.visibleXRange
                }
            }
        }
    }

    private fun calculateVerticalCenter() : Float {
        chartView.let {
            val visibleTop = it.getValuesByTouchPoint(
                    it.viewPortHandler.contentLeft(),
                    it.viewPortHandler.contentTop(),
                    YAxis.AxisDependency.LEFT
            ).y

            val visibleBottom = it.getValuesByTouchPoint(
                    it.viewPortHandler.contentLeft(),
                    it.viewPortHandler.contentBottom(),
                    YAxis.AxisDependency.LEFT
            ).y
            return ((visibleTop + visibleBottom) / 2).toFloat()
        }
    }

    interface TimeArrowsHandler {
        fun handleVisibility(isStartArrowVisible: Boolean, isEndArrowVisible: Boolean)
    }

    private val chartGestureListener = object : OnChartGestureListener {
        override fun onChartGestureStart(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {}
        override fun onChartGestureEnd(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {}
        override fun onChartLongPressed(me: MotionEvent?) {}

        override fun onChartDoubleTapped(me: MotionEvent?) {
            chartView.apply {
                val touchedCoordinates = getValuesByTouchPoint(
                        me?.x ?: 0f,
                        me?.y ?: 0f,
                        YAxis.AxisDependency.LEFT
                )
                zoom(0f, 0f, center.x, center.y)
                moveViewToX(touchedCoordinates.x.toFloat() - visibleXRange / 2)
            }
        }

        override fun onChartSingleTapped(me: MotionEvent?) {}
        override fun onChartFling(me1: MotionEvent?, me2: MotionEvent?, velocityX: Float, velocityY: Float) {}
        override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {}
        override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {}

    }

    private inner class Painter {

        fun displayNewData(newData: List<ScannedDevice>, highlightedDevice: ScannedDevice?) {
            val dataSets = createDataSets(newData, highlightedDevice)

            chartView.apply {
                data = LineData(dataSets as List<ILineDataSet>).apply {
                    setDrawValues(false)
                }
                setVisibleYRange(axisLeft.mAxisRange, 30f, YAxis.AxisDependency.LEFT)
                setVisibleXRange(30f, 5f)
            }
            showChart()
        }

        private fun createDataSets(newData: List<ScannedDevice>, highlightedDevice: ScannedDevice?) : List<LineDataSet> {
            synchronized(newData) {
                return newData.map { device ->
                    LineDataSet(convertToSetEntries(device.graphData), null).also { dataSet ->
                        highlightedDevice?.let {
                            setHighlightedDataSetConfiguration(dataSet, device, it)
                        } ?: setDefaultDataSetConfiguration(dataSet, device)
                    }
                }
            }
        }

        private fun convertToSetEntries(graphData: List<GraphPoint>) : MutableList<Entry>{
            synchronized(graphData) {
                return graphData.map { point -> Entry(
                        point.convertTimestampToSeconds(startScanTimestamp),
                        point.rssi.toFloat()
                ).also {
                    checkForAxisRangeUpdate(it)
                }
                }.toMutableList()
            }
        }

        private fun setDataSetConfiguration(set: LineDataSet) {
            set.apply {
                mode = LineDataSet.Mode.LINEAR
                setDrawCircles(false)
                fillAlpha = 255
            }
        }

        private fun setDefaultDataSetConfiguration(set: LineDataSet, device: ScannedDevice) {
            setDataSetConfiguration(set)
            set.apply {
                lineWidth = 1f
                color = device.dataColor
                fillColor = device.dataColor
            }
        }

        private fun setHighlightedDataSetConfiguration(set: LineDataSet, currentDevice: ScannedDevice,
                                                       highlightedDevice: ScannedDevice?) {
            setDataSetConfiguration(set)
            set.apply {
                if (currentDevice.address == highlightedDevice?.address) {
                    lineWidth = 3f
                    color = currentDevice.dataColor
                    fillColor = currentDevice.dataColor
                } else {
                    lineWidth = 1f
                    color = GRAYED_OUT_DATA_COLOR
                    fillColor = GRAYED_OUT_DATA_COLOR
                }
            }
        }

        private fun checkForAxisRangeUpdate(entry: Entry) {
            when {
                entry.y < chartView.axisLeft.axisMinimum -> {
                    if (entry.y + HORIZONTAL_DOTTED_LINES_SPACING <= horizontalDottedLines.last()) {
                        val newLinePosition = horizontalDottedLines.last() - HORIZONTAL_DOTTED_LINES_SPACING
                        horizontalDottedLines.add(newLinePosition)
                        chartView.axisLeft.addLimitLine(createDashLine(newLinePosition))
                    }
                    chartView.axisLeft.mAxisMinimum = entry.y
                }
                entry.y > chartView.axisLeft.axisMaximum -> {
                    if (entry.y - HORIZONTAL_DOTTED_LINES_SPACING >= horizontalDottedLines.first()) {
                        val newLinePosition = horizontalDottedLines.first() + HORIZONTAL_DOTTED_LINES_SPACING
                        horizontalDottedLines.add(0, newLinePosition)
                        chartView.axisLeft.addLimitLine(createDashLine(newLinePosition))
                    }
                    chartView.axisLeft.mAxisMaximum = entry.x
                }
                entry.x > chartView.xAxis.mAxisMaximum -> {
                    if (entry.x - VERTICAL_DOTTED_LINES_SPACING >= verticalDottedLines.last()) {
                        val newLinePosition = verticalDottedLines.last() + VERTICAL_DOTTED_LINES_SPACING
                        verticalDottedLines.add(newLinePosition)
                        chartView.xAxis.addLimitLine(createDashLine(newLinePosition))
                    }
                    chartView.xAxis.mAxisMaximum = entry.x
                }
            }
        }

        private fun showChart() {
            chartView.let {
                val isChartOnCurrentMoment = it.xAxis.mAxisMaximum - it.highestVisibleX < CURRENT_MOMENT_MARGIN
                val xLeftAnchor = calculateLeftAnchor(isChartOnCurrentMoment)
                val yCenter = calculateVerticalCenter()

                timeArrowsHandler?.handleVisibility(it.lowestVisibleX > 0, !isChartOnCurrentMoment)
                it.moveViewTo(xLeftAnchor, yCenter, YAxis.AxisDependency.LEFT)
            }
        }
    }

    private inner class Creator {
        private val graphTextColor = ContextCompat.getColor(context, R.color.silabs_dark_gray_text)

        fun configure() {
            setupConfigurationFlags()
            chartView.onChartGestureListener = chartGestureListener
            initYAxis(graphTextColor)
            initXAxis(graphTextColor)

            chartView.apply {
                data = createSingleDataPoint() // to show lines of the graph
                data.notifyDataChanged()
                invalidate()
            }
        }

        private fun setupConfigurationFlags() {
            chartView.apply {
                description.isEnabled = false
                legend.isEnabled = false
                isHighlightPerTapEnabled = false
                isHighlightPerDragEnabled = false
                isDoubleTapToZoomEnabled = false
                axisRight.isEnabled = false
                minOffset = 16f
            }
        }

        private fun initYAxis(graphTextColor: Int) {
            chartView.axisLeft.apply {
                setAxisConfiguration(this, graphTextColor)
                axisMinimum = -100f
                axisMaximum = 0f
                valueFormatter = amplitudeValueFormatter
                horizontalDottedLines.forEach { addLimitLine(createDashLine(it)) }
            }
        }

        private fun initXAxis(graphTextColor: Int) {
            chartView.xAxis.apply {
                setAxisConfiguration(this, graphTextColor)
                position = XAxis.XAxisPosition.BOTTOM
                axisMinimum = 0f
                axisMaximum = 30f
                valueFormatter = timeValueFormatter
                verticalDottedLines.forEach { addLimitLine(createDashLine(it)) }
            }
        }

        private fun setAxisConfiguration(axis: AxisBase, graphTextColor: Int) {
            axis.apply {
                setDrawAxisLine(true)
                setDrawGridLines(false)
                textSize = 10f
                textColor = graphTextColor
                axisLineColor = graphAxisColor
                axisLineWidth = 0.5f
                granularity = 1f
            }
        }

        private fun createSingleDataPoint(): LineData {
            val entries = mutableListOf(Entry(0f, 0f)) // initialize with a single point of data
            val chartDataSet = LineDataSet(entries, null)
            return LineData(chartDataSet).apply {
                setDrawValues(false)
            }
        }

        private val timeValueFormatter = IAxisValueFormatter { value, _ -> "${value.toInt()} s" }
        private val amplitudeValueFormatter = IAxisValueFormatter { value, _ -> "${value.toInt()} dBm" }
    }

    companion object {
        private val GRAYED_OUT_DATA_COLOR = Color.rgb(128, 128, 128)
        private const val HORIZONTAL_DOTTED_LINES_SPACING = 20f
        private const val VERTICAL_DOTTED_LINES_SPACING = 5f
        private const val CURRENT_MOMENT_MARGIN = 3f
    }
}