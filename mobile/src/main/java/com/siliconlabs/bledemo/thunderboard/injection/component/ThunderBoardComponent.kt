package com.siliconlabs.bledemo.thunderboard.injection.component

import com.siliconlabs.bledemo.Application.SiliconLabsDemoApplication
import com.siliconlabs.bledemo.thunderboard.injection.module.ThunderBoardModule
import com.siliconlabs.bledemo.thunderboard.utils.PreferenceManager
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [ThunderBoardModule::class])
interface ThunderBoardComponent {
    fun providePreferenceManager(): PreferenceManager
    fun inject(o: SiliconLabsDemoApplication?)
}