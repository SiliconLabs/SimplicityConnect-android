package com.siliconlabs.bledemo.MainMenu.Fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.siliconlabs.bledemo.MainMenu.Adapters.MenuAdapter
import com.siliconlabs.bledemo.MainMenu.MenuItems.Develop.*
import com.siliconlabs.bledemo.MainMenu.MenuItems.MainMenuItem
import com.siliconlabs.bledemo.R
import kotlinx.android.synthetic.main.fragment_develop.*

class DevelopFragment : Fragment(R.layout.fragment_develop), MenuAdapter.OnMenuItemClickListener {
    private val list: ArrayList<MainMenuItem> = ArrayList()
    private var menuItemClickListener: MenuAdapter.OnMenuItemClickListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        list.apply {
            add(Browser(R.drawable.ic_browser, getString(R.string.title_Browser), getString(R.string.main_menu_description_browser)))
            add(Advertiser(R.drawable.ic_advertiser, getString(R.string.title_Advertiser), getString(R.string.main_menu_description_advertiser)))
            add(GattConfigurator(R.drawable.ic_gatt_configurator, getString(R.string.title_GATT_Configurator), getString(R.string.main_menu_description_gatt_configurator)))
            add(IOPTest(R.drawable.ic_iop_tester, getString(R.string.title_Interoperability_Test), getString(R.string.main_menu_description_iop_test)))
            add(RssiGraph(R.drawable.ic_range_test, getString(R.string.rssi_graph_label), getString(R.string.rssi_graph_demo_description)))
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        menuItemClickListener = context as MenuAdapter.OnMenuItemClickListener?
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = MenuAdapter(list, this, activity as Context)
        rv_develop_menu.layoutManager = GridLayoutManager(activity, 2)
        rv_develop_menu.adapter = adapter
    }

    override fun onMenuItemClick(menuItem: MainMenuItem) {
        menuItemClickListener?.onMenuItemClick(menuItem)
    }

    override fun onDetach() {
        super.onDetach()
        menuItemClickListener = null
    }
}