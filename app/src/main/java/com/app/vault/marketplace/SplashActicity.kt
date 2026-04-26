package com.app.vault.marketplace

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_splash)
        Handler(Looper.getMainLooper()).postDelayed({
            val dest = if (SessionManager(this).isLoggedIn()) MainActivity::class.java else LoginActivity::class.java
            startActivity(Intent(this, dest))
            finish()
        }, 2000)
    }
}