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

class MyStoreFragment : Fragment() {
    private var _b: FragmentMyStoreBinding? = null
    private val b get() = _b!!
    private lateinit var db: DatabaseHelper
    private lateinit var session: SessionManager

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentMyStoreBinding.inflate(inf, c, false)
        return b.root
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)
        db = DatabaseHelper(requireContext())
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
        val view = layoutInflater.inflate(R.layout.bottom_sheet_product_actions, null)
        
        view.findViewById<TextView>(R.id.tvTitle).text = item.name
        
        view.findViewById<View>(R.id.btnEdit).setOnClickListener {
            dialog.dismiss()
            val intent = Intent(requireContext(), AddProductActivity::class.java).apply {
                putExtra("item_id", item.id)
            }
            startActivity(intent)
        }
        
        view.findViewById<View>(R.id.btnDelete).setOnClickListener {
            dialog.dismiss()
            showDeleteConfirmation(item)
        }
        
        view.findViewById<View>(R.id.btnView).setOnClickListener {
            dialog.dismiss()
            Intent(requireContext(), ItemDetailActivity::class.java).also {
                it.putExtra("item_id", item.id)
                startActivity(it)
            }
        }
        
        dialog.setContentView(view)
        dialog.show()
    }

    private fun showDeleteConfirmation(item: Item) {
        val builder = AlertDialog.Builder(requireContext(), R.style.VaultDialog)
        val view = layoutInflater.inflate(R.layout.dialog_confirm_delete, null)
        
        view.findViewById<TextView>(R.id.tvMessage).text = "Are you sure you want to delete '${item.name}'? This action is permanent."
        
        val dialog = builder.setView(view).create()
        
        view.findViewById<View>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        view.findViewById<View>(R.id.btnConfirmDelete).setOnClickListener {
            db.deleteItem(item.id)
            loadItems()
            dialog.dismiss()
        }
        
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        loadItems()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}