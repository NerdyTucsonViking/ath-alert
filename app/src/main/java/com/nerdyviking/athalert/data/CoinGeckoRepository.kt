package com.nerdyviking.athalert.data

import com.nerdyviking.athalert.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
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
        val symbol = coinId.trim().uppercase()
        if (symbol.isBlank()) {
            throw IOException("Enter a CoinMarketCap symbol like BTC, ETH, SOL, or LCAI.")
        }

        val url = "https://pro-api.coinmarketcap.com/v1/cryptocurrency/quotes/latest"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("symbol", symbol)
            .addQueryParameter("convert", "USD")
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json")
            .addHeader("X-CMC_PRO_API_KEY", BuildConfig.CMC_API_KEY)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")

            val body = response.body?.string().orEmpty()
            val root = JSONObject(body)

            val status = root.optJSONObject("status")
            val errorCode = status?.optInt("error_code", 0) ?: 0
            val errorMessage = status?.optString("error_message", "") ?: ""

            if (errorCode != 0) {
                throw IOException(errorMessage.ifBlank { "CoinMarketCap API error $errorCode" })
            }

            val data = root.optJSONObject("data")
                ?: throw IOException("No data returned from CoinMarketCap.")

            val coinObject = data.optJSONObject(symbol)
                ?: throw IOException("Coin not found. Use the CoinMarketCap symbol, like BTC, ETH, SOL, or LCAI.")

            val quoteUsd = coinObject
                .optJSONObject("quote")
                ?.optJSONObject("USD")
                ?: throw IOException("USD quote missing from CoinMarketCap response.")

            val name = coinObject.optString("name", symbol)
            val returnedSymbol = coinObject.optString("symbol", symbol).uppercase()
            val currentPrice = quoteUsd.optDouble("price", 0.0)

            if (currentPrice <= 0.0) {
                throw IOException("CoinMarketCap returned an invalid price for $returnedSymbol.")
            }

            return MarketSnapshot(
                name = name,
                symbol = returnedSymbol,
                currentPrice = currentPrice,
                ath = currentPrice
            )
        }
    }
}
