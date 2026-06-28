package com.app.vault.marketplace

import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import java.util.UUID

/**
 * Pusat akses ke Firebase Auth, Firestore, dan Storage.
 * Menggantikan DatabaseHelper (SQLite) sepenuhnya — tidak ada lagi data
 * yang disimpan lokal di device, semua sumber kebenaran ada di cloud.
 */
class FirebaseRepository {

    val auth by lazy { Firebase.auth }
    val firestore by lazy { Firebase.firestore }
    val storage by lazy { Firebase.storage }

    val currentUser: FirebaseUser? get() = auth.currentUser
    val isLoggedIn: Boolean get() = auth.currentUser != null

    // --- AUTH ---

    /** Login memakai username: cari email yang terdaftar untuk username itu, lalu login via Firebase Auth. */
    fun loginWithUsername(
        username: String,
        password: String,
        onSuccess: (UserProfile) -> Unit,
        onError: (String) -> Unit
    ) {
        firestore.collection("users")
            .whereEqualTo("username", username)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val userDoc = snapshot.documents.firstOrNull()
                val email = userDoc?.getString("email")
                if (email.isNullOrEmpty()) {
                    onError("Username tidak ditemukan")
                    return@addOnSuccessListener
                }
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener { result ->
                        val uid = result.user?.uid ?: return@addOnSuccessListener onError("Login gagal")
                        getUserProfile(uid,
                            onSuccess = onSuccess,
                            onError = { onError("Login gagal") }
                        )
                    }
                    .addOnFailureListener { onError(it.message ?: "Username atau password salah") }
            }
            .addOnFailureListener { onError(it.message ?: "Login gagal") }
    }

    /** Register akun baru: buat user di Firebase Auth, lalu simpan profil di Firestore "users". */
    fun register(
        email: String,
        username: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Pastikan username belum dipakai sebelum membuat akun Auth
        firestore.collection("users")
            .whereEqualTo("username", username)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    onError("Username sudah dipakai")
                    return@addOnSuccessListener
                }
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { result ->
                        val uid = result.user?.uid ?: return@addOnSuccessListener onError("Registrasi gagal")
                        val profile = hashMapOf(
                            "username" to username,
                            "email" to email,
                            "description" to "",
                            "avatarUrl" to ""
                        )
                        firestore.collection("users").document(uid)
                            .set(profile)
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { onError(it.message ?: "Gagal menyimpan profil") }
                    }
                    .addOnFailureListener { onError(it.message ?: "Registrasi gagal") }
            }
            .addOnFailureListener { onError(it.message ?: "Registrasi gagal") }
    }

    fun logout() = auth.signOut()

    // --- USER PROFILE ---

    fun getUserProfile(uid: String, onSuccess: (UserProfile) -> Unit, onError: (String) -> Unit) {
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) { onError("Profil tidak ditemukan"); return@addOnSuccessListener }
                onSuccess(
                    UserProfile(
                        uid = doc.id,
                        username = doc.getString("username") ?: "",
                        email = doc.getString("email") ?: "",
                        description = doc.getString("description") ?: "",
                        avatarUrl = doc.getString("avatarUrl") ?: ""
                    )
                )
            }
            .addOnFailureListener { onError(it.message ?: "Gagal memuat profil") }
    }

    fun updateUserProfile(
        uid: String,
        username: String,
        description: String,
        avatarUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val values = hashMapOf<String, Any>(
            "username" to username,
            "description" to description,
            "avatarUrl" to avatarUrl
        )
        firestore.collection("users").document(uid)
            .set(values, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal menyimpan profil") }
    }

    /** Upload gambar (produk/avatar) ke Firebase Storage, mengembalikan download URL via callback. */
    fun uploadImage(
        uri: Uri,
        folder: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val fileName = "${folder}/${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child(fileName)
        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl
                    .addOnSuccessListener { url -> onSuccess(url.toString()) }
                    .addOnFailureListener { onError(it.message ?: "Gagal mengambil URL gambar") }
            }
            .addOnFailureListener { onError(it.message ?: "Gagal mengunggah gambar") }
    }

    // --- PRODUCTS ---

    fun addProduct(item: FirestoreItem, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val data = hashMapOf(
            "sellerUid" to item.sellerUid,
            "sellerName" to item.sellerName,
            "name" to item.name,
            "price" to item.price,
            "description" to item.description,
            "imageUrl" to item.imageUrl,
            "category" to item.category,
            "createdAt" to System.currentTimeMillis()
        )
        firestore.collection("products").add(data)
            .addOnSuccessListener { onSuccess(it.id) }
            .addOnFailureListener { onError(it.message ?: "Gagal menyimpan produk") }
    }

    fun updateProduct(
        docId: String,
        name: String,
        price: Double,
        description: String,
        category: String,
        imageUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf<String, Any>(
            "name" to name,
            "price" to price,
            "description" to description,
            "category" to category,
            "imageUrl" to imageUrl
        )
        firestore.collection("products").document(docId)
            .update(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal memperbarui produk") }
    }

    fun deleteProduct(docId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        firestore.collection("products").document(docId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal menghapus produk") }
    }

    fun getProduct(docId: String, onSuccess: (FirestoreItem) -> Unit, onError: (String) -> Unit) {
        firestore.collection("products").document(docId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) { onError("Produk tidak ditemukan"); return@addOnSuccessListener }
                onSuccess(mapDocToItem(doc))
            }
            .addOnFailureListener { onError(it.message ?: "Gagal memuat produk") }
    }

    fun mapDocToItem(doc: com.google.firebase.firestore.DocumentSnapshot): FirestoreItem {
        return FirestoreItem(
            firestoreDocId = doc.id,
            sellerUid = doc.getString("sellerUid") ?: "",
            sellerName = doc.getString("sellerName") ?: "",
            name = doc.getString("name") ?: "",
            price = doc.getDouble("price") ?: 0.0,
            description = doc.getString("description") ?: "",
            imageUrl = doc.getString("imageUrl") ?: "",
            category = doc.getString("category") ?: "Other",
            createdAt = doc.getLong("createdAt") ?: 0L
        )
    }

    // --- COMMENTS (subcollection of products) ---

    fun addComment(productDocId: String, userUid: String, userName: String, text: String,
                   onSuccess: () -> Unit, onError: (String) -> Unit) {
        val data = hashMapOf(
            "userUid" to userUid,
            "userName" to userName,
            "text" to text,
            "reply" to null,
            "timestamp" to System.currentTimeMillis()
        )
        firestore.collection("products").document(productDocId)
            .collection("comments").add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal mengirim komentar") }
    }

    fun addReply(productDocId: String, commentId: String, replyText: String,
                 onSuccess: () -> Unit, onError: (String) -> Unit) {
        firestore.collection("products").document(productDocId)
            .collection("comments").document(commentId)
            .update("reply", replyText)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal mengirim balasan") }
    }
}
