package com.app.vault.marketplace

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.vault.marketplace.databinding.FragmentMarketBinding
import com.google.android.material.chip.Chip

class MarketFragment : Fragment() {
    private var _b: FragmentMarketBinding? = null
    private val b get() = _b!!
    private lateinit var db: DatabaseHelper
    private lateinit var session: SessionManager
    private var currentCategory: String = "All"
    private var currentSearch: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentMarketBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DatabaseHelper(requireContext())
        session = SessionManager(requireContext())

        b.tvWelcome.text = "Hello, ${session.getUsername()}!"

        b.rvMarket.layoutManager = LinearLayoutManager(requireContext())
        updateList()

        b.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearch = s.toString()
                updateList()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        b.chipGroupFilter.setOnCheckedChangeListener { group, checkedId ->
            val chip = group.findViewById<Chip>(checkedId)
            currentCategory = chip?.text?.toString() ?: "All"
            updateList()
        }
    }

    private fun updateList() {
        val items = db.getMarketItems(session.getUserId(), currentSearch, currentCategory)
        b.rvMarket.adapter = ItemAdapter(items) { item ->
            Intent(requireContext(), ItemDetailActivity::class.java).also {
                it.putExtra("item_id", item.id)
                startActivity(it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}