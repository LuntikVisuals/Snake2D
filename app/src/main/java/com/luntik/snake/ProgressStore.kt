package com.luntik.snake

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class LeaderEntry(val name: String, val score: Int, val mode: String)

class ProgressStore(ctx: Context) {
    private val p = ctx.getSharedPreferences("snake2d_progress", Context.MODE_PRIVATE)

    var coins: Int
        get() = p.getInt("coins", 100)
        set(v) = p.edit().putInt("coins", v.coerceAtLeast(0)).apply()

    var xp: Int
        get() = p.getInt("xp", 0)
        set(v) = p.edit().putInt("xp", v.coerceAtLeast(0)).apply()

    val level: Int
        get() = 1 + xp / 100

    var nickname: String
        get() = p.getString("nick", "Игрок") ?: "Игрок"
        set(v) = p.edit().putString("nick", v.take(16).ifBlank { "Игрок" }).apply()

    var selectedSkin: String
        get() = p.getString("skin", "default") ?: "default"
        set(v) = p.edit().putString("skin", v).apply()

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

    fun addCoins(n: Int) { coins = coins + n }
    fun spendCoins(n: Int): Boolean {
        if (coins < n) return false
        coins = coins - n
        return true
    }

    fun addXp(n: Int) { xp = xp + n }

    fun bestScore(mode: String): Int = p.getInt("best_$mode", 0)

    fun setBestScore(mode: String, score: Int) {
        if (score > bestScore(mode)) p.edit().putInt("best_$mode", score).apply()
    }

    fun leaderboard(): List<LeaderEntry> {
        val raw = p.getString("board", "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                LeaderEntry(o.getString("name"), o.getInt("score"), o.optString("mode", "classic"))
            }.sortedByDescending { it.score }.take(30)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun pushScore(name: String, score: Int, mode: String) {
        if (score <= 0) return
        val list = leaderboard().toMutableList()
        list.add(LeaderEntry(name, score, mode))
        val top = list.sortedByDescending { it.score }.take(30)
        val arr = JSONArray()
        top.forEach {
            arr.put(JSONObject().put("name", it.name).put("score", it.score).put("mode", it.mode))
        }
        p.edit().putString("board", arr.toString()).apply()
        setBestScore(mode, score)
    }
}
