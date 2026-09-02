package com.ledger.simpleledger.data.repository

import com.ledger.simpleledger.data.db.dao.CategoryDao
import com.ledger.simpleledger.data.db.dao.CategoryTotal
import com.ledger.simpleledger.data.db.dao.PersonDao
import com.ledger.simpleledger.data.db.dao.PersonWithTotals
import com.ledger.simpleledger.data.db.dao.PeriodTotals
import com.ledger.simpleledger.data.db.dao.TransactionDao
import com.ledger.simpleledger.data.db.dao.TransactionWithPerson
import com.ledger.simpleledger.data.db.entities.CategoryEntity
import com.ledger.simpleledger.data.db.entities.DefaultCategories
import com.ledger.simpleledger.data.db.entities.PersonEntity
import com.ledger.simpleledger.data.db.entities.TransactionEntity
import com.ledger.simpleledger.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for all app data. UI layer (ViewModels) only talks to this class,
 * never directly to DAOs. This also makes it straightforward to later introduce a
 * cloud-sync layer (e.g. wrap these same methods with an API/WhatsApp bridge) without
 * touching the UI.
 */
class LedgerRepository(
    private val personDao: PersonDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) {
    // ---------- People ----------

    fun observePeople(): Flow<List<PersonEntity>> = personDao.observeAll()

    fun observePeopleWithTotals(): Flow<List<PersonWithTotals>> = personDao.observeAllWithTotals()

    fun observePersonWithTotals(personId: Long): Flow<PersonWithTotals?> =
        personDao.observePersonWithTotals(personId)

    fun searchPeople(query: String): Flow<List<PersonEntity>> = personDao.search(query)

    suspend fun getPerson(id: Long): PersonEntity? = personDao.getById(id)

    suspend fun addPerson(name: String, phone: String?, notes: String?): Long {
        require(name.isNotBlank()) { "Person name cannot be empty" }
        return personDao.insert(PersonEntity(name = name.trim(), phone = phone?.trim(), notes = notes?.trim()))
    }

    suspend fun updatePerson(person: PersonEntity) {
        require(person.name.isNotBlank()) { "Person name cannot be empty" }
        personDao.update(person.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deletePerson(person: PersonEntity) = personDao.delete(person)

    // ---------- Categories ----------

    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    suspend fun addCategory(name: String) {
        require(name.isNotBlank()) { "Category name cannot be empty" }
        categoryDao.insert(CategoryEntity(name = name.trim(), isDefault = false))
    }

    suspend fun deleteCategory(category: CategoryEntity) = categoryDao.delete(category)

    suspend fun ensureDefaultCategoriesSeeded() {
        if (categoryDao.count() == 0) {
            categoryDao.insertAll(DefaultCategories.NAMES.map { CategoryEntity(name = it, isDefault = true) })
        }
    }

    // ---------- Transactions ----------

    fun observeRecentTransactions(limit: Int = 20): Flow<List<TransactionWithPerson>> =
        transactionDao.observeRecent(limit)

    fun searchTransactions(
        type: TransactionType?,
        category: String?,
        personId: Long?,
        startDate: Long?,
        endDate: Long?,
        query: String?
    ): Flow<List<TransactionWithPerson>> = transactionDao.search(
        typeFilter = type?.name,
        categoryFilter = category,
        personId = personId,
        startDate = startDate,
        endDate = endDate,
        query = query
    )

    fun observeTransactionsForPerson(personId: Long): Flow<List<TransactionEntity>> =
        transactionDao.observeByPerson(personId)

    fun observeGrandTotals(): Flow<PeriodTotals> = transactionDao.observeGrandTotals()

    suspend fun getPeriodTotals(startDate: Long, endDate: Long): PeriodTotals =
        transactionDao.getPeriodTotals(startDate, endDate)

    suspend fun getCategoryTotals(startDate: Long, endDate: Long): List<CategoryTotal> =
        transactionDao.getCategoryTotals(startDate, endDate)

    suspend fun getTransaction(id: Long): TransactionEntity? = transactionDao.getById(id)

    suspend fun addTransaction(
        personId: Long,
        type: TransactionType,
        amountMinor: Long,
        currency: String,
        categoryName: String,
        date: Long,
        note: String?,
        paymentMethod: String?,
        reference: String?
    ): Long {
        require(amountMinor > 0) { "Amount must be greater than zero" }
        require(personId > 0) { "A person must be selected" }
        return transactionDao.insert(
            TransactionEntity(
                personId = personId,
                type = type,
                amountMinor = amountMinor,
                currency = currency.ifBlank { "PKR" },
                categoryName = categoryName.ifBlank { "Other" },
                date = date,
                note = note?.trim()?.ifBlank { null },
                paymentMethod = paymentMethod?.trim()?.ifBlank { null },
                reference = reference?.trim()?.ifBlank { null }
            )
        )
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        require(transaction.amountMinor > 0) { "Amount must be greater than zero" }
        transactionDao.update(transaction.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) = transactionDao.delete(transaction)

    // ---------- Backup / Restore raw access ----------

    suspend fun getAllPeopleRaw(): List<PersonEntity> = personDao.getAll()

    suspend fun getAllCategoriesRaw(): List<CategoryEntity> = categoryDao.getAll()

    suspend fun getAllTransactionsRaw(): List<TransactionEntity> = transactionDao.getAll()

    suspend fun restoreData(
        people: List<PersonEntity>,
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>
    ) {
        wipeAllData()
        // Insert people first and remember old-id -> new-id mapping, since autoGenerate
        // ids may differ from the backup file after a fresh insert.
        val idMap = HashMap<Long, Long>()
        for (p in people) {
            val newId = personDao.insert(p.copy(id = 0))
            idMap[p.id] = newId
        }
        for (c in categories) {
            categoryDao.insert(c.copy(id = 0))
        }
        for (t in transactions) {
            val mappedPersonId = idMap[t.personId] ?: continue
            transactionDao.insert(t.copy(id = 0, personId = mappedPersonId))
        }
    }

    suspend fun wipeAllData() {
        transactionDao.deleteAll()
        personDao.deleteAll()
        categoryDao.deleteAll()
    }
}
