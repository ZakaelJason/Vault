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
        session = SessionManager(requireContext())

        b.tvWelcome.text = "Hello, ${session.getUsername()}!"
        b.rvMarket.layoutManager = GridLayoutManager(requireContext(), 2)

        val searchDbHelper = SearchDatabaseHelper(requireContext())
        val recentSearches = searchDbHelper.getRecentSearches()
        val searchAdapter = android.widget.ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            recentSearches
        )
        b.etSearch.setAdapter(searchAdapter)

        b.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearch = s.toString()
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        b.etSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val query = b.etSearch.text.toString().trim()
                if (query.isNotEmpty()) {
                    searchDbHelper.saveSearch(query)
                }
                // Hide keyboard
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else {
                false
            }
        }

        b.chipGroupFilter.setOnCheckedChangeListener { group, checkedId ->
            val chip = group.findViewById<Chip>(checkedId)
            currentCategory = chip?.text?.toString() ?: "All"
            applyFilters()
        }

        b.fabChatList.setOnClickListener {
            startActivity(Intent(requireContext(), ChatListActivity::class.java))
        }

        b.btnNews.setOnClickListener {
            startActivity(Intent(requireContext(), NewsActivity::class.java))
        }

        startFirestoreListener()
    }

    private fun startFirestoreListener() {
        val myUid = FirebaseRepository().currentUser?.uid ?: ""

        listenerRegistration = firestoreDb.collection("products")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (_b == null) return@addSnapshotListener
                if (error != null || snapshot == null) return@addSnapshotListener

                // Sembunyikan produk milik sendiri, filter berdasarkan sellerUid (stabil & unik)
                allItems = snapshot.documents.mapNotNull { doc ->
                    val sellerUid = doc.getString("sellerUid") ?: ""
                    if (sellerUid == myUid) return@mapNotNull null

                    FirestoreItem(
                        firestoreDocId = doc.id,
                        sellerUid   = sellerUid,
                        sellerName  = doc.getString("sellerName") ?: "",
                        name        = doc.getString("name") ?: "",
                        price       = doc.getDouble("price") ?: 0.0,
                        description = doc.getString("description") ?: "",
                        imageUrl    = doc.getString("imageUrl") ?: "",
                        category    = doc.getString("category") ?: "Other",
                        createdAt   = doc.getLong("createdAt") ?: 0L
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