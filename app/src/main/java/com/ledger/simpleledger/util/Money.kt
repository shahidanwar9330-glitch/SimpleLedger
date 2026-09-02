package com.ledger.simpleledger.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * All money math in this app happens on Long "minor units" (value * 100) so we never touch
 * Float/Double for financial amounts. These helpers are the only place decimal <-> minor
 * conversion happens.
 */
object Money {
    private const val MINOR_UNITS_PER_MAJOR = 100L

    /** Parses a user-typed amount string (e.g. "1500", "1500.50") into safe minor units.
     * Returns null if the string is not a valid positive amount. */
    fun parseToMinorUnits(input: String): Long? {
        val cleaned = input.trim().replace(",", "")
        if (cleaned.isEmpty()) return null
        return try {
            val decimal = BigDecimal(cleaned)
            if (decimal.signum() <= 0) return null
            decimal.multiply(BigDecimal(MINOR_UNITS_PER_MAJOR))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
        } catch (e: Exception) {
            null
        }
    }

    fun minorToMajor(minor: Long): BigDecimal =
        BigDecimal(minor).divide(BigDecimal(MINOR_UNITS_PER_MAJOR))

    /** Formats minor units as a display string, e.g. 15000000 -> "Rs 150,000" (no decimals when whole). */
    fun format(minor: Long, currency: String = "PKR"): String {
        val major = minorToMajor(minor)
        val symbol = when (currency.uppercase(Locale.US)) {
            "PKR" -> "Rs "
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "AED" -> "AED "
            "SAR" -> "SAR "
            else -> "$currency "
        }
        val isWhole = major.stripTrailingZeros().scale() <= 0
        val nf = NumberFormat.getNumberInstance(Locale.US)
        return if (isWhole) {
            nf.maximumFractionDigits = 0
            symbol + nf.format(major)
        } else {
            nf.minimumFractionDigits = 2
            nf.maximumFractionDigits = 2
            symbol + nf.format(major)
        }
    }

    fun formatSigned(minor: Long, currency: String = "PKR"): String {
        val prefix = if (minor >= 0) "+" else "-"
        return prefix + format(kotlin.math.abs(minor), currency)
    }
}
