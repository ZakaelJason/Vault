package com.app.vault.marketplace

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.app.vault.marketplace.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {
    private lateinit var b: ActivityRegisterBinding
    private val repo = FirebaseRepository()

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnRegister.setOnClickListener {
            val e = b.etEmail.text.toString().trim()
            val u = b.etUsername.text.toString().trim()
            val p = b.etPassword.text.toString().trim()
            val c = b.etConfirm.text.toString().trim()
            if (e.isEmpty() || u.isEmpty() || p.isEmpty() || c.isEmpty()) { toast("Fill all fields"); return@setOnClickListener }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()) { toast("Invalid email format"); return@setOnClickListener }
            if (p != c) { toast("Passwords do not match"); return@setOnClickListener }
            if (p.length < 6) { toast("Password min 6 chars"); return@setOnClickListener }

            b.btnRegister.isEnabled = false
            repo.register(
                email = e, username = u, password = p,
                onSuccess = {
                    toast("Account created!")
                    finish()
                },
                onError = { msg ->
                    b.btnRegister.isEnabled = true
                    toast(msg)
                }
            )
        }

        b.btnGoLogin.setOnClickListener { finish() }
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}