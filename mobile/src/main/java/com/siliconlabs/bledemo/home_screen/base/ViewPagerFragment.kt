package com.siliconlabs.bledemo.home_screen.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.siliconlabs.bledemo.bluetooth.services.BluetoothService
import com.siliconlabs.bledemo.features.scan.browser.fragments.BrowserFragment
import com.siliconlabs.bledemo.features.scan.active_connections.fragments.ConnectionsFragment
import com.siliconlabs.bledemo.home_screen.viewmodels.ScanFragmentViewModel
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.home_screen.fragments.ScanFragment
import com.siliconlabs.bledemo.features.scan.rssi_graph.fragments.RssiGraphFragment
import kotlinx.android.synthetic.main.fragment_view_pager.*

class ViewPagerFragment : Fragment() {

    private lateinit var viewModel: ScanFragmentViewModel
    private var bluetoothService: BluetoothService? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = getScanFragment().viewModel
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_view_pager, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initScanPager()
        setupDataObservers()
        viewModel.updateActiveConnections(bluetoothService?.getActiveConnections())
    }

    fun getScanFragment() = parentFragment as ScanFragment

    fun setBluetoothService(service: BluetoothService?) {
        bluetoothService = service
    }

    private fun initScanPager() {
        scan_view_pager2.apply {
            adapter = ScanPagerAdapter()
        }

        TabLayoutMediator(scan_tab_layout, scan_view_pager2) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_scanner_label)
                1 -> getString(R.string.tab_rssi_graph_label)
                2 -> getString(R.string.tab_connections_label)
                else -> null
            }
        }.attach()
    }

    private fun setupDataObservers() {
        viewModel.numberOfConnectedDevices.observe(viewLifecycleOwner) {
            scan_tab_layout.getTabAt(2)?.text = getString(R.string.tab_connections_label, it)
        }
    }

    private inner class ScanPagerAdapter : FragmentStateAdapter(this) {
        override fun getItemCount() = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> BrowserFragment()
                1 -> RssiGraphFragment()
                2 -> ConnectionsFragment()
                else -> BrowserFragment()
            }
        }
    }

}