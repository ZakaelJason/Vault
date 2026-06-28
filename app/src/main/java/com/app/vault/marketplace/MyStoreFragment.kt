package com.app.vault.marketplace

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.vault.marketplace.databinding.FragmentMyStoreBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class MyStoreFragment : Fragment() {
    private var _b: FragmentMyStoreBinding? = null
    private val b get() = _b!!
    private lateinit var db: DatabaseHelper
    private lateinit var session: SessionManager
    private val firestoreDb by lazy { Firebase.firestore }

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentMyStoreBinding.inflate(inf, c, false)
        return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)
        db      = DatabaseHelper(requireContext())
        session = SessionManager(requireContext())
        b.rvMyStore.layoutManager = LinearLayoutManager(requireContext())
        loadItems()

        b.fabAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AddProductActivity::class.java))
        }
    }

    private fun loadItems() {
        val items = db.getMyItems(session.getUserId())
        b.rvMyStore.adapter = ItemAdapter(items) { item ->
            showActionBottomSheet(item)
        }
    }

    private fun showActionBottomSheet(item: Item) {
        val dialog = BottomSheetDialog(requireContext())
        val view   = layoutInflater.inflate(R.layout.bottom_sheet_product_actions, null)

        view.findViewById<TextView>(R.id.tvTitle).text = item.name

        view.findViewById<View>(R.id.btnView).setOnClickListener {
            dialog.dismiss()
            // Cari firestoreDocId produk milik sendiri lalu buka detail
            loadFirestoreDocId(item.id) { docId ->
                if (docId.isNotEmpty()) {
                    startActivity(Intent(requireContext(), ItemDetailActivity::class.java).apply {
                        putExtra("firestore_doc_id", docId)
                    })
                }
            }
        }

        view.findViewById<View>(R.id.btnEdit).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(requireContext(), AddProductActivity::class.java).apply {
                putExtra("item_id", item.id)
            })
        }

        view.findViewById<View>(R.id.btnDelete).setOnClickListener {
            dialog.dismiss()
            showDeleteConfirmation(item)
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun loadFirestoreDocId(localId: Int, callback: (String) -> Unit) {
        val myUsername = session.getUsername()
        // FIX: cari di Firestore berdasarkan sellerName + localId — unik per user
        firestoreDb.collection("products")
            .whereEqualTo("sellerName", myUsername)
            .whereEqualTo("localId", localId.toLong())
            .get()
            .addOnSuccessListener { snapshot ->
                val docId = snapshot.documents.firstOrNull()?.id ?: ""
                callback(docId)
            }
            .addOnFailureListener { callback("") }
    }

    private fun showDeleteConfirmation(item: Item) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Produk")
            .setMessage("Hapus '${item.name}'? Tindakan ini tidak bisa dibatalkan.")
            .setPositiveButton("Hapus") { _, _ ->
                // 1. Hapus dari SQLite lokal
                db.deleteItem(item.id)

                // FIX: hapus dari Firestore berdasarkan sellerName + localId (bukan hanya localId)
                // agar tidak menghapus produk device lain yang kebetulan punya localId sama
                val myUsername = session.getUsername()
                firestoreDb.collection("products")
                    .whereEqualTo("sellerName", myUsername)
                    .whereEqualTo("localId", item.id.toLong())
                    .get()
                    .addOnSuccessListener { snapshot ->
                        snapshot.documents.forEach { it.reference.delete() }
                    }

                loadItems()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadItems()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}