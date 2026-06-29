package com.app.vault.marketplace

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    const val CHANNEL_GENERAL = "vault_general"
    const val CHANNEL_ORDERS  = "vault_orders"
    const val CHANNEL_NETWORK = "vault_network"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ORDERS, "Pesanan & Pembayaran", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifikasi orderan baru, status pembayaran, dan bukti transfer"
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_GENERAL, "Umum", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    /** Notifikasi system tray — dipakai untuk pesanan, pembayaran, dan push antar device. */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun show(context: Context, channelId: String, notifId: Int, title: String, message: String) {
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // ganti dengan ikon kecil yang sesuai jika ada
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(notifId, builder.build())
    }

    /** Toast sederhana — dipakai khusus untuk status koneksi online/offline. */
    fun showToast(context: Context, message: String) {
        // Pastikan selalu jalan di main thread, karena NetworkCallback bisa terpanggil
        // dari background thread dan Toast wajib dari main thread.
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }
}