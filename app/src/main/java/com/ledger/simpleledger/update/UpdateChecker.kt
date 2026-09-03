package com.ledger.simpleledger.update

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val buildNumber: Int,
    val downloadUrl: String,
    val releaseName: String
)

/**
 * Checks the app's own GitHub repository for the latest release published by the
 * GitHub Actions build workflow, and can download the APK so it can be installed.
 * This repository must be public for these plain HTTPS calls to work without a token.
 */
object UpdateChecker {
    private const val API_URL =
        "https://api.github.com/repos/shahidanwar9330-glitch/SimpleLedger/releases/latest"
    private val TAG_REGEX = Regex("""build-(\d+)""")

    suspend fun fetchLatest(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(API_URL).openConnection() as HttpURLConnection
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            val code = connection.responseCode
            if (code !in 200..299) return@withContext null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tag = json.optString("tag_name")
            val buildNumber = TAG_REGEX.find(tag)?.groupValues?.get(1)?.toIntOrNull()
                ?: return@withContext null
            val assets = json.optJSONArray("assets") ?: return@withContext null
            var url: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name") == "app-debug.apk") {
                    url = asset.optString("browser_download_url")
                    break
                }
            }
            if (url.isNullOrBlank()) return@withContext null
            UpdateInfo(buildNumber, url, json.optString("name"))
        } catch (e: Exception) {
            null
        }
    }

    /** Downloads the APK into the public Downloads directory, reporting progress from 0f to 1f.
     * Returns the downloaded file, or null on failure.
     * The Downloads directory is globally readable by the package installer. */
    suspend fun downloadApk(
        context: Context,
        url: String,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.connect()
            val total = connection.contentLength
            
            // Use public Downloads directory for better installer compatibility
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val outFile = File(downloadsDir, "SimpleLedger_update.apk")
            
            connection.inputStream.use { input ->
                outFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) onProgress(downloaded.toFloat() / total)
                    }
                }
            }
            outFile
        } catch (e: Exception) {
            null
        }
    }
}
