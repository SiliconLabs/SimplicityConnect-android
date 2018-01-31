package com.siliconlabs.bledemo.utils;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import timber.log.Timber;

/**
 * This class can be the base-class of any service that runs locally, i.e. runs in the
 * same process as the code that uses the service.
*/
public class LocalService<S extends LocalService> extends Service {

    /**
     * Implement this class to properly define the implementing class of a LocalService
     * and to receive an instance of the LocalService when it is created and connected/bound.
     * @param <S>
     */
    public static abstract class Binding<S extends LocalService> {
        final Context context;
        volatile boolean boundOK;

        final ServiceConnection connection = new ServiceConnection() {
            @Override
            public final void onServiceConnected(ComponentName className, IBinder serviceBinder) {
                final LocalService<S>.LocalBinder binder = (LocalService<S>.LocalBinder)serviceBinder;
                onBound(binder.getService());
            }

            @Override
            public final void onServiceDisconnected(ComponentName className) {
                onBound(null);
            }

            @Override
            public String toString() {
                return Binding.this.toString();
            }
        };

        protected Binding(Context context) {
            this.context = context;
        }

        public void unbind() {
            if (boundOK) {
                boundOK = false;
                try {
                    context.unbindService(connection);
                    Timber.d("LocalService unbound from %s", context.toString());
                } catch (Exception e) {
                    Timber.e(e, "LocalService problem unbinding service.");
                    Log.e("unbind", "Problem unbinding service: " + e);
                }
            }
        }

        boolean bind() {
            Intent serviceIntent = new Intent(context, getServiceClass());
            boundOK = context.bindService(serviceIntent, connection, Service.BIND_AUTO_CREATE);
            if (boundOK) {
                Timber.d("LocalService bound to %s", context.toString());
            } else {
                Timber.d("LocalService could not bind to %s", context.toString());
            }
            return boundOK;
        }

        /**
         * Returns the specific sub-class of the LocalService that needs to be created.
         * @return The LocalService's sub-class.
         */
        protected abstract Class<S> getServiceClass();

        /**
         * Is called when the LocalService is bound to the caller after a call to {@link LocalService#bind(LocalService.Binding)}.
         * To unbind, call {@link LocalService.Binding#unbind()}.
         * @param service The service that is now bound. If it is null, the service could not be bound.
         * @return True if binding was successful.
         */
        protected abstract void onBound(S service);

        @Override
        public String toString() {
            return "Binding["+getServiceClass().toString()+"]";
        }
    }

    /**
     * Binds a service to the given binding (and its context).
     * @param binding
     * @param <T>
     */
    public static <T extends LocalService<T>> boolean bind(Binding<T> binding) {
        return binding.bind();
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public final IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public class LocalBinder extends Binder {
        final S getService() {
            return (S)LocalService.this;
        }
    }
}
