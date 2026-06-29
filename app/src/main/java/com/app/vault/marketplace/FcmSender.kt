package com.app.vault.marketplace

import android.content.Context
import android.util.Log
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.Firebase
import com.google.firebase.app
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object FcmSender {

    private const val SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
    private val client = OkHttpClient()

    private suspend fun getAccessToken(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            val stream = context.resources.openRawResource(R.raw.service_account)
            val credentials = GoogleCredentials.fromStream(stream).createScoped(listOf(SCOPE))
            credentials.refresh()
            credentials.accessToken.tokenValue
        } catch (e: Exception) {
            Log.e("FcmSender", "OAuth2 Error: ${e.message}. Periksa file service_account.json Anda.")
            null
        }
    }

    suspend fun sendToToken(
        context: Context,
        targetToken: String,
        title: String,
        body: String,
        type: String = "general"
    ): Boolean = withContext(Dispatchers.IO) {
        val accessToken = getAccessToken(context) ?: return@withContext false
        val projectId   = Firebase.app.options.projectId ?: "vault-app-91f13"

        val channelId = when (type) {
            "chat"  -> NotificationHelper.CHANNEL_CHATS
            "order" -> NotificationHelper.CHANNEL_ORDERS
            else    -> NotificationHelper.CHANNEL_GENERAL
        }

        val payload = JSONObject().apply {
            put("message", JSONObject().apply {
                put("token", targetToken)
                put("notification", JSONObject().apply {
                    put("title", title)
                    put("body", body)
                })
                put("android", JSONObject().apply {
                    put("priority", "HIGH")
                    put("notification", JSONObject().apply {
                        put("channel_id", channelId)
                        put("notification_priority", "PRIORITY_MAX")
                        put("sound", "default")
                    })
                })
                put("data", JSONObject().apply {
                    put("title", title)
                    put("body", body)
                    put("type",  type)
                })
            })
        }

        val request = Request.Builder()
            .url("https://fcm.googleapis.com/v1/projects/$projectId/messages:send")
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json; UTF-8")
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response -> 
                val resStr = response.body?.string()
                if (response.isSuccessful) {
                    Log.d("FcmSender", "Notifikasi Terkirim!")
                    true
                } else {
                    Log.e("FcmSender", "FCM Error: ${response.code} - $resStr")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("FcmSender", "Network Error: ${e.message}")
            false
        }
    }
}