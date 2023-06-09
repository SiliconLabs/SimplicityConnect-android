package com.siliconlabs.bledemo.common.other

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.siliconlabs.bledemo.R

class ImagesHorizontalDecoration : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildViewHolder(view).adapterPosition

        val horizontalSpacing = parent.context.resources.getDimensionPixelSize(
            R.dimen.rv_loaded_images_horizontal_margin)

        outRect.left = if (position != 0) horizontalSpacing else 0
    }
}