package com.siliconlabs.bledemo.features.configure.gatt_configurator.di

import android.content.Context
import com.siliconlabs.bledemo.features.configure.gatt_configurator.utils.GattConfiguratorStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent


@InstallIn(SingletonComponent::class)
@Module
class GattConfiguratorModule {

    @Provides
    fun provideGattConfiguratorStorage(@ApplicationContext context: Context): GattConfiguratorStorage {
        return GattConfiguratorStorage(context)
    }

}