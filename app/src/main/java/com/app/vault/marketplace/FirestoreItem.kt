package com.app.vault.marketplace

// Data class untuk produk yang dibaca dari Firestore
// Dipisah dari data class Item (SQLite) agar tidak ada konflik ID
data class FirestoreItem(
    val firestoreDocId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val description: String = "",
    val imageUri: String = "",
    val category: String = "Other",
    val sellerName: String = ""
)