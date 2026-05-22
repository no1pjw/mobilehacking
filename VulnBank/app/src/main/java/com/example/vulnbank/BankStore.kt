package com.example.vulnbank

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.security.MessageDigest
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class HomeSnapshot(
    val username: String,
    val displayName: String,
    val accountNo: String,
    val balance: Long,
    val monthlySpent: Long,
    val savingGoal: Long,
    val saved: Long,
    val cardLimit: Long,
    val cardSpent: Long,
    val rewardPoints: Long
)

data class TimelineItem(
    val title: String,
    val subtitle: String,
    val amount: Long,
    val direction: String,
    val createdAt: Long
)

class BankStore(private val context: Context) {
    private val db: SQLiteDatabase = context.openOrCreateDatabase("neobank.db", Context.MODE_PRIVATE, null)
    private val won = NumberFormat.getNumberInstance(Locale.KOREA)
    private val dayFormat = SimpleDateFormat("MM.dd HH:mm", Locale.KOREA)

    init {
        setup()
    }

    private fun setup() {
        db.execSQL("DROP TABLE IF EXISTS users")
        db.execSQL("DROP TABLE IF EXISTS transfers")
        db.execSQL("DROP TABLE IF EXISTS beneficiaries")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS profiles (
                username TEXT PRIMARY KEY,
                display_name TEXT NOT NULL,
                pin_hash TEXT NOT NULL,
                account_no TEXT NOT NULL,
                balance INTEGER NOT NULL,
                saving_goal INTEGER NOT NULL,
                saved INTEGER NOT NULL,
                card_limit INTEGER NOT NULL,
                card_spent INTEGER NOT NULL,
                reward_points INTEGER NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ledger (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                owner TEXT NOT NULL,
                counterparty TEXT NOT NULL,
                title TEXT NOT NULL,
                amount INTEGER NOT NULL,
                direction TEXT NOT NULL,
                memo TEXT NOT NULL,
                receipt_id TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS favorites (
                owner TEXT NOT NULL,
                target TEXT NOT NULL,
                alias TEXT NOT NULL,
                PRIMARY KEY(owner, target)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS session_cache (
                username TEXT PRIMARY KEY,
                access_token TEXT NOT NULL,
                device_fingerprint TEXT NOT NULL,
                approval_seed TEXT NOT NULL,
                last_account_snapshot TEXT NOT NULL,
                cached_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS support_cache (
                username TEXT PRIMARY KEY,
                diagnostic_bundle TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        ensureSystemProfile("market", "모아상점")
        ensureSystemProfile("taxi", "블루택시")
        ensureSystemProfile("coffee", "라운지커피")
    }

    private fun ensureSystemProfile(username: String, displayName: String) {
        if (exists(username)) return
        val values = ContentValues().apply {
            put("username", username)
            put("display_name", displayName)
            put("pin_hash", sha256("system:$username"))
            put("account_no", accountNo(username))
            put("balance", 0L)
            put("saving_goal", 0L)
            put("saved", 0L)
            put("card_limit", 0L)
            put("card_spent", 0L)
            put("reward_points", 0L)
            put("created_at", System.currentTimeMillis())
        }
        db.insert("profiles", null, values)
    }

    fun createProfile(username: String, displayName: String, pin: String): Boolean {
        val clean = username.trim().lowercase(Locale.US)
        if (!clean.matches(Regex("[a-z0-9_]{3,16}")) || pin.length < 4 || exists(clean)) return false
        val values = ContentValues().apply {
            put("username", clean)
            put("display_name", displayName.ifBlank { clean })
            put("pin_hash", hashPin(clean, pin))
            put("account_no", accountNo(clean))
            put("balance", 2_350_000L)
            put("saving_goal", 5_000_000L)
            put("saved", 860_000L)
            put("card_limit", 1_200_000L)
            put("card_spent", 248_500L)
            put("reward_points", 18_420L)
            put("created_at", System.currentTimeMillis())
        }
        db.insert("profiles", null, values)
        addFavorite(clean, "coffee", "라운지커피")
        addFavorite(clean, "taxi", "블루택시")
        refreshSessionCache(clean)
        cacheSupportBundle(clean)
        return true
    }

    fun login(username: String, pin: String): Boolean {
        val clean = username.trim().lowercase(Locale.US)
        val cursor = db.rawQuery("SELECT pin_hash FROM profiles WHERE username = ?", arrayOf(clean))
        val ok = if (cursor.moveToFirst()) cursor.getString(0) == hashPin(clean, pin) else false
        cursor.close()
        if (ok) {
            refreshSessionCache(clean)
            cacheSupportBundle(clean)
        }
        return ok
    }

    fun snapshot(username: String): HomeSnapshot {
        val cursor = db.rawQuery(
            """
            SELECT display_name, account_no, balance, saving_goal, saved, card_limit, card_spent, reward_points
            FROM profiles WHERE username = ?
            """.trimIndent(),
            arrayOf(username)
        )
        var snapshot = HomeSnapshot(username, username, accountNo(username), 0, 0, 0, 0, 0, 0, 0)
        if (cursor.moveToFirst()) {
            snapshot = HomeSnapshot(
                username,
                cursor.getString(0),
                cursor.getString(1),
                cursor.getLong(2),
                monthlySpent(username),
                cursor.getLong(3),
                cursor.getLong(4),
                cursor.getLong(5),
                cursor.getLong(6),
                cursor.getLong(7)
            )
        }
        cursor.close()
        return snapshot
    }

    fun sendMoney(owner: String, target: String, amount: Long, memo: String): String {
        require(amount > 0) { "금액을 확인해주세요." }
        require(exists(target)) { "받는 분을 찾을 수 없어요." }
        val fee = if (amount >= 100_000L) 0L else 100L
        require(balance(owner) >= amount + fee) { "잔액이 부족해요." }
        val receipt = "NB-${UUID.randomUUID().toString().take(18)}"
        db.beginTransaction()
        try {
            updateBalance(owner, balance(owner) - amount - fee)
            updateBalance(target, balance(target) + amount)
            insertLedger(owner, target, displayName(target), amount + fee, "OUT", memo, receipt)
            insertLedger(target, owner, displayName(owner), amount, "IN", memo, receipt)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshSessionCache(owner)
        cacheSupportBundle(owner)
        return receipt
    }

    fun payCard(owner: String, merchant: String, amount: Long): String {
        val snapshot = snapshot(owner)
        require(amount > 0) { "결제 금액을 확인해주세요." }
        require(snapshot.cardSpent + amount <= snapshot.cardLimit) { "카드 한도를 초과했어요." }
        val receipt = sendMoney(owner, merchant, amount, "카드 결제")
        val values = ContentValues().apply {
            put("card_spent", snapshot.cardSpent + amount)
            put("reward_points", snapshot.rewardPoints + amount / 100L)
        }
        db.update("profiles", values, "username = ?", arrayOf(owner))
        return receipt
    }

    fun setCardLimit(owner: String, limit: Long) {
        val values = ContentValues().apply { put("card_limit", limit.coerceIn(100_000L, 5_000_000L)) }
        db.update("profiles", values, "username = ?", arrayOf(owner))
        refreshSessionCache(owner)
    }

    fun saveGoal(owner: String, amount: Long): String {
        require(amount > 0) { "저축 금액을 확인해주세요." }
        require(balance(owner) >= amount) { "잔액이 부족해요." }
        val current = snapshot(owner)
        updateBalance(owner, current.balance - amount)
        val values = ContentValues().apply { put("saved", current.saved + amount) }
        db.update("profiles", values, "username = ?", arrayOf(owner))
        insertLedger(owner, "savings", "저축 목표", amount, "OUT", "자동 저축", "NB-SAVE-${UUID.randomUUID().toString().take(8)}")
        refreshSessionCache(owner)
        return money(current.saved + amount)
    }

    fun timeline(username: String, limit: Int = 12): List<TimelineItem> {
        val cursor = db.rawQuery(
            """
            SELECT title, counterparty, amount, direction, created_at
            FROM ledger WHERE owner = ?
            ORDER BY created_at DESC LIMIT ?
            """.trimIndent(),
            arrayOf(username, limit.toString())
        )
        val rows = mutableListOf<TimelineItem>()
        while (cursor.moveToNext()) {
            rows += TimelineItem(cursor.getString(0), cursor.getString(1), cursor.getLong(2), cursor.getString(3), cursor.getLong(4))
        }
        cursor.close()
        return rows
    }

    fun favorites(username: String): List<Pair<String, String>> {
        val cursor = db.rawQuery("SELECT target, alias FROM favorites WHERE owner = ? ORDER BY alias", arrayOf(username))
        val rows = mutableListOf<Pair<String, String>>()
        while (cursor.moveToNext()) rows += cursor.getString(0) to cursor.getString(1)
        cursor.close()
        return rows
    }

    fun addFavorite(owner: String, target: String, alias: String): Boolean {
        if (!exists(target) || owner == target) return false
        val values = ContentValues().apply {
            put("owner", owner)
            put("target", target)
            put("alias", alias.ifBlank { displayName(target) })
        }
        db.insertWithOnConflict("favorites", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        return true
    }

    fun directory(keyword: String): List<String> {
        val cursor = db.rawQuery(
            "SELECT username, display_name FROM profiles WHERE username LIKE ? OR display_name LIKE ? ORDER BY display_name LIMIT 8",
            arrayOf("%${keyword.trim()}%", "%${keyword.trim()}%")
        )
        val rows = mutableListOf<String>()
        while (cursor.moveToNext()) rows += "${cursor.getString(1)} (${cursor.getString(0)})"
        cursor.close()
        return rows
    }

    fun copyPayload(username: String): String {
        val s = snapshot(username)
        val token = sessionToken(username)
        return "계좌 ${s.accountNo} / ${s.displayName} / 확인코드 ${token.takeLast(6)}"
    }

    fun supportDiagnostics(username: String): String {
        cacheSupportBundle(username)
        val cursor = db.rawQuery("SELECT diagnostic_bundle FROM support_cache WHERE username = ?", arrayOf(username))
        val value = if (cursor.moveToFirst()) cursor.getString(0) else ""
        cursor.close()
        return value
    }

    fun money(value: Long): String = "${won.format(value)}원"

    fun date(value: Long): String = dayFormat.format(Date(value))

    private fun refreshSessionCache(username: String) {
        val s = snapshot(username)
        val token = sessionToken(username)
        val values = ContentValues().apply {
            put("username", username)
            put("access_token", token)
            put("device_fingerprint", sha256("${context.packageName}:$username:${s.accountNo}").take(32))
            put("approval_seed", sha256("$token:${s.balance}:${s.cardSpent}").take(16))
            put("last_account_snapshot", "name=${s.displayName};account=${s.accountNo};balance=${s.balance};card=${s.cardSpent}/${s.cardLimit}")
            put("cached_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict("session_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun cacheSupportBundle(username: String) {
        val s = snapshot(username)
        val recent = timeline(username, 4).joinToString(" | ") { "${it.title}:${it.amount}:${it.direction}" }
        val values = ContentValues().apply {
            put("username", username)
            put("diagnostic_bundle", "profile=${s.displayName};account=${s.accountNo};token=${sessionToken(username)};recent=$recent")
            put("updated_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict("support_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun sessionToken(username: String): String =
        sha256("$username:${accountNo(username)}:mobile-session").take(40)

    private fun insertLedger(owner: String, counterparty: String, title: String, amount: Long, direction: String, memo: String, receipt: String) {
        val values = ContentValues().apply {
            put("owner", owner)
            put("counterparty", counterparty)
            put("title", title)
            put("amount", amount)
            put("direction", direction)
            put("memo", memo)
            put("receipt_id", receipt)
            put("created_at", System.currentTimeMillis())
        }
        db.insert("ledger", null, values)
    }

    private fun monthlySpent(username: String): Long {
        val cursor = db.rawQuery("SELECT COALESCE(SUM(amount), 0) FROM ledger WHERE owner = ? AND direction = 'OUT'", arrayOf(username))
        val value = if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        cursor.close()
        return value
    }

    private fun balance(username: String): Long {
        val cursor = db.rawQuery("SELECT balance FROM profiles WHERE username = ?", arrayOf(username))
        val value = if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        cursor.close()
        return value
    }

    private fun updateBalance(username: String, value: Long) {
        val values = ContentValues().apply { put("balance", value) }
        db.update("profiles", values, "username = ?", arrayOf(username))
    }

    private fun exists(username: String): Boolean {
        val cursor = db.rawQuery("SELECT 1 FROM profiles WHERE username = ? LIMIT 1", arrayOf(username))
        val ok = cursor.moveToFirst()
        cursor.close()
        return ok
    }

    private fun displayName(username: String): String {
        val cursor = db.rawQuery("SELECT display_name FROM profiles WHERE username = ?", arrayOf(username))
        val value = if (cursor.moveToFirst()) cursor.getString(0) else username
        cursor.close()
        return value
    }

    private fun accountNo(seed: String): String {
        val digits = sha256(seed).filter { it.isDigit() }.padEnd(12, '7').take(12)
        return "1002-${digits.take(4)}-${digits.drop(4).take(4)}-${digits.drop(8)}"
    }

    private fun hashPin(username: String, pin: String): String = sha256("$username:$pin:neo")

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
