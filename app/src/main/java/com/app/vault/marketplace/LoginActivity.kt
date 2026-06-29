package com.app.vault.marketplace

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.app.vault.marketplace.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var b: ActivityLoginBinding
    private lateinit var session: SessionManager
    private val repo = FirebaseRepository()

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)
        session = SessionManager(this)

        b.btnLogin.setOnClickListener {
            val u = b.etUsername.text.toString().trim()
            val p = b.etPassword.text.toString().trim()
            if (u.isEmpty() || p.isEmpty()) { toast("Fill all fields"); return@setOnClickListener }

            b.btnLogin.isEnabled = false
            repo.loginWithUsername(
                username = u, password = p,
                onSuccess = { profile ->
                    session.saveProfile(profile.uid, profile.username)

                    // Daftarkan FCM token device ini ke profil user
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                        .addOnSuccessListener { token ->
                            repo.firestore.collection("users").document(profile.uid)
                                .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                        }

                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                },
                onError = { msg ->
                    b.btnLogin.isEnabled = true
                    toast(msg)
                }
            )
        }

        b.btnGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}
