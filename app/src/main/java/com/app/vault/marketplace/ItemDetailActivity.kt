package com.app.vault.marketplace

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.vault.marketplace.databinding.ActivityItemDetailBinding
import com.app.vault.marketplace.databinding.DialogReplyBinding
import java.io.File
import java.text.NumberFormat
import java.util.Locale

class ItemDetailActivity : AppCompatActivity() {
    private lateinit var b: ActivityItemDetailBinding
    private lateinit var db: DatabaseHelper
    private lateinit var sm: SessionManager
    private var itemId: Int = -1

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityItemDetailBinding.inflate(layoutInflater)
        setContentView(b.root)

        itemId = intent.getIntExtra("item_id", -1)
        if (itemId == -1) { finish(); return }

        db = DatabaseHelper(this)
        sm = SessionManager(this)
        
        loadItemDetails()
        loadComments()

        b.toolbar.setNavigationOnClickListener { finish() }

        b.btnSendComment.setOnClickListener {
            val text = b.etComment.text.toString().trim()
            if (text.isNotEmpty()) {
                db.addComment(itemId, sm.getUserId(), text)
                b.etComment.setText("")
                loadComments()
            }
        }
    }

    private fun loadItemDetails() {
        val item = db.getItem(itemId) ?: return

        val fmt = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        b.tvName.text = item.name
        b.tvPrice.text = fmt.format(item.price).replace("Rp", "Rp ")
        b.tvDesc.text = item.description
        b.tvSeller.text = item.sellerName
        
        if (item.imageUri.isNotEmpty()) {
            val file = File(item.imageUri)
            if (file.exists()) {
                b.ivItemImage.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
            } else {
                setProductPlaceholder(item.category)
            }
        } else {
            setProductPlaceholder(item.category)
        }

        val isMine = item.sellerId == sm.getUserId()
        if (isMine) {
            b.btnBuy.visibility = View.GONE
            b.layoutAddComment.visibility = View.GONE
        }

        b.btnShare.setOnClickListener {
            val shareText = "${item.name} - ${fmt.format(item.price).replace("Rp", "Rp ")}. Available in Vault"
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }

        b.btnBuy.setOnClickListener {
            db.insertTransaction(item.id, sm.getUserId(), item.sellerId)
            Toast.makeText(this, "Order placed!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("open_orders", true)
            })
            finish()
        }
    }

    private fun setProductPlaceholder(category: String) {
        val resId = when (category) {
            "Joki" -> R.drawable.placeholder_joki
            "Top Up" -> R.drawable.placeholder_topup
            "Account" -> R.drawable.placeholder_account
            else -> R.drawable.placeholder_account
        }
        b.ivItemImage.setImageResource(resId)
    }

    private fun loadComments() {
        val item = db.getItem(itemId) ?: return
        val comments = db.getComments(itemId)
        b.rvComments.layoutManager = LinearLayoutManager(this)
        b.rvComments.adapter = CommentAdapter(comments, sm.getUserId(), item.sellerId) { comment ->
            showReplyDialog(comment.id)
        }
    }

    private fun showReplyDialog(commentId: Int) {
        val dbinding = DialogReplyBinding.inflate(LayoutInflater.from(this))
        AlertDialog.Builder(this)
            .setView(dbinding.root)
            .setPositiveButton("Send") { _, _ ->
                val replyText = dbinding.etReply.text.toString().trim()
                if (replyText.isNotEmpty()) {
                    db.addReply(commentId, replyText)
                    loadComments()
                    Toast.makeText(this, "Reply sent", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
