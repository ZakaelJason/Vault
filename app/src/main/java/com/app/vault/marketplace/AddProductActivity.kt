package com.app.vault.marketplace

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.app.vault.marketplace.databinding.ActivityAddProductBinding
import com.bumptech.glide.Glide

class AddProductActivity : AppCompatActivity() {
    private lateinit var b: ActivityAddProductBinding
    private val repo = FirebaseRepository()
    private var editDocId: String = ""
    private var selectedImageUri: Uri? = null
    private var currentImageUrl: String = ""

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            b.ivProduct.setImageURI(selectedImageUri)
            b.tvAddPhoto.visibility = android.view.View.GONE
        }
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(b.root)

        editDocId = intent.getStringExtra("firestore_doc_id") ?: ""

        val categories = arrayOf("Top Up", "Account", "Joki", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        b.etCategory.setAdapter(adapter)

        if (editDocId.isNotEmpty()) {
            setupEditMode()
        }

        b.cardImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            pickImage.launch(intent)
        }

        b.btnBack.setOnClickListener { finish() }
        b.btnSubmit.setOnClickListener { saveProduct() }
    }

    private fun setupEditMode() {
        b.btnSubmit.text = "Update Product"
        repo.getProduct(
            docId = editDocId,
            onSuccess = { item ->
                b.etName.setText(item.name)
                b.etPrice.setText(item.price.toString())
                b.etDesc.setText(item.description)
                b.etCategory.setText(item.category, false)

                currentImageUrl = item.imageUrl
                if (currentImageUrl.isNotEmpty()) {
                    Glide.with(this).load(currentImageUrl).into(b.ivProduct)
                    b.tvAddPhoto.visibility = android.view.View.GONE
                }
            },
            onError = {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun saveProduct() {
        val name     = b.etName.text.toString().trim()
        val priceStr = b.etPrice.text.toString().trim()
        val desc     = b.etDesc.text.toString().trim()
        val cat      = b.etCategory.text.toString().trim()

        if (name.isEmpty() || priceStr.isEmpty() || desc.isEmpty() || cat.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceStr.toDoubleOrNull() ?: 0.0
        b.btnSubmit.isEnabled = false

        if (selectedImageUri != null) {
            repo.uploadImage(
                uri = selectedImageUri!!,
                folder = "products",
                onSuccess = { url -> persistProduct(name, price, desc, cat, url) },
                onError = {
                    b.btnSubmit.isEnabled = true
                    Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            persistProduct(name, price, desc, cat, currentImageUrl)
        }
    }

    private fun persistProduct(name: String, price: Double, desc: String, cat: String, imageUrl: String) {
        if (editDocId.isNotEmpty()) {
            repo.updateProduct(
                docId = editDocId, name = name, price = price, description = desc,
                category = cat, imageUrl = imageUrl,
                onSuccess = {
                    Toast.makeText(this, "Product updated!", Toast.LENGTH_SHORT).show()
                    finish()
                },
                onError = {
                    b.btnSubmit.isEnabled = true
                    Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            val session = SessionManager(this)
            val uid = repo.currentUser?.uid ?: ""
            val sellerName = session.getUsername()

            val item = FirestoreItem(
                sellerUid = uid,
                sellerName = sellerName,
                name = name,
                price = price,
                description = desc,
                imageUrl = imageUrl,
                category = cat
            )

            repo.addProduct(
                item = item,
                onSuccess = {
                    Toast.makeText(this, "Item listed!", Toast.LENGTH_SHORT).show()
                    finish()
                },
                onError = {
                    b.btnSubmit.isEnabled = true
                    Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
