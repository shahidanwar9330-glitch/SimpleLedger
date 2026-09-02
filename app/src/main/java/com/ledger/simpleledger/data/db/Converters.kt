package com.ledger.simpleledger.data.db

import androidx.room.TypeConverter
import com.ledger.simpleledger.data.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
}
