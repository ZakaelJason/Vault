package com.app.vault.marketplace

import android.content.Context
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object FcmSender {

    private const val PROJECT_ID = "vault-app-91f13" // sesuaikan dengan project_id Anda
    private const val SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
    private val client = OkHttpClient()

    /** Generate OAuth2 access token dari service_account.json. Wajib dipanggil di background thread. */
    private suspend fun getAccessToken(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            val stream = context.resources.openRawResource(R.raw.service_account)
            val credentials = GoogleCredentials.fromStream(stream).createScoped(listOf(SCOPE))
            credentials.refresh()
            credentials.accessToken.tokenValue
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Kirim push notification ke satu device tertentu lewat FCM HTTP v1.
     * @param targetToken FCM token milik device tujuan (diambil dari field fcmToken di Firestore)
     */
    suspend fun sendToToken(
        context: Context,
        targetToken: String,
        title: String,
        body: String
    ): Boolean = withContext(Dispatchers.IO) {
        val accessToken = getAccessToken(context) ?: return@withContext false

        val payload = JSONObject().apply {
            put("message", JSONObject().apply {
                put("token", targetToken)
                put("notification", JSONObject().apply {
                    put("title", title)
                    put("body", body)
                })
                // Kirim juga sebagai data, supaya VaultMessagingService.onMessageReceived
                // konsisten menampilkannya lewat NotificationHelper milik app sendiri
                put("data", JSONObject().apply {
                    put("title", title)
                    put("body", body)
                })
            })
        }

        val request = Request.Builder()
            .url("https://fcm.googleapis.com/v1/projects/$PROJECT_ID/messages:send")
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json; UTF-8")
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response -> response.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }
}