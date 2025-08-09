package app.cclauncher.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import app.cclauncher.data.repository.SettingsRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Simplified settings updater without reflection
 */
class SettingsUpdater(
    val dataStore: DataStore<Preferences>,
    val json: Json = Json { ignoreUnknownKeys = true }
) {

    /**
     * Update a boolean setting
     */
    suspend fun updateBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { prefs -> prefs[key] = value }
    }

    /**
     * Update an int setting
     */
    suspend fun updateInt(key: Preferences.Key<Int>, value: Int) {
        dataStore.edit { prefs -> prefs[key] = value }
    }

    /**
     * Update a float setting
     */
    suspend fun updateFloat(key: Preferences.Key<Float>, value: Float) {
        dataStore.edit { prefs -> prefs[key] = value }
    }

    /**
     * Update a string setting
     */
    suspend fun updateString(key: Preferences.Key<String>, value: String) {
        dataStore.edit { prefs -> prefs[key] = value }
    }

    /**
     * Update a long setting
     */
    suspend fun updateLong(key: Preferences.Key<Long>, value: Long) {
        dataStore.edit { prefs -> prefs[key] = value }
    }

    /**
     * Update a string set setting
     */
    suspend fun updateStringSet(key: Preferences.Key<Set<String>>, value: Set<String>) {
        dataStore.edit { prefs -> prefs[key] = value }
    }

    /**
     * Update JSON-serialized setting
     */
    suspend inline fun <reified T> updateJson(key: Preferences.Key<String>, value: T) {
        dataStore.edit { prefs ->
            prefs[key] = json.encodeToString(value)
        }
    }

    /**
     * Batch update multiple settings
     */
    suspend fun updateMultiple(updates: suspend (MutablePreferences) -> Unit) {
        dataStore.edit { prefs ->
            updates(prefs)
        }
    }
}