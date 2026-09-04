package com.tokoaksesoris.kasir.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeHelper {
    private const val PREF_NAME = "pengaturan"
    private const val KEY_DARK = "dark_mode"

    fun applySavedTheme(context: Context) {
        val isDark = isDarkMode(context)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun toggleTheme(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val newValue = !isDarkMode(context)
        prefs.edit().putBoolean(KEY_DARK, newValue).apply()
        AppCompatDelegate.setDefaultNightMode(
            if (newValue) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun isDarkMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DARK, false)
    }
}
