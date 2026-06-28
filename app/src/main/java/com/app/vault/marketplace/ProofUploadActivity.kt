package com.app.vault.marketplace

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.app.vault.marketplace.databinding.ActivityProofUploadBinding
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class ProofUploadActivity : AppCompatActivity() {
    private lateinit var b: ActivityProofUploadBinding
    private val firestoreDb by lazy { Firebase.firestore }
    private val repo = FirebaseRepository()
    private var selectedImageUri: Uri? = null
    private var firestoreDocId: String = ""

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            b.ivProof.setImageURI(it)
            b.ivProof.visibility = View.VISIBLE
            b.tvProofPlaceholder.visibility = View.GONE
        }
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityProofUploadBinding.inflate(layoutInflater)
        setContentView(b.root)

        firestoreDocId = intent.getStringExtra("firestore_doc_id") ?: ""
        if (firestoreDocId.isEmpty()) { finish(); return }

        val session = SessionManager(this)
        b.btnBack.setOnClickListener { finish() }

        // Baca data transaksi dari Firestore berdasarkan docId
        firestoreDb.collection("transactions")
            .document(firestoreDocId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) { finish(); return@addOnSuccessListener }

                val itemName    = doc.getString("itemName") ?: ""
                val buyerName   = doc.getString("buyerUsername") ?: ""
                val sellerName  = doc.getString("sellerUsername") ?: ""
                val status      = doc.getString("status") ?: "Pending"
                val proofUrl    = doc.getString("proofImageUrl") ?: ""

                b.tvOrderInfo.text =
                    "Item: $itemName\nBuyer: $buyerName → Seller: $sellerName\nStatus: $status"

                // Tampilkan gambar bukti kalau sudah ada (URL Firebase Storage, bisa diakses dari device manapun)
                if (proofUrl.isNotEmpty()) {
                    Glide.with(this).load(proofUrl)
                        .placeholder(R.drawable.placeholder_bukti)
                        .error(R.drawable.placeholder_bukti)
                        .into(b.ivProof)
                    b.ivProof.visibility = View.VISIBLE
                    b.tvProofPlaceholder.visibility = View.GONE
                }

                val isSeller = sellerName == session.getUsername()
                val isBuyer  = buyerName  == session.getUsername()

                when {
                    isSeller && (status == "Pending" || status == "Proof Uploaded") ->
                        setupSellerView(status, proofUrl)
                    isBuyer && status == "Proof Uploaded" ->
                        setupBuyerView()
                    else -> {
                        b.btnSelectImage.visibility = View.GONE
                        b.btnAction.isEnabled = false
                        b.btnAction.text = "No Action Required"
                    }
                }
            }
            .addOnFailureListener { finish() }
    }

    private fun setupSellerView(status: String, existingProofUrl: String) {
        b.tvTitle.text = if (status == "Pending") "Upload Proof" else "Update Proof"
        b.btnSelectImage.visibility = View.VISIBLE
        b.btnAction.text = "Submit Proof"

        b.btnSelectImage.setOnClickListener { pickImage.launch("image/*") }

        b.btnAction.setOnClickListener {
            val uri = selectedImageUri
            if (uri == null && existingProofUrl.isEmpty()) {
                Toast.makeText(this, "Please select a proof image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            b.btnAction.isEnabled = false

            if (uri != null) {
                repo.uploadImage(
                    uri = uri,
                    folder = "proofs",
                    onSuccess = { url -> submitProof(url) },
                    onError = {
                        b.btnAction.isEnabled = true
                        Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                submitProof(existingProofUrl)
            }
        }
    }

    private fun submitProof(proofUrl: String) {
        firestoreDb.collection("transactions")
            .document(firestoreDocId)
            .update(
                mapOf(
                    "status"        to "Proof Uploaded",
                    "proofImageUrl" to proofUrl
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Proof uploaded!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                b.btnAction.isEnabled = true
                Toast.makeText(this, "Gagal upload, coba lagi", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupBuyerView() {
        b.tvTitle.text = "Confirm Receipt"
        b.btnSelectImage.visibility = View.GONE
        b.btnAction.text = "Confirm Receipt"

        b.btnAction.setOnClickListener {
            firestoreDb.collection("transactions")
                .document(firestoreDocId)
                .update("status", "Completed")
                .addOnSuccessListener {
                    Toast.makeText(this, "Transaction completed!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal konfirmasi, coba lagi", Toast.LENGTH_SHORT).show()
                }
        }
    }
}