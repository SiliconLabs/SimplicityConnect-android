package com.siliconlabs.bledemo.home_screen.activities

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.siliconlabs.bledemo.R
import dagger.hilt.android.AndroidEntryPoint

/* Splash Screen API (as of API 31) has to much limitations for a custom splash screen currently
being used. */
@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            installSplashScreen().setOnExitAnimationListener {
                ObjectAnimator.ofFloat(it.view, View.ALPHA, 1f, 0f).apply {
                    duration = SYSTEM_SPLASH_SCREEN_FADE_OUT
                }.start()
            }
        } else setTheme(R.style.CustomSplashTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.efr_splash_screen)

        Handler(Looper.getMainLooper()).postDelayed({
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }.also {
                startActivity(it)
            }
            overridePendingTransition(R.anim.activity_fade_in, R.anim.activity_fade_out)
        }, SPLASH_SCREEN_TIME)
    }

    companion object {
        private const val SPLASH_SCREEN_TIME = 2000L
        private const val SYSTEM_SPLASH_SCREEN_FADE_OUT = 1000L
    }
}