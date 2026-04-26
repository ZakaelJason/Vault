package com.app.vault.marketplace

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.app.vault.marketplace.databinding.ActivityAddProductBinding
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class AddProductActivity : AppCompatActivity() {
    private lateinit var b: ActivityAddProductBinding
    private lateinit var db: DatabaseHelper
    private var editItemId: Int = -1
    private var selectedImageUri: Uri? = null
    private var currentImageUri: String = ""

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

        db = DatabaseHelper(this)
        editItemId = intent.getIntExtra("item_id", -1)

        val categories = arrayOf("Top Up", "Account", "Joki", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        b.etCategory.setAdapter(adapter)

        if (editItemId != -1) {
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
        val item = db.getItem(editItemId) ?: return
        b.etName.setText(item.name)
        b.etPrice.setText(item.price.toString())
        b.etDesc.setText(item.description)
        b.etCategory.setText(item.category, false)
        b.btnSubmit.text = "Update Product"
        
        currentImageUri = item.imageUri
        if (currentImageUri.isNotEmpty()) {
            val file = File(currentImageUri)
            if (file.exists()) {
                b.ivProduct.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
                b.tvAddPhoto.visibility = android.view.View.GONE
            }
        }
    }

    private fun saveProduct() {
        val name = b.etName.text.toString().trim()
        val priceStr = b.etPrice.text.toString().trim()
        val desc = b.etDesc.text.toString().trim()
        val cat = b.etCategory.text.toString().trim()

        if (name.isEmpty() || priceStr.isEmpty() || desc.isEmpty() || cat.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceStr.toDoubleOrNull() ?: 0.0
        
        // Save image to internal storage if new one selected
        val finalImageUri = if (selectedImageUri != null) {
            copyImageToInternal(selectedImageUri!!) ?: currentImageUri
        } else {
            currentImageUri
        }

        val session = SessionManager(this)
        if (editItemId != -1) {
            // In a real app, updateItem should include imageUri. Updating DatabaseHelper next.
            db.writableDatabase.update(DatabaseHelper.T_ITEMS, android.content.ContentValues().apply {
                put("name", name); put("price", price); put("description", desc)
                put("category", cat); put("image_uri", finalImageUri)
            }, "id=?", arrayOf(editItemId.toString()))
            Toast.makeText(this, "Product updated!", Toast.LENGTH_SHORT).show()
        } else {
            db.writableDatabase.insert(DatabaseHelper.T_ITEMS, null, android.content.ContentValues().apply {
                put("seller_id", session.getUserId()); put("name", name); put("price", price)
                put("description", desc); put("image_uri", finalImageUri); put("category", cat)
            })
            Toast.makeText(this, "Item listed!", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    private fun copyImageToInternal(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = "prod_${UUID.randomUUID()}.jpg"
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