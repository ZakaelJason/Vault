package com.app.vault.marketplace

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.vault.marketplace.databinding.FragmentMyStoreBinding

class MyStoreFragment : Fragment() {
    private var _b: FragmentMyStoreBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentMyStoreBinding.inflate(inf, c, false)
        return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)
        val session = SessionManager(requireContext())
        val db = DatabaseHelper(requireContext())
        val items = db.getMyItems(session.getUserId())

        b.rvMyStore.layoutManager = LinearLayoutManager(requireContext())
        b.rvMyStore.adapter = ItemAdapter(items) { item ->
            Intent(requireContext(), ItemDetailActivity::class.java).also {
                it.putExtra("item_id", item.id)
                startActivity(it)
            }
        }

        b.fabAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AddProductActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        val session = SessionManager(requireContext())
        val db = DatabaseHelper(requireContext())
        b.rvMyStore.adapter = ItemAdapter(db.getMyItems(session.getUserId())) { item ->
            Intent(requireContext(), ItemDetailActivity::class.java).also {
                it.putExtra("item_id", item.id); startActivity(it)
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}