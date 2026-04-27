package com.app.vault.marketplace

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

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
    val sellerName: String = ""
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

class DatabaseHelper(ctx: Context) : SQLiteOpenHelper(ctx, "vault.db", null, 8) {

    companion object {
        const val T_USERS = "users"
        const val T_ITEMS = "items"
        const val T_TRANS = "transactions"
        const val T_COMMENTS = "comments"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $T_USERS(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username TEXT UNIQUE, " +
                    "password TEXT, " +
                    "email TEXT, " +
                    "description TEXT, " +
                    "avatar_uri TEXT)"
        )
        db.execSQL(
            "CREATE TABLE $T_ITEMS(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "seller_id INTEGER, " +
                    "name TEXT, " +
                    "price REAL, " +
                    "description TEXT, " +
                    "image_uri TEXT, " +
                    "category TEXT DEFAULT 'Other')"
        )
        db.execSQL(
            "CREATE TABLE $T_TRANS(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "item_id INTEGER, " +
                    "buyer_id INTEGER, " +
                    "seller_id INTEGER, " +
                    "status TEXT, " +
                    "proof_image_uri TEXT,"+
                    "payment_method TEXT)"
        )
        db.execSQL(
            "CREATE TABLE $T_COMMENTS(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "item_id INTEGER, " +
                    "user_id INTEGER, " +
                    "comment_text TEXT, " +
                    "reply_text TEXT, " +
                    "timestamp INTEGER)"
        )

        // Seed users
        listOf(
            Triple("alice", "pass123", "alice@gmail.com"),
            Triple("bob", "pass123", "bob@gmail.com"),
            Triple("charlie", "pass123", "charlie@gmail.com")
        ).forEach { (u, p, e) ->
            db.insert(T_USERS, null, ContentValues().apply {
                put("username", u)
                put("password", p)
                put("email", e)
                put("description", "Hello, I am $u!")
                put("avatar_uri", "")
            })
        }

        // Seed items
        data class ItemSeed(val sid: Int, val name: String, val price: Double, val desc: String, val cat: String)
        val items = listOf(
            ItemSeed(1, "Mobile Legends Account", 350000.0, "High rank account with many skins.", "Account"),
            ItemSeed(1, "Free Fire Bundle Package", 120000.0, "Exclusive bundle package.", "Top Up"),
            ItemSeed(2, "PUBG Mobile UC 3000", 450000.0, "Direct top-up UC.", "Top Up"),
            ItemSeed(2, "Valorant Points 4300", 800000.0, "Direct top-up VP.", "Top Up"),
            ItemSeed(3, "Genshin Impact Welkin x5", 275000.0, "Monthly card subscription.", "Top Up"),
            ItemSeed(3, "Roblox Robux 10000", 220000.0, "Direct top-up Robux.", "Top Up"),
            ItemSeed(1, "Rank Boosting Mythic", 500000.0, "Fast boosting service.", "Joki")
        )
        items.forEach { (sid, name, price, desc, cat) ->
            db.insert(T_ITEMS, null, ContentValues().apply {
                put("seller_id", sid); put("name", name); put("price", price)
                put("description", desc); put("image_uri", ""); put("category", cat)
            })
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, o: Int, n: Int) {
        db.execSQL("DROP TABLE IF EXISTS $T_USERS")
        db.execSQL("DROP TABLE IF EXISTS $T_ITEMS")
        db.execSQL("DROP TABLE IF EXISTS $T_TRANS")
        db.execSQL("DROP TABLE IF EXISTS $T_COMMENTS")
        db.execSQL("DROP TABLE IF EXISTS chats")
        onCreate(db)
    }

    fun login(username: String, password: String): User? {
        val c = readableDatabase.rawQuery(
            "SELECT * FROM $T_USERS WHERE username=? AND password=?", arrayOf(username, password)
        )
        return if (c.moveToFirst()) {
            User(
                c.getInt(0),
                c.getString(1),
                c.getString(2),
                c.getString(3) ?: "",
                c.getString(4) ?: "",
                c.getString(5) ?: ""
            ).also { c.close() }
        } else {
            c.close(); null
        }
    }

    fun register(email: String, username: String, password: String): Boolean {
        return try {
            writableDatabase.insert(T_USERS, null, ContentValues().apply {
                put("email", email); put("username", username); put("password", password)
                put("description", ""); put("avatar_uri", "")
            }) != -1L
        } catch (e: Exception) {
            false
        }
    }

    fun getUser(userId: Int): User? {
        val c = readableDatabase.rawQuery("SELECT * FROM $T_USERS WHERE id=?", arrayOf(userId.toString()))
        return if (c.moveToFirst()) {
            User(
                c.getInt(0),
                c.getString(1),
                c.getString(2),
                c.getString(3) ?: "",
                c.getString(4) ?: "",
                c.getString(5) ?: ""
            ).also { c.close() }
        } else {
            c.close(); null
        }
    }

    fun updateUser(userId: Int, username: String, email: String, desc: String, avatar: String): Int {
        return writableDatabase.update(T_USERS, ContentValues().apply {
            put("username", username)
            put("email", email)
            put("description", desc)
            put("avatar_uri", avatar)
        }, "id=?", arrayOf(userId.toString()))
    }

    fun getMarketItems(currentUserId: Int, search: String? = null, category: String? = null): List<Item> {
        val list = mutableListOf<Item>()
        var query = "SELECT i.*, u.username FROM $T_ITEMS i JOIN $T_USERS u ON i.seller_id=u.id WHERE i.seller_id!=$currentUserId"
        val args = mutableListOf<String>()

        if (!search.isNullOrBlank()) {
            query += " AND i.name LIKE ?"
            args.add("%$search%")
        }
        if (!category.isNullOrBlank() && category != "All") {
            query += " AND i.category = ?"
            args.add(category)
        }

        val c = readableDatabase.rawQuery(query, args.toTypedArray())
        while (c.moveToNext()) list.add(
            Item(
                c.getInt(0),
                c.getInt(1),
                c.getString(2),
                c.getDouble(3),
                c.getString(4),
                c.getString(5),
                c.getString(6),
                c.getString(7)
            )
        )
        c.close()
        return list
    }

    fun getMyItems(sellerId: Int): List<Item> {
        val list = mutableListOf<Item>()
        val c = readableDatabase.rawQuery(
            "SELECT i.*, u.username FROM $T_ITEMS i JOIN $T_USERS u ON i.seller_id=u.id WHERE i.seller_id=?",
            arrayOf(sellerId.toString())
        )
        while (c.moveToNext()) list.add(
            Item(
                c.getInt(0),
                c.getInt(1),
                c.getString(2),
                c.getDouble(3),
                c.getString(4),
                c.getString(5),
                c.getString(6),
                c.getString(7)
            )
        )
        c.close()
        return list
    }

    fun getItem(itemId: Int): Item? {
        val c = readableDatabase.rawQuery(
            "SELECT i.*, u.username FROM $T_ITEMS i JOIN $T_USERS u ON i.seller_id=u.id WHERE i.id=?",
            arrayOf(itemId.toString())
        )
        return if (c.moveToFirst()) Item(
            c.getInt(0),
            c.getInt(1),
            c.getString(2),
            c.getDouble(3),
            c.getString(4),
            c.getString(5),
            c.getString(6),
            c.getString(7)
        ).also { c.close() }
        else {
            c.close(); null
        }
    }

    fun addItem(sellerId: Int, name: String, price: Double, desc: String, cat: String): Long {
        return writableDatabase.insert(T_ITEMS, null, ContentValues().apply {
            put("seller_id", sellerId); put("name", name); put("price", price)
            put("description", desc); put("image_uri", ""); put("category", cat)
        })
    }

    fun updateItem(itemId: Int, name: String, price: Double, desc: String, cat: String): Int {
        return writableDatabase.update(T_ITEMS, ContentValues().apply {
            put("name", name)
            put("price", price)
            put("description", desc)
            put("category", cat)
        }, "id=?", arrayOf(itemId.toString()))
    }

    fun deleteItem(itemId: Int): Int {
        return writableDatabase.delete(T_ITEMS, "id=?", arrayOf(itemId.toString()))
    }

    fun getUserTransactions(userId: Int): List<Transaction> {
        val list = mutableListOf<Transaction>()
        val c = readableDatabase.rawQuery(
            """SELECT t.*, i.name, ub.username, us.username FROM $T_TRANS t
               JOIN $T_ITEMS i ON t.item_id=i.id
               JOIN $T_USERS ub ON t.buyer_id=ub.id
               JOIN $T_USERS us ON t.seller_id=us.id
               WHERE t.buyer_id=? OR t.seller_id=?""",
            arrayOf(userId.toString(), userId.toString())
        )
        while (c.moveToNext()) list.add(
            Transaction(
                c.getInt(0),
                c.getInt(1),
                c.getInt(2),
                c.getInt(3),
                c.getString(4),
                c.getString(5),
                c.getString(6),
                c.getString(7),
                c.getString(8),
                c.getString(9)
            )
        )
        c.close()
        return list
    }

    fun insertTransaction(itemId: Int, buyerId: Int, sellerId: Int, paymentMethod: String): Long {
        return writableDatabase.insert(T_TRANS, null, ContentValues().apply {
            put("item_id", itemId); put("buyer_id", buyerId); put("seller_id", sellerId)
            put("status", "Pending"); put("proof_image_uri", "")
            put("payment_method", paymentMethod) // simpan detail opsi transasksi
        })
    }

    fun updateTransactionProof(transId: Int, proofUri: String) {
        writableDatabase.update(T_TRANS, ContentValues().apply {
            put("status", "Proof Uploaded"); put("proof_image_uri", proofUri)
        }, "id=?", arrayOf(transId.toString()))
    }

    fun completeTransaction(transId: Int) {
        writableDatabase.update(T_TRANS, ContentValues().apply {
            put("status", "Completed")
        }, "id=?", arrayOf(transId.toString()))
    }

    fun getTransaction(transId: Int): Transaction? {
        val c = readableDatabase.rawQuery(
            """SELECT t.*, i.name, ub.username, us.username FROM $T_TRANS t
               JOIN $T_ITEMS i ON t.item_id=i.id
               JOIN $T_USERS ub ON t.buyer_id=ub.id
               JOIN $T_USERS us ON t.seller_id=us.id
               WHERE t.id=?""", arrayOf(transId.toString())
        )
        return if (c.moveToFirst()) Transaction(
            c.getInt(0),
            c.getInt(1),
            c.getInt(2),
            c.getInt(3),
            c.getString(4),
            c.getString(5),
            c.getString(6),
            c.getString(7),
            c.getString(8),
            c.getString(9)
        ).also { c.close() }
        else {
            c.close(); null
        }
    }

    fun getComments(itemId: Int): List<Comment> {
        val list = mutableListOf<Comment>()
        val c = readableDatabase.rawQuery(
            "SELECT c.*, u.username FROM $T_COMMENTS c JOIN $T_USERS u ON c.user_id=u.id WHERE c.item_id=? ORDER BY c.timestamp DESC",
            arrayOf(itemId.toString())
        )
        while (c.moveToNext()) {
            list.add(
                Comment(
                    c.getInt(0),
                    c.getInt(1),
                    c.getInt(2),
                    c.getString(6),
                    c.getString(3),
                    c.getString(4),
                    c.getLong(5)
                )
            )
        }
        c.close()
        return list
    }

    fun addComment(itemId: Int, userId: Int, text: String): Long {
        return writableDatabase.insert(T_COMMENTS, null, ContentValues().apply {
            put("item_id", itemId)
            put("user_id", userId)
            put("comment_text", text)
            put("timestamp", System.currentTimeMillis())
        })
    }

    fun addReply(commentId: Int, replyText: String): Int {
        return writableDatabase.update(T_COMMENTS, ContentValues().apply {
            put("reply_text", replyText)
        }, "id=?", arrayOf(commentId.toString()))
    }
}
