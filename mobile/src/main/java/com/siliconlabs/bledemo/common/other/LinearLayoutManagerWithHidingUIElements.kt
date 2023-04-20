package com.siliconlabs.bledemo.common.other

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.common.views.MainActionButton
import com.siliconlabs.bledemo.home_screen.views.HidableBottomNavigationView

class LinearLayoutManagerWithHidingUIElements(private val actionButton: MainActionButton?, private val hidableNav: HidableBottomNavigationView?, context: Context?) : LinearLayoutManager(context) {

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        actionButton?.let {
            if (dy > 0) {
                it.hide()
            } else if (dy < 0) {
                it.show()
            }
        }
        hidableNav?.let {
            if (dy > 0) {
                it.hide()
            } else if (dy < 0) {
                it.show()
            }
        }
        return super.scrollVerticallyBy(dy, recycler, state)
    }
}

