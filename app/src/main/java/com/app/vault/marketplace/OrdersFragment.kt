// OrdersFragment.kt
package com.app.vault.marketplace

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.vault.marketplace.databinding.FragmentOrdersBinding
import com.google.android.material.tabs.TabLayout

class OrdersFragment : Fragment() {
    private var _b: FragmentOrdersBinding? = null
    private val b get() = _b!!
    private var allTxns: List<Transaction> = emptyList()
    private var currentUserId: Int = -1

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentOrdersBinding.inflate(inf, c, false)
        return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)
        val session = SessionManager(requireContext())
        currentUserId = session.getUserId()
        
        b.rvOrders.layoutManager = LinearLayoutManager(requireContext())
        
        b.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                filterOrders(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        loadOrders()
    }

    override fun onResume() { 
        super.onResume()
        loadOrders() 
    }

    private fun loadOrders() {
        val db = DatabaseHelper(requireContext())
        allTxns = db.getUserTransactions(currentUserId)
        filterOrders(b.tabLayout.selectedTabPosition)
    }

    private fun filterOrders(position: Int) {
        val filtered = if (position == 0) {
            // My Purchase: I am the buyer
            allTxns.filter { it.buyerId == currentUserId }
        } else {
            // My Listing: I am the seller
            allTxns.filter { it.sellerId == currentUserId }
        }

        b.rvOrders.adapter = OrderAdapter(filtered, currentUserId) { txn ->
            Intent(requireContext(), ProofUploadActivity::class.java).also {
                it.putExtra("transaction_id", txn.id)
                startActivity(it)
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}