package com.app.vault.marketplace

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.app.vault.marketplace.databinding.FragmentMarketBinding
import com.google.android.material.chip.Chip
import com.google.firebase.Firebase
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore

class MarketFragment : Fragment() {
    private var _b: FragmentMarketBinding? = null
    private val b get() = _b!!
    private lateinit var db: DatabaseHelper
    private lateinit var session: SessionManager
    private val firestoreDb by lazy { Firebase.firestore }
    private var listenerRegistration: ListenerRegistration? = null
    private var currentCategory: String = "All"
    private var currentSearch: String = ""
    private var allItems: List<FirestoreItem> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentMarketBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db      = DatabaseHelper(requireContext())
        session = SessionManager(requireContext())

        b.tvWelcome.text = "Hello, ${session.getUsername()}!"
        b.rvMarket.layoutManager = GridLayoutManager(requireContext(), 2)

        b.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearch = s.toString()
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        b.chipGroupFilter.setOnCheckedChangeListener { group, checkedId ->
            val chip = group.findViewById<Chip>(checkedId)
            currentCategory = chip?.text?.toString() ?: "All"
            applyFilters()
        }

        startFirestoreListener()
    }

    private fun startFirestoreListener() {
        val myUsername = session.getUsername()

        listenerRegistration = firestoreDb.collection("products")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (_b == null) return@addSnapshotListener
                if (error != null || snapshot == null) return@addSnapshotListener

                // FIX: filter berdasarkan sellerName (username), bukan sellerId integer
                allItems = snapshot.documents.mapNotNull { doc ->
                    val sellerName = doc.getString("sellerName") ?: ""
                    // Sembunyikan produk milik sendiri
                    if (sellerName == myUsername) return@mapNotNull null

                    FirestoreItem(
                        firestoreDocId = doc.id,
                        name        = doc.getString("name") ?: "",
                        price       = doc.getDouble("price") ?: 0.0,
                        description = doc.getString("description") ?: "",
                        imageUri    = doc.getString("imageUri") ?: "",
                        category    = doc.getString("category") ?: "Other",
                        sellerName  = sellerName
                    )
                }
                applyFilters()
            }
    }

    private fun applyFilters() {
        if (_b == null) return
        val filtered = allItems.filter { item ->
            val matchSearch   = currentSearch.isBlank() ||
                    item.name.contains(currentSearch, ignoreCase = true)
            val matchCategory = currentCategory == "All" ||
                    item.category.equals(currentCategory, ignoreCase = true)
            matchSearch && matchCategory
        }

        // FIX: kirim firestoreDocId ke ItemDetailActivity, bukan localId
        b.rvMarket.adapter = FirestoreItemAdapter(filtered) { item ->
            Intent(requireContext(), ItemDetailActivity::class.java).also {
                it.putExtra("firestore_doc_id", item.firestoreDocId)
                startActivity(it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
        listenerRegistration = null
        _b = null
    }
}