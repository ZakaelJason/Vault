package com.app.vault.marketplace

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.vault.marketplace.databinding.FragmentOrdersBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.firebase.Firebase
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore

class OrdersFragment : Fragment() {
    private var _b: FragmentOrdersBinding? = null
    private val b get() = _b!!
    private lateinit var session: SessionManager
    private val firestoreDb by lazy { Firebase.firestore }
    private var buyerListener: ListenerRegistration? = null
    private var sellerListener: ListenerRegistration? = null

    private var allTxns: List<Transaction> = emptyList()
    private var currentUid: String = ""
    private var currentUsername: String = ""

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentOrdersBinding.inflate(inf, c, false)
        return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)
        session = SessionManager(requireContext())
        currentUid = session.getUid()
        currentUsername = session.getUsername()

        b.rvOrders.layoutManager = LinearLayoutManager(requireContext())

        b.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) { filterOrders(tab?.position ?: 0) }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        startFirestoreListener()
    }

    private fun startFirestoreListener() {
        if (currentUid.isEmpty()) return

        var buyerTxns:  List<Transaction> = emptyList()
        var sellerTxns: List<Transaction> = emptyList()

        buyerListener = firestoreDb.collection("transactions")
            .whereEqualTo("buyerUid", currentUid)
            .addSnapshotListener { snapshot, _ ->
                if (_b == null) return@addSnapshotListener
                buyerTxns = snapshot?.documents?.mapNotNull { mapDocToTransaction(it) } ?: emptyList()
                allTxns = (buyerTxns + sellerTxns).distinctBy { it.firestoreDocId }
                filterOrders(b.tabLayout.selectedTabPosition)
            }

        sellerListener = firestoreDb.collection("transactions")
            .whereEqualTo("sellerUid", currentUid)
            .addSnapshotListener { snapshot, _ ->
                if (_b == null) return@addSnapshotListener
                sellerTxns = snapshot?.documents?.mapNotNull { mapDocToTransaction(it) } ?: emptyList()
                allTxns = (buyerTxns + sellerTxns).distinctBy { it.firestoreDocId }
                filterOrders(b.tabLayout.selectedTabPosition)
            }
    }

    private fun mapDocToTransaction(
        doc: com.google.firebase.firestore.DocumentSnapshot
    ): Transaction? {
        return try {
            Transaction(
                firestoreDocId = doc.id,
                itemDocId      = doc.getString("itemDocId") ?: "",
                itemName       = doc.getString("itemName") ?: "",
                buyerUid       = doc.getString("buyerUid") ?: "",
                buyerName      = doc.getString("buyerUsername") ?: "",
                sellerUid      = doc.getString("sellerUid") ?: "",
                sellerName     = doc.getString("sellerUsername") ?: "",
                status         = doc.getString("status") ?: "Pending",
                proofImageUrl  = doc.getString("proofImageUrl") ?: "",
                paymentMethod  = doc.getString("paymentMethod") ?: "",
                createdAt      = doc.getLong("createdAt") ?: 0L
            )
        } catch (e: Exception) { null }
    }

    private fun filterOrders(position: Int) {
        if (_b == null) return
        val filtered = if (position == 0) {
            allTxns.filter { it.buyerUid == currentUid }
        } else {
            allTxns.filter { it.sellerUid == currentUid }
        }

        b.rvOrders.adapter = OrderAdapter(
            filtered,
            currentUsername,
            onAction = { txn ->
                Intent(requireContext(), ProofUploadActivity::class.java).also {
                    it.putExtra("firestore_doc_id", txn.firestoreDocId)
                    startActivity(it)
                }
            },
            onDelete = { txn -> showDeleteConfirmation(txn) },
            onChat   = { txn ->
                Intent(requireContext(), ChatActivity::class.java).also {
                    it.putExtra("seller_uid",      txn.sellerUid)
                    it.putExtra("seller_username", txn.sellerName)
                    it.putExtra("buyer_uid",       txn.buyerUid)
                    it.putExtra("buyer_username",  txn.buyerName)
                    it.putExtra("item_name",       txn.itemName)
                    startActivity(it)
                }
            }
        )
    }

    private fun showDeleteConfirmation(txn: Transaction) {
        val isCancellable = txn.status == "Pending"
        val title = if (isCancellable) "Batalkan Pesanan" else "Hapus dari Riwayat"
        val message = if (isCancellable)
            "Batalkan pesanan '${txn.itemName}'? Status akan ditandai gagal."
        else
            "Hapus pesanan '${txn.itemName}' dari riwayat?"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(if (isCancellable) "Batalkan" else "Hapus") { _, _ ->
                if (isCancellable) {
                    // Tandai gagal dulu, supaya listener notifikasi sempat mendeteksi
                    firestoreDb.collection("transactions")
                        .document(txn.firestoreDocId)
                        .update("status", "Cancelled")
                        .addOnFailureListener {
                            if (_b != null) Toast.makeText(requireContext(), "Gagal membatalkan", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    firestoreDb.collection("transactions")
                        .document(txn.firestoreDocId)
                        .delete()
                        .addOnSuccessListener {
                            if (_b != null) Toast.makeText(requireContext(), "Pesanan dihapus", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            if (_b != null) Toast.makeText(requireContext(), "Gagal menghapus pesanan", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        buyerListener?.remove()
        sellerListener?.remove()
        _b = null
    }
}