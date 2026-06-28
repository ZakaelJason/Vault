package com.app.vault.marketplace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.vault.marketplace.databinding.ActivityChatBinding
import com.google.firebase.Firebase
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore

data class ChatMessage(
    val text:       String  = "",
    val senderUsername: String = "",
    val timestamp:  Long    = 0L
)

class ChatActivity : AppCompatActivity() {
    private lateinit var b: ActivityChatBinding
    private val firestoreDb by lazy { Firebase.firestore }
    private var listenerRegistration: ListenerRegistration? = null
    private lateinit var myUsername: String
    private lateinit var chatRoomId: String

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityChatBinding.inflate(layoutInflater)
        setContentView(b.root)

        val sellerUsername = intent.getStringExtra("seller_username") ?: ""
        val buyerUsername  = intent.getStringExtra("buyer_username")  ?: ""
        val itemName       = intent.getStringExtra("item_name")       ?: ""

        myUsername = SessionManager(this).getUsername()

        // ID room chat: kombinasi seller+buyer+item agar unik dan konsisten
        chatRoomId = listOf(sellerUsername, buyerUsername, itemName)
            .sorted()
            .joinToString("_")
            .replace(" ", "-")

        b.tvChatTitle.text  = "Chat — $itemName"
        b.tvChatSubtitle.text = if (myUsername == sellerUsername)
            "dengan $buyerUsername" else "dengan $sellerUsername"

        b.btnBack.setOnClickListener { finish() }

        b.rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true  // scroll otomatis ke pesan terbaru
        }

        startChatListener()

        b.btnSend.setOnClickListener {
            val text = b.etMessage.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            val message = hashMapOf(
                "text"            to text,
                "senderUsername"  to myUsername,
                "timestamp"       to System.currentTimeMillis()
            )

            firestoreDb.collection("chats")
                .document(chatRoomId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener {
                    b.etMessage.setText("")
                }
        }
    }

    private fun startChatListener() {
        listenerRegistration = firestoreDb
            .collection("chats")
            .document(chatRoomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val messages = snapshot.documents.mapNotNull { doc ->
                    try {
                        ChatMessage(
                            text            = doc.getString("text") ?: "",
                            senderUsername  = doc.getString("senderUsername") ?: "",
                            timestamp       = doc.getLong("timestamp") ?: 0L
                        )
                    } catch (e: Exception) { null }
                }

                b.rvMessages.adapter = ChatAdapter(messages, myUsername)
                // Scroll ke pesan terbaru
                if (messages.isNotEmpty()) {
                    b.rvMessages.scrollToPosition(messages.size - 1)
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }
}

// Adapter untuk RecyclerView pesan chat
class ChatAdapter(
    private val messages: List<ChatMessage>,
    private val myUsername: String
) : RecyclerView.Adapter<ChatAdapter.VH>() {

    companion object {
        private const val VIEW_SENT     = 1
        private const val VIEW_RECEIVED = 2
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMessage: TextView  = itemView.findViewById(R.id.tvChatMessage)
        val tvSender: TextView   = itemView.findViewById(R.id.tvChatSender)
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderUsername == myUsername) VIEW_SENT else VIEW_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val layout = if (viewType == VIEW_SENT)
            R.layout.item_chat_sent else R.layout.item_chat_received
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val msg = messages[pos]
        h.tvMessage.text = msg.text
        h.tvSender.text  = msg.senderUsername
    }

    override fun getItemCount() = messages.size
}