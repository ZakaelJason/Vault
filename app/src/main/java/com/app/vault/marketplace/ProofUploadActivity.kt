package com.app.vault.marketplace

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.app.vault.marketplace.databinding.ActivityProofUploadBinding
import java.text.NumberFormat
import java.util.Locale

class ProofUploadActivity : AppCompatActivity() {
    private lateinit var b: ActivityProofUploadBinding
    private var selectedImageUri: Uri? = null
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            b.ivProof.setImageURI(it)
            b.ivProof.visibility = android.view.View.VISIBLE
            b.tvProofPlaceholder.visibility = android.view.View.GONE
        }
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityProofUploadBinding.inflate(layoutInflater)
        setContentView(b.root)

        val txnId = intent.getIntExtra("transaction_id", -1)
        if (txnId == -1) { finish(); return }

        val db = DatabaseHelper(this)
        val session = SessionManager(this)
        val txn = db.getTransaction(txnId)

        if (txn == null) { finish(); return }

        b.btnBack.setOnClickListener { finish() }
        b.tvOrderInfo.text = "Item: ${txn.itemName}\nBuyer: ${txn.buyerName} → Seller: ${txn.sellerName}\nStatus: ${txn.status}"

        val isSeller = txn.sellerId == session.getUserId()
        val isBuyer = txn.buyerId == session.getUserId()

        when {
            isSeller && txn.status == "Pending" -> setupSellerPendingView(db, txn)
            isBuyer && txn.status == "Proof Uploaded" -> setupBuyerProofView(db, txn)
            else -> {
                b.btnSelectImage.visibility = android.view.View.GONE
                b.btnAction.isEnabled = false
                b.btnAction.text = "No Action Required"
                if (txn.proofImageUri.isNotEmpty() && txn.proofImageUri != "mock_proof_uri") {
                    b.ivProof.setImageURI(Uri.parse(txn.proofImageUri))
                    b.ivProof.visibility = android.view.View.VISIBLE
                    b.tvProofPlaceholder.visibility = android.view.View.GONE
                } else if (txn.status != "Pending") {
                    b.tvProofPlaceholder.text = "PROOF_SUBMITTED_ID_${txn.id}"
                }
            }
        }
    }

    private fun setupSellerPendingView(db: DatabaseHelper, txn: Transaction) {
        b.tvTitle.text = "Upload Proof"
        b.btnSelectImage.visibility = android.view.View.VISIBLE
        b.btnAction.text = "Submit Proof"
        b.tvProofPlaceholder.text = "Tap 'Select Screenshot' to choose delivery proof"

        b.btnSelectImage.setOnClickListener { pickImage.launch("image/*") }

        b.btnAction.setOnClickListener {
            val uri = selectedImageUri
            if (uri == null) { Toast.makeText(this, "Please select a proof image", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            db.updateTransactionProof(txn.id, uri.toString())
            Toast.makeText(this, "Proof uploaded!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupBuyerProofView(db: DatabaseHelper, txn: Transaction) {
        b.tvTitle.text = "Confirm Receipt"
        b.btnSelectImage.visibility = android.view.View.GONE
        b.btnAction.text = "Confirm Receipt"

        if (txn.proofImageUri.isNotEmpty() && txn.proofImageUri != "mock_proof_uri") {
            try {
                b.ivProof.setImageURI(Uri.parse(txn.proofImageUri))
                b.ivProof.visibility = android.view.View.VISIBLE
                b.tvProofPlaceholder.visibility = android.view.View.GONE
            } catch (e: Exception) {
                b.tvProofPlaceholder.text = "PROOF_IMAGE_ID_${txn.id}"
            }
        } else {
            b.tvProofPlaceholder.text = "MOCK_PROOF_ID_${txn.id}"
        }

        b.btnAction.setOnClickListener {
            db.completeTransaction(txn.id)
            Toast.makeText(this, "Transaction completed!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}