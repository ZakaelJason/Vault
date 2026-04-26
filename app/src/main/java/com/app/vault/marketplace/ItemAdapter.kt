package com.app.vault.marketplace

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.vault.marketplace.databinding.ItemProductBinding
import java.io.File
import java.text.NumberFormat
import java.util.Locale

class ItemAdapter(
    private val items: List<Item>,
    private val onClick: (Item) -> Unit
) : RecyclerView.Adapter<ItemAdapter.VH>() {

    private val fmt = NumberFormat.getNumberInstance(Locale("id", "ID"))

    inner class VH(val b: ItemProductBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemProductBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = items[pos]
        h.b.tvItemName.text = item.name
        h.b.tvItemDesc.text = item.description
        h.b.tvItemPrice.text = "Rp ${fmt.format(item.price)}"
        
        if (item.imageUri.isNotEmpty()) {
            val file = File(item.imageUri)
            if (file.exists()) {
                h.b.ivProduct.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
            } else {
                setProductPlaceholder(h, item.category)
            }
        } else {
            setProductPlaceholder(h, item.category)
        }

        h.b.root.setOnClickListener { onClick(item) }
    }

    private fun setProductPlaceholder(h: VH, category: String) {
        val resId = when (category) {
            "Joki" -> R.drawable.placeholder_joki
            "Top Up" -> R.drawable.placeholder_topup
            "Account" -> R.drawable.placeholder_account
            else -> R.drawable.placeholder_account
        }
        h.b.ivProduct.setImageResource(resId)
    }
}
