package com.nerdyviking.athalert.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException

class CoinGeckoRepository {
    private val client = OkHttpClient()

    data class MarketSnapshot(
        val name: String,
        val symbol: String,
        val currentPrice: Double,
        val ath: Double
    )

    @Throws(IOException::class)
    fun fetchMarketSnapshot(coinId: String): MarketSnapshot {
        val url = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&ids=$coinId"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body?.string().orEmpty()
            val json = JSONArray(body)
            if (json.length() == 0) throw IOException("Coin not found. Use the CoinGecko coin id, like bitcoin or ethereum.")
            val item = json.getJSONObject(0)
            return MarketSnapshot(
                name = item.optString("name", coinId),
                symbol = item.optString("symbol", "").uppercase(),
                currentPrice = item.optDouble("current_price", 0.0),
                ath = item.optDouble("ath", 0.0)
            )
        }
    }
}
