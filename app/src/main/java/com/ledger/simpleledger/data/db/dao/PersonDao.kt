package com.ledger.simpleledger.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ledger.simpleledger.data.db.entities.PersonEntity
import kotlinx.coroutines.flow.Flow

data class PersonWithTotals(
    val id: Long,
    val name: String,
    val phone: String?,
    val notes: String?,
    val totalLiyaMinor: Long,
    val totalDiyaMinor: Long
) {
    val balanceMinor: Long get() = totalLiyaMinor - totalDiyaMinor
}

@Dao
interface PersonDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(person: PersonEntity): Long

    @Update
    suspend fun update(person: PersonEntity)

    @Delete
    suspend fun delete(person: PersonEntity)

    @Query("SELECT * FROM people WHERE id = :id")
    suspend fun getById(id: Long): PersonEntity?

    @Query("SELECT * FROM people ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people WHERE name LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE ASC")
    fun search(query: String): Flow<List<PersonEntity>>

    @Query(
        """
        SELECT p.id as id, p.name as name, p.phone as phone, p.notes as notes,
        COALESCE(SUM(CASE WHEN t.type = 'LIYA' THEN t.amountMinor ELSE 0 END), 0) as totalLiyaMinor,
        COALESCE(SUM(CASE WHEN t.type = 'DIYA' THEN t.amountMinor ELSE 0 END), 0) as totalDiyaMinor
        FROM people p
        LEFT JOIN transactions t ON t.personId = p.id
        GROUP BY p.id
        ORDER BY p.name COLLATE NOCASE ASC
        """
    )
    fun observeAllWithTotals(): Flow<List<PersonWithTotals>>

    @Query(
        """
        SELECT p.id as id, p.name as name, p.phone as phone, p.notes as notes,
        COALESCE(SUM(CASE WHEN t.type = 'LIYA' THEN t.amountMinor ELSE 0 END), 0) as totalLiyaMinor,
        COALESCE(SUM(CASE WHEN t.type = 'DIYA' THEN t.amountMinor ELSE 0 END), 0) as totalDiyaMinor
        FROM people p
        LEFT JOIN transactions t ON t.personId = p.id
        WHERE p.id = :personId
        GROUP BY p.id
        """
    )
    fun observePersonWithTotals(personId: Long): Flow<PersonWithTotals?>

    @Query("SELECT * FROM people")
    suspend fun getAll(): List<PersonEntity>

    @Query("SELECT COUNT(*) FROM people")
    suspend fun count(): Int

    @Query("DELETE FROM people")
    suspend fun deleteAll()
}
