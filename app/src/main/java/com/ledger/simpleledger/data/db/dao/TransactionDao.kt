package com.ledger.simpleledger.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ledger.simpleledger.data.db.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class TransactionWithPerson(
    val id: Long,
    val personId: Long,
    val personName: String,
    val type: String,
    val amountMinor: Long,
    val currency: String,
    val categoryName: String,
    val date: Long,
    val note: String?,
    val paymentMethod: String?,
    val reference: String?
)

data class CategoryTotal(
    val categoryName: String,
    val totalMinor: Long
)

data class PeriodTotals(
    val totalLiyaMinor: Long,
    val totalDiyaMinor: Long
)

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query(
        """
        SELECT t.id as id, t.personId as personId, p.name as personName, t.type as type,
        t.amountMinor as amountMinor, t.currency as currency, t.categoryName as categoryName,
        t.date as date, t.note as note, t.paymentMethod as paymentMethod, t.reference as reference
        FROM transactions t INNER JOIN people p ON p.id = t.personId
        ORDER BY t.date DESC, t.id DESC LIMIT :limit
        """
    )
    fun observeRecent(limit: Int = 20): Flow<List<TransactionWithPerson>>

    @Query(
        """
        SELECT t.id as id, t.personId as personId, p.name as personName, t.type as type,
        t.amountMinor as amountMinor, t.currency as currency, t.categoryName as categoryName,
        t.date as date, t.note as note, t.paymentMethod as paymentMethod, t.reference as reference
        FROM transactions t INNER JOIN people p ON p.id = t.personId
        WHERE
          (:typeFilter IS NULL OR t.type = :typeFilter)
          AND (:categoryFilter IS NULL OR t.categoryName = :categoryFilter)
          AND (:personId IS NULL OR t.personId = :personId)
          AND (:startDate IS NULL OR t.date >= :startDate)
          AND (:endDate IS NULL OR t.date <= :endDate)
          AND (
            :query IS NULL OR :query = '' OR
            p.name LIKE '%' || :query || '%' OR
            t.categoryName LIKE '%' || :query || '%' OR
            t.reference LIKE '%' || :query || '%' OR
            t.note LIKE '%' || :query || '%'
          )
        ORDER BY t.date DESC, t.id DESC
        """
    )
    fun search(
        typeFilter: String?,
        categoryFilter: String?,
        personId: Long?,
        startDate: Long?,
        endDate: Long?,
        query: String?
    ): Flow<List<TransactionWithPerson>>

    @Query(
        """
        SELECT * FROM transactions WHERE personId = :personId ORDER BY date DESC, id DESC
        """
    )
    fun observeByPerson(personId: Long): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT
        COALESCE(SUM(CASE WHEN type = 'LIYA' THEN amountMinor ELSE 0 END), 0) as totalLiyaMinor,
        COALESCE(SUM(CASE WHEN type = 'DIYA' THEN amountMinor ELSE 0 END), 0) as totalDiyaMinor
        FROM transactions
        """
    )
    fun observeGrandTotals(): Flow<PeriodTotals>

    @Query(
        """
        SELECT
        COALESCE(SUM(CASE WHEN type = 'LIYA' THEN amountMinor ELSE 0 END), 0) as totalLiyaMinor,
        COALESCE(SUM(CASE WHEN type = 'DIYA' THEN amountMinor ELSE 0 END), 0) as totalDiyaMinor
        FROM transactions
        WHERE date >= :startDate AND date <= :endDate
        """
    )
    suspend fun getPeriodTotals(startDate: Long, endDate: Long): PeriodTotals

    @Query(
        """
        SELECT categoryName, COALESCE(SUM(amountMinor), 0) as totalMinor
        FROM transactions
        WHERE date >= :startDate AND date <= :endDate
        GROUP BY categoryName
        ORDER BY totalMinor DESC
        """
    )
    suspend fun getCategoryTotals(startDate: Long, endDate: Long): List<CategoryTotal>

    @Query("SELECT * FROM transactions")
    suspend fun getAll(): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
