package com.nerdyviking.athalert.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nerdyviking.athalert.data.CoinGeckoRepository
import com.nerdyviking.athalert.data.PrefsRepository

class AthWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val prefs = PrefsRepository(appContext)
    private val repo = CoinGeckoRepository()

    override suspend fun doWork(): Result {
        return try {
            val current = prefs.currentPrefs()
            val snapshot = repo.fetchMarketSnapshot(current.coinId)
            val label = if (snapshot.symbol.isNotBlank()) {
                "${snapshot.name} (${snapshot.symbol})"
            } else {
                snapshot.name
            }

            if (current.initializedAth <= 0.0) {
                prefs.updateMarketState(
                    apiAth = snapshot.ath,
                    currentPrice = snapshot.currentPrice,
                    status = "Initialized at ATH $${"%.2f".format(snapshot.ath)} for $label.",
                    initializeOnly = true
                )
                return Result.success()
            }

            val hasNewAth = snapshot.ath > current.lastNotifiedAth && snapshot.ath > 0.0
            if (hasNewAth && current.notificationsEnabled) {
                NotificationHelper.createChannel(applicationContext)
                NotificationHelper.showAthNotification(
                    applicationContext,
                    title = "$label hit a new ATH",
                    body = "New ATH: $${"%.2f".format(snapshot.ath)} | Current: $${"%.2f".format(snapshot.currentPrice)}"
                )
                prefs.markAthNotified(
                    newAth = snapshot.ath,
                    status = "New ATH detected for $label: $${"%.2f".format(snapshot.ath)}",
                    currentPrice = snapshot.currentPrice
                )
            } else {
                prefs.updateMarketState(
                    apiAth = snapshot.ath,
                    currentPrice = snapshot.currentPrice,
                    status = "Checked $label. Price: $${"%.2f".format(snapshot.currentPrice)} | ATH: $${"%.2f".format(snapshot.ath)}"
                )
            }
            Result.success()
        } catch (e: Exception) {
            prefs.setStatus("Check failed: ${e.message}")
            Result.retry()
        }
    }
}
