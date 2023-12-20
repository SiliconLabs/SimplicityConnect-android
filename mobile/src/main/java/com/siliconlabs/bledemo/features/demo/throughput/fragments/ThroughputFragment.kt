package com.siliconlabs.bledemo.features.demo.throughput.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.FragmentThroughputBinding
import com.siliconlabs.bledemo.features.demo.throughput.activities.ThroughputActivity
import com.siliconlabs.bledemo.features.demo.throughput.views.SpeedView
import com.siliconlabs.bledemo.features.demo.throughput.viewmodels.ThroughputViewModel


class ThroughputFragment : Fragment(R.layout.fragment_throughput) {
    private lateinit var viewModel: ThroughputViewModel
    private val unitsArray = arrayListOf(
        "0",
        "250kbps",
        "500kbps",
        "750kbps",
        "1Mbit",
        "1.25Mbit",
        "1.5Mbit",
        "1.75Mbit",
        "2Mbit"
    )
    private lateinit var binding: FragmentThroughputBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(ThroughputViewModel::class.java)
        //speed_view.setUnitsArray(unitsArray)
        binding.speedView.setUnitsArray(unitsArray)

        handleClickEvents()
        observeChanges()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentThroughputBinding.inflate(inflater, container, false)
        return binding.root
    }


    private fun observeChanges() {
        viewModel.phyStatus.observe(viewLifecycleOwner, Observer {
            // tv_phy_status_value.text = getString(it.stringResId)
            binding.viewConnectionParameters.tvPhyStatusValue.text = getString(it.stringResId)
        })
        viewModel.connectionInterval.observe(viewLifecycleOwner, Observer {
            //tv_interval_value.text = it.toString().plus(getString(R.string.throughput_ms))
            binding.viewConnectionParameters.tvIntervalValue.text =
                it.toString().plus(getString(R.string.throughput_ms))
        })
        viewModel.slaveLatency.observe(viewLifecycleOwner, Observer {
            // tv_latency_value.text = it.toString().plus(getString(R.string.throughput_ms))
            binding.viewConnectionParameters.tvLatencyValue.text =
                it.toString().plus(getString(R.string.throughput_ms))
        })
        viewModel.supervisionTimeout.observe(viewLifecycleOwner, Observer {
            // tv_supervision_timeout_value.text =
            binding.viewConnectionParameters.tvSupervisionTimeoutValue.text =
                it.toString().plus(getString(R.string.throughput_ms))
        })
        viewModel.mtuSize.observe(viewLifecycleOwner, Observer {
//            tv_mtu_value.text = getString(R.string.throughput_n_bytes, it)
            binding.viewConnectionParameters.tvMtuValue.text =
                getString(R.string.throughput_n_bytes, it)
        })
        viewModel.pduSize.observe(viewLifecycleOwner, Observer {
            // tv_pdu_value.text = getString(R.string.throughput_n_bytes, it)
            binding.viewConnectionParameters.tvPduValue.text =
                getString(R.string.throughput_n_bytes, it)
        })
        viewModel.throughputSpeed.observe(viewLifecycleOwner, Observer {
            updateSpeed(it)
        })
        viewModel.isDownloadActive.observe(viewLifecycleOwner, Observer {
            // btn_start_stop.isEnabled = !it
            binding.btnStartStop.isEnabled = !it
            toggleRadioButtonsClickable(!it)
            when (viewModel.isDownloadingNotifications) {
                // true -> rb_notifications.isChecked = true
//                false -> rb_indications.isChecked = true
                true -> binding.viewConnectionParameters.rbNotifications.isChecked = true
                false -> binding.viewConnectionParameters.rbIndications.isChecked = true
            }
        })
    }

    private fun getProgress(speed: Int): Int {
        return ((speed / 2000000.0) * 100).toInt()
    }

    private fun handleClickEvents() {
        /*  btn_start_stop.setOnClickListener */
        binding.btnStartStop.setOnClickListener {
            when (viewModel.isUploadActive) {
                true -> {
                    (activity as ThroughputActivity).stopUploadTest()
                    //  btn_start_stop.text = requireContext().getString(R.string.button_start)
                    binding.btnStartStop.text = requireContext().getString(R.string.button_start)
                    toggleRadioButtonsClickable(true)
                }

                false -> {
                    (activity as ThroughputActivity).startUploadTest(/*rb_notifications.isChecked*/
                        binding.viewConnectionParameters.rbNotifications.isChecked
                    )
                    // btn_start_stop.text = requireContext().getString(R.string.button_stop)
                    binding.btnStartStop.text = requireContext().getString(R.string.button_stop)
                    toggleRadioButtonsClickable(false)
                }
            }
        }
    }

    private fun toggleRadioButtonsClickable(enabled: Boolean) {
        for (/*button in rl_radio_boxes.children*/
        button in binding.viewConnectionParameters.rlRadioBoxes.children) {
            button.isEnabled = enabled
        }
    }

    private fun updateSpeed(speed: Int) {
      //  speed_view
        binding.speedView
            .updateSpeed(
            getProgress(speed),
            getSpeedAsString(speed),
            getUnitAsString(speed),
            getTestMode()
        )
    }

    private fun getTestMode(): SpeedView.Mode {
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