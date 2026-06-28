package com.app.vault.marketplace

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.vault.marketplace.databinding.ActivityItemDetailBinding
import com.app.vault.marketplace.databinding.DialogReplyBinding
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.text.NumberFormat
import java.util.Locale

class ItemDetailActivity : AppCompatActivity() {
    private lateinit var b: ActivityItemDetailBinding
    private lateinit var db: DatabaseHelper
    private lateinit var sm: SessionManager
    private val firestoreDb by lazy { Firebase.firestore }

    // FIX: sekarang pakai firestoreDocId, bukan integer itemId
    private var firestoreDocId: String = ""

    // Data produk yang dibaca dari Firestore
    private var currentItem: FirestoreItem? = null

    // itemId lokal — dipakai hanya untuk comments (SQLite lokal)
    private var localItemId: Int = -1

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityItemDetailBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = DatabaseHelper(this)
        sm = SessionManager(this)

        firestoreDocId = intent.getStringExtra("firestore_doc_id") ?: ""
        if (firestoreDocId.isEmpty()) { finish(); return }

        b.toolbar.setNavigationOnClickListener { finish() }
        loadFromFirestore()
    }

    private fun loadFromFirestore() {
        firestoreDb.collection("products")
            .document(firestoreDocId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) { finish(); return@addOnSuccessListener }

                val item = FirestoreItem(
                    firestoreDocId = doc.id,
                    name        = doc.getString("name") ?: "",
                    price       = doc.getDouble("price") ?: 0.0,
                    description = doc.getString("description") ?: "",
                    imageUri    = doc.getString("imageUri") ?: "",
                    category    = doc.getString("category") ?: "Other",
                    sellerName  = doc.getString("sellerName") ?: ""
                )
                currentItem = item
                localItemId = (doc.getLong("localId") ?: -1L).toInt()
                displayItem(item)
                if (localItemId != -1) loadComments()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat produk", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun displayItem(item: FirestoreItem) {
        val fmt = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

        b.tvName.text   = item.name
        b.tvPrice.text  = fmt.format(item.price).replace("Rp", "Rp ")
        b.tvDesc.text   = item.description
        b.tvSeller.text = item.sellerName

        // Gambar: path lokal hanya valid di device pemilik — pakai placeholder untuk device lain
        val resId = when (item.category) {
            "Joki"    -> R.drawable.placeholder_joki
            "Top Up"  -> R.drawable.placeholder_topup
            "Account" -> R.drawable.placeholder_account
            else      -> R.drawable.placeholder_account
        }
        b.ivItemImage.setImageResource(resId)

        val isMine = item.sellerName == sm.getUsername()
        if (isMine) {
            b.btnBuy.visibility  = View.GONE
            b.btnChat.visibility = View.GONE
            b.layoutAddComment.visibility = View.GONE
        } else {
            b.btnBuy.visibility  = View.VISIBLE
            b.btnChat.visibility = View.VISIBLE
            b.btnBuy.setOnClickListener  { showCheckoutDialog(item, fmt) }
            b.btnChat.setOnClickListener {
                startActivity(Intent(this, ChatActivity::class.java).apply {
                    putExtra("seller_username", item.sellerName)
                    putExtra("buyer_username",  sm.getUsername())
                    putExtra("item_name",       item.name)
                })
            }
        }

        b.btnShare.setOnClickListener {
            startActivity(Intent.createChooser(Intent().apply {
                action = Intent.ACTION_SEND
                type   = "text/plain"
                putExtra(Intent.EXTRA_TEXT,
                    "Cek produk ini di Vault: ${item.name} — ${b.tvPrice.text}")
            }, "Bagikan via"))
        }
    }

    private fun showCheckoutDialog(item: FirestoreItem, fmt: NumberFormat) {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view   = layoutInflater.inflate(R.layout.dialog_checkout, null)

        view.findViewById<android.widget.TextView>(R.id.tvCheckoutItemName).text = "Item: ${item.name}"
        view.findViewById<android.widget.TextView>(R.id.tvCheckoutTotal).text =
            "Total: ${fmt.format(item.price).replace("Rp", "Rp ")}"

        val rgPayment  = view.findViewById<android.widget.RadioGroup>(R.id.rgPaymentMethods)
        val btnConfirm = view.findViewById<android.widget.Button>(R.id.btnConfirmPurchase)

        btnConfirm.setOnClickListener {
            val paymentMethod = when (rgPayment.checkedRadioButtonId) {
                R.id.rbBankTransfer -> "Bank Transfer"
                R.id.rbEWallet      -> "E-Wallet"
                else -> {
                    Toast.makeText(this, "Pilih metode pembayaran", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
            dialog.dismiss()

            val txnData = hashMapOf(
                "itemId"         to localItemId,
                "itemName"       to item.name,
                "buyerUsername"  to sm.getUsername(),
                "sellerUsername" to item.sellerName,
                "paymentMethod"  to paymentMethod,
                "status"         to "Pending",
                "proofImageUri"  to "",
                "createdAt"      to System.currentTimeMillis()
            )

            firestoreDb.collection("transactions")
                .add(txnData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Pesanan berhasil!", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("open_orders", true)
                    })
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal membuat pesanan", Toast.LENGTH_SHORT).show()
                }
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun loadComments() {
        if (localItemId == -1) return
        val comments = db.getComments(localItemId)
        b.rvComments.layoutManager = LinearLayoutManager(this)
        b.rvComments.adapter = CommentAdapter(
            comments, sm.getUserId(),
            currentItem?.let {
                // sellerId lokal tidak diketahui dari Firestore, pakai -1
                // CommentAdapter hanya butuh ini untuk tampilkan reply button ke seller
                if (it.sellerName == sm.getUsername()) sm.getUserId() else -1
            } ?: -1
        ) { comment -> showReplyDialog(comment.id) }

        b.btnSendComment.setOnClickListener {
            val text = b.etComment.text.toString().trim()
            if (text.isNotEmpty() && localItemId != -1) {
                db.addComment(localItemId, sm.getUserId(), text)
                b.etComment.setText("")
                loadComments()
            }
        }
    }

    private fun showReplyDialog(commentId: Int) {
        val dbinding = DialogReplyBinding.inflate(LayoutInflater.from(this))
        AlertDialog.Builder(this)
            .setView(dbinding.root)
            .setPositiveButton("Kirim") { _, _ ->
                val replyText = dbinding.etReply.text.toString().trim()
                if (replyText.isNotEmpty()) {
                    db.addReply(commentId, replyText)
                    loadComments()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}