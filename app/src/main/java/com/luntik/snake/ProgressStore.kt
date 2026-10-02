package com.luntik.snake

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class LeaderEntry(val name: String, val score: Int, val mode: String)

data class HistoryEntry(
    val timestamp: Long,
    val type: String,
    val description: String,
    val amount: Int = 0
)

data class Stats(
    val gamesPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val totalFoodEaten: Int = 0,
    val maxLength: Int = 3,
    val totalCoinsEarned: Int = 0,
    val totalCoinsSpent: Int = 0,
    val casesOpened: Int = 0
)

class ProgressStore(ctx: Context) {
    private val p = ctx.getSharedPreferences("snake2d_progress", Context.MODE_PRIVATE)

    companion object {
        private const val SCHEMA_VERSION = 2
        private const val MAX_HISTORY = 50
    }

    init {
        migrateIfNeeded()
    }

    private fun migrateIfNeeded() {
        val current = p.getInt("schemaVersion", 1)
        if (current < SCHEMA_VERSION) {
            p.edit()
                .putInt("schemaVersion", SCHEMA_VERSION)
                .putInt("gamesPlayed", p.getInt("gamesPlayed", 0))
                .putInt("wins", p.getInt("wins", 0))
                .putInt("losses", p.getInt("losses", 0))
                .putInt("totalFoodEaten", p.getInt("totalFoodEaten", 0))
                .putInt("maxLength", p.getInt("maxLength", 3))
                .putInt("totalCoinsEarned", p.getInt("totalCoinsEarned", 0))
                .putInt("totalCoinsSpent", p.getInt("totalCoinsSpent", 0))
                .putInt("casesOpened", p.getInt("casesOpened", 0))
                .putString("history", p.getString("history", "[]") ?: "[]")
                .putString("playerId", p.getString("playerId", null) ?: UUID.randomUUID().toString())
                .apply()
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
    }

    fun spendCoins(n: Int, reason: String = "spend"): Boolean {
        if (n <= 0) return true
        if (coins < n) return false
        coins = coins - n
        incrementStat("totalCoinsSpent", n)
        appendHistory("spend", reason, -n)
        return true
    }

    var xp: Int
        get() = p.getInt("xp", 0).coerceAtLeast(0)
        set(v) = p.edit().putInt("xp", v.coerceAtLeast(0)).apply()

    val level: Int
        get() = 1 + xp / 100

    fun addXp(n: Int) {
        if (n <= 0) return
        xp = xp + n
    }

    var nickname: String
        get() = p.getString("nick", "Игрок") ?: "Игрок"
        set(v) {
            val cleaned = v.trim().filter { it.isLetterOrDigit() || it in " _-." }.take(16).ifBlank { "Игрок" }
            p.edit().putString("nick", cleaned).apply()
        }

    var selectedSkin: String
        get() = p.getString("skin", "default") ?: "default"
        set(v) = p.edit().putString("skin", v).apply()

    var selectedCharacter: String
        get() = p.getString("character", "basic") ?: "basic"
        set(v) = p.edit().putString("character", v).apply()

    fun unlockedCharacters(): Set<String> {
        val s = p.getStringSet("chars", setOf("basic")) ?: setOf("basic")
        return s + "basic"
    }

    fun unlockCharacter(id: String) {
        val set = unlockedCharacters().toMutableSet()
        set.add(id)
        p.edit().putStringSet("chars", set).apply()
    }

    fun isCharacterUnlocked(id: String) = id in unlockedCharacters()

    fun unlockedSkins(): Set<String> {
        val s = p.getStringSet("unlocked", setOf("default")) ?: setOf("default")
        return s + "default"
    }

    fun unlockSkin(id: String) {
        val set = unlockedSkins().toMutableSet()
        set.add(id)
        p.edit().putStringSet("unlocked", set).apply()
    }

    fun isUnlocked(id: String) = id in unlockedSkins()

    fun bestScore(mode: String): Int = p.getInt("best_$mode", 0)

    fun setBestScore(mode: String, score: Int) {
        if (score > bestScore(mode)) p.edit().putInt("best_$mode", score).apply()
    }

    fun leaderboard(): List<LeaderEntry> {
        val raw = p.getString("board", "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull {
                val o = arr.optJSONObject(it) ?: return@mapNotNull null
                val name = o.optString("name", "Игрок").take(16)
                val score = o.optInt("score", -1)
                val mode = o.optString("mode", "CLASSIC")
                if (score < 0) null else LeaderEntry(name, score, mode)
            }.sortedByDescending { it.score }.take(30)
        } catch (_: Exception) { emptyList() }
    }

    fun pushScore(name: String, score: Int, mode: String) {
        if (score <= 0) return
        val list = leaderboard().toMutableList()
        list.add(LeaderEntry(name.take(16), score, mode))
        val top = list.sortedByDescending { it.score }.take(30)
        val arr = JSONArray()
        top.forEach {
            arr.put(JSONObject().put("name", it.name).put("score", it.score).put("mode", it.mode))
        }
        p.edit().putString("board", arr.toString()).apply()
        setBestScore(mode, score)
    }

    fun getStats(): Stats = Stats(
        gamesPlayed = p.getInt("gamesPlayed", 0),
        wins = p.getInt("wins", 0),
        losses = p.getInt("losses", 0),
        totalFoodEaten = p.getInt("totalFoodEaten", 0),
        maxLength = p.getInt("maxLength", 3),
        totalCoinsEarned = p.getInt("totalCoinsEarned", 0),
        totalCoinsSpent = p.getInt("totalCoinsSpent", 0),
        casesOpened = p.getInt("casesOpened", 0)
    )

    private fun incrementStat(key: String, by: Int = 1) {
        p.edit().putInt(key, p.getInt(key, 0) + by).apply()
    }

    fun recordGameEnd(won: Boolean, foodEaten: Int, length: Int) {
        incrementStat("gamesPlayed")
        if (won) incrementStat("wins") else incrementStat("losses")
        if (foodEaten > 0) incrementStat("totalFoodEaten", foodEaten)
        val currentMax = p.getInt("maxLength", 3)
        if (length > currentMax) p.edit().putInt("maxLength", length).apply()
    }

    fun recordCaseOpened() { incrementStat("casesOpened") }

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

    fun logPurchase(skinName: String, price: Int) {
        appendHistory("purchase", "Куплен скин: $skinName", -price)
    }

    fun logCase(caseName: String, dropName: String, wasDuplicate: Boolean) {
        if (wasDuplicate) appendHistory("duplicate", "Кейс $caseName → дубликат $dropName (+25)", 25)
        else appendHistory("case", "Кейс $caseName → $dropName", 0)
    }

    fun lastDailyClaimDay(): String = p.getString("lastDailyDay", "") ?: ""

    fun canClaimDaily(): Boolean = lastDailyClaimDay() != java.time.LocalDate.now().toString()

    fun claimDaily(): Int {
        if (!canClaimDaily()) return 0
        val today = java.time.LocalDate.now().toString()
        val streak = p.getInt("dailyStreak", 0)
        val yesterday = java.time.LocalDate.now().minusDays(1).toString()
        val newStreak = if (lastDailyClaimDay() == yesterday) streak + 1 else 1
        val reward = (15 + newStreak * 5).coerceAtMost(50)
        p.edit().putString("lastDailyDay", today).putInt("dailyStreak", newStreak).apply()
        addCoins(reward, "Ежедневная награда (серия $newStreak)")
        return reward
    }

    fun dailyStreak(): Int = p.getInt("dailyStreak", 0)

    fun unlockedAchievements(): Set<String> = p.getStringSet("achievements", emptySet()) ?: emptySet()

    fun isAchievementUnlocked(id: String) = id in unlockedAchievements()

    fun unlockAchievement(id: String): Boolean {
        if (isAchievementUnlocked(id)) return false
        val set = unlockedAchievements().toMutableSet()
        set.add(id)
        p.edit().putStringSet("achievements", set).apply()
        val ach = ShopData.achievements.find { it.id == id }
        if (ach != null) {
            addCoins(ach.rewardCoins, "Достижение: ${ach.title}")
            appendHistory("achievement", "Достижение: ${ach.title}", ach.rewardCoins)
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
        p.edit()
            .putInt("schemaVersion", SCHEMA_VERSION)
            .putInt("coins", 100)
            .putInt("xp", 0)
            .putString("nick", "Игрок")
            .putString("skin", "default")
            .putStringSet("unlocked", setOf("default"))
            .putString("character", "basic")
            .putStringSet("chars", setOf("basic"))
            .putString("playerId", UUID.randomUUID().toString())
            .putString("history", "[]")
            .putStringSet("achievements", emptySet())
            .putString("lastDailyDay", "")
            .putInt("dailyStreak", 0)
            .apply()
    }
}
