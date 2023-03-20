package com.siliconlabs.bledemo.features.scan.browser.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService
import com.siliconlabs.bledemo.features.scan.browser.activities.DeviceServicesActivity
import com.siliconlabs.bledemo.features.scan.browser.adapters.LogAdapter
import com.siliconlabs.bledemo.features.scan.browser.models.logs.Log
import com.siliconlabs.bledemo.features.scan.browser.services.ShareLogServices
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.FragmentLogBinding
import com.siliconlabs.bledemo.utils.Constants

class LogFragment : Fragment() {

    private lateinit var _binding: FragmentLogBinding
    var logAdapter: LogAdapter? = null
    private var handler: Handler? = null
    private var allowRefreshScrollBottom = true

    private var service: BluetoothService? = null


    private val logUpdater: Runnable = object : Runnable {
        override fun run() {
            logAdapter?.let {
                it.setLogList(getLogs())
                it.notifyDataSetChanged()
                if (allowRefreshScrollBottom) {
                    it.itemCount.let { position ->
                        _binding.rvLog.scrollToPosition(position - 1)
                    }
                }
                handler?.postDelayed(this, LOG_UPDATE_PERIOD)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setHasOptionsMenu(true)
        _binding = FragmentLogBinding.inflate(inflater)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as DeviceServicesActivity).let {
            service = it.bluetoothService
            it.supportActionBar?.title = getString(R.string.activity_log)
        }
        setupRecyclerView()
        setupUiListeners()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_logs, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        with(menu) {
            findItem(R.id.show_logs)?.isVisible = false
            findItem(R.id.request_priority)?.isVisible = false
            findItem(R.id.request_mtu)?.isVisible = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_icon_clear_logs -> {
                clearLogs()
                logAdapter?.let {
                    it.setLogList(getLogs())
                    it.notifyDataSetChanged()
                }
                true
            }
            android.R.id.home -> {
                requireActivity().onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        logAdapter = LogAdapter(getLogs())
        _binding.rvLog.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false).apply {
                stackFromEnd = true
            }
            adapter = logAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    allowRefreshScrollBottom = !recyclerView.canScrollVertically(1)
                }
            })
        }

        logAdapter?.notifyDataSetChanged()
    }

    private fun setupUiListeners() {
        _binding.btnShare.setOnClickListener {
            Constants.LOGS = getLogs()
            activity?.startService(Intent(context, ShareLogServices::class.java))
        }
    }

    private fun getLogs() : List<Log> {
        val gatt = (activity as DeviceServicesActivity).bluetoothGatt
        return gatt?.let {
            service?.getLogsForDevice(it.device.address)
        } ?: emptyList()
    }

    private fun clearLogs() {
        val gatt = (activity as DeviceServicesActivity).bluetoothGatt
        gatt?.let {
            service?.clearLogsForDevice(it.device.address)
        }
    }

    override fun onResume() {
        super.onResume()
        runLogUpdater()
    }

    override fun onPause() {
        super.onPause()
        stopLogUpdater()
    }

    private fun runLogUpdater() {
        handler = (handler ?: Handler(Looper.getMainLooper())).also {
            it.removeCallbacks(logUpdater)
            it.postDelayed(logUpdater, LOG_UPDATE_PERIOD)
        }
    }

    private fun stopLogUpdater() {
        handler?.removeCallbacks(logUpdater)
    }

    companion object {
        private const val LOG_UPDATE_PERIOD = 2000L
    }
}
