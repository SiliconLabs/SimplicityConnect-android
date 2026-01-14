package com.siliconlabs.bledemo.features.demo.channel_sounding.views

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.siliconlabs.bledemo.R
import timber.log.Timber

class DynamicRippleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var ripplePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.silabs_blue)
    }

    private val ripples = mutableListOf<Ripple>()
    private var animatorSet: AnimatorSet? = null
    private var isAnimating = false

    // Configuration
    private var rippleDuration = 3000L
    private var rippleAmount = 6
    private var rippleRadius = 50f
    private var rippleScale = 6f
    private var rippleColor: Int = ContextCompat.getColor(context, R.color.silabs_blue)

    data class Ripple(
        var radius: Float = 0f,
        var alpha: Int = 255,
        var progress: Float = 0f
    )

    init {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.DynamicRippleView)
            rippleColor = typedArray.getColor(
                R.styleable.DynamicRippleView_rippleColor,
                ContextCompat.getColor(context, R.color.silabs_blue)
            )
            rippleDuration = typedArray.getInt(
                R.styleable.DynamicRippleView_rippleDuration,
                3000
            ).toLong()
            rippleAmount = typedArray.getInt(
                R.styleable.DynamicRippleView_rippleAmount,
                6
            )
            rippleRadius = typedArray.getDimension(
                R.styleable.DynamicRippleView_rippleRadius,
                50f
            )
            rippleScale = typedArray.getFloat(
                R.styleable.DynamicRippleView_rippleScale,
                6f
            )
            typedArray.recycle()
        }
        ripplePaint.color = rippleColor
        setWillNotDraw(false) // Enable drawing for ripple animation
        setBackgroundColor(android.graphics.Color.TRANSPARENT) // Make background transparent
    }

    fun setRippleColor(color: Int) {
        rippleColor = color
        ripplePaint.color = color
        invalidate()
    }

    fun startRippleAnimation() {
        if (isAnimating) {
            stopRippleAnimation()
        }

        // Validate rippleAmount to prevent division by zero
        if (rippleAmount <= 0) {
            Timber.tag(TAG).e("Invalid rippleAmount: $rippleAmount. Must be greater than 0.")
            return
        }

        // Validate rippleDuration to ensure positive value
        if (rippleDuration <= 0) {
            Timber.tag(TAG).e(
                "Invalid rippleDuration: $rippleDuration. Must be greater than 0."
            )
            return
        }

        isAnimating = true
        ripples.clear()

        val animators = mutableListOf<Animator>()
        val delayBetweenRipples = rippleDuration / rippleAmount

        for (i in 0 until rippleAmount) {
            val ripple = Ripple(rippleRadius, 255, 0f)
            ripples.add(ripple)

            val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = rippleDuration
                startDelay = i * delayBetweenRipples
                interpolator = LinearInterpolator()
                repeatCount = ValueAnimator.INFINITE

                addUpdateListener { animation ->
                    val progress = animation.animatedValue as Float
                    ripple.progress = progress

                    // Calculate radius based on progress
                    ripple.radius =
                        rippleRadius + (rippleRadius * rippleScale - rippleRadius) * progress

                    // Calculate alpha based on progress (fade out as it expands)
                    ripple.alpha = (255 * (1 - progress)).toInt()

                    // Invalidate to redraw on every frame
                    postInvalidate()
                }
            }

            animators.add(animator)
        }

        animatorSet = AnimatorSet().apply {
            playTogether(animators)
            start()
        }
    }

    fun stopRippleAnimation() {
        isAnimating = false
        animatorSet?.cancel()
        animatorSet?.removeAllListeners()
        animatorSet = null
        ripples.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        // Draw ripples first (behind children)
        if (width > 0 && height > 0) {
            val centerX = width / 2f
            val centerY = height / 2f

            for (ripple in ripples) {
                if (ripple.alpha > 0 && ripple.radius > 0) {
                    ripplePaint.alpha = ripple.alpha
                    canvas.drawCircle(centerX, centerY, ripple.radius, ripplePaint)
                }
            }
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        // Draw children on top of ripples
        super.dispatchDraw(canvas)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopRippleAnimation()
    }

    private val TAG = DynamicRippleView::class.java.toString()
}

