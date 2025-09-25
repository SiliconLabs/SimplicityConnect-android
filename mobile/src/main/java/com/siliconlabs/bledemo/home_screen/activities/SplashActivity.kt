package com.siliconlabs.bledemo.home_screen.activities

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.features.demo.matter_demo.dishwasher_demo.view.MatterDishwasherFragment.Companion.DISHWASHER_PREF
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.SharedPrefsUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* Splash Screen API (as of API 31) has to much limitations for a custom splash screen currently
being used. */
@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    private lateinit var dishwasherPref: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            installSplashScreen().setOnExitAnimationListener {
                ObjectAnimator.ofFloat(it.view, View.ALPHA, 1f, 0f).apply {
                    duration = SYSTEM_SPLASH_SCREEN_FADE_OUT
                }.start()
            }
        } else setTheme(R.style.CustomSplashTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.si_connect_splash_screen)
        CoroutineScope(Dispatchers.IO).launch {
            dishwasherPref = getSharedPreferences(
                DISHWASHER_PREF,
                AppCompatActivity.MODE_PRIVATE
            )
            SharedPrefsUtils.clearDishwasherSharedPreferences(dishwasherPref)
            launch(Dispatchers.Main) {
                // Add a delay before starting the next activity, typically to show splash screen time
                delay(SPLASH_SCREEN_TIME)

                // Start MainActivity and clear task stack
                Intent(this@SplashActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }.also {
                    startActivity(it)
                    // Apply fade transition
                    overridePendingTransition(R.anim.activity_fade_in, R.anim.activity_fade_out)
                }
            }
        }


    }

    companion object {
        private const val SPLASH_SCREEN_TIME = 2000L
        private const val SYSTEM_SPLASH_SCREEN_FADE_OUT = 1000L
    }
}