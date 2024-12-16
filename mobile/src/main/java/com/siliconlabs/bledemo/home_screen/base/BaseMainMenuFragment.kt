package com.siliconlabs.bledemo.home_screen.base

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.siliconlabs.bledemo.common.other.LinearLayoutManagerWithHidingUIElements
import com.siliconlabs.bledemo.common.views.MainActionButton
import com.siliconlabs.bledemo.common.other.WithHidableUIElements
import com.siliconlabs.bledemo.home_screen.activities.MainActivity
import com.siliconlabs.bledemo.home_screen.views.HidableBottomNavigationView

abstract class BaseMainMenuFragment : Fragment(), WithHidableUIElements {

    protected lateinit var bottomNav: HidableBottomNavigationView
    protected var hidableActionButton: MainActionButton? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).let {
            bottomNav = it.getMainNavigation()!!
        }
    }

    fun restoreHiddenUI() {
        hidableActionButton?.show()
        bottomNav.show()
    }

    override fun onResume() {
        super.onResume()
        hidableActionButton?.show()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if(!hidden) {
            hidableActionButton?.show()
        }
    }

    override fun onPause() {
        restoreHiddenUI()
        super.onPause()
    }

    override fun onStop() {
        restoreHiddenUI()
        super.onStop()
    }

    override fun getLayoutManagerWithHidingUIElements(context: Context?): LinearLayoutManagerWithHidingUIElements {
        return LinearLayoutManagerWithHidingUIElements(hidableActionButton, bottomNav, context)
    }

}