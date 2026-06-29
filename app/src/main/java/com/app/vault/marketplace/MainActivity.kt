package com.app.vault.marketplace

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.app.vault.marketplace.databinding.ActivityMainBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.messaging

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        NotificationHelper.createChannels(this)
        requestNotificationPermission()

        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java)); finish(); return
        } else {
            val session = SessionManager(this)
            if (session.getUid().isEmpty()) {
                FirebaseRepository().getUserProfile(currentUser.uid, { profile ->
                    session.saveProfile(profile.uid, profile.username)
                }, { })
            }
            updateFcmToken(currentUser.uid)
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

    private fun updateFcmToken(uid: String) {
        Firebase.messaging.token.addOnSuccessListener { token ->
            Log.d("VaultFCM", "Token Device: $token")
            Firebase.firestore.collection("users").document(uid)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d("VaultFCM", "FCM Token updated successfully")
                }
                .addOnFailureListener {
                    Firebase.firestore.collection("users").document(uid)
                        .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                }
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