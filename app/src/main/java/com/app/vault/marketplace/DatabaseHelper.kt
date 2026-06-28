package com.app.vault.marketplace

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// --- Models ---
data class User(
    val id: Int,
    val username: String,
    val password: String,
    val email: String = "",
    val description: String = "",
    val avatarUri: String = ""
)

data class Item(
    val id: Int,
    val sellerId: Int,
    val name: String,
    val price: Double,
    val description: String,
    val imageUri: String,
    val category: String = "Other",
    val sellerName: String = ""
)

data class Transaction(
    val id: Int,
    val itemId: Int,
    val buyerId: Int,
    val sellerId: Int,
    val status: String,
    val proofImageUri: String,
    val paymentMethod: String,
    val itemName: String = "",
    val buyerName: String = "",
    val sellerName: String = "",
    val firestoreDocId: String = ""
)

data class Comment(
    val id: Int,
    val itemId: Int,
    val userId: Int,
    val userName: String,
    val text: String,
    val reply: String? = null,
    val timestamp: Long
)

// --- Database Helper ---
class DatabaseHelper(ctx: Context) : SQLiteOpenHelper(ctx, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "vault.db"
        private const val DATABASE_VERSION = 9 // Incremented version

        // Table Names
        const val T_USERS = "users"
        const val T_ITEMS = "items"
        const val T_TRANS = "transactions"
        const val T_COMMENTS = "comments"

        // Common Columns
        const val COL_ID = "id"

        // User Columns
        const val U_USERNAME = "username"
        const val U_PASSWORD = "password"
        const val U_EMAIL = "email"
        const val U_DESC = "description"
        const val U_AVATAR = "avatar_uri"

        // Item Columns
        const val I_SELLER_ID = "seller_id"
        const val I_NAME = "name"
        const val I_PRICE = "price"
        const val I_DESC = "description"
        const val I_IMAGE = "image_uri"
        const val I_CAT = "category"

        // Transaction Columns
        const val TR_ITEM_ID = "item_id"
        const val TR_BUYER_ID = "buyer_id"
        const val TR_SELLER_ID = "seller_id"
        const val TR_STATUS = "status"
        const val TR_PROOF = "proof_image_uri"
        const val TR_PAYMENT = "payment_method"

        // Comment Columns
        const val C_ITEM_ID = "item_id"
        const val C_USER_ID = "user_id"
        const val C_TEXT = "comment_text"
        const val C_REPLY = "reply_text"
        const val C_TIME = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $T_USERS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $U_USERNAME TEXT UNIQUE,
                $U_PASSWORD TEXT,
                $U_EMAIL TEXT,
                $U_DESC TEXT,
                $U_AVATAR TEXT
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $T_ITEMS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $I_SELLER_ID INTEGER,
                $I_NAME TEXT,
                $I_PRICE REAL,
                $I_DESC TEXT,
                $I_IMAGE TEXT,
                $I_CAT TEXT DEFAULT 'Other'
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $T_TRANS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $TR_ITEM_ID INTEGER,
                $TR_BUYER_ID INTEGER,
                $TR_SELLER_ID INTEGER,
                $TR_STATUS TEXT,
                $TR_PROOF TEXT,
                $TR_PAYMENT TEXT
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $T_COMMENTS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $C_ITEM_ID INTEGER,
                $C_USER_ID INTEGER,
                $C_TEXT TEXT,
                $C_REPLY TEXT,
                $C_TIME INTEGER
            )
        """.trimIndent())

        seedInitialData(db)
    }

    private fun seedInitialData(db: SQLiteDatabase) {
        // Seed users
        val users = listOf(
            Triple("alice", "pass123", "alice@gmail.com"),
            Triple("bob", "pass123", "bob@gmail.com")
        )
        users.forEach { (u, p, e) ->
            db.insert(T_USERS, null, ContentValues().apply {
                put(U_USERNAME, u); put(U_PASSWORD, p); put(U_EMAIL, e)
                put(U_DESC, "Hello, I am $u!"); put(U_AVATAR, "")
            })
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, o: Int, n: Int) {
        db.execSQL("DROP TABLE IF EXISTS $T_USERS")
        db.execSQL("DROP TABLE IF EXISTS $T_ITEMS")
        db.execSQL("DROP TABLE IF EXISTS $T_TRANS")
        db.execSQL("DROP TABLE IF EXISTS $T_COMMENTS")
        onCreate(db)
    }

    // --- USER CRUD ---

    fun login(username: String, password: String): User? {
        val query = "SELECT * FROM $T_USERS WHERE $U_USERNAME=? AND $U_PASSWORD=?"
        readableDatabase.rawQuery(query, arrayOf(username, password)).use { c ->
            return if (c.moveToFirst()) mapCursorToUser(c) else null
        }
    }

    fun register(email: String, username: String, password: String): Boolean {
        val values = ContentValues().apply {
            put(U_EMAIL, email); put(U_USERNAME, username); put(U_PASSWORD, password)
            put(U_DESC, ""); put(U_AVATAR, "")
        }
        return writableDatabase.insert(T_USERS, null, values) != -1L
    }

    fun getUser(userId: Int): User? {
        readableDatabase.rawQuery("SELECT * FROM $T_USERS WHERE $COL_ID=?", arrayOf(userId.toString())).use { c ->
            return if (c.moveToFirst()) mapCursorToUser(c) else null
        }
    }

    fun updateUser(userId: Int, username: String, email: String, desc: String, avatar: String): Int {
        val values = ContentValues().apply {
            put(U_USERNAME, username); put(U_EMAIL, email)
            put(U_DESC, desc); put(U_AVATAR, avatar)
        }
        return writableDatabase.update(T_USERS, values, "$COL_ID=?", arrayOf(userId.toString()))
    }

    // --- ITEM CRUD ---

    fun addItem(sellerId: Int, name: String, price: Double, desc: String, cat: String, image: String = ""): Long {
        val v = ContentValues().apply {
            put(I_SELLER_ID, sellerId); put(I_NAME, name); put(I_PRICE, price)
            put(I_DESC, desc); put(I_IMAGE, image); put(I_CAT, cat)
        }
        return writableDatabase.insert(T_ITEMS, null, v)
    }

    fun getItem(itemId: Int): Item? {
        val q = "SELECT i.*, u.$U_USERNAME FROM $T_ITEMS i JOIN $T_USERS u ON i.$I_SELLER_ID=u.$COL_ID WHERE i.$COL_ID=?"
        readableDatabase.rawQuery(q, arrayOf(itemId.toString())).use { c ->
            return if (c.moveToFirst()) mapCursorToItem(c) else null
        }
    }

    fun getMarketItems(currentUserId: Int, search: String? = null, category: String? = null): List<Item> {
        val list = mutableListOf<Item>()
        var query = "SELECT i.*, u.$U_USERNAME FROM $T_ITEMS i JOIN $T_USERS u ON i.$I_SELLER_ID=u.$COL_ID WHERE 1=1"
        val args = mutableListOf<String>()

        if (!search.isNullOrBlank()) {
            query += " AND i.$I_NAME LIKE ?"
            args.add("%$search%")
        }
        if (!category.isNullOrBlank() && category != "All") {
            query += " AND i.$I_CAT = ?"
            args.add(category)
        }

        readableDatabase.rawQuery(query, args.toTypedArray()).use { c ->
            while (c.moveToNext()) list.add(mapCursorToItem(c))
        }
        return list
    }

    fun getMyItems(userId: Int): List<Item> {
        val list = mutableListOf<Item>()
        val query = "SELECT i.*, u.$U_USERNAME FROM $T_ITEMS i JOIN $T_USERS u ON i.$I_SELLER_ID=u.$COL_ID WHERE i.$I_SELLER_ID=?"
        readableDatabase.rawQuery(query, arrayOf(userId.toString())).use { c ->
            while (c.moveToNext()) list.add(mapCursorToItem(c))
        }
        return list
    }

    fun updateItem(itemId: Int, name: String, price: Double, desc: String, cat: String): Int {
        val v = ContentValues().apply {
            put(I_NAME, name); put(I_PRICE, price); put(I_DESC, desc); put(I_CAT, cat)
        }
        return writableDatabase.update(T_ITEMS, v, "$COL_ID=?", arrayOf(itemId.toString()))
    }

    fun deleteItem(itemId: Int): Int {
        return writableDatabase.delete(T_ITEMS, "$COL_ID=?", arrayOf(itemId.toString()))
    }

    // --- TRANSACTION CRUD ---

    fun insertTransaction(itemId: Int, buyerId: Int, sellerId: Int, paymentMethod: String): Long {
        val v = ContentValues().apply {
            put(TR_ITEM_ID, itemId); put(TR_BUYER_ID, buyerId); put(TR_SELLER_ID, sellerId)
            put(TR_STATUS, "Pending"); put(TR_PROOF, ""); put(TR_PAYMENT, paymentMethod)
        }
        return writableDatabase.insert(T_TRANS, null, v)
    }

    fun getTransaction(txnId: Int): Transaction? {
        val q = """
            SELECT t.*, i.$I_NAME, ub.$U_USERNAME, us.$U_USERNAME FROM $T_TRANS t
            JOIN $T_ITEMS i ON t.$TR_ITEM_ID=i.$COL_ID
            JOIN $T_USERS ub ON t.$TR_BUYER_ID=ub.$COL_ID
            JOIN $T_USERS us ON t.$TR_SELLER_ID=us.$COL_ID
            WHERE t.$COL_ID=?
        """.trimIndent()
        readableDatabase.rawQuery(q, arrayOf(txnId.toString())).use { c ->
            return if (c.moveToFirst()) mapCursorToTransaction(c) else null
        }
    }

    fun getUserTransactions(userId: Int): List<Transaction> {
        val list = mutableListOf<Transaction>()
        val q = """
            SELECT t.*, i.$I_NAME, ub.$U_USERNAME, us.$U_USERNAME FROM $T_TRANS t
            JOIN $T_ITEMS i ON t.$TR_ITEM_ID=i.$COL_ID
            JOIN $T_USERS ub ON t.$TR_BUYER_ID=ub.$COL_ID
            JOIN $T_USERS us ON t.$TR_SELLER_ID=us.$COL_ID
            WHERE t.$TR_BUYER_ID=? OR t.$TR_SELLER_ID=?
        """.trimIndent()
        
        readableDatabase.rawQuery(q, arrayOf(userId.toString(), userId.toString())).use { c ->
            while (c.moveToNext()) list.add(mapCursorToTransaction(c))
        }
        return list
    }

    fun updateTransactionStatus(transId: Int, status: String, proofUri: String? = null): Int {
        val v = ContentValues().apply {
            put(TR_STATUS, status)
            proofUri?.let { put(TR_PROOF, it) }
        }
        return writableDatabase.update(T_TRANS, v, "$COL_ID=?", arrayOf(transId.toString()))
    }

    fun updateTransactionProof(txnId: Int, proofUri: String): Int {
        return updateTransactionStatus(txnId, "Proof Uploaded", proofUri)
    }

    fun completeTransaction(txnId: Int): Int {
        return updateTransactionStatus(txnId, "Completed")
    }

    fun deleteTransaction(transId: Int): Int {
        return writableDatabase.delete(T_TRANS, "$COL_ID=?", arrayOf(transId.toString()))
    }

    // --- COMMENT CRUD ---

    fun addComment(itemId: Int, userId: Int, text: String): Long {
        val v = ContentValues().apply {
            put(C_ITEM_ID, itemId); put(C_USER_ID, userId); put(C_TEXT, text)
            put(C_TIME, System.currentTimeMillis())
        }
        return writableDatabase.insert(T_COMMENTS, null, v)
    }

    fun getComments(itemId: Int): List<Comment> {
        val list = mutableListOf<Comment>()
        val q = "SELECT c.*, u.$U_USERNAME FROM $T_COMMENTS c JOIN $T_USERS u ON c.$C_USER_ID=u.$COL_ID WHERE c.$C_ITEM_ID=? ORDER BY c.$C_TIME DESC"
        readableDatabase.rawQuery(q, arrayOf(itemId.toString())).use { c ->
            while (c.moveToNext()) {
                list.add(Comment(c.getInt(0), c.getInt(1), c.getInt(2), c.getString(6), c.getString(3), c.getString(4), c.getLong(5)))
            }
        }
        return list
    }

    fun addReply(commentId: Int, replyText: String): Int {
        val v = ContentValues().apply { put(C_REPLY, replyText) }
        return writableDatabase.update(T_COMMENTS, v, "$COL_ID=?", arrayOf(commentId.toString()))
    }

    // --- Helpers untuk Mapping ---

    private fun mapCursorToUser(c: Cursor) = User(
        c.getInt(0), c.getString(1), c.getString(2), 
        c.getString(3) ?: "", c.getString(4) ?: "", c.getString(5) ?: ""
    )

    private fun mapCursorToItem(c: Cursor) = Item(
        c.getInt(0), c.getInt(1), c.getString(2), c.getDouble(3),
        c.getString(4), c.getString(5), c.getString(6), c.getString(7)
    )

    private fun mapCursorToTransaction(c: Cursor) = Transaction(
        c.getInt(0), c.getInt(1), c.getInt(2), c.getInt(3),
        c.getString(4), c.getString(5), c.getString(6),
        c.getString(7), c.getString(8), c.getString(9)
    )
}
