package com.app.vault.marketplace

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.app.vault.marketplace.databinding.ActivityItemDetailBinding
import java.text.NumberFormat
import java.util.Locale

class ItemDetailActivity : AppCompatActivity() {
    private lateinit var b: ActivityItemDetailBinding

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityItemDetailBinding.inflate(layoutInflater)
        setContentView(b.root)

        val itemId = intent.getIntExtra("item_id", -1)
        if (itemId == -1) { finish(); return }

        val db = DatabaseHelper(this)
        val session = SessionManager(this)
        val item = db.getItem(itemId)

        if (item == null) { finish(); return }

        val fmt = NumberFormat.getNumberInstance(Locale("id", "ID"))
        b.tvName.text = item.name
        b.tvPrice.text = "Rp ${fmt.format(item.price)}"
        b.tvDesc.text = item.description
        b.tvSeller.text = item.sellerName
        b.tvImgId.text = "IMG_${item.id}"

        val isMine = item.sellerId == session.getUserId()
        if (isMine) b.btnBuy.visibility = android.view.View.GONE

        b.btnBack.setOnClickListener { finish() }

        b.btnShare.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Check out '${item.name}' on Vault Marketplace for Rp ${fmt.format(item.price)}!")
                putExtra(Intent.EXTRA_SUBJECT, "Vault - ${item.name}")
            }.also { startActivity(Intent.createChooser(it, "Share Item")) }
        }

        b.btnBuy.setOnClickListener {
            db.insertTransaction(item.id, session.getUserId(), item.sellerId)
            Toast.makeText(this, "Order placed!", Toast.LENGTH_SHORT).show()
            finish()
            (this as? MainActivity)?.navigateToOrders()
            // Navigate via intent flags
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("open_orders", true)
            }.also { startActivity(it) }
        }
    }
}