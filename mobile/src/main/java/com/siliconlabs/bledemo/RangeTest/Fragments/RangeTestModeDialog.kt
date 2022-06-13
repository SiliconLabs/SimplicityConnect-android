package com.siliconlabs.bledemo.RangeTest.Fragments

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.RangeTest.Activities.RangeTestActivity
import com.siliconlabs.bledemo.RangeTest.Models.RangeTestMode
import com.siliconlabs.bledemo.RangeTest.Models.TxPower
import com.siliconlabs.bledemo.RangeTest.Presenters.RangeTestPresenter.Controller
import com.siliconlabs.bledemo.RangeTest.Presenters.RangeTestPresenter.RangeTestView
import kotlinx.android.synthetic.main.dialog_range_test_mode.*
import java.text.DecimalFormat
import java.util.*

/**
 * @author Comarch S.A.
 */
class RangeTestModeDialog : DialogFragment(), RangeTestView {
    lateinit var controller: Controller

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireContext(), theme) {
            override fun onBackPressed() {
                controller.cancelTestMode()
                dismiss()
            }
        }.apply {
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        setStyle(STYLE_NO_TITLE, theme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_range_test_mode, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        controller = activity as RangeTestActivity
        controller.setView(this)
        isCancelable = false

        tx_mode.setOnClickListener {
            onTxModeButtonClicked()
        }

        rx_mode.setOnClickListener {
            onRxModeButtonClicked()
        }

        btn_cancel.setOnClickListener {
            onCancelButtonClicked()
        }

    }

    private fun onTxModeButtonClicked() {
        dismiss()
        controller.setView(null)
        controller.initTestMode(RangeTestMode.Tx)
    }

    private fun onRxModeButtonClicked() {
        dismiss()
        controller.setView(null)
        controller.initTestMode(RangeTestMode.Rx)
    }

    private fun onCancelButtonClicked() {
        dismiss()
        controller.cancelTestMode()
    }

    override fun runOnUiThread(runnable: Runnable?) {
        val view = view
        view?.post(runnable)
    }

    override fun showDeviceName(name: String?) {
        tv_device_name.text = name
    }

    override fun showModelNumber(number: String?, running: Boolean?) {
        tv_device_number.text = number
    }

    override fun showTxPower(power: TxPower?, values: List<TxPower>) {
        val value = power?.asDisplayValue()
        val formatter = DecimalFormat("#.##")
        tv_tx_power.text = String.format(Locale.ROOT, "%sdBm", formatter.format(value?.toDouble()))
    }

    override fun showPayloadLength(length: Int, values: List<Int>) {
        // not available in this view
    }

    override fun showMaWindowSize(size: Int, values: List<Int>) {
        // not available in this view
    }

    override fun showChannelNumber(number: Int) {
        // not available in this view
    }

    override fun showPacketCountRepeat(enabled: Boolean) {
        // not available in this view
    }

    override fun showPacketRequired(required: Int) {
        // not available in this view
    }

    override fun showPacketSent(sent: Int) {
        // not available in this view
    }

    override fun showPer(per: Float) {
        // not available in this view
    }

    override fun showMa(ma: Float) {
        // not available in this view
    }

    override fun showRemoteId(id: Int) {
        // not available in this view
    }

    override fun showSelfId(id: Int) {
        // not available in this view
    }

    override fun showUartLogEnabled(enabled: Boolean) {
        // not available in this view
    }

    override fun showRunningState(running: Boolean) {
        // not available in this view
    }

    override fun showTestRssi(rssi: Int) {
        // not available in this view
    }

    override fun showTestRx(received: Int, required: Int) {
        // not available in this view
    }

    override fun clearTestResults() {
        // not available in this view
    }

    override fun showPhy(phy: Int, values: LinkedHashMap<Int, String>) {
        // not available in this view
    }
}