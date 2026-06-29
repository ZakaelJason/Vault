package com.app.vault.marketplace

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.app.vault.marketplace.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        NotificationHelper.createChannels(this)
        requestNotificationPermission()

        if (!FirebaseRepository().isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java)); finish(); return
        }

        if (savedInstanceState == null) {
            loadFragment(MarketFragment())
        }

        b.bottomNav.setOnItemSelectedListener { item ->
            val f: Fragment = when (item.itemId) {
                R.id.nav_market -> MarketFragment()
                R.id.nav_store -> MyStoreFragment()
                R.id.nav_orders -> OrdersFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> MarketFragment()
            }
            loadFragment(f)
            true
        }

        if (intent.getBooleanExtra("open_orders", false)) {
            navigateToOrders()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }

    fun navigateToOrders() {
        b.bottomNav.selectedItemId = R.id.nav_orders
    }

    private fun loadFragment(f: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, f)
            .commit()
    }
}