package com.luntik.snake

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class LeaderEntry(val name: String, val score: Int, val mode: String)
data class HistoryEntry(val timestamp: Long, val type: String, val description: String, val amount: Int = 0)
data class Stats(
    val gamesPlayed: Int = 0, val wins: Int = 0, val losses: Int = 0,
    val totalFoodEaten: Int = 0, val maxLength: Int = 3,
    val totalCoinsEarned: Int = 0, val totalCoinsSpent: Int = 0, val casesOpened: Int = 0
)

class ProgressStore(ctx: Context) {
    private val appCtx = ctx.applicationContext
    private val p = ctx.getSharedPreferences("snake2d_progress", Context.MODE_PRIVATE)

    companion object {
        private const val SCHEMA_VERSION = 2
        private const val MAX_HISTORY = 50
    }

    init {
        migrateIfNeeded()
        if (p.getInt("gamesPlayed", 0) == 0 && p.getInt("coins", 100) == 100 && p.getInt("xp", 0) == 0) {
            restoreFromBackup()
        } else {
            saveBackup()
        }
    }

    private fun migrateIfNeeded() {
        val current = p.getInt("schemaVersion", 1)
        if (current < SCHEMA_VERSION) {
            p.edit().putInt("schemaVersion", SCHEMA_VERSION).apply()
        }
    }

    val playerId: String
        get() = p.getString("playerId", null) ?: run {
            val id = UUID.randomUUID().toString()
            p.edit().putString("playerId", id).apply()
            id
        }

    var coins: Int
        get() = p.getInt("coins", 100).coerceAtLeast(0)
        set(v) = p.edit().putInt("coins", v.coerceAtLeast(0)).apply()

    fun addCoins(n: Int, reason: String = "reward") {
        if (n <= 0) return
        coins = coins + n
        incrementStat("totalCoinsEarned", n)
        appendHistory("reward", reason, n)
        saveBackup()
    }

    fun spendCoins(n: Int, reason: String = "spend"): Boolean {
        if (n <= 0) return true
        if (coins < n) return false
        coins = coins - n
        incrementStat("totalCoinsSpent", n)
        appendHistory("spend", reason, -n)
        saveBackup()
        return true
    }

    var xp: Int
        get() = p.getInt("xp", 0).coerceAtLeast(0)
        set(v) = p.edit().putInt("xp", v.coerceAtLeast(0)).apply()

    val level: Int get() = 1 + xp / 100

    fun addXp(n: Int) { if (n > 0) { xp = xp + n; saveBackup() } }

    var nickname: String
        get() = p.getString("nick", "Игрок") ?: "Игрок"
        set(v) {
            val cleaned = v.trim().filter { it.isLetterOrDigit() || it in " _-." }.take(16).ifBlank { "Игрок" }
            p.edit().putString("nick", cleaned).apply()
            saveBackup()
        }

    var selectedSkin: String
        get() = p.getString("skin", "default") ?: "default"
        set(v) { p.edit().putString("skin", v).apply(); saveBackup() }

    var selectedCharacter: String
        get() = p.getString("character", "basic") ?: "basic"
        set(v) { p.edit().putString("character", v).apply(); saveBackup() }

    fun unlockedCharacters(): Set<String> =
        (p.getStringSet("chars", setOf("basic")) ?: setOf("basic")) + "basic"

    fun unlockCharacter(id: String) {
        val set = unlockedCharacters().toMutableSet(); set.add(id)
        p.edit().putStringSet("chars", set).apply(); saveBackup()
    }

    fun isCharacterUnlocked(id: String) = id in unlockedCharacters()

    fun unlockedSkins(): Set<String> =
        (p.getStringSet("unlocked", setOf("default")) ?: setOf("default")) + "default"

    fun unlockSkin(id: String) {
        val set = unlockedSkins().toMutableSet(); set.add(id)
        p.edit().putStringSet("unlocked", set).apply(); saveBackup()
    }

    fun isUnlocked(id: String) = id in unlockedSkins()
    fun bestScore(mode: String): Int = p.getInt("best_$mode", 0)
    fun setBestScore(mode: String, score: Int) {
        if (score > bestScore(mode)) p.edit().putInt("best_$mode", score).apply()
    }

    fun pushScore(name: String, score: Int, mode: String) {
        if (score <= 0) return
        val list = leaderboard().toMutableList()
        list.add(LeaderEntry(name.take(16), score, mode))
        val top = list.sortedByDescending { it.score }.take(30)
        val arr = JSONArray()
        top.forEach { arr.put(JSONObject().put("name", it.name).put("score", it.score).put("mode", it.mode)) }
        p.edit().putString("board", arr.toString()).apply()
        setBestScore(mode, score)
        saveBackup()
    }

    fun leaderboard(): List<LeaderEntry> {
        val raw = p.getString("board", "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull {
                val o = arr.optJSONObject(it) ?: return@mapNotNull null
                val score = o.optInt("score", -1)
                if (score < 0) null else LeaderEntry(o.optString("name", "Игрок").take(16), score, o.optString("mode", "CLASSIC"))
            }.sortedByDescending { it.score }.take(30)
        } catch (_: Exception) { emptyList() }
    }

    fun getStats(): Stats = Stats(
        p.getInt("gamesPlayed", 0), p.getInt("wins", 0), p.getInt("losses", 0),
        p.getInt("totalFoodEaten", 0), p.getInt("maxLength", 3),
        p.getInt("totalCoinsEarned", 0), p.getInt("totalCoinsSpent", 0), p.getInt("casesOpened", 0)
    )

    private fun incrementStat(key: String, by: Int = 1) {
        p.edit().putInt(key, p.getInt(key, 0) + by).apply()
    }

    fun recordGameEnd(won: Boolean, foodEaten: Int, length: Int) {
        incrementStat("gamesPlayed")
        if (won) incrementStat("wins") else incrementStat("losses")
        if (foodEaten > 0) incrementStat("totalFoodEaten", foodEaten)
        if (length > p.getInt("maxLength", 3)) p.edit().putInt("maxLength", length).apply()
        saveBackup()
    }

    fun recordCaseOpened() { incrementStat("casesOpened"); saveBackup() }

    fun history(): List<HistoryEntry> {
        val raw = p.getString("history", "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull {
                val o = arr.optJSONObject(it) ?: return@mapNotNull null
                HistoryEntry(o.optLong("ts", 0L), o.optString("type", ""), o.optString("desc", ""), o.optInt("amount", 0))
            }.sortedByDescending { it.timestamp }.take(MAX_HISTORY)
        } catch (_: Exception) { emptyList() }
    }

    private fun appendHistory(type: String, description: String, amount: Int = 0) {
        val list = history().toMutableList()
        list.add(0, HistoryEntry(System.currentTimeMillis(), type, description, amount))
        val arr = JSONArray()
        list.take(MAX_HISTORY).forEach {
            arr.put(JSONObject().put("ts", it.timestamp).put("type", it.type).put("desc", it.description).put("amount", it.amount))
        }
        p.edit().putString("history", arr.toString()).apply()
    }

    fun logPurchase(skinName: String, price: Int) = appendHistory("purchase", "Куплен скин: $skinName", -price)
    fun logCase(caseName: String, dropName: String, wasDuplicate: Boolean) {
        if (wasDuplicate) appendHistory("duplicate", "Кейс $caseName → дубликат $dropName", 20)
        else appendHistory("case", "Кейс $caseName → $dropName", 0)
    }

    fun canClaimDaily(): Boolean =
        (p.getString("lastDailyDay", "") ?: "") != java.time.LocalDate.now().toString()

    fun claimDaily(): Int {
        if (!canClaimDaily()) return 0
        val today = java.time.LocalDate.now().toString()
        val streak = p.getInt("dailyStreak", 0)
        val yesterday = java.time.LocalDate.now().minusDays(1).toString()
        val newStreak = if ((p.getString("lastDailyDay", "") ?: "") == yesterday) streak + 1 else 1
        val reward = (15 + newStreak * 5).coerceAtMost(50)
        p.edit().putString("lastDailyDay", today).putInt("dailyStreak", newStreak).apply()
        addCoins(reward, "Ежедневная награда (серия $newStreak)")
        return reward
    }

    fun unlockedAchievements(): Set<String> = p.getStringSet("achievements", emptySet()) ?: emptySet()
    fun isAchievementUnlocked(id: String) = id in unlockedAchievements()
    fun unlockAchievement(id: String): Boolean {
        if (isAchievementUnlocked(id)) return false
        val set = unlockedAchievements().toMutableSet(); set.add(id)
        p.edit().putStringSet("achievements", set).apply()
        ShopData.achievements.find { it.id == id }?.let {
            addCoins(it.rewardCoins, "Достижение: ${it.title}")
        }
        return true
    }

    fun checkAchievementsAfterGame(score: Int, length: Int, won: Boolean) {
        val stats = getStats()
        if (stats.gamesPlayed >= 1) unlockAchievement("first_game")
        if (won) unlockAchievement("first_win")
        if (score >= 100) unlockAchievement("score_100")
        if (score >= 300) unlockAchievement("score_300")
        if (length >= 20) unlockAchievement("length_20")
        if (stats.casesOpened >= 3) unlockAchievement("open_3_cases")
        if (unlockedSkins().size >= 5) unlockAchievement("skins_5")
    }

    fun resetAll() {
        p.edit().clear().apply()
        p.edit().putInt("schemaVersion", SCHEMA_VERSION).putInt("coins", 100).putInt("xp", 0)
            .putString("nick", "Игрок").putString("skin", "default").putStringSet("unlocked", setOf("default"))
            .putString("character", "basic").putStringSet("chars", setOf("basic"))
            .putString("playerId", UUID.randomUUID().toString()).putString("history", "[]")
            .putStringSet("achievements", emptySet()).putString("lastDailyDay", "").putInt("dailyStreak", 0).apply()
        saveBackup()
    }

    fun saveBackup() {
        try {
            val o = JSONObject()
            o.put("coins", coins).put("xp", xp).put("nick", nickname).put("skin", selectedSkin)
                .put("character", selectedCharacter)
                .put("unlocked", JSONArray(unlockedSkins().toList()))
                .put("chars", JSONArray(unlockedCharacters().toList()))
                .put("gamesPlayed", p.getInt("gamesPlayed", 0))
                .put("wins", p.getInt("wins", 0)).put("losses", p.getInt("losses", 0))
                .put("totalFoodEaten", p.getInt("totalFoodEaten", 0))
                .put("maxLength", p.getInt("maxLength", 3))
                .put("totalCoinsEarned", p.getInt("totalCoinsEarned", 0))
                .put("totalCoinsSpent", p.getInt("totalCoinsSpent", 0))
                .put("casesOpened", p.getInt("casesOpened", 0))
                .put("playerId", playerId)
                .put("history", p.getString("history", "[]"))
                .put("lastDailyDay", p.getString("lastDailyDay", ""))
            val bests = JSONObject()
            GameMode.entries.forEach { bests.put(it.name, bestScore(it.name)) }
            o.put("bests", bests).put("savedAt", System.currentTimeMillis())
            GameFiles.writeProgressBackup(appCtx, o.toString())
        } catch (_: Exception) { }
    }

    fun restoreFromBackup() {
        try {
            val raw = GameFiles.readProgressBackup(appCtx) ?: return
            val o = JSONObject(raw)
            p.edit()
                .putInt("coins", o.optInt("coins", 100)).putInt("xp", o.optInt("xp", 0))
                .putString("nick", o.optString("nick", "Игрок"))
                .putString("skin", o.optString("skin", "default"))
                .putString("character", o.optString("character", "basic"))
                .putInt("gamesPlayed", o.optInt("gamesPlayed", 0))
                .putInt("wins", o.optInt("wins", 0)).putInt("losses", o.optInt("losses", 0))
                .putInt("totalFoodEaten", o.optInt("totalFoodEaten", 0))
                .putInt("maxLength", o.optInt("maxLength", 3))
                .putInt("totalCoinsEarned", o.optInt("totalCoinsEarned", 0))
                .putInt("totalCoinsSpent", o.optInt("totalCoinsSpent", 0))
                .putInt("casesOpened", o.optInt("casesOpened", 0))
                .putString("playerId", o.optString("playerId", UUID.randomUUID().toString()))
                .putString("history", o.optString("history", "[]"))
                .putString("lastDailyDay", o.optString("lastDailyDay", "")).apply()
            val unlocked = mutableSetOf<String>()
            o.optJSONArray("unlocked")?.let { for (i in 0 until it.length()) unlocked.add(it.getString(i)) }
            if (unlocked.isEmpty()) unlocked.add("default")
            p.edit().putStringSet("unlocked", unlocked).apply()
            val chars = mutableSetOf<String>()
            o.optJSONArray("chars")?.let { for (i in 0 until it.length()) chars.add(it.getString(i)) }
            if (chars.isEmpty()) chars.add("basic")
            p.edit().putStringSet("chars", chars).apply()
            o.optJSONObject("bests")?.let { b ->
                val keys = b.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    p.edit().putInt("best_$k", b.optInt(k, 0)).apply()
                }
            }
        } catch (_: Exception) { }
    }
}
