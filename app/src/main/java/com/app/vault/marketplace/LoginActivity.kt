package com.app.vault.marketplace

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.app.vault.marketplace.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var b: ActivityLoginBinding
    private lateinit var db: DatabaseHelper
    private lateinit var session: SessionManager

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)
        db = DatabaseHelper(this)
        session = SessionManager(this)

        b.btnLogin.setOnClickListener {
            val u = b.etUsername.text.toString().trim()
            val p = b.etPassword.text.toString().trim()
            if (u.isEmpty() || p.isEmpty()) { toast("Fill all fields"); return@setOnClickListener }
            val user = db.login(u, p)
            if (user != null) {
                session.save(user.id, user.username)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else toast("Invalid credentials")
        }

        b.btnGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}