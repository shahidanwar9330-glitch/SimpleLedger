package com.ledger.simpleledger.data.backup

import android.content.Context
import android.net.Uri
import com.google.gson.GsonBuilder
import com.ledger.simpleledger.data.db.entities.CategoryEntity
import com.ledger.simpleledger.data.db.entities.PersonEntity
import com.ledger.simpleledger.data.db.entities.TransactionEntity
import com.ledger.simpleledger.data.repository.LedgerRepository

data class BackupPayload(
    val schemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val appName: String = "Simple Ledger",
    val people: List<PersonEntity>,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>
)

data class RestoreResult(
    val peopleCount: Int,
    val categoriesCount: Int,
    val transactionsCount: Int
)

sealed class BackupException(message: String) : Exception(message) {
    class InvalidFile(message: String) : BackupException(message)
    class WriteFailed(message: String) : BackupException(message)
}

/**
 * Handles exporting the whole database into a single structured JSON file the user picks
 * a location for (via Storage Access Framework), and restoring from such a file.
 * This is fully local/offline. When cloud sync is added later, this same payload shape
 * can be reused as the sync format.
 */
class BackupManager(private val repository: LedgerRepository) {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    /** Builds the same backup payload used for local export, as raw JSON bytes —
     * used for uploading directly to Google Drive without going through a file Uri. */
    suspend fun buildBackupJson(): ByteArray {
        val people = repository.getAllPeopleRaw()
        val categories = repository.getAllCategoriesRaw()
        val transactions = repository.getAllTransactionsRaw()
        val payload = BackupPayload(
            people = people,
            categories = categories,
            transactions = transactions
        )
        return gson.toJson(payload).toByteArray(Charsets.UTF_8)
    }

    suspend fun exportTo(context: Context, uri: Uri): RestoreResult {
        val people = repository.getAllPeopleRaw()
        val categories = repository.getAllCategoriesRaw()
        val transactions = repository.getAllTransactionsRaw()

        val payload = BackupPayload(
            people = people,
            categories = categories,
            transactions = transactions
        )
        val json = gson.toJson(payload)

        try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: throw BackupException.WriteFailed("Could not open the selected file for writing")
        } catch (e: Exception) {
            throw BackupException.WriteFailed(e.message ?: "Backup failed")
        }

        return RestoreResult(people.size, categories.size, transactions.size)
    }

    suspend fun importFrom(context: Context, uri: Uri): RestoreResult {
        val text = try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            } ?: throw BackupException.InvalidFile("Could not open the selected file")
        } catch (e: Exception) {
            throw BackupException.InvalidFile(e.message ?: "Could not read the file")
        }

        val payload = try {
            gson.fromJson(text, BackupPayload::class.java)
        } catch (e: Exception) {
            throw BackupException.InvalidFile("This file is not a valid Simple Ledger backup")
        } ?: throw BackupException.InvalidFile("This file is not a valid Simple Ledger backup")

        repository.restoreData(payload.people, payload.categories, payload.transactions)

        return RestoreResult(payload.people.size, payload.categories.size, payload.transactions.size)
    }
}
