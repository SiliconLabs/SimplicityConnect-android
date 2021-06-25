package com.siliconlabs.bledemo.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.siliconlabs.bledemo.R
import kotlin.math.max

class FlowLayout(context: Context, attrs: AttributeSet?) : ViewGroup(context, attrs) {
    private var mHorizontalSpacing = 0
    private var mVerticalSpacing = 0
    private val mPaint: Paint

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec) - paddingRight
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)

        val growHeight = widthMode != MeasureSpec.UNSPECIFIED

        var width = 0
        var height = paddingTop

        var currentWidth = paddingLeft
        var currentHeight = 0

        var breakLine = false
        var newLine = false
        var spacing = 0

        val count = childCount
        for (i in 0 until count) {
            val child = getChildAt(i)
            measureChild(child, widthMeasureSpec, heightMeasureSpec)

            val lp = child.layoutParams as LayoutParams
            spacing = mHorizontalSpacing
            if (lp.horizontalSpacing >= 0) {
                spacing = lp.horizontalSpacing
            }

            if (growHeight && (breakLine || currentWidth + child.measuredWidth > widthSize)) {
                height += currentHeight + mVerticalSpacing
                currentHeight = 0
                width = max(width, currentWidth - spacing)
                currentWidth = paddingLeft
                newLine = true
            } else {
                newLine = false
            }

            lp.x = currentWidth
            lp.y = height

            currentWidth += child.measuredWidth + spacing
            currentHeight = max(currentHeight, child.measuredHeight)

            breakLine = lp.breakLine
        }
        if (!newLine) {
            height += currentHeight
            width = max(width, currentWidth - spacing)
        }

        width += paddingRight
        height += paddingBottom

        setMeasuredDimension(View.resolveSize(width, widthMeasureSpec),
                View.resolveSize(height, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val count = childCount
        for (i in 0 until count) {
            val child = getChildAt(i)
            val lp = child.layoutParams as LayoutParams
            child.layout(lp.x, lp.y, lp.x + child.measuredWidth, lp.y + child.measuredHeight)
        }
    }

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        val more = super.drawChild(canvas, child, drawingTime)
        val lp = child.layoutParams as LayoutParams
        if (lp.horizontalSpacing > 0) {
            val x = child.right.toFloat()
            val y = child.top + child.height / 2.0f
            canvas.drawLine(x, y - 4.0f, x, y + 4.0f, mPaint)
            canvas.drawLine(x, y, x + lp.horizontalSpacing, y, mPaint)
            canvas.drawLine(x + lp.horizontalSpacing, y - 4.0f, x + lp.horizontalSpacing, y + 4.0f, mPaint)
        }
        if (lp.breakLine) {
            val x = child.right.toFloat()
            val y = child.top + child.height / 2.0f
            canvas.drawLine(x, y, x, y + 6.0f, mPaint)
            canvas.drawLine(x, y + 6.0f, x + 6.0f, y + 6.0f, mPaint)
        }
        return more
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams): Boolean {
        return p is LayoutParams
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams): LayoutParams {
        return LayoutParams(p.width, p.height)
    }

    class LayoutParams : ViewGroup.LayoutParams {
        var x = 0
        var y = 0

        var horizontalSpacing = 0
        var breakLine = false

        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.FlowLayout_LayoutParams)
            try {
                horizontalSpacing = a.getDimensionPixelSize(R.styleable.FlowLayout_LayoutParams_layout_horizontalSpacing, -1)
                breakLine = a.getBoolean(R.styleable.FlowLayout_LayoutParams_layout_breakLine, false)
            } finally {
                a.recycle()
            }
        }

        constructor(w: Int, h: Int) : super(w, h)
    }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.FlowLayout)

        try {
            mHorizontalSpacing = a.getDimensionPixelSize(R.styleable.FlowLayout_horizontalSpacing, 0)
            mVerticalSpacing = a.getDimensionPixelSize(R.styleable.FlowLayout_verticalSpacing, 0)
        } finally {
            a.recycle()
        }

        mPaint = Paint()
        mPaint.isAntiAlias = true
        mPaint.color = -0x10000
        mPaint.strokeWidth = 2.0f
    }
}