package com.example.dsbalance.wear

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Balance(
    val available: Boolean,
    val currency: String,
    val total: String,
    val granted: String,
    val toppedUp: String,
)

/**
 * 调用 DeepSeek 官方接口查询余额。
 * GET https://api.deepseek.com/user/balance
 */
object BalanceApi {

    fun query(apiKey: String): Result<Balance> {
        return try {
            val url = URL("https://api.deepseek.com/user/balance")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.setRequestProperty("Accept", "application/json")

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            conn.disconnect()

            if (code !in 200..299) {
                return Result.failure(
                    Exception(
                        when (code) {
                            401 -> "认证失败(401)：API Key 无效或已吊销"
                            402 -> "余额不足(402)：账户欠费"
                            429 -> "请求过于频繁(429)：稍后再试"
                            else -> "HTTP $code：$body"
                        }
                    )
                )
            }

            val json = JSONObject(body)
            val arr = json.optJSONArray("balance_infos")
            val o = if (arr != null && arr.length() > 0) arr.getJSONObject(0) else JSONObject()
            Result.success(
                Balance(
                    available = json.optBoolean("is_available", false),
                    currency = o.optString("currency", "CNY"),
                    total = o.optString("total_balance", "-"),
                    granted = o.optString("granted_balance", "-"),
                    toppedUp = o.optString("topped_up_balance", "-"),
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
