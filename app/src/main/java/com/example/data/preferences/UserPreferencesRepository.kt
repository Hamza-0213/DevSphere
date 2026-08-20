package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "docsphere_settings")

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class SortOption {
    NAME, DATE, SIZE, TYPE
}

data class UserSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val sortBy: SortOption = SortOption.DATE,
    val sortAscending: Boolean = false,
    val enableAnimations: Boolean = true,
    val textReaderTheme: String = "LIGHT",
    val textFontSize: Int = 16,
    val hasInitializedSamples: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SORT_BY = stringPreferencesKey("sort_by")
        val SORT_ASCENDING = booleanPreferencesKey("sort_ascending")
        val ENABLE_ANIMATIONS = booleanPreferencesKey("enable_animations")
        val TEXT_READER_THEME = stringPreferencesKey("text_reader_theme")
        val TEXT_FONT_SIZE = intPreferencesKey("text_font_size")
        val HAS_INITIALIZED_SAMPLES = booleanPreferencesKey("has_initialized_samples")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        val themeStr = preferences[PreferencesKeys.THEME_MODE] ?: AppThemeMode.SYSTEM.name
        val themeMode = try {
            AppThemeMode.valueOf(themeStr)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }

        val sortStr = preferences[PreferencesKeys.SORT_BY] ?: SortOption.DATE.name
        val sortBy = try {
            SortOption.valueOf(sortStr)
        } catch (e: Exception) {
            SortOption.DATE
        }

        val sortAscending = preferences[PreferencesKeys.SORT_ASCENDING] ?: false
        val enableAnimations = preferences[PreferencesKeys.ENABLE_ANIMATIONS] ?: true
        val textReaderTheme = preferences[PreferencesKeys.TEXT_READER_THEME] ?: "LIGHT"
        val textFontSize = preferences[PreferencesKeys.TEXT_FONT_SIZE] ?: 16
        val hasInitializedSamples = preferences[PreferencesKeys.HAS_INITIALIZED_SAMPLES] ?: false

        UserSettings(
            themeMode = themeMode,
            sortBy = sortBy,
            sortAscending = sortAscending,
            enableAnimations = enableAnimations,
            textReaderTheme = textReaderTheme,
            textFontSize = textFontSize,
            hasInitializedSamples = hasInitializedSamples
        )
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun setSortOption(sortOption: SortOption, ascending: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SORT_BY] = sortOption.name
            preferences[PreferencesKeys.SORT_ASCENDING] = ascending
        }
    }

    suspend fun setEnableAnimations(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_ANIMATIONS] = enabled
        }
    }

    suspend fun setTextReaderTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TEXT_READER_THEME] = theme
        }
    }

    suspend fun setTextFontSize(fontSize: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TEXT_FONT_SIZE] = fontSize.coerceIn(12, 32)
        }
    }

    suspend fun setHasInitializedSamples(initialized: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_INITIALIZED_SAMPLES] = initialized
        }
    }

    suspend fun resetAllSettings() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
