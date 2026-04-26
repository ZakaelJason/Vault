package com.app.vault.marketplace

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.vault.marketplace.databinding.ItemCommentBinding

class CommentAdapter(
    private val comments: List<Comment>,
    private val currentUserId: Int,
    private val sellerId: Int,
    private val onReplyClick: (Comment) -> Unit
) : RecyclerView.Adapter<CommentAdapter.VH>() {

    inner class VH(val b: ItemCommentBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val comment = comments[position]
        holder.b.tvUserName.text = comment.userName
        holder.b.tvCommentText.text = comment.text

        if (!comment.reply.isNullOrEmpty()) {
            holder.b.layoutReply.visibility = View.VISIBLE
            holder.b.tvReplyText.text = comment.reply
        } else {
            holder.b.layoutReply.visibility = View.GONE
        }

        if (currentUserId == sellerId && comment.reply.isNullOrEmpty()) {
            holder.b.btnReply.visibility = View.VISIBLE
            holder.b.btnReply.setOnClickListener { onReplyClick(comment) }
        } else {
            holder.b.btnReply.visibility = View.GONE
        }
    }

    override fun getItemCount() = comments.size
}