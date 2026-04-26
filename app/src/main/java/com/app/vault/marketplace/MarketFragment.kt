// MarketFragment.kt
package com.app.vault.marketplace

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.vault.marketplace.databinding.FragmentMarketBinding

class MarketFragment : Fragment() {
    private var _b: FragmentMarketBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentMarketBinding.inflate(inf, c, false)
        return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)
        val session = SessionManager(requireContext())
        val db = DatabaseHelper(requireContext())
        val items = db.getMarketItems(session.getUserId())

        b.rvMarket.layoutManager = LinearLayoutManager(requireContext())
        b.rvMarket.adapter = ItemAdapter(items) { item ->
            Intent(requireContext(), ItemDetailActivity::class.java).also {
                it.putExtra("item_id", item.id)
                startActivity(it)
            }
        }

        b.ivCallSupport.setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:1500123")))
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}