package com.siliconlabs.bledemo.MainMenu.Fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.siliconlabs.bledemo.MainMenu.Adapters.MenuAdapter
import com.siliconlabs.bledemo.MainMenu.MenuItems.Demo.*
import com.siliconlabs.bledemo.MainMenu.MenuItems.MainMenuItem
import com.siliconlabs.bledemo.R
import kotlinx.android.synthetic.main.fragment_demo.*

class DemoFragment : Fragment(R.layout.fragment_demo), MenuAdapter.OnMenuItemClickListener {
    private val list: ArrayList<MainMenuItem> = ArrayList()
    private var menuItemClickListener: MenuAdapter.OnMenuItemClickListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        list.apply {
            add(HealthThermometer(R.drawable.ic_thermometer, getString(R.string.title_Health_Thermometer), getString(R.string.main_menu_description_thermometer)))
            add(ConnectedLighting(R.drawable.ic_connected_lighting, getString(R.string.title_Connected_Lighting), getString(R.string.main_menu_description_connected_lighting)))
            add(RangeTest(R.drawable.ic_range_test, getString(R.string.title_Range_Test), getString(R.string.main_menu_description_range_test)))
            add(Blinky(R.drawable.ic_blinky, getString(R.string.title_Blinky), getString(R.string.main_menu_description_blinky)))
            add(Throughput(R.drawable.ic_throughput, getString(R.string.title_Throughput), getString(R.string.main_menu_description_throughput)))
            add(WifiCommissioning(R.drawable.ic_wifi_commissioning, getString(R.string.wifi_commissioning_label), getString(R.string.wifi_commissioning_description)))
            add(Motion(R.drawable.icon_motion, getString(R.string.motion_demo_title), getString(R.string.motion_demo_description)))
            add(Environment(R.drawable.icon_environment, getString(R.string.environment_demo_title), getString(R.string.environment_demo_description)))
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        menuItemClickListener = context as MenuAdapter.OnMenuItemClickListener?
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = MenuAdapter(list, this, activity as Context)
        rv_demo_menu.layoutManager = GridLayoutManager(activity, 2)
        rv_demo_menu.adapter = adapter
    }

    override fun onMenuItemClick(menuItem: MainMenuItem) {
        menuItemClickListener?.onMenuItemClick(menuItem)
    }

    override fun onDetach() {
        super.onDetach()
        menuItemClickListener = null
    }
}