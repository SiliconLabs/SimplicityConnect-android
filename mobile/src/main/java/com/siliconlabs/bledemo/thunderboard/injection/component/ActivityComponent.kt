package com.siliconlabs.bledemo.thunderboard.injection.component

import com.siliconlabs.bledemo.blinky_thunderboard.activities.BlinkyThunderboardActivity
import com.siliconlabs.bledemo.environment.activities.EnvironmentActivity
import com.siliconlabs.bledemo.environment.activities.SettingsActivity
import com.siliconlabs.bledemo.thunderboard.injection.scope.ActivityScope
import dagger.Component

@ActivityScope
@Component(dependencies = [ThunderBoardComponent::class])
interface ActivityComponent {
    fun inject(activity: SettingsActivity?)
    fun inject(activity: BlinkyThunderboardActivity?)
    fun inject(activity: EnvironmentActivity?)
}