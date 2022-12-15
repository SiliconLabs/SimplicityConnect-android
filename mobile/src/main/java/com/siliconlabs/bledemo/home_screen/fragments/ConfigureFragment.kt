package com.siliconlabs.bledemo.home_screen.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.siliconlabs.bledemo.features.configure.advertiser.fragments.AdvertiserFragment
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.features.configure.gatt_configurator.fragments.GattConfiguratorFragment
import kotlinx.android.synthetic.main.fragment_configure.*

class ConfigureFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_configure, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = getString(R.string.main_navigation_configure_title)

        configure_view_pager2.adapter = ConfigurePagerAdapter()
        TabLayoutMediator(configure_tab_layout, configure_view_pager2) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_advertiser_label)
                1 -> getString(R.string.tab_gatt_configurator_label)
                else -> null
            }
        }.attach()
    }

    private inner class ConfigurePagerAdapter : FragmentStateAdapter(this) {
        override fun getItemCount() = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> AdvertiserFragment()
                1 -> GattConfiguratorFragment()
                else -> AdvertiserFragment()
            }
        }
    }
}