package com.siliconlabs.bledemo.features.demo.matter_demo.evse.di

import com.siliconlabs.bledemo.features.demo.matter_demo.evse.data.EVRepositoryImpl
import com.siliconlabs.bledemo.features.demo.matter_demo.evse.domain.repo.EVRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EVModule {

    @Provides
    @Singleton
    fun provideEVRepository(): EVRepository = EVRepositoryImpl()
}
