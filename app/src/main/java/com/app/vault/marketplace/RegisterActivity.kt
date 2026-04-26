package com.app.vault.marketplace

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.app.vault.marketplace.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {
    private lateinit var b: ActivityRegisterBinding
    private lateinit var db: DatabaseHelper

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(b.root)
        db = DatabaseHelper(this)

        b.btnRegister.setOnClickListener {
            val u = b.etUsername.text.toString().trim()
            val p = b.etPassword.text.toString().trim()
            val c = b.etConfirm.text.toString().trim()
            if (u.isEmpty() || p.isEmpty() || c.isEmpty()) { toast("Fill all fields"); return@setOnClickListener }
            if (p != c) { toast("Passwords do not match"); return@setOnClickListener }
            if (p.length < 6) { toast("Password min 6 chars"); return@setOnClickListener }
            if (db.register(u, p)) { toast("Account created!"); finish() }
            else toast("Username already taken")
        }

        b.btnGoLogin.setOnClickListener { finish() }
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}