package com.app.vault.marketplace

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.vault.marketplace.databinding.ItemProductBinding
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.util.Locale

// Adapter untuk menampilkan produk dari Firestore, dipakai di MarketFragment & MyStoreFragment
class FirestoreItemAdapter(
    private val items: List<FirestoreItem>,
    private val onClick: (FirestoreItem) -> Unit
) : RecyclerView.Adapter<FirestoreItemAdapter.VH>() {

    private val fmt = NumberFormat.getNumberInstance(Locale("id", "ID"))

    inner class VH(val b: ItemProductBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemProductBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = items[pos]
        h.b.tvItemName.text  = item.name
        h.b.tvItemDesc.text  = item.description
        h.b.tvItemPrice.text = "Rp ${fmt.format(item.price)}"

        val placeholderRes = when (item.category) {
            "Joki"    -> R.drawable.placeholder_joki
            "Top Up"  -> R.drawable.placeholder_topup
            "Account" -> R.drawable.placeholder_account
            else      -> R.drawable.placeholder_account
        }

        if (item.imageUrl.isNotEmpty()) {
            Glide.with(h.b.ivProduct)
                .load(item.imageUrl)
                .placeholder(placeholderRes)
                .error(placeholderRes)
                .into(h.b.ivProduct)
        } else {
            h.b.ivProduct.setImageResource(placeholderRes)
        }

        h.b.root.setOnClickListener { onClick(item) }
    }
}
