package com.app.vault.marketplace

// --- Model terpusat untuk seluruh data yang sekarang hidup di Firestore ---
// Menggantikan model lama yang berasal dari SQLite (DatabaseHelper).

// Profil user, disimpan di koleksi "users", id dokumen = Firebase Auth uid
data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val description: String = "",
    val avatarUrl: String = ""
)

// Produk yang dijual, disimpan di koleksi "products"
data class FirestoreItem(
    val firestoreDocId: String = "",
    val sellerUid: String = "",
    val sellerName: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val description: String = "",
    val imageUrl: String = "",
    val category: String = "Other",
    val createdAt: Long = 0L
)

// Transaksi jual-beli, disimpan di koleksi "transactions"
data class Transaction(
    val firestoreDocId: String = "",
    val itemDocId: String = "",
    val itemName: String = "",
    val buyerUid: String = "",
    val buyerName: String = "",
    val sellerUid: String = "",
    val sellerName: String = "",
    val status: String = "Pending",
    val proofImageUrl: String = "",
    val paymentMethod: String = "",
    val createdAt: Long = 0L
)

// Komentar pada produk, disimpan sebagai subcollection "products/{docId}/comments"
data class Comment(
    val id: String = "",
    val userUid: String = "",
    val userName: String = "",
    val text: String = "",
    val reply: String? = null,
    val timestamp: Long = 0L
)

// Pesan chat, disimpan di "chats/{chatRoomId}/messages" (struktur lama dipertahankan)
data class ChatMessage(
    val text: String = "",
    val senderUsername: String = "",
    val timestamp: Long = 0L
)
