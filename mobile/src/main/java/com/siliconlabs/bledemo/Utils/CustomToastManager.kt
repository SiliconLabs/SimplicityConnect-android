package com.siliconlabs.bledemo.utils



import android.annotation.SuppressLint
import android.app.Activity
import android.app.Service
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.TextView
import android.widget.Toast
import com.siliconlabs.bledemo.R
import timber.log.Timber
import java.util.logging.Logger

@SuppressLint("StaticFieldLeak")
object CustomToastManager {
    private var toastView: View? = null
    private var windowManager: WindowManager? = null

    fun show(context: Context, message: String, duration: Long = 5000) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            if (context !is Activity || context.isFinishing || context.isDestroyed) {
                return@post  // Prevent adding toast if activity is not valid
            }

            val inflater = LayoutInflater.from(context)
            val customToastView = inflater.inflate(R.layout.custom_toast, null)
            val textView: TextView = customToastView.findViewById(R.id.custom_toast_text)
            textView.text = message

            dismiss()  // Remove any existing toast

            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            )

            params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            params.y = 200

            if (context.window != null) {
                params.token = context.window.decorView.windowToken  // Set token from valid Activity window
            }

            if (!context.isFinishing && !context.isDestroyed) {
                windowManager?.addView(customToastView, params)
                toastView = customToastView
            }



            handler.postDelayed({ dismiss() }, duration)
        }
    }

    fun dismiss() {
        toastView?.let {
            try {
                if(it.windowToken != null){
                    windowManager?.removeView(it)
                }
            }catch (e:IllegalArgumentException){
                Timber.e(e.message)
            } finally {
                toastView = null
            }


        }
    }
}
