package com.ledger.simpleledger.util

import androidx.compose.ui.graphics.Color
import com.ledger.simpleledger.ui.theme.AvatarPalette

object AvatarUtil {

    /** Up to 2-letter initials from a name, e.g. "1st floor" -> "1F", "Ali" -> "A". */
    fun initialsFor(name: String): String {
        val words = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return "?"
        if (words.size == 1) {
            val w = words[0]
            return if (w.length >= 2) w.substring(0, 2).uppercase() else w.uppercase()
        }
        return (words[0].take(1) + words[1].take(1)).uppercase()
    }

    /** Deterministic color per name so the same person always gets the same avatar color. */
    fun colorFor(name: String): Color {
        val index = kotlin.math.abs(name.trim().lowercase().hashCode()) % AvatarPalette.size
        return AvatarPalette[index]
    }
}
