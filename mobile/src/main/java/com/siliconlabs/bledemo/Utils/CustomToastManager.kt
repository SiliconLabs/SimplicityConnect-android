package com.siliconlabs.bledemo.utils



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

object CustomToastManager {
    private var toastView: View? = null
    private var windowManager: WindowManager? = null

    fun show(context: Context, message: String, duration: Long = 5000,showStandardToast: Boolean = true) {
        val handler = Handler(Looper.getMainLooper())

        handler.post {
            // Use standard Toast for non-activity contexts
            //Toast.makeText(context, message, Toast.LENGTH_LONG).show()

            // Inflate custom layout
            val inflater = LayoutInflater.from(context)
            val customToastView = inflater.inflate(R.layout.custom_toast, null)
            val textView: TextView = customToastView.findViewById(R.id.custom_toast_text)
            textView.text = message



            // Remove any existing toast
            dismiss()

            // Display custom toast for longer duration (only for Activity-based context)
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,  // No special permission needed
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            params.y = 200  // Adjust position from bottom

            windowManager?.addView(customToastView, params)
            toastView = customToastView

            // Auto-dismiss after duration
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
