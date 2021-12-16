package com.siliconlabs.bledemo.thunderboard.injection.module

import android.content.Context
import com.siliconlabs.bledemo.thunderboard.injection.scope.ForApplication
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module
class ThunderBoardModule {
    private var context: Context? = null
    fun setContext(context: Context?) {
        this.context = context
    }

    @Provides
    @Singleton
    @ForApplication
    fun provideContext(): Context {
        return context!!
    }
}