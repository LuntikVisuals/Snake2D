package com.luntik.snake

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val available: Boolean,
    val latestTag: String,
    val latestName: String,
    val body: String,
    val htmlUrl: String,
    val apkUrl: String?,
    val message: String
)

/**
 * Проверка обновлений через GitHub Releases.
 * Установка поверх старой версии (тот же applicationId + больший versionCode)
 * сохраняет SharedPreferences / прогресс — переустанавливать с нуля не нужно.
 */
object UpdateChecker {
    private const val API = "https://api.github.com/repos/LuntikVisuals/Snake2D/releases/latest"
    private const val RELEASES_PAGE = "https://github.com/LuntikVisuals/Snake2D/releases/latest"

    fun currentVersionName(context: Context): String {
        return try {
            val p = context.packageManager.getPackageInfo(context.packageName, 0)
            p.versionName ?: "?"
        } catch (_: Exception) {
            "?"
        }
    }

    fun currentVersionCode(context: Context): Long {
        return try {
            val p = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= 28) p.longVersionCode else @Suppress("DEPRECATION") p.versionCode.toLong()
        } catch (_: Exception) {
            0L
        }
    }

    suspend fun check(context: Context): UpdateInfo = withContext(Dispatchers.IO) {
        val currentCode = currentVersionCode(context)
        val currentName = currentVersionName(context)
        try {
            val conn = URL(API).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "Snake2D-UpdateChecker")
            try {
                val code = conn.responseCode
                if (code !in 200..299) {
                    return@withContext UpdateInfo(
                        false, "", "", "", RELEASES_PAGE, null,
                        "Не удалось проверить (HTTP $code). Открой релизы вручную."
                    )
                }
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)
                val tag = json.optString("tag_name", "")
                val name = json.optString("name", tag)
                val body = json.optString("body", "")
                val htmlUrl = json.optString("html_url", RELEASES_PAGE)
                var apkUrl: String? = null
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val a = assets.optJSONObject(i) ?: continue
                        val n = a.optString("name", "")
                        if (n.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = a.optString("browser_download_url", null)
                            break
                        }
                    }
                }
                val remoteCode = parseVersionCode(tag, name, body)
                val available = remoteCode > currentCode ||
                    (remoteCode == 0L && isNameNewer(name, tag, currentName))
                val msg = if (available) {
                    "Доступно обновление: $name\nСейчас у тебя: $currentName ($currentCode)\nПрогресс сохранится при установке поверх."
                } else {
                    "У тебя актуальная версия: $currentName ($currentCode)"
                }
                UpdateInfo(available, tag, name, body, htmlUrl, apkUrl, msg)
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            UpdateInfo(
                false, "", "", "", RELEASES_PAGE, null,
                "Нет сети или ошибка проверки: ${e.message ?: "unknown"}\nРелизы: $RELEASES_PAGE"
            )
        }
    }

    private fun parseVersionCode(tag: String, name: String, body: String): Long {
        Regex("""build-(\d+)""", RegexOption.IGNORE_CASE).find(tag)?.groupValues?.getOrNull(1)?.toLongOrNull()?.let { return it }
        Regex("""build-(\d+)""", RegexOption.IGNORE_CASE).find(name)?.groupValues?.getOrNull(1)?.toLongOrNull()?.let { return it }
        Regex("""versionCode\\s*[=:]\\s*(\d+)""", RegexOption.IGNORE_CASE).find(body)?.groupValues?.getOrNull(1)?.toLongOrNull()?.let { return it }
        Regex("""^v?(\d+)""").find(tag)?.groupValues?.getOrNull(1)?.toLongOrNull()?.let { return it }
        return 0L
    }

    private fun isNameNewer(name: String, tag: String, current: String): Boolean {
        if (current == "?" || current.isBlank()) return true
        val a = name.ifBlank { tag }
        return a != current && (a.contains("beta", true) || a > current)
    }

    fun openUrl(context: Context, url: String) {
        try {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        } catch (_: Exception) { }
    }
}
