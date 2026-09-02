package com.ledger.simpleledger.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isDefault: Boolean = false
)

object DefaultCategories {
    val NAMES = listOf(
        "Payment", "Purchase", "Sale", "Loan", "Advance",
        "Refund", "Commission", "Expense", "Investment", "Other"
    )
}
