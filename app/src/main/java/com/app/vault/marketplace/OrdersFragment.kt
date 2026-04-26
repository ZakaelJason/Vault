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

class OrdersFragment : Fragment() {
    private var _b: FragmentOrdersBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentOrdersBinding.inflate(inf, c, false)
        return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)
        loadOrders()
    }

    override fun onResume() { super.onResume(); loadOrders() }

    private fun loadOrders() {
        val session = SessionManager(requireContext())
        val db = DatabaseHelper(requireContext())
        val txns = db.getUserTransactions(session.getUserId())
        b.rvOrders.layoutManager = LinearLayoutManager(requireContext())
        b.rvOrders.adapter = OrderAdapter(txns, session.getUserId()) { txn ->
            Intent(requireContext(), ProofUploadActivity::class.java).also {
                it.putExtra("transaction_id", txn.id)
                startActivity(it)
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}