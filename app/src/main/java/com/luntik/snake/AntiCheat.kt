package com.luntik.snake

import android.content.Context

data class AntiCheatVerdict(
    val ok: Boolean,
    val fairForLeaderboard: Boolean,
    val flags: List<String>,
    val detail: String
)

object AntiCheat {
    data class Verdict(
        val ok: Boolean,
        val fairForLeaderboard: Boolean,
        val flags: List<String>,
        val detail: String
    )

    fun scan(context: Context, store: ProgressStore): Verdict {
        val flags = mutableListOf<String>()
        val manifest = GameFiles.readCompanionManifest(context)
        if (manifest != null) {
            val src = manifest.optString("source", "unknown")
            val name = manifest.optString("name", "companion")
            flags += "companion:$name:$src"
            if (src.contains("cheat", ignoreCase = true) ||
                name.contains("DeltaSnake", ignoreCase = true)
            ) {
                flags += "delta_snake_detected"
            }
        }
        if (store.coins > 5_000_000) flags += "coins_anomaly"
        if (store.xp > 10_000_000) flags += "xp_anomaly"
        GameMode.entries.forEach { m ->
            val b = store.bestScore(m.name)
            if (b > 500_000) flags += "score_anomaly_${m.name}"
        }
        val fair = flags.none {
            it.startsWith("delta_snake") || it.contains("anomaly") || it.contains("cheat")
        }
        return Verdict(
            ok = true,
            fairForLeaderboard = fair,
            flags = flags,
            detail = if (fair) "fair" else "flags=${flags.joinToString(",")}"
        )
    }

    fun validateScore(score: Int, durationMs: Long, foodEaten: Int): Boolean {
        if (score < 0 || foodEaten < 0) return false
        if (score > 0 && durationMs < 500) return false
        if (foodEaten > 0 && score > foodEaten * 50 + 100) return false
        return true
    }

    fun markRun(context: Context, fair: Boolean, extra: String = "") {
        GameFiles.writeSessionLog(context, "run fair=$fair $extra")
    }
}
