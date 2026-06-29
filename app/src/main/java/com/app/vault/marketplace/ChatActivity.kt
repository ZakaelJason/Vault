package com.app.vault.marketplace

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.vault.marketplace.databinding.ActivityChatBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {
    private lateinit var b: ActivityChatBinding
    private val firestoreDb by lazy { Firebase.firestore }
    private var listenerRegistration: ListenerRegistration? = null
    private lateinit var myUsername: String
    private lateinit var chatRoomId: String
    
    private var recipientUid: String = ""
    private var itemName: String = ""

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityChatBinding.inflate(layoutInflater)
        setContentView(b.root)

        val sellerUid      = intent.getStringExtra("seller_uid")      ?: ""
        val sellerUsername = intent.getStringExtra("seller_username") ?: ""
        val buyerUid       = intent.getStringExtra("buyer_uid")       ?: ""
        val buyerUsername  = intent.getStringExtra("buyer_username")  ?: ""
        itemName           = intent.getStringExtra("item_name")       ?: ""

        val myUid = Firebase.auth.currentUser?.uid ?: ""
        myUsername = SessionManager(this).getUsername()

        recipientUid = if (myUid == sellerUid) buyerUid else sellerUid
        
        chatRoomId = listOf(sellerUsername, buyerUsername, itemName)
            .sorted()
            .joinToString("_")
            .replace(" ", "-")

        Log.d("VaultChat", "Room: $chatRoomId | Recipient: $recipientUid")

        shareTokenInRoom(myUid)

        b.tvChatTitle.text  = "Chat — $itemName"
        b.tvChatSubtitle.text = if (myUsername == sellerUsername)
            "dengan $buyerUsername" else "dengan $sellerUsername"

        b.btnBack.setOnClickListener { finish() }
        b.rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }

        startChatListener()

        b.btnSend.setOnClickListener {
            val text = b.etMessage.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            val message = hashMapOf(
                "text"            to text,
                "senderUsername"  to myUsername,
                "timestamp"       to System.currentTimeMillis()
            )

            firestoreDb.collection("chats").document(chatRoomId)
                .collection("messages").add(message)
                .addOnSuccessListener {
                    b.etMessage.setText("")
                    sendPushNotification(text)
                }
        }
    }

    private fun shareTokenInRoom(myUid: String) {
        if (myUid.isEmpty()) return
        Firebase.messaging.token.addOnSuccessListener { token ->
            val data = mapOf("tokens" to mapOf(myUid to token))
            firestoreDb.collection("chats").document(chatRoomId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener { Log.d("VaultChat", "Token shared in room doc") }
        }
    }

    private fun sendPushNotification(messageText: String) {
        if (recipientUid.isEmpty()) return

        firestoreDb.collection("chats").document(chatRoomId).get()
            .addOnSuccessListener { doc ->
                val tokens = doc.get("tokens") as? Map<*, *>
                val token = tokens?.get(recipientUid) as? String
                
                if (!token.isNullOrEmpty()) {
                    Log.d("VaultChat", "Token found in room sharing")
                    executePush(token, messageText)
                } else {
                    fetchTokenFromUsers(messageText)
                }
            }
            .addOnFailureListener { fetchTokenFromUsers(messageText) }
    }

    private fun fetchTokenFromUsers(messageText: String) {
        firestoreDb.collection("users").document(recipientUid).get()
            .addOnSuccessListener { doc ->
                val token = doc.getString("fcmToken")
                if (!token.isNullOrEmpty()) {
                    executePush(token, messageText)
                } else {
                    Log.w("VaultChat", "Recipient token not found in users collection")
                }
            }
            .addOnFailureListener { e ->
                Log.e("VaultChat", "Failed to fetch recipient token: ${e.message}")
            }
    }

    private fun executePush(token: String, messageText: String) {
        lifecycleScope.launch {
            val success = FcmSender.sendToToken(
                context = this@ChatActivity,
                targetToken = token,
                title = "Pesan dari $myUsername",
                body = messageText,
                type = "chat"
            )
            if (success) {
                Log.d("VaultChat", "Notifikasi berhasil dikirim ke server FCM")
            } else {
                Log.e("VaultChat", "Gagal mengirim notifikasi. Cek res/raw/service_account.json")
            }
        }
    }

    private fun startChatListener() {
        listenerRegistration = firestoreDb.collection("chats").document(chatRoomId)
            .collection("messages").orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("VaultChat", "Listener error: ${error.message}")
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        ChatMessage(
                            text            = doc.getString("text") ?: "",
                            senderUsername  = doc.getString("senderUsername") ?: "",
                            timestamp       = doc.getLong("timestamp") ?: 0L
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList()

                b.rvMessages.adapter = ChatAdapter(messages, myUsername)
                if (messages.isNotEmpty()) b.rvMessages.scrollToPosition(messages.size - 1)
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }
}

class ChatAdapter(private val messages: List<ChatMessage>, private val myUsername: String) : RecyclerView.Adapter<ChatAdapter.VH>() {
    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvMessage: TextView = v.findViewById(R.id.tvChatMessage)
        val tvSender: TextView = v.findViewById(R.id.tvChatSender)
    }
    override fun getItemViewType(pos: Int) = if (messages[pos].senderUsername == myUsername) 1 else 2
    override fun onCreateViewHolder(p: ViewGroup, vt: Int): VH {
        val layout = if (vt == 1) R.layout.item_chat_sent else R.layout.item_chat_received
        return VH(LayoutInflater.from(p.context).inflate(layout, p, false))
    }
    override fun onBindViewHolder(h: VH, pos: Int) {
        val msg = messages[pos]
        h.tvMessage.text = msg.text
        if (getItemViewType(pos) == 1) h.tvSender.visibility = View.GONE
        else { h.tvSender.visibility = View.VISIBLE; h.tvSender.text = msg.senderUsername }
    }
    override fun getItemCount() = messages.size
}