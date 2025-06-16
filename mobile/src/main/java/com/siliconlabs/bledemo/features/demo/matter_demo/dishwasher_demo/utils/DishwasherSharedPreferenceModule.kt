package com.siliconlabs.bledemo.features.demo.matter_demo.dishwasher_demo.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.siliconlabs.bledemo.features.demo.matter_demo.dishwasher_demo.view.MatterDishwasherFragment.Companion.DISHWASHER_PREF
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SharedPreferencesModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(DISHWASHER_PREF, AppCompatActivity.MODE_PRIVATE)
    }
}