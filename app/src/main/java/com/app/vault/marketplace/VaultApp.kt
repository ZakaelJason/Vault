package com.app.vault.marketplace

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.app.ActivityCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class VaultApp : Application() {

    private lateinit var connectivityManager: ConnectivityManager

    private val lastKnownStatus = mutableMapOf<String, String>()

    // null = belum pernah dicek sama sekali (state awal sebelum app tahu apa-apa)
    private var isCurrentlyOnline: Boolean? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        registerNetworkCallback()
        listenTransactionStatusChanges()

        // Pasang ulang listener setiap kali user login/logout
        Firebase.auth.addAuthStateListener {
            lastKnownStatus.clear()
            listenTransactionStatusChanges()
        }
    }

    private fun listenTransactionStatusChanges() {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val db = Firebase.firestore

        // Sebagai BUYER: pantau status pembayaran sendiri
        db.collection("transactions")
            .whereEqualTo("buyerUid", uid)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documentChanges?.forEach { change ->
                    val doc = change.document
                    val status = doc.getString("status") ?: return@forEach
                    val itemName = doc.getString("itemName") ?: "Produk"
                    val key = doc.id

                    val previous = lastKnownStatus[key]
                    if (previous != null && previous != status) {
                        if (ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return@forEach
                        }
                        when (status) {
                            "Completed" -> NotificationHelper.show(
                                this, NotificationHelper.CHANNEL_ORDERS, doc.id.hashCode(),
                                "Pembayaran berhasil",
                                "Transaksi untuk \"$itemName\" sudah selesai."
                            )
                            "Cancelled" -> NotificationHelper.show(
                                this, NotificationHelper.CHANNEL_ORDERS, doc.id.hashCode(),
                                "Pembayaran gagal",
                                "Pesanan \"$itemName\" dibatalkan."
                            )
                        }
                    }
                    lastKnownStatus[key] = status
                }
            }
    }

    private fun registerNetworkCallback() {
        connectivityManager = getSystemService(ConnectivityManager::class.java)

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (isCurrentlyOnline != true) {
                    val isFirstCheck = isCurrentlyOnline == null
                    NotificationHelper.showToast(
                        this@VaultApp,
                        if (isFirstCheck) "Terhubung ke internet" else "Koneksi internet kembali normal"
                    )
                }
                isCurrentlyOnline = true
            }

            override fun onLost(network: Network) {
                if (isCurrentlyOnline != false) {
                    val isFirstCheck = isCurrentlyOnline == null
                    NotificationHelper.showToast(
                        this@VaultApp,
                        if (isFirstCheck) "Tidak ada koneksi internet" else "Koneksi internet putus"
                    )
                }
                isCurrentlyOnline = false
            }
        })
    }
}