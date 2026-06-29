package com.app.vault.marketplace

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class VaultMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("VaultFCM", "Token baru dihasilkan: $token")
        saveTokenToFirestore(token)
    }

    private fun saveTokenToFirestore(token: String) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        Firebase.firestore.collection("users").document(uid)
            .update("fcmToken", token)
            .addOnSuccessListener { Log.d("VaultFCM", "Token berhasil diupdate di Firestore") }
            .addOnFailureListener {
                Firebase.firestore.collection("users").document(uid)
                    .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
            }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("VaultFCM", "Pesan FCM Masuk dari: ${message.from}")
        
        val title = message.data["title"] ?: message.notification?.title ?: "Vault"
        val body  = message.data["body"]  ?: message.notification?.body  ?: ""
        val type  = message.data["type"]  ?: "general"

        NotificationHelper.showToast(this, "FCM Diterima: $body")

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                Log.e("VaultFCM", "Gagal menampilkan notif: Izin POST_NOTIFICATIONS tidak ada.")
                return
            }
        }

        val channelId = when (type) {
            "chat"  -> NotificationHelper.CHANNEL_CHATS
            "order" -> NotificationHelper.CHANNEL_ORDERS
            else    -> NotificationHelper.CHANNEL_GENERAL
        }

        NotificationHelper.show(
            this, channelId,
            notifId = System.currentTimeMillis().toInt(),
            title = title, message = body
        )
    }
}