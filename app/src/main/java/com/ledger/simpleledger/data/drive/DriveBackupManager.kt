package com.ledger.simpleledger.data.drive

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val DRIVE_SCOPE = "oauth2:https://www.googleapis.com/auth/drive.file"
private const val BACKUP_FILE_NAME = "KhataBook_backup.json"

sealed class DriveBackupResult {
    object Success : DriveBackupResult()
    data class NeedsConsent(val intent: Intent) : DriveBackupResult()
    data class Error(val message: String) : DriveBackupResult()
}

/**
 * Handles connecting the user's Google account (per-file Drive access only — this app can
 * only see/manage the single backup file it creates, nothing else in their Drive) and
 * uploading/updating the weekly backup file there.
 */
object DriveBackupManager {

    fun signInClient(context: Context): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.file"))
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    fun lastSignedInAccount(context: Context): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(context)

    /** Uploads (or updates) the backup file in the user's Drive. */
    suspend fun backupNow(context: Context, jsonBytes: ByteArray): DriveBackupResult = withContext(Dispatchers.IO) {
        try {
            val account = lastSignedInAccount(context)
                ?: return@withContext DriveBackupResult.Error("Not signed in to Google")
            val token = GoogleAuthUtil.getToken(context, account.account!!, DRIVE_SCOPE)

            val existingFileId = findExistingFileId(token)
            val code = if (existingFileId != null) {
                updateFile(token, existingFileId, jsonBytes)
            } else {
                createFile(token, jsonBytes)
            }
            if (code in 200..299) {
                DriveBackupResult.Success
            } else {
                DriveBackupResult.Error("Drive rejected the upload (code $code)")
            }
        } catch (e: UserRecoverableAuthException) {
            val consentIntent = e.intent
            if (consentIntent != null) {
                DriveBackupResult.NeedsConsent(consentIntent)
            } else {
                DriveBackupResult.Error(e.message ?: "Needs additional permission")
            }
        } catch (e: Exception) {
            DriveBackupResult.Error(e.message ?: e.javaClass.simpleName)
        }
    }

    private fun findExistingFileId(token: String): String? {
        val query = "name='$BACKUP_FILE_NAME' and trashed=false"
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = URL("https://www.googleapis.com/drive/v3/files?q=$encodedQuery&fields=files(id,name)")
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        val code = connection.responseCode
        if (code !in 200..299) return null
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(body)
        val files = json.optJSONArray("files") ?: return null
        if (files.length() == 0) return null
        return files.getJSONObject(0).optString("id")
    }

    private fun createFile(token: String, jsonBytes: ByteArray): Int {
        val boundary = "khatabook_backup_boundary"
        val metadata = JSONObject().apply {
            put("name", BACKUP_FILE_NAME)
            put("mimeType", "application/json")
        }
        val body = buildMultipartBody(boundary, metadata.toString(), jsonBytes)

        val url = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
        connection.connectTimeout = 15000
        connection.readTimeout = 20000
        connection.outputStream.use { it.write(body) }
        val code = connection.responseCode
        connection.disconnect()
        return code
    }

    private fun updateFile(token: String, fileId: String, jsonBytes: ByteArray): Int {
        val url = URL("https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "PATCH"
        connection.doOutput = true
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.connectTimeout = 15000
        connection.readTimeout = 20000
        connection.outputStream.use { it.write(jsonBytes) }
        val code = connection.responseCode
        connection.disconnect()
        return code
    }

    private fun buildMultipartBody(boundary: String, metadataJson: String, fileBytes: ByteArray): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        fun writeText(s: String) = out.write(s.toByteArray(Charsets.UTF_8))

        writeText("--$boundary\r\n")
        writeText("Content-Type: application/json; charset=UTF-8\r\n\r\n")
        writeText(metadataJson)
        writeText("\r\n--$boundary\r\n")
        writeText("Content-Type: application/json\r\n\r\n")
        out.write(fileBytes)
        writeText("\r\n--$boundary--")

        return out.toByteArray()
    }
}
