package com.siliconlabs.bledemo.common.other

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.R

class CardViewListDecoration : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildViewHolder(view).adapterPosition

        val verticalSpacing = parent.context.resources.getDimensionPixelSize(
                R.dimen.recycler_view_card_view_vertical_separation)
        val horizontalMargin = parent.context.resources.getDimensionPixelSize(
                R.dimen.recycler_view_card_view_horizontal_margin)

        outRect.left = horizontalMargin
        outRect.right = horizontalMargin
        outRect.top = if (position == 0) verticalSpacing else 0
        outRect.bottom = verticalSpacing
    }
}