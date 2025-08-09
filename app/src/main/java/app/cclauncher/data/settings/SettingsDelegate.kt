package app.cclauncher.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.reflect.KProperty

/**
 * Delegate for DataStore preferences to reduce boilerplate
 */
class SettingsDelegate<T>(
    private val dataStore: DataStore<Preferences>,
    private val key: Preferences.Key<T>,
    private val defaultValue: T
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Flow<T> {
        return dataStore.data.map { preferences ->
            preferences[key] ?: defaultValue
        }
    }

    suspend fun setValue(value: T) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }
}

/**
 * Helper class to create delegates for different types
 */
class PreferencesDelegateProvider(private val dataStore: DataStore<Preferences>) {

    fun boolean(key: Preferences.Key<Boolean>, default: Boolean = false) =
        SettingsDelegate(dataStore, key, default)

    fun int(key: Preferences.Key<Int>, default: Int = 0) =
        SettingsDelegate(dataStore, key, default)

    fun float(key: Preferences.Key<Float>, default: Float = 0f) =
        SettingsDelegate(dataStore, key, default)

    fun long(key: Preferences.Key<Long>, default: Long = 0L) =
        SettingsDelegate(dataStore, key, default)

    fun string(key: Preferences.Key<String>, default: String = "") =
        SettingsDelegate(dataStore, key, default)

    fun stringSet(key: Preferences.Key<Set<String>>, default: Set<String> = emptySet()) =
        SettingsDelegate(dataStore, key, default)
}