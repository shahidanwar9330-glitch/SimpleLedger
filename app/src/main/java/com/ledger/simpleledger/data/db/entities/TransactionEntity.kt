package com.ledger.simpleledger.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ledger.simpleledger.data.model.TransactionType

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("personId"),
        Index("type"),
        Index("date"),
        Index("categoryName")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personId: Long,
    val type: TransactionType,
    // Amount stored in MINOR units (e.g. paisa/cents) to avoid floating point errors.
    val amountMinor: Long,
    val currency: String = "PKR",
    val categoryName: String = "Other",
    val date: Long, // epoch millis, user-chosen transaction date
    val note: String? = null,
    val paymentMethod: String? = null,
    val reference: String? = null,
    val attachmentUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
