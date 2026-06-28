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
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.util.Locale

class ItemDetailActivity : AppCompatActivity() {
    private lateinit var b: ActivityItemDetailBinding
    private lateinit var sm: SessionManager
    private val repo = FirebaseRepository()

    private var firestoreDocId: String = ""
    private var currentItem: FirestoreItem? = null

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityItemDetailBinding.inflate(layoutInflater)
        setContentView(b.root)

        sm = SessionManager(this)

        firestoreDocId = intent.getStringExtra("firestore_doc_id") ?: ""
        if (firestoreDocId.isEmpty()) { finish(); return }

        b.toolbar.setNavigationOnClickListener { finish() }
        loadItem()
    }

    private fun loadItem() {
        repo.getProduct(
            docId = firestoreDocId,
            onSuccess = { item ->
                currentItem = item
                displayItem(item)
                loadComments()
            },
            onError = {
                Toast.makeText(this, "Gagal memuat produk", Toast.LENGTH_SHORT).show()
                finish()
            }
        )
    }

    private fun displayItem(item: FirestoreItem) {
        val fmt = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

        b.tvName.text   = item.name
        b.tvPrice.text  = fmt.format(item.price).replace("Rp", "Rp ")
        b.tvDesc.text   = item.description
        b.tvSeller.text = item.sellerName

        val placeholderRes = when (item.category) {
            "Joki"    -> R.drawable.placeholder_joki
            "Top Up"  -> R.drawable.placeholder_topup
            "Account" -> R.drawable.placeholder_account
            else      -> R.drawable.placeholder_account
        }
        if (item.imageUrl.isNotEmpty()) {
            Glide.with(this).load(item.imageUrl)
                .placeholder(placeholderRes).error(placeholderRes)
                .into(b.ivItemImage)
        } else {
            b.ivItemImage.setImageResource(placeholderRes)
        }

        val myUid = repo.currentUser?.uid ?: ""
        val isMine = item.sellerUid == myUid
        if (isMine) {
            b.btnBuy.visibility  = View.GONE
            b.btnChat.visibility = View.GONE
            b.layoutAddComment.visibility = View.GONE
        } else {
            b.btnBuy.visibility  = View.VISIBLE
            b.btnChat.visibility = View.GONE // tampil hanya jika sudah pernah membeli item ini
            b.btnBuy.setOnClickListener  { showCheckoutDialog(item, fmt) }
            b.btnChat.setOnClickListener {
                startActivity(Intent(this, ChatActivity::class.java).apply {
                    putExtra("seller_username", item.sellerName)
                    putExtra("buyer_username",  sm.getUsername())
                    putExtra("item_name",       item.name)
                })
            }
            checkChatEligibility(item, myUid)
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

    private fun checkChatEligibility(item: FirestoreItem, myUid: String) {
        if (myUid.isEmpty()) return
        // Tombol Chat hanya tampil jika user ini sudah membeli item ini
        // (ada dokumen transaksi dengan buyerUid = saya dan itemDocId = produk ini).
        repo.firestore.collection("transactions")
            .whereEqualTo("buyerUid", myUid)
            .whereEqualTo("itemDocId", firestoreDocId)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                b.btnChat.visibility = if (!snapshot.isEmpty) View.VISIBLE else View.GONE
            }
            .addOnFailureListener {
                b.btnChat.visibility = View.GONE
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

            val myUid = repo.currentUser?.uid ?: ""
            val txnData = hashMapOf(
                "itemDocId"      to firestoreDocId,
                "itemName"       to item.name,
                "buyerUid"       to myUid,
                "buyerUsername"  to sm.getUsername(),
                "sellerUid"      to item.sellerUid,
                "sellerUsername" to item.sellerName,
                "paymentMethod"  to paymentMethod,
                "status"         to "Pending",
                "proofImageUrl"  to "",
                "createdAt"      to System.currentTimeMillis()
            )

            repo.firestore.collection("transactions")
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
    }

    private fun showCheckoutDialog(item: FirestoreItem, fmt: NumberFormat) {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view   = layoutInflater.inflate(R.layout.dialog_checkout, null)

        dialog.setContentView(view)
        dialog.show()
    }

    private fun loadComments() {
        repo.firestore.collection("products").document(firestoreDocId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val comments = snapshot.documents.map { doc ->
                    Comment(
                        id = doc.id,
                        userUid = doc.getString("userUid") ?: "",
                        userName = doc.getString("userName") ?: "",
                        text = doc.getString("text") ?: "",
                        reply = doc.getString("reply"),
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                }

                val myUid = repo.currentUser?.uid ?: ""
                val sellerUid = currentItem?.sellerUid ?: ""

                b.rvComments.layoutManager = LinearLayoutManager(this)
                b.rvComments.adapter = CommentAdapter(comments, myUid, sellerUid) { comment ->
                    showReplyDialog(comment.id)
                }
            }

        b.btnSendComment.setOnClickListener {
            val text = b.etComment.text.toString().trim()
            val myUid = repo.currentUser?.uid ?: ""
            if (text.isNotEmpty()) {
                repo.addComment(
                    productDocId = firestoreDocId,
                    userUid = myUid,
                    userName = sm.getUsername(),
                    text = text,
                    onSuccess = { b.etComment.setText("") },
                    onError = { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
                )
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

    private fun showReplyDialog(commentId: String) {
        val dbinding = DialogReplyBinding.inflate(LayoutInflater.from(this))
        MaterialAlertDialogBuilder(this)
            .setView(dbinding.root)
            .setPositiveButton("Kirim") { _, _ ->
                val replyText = dbinding.etReply.text.toString().trim()
                if (replyText.isNotEmpty()) {
                    repo.addReply(
                        productDocId = firestoreDocId,
                        commentId = commentId,
                        replyText = replyText,
                        onSuccess = {},
                        onError = { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
                    )
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
