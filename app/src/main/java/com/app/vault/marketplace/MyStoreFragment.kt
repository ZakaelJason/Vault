package com.app.vault.marketplace

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.vault.marketplace.databinding.FragmentMyStoreBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class MyStoreFragment : Fragment() {
    private var _b: FragmentMyStoreBinding? = null
    private val b get() = _b!!
    private lateinit var session: SessionManager
    private val repo = FirebaseRepository()
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentMyStoreBinding.inflate(inf, c, false)
        return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)
        session = SessionManager(requireContext())
        b.rvMyStore.layoutManager = LinearLayoutManager(requireContext())
        startListener()

        b.fabAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AddProductActivity::class.java))
        }
    }

    private fun startListener() {
        val uid = repo.currentUser?.uid ?: return
        listenerRegistration = repo.firestore.collection("products")
            .whereEqualTo("sellerUid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (_b == null) return@addSnapshotListener
                if (error != null || snapshot == null) return@addSnapshotListener

                val items = snapshot.documents.map { repo.mapDocToItem(it) }
                b.rvMyStore.adapter = FirestoreItemAdapter(items) { item ->
                    showActionBottomSheet(item)
                }
            }
    }

    private fun showActionBottomSheet(item: FirestoreItem) {
        val dialog = BottomSheetDialog(requireContext())
        val view   = layoutInflater.inflate(R.layout.bottom_sheet_product_actions, null)

        view.findViewById<TextView>(R.id.tvTitle).text = item.name

        view.findViewById<View>(R.id.btnView).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(requireContext(), ItemDetailActivity::class.java).apply {
                putExtra("firestore_doc_id", item.firestoreDocId)
            })
        }

        view.findViewById<View>(R.id.btnEdit).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(requireContext(), AddProductActivity::class.java).apply {
                putExtra("firestore_doc_id", item.firestoreDocId)
            })
        }

        view.findViewById<View>(R.id.btnDelete).setOnClickListener {
            dialog.dismiss()
            showDeleteConfirmation(item)
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun showDeleteConfirmation(item: FirestoreItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hapus Produk")
            .setMessage("Hapus '${item.name}'? Tindakan ini tidak bisa dibatalkan.")
            .setPositiveButton("Hapus") { _, _ ->
                repo.deleteProduct(
                    docId = item.firestoreDocId,
                    onSuccess = {
                        if (_b != null) {
                            Toast.makeText(requireContext(), "Produk dihapus", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onError = { msg ->
                        if (_b != null) {
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
        listenerRegistration = null
        _b = null
    }
}