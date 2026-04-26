package com.app.vault.marketplace

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.vault.marketplace.databinding.ItemOrderBinding

class OrderAdapter(
    private val txns: List<Transaction>,
    private val currentUserId: Int,
    private val onAction: (Transaction) -> Unit
) : RecyclerView.Adapter<OrderAdapter.VH>() {

    inner class VH(val b: ItemOrderBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemOrderBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun getItemCount() = txns.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val txn = txns[pos]
        h.b.tvOrderItemName.text = txn.itemName

        h.b.tvOrderStatus.text = txn.status
        val statusColor = when (txn.status) {
            "Pending" -> Color.parseColor("#FFB300")
            "Proof Uploaded" -> Color.parseColor("#29B6F6")
            "Completed" -> Color.parseColor("#66BB6A")
            else -> Color.parseColor("#A0A0A0")
        }
        h.b.tvOrderStatus.setTextColor(statusColor)

        val isSeller = txn.sellerId == currentUserId
        h.b.tvOrderRole.text = if (isSeller) "Your listing → Buyer: ${txn.buyerName}" else "You bought from: ${txn.sellerName}"

        val showAction = (isSeller && txn.status == "Pending") || (!isSeller && txn.status == "Proof Uploaded")
        if (showAction) {
            h.b.btnAction.visibility = View.VISIBLE
            h.b.btnAction.text = if (isSeller) "Upload Proof" else "View Proof"
            h.b.btnAction.setOnClickListener { onAction(txn) }
        } else {
            h.b.btnAction.visibility = View.GONE
        }
    }
}