package com.siliconlabs.bledemo.application;

import android.app.Application;

import com.siliconlabs.bledemo.BuildConfig;

import timber.log.Timber;

public class SiliconLabsDemoApplication extends Application {
    public static SiliconLabsDemoApplication APP;

    public SiliconLabsDemoApplication() {
        APP = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }
}
