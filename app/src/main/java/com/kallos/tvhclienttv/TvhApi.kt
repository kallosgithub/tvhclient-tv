package com.kallos.tvhclienttv

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class TvhChannel(
    val uuid: String,
    val name: String,
    val number: String,
    val tagIds: List<String>,
)

data class TvhTag(
    val uuid: String,
    val name: String,
)

data class ChannelLoadResult(
    val channels: List<TvhChannel> = emptyList(),
    val tags: List<TvhTag> = emptyList(),
    val error: String? = null,
)

fun loadTvhChannels(
    serverUrl: String,
    username: String,
    password: String,
): ChannelLoadResult {
    return try {
        val channelJson = requestJson(
            "$serverUrl/api/channel/grid?limit=2000&start=0",
            username,
            password,
        )

        val tagJson = requestJson(
            "$serverUrl/api/channeltag/grid?limit=500&start=0",
            username,
            password,
        )

        val channels = parseChannels(channelJson)
        val tags = parseTags(tagJson)

        ChannelLoadResult(
            channels = channels.sortedWith(compareBy<TvhChannel> { channelSortKey(it.number) }.thenBy { it.name }),
            tags = tags.sortedBy { it.name },
        )
    } catch (error: Exception) {
        ChannelLoadResult(
            error = error.message ?: error.javaClass.simpleName,
        )
    }
}

private fun requestJson(
    address: String,
    username: String,
    password: String,
): String {
    val connection = URL(address).openConnection() as HttpURLConnection

    connection.requestMethod = "GET"
    connection.connectTimeout = 10_000
    connection.readTimeout = 10_000
    connection.setRequestProperty("Accept", "application/json")

    if (username.isNotBlank()) {
        val credentials = "$username:$password"
        val encoded = Base64.encodeToString(
            credentials.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP,
        )
        connection.setRequestProperty("Authorization", "Basic $encoded")
    }

    val responseCode = connection.responseCode

    if (responseCode !in 200..299) {
        connection.disconnect()
        throw IllegalStateException("TVHeadend HTTP 응답 코드 $responseCode")
    }

    val body = connection.inputStream.bufferedReader().use { it.readText() }
    connection.disconnect()

    return body
}

private fun parseChannels(json: String): List<TvhChannel> {
    val root = JSONObject(json)
    val entries = root.optJSONArray("entries") ?: JSONArray()

    return buildList {
        for (index in 0 until entries.length()) {
            val item = entries.optJSONObject(index) ?: continue

            val uuid = item.optString("uuid")
            val name = item.optString("name")
            val number = item.optString("number")

            if (uuid.isBlank() || name.isBlank()) {
                continue
            }

            val tagIds = buildList {
                val tags = item.optJSONArray("tags") ?: JSONArray()

                for (tagIndex in 0 until tags.length()) {
                    val tagId = tags.optString(tagIndex)

                    if (tagId.isNotBlank()) {
                        add(tagId)
                    }
                }
            }

            add(
                TvhChannel(
                    uuid = uuid,
                    name = name,
                    number = number,
                    tagIds = tagIds,
                )
            )
        }
    }
}

private fun parseTags(json: String): List<TvhTag> {
    val root = JSONObject(json)
    val entries = root.optJSONArray("entries") ?: JSONArray()

    return buildList {
        for (index in 0 until entries.length()) {
            val item = entries.optJSONObject(index) ?: continue

            val uuid = item.optString("uuid")
            val name = item.optString("name")

            if (uuid.isNotBlank() && name.isNotBlank()) {
                add(
                    TvhTag(
                        uuid = uuid,
                        name = name,
                    )
                )
            }
        }
    }
}

private fun channelSortKey(number: String): Int {
    val parts = number
        .replace("-", ".")
        .split(".")
        .mapNotNull { it.toIntOrNull() }

    if (parts.isEmpty()) {
        return Int.MAX_VALUE
    }

    val main = parts.getOrElse(0) { 0 }
    val sub = parts.getOrElse(1) { 0 }

    return (main * 1000) + sub
}
