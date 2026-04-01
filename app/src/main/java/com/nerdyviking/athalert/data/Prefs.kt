package com.nerdyviking.athalert.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ath_alert_prefs")

data class AppPrefs(
    val coinId: String = "BTC",
    val coinLabel: String = "Bitcoin",
    val notificationsEnabled: Boolean = true,
    val initializedAth: Double = 0.0,
    val lastNotifiedAth: Double = 0.0,
    val lastSeenPrice: Double = 0.0,
    val lastStatus: String = "Waiting for first check"
)

class PrefsRepository(private val context: Context) {
    private object Keys {
        val COIN_ID = stringPreferencesKey("coin_id")
        val COIN_LABEL = stringPreferencesKey("coin_label")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val INITIALIZED_ATH = doublePreferencesKey("initialized_ath")
        val LAST_NOTIFIED_ATH = doublePreferencesKey("last_notified_ath")
        val LAST_SEEN_PRICE = doublePreferencesKey("last_seen_price")
        val LAST_STATUS = stringPreferencesKey("last_status")
    }

    val prefsFlow: Flow<AppPrefs> = context.dataStore.data.map { prefs ->
        AppPrefs(
            coinId = prefs[Keys.COIN_ID] ?: "BTC",
            coinLabel = prefs[Keys.COIN_LABEL] ?: "Bitcoin",
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            initializedAth = prefs[Keys.INITIALIZED_ATH] ?: 0.0,
            lastNotifiedAth = prefs[Keys.LAST_NOTIFIED_ATH] ?: 0.0,
            lastSeenPrice = prefs[Keys.LAST_SEEN_PRICE] ?: 0.0,
            lastStatus = prefs[Keys.LAST_STATUS] ?: "Waiting for first check"
        )
    }

    suspend fun saveCoin(coinId: String, label: String) {
    val cleanedSymbol = coinId.trim().uppercase()
    context.dataStore.edit {
        it[Keys.COIN_ID] = cleanedSymbol
        it[Keys.COIN_LABEL] = label.trim().ifBlank { cleanedSymbol }
        it[Keys.LAST_STATUS] = "Saved ${label.ifBlank { cleanedSymbol }}. Tap Start Tracking."
    }
}

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit {
            it[Keys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun updateMarketState(
        apiAth: Double,
        currentPrice: Double,
        status: String,
        initializeOnly: Boolean = false
    ) {
        context.dataStore.edit {
            val currentInitialized = it[Keys.INITIALIZED_ATH] ?: 0.0
            val currentNotified = it[Keys.LAST_NOTIFIED_ATH] ?: 0.0
            if (currentInitialized <= 0.0 || initializeOnly) {
                it[Keys.INITIALIZED_ATH] = apiAth
                if (currentNotified <= 0.0) {
                    it[Keys.LAST_NOTIFIED_ATH] = apiAth
                }
            }
            it[Keys.LAST_SEEN_PRICE] = currentPrice
            it[Keys.LAST_STATUS] = status
        }
    }

    suspend fun markAthNotified(newAth: Double, status: String, currentPrice: Double) {
        context.dataStore.edit {
            it[Keys.INITIALIZED_ATH] = newAth
            it[Keys.LAST_NOTIFIED_ATH] = newAth
            it[Keys.LAST_SEEN_PRICE] = currentPrice
            it[Keys.LAST_STATUS] = status
        }
    }

    suspend fun setStatus(status: String) {
        context.dataStore.edit {
            it[Keys.LAST_STATUS] = status
        }
    }

    suspend fun currentPrefs(): AppPrefs = prefsFlow.first()
}
