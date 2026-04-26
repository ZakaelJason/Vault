package com.app.vault.marketplace

import android.content.Context
import android.content.SharedPreferences

class SessionManager(ctx: Context) {
    private val prefs: SharedPreferences = ctx.getSharedPreferences("vault_session", Context.MODE_PRIVATE)

    fun save(userId: Int, username: String) {
        prefs.edit().putInt("user_id", userId).putString("username", username).putBoolean("logged_in", true).apply()
    }

    fun isLoggedIn() = prefs.getBoolean("logged_in", false)
    fun getUserId() = prefs.getInt("user_id", -1)
    fun getUsername() = prefs.getString("username", "") ?: ""

    fun clear() = prefs.edit().clear().apply()
}