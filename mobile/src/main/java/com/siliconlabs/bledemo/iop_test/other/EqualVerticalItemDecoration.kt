package com.siliconlabs.bledemo.iop_test.other

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class EqualVerticalItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildViewHolder(view).adapterPosition
        val itemCount = state.itemCount

        outRect.apply {
            left = spacing
            right = spacing
            top = if (position == 0) spacing else spacing / 2
            bottom = if(position == itemCount - 1) spacing else spacing / 2
        }
    }
}