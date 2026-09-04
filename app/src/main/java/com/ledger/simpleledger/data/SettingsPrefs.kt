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

    /** The GitHub release "build number" (from tag build-N) currently installed on this
     * device. Defaults to 0 so the very first update check always finds something newer. */
    var installedBuildNumber: Int
        get() = prefs.getInt(KEY_INSTALLED_BUILD, 0)
        set(value) = prefs.edit().putInt(KEY_INSTALLED_BUILD, value).apply()

    var lastDriveBackupAt: Long
        get() = prefs.getLong(KEY_LAST_DRIVE_BACKUP, -1L)
        set(value) = prefs.edit().putLong(KEY_LAST_DRIVE_BACKUP, value).apply()

    companion object {
        private const val KEY_LAST_BACKUP = "last_backup_at"
        private const val KEY_DARK_MODE = "dark_mode_override" // "system" | "light" | "dark"
        private const val KEY_CURRENCY = "default_currency"
        private const val KEY_INSTALLED_BUILD = "installed_build_number"
        private const val KEY_LAST_DRIVE_BACKUP = "last_drive_backup_at"
    }
}
