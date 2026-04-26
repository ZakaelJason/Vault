package com.app.vault.marketplace

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.app.vault.marketplace.databinding.ActivityAddProductBinding

class AddProductActivity : AppCompatActivity() {
    private lateinit var b: ActivityAddProductBinding

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnBack.setOnClickListener { finish() }

        b.btnSubmit.setOnClickListener {
            val name = b.etName.text.toString().trim()
            val priceStr = b.etPrice.text.toString().trim()
            val desc = b.etDesc.text.toString().trim()

            if (name.isEmpty() || priceStr.isEmpty() || desc.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val price = priceStr.toDoubleOrNull()
            if (price == null || price <= 0) {
                Toast.makeText(this, "Enter a valid price", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val session = SessionManager(this)
            DatabaseHelper(this).addItem(session.getUserId(), name, price, desc)
            Toast.makeText(this, "Item listed!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}