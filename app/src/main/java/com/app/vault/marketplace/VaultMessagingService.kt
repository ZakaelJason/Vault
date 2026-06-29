package com.app.vault.marketplace

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class VaultMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveTokenToFirestore(token)
    }

    private fun saveTokenToFirestore(token: String) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        Firebase.firestore.collection("users").document(uid)
            .update("fcmToken", token)
            .addOnFailureListener {
                // Jika dokumen belum ada field ini, set() dengan merge sebagai fallback
                Firebase.firestore.collection("users").document(uid)
                    .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
            }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        val title = message.data["title"] ?: message.notification?.title ?: "Vault"
        val body  = message.data["body"]  ?: message.notification?.body  ?: ""

        // Android 13+ mewajibkan pengecekan izin POST_NOTIFICATIONS
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                // Jika tidak ada izin, tidak bisa menampilkan notifikasi sistem tray
                return
            }
        }

        NotificationHelper.show(
            this, NotificationHelper.CHANNEL_ORDERS,
            notifId = System.currentTimeMillis().toInt(),
            title = title, message = body
        )
    }
}