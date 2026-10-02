package com.luntik.snake

import android.content.Context
import org.json.JSONObject
import java.io.File

object GameFiles {
    private const val DIR = "snake2d"
    private const val COMPANION = "companion"
    const val PROTOCOL = 1

    fun root(context: Context): File =
        File(context.filesDir, DIR).also { it.mkdirs() }

    fun companionDir(context: Context): File =
        File(root(context), COMPANION).also { it.mkdirs() }

    fun ensureLayout(context: Context) {
        val r = root(context)
        File(r, "saves").mkdirs()
        File(r, "logs").mkdirs()
        companionDir(context)
        val meta = File(r, "game_meta.json")
        if (!meta.exists()) {
            meta.writeText(
                JSONObject()
                    .put("protocol", PROTOCOL)
                    .put("app", "Snake2D")
                    .put("created", System.currentTimeMillis())
                    .toString()
            )
        }
        File(r, ".snake2d_install").writeText("ok\n${System.currentTimeMillis()}\n")
    }

    fun writeSessionLog(context: Context, line: String) {
        try {
            val f = File(File(root(context), "logs"), "session.log")
            f.appendText("${System.currentTimeMillis()}\t$line\n")
        } catch (_: Exception) { }
    }

    fun readCompanionManifest(context: Context): JSONObject? {
        val f = File(companionDir(context), "manifest.json")
        if (!f.isFile) return null
        return try {
            JSONObject(f.readText())
        } catch (_: Exception) {
            null
        }
    }
}
