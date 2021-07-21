package com.siliconlabs.bledemo.throughput.fragments


import android.os.Bundle
import android.view.View
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.throughput.activities.ThroughputActivity
import com.siliconlabs.bledemo.throughput.views.SpeedView
import com.siliconlabs.bledemo.throughput.viewmodels.ThroughputViewModel
import kotlinx.android.synthetic.main.fragment_throughput.*
import kotlinx.android.synthetic.main.throughput_connection_parameters.*

class ThroughputFragment : Fragment(R.layout.fragment_throughput) {
    private lateinit var viewModel: ThroughputViewModel
    private val unitsArray = arrayListOf("0", "250kbps", "500kbps", "750kbps", "1Mbit", "1.25Mbit", "1.5Mbit", "1.75Mbit", "2Mbit")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(ThroughputViewModel::class.java)
        speed_view.setUnitsArray(unitsArray)

        handleClickEvents()
        observeChanges()
    }

    private fun observeChanges() {
        viewModel.phyStatus.observe(viewLifecycleOwner, Observer {
            tv_phy_status_value.text = getString(it.stringResId)
        })
        viewModel.connectionInterval.observe(viewLifecycleOwner, Observer {
            tv_interval_value.text = it.toString().plus(getString(R.string.throughput_ms))
        })
        viewModel.slaveLatency.observe(viewLifecycleOwner, Observer {
            tv_latency_value.text = it.toString().plus(getString(R.string.throughput_ms))
        })
        viewModel.supervisionTimeout.observe(viewLifecycleOwner, Observer {
            tv_supervision_timeout_value.text = it.toString().plus(getString(R.string.throughput_ms))
        })
        viewModel.mtuSize.observe(viewLifecycleOwner, Observer {
            tv_mtu_value.text = getString(R.string.throughput_n_bytes, it)
        })
        viewModel.pduSize.observe(viewLifecycleOwner, Observer {
            tv_pdu_value.text = getString(R.string.throughput_n_bytes, it)
        })
        viewModel.throughputSpeed.observe(viewLifecycleOwner, Observer {
            updateSpeed(it)
        })
        viewModel.isDownloadActive.observe(viewLifecycleOwner, Observer {
            btn_start_stop.isEnabled = !it
            toggleRadioButtonsClickable(!it)
            when (viewModel.isDownloadingNotifications) {
                true -> rb_notifications.isChecked = true
                false -> rb_indications.isChecked = true
            }
        })
    }

    private fun getProgress(speed: Int): Int {
        return ((speed / 2000000.0) * 100).toInt()
    }

    private fun handleClickEvents() {
        btn_start_stop.setOnClickListener {
            when (viewModel.isUploadActive) {
                true -> {
                    (activity as ThroughputActivity).stopUploadTest()
                    btn_start_stop.text = requireContext().getString(R.string.button_start)
                    toggleRadioButtonsClickable(true)
                }
                false -> {
                    (activity as ThroughputActivity).startUploadTest(rb_notifications.isChecked)
                    btn_start_stop.text = requireContext().getString(R.string.button_stop)
                    toggleRadioButtonsClickable(false)
                }
            }
        }
    }

    private fun toggleRadioButtonsClickable(enabled: Boolean) {
        for (button in rl_radio_boxes.children) {
            button.isEnabled = enabled
        }
    }

    private fun updateSpeed(speed: Int) {
        speed_view.updateSpeed(getProgress(speed), getSpeedAsString(speed), getUnitAsString(speed), getTestMode())
    }

    private fun getTestMode() : SpeedView.Mode {
        return when {
            viewModel.isDownloadActive.value == true -> SpeedView.Mode.DOWNLOAD
            viewModel.isUploadActive -> SpeedView.Mode.UPLOAD
            else -> SpeedView.Mode.NONE
        }
    }

    private fun getSpeedAsString(speed: Int): String {
        return if (speed >= 1000000) {
            (speed / 1000000.0).toString()
        } else {
            (speed / 1000.0).toString()
        }
    }

    private fun getUnitAsString(speed: Int): String {
        return if (speed >= 1000000) {
            "Mbps"
        } else {
            "kbps"
        }
    }

}