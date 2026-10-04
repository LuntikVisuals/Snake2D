package com.luntik.snake

import android.content.Context
import org.json.JSONObject
import java.io.File

object GameFiles {
    private const val DIR = "snake2d"
    private const val COMPANION = "companion"
    const val PROTOCOL = 1
    const val PROGRESS_FILE = "progress_backup.json"

    fun root(context: Context): File =
        File(context.filesDir, DIR).also { it.mkdirs() }

    fun externalRoot(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return File(base, DIR).also { it.mkdirs() }
    }

    fun companionDir(context: Context): File =
        File(root(context), COMPANION).also { it.mkdirs() }

    fun ensureLayout(context: Context) {
        val r = root(context)
        File(r, "saves").mkdirs()
        File(r, "logs").mkdirs()
        companionDir(context)
        val ext = externalRoot(context)
        File(ext, "saves").mkdirs()
        File(ext, "logs").mkdirs()
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
        File(ext, "README.txt").writeText(
            "Snake2D data folder.\nprogress_backup.json — backup.\n" +
                "Path: Android/data/com.luntik.snake/files/snake2d/\n"
        )
    }

    fun progressBackupFile(context: Context): File =
        File(externalRoot(context), PROGRESS_FILE)

    fun writeProgressBackup(context: Context, json: String) {
        try {
            ensureLayout(context)
            progressBackupFile(context).writeText(json)
            File(root(context), PROGRESS_FILE).writeText(json)
        } catch (_: Exception) { }
    }

    fun readProgressBackup(context: Context): String? {
        return try {
            val ext = progressBackupFile(context)
            val int = File(root(context), PROGRESS_FILE)
            when {
                ext.isFile && ext.length() > 2 -> ext.readText()
                int.isFile && int.length() > 2 -> int.readText()
                else -> null
            }
        } catch (_: Exception) { null }
    }

    fun writeProfileCard(context: Context, nick: String, level: Int, xp: Int, bpLevel: Int, seasonId: String) {
        try {
            ensureLayout(context)
            val o = JSONObject()
                .put("nick", nick).put("level", level).put("xp", xp)
                .put("bpLevel", bpLevel).put("season", seasonId)
                .put("updated", System.currentTimeMillis())
            File(externalRoot(context), "profile.json").writeText(o.toString())
            File(root(context), "profile.json").writeText(o.toString())
        } catch (_: Exception) { }
    }

    fun writeSessionLog(context: Context, line: String) {
        try {
            val f = File(File(root(context), "logs"), "session.log")
            f.appendText("${System.currentTimeMillis()}\t$line\n")
            val f2 = File(File(externalRoot(context), "logs"), "session.log")
            f2.appendText("${System.currentTimeMillis()}\t$line\n")
        } catch (_: Exception) { }
    }

    fun readCompanionManifest(context: Context): JSONObject? {
        val f = File(companionDir(context), "manifest.json")
        if (!f.isFile) return null
        return try { JSONObject(f.readText()) } catch (_: Exception) { null }
    }
}
