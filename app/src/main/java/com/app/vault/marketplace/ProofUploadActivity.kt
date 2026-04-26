package com.app.vault.marketplace

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.app.vault.marketplace.databinding.ActivityProofUploadBinding
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ProofUploadActivity : AppCompatActivity() {
    private lateinit var b: ActivityProofUploadBinding
    private var selectedImageUri: Uri? = null
    
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

        // Load image or placeholder
        if (txn.proofImageUri.isNotEmpty()) {
            val file = File(txn.proofImageUri)
            if (file.exists()) {
                b.ivProof.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
                b.ivProof.visibility = View.VISIBLE
                b.tvProofPlaceholder.visibility = View.GONE
            } else {
                b.ivProof.setImageResource(R.drawable.placeholder_bukti)
                b.ivProof.visibility = View.VISIBLE
                b.tvProofPlaceholder.visibility = View.GONE
            }
        } else if (txn.status != "Pending") {
            b.ivProof.setImageResource(R.drawable.placeholder_bukti)
            b.ivProof.visibility = View.VISIBLE
            b.tvProofPlaceholder.visibility = View.GONE
        }

        when {
            isSeller && (txn.status == "Pending" || txn.status == "Proof Uploaded") -> setupSellerUploadView(db, txn)
            isBuyer && txn.status == "Proof Uploaded" -> setupBuyerProofView(db, txn)
            else -> {
                b.btnSelectImage.visibility = View.GONE
                b.btnAction.isEnabled = false
                b.btnAction.text = "No Action Required"
            }
        }
    }

    private fun setupSellerUploadView(db: DatabaseHelper, txn: Transaction) {
        b.tvTitle.text = if (txn.status == "Pending") "Upload Proof" else "Update Proof"
        b.btnSelectImage.visibility = View.VISIBLE
        b.btnAction.text = "Submit Proof"
        
        b.btnSelectImage.setOnClickListener { pickImage.launch("image/*") }

        b.btnAction.setOnClickListener {
            val uri = selectedImageUri
            if (uri == null && txn.proofImageUri.isEmpty()) { 
                Toast.makeText(this, "Please select a proof image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener 
            }
            
            val finalPath = if (uri != null) copyImageToInternal(uri) else txn.proofImageUri
            if (finalPath != null) {
                db.updateTransactionProof(txn.id, finalPath)
                Toast.makeText(this, "Proof updated successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupBuyerProofView(db: DatabaseHelper, txn: Transaction) {
        b.tvTitle.text = "Confirm Receipt"
        b.btnSelectImage.visibility = View.GONE
        b.btnAction.text = "Confirm Receipt"

        b.btnAction.setOnClickListener {
            db.completeTransaction(txn.id)
            Toast.makeText(this, "Transaction completed!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun copyImageToInternal(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = "proof_${UUID.randomUUID()}.jpg"
            val file = File(filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}