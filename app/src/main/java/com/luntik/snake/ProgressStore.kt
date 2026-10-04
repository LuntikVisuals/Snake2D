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
        private const val SCHEMA_VERSION = 3
        private const val MAX_HISTORY = 50
        const val MAX_LEVEL = 1000
        const val POINTS_PER_APPLE = 5
    }

    init {
        if (p.getInt("schemaVersion", 1) < SCHEMA_VERSION) p.edit().putInt("schemaVersion", SCHEMA_VERSION).apply()
        if (p.getInt("gamesPlayed", 0) == 0 && p.getInt("coins", 100) == 100 && p.getInt("xp", 0) == 0) restoreFromBackup()
        else saveBackup()
    }

    val playerId: String
        get() = p.getString("playerId", null) ?: run {
            val id = UUID.randomUUID().toString(); p.edit().putString("playerId", id).apply(); id
        }

    var coins: Int
        get() = p.getInt("coins", 100).coerceAtLeast(0)
        set(v) = p.edit().putInt("coins", v.coerceAtLeast(0)).apply()
    var gems: Int
        get() = p.getInt("gems", 0).coerceAtLeast(0)
        set(v) = p.edit().putInt("gems", v.coerceAtLeast(0)).apply()
    var tix: Int
        get() = p.getInt("tix", 0).coerceAtLeast(0)
        set(v) = p.edit().putInt("tix", v.coerceAtLeast(0)).apply()

    fun addCoins(n: Int, reason: String = "reward") {
        if (n <= 0) return
        coins += n; incrementStat("totalCoinsEarned", n); appendHistory("reward", reason, n); saveBackup()
    }
    fun spendCoins(n: Int, reason: String = "spend"): Boolean {
        if (n <= 0) return true
        if (coins < n) return false
        coins -= n; incrementStat("totalCoinsSpent", n); appendHistory("spend", reason, -n); saveBackup(); return true
    }
    fun addGems(n: Int, reason: String = "gems") {
        if (n <= 0) return; gems += n; appendHistory("gems", reason, n); saveBackup()
    }
    fun spendGems(n: Int, reason: String = "spend_gems"): Boolean {
        if (n <= 0) return true
        if (gems < n) return false
        gems -= n; appendHistory("spend_gems", reason, -n); saveBackup(); return true
    }
    fun addTix(n: Int, reason: String = "tix") {
        if (n <= 0) return; tix += n; appendHistory("tix", reason, n); saveBackup()
    }

    var xp: Int
        get() = p.getInt("xp", 0).coerceAtLeast(0)
        set(v) = p.edit().putInt("xp", v.coerceAtLeast(0)).apply()

    val level: Int
        get() {
            var lv = 1; var need = 50; var rest = xp
            while (lv < MAX_LEVEL && rest >= need) { rest -= need; lv++; need = 50 + lv * 10 }
            return lv
        }

    fun addXp(n: Int): Boolean {
        if (n <= 0) return false
        val before = level; xp += n; saveBackup(); return level > before
    }

    var nickname: String
        get() = p.getString("nick", "Игрок") ?: "Игрок"
        set(v) {
            val cleaned = v.trim().filter { it.isLetterOrDigit() || it in " _-." }.take(16).ifBlank { "Игрок" }
            p.edit().putString("nick", cleaned).apply(); saveBackup()
        }

    var selectedSkin: String
        get() = p.getString("skin", "default") ?: "default"
        set(v) { p.edit().putString("skin", v).apply(); saveBackup() }
    var selectedCharacter: String
        get() = p.getString("character", "basic") ?: "basic"
        set(v) { p.edit().putString("character", v).apply(); saveBackup() }
    var selectedAppearance: String
        get() = p.getString("appearance", "viper") ?: "viper"
        set(v) { p.edit().putString("appearance", v).apply(); saveBackup() }
    var selectedApple: String
        get() = p.getString("appleSkin", "apple") ?: "apple"
        set(v) { p.edit().putString("appleSkin", v).apply(); saveBackup() }

    var avatarUri: String
        get() = p.getString("avatarUri", "") ?: ""
        set(v) { p.edit().putString("avatarUri", v).apply(); saveBackup() }
    var bannerUri: String
        get() = p.getString("bannerUri", "") ?: ""
        set(v) { p.edit().putString("bannerUri", v).apply(); saveBackup() }
    var gridColor: Long
        get() = p.getLong("gridColor", 0x12FFFFFF)
        set(v) { p.edit().putLong("gridColor", v).apply(); saveBackup() }
    var fieldBg: Long
        get() = p.getLong("fieldBg", 0xFF0B1420)
        set(v) { p.edit().putLong("fieldBg", v).apply(); saveBackup() }
    var screenBg: Long
        get() = p.getLong("screenBg", 0xFF070B12)
        set(v) { p.edit().putLong("screenBg", v).apply(); saveBackup() }
    var customFieldSlot: Boolean
        get() = p.getBoolean("customField", false)
        set(v) { p.edit().putBoolean("customField", v).apply(); saveBackup() }
    var fieldPhotoUri: String
        get() = p.getString("fieldPhoto", "") ?: ""
        set(v) { p.edit().putString("fieldPhoto", v).apply(); saveBackup() }
    var showGrid: Boolean
        get() = p.getBoolean("showGrid", false)
        set(v) { p.edit().putBoolean("showGrid", v).apply(); saveBackup() }
    var showHitboxes: Boolean
        get() = p.getBoolean("showHitboxes", false)
        set(v) { p.edit().putBoolean("showHitboxes", v).apply(); saveBackup() }
    var targetFps: Int
        get() = p.getInt("targetFps", 120).coerceIn(60, 144)
        set(v) { p.edit().putInt("targetFps", v.coerceIn(60, 144)).apply(); saveBackup() }
    var controlMode: String
        get() = p.getString("controlMode", "buttons") ?: "buttons"
        set(v) { p.edit().putString("controlMode", v).apply(); saveBackup() }
    var registered: Boolean
        get() = p.getBoolean("registered", false)
        set(v) = p.edit().putBoolean("registered", v).apply()
    var bpPremium: Boolean
        get() = p.getBoolean("bpPremium", false)
        set(v) { p.edit().putBoolean("bpPremium", v).apply(); saveBackup() }
    var bpTix: Int
        get() = p.getInt("bpTix", 0).coerceAtLeast(0)
        set(v) { p.edit().putInt("bpTix", v.coerceAtLeast(0)).apply(); saveBackup() }
    val bpLevel: Int get() = BattlePassData.levelFromTix(bpTix)

    fun addBpTix(n: Int) { if (n <= 0) return; bpTix += n; tix += n; saveBackup() }

    fun claimedBp(level: Int, premium: Boolean): Boolean =
        p.getBoolean("bp_claim_${if (premium) "p" else "f"}_$level", false)

    fun claimBp(level: Int, premium: Boolean): Boolean {
        if (claimedBp(level, premium) || bpLevel < level) return false
        if (premium && !bpPremium) return false
        p.edit().putBoolean("bp_claim_${if (premium) "p" else "f"}_$level", true).apply()
        val reward = BattlePassData.levels.find { it.level == level }?.let { if (premium) it.premium else it.free } ?: return false
        when (reward.type) {
            BpRewardType.COINS -> addCoins(reward.amount, "Батлпасс: ${reward.title}")
            BpRewardType.GEMS -> addGems(reward.amount, "Батлпасс: ${reward.title}")
            BpRewardType.TIX -> addTix(reward.amount, "Батлпасс")
            BpRewardType.SKIN -> unlockSkin(reward.itemId)
            BpRewardType.CHARACTER -> unlockCharacter(reward.itemId)
            BpRewardType.APPEARANCE -> {
                val s = unlockedAppearances().toMutableSet(); s.add(reward.itemId)
                p.edit().putStringSet("appearances", s).apply()
            }
            BpRewardType.APPLE -> {
                val s = unlockedApples().toMutableSet(); s.add(reward.itemId)
                p.edit().putStringSet("apples", s).apply()
            }
            BpRewardType.XP_CRATE -> {
                addCoins((20..60).random(), "XP-ящик"); addGems((1..5).random(), "XP-ящик")
            }
            BpRewardType.NONE -> {}
        }
        saveBackup(); return true
    }

    fun unlockedAppearances(): Set<String> =
        (p.getStringSet("appearances", setOf("classic", "viper")) ?: setOf("classic", "viper")) + "classic"
    fun unlockedApples(): Set<String> =
        (p.getStringSet("apples", setOf("apple")) ?: setOf("apple")) + "apple"
    fun unlockApple(id: String) {
        val s = unlockedApples().toMutableSet(); s.add(id)
        p.edit().putStringSet("apples", s).apply(); saveBackup()
    }
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
    fun setBestScore(mode: String, score: Int) { if (score > bestScore(mode)) p.edit().putInt("best_$mode", score).apply() }

    fun pushScore(name: String, score: Int, mode: String) {
        if (score <= 0) return
        fun saveBoard(key: String, list: List<LeaderEntry>) {
            val top = list.sortedByDescending { it.score }.take(30)
            val arr = JSONArray(); top.forEach { arr.put(JSONObject().put("name", it.name).put("score", it.score).put("mode", it.mode)) }
            p.edit().putString(key, arr.toString()).apply()
        }
        val modeList = leaderboard(mode).toMutableList(); modeList.add(LeaderEntry(name.take(16), score, mode))
        saveBoard("board_$mode", modeList)
        val all = leaderboard(null).toMutableList(); all.add(LeaderEntry(name.take(16), score, mode))
        saveBoard("board", all)
        setBestScore(mode, score); saveBackup()
    }

    fun leaderboard(mode: String? = null): List<LeaderEntry> {
        val key = if (mode != null) "board_$mode" else "board"
        val raw = p.getString(key, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull {
                val o = arr.optJSONObject(it) ?: return@mapNotNull null
                val score = o.optInt("score", -1)
                if (score < 0) null else LeaderEntry(o.optString("name", "Игрок").take(16), score, o.optString("mode", "CLASSIC"))
            }.sortedByDescending { it.score }.take(30)
        } catch (_: Exception) { emptyList() }
    }

    fun getStats(): Stats = Stats(p.getInt("gamesPlayed", 0), p.getInt("wins", 0), p.getInt("losses", 0),
        p.getInt("totalFoodEaten", 0), p.getInt("maxLength", 3), p.getInt("totalCoinsEarned", 0),
        p.getInt("totalCoinsSpent", 0), p.getInt("casesOpened", 0))
    private fun incrementStat(key: String, by: Int = 1) { p.edit().putInt(key, p.getInt(key, 0) + by).apply() }
    fun recordGameEnd(won: Boolean, foodEaten: Int, length: Int) {
        incrementStat("gamesPlayed"); if (won) incrementStat("wins") else incrementStat("losses")
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
    fun canClaimDaily(): Boolean = (p.getString("lastDailyDay", "") ?: "") != java.time.LocalDate.now().toString()
    fun claimDaily(): Int {
        if (!canClaimDaily()) return 0
        val today = java.time.LocalDate.now().toString()
        val streak = p.getInt("dailyStreak", 0)
        val yesterday = java.time.LocalDate.now().minusDays(1).toString()
        val newStreak = if ((p.getString("lastDailyDay", "") ?: "") == yesterday) streak + 1 else 1
        val reward = (15 + newStreak * 5).coerceAtMost(50)
        p.edit().putString("lastDailyDay", today).putInt("dailyStreak", newStreak).apply()
        addCoins(reward, "Ежедневная награда"); return reward
    }
    fun unlockedAchievements(): Set<String> = p.getStringSet("achievements", emptySet()) ?: emptySet()
    fun isAchievementUnlocked(id: String) = id in unlockedAchievements()
    fun unlockAchievement(id: String): Boolean {
        if (isAchievementUnlocked(id)) return false
        val set = unlockedAchievements().toMutableSet(); set.add(id)
        p.edit().putStringSet("achievements", set).apply()
        ShopData.achievements.find { it.id == id }?.let { addCoins(it.rewardCoins, "Достижение: ${it.title}") }
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

    fun grantMatchRewards(score: Int, mode: GameMode): Triple<Int, Int, Int> {
        val coinsGain = (score * mode.coinMul / 2f).toInt().coerceAtLeast(if (score > 0) 2 else 0)
        val gemsGain = ((score / 50) * mode.xpMul).toInt().coerceIn(0, 20)
        val tixGain = ((score / 10f) * mode.tixMul).toInt().coerceAtLeast(if (score > 0) 1 else 0)
        if (coinsGain > 0) addCoins(coinsGain, "Партия ${mode.title}")
        if (gemsGain > 0) addGems(gemsGain, "Партия ${mode.title}")
        if (tixGain > 0) addBpTix(tixGain)
        addXp((score * mode.xpMul / 2f).toInt().coerceAtLeast(3))
        return Triple(coinsGain, gemsGain, tixGain)
    }

    fun saveBackup() {
        try {
            val o = JSONObject()
            o.put("coins", coins).put("gems", gems).put("tix", tix).put("xp", xp).put("nick", nickname)
                .put("skin", selectedSkin).put("character", selectedCharacter)
                .put("appearance", selectedAppearance).put("appleSkin", selectedApple)
                .put("showGrid", showGrid).put("showHitboxes", showHitboxes).put("targetFps", targetFps)
                .put("controlMode", controlMode).put("registered", registered)
                .put("bpPremium", bpPremium).put("bpTix", bpTix)
                .put("unlocked", JSONArray(unlockedSkins().toList()))
                .put("chars", JSONArray(unlockedCharacters().toList()))
                .put("playerId", playerId)
            GameFiles.writeProgressBackup(appCtx, o.toString())
            GameFiles.writeProfileCard(appCtx, nickname, level, xp, bpLevel, BattlePassData.SEASON_ID)
        } catch (_: Exception) { }
    }

    fun restoreFromBackup() {
        try {
            val raw = GameFiles.readProgressBackup(appCtx) ?: return
            val o = JSONObject(raw)
            p.edit().putInt("coins", o.optInt("coins", 100)).putInt("gems", o.optInt("gems", 0))
                .putInt("tix", o.optInt("tix", 0)).putInt("xp", o.optInt("xp", 0))
                .putString("nick", o.optString("nick", "Игрок"))
                .putString("skin", o.optString("skin", "default"))
                .putString("character", o.optString("character", "basic"))
                .putBoolean("showGrid", o.optBoolean("showGrid", false))
                .putInt("targetFps", o.optInt("targetFps", 120))
                .putBoolean("registered", o.optBoolean("registered", false))
                .putBoolean("bpPremium", o.optBoolean("bpPremium", false))
                .putInt("bpTix", o.optInt("bpTix", 0)).apply()
            val unlocked = mutableSetOf<String>()
            o.optJSONArray("unlocked")?.let { for (i in 0 until it.length()) unlocked.add(it.getString(i)) }
            if (unlocked.isEmpty()) unlocked.add("default")
            p.edit().putStringSet("unlocked", unlocked).apply()
            val chars = mutableSetOf<String>()
            o.optJSONArray("chars")?.let { for (i in 0 until it.length()) chars.add(it.getString(i)) }
            if (chars.isEmpty()) chars.add("basic")
            p.edit().putStringSet("chars", chars).apply()
        } catch (_: Exception) { }
    }
}
