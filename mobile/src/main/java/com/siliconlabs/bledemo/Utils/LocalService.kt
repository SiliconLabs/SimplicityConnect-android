package com.siliconlabs.bledemo.utils

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import android.util.Log

/**
 * This class can be the base-class of any service that runs locally, i.e. runs in the
 * same process as the code that uses the service.
 */
open class LocalService<S : LocalService<S>> : Service() {
    /**
     * Implement this class to properly define the implementing class of a LocalService
     * and to receive an instance of the LocalService when it is created and connected/bound.
     *
     * @param <S>
    </S> */
    abstract class Binding<S : LocalService<S>> protected constructor(val context: Context) {

        @Volatile
        var boundOK = false

        val connection: ServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, serviceBinder: IBinder) {
                val binder = serviceBinder as LocalService<S>.LocalBinder
                onBound(binder.getService())
            }

            override fun onServiceDisconnected(className: ComponentName) {
                onBound(null)
            }

            override fun toString(): String {
                return this@Binding.toString()
            }
        }

        fun unbind() {
            if (boundOK) {
                boundOK = false
                try {
                    context.unbindService(connection)
                    Log.d("unbind", "LocalService unbound from $context")
                } catch (e: Exception) {
                    Log.e("unbind", "Problem unbinding service: $e")
                }
            }
        }

        fun bind(): Boolean {
            val serviceIntent = Intent(context, getServiceClass())
            boundOK = context.bindService(serviceIntent, connection, BIND_AUTO_CREATE)
            if (boundOK) {
                Log.d("bind", "LocalService bound to $context")
            } else {
                Log.d("bind", "LocalService could not bind to $context")
            }
            return boundOK
        }

        /**
         * Returns the specific sub-class of the LocalService that needs to be created.
         *
         * @return The LocalService's sub-class.
         */
        protected abstract fun getServiceClass(): Class<S>

        /**
         * Is called when the LocalService is bound to the caller after a call to [LocalService.bind].
         * To unbind, call [LocalService.Binding.unbind].
         *
         * @param service The service that is now bound. If it is null, the service could not be bound.
         * @return True if binding was successful.
         */
        protected abstract fun onBound(service: S?)

        override fun toString(): String {
            return "Binding[${getServiceClass()}]"
        }

    }

    override fun onBind(intent: Intent): IBinder? {
        return LocalBinder()
    }


    inner class LocalBinder : Binder() {
        fun getService(): S {
            return this@LocalService as S
        }
    }

    companion object {
        /**
         * Binds a service to the given binding (and its context).
         *
         * @param binding
         * @param <T>
        </T> */
        fun <T : LocalService<T>> bind(binding: Binding<T>): Boolean {
            return binding.bind()
        }
    }
}
