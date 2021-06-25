package com.siliconlabs.bledemo.browser

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import java.util.*

object MessageQueue {
    private val handler = Handler(Looper.getMainLooper())
    private val queue = LinkedList<Pair<String, Int>>()
    private var isTaskPlanned: Boolean = false
    private var context: Context? = null

    private const val DELAY: Long = 1000
    private const val DELAY_SHORT: Long = 500

    private val runnable = object : Runnable {
        override fun run() {
            synchronized(queue) {
                if (queue.size > 0) {
                    val message = queue.first.first
                    val length = queue.first.second
                    Toast.makeText(context, message, length).show()
                    queue.removeFirst()
                    handler.postDelayed(this, DELAY)
                } else {
                    isTaskPlanned = false
                }
            }
        }
    }

    fun init(context: Context) {
        this.context = context
    }

    fun add(message: String, toastLength: Int) {
        synchronized(queue) {
            queue.add(Pair(message, toastLength))
            if (!isTaskPlanned) {
                handler.postDelayed(runnable, DELAY_SHORT)
                isTaskPlanned = true
            }
        }
    }
}
