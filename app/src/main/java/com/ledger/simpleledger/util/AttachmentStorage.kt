package com.ledger.simpleledger.util

import android.content.Context
import android.net.Uri
import java.io.File

/** Stores transaction attachments (receipt images, voice notes) in the app's private
 * internal storage, so they persist independently of any temporary picker permissions
 * and are included automatically in local backup/restore of the app's data folder. */
object AttachmentStorage {

    private fun attachmentsDir(context: Context): File {
        val dir = File(context.filesDir, "attachments")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** Copies the picked image into internal storage and returns its absolute path,
     * or null if the copy failed. */
    fun copyImage(context: Context, sourceUri: Uri): String? {
        return try {
            val fileName = "img_${System.currentTimeMillis()}.jpg"
            val destFile = File(attachmentsDir(context), fileName)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /** Returns a fresh file path to record a new voice note into. */
    fun newVoiceNoteFile(context: Context): File {
        val fileName = "voice_${System.currentTimeMillis()}.m4a"
        return File(attachmentsDir(context), fileName)
    }

    fun deleteIfExists(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }
}
