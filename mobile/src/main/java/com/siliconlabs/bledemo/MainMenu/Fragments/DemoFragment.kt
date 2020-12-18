package com.siliconlabs.bledemo.MainMenu.Fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.siliconlabs.bledemo.MainMenu.Adapters.MenuAdapter
import com.siliconlabs.bledemo.MainMenu.MenuItems.ConnectedLighting
import com.siliconlabs.bledemo.MainMenu.MenuItems.HealthThermometer
import com.siliconlabs.bledemo.MainMenu.MenuItems.MainMenuItem
import com.siliconlabs.bledemo.MainMenu.MenuItems.RangeTest
import com.siliconlabs.bledemo.R
import kotlinx.android.synthetic.main.fragment_demo.*

class DemoFragment : Fragment(), MenuAdapter.OnMenuItemClickListener {
    private val list: ArrayList<MainMenuItem> = ArrayList()
    private var menuItemClickListener: MenuAdapter.OnMenuItemClickListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        list.add(HealthThermometer(R.drawable.ic_temp, getString(R.string.title_Health_Thermometer), getString(R.string.main_menu_description_thermometer)))
        list.add(ConnectedLighting(R.drawable.ic_connected_lighting, getString(R.string.title_Connected_Lighting), getString(R.string.main_menu_description_connected_lighting)))
        list.add(RangeTest(R.drawable.ic_range_test, getString(R.string.title_Range_Test), getString(R.string.main_menu_description_range_test)))
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        menuItemClickListener = context as MenuAdapter.OnMenuItemClickListener?
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_demo, container, false)
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