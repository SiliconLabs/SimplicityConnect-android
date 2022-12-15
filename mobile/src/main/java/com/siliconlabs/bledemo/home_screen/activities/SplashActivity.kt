package com.siliconlabs.bledemo.home_screen.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.siliconlabs.bledemo.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
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
        private const val SPLASH_SCREEN_TIME = 1000L
    }
}