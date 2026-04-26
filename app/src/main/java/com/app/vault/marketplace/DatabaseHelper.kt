package com.app.vault.marketplace

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class User(val id: Int, val username: String, val password: String)
data class Item(val id: Int, val sellerId: Int, val name: String, val price: Double, val description: String, val imageUri: String, val sellerName: String = "")
data class Transaction(val id: Int, val itemId: Int, val buyerId: Int, val sellerId: Int, val status: String, val proofImageUri: String, val itemName: String = "", val buyerName: String = "", val sellerName: String = "")

class DatabaseHelper(ctx: Context) : SQLiteOpenHelper(ctx, "vault.db", null, 1) {

    companion object {
        const val T_USERS = "users"
        const val T_ITEMS = "items"
        const val T_TRANS = "transactions"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $T_USERS(id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT)")
        db.execSQL("CREATE TABLE $T_ITEMS(id INTEGER PRIMARY KEY AUTOINCREMENT, seller_id INTEGER, name TEXT, price REAL, description TEXT, image_uri TEXT)")
        db.execSQL("CREATE TABLE $T_TRANS(id INTEGER PRIMARY KEY AUTOINCREMENT, item_id INTEGER, buyer_id INTEGER, seller_id INTEGER, status TEXT, proof_image_uri TEXT)")

        // Seed users
        listOf(
            Pair("alice", "pass123"),
            Pair("bob", "pass123"),
            Pair("charlie", "pass123")
        ).forEach { (u, p) ->
            db.insert(T_USERS, null, ContentValues().apply { put("username", u); put("password", p) })
        }

        // Seed items (seller_id 1=alice, 2=bob, 3=charlie)
        data class ItemSeed(val sid: Int, val name: String, val price: Double, val desc: String)
        val items = listOf(
            ItemSeed(1, "Mobile Legends Account", 350000.0, "High rank account with many skins."),
            ItemSeed(1, "Free Fire Bundle Package", 120000.0, "Exclusive bundle package."),
            ItemSeed(2, "PUBG Mobile UC 3000", 450000.0, "Direct top-up UC."),
            ItemSeed(2, "Valorant Points 4300", 800000.0, "Direct top-up VP."),
            ItemSeed(3, "Genshin Impact Welkin x5", 275000.0, "Monthly card subscription."),
            ItemSeed(3, "Roblox Robux 10000", 220000.0, "Direct top-up Robux.")
        )
        items.forEach { (sid, name, price, desc) ->
            db.insert(T_ITEMS, null, ContentValues().apply {
                put("seller_id", sid); put("name", name); put("price", price)
                put("description", desc); put("image_uri", "")
            })
        }

        // Seed transactions
        // buyer_id=2(bob) buys from alice(1), buyer_id=3(charlie) buys from bob(2)
        db.insert(T_TRANS, null, ContentValues().apply {
            put("item_id", 1); put("buyer_id", 2); put("seller_id", 1)
            put("status", "Pending"); put("proof_image_uri", "")
        })
        db.insert(T_TRANS, null, ContentValues().apply {
            put("item_id", 3); put("buyer_id", 3); put("seller_id", 2)
            put("status", "Proof Uploaded"); put("proof_image_uri", "mock_proof_uri")
        })
        db.insert(T_TRANS, null, ContentValues().apply {
            put("item_id", 5); put("buyer_id", 1); put("seller_id", 3)
            put("status", "Completed"); put("proof_image_uri", "mock_proof_uri")
        })
    }

    override fun onUpgrade(db: SQLiteDatabase, o: Int, n: Int) {
        db.execSQL("DROP TABLE IF EXISTS $T_USERS")
        db.execSQL("DROP TABLE IF EXISTS $T_ITEMS")
        db.execSQL("DROP TABLE IF EXISTS $T_TRANS")
        onCreate(db)
    }

    fun login(username: String, password: String): User? {
        val c = readableDatabase.rawQuery(
            "SELECT * FROM $T_USERS WHERE username=? AND password=?", arrayOf(username, password))
        return if (c.moveToFirst()) User(c.getInt(0), c.getString(1), c.getString(2)).also { c.close() }
        else { c.close(); null }
    }

    fun register(username: String, password: String): Boolean {
        return try {
            writableDatabase.insert(T_USERS, null, ContentValues().apply {
                put("username", username); put("password", password)
            }) != -1L
        } catch (e: Exception) { false }
    }

    fun getMarketItems(currentUserId: Int): List<Item> {
        val list = mutableListOf<Item>()
        val c = readableDatabase.rawQuery(
            "SELECT i.*, u.username FROM $T_ITEMS i JOIN $T_USERS u ON i.seller_id=u.id WHERE i.seller_id!=?",
            arrayOf(currentUserId.toString()))
        while (c.moveToNext()) list.add(Item(c.getInt(0), c.getInt(1), c.getString(2), c.getDouble(3), c.getString(4), c.getString(5), c.getString(6)))
        c.close()
        return list
    }

    fun getMyItems(sellerId: Int): List<Item> {
        val list = mutableListOf<Item>()
        val c = readableDatabase.rawQuery(
            "SELECT i.*, u.username FROM $T_ITEMS i JOIN $T_USERS u ON i.seller_id=u.id WHERE i.seller_id=?",
            arrayOf(sellerId.toString()))
        while (c.moveToNext()) list.add(Item(c.getInt(0), c.getInt(1), c.getString(2), c.getDouble(3), c.getString(4), c.getString(5), c.getString(6)))
        c.close()
        return list
    }

    fun getItem(itemId: Int): Item? {
        val c = readableDatabase.rawQuery(
            "SELECT i.*, u.username FROM $T_ITEMS i JOIN $T_USERS u ON i.seller_id=u.id WHERE i.id=?",
            arrayOf(itemId.toString()))
        return if (c.moveToFirst()) Item(c.getInt(0), c.getInt(1), c.getString(2), c.getDouble(3), c.getString(4), c.getString(5), c.getString(6)).also { c.close() }
        else { c.close(); null }
    }

    fun addItem(sellerId: Int, name: String, price: Double, desc: String): Long {
        return writableDatabase.insert(T_ITEMS, null, ContentValues().apply {
            put("seller_id", sellerId); put("name", name); put("price", price)
            put("description", desc); put("image_uri", "")
        })
    }

    fun getUserTransactions(userId: Int): List<Transaction> {
        val list = mutableListOf<Transaction>()
        val c = readableDatabase.rawQuery(
            """SELECT t.*, i.name, ub.username, us.username FROM $T_TRANS t
               JOIN $T_ITEMS i ON t.item_id=i.id
               JOIN $T_USERS ub ON t.buyer_id=ub.id
               JOIN $T_USERS us ON t.seller_id=us.id
               WHERE t.buyer_id=? OR t.seller_id=?""",
            arrayOf(userId.toString(), userId.toString()))
        while (c.moveToNext()) list.add(Transaction(c.getInt(0), c.getInt(1), c.getInt(2), c.getInt(3), c.getString(4), c.getString(5), c.getString(6), c.getString(7), c.getString(8)))
        c.close()
        return list
    }

    fun insertTransaction(itemId: Int, buyerId: Int, sellerId: Int): Long {
        return writableDatabase.insert(T_TRANS, null, ContentValues().apply {
            put("item_id", itemId); put("buyer_id", buyerId); put("seller_id", sellerId)
            put("status", "Pending"); put("proof_image_uri", "")
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
               WHERE t.id=?""", arrayOf(transId.toString()))
        return if (c.moveToFirst()) Transaction(c.getInt(0), c.getInt(1), c.getInt(2), c.getInt(3), c.getString(4), c.getString(5), c.getString(6), c.getString(7), c.getString(8)).also { c.close() }
        else { c.close(); null }
    }
}