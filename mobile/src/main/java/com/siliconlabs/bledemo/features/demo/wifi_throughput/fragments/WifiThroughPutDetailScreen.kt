package com.siliconlabs.bledemo.features.demo.wifi_throughput.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Visibility
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.FragmentWifiThroughPutDetailScreenBinding
import com.siliconlabs.bledemo.features.demo.throughput.views.SpeedView
import com.siliconlabs.bledemo.features.demo.wifi_throughput.activities.WifiThroughputActivity
import com.siliconlabs.bledemo.features.demo.wifi_throughput.adapter.WifiThroughputAdapter
import com.siliconlabs.bledemo.features.demo.wifi_throughput.utils.ThroughputUtils
import com.siliconlabs.bledemo.features.demo.wifi_throughput.viewmodel.WifiThroughputViewModel
import com.siliconlabs.bledemo.utils.CustomToastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WifiThroughPutDetailScreen : Fragment() {
    private var mBundle : Bundle? = null
    private lateinit var binding: FragmentWifiThroughPutDetailScreenBinding

    private val throughPutUnitsArray =
        arrayListOf(
            "0",
            "12.5 Mbps",
            "25 Mbps",
            "37.5 Mbps",
            "50 Mbps",
            "62.5 Mbps",
            "75 Mbps",
            "87.5 Mbps",
            "100 Mbps"
        )
    private val viewModel : WifiThroughputViewModel by viewModels()
    var list: MutableList<String> = mutableListOf()
    var throughPutTestModeDownload = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        // Inflate the layout for this fragment
        binding = FragmentWifiThroughPutDetailScreenBinding.inflate(inflater, container, false)
        binding.wifiSpeedView.setUnitsArray(throughPutUnitsArray)
        binding.finalResultLl.visibility = View.GONE
        binding.headers.visibility = View.GONE

        mBundle = arguments


        updateThroughPutSpeed()

        binding.wifiTpMainLayout.setOnClickListener{
            //Do nothing. It is to avoid background touch.
        }

        viewModel.isConnceted()
            .observe(viewLifecycleOwner) {
                if (it){
                    binding.connectionStatus.visibility = View.GONE
                }else{
                    binding.connectionStatus.visibility = View.VISIBLE
                }
            }

        val customAdapter = WifiThroughputAdapter(list)
        val recyclerView =  binding.incrementalLog
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = customAdapter
        viewModel.updateLogsPerSecond().observe(viewLifecycleOwner){
            customAdapter.submitList(it)
            recyclerView.visibility = View.VISIBLE
            if (it.isNotEmpty()){
                binding.headers.visibility = View.VISIBLE
                recyclerView.smoothScrollToPosition(it.size - 1)
            }
        }

        startThroughPutServer()

        binding.start.setOnClickListener {
            binding.finalResultLl.visibility = View.GONE
            binding.headers.visibility = View.GONE
            binding.start.isEnabled = false
            startThroughPutServer()
        }

        binding.cancel.setOnClickListener {
            //dialog?.dismiss()
            viewModel.stop()
            var fm : FragmentManager = requireActivity().supportFragmentManager
            fm.popBackStack()
        }

        viewModel.updateFinalResult().observe(viewLifecycleOwner){
            if (null != it && it.isNotEmpty()){
                binding.wifiSpeedView.updateSpeed( getProgress(0f),
                    getSpeedAsString(0f),
                    getUnitAsString(0f),
                    getTestMode())
                binding.start.isEnabled = true
                binding.finalResultLl.visibility = View.VISIBLE
                if (throughPutTestModeDownload){
                    val finalBytesSent = "<font color=${context?.getColor(R.color.masala)}>${getString(R.string.total_bytes_received)}</font>: ${it.get("transfer")}"
                    binding.finalBytesSent.text = Html.fromHtml(finalBytesSent,HtmlCompat.FROM_HTML_MODE_LEGACY)
                }else{
                    val totalBytesSent = "<font color=${context?.getColor(R.color.masala)}>${getString(R.string.total_bytes_sent)}</font>: ${it.get("transfer")}"
                    binding.finalBytesSent.text = Html.fromHtml(totalBytesSent,HtmlCompat.FROM_HTML_MODE_LEGACY)
                }
                val throughPutAchieved = "<font color=${context?.getColor(R.color.masala)}>${getString(R.string.throughput_achieved)}</font>: @ ${it.get("bandwidth")} ${getString(R.string.mbps_in_30_successfully)}"
                binding.finalAcheievedBandvidth.text = Html.fromHtml(throughPutAchieved,HtmlCompat.FROM_HTML_MODE_LEGACY)
            }
        }

        viewModel.updateTotalBytesInProgress().observe(viewLifecycleOwner) { totalBytes ->
            // Only show progress if not waiting for connection
            if (viewModel.waitingForConnection().value == null && totalBytes > 0) {
                // Update UI with real-time bytes transferred during test
                val formattedBytes = viewModel.bytesToHumanReadableSize(totalBytes.toFloat())
                val statusMessage = if (throughPutTestModeDownload) {
                    "<b><font color=${context?.getColor(R.color.masala)}>${getString(R.string.test_in_progress)}</font> ${getString(R.string.total_data_received)}: $formattedBytes</b>"
                } else {
                    "<b><font color=${context?.getColor(R.color.masala)}>${getString(R.string.test_in_progress)}</font> ${getString(R.string.total_data_sent)}: $formattedBytes</b>"
                }
                binding.finalBytesSent.text = Html.fromHtml(statusMessage, HtmlCompat.FROM_HTML_MODE_LEGACY)
                binding.finalResultLl.visibility = View.VISIBLE
            }
        }

        viewModel.waitingForConnection().observe(viewLifecycleOwner) { role ->
            // Show waiting message before connection is established
            if (role != null) {
                val waitingMessage = if (role == "client") {
                    "<b>${getString(R.string.waiting_for_client)}</b>"
                } else {
                    "<b>${getString(R.string.waiting_for_server)}</b>"
                }
                binding.finalBytesSent.text = Html.fromHtml(waitingMessage, HtmlCompat.FROM_HTML_MODE_LEGACY)
                binding.finalResultLl.visibility = View.VISIBLE
            }
            // When role is null (connected), the progress observer will take over
        }

        viewModel.handleException().observe(viewLifecycleOwner){
            if (it){
                binding.start.isEnabled = true
                recyclerView.visibility = View.GONE
               // binding.finalResultLl.visibility = View.GONE
                /*Toast.makeText(context,
                    getString(R.string.connection_couldn_t_be_established), Toast.LENGTH_LONG).show()*/
                CustomToastManager.show(
                    requireContext(),getString(R.string.connection_couldn_t_be_established),5000
                )
                viewModel.stop()
                requireActivity().supportFragmentManager.popBackStack()
                (activity as? WifiThroughputActivity)?.updateActionBarTitle(requireContext().getString(R.string.wifi_title_Throughput))
            }
        }

        viewModel.handleTlsZeroBytes().observe(viewLifecycleOwner){
            if (it){
                binding.start.isEnabled = true
                recyclerView.visibility = View.GONE
                // binding.finalResultLl.visibility = View.GONE
                /*Toast.makeText(context,
                    getString(R.string.ensure_right_combination_of_fw_is_flashed), Toast.LENGTH_LONG).show()*/
                CustomToastManager.show(
                    requireContext(),getString(R.string.ensure_right_combination_of_fw_is_flashed),5000
                )
                viewModel.stop()
                requireActivity().supportFragmentManager.popBackStack()
                (activity as? WifiThroughputActivity)?.updateActionBarTitle(requireContext().getString(R.string.wifi_title_Throughput))
            }
        }

        return binding.root
    }

    private fun startThroughPutServer() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            mBundle?.let {
                if (it.getString(ThroughputUtils.throughPutType)?.equals(
                        ThroughputUtils.THROUGHPUT_TYPE_TCP_UPLOAD,
                        ignoreCase = true
                    ) == true
                ) {
                    throughPutTestModeDownload = false
                    //set the actionBar title
                    withContext(Dispatchers.Main) {
                        (activity as? WifiThroughputActivity)?.updateActionBarTitle(it.getString(ThroughputUtils.throughPutType).toString())
                        binding.connectionStatus.visibility = View.VISIBLE
                        if(binding.connectionStatus.isVisible){
                            binding.connectionStatusTv.text = getString(R.string.waiting_for_client_to_connect)
                        }
                    }
                    viewModel.tcpClient(
                        it.getString(ThroughputUtils.ipAddress)!!,
                        it.getString(ThroughputUtils.portNumber)!!.toInt()
                    )
                } else if (it.getString(ThroughputUtils.throughPutType)?.equals(
                        ThroughputUtils.THROUGHPUT_TYPE_TCP_DOWNLOAD,
                        ignoreCase = true
                    ) == true
                ) {
                    //set the actionBar title
                    throughPutTestModeDownload = true
                    withContext(Dispatchers.Main) {
                        (activity as? WifiThroughputActivity)?.updateActionBarTitle(it.getString(ThroughputUtils.throughPutType).toString())
                        binding.connectionStatus.visibility = View.VISIBLE
                        if(binding.connectionStatus.isVisible){
                            binding.connectionStatusTv.text = getString(R.string.wifi_throughput_connecting_status)
                        }
                    }

                    viewModel.tcpServer(it.getString(ThroughputUtils.portNumber)!!.toInt())
                } else if (it.getString(ThroughputUtils.throughPutType)?.equals(
                        ThroughputUtils.THROUGHPUT_TYPE_UDP_UPLOAD,
                        ignoreCase = true
                    ) == true
                ) {
                    //set the actionBar title
                    throughPutTestModeDownload = false
                    withContext(Dispatchers.Main) {
                        (activity as? WifiThroughputActivity)?.updateActionBarTitle(it.getString(ThroughputUtils.throughPutType).toString())
                        binding.connectionStatus.visibility = View.VISIBLE
                        if(binding.connectionStatus.isVisible){
                            binding.connectionStatusTv.text = getString(R.string.waiting_for_client_to_connect)
                            binding.connectionStatusTv.visibility = View.GONE
                        }
                    }

                    viewModel.udpClient(
                        it.getString(ThroughputUtils.ipAddress)!!,
                        it.getString(ThroughputUtils.portNumber)!!.toInt()
                    )
                } else if (it.getString(ThroughputUtils.throughPutType)?.equals(
                        ThroughputUtils.THROUGHPUT_TYPE_UDP_DOWNLOAD,
                        ignoreCase = true
                    ) == true
                ) {
                    //set the actionBar title
                    throughPutTestModeDownload = true
                    withContext(Dispatchers.Main) {
                        (activity as? WifiThroughputActivity)?.updateActionBarTitle(it.getString(ThroughputUtils.throughPutType).toString())
                        binding.connectionStatus.visibility = View.VISIBLE
                        if(binding.connectionStatus.isVisible){
                            binding.connectionStatusTv.text = getString(R.string.wifi_throughput_connecting_status)
                            //binding.connectionStatusTv.visibility = View.GONE
                        }
                    }

                    viewModel.udpServer(it.getString(ThroughputUtils.portNumber)!!.toInt())
                    //ThroughputUtils.receiUDP()
                } else if (it.getString(ThroughputUtils.throughPutType)?.equals(
                        ThroughputUtils.THROUGHPUT_TYPE_TLS_UPLOAD,
                        ignoreCase = true
                    ) == true
                ) {
                    //set the actionBar title
                    throughPutTestModeDownload = false
                    withContext(Dispatchers.Main) {
                        (activity as? WifiThroughputActivity)?.updateActionBarTitle(it.getString(ThroughputUtils.throughPutType).toString())
                        binding.connectionStatus.visibility = View.VISIBLE
                        if(binding.connectionStatus.isVisible){
                            binding.connectionStatusTv.text = getString(R.string.waiting_for_client_to_connect)
                        }
                    }

                    viewModel.startTLSServer(
                        it.getString(ThroughputUtils.portNumber)!!.toInt(),
                        requireContext(),
                        true
                    )
                } else if (it.getString(ThroughputUtils.throughPutType)?.equals(
                        ThroughputUtils.THROUGHPUT_TYPE_TLS_DOWNLOAD,
                        ignoreCase = true
                    ) == true
                ) {
                    //set the actionBar title
                    throughPutTestModeDownload = true
                    withContext(Dispatchers.Main) {
                        (activity as? WifiThroughputActivity)?.updateActionBarTitle(it.getString(ThroughputUtils.throughPutType).toString())
                        binding.connectionStatus.visibility = View.VISIBLE
                        if(binding.connectionStatus.isVisible){
                            binding.connectionStatusTv.text = getString(R.string.waiting_for_client_to_connect)
                        }
                    }

                    viewModel.startTLSServer(
                        it.getString(ThroughputUtils.portNumber)!!.toInt(),
                        requireContext(),
                        false
                    )
                } else {
                    /* Toast.makeText(requireContext(), "Working in progress", Toast.LENGTH_LONG)
                             .show()*/
                }
            }
        }
    }

    private fun updateThroughPutSpeed() {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                viewModel.updateSpeed()
                    .observe(viewLifecycleOwner) {
                        if (it.toString().equals("Infinity", ignoreCase = true) || it.toString()
                                .equals("Nan", ignoreCase = true)
                        )
                            return@observe


                        binding.wifiSpeedView.updateSpeed(
                            getWifiThroughPutProgress(it),
                            getSpeedAsString(it),
                            getUnitAsString(it),
                            getTestMode()
                        )
                    }
            }
        }
    }

    private fun getProgress(speed: Float):Int {
        //println("Speed : $speed")
        return ((speed / 20.0) * 100).toInt()

       // return (speed / 20.0).toInt()
    }

    private fun getWifiThroughPutProgress(speed: Float):Int {
        return ((speed / 100.0) * 100).toInt()
    }

    private fun getSpeedAsString(speed: Float): String {
       /* return if (speed >= 1000000) {
            (speed / 1000000.0).toString()
        } else {
            (speed / 100.0).toString()
        }*/
        //println("Speed getSpeedAsString : $speed")

        return String.format("%.2f", speed)
    }

    private fun getUnitAsString(speed: Float): String {
        /*return if (speed >= 1000000) {
            "Mbps"
        } else {
            "kbps"
        }*/
        return "Mbps"
    }

    private fun getTestMode(): SpeedView.Mode {
        if (throughPutTestModeDownload){
            return SpeedView.Mode.DOWNLOAD
        }
        return SpeedView.Mode.UPLOAD
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stop()
    }
}