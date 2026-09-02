package com.ledger.simpleledger.data

import android.content.Context

class SettingsPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("simple_ledger_prefs", Context.MODE_PRIVATE)

    var lastBackupAt: Long
        get() = prefs.getLong(KEY_LAST_BACKUP, -1L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKUP, value).apply()

    var darkModeOverride: String
        get() = prefs.getString(KEY_DARK_MODE, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_DARK_MODE, value).apply()

    var defaultCurrency: String
        get() = prefs.getString(KEY_CURRENCY, "PKR") ?: "PKR"
        set(value) = prefs.edit().putString(KEY_CURRENCY, value).apply()

    companion object {
        private const val KEY_LAST_BACKUP = "last_backup_at"
        private const val KEY_DARK_MODE = "dark_mode_override" // "system" | "light" | "dark"
        private const val KEY_CURRENCY = "default_currency"
    }
}
