package com.app.vault.marketplace

import android.content.Context
import android.content.SharedPreferences

/**
 * Menyimpan cache ringan profil user yang sedang login (uid & username)
 * agar tidak perlu fetch Firestore berulang kali untuk hal-hal sederhana
 * seperti menampilkan nama di toolbar.
 *
 * Status login yang sesungguhnya berasal dari FirebaseAuth.currentUser
 * (lihat FirebaseRepository.isLoggedIn), bukan dari SharedPreferences ini.
 */
class SessionManager(ctx: Context) {
    private val prefs: SharedPreferences = ctx.getSharedPreferences("vault_session", Context.MODE_PRIVATE)

    fun saveProfile(uid: String, username: String) {
        prefs.edit().putString("uid", uid).putString("username", username).apply()
    }

    fun getUid() = prefs.getString("uid", "") ?: ""
    fun getUsername() = prefs.getString("username", "") ?: ""

    fun isFirstRun(): Boolean {
        val first = prefs.getBoolean("first_run", true)
        if (first) prefs.edit().putBoolean("first_run", false).apply()
        return first
    }

    fun clear() = prefs.edit().clear().apply()
}