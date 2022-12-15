package com.siliconlabs.bledemo.base.dialogs

import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import com.siliconlabs.bledemo.R
import kotlinx.android.synthetic.main.dialog_with_progress_spinner.*

class ProgressDialogWithSpinner(
        context: Context,
        private val caption: String,
        private val animate: Boolean,
        private val imageViewResourceId: Int,
        private val cancelable: Boolean = true
) : ProgressDialog(context) {

    private lateinit var handler: Handler
    private var rotateIconAnimation: Animation? = null

    private val autoDismiss = Runnable {
        if (isShowing) {
            dismiss()
        }
    }

    init {
        init()
    }

    private fun init() {
        handler = Handler(Looper.getMainLooper())
        setCancelable(cancelable)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_with_progress_spinner)

        dialog_text.text = caption
        if (animate) {
            rotateIconAnimation = AnimationUtils.loadAnimation(context, R.anim.rotate_progress_dialog_spinner)
        } else {
            // if not using spinner to animate, set the image resource for the imageview
            progress_spinner.setImageResource(imageViewResourceId)
            progress_spinner.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
    }

    override fun show() {
        super.show()
        if (animate) {
            // turn off hardware acceleration, prevents graphic/animation from getting broken
            progress_spinner.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            progress_spinner.startAnimation(rotateIconAnimation)
        }
    }

    fun show(timer: Long) {
        show()
        handler.postDelayed(autoDismiss, timer)
    }

    fun setCaption(caption: String) {
        dialog_text.text = caption
    }

    override fun dismiss() {
        super.dismiss()
        handler.removeCallbacks(autoDismiss)
        clearAnimation()
    }

    fun clearAnimation() {
        progress_spinner.clearAnimation()
    }

}