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
    val iconUrl: String,
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

            val iconUrl = item.optString("icon_public_url")

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
                    iconUrl = iconUrl,
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

data class StreamProfile(
    val uuid: String,
    val name: String,
)

data class TvhEpgEvent(
    val channelUuid: String,
    val title: String,
    val subtitle: String,
    val start: Long,
    val stop: Long,
)

data class ProfileLoadResult(
    val profiles: List<StreamProfile> = emptyList(),
    val error: String? = null,
)

data class EpgLoadResult(
    val events: List<TvhEpgEvent> = emptyList(),
    val error: String? = null,
)

fun loadTvhProfiles(
    serverUrl: String,
    username: String,
    password: String,
): ProfileLoadResult {
    return try {
        val json = requestJson(
            "$serverUrl/api/profile/list",
            username,
            password,
        )

        val root = JSONObject(json)
        val entries = root.optJSONArray("entries") ?: JSONArray()

        val profiles = buildList {
            for (index in 0 until entries.length()) {
                val item = entries.optJSONObject(index) ?: continue

                val uuid = item.optString("uuid")
                    .ifBlank { item.optString("key") }

                val name = item.optString("name")
                    .ifBlank { item.optString("val") }

                if (uuid.isNotBlank() && name.isNotBlank()) {
                    add(StreamProfile(uuid, name))
                }
            }
        }

        ProfileLoadResult(
            profiles = if (profiles.isNotEmpty()) profiles else defaultProfiles(),
        )
    } catch (error: Exception) {
        ProfileLoadResult(
            profiles = defaultProfiles(),
            error = error.message ?: error.javaClass.simpleName,
        )
    }
}

fun loadCurrentEpg(
    serverUrl: String,
    username: String,
    password: String,
): EpgLoadResult {
    return try {
        val json = requestJson(
            "$serverUrl/api/epg/events/grid?limit=3000&start=0&mode=now",
            username,
            password,
        )

        val root = JSONObject(json)
        val entries = root.optJSONArray("entries") ?: JSONArray()

        val events = buildList {
            for (index in 0 until entries.length()) {
                val item = entries.optJSONObject(index) ?: continue

                val channelUuid = item.optString("channelUuid")
                    .ifBlank { item.optString("channel") }

                val title = item.optString("title")
                val subtitle = item.optString("subtitle")

                if (channelUuid.isBlank() || title.isBlank()) {
                    continue
                }

                add(
                    TvhEpgEvent(
                        channelUuid = channelUuid,
                        title = title,
                        subtitle = subtitle,
                        start = item.optLong("start"),
                        stop = item.optLong("stop"),
                    )
                )
            }
        }

        EpgLoadResult(events = events)
    } catch (error: Exception) {
        EpgLoadResult(
            error = error.message ?: error.javaClass.simpleName,
        )
    }
}


fun loadGuideEpg(
    serverUrl: String,
    username: String,
    password: String,
): EpgLoadResult {
    return try {
        val json = requestJson(
            "$serverUrl/api/epg/events/grid?limit=5000&start=0",
            username,
            password,
        )

        val root = JSONObject(json)
        val entries = root.optJSONArray("entries") ?: JSONArray()

        val events = buildList {
            for (index in 0 until entries.length()) {
                val item = entries.optJSONObject(index) ?: continue

                val channelUuid = item.optString("channelUuid")
                    .ifBlank { item.optString("channel") }

                val title = item.optString("title")
                val subtitle = item.optString("subtitle")

                if (channelUuid.isBlank() || title.isBlank()) {
                    continue
                }

                add(
                    TvhEpgEvent(
                        channelUuid = channelUuid,
                        title = title,
                        subtitle = subtitle,
                        start = item.optLong("start"),
                        stop = item.optLong("stop"),
                    )
                )
            }
        }

        EpgLoadResult(events = events)
    } catch (error: Exception) {
        EpgLoadResult(
            error = error.message ?: error.javaClass.simpleName,
        )
    }
}

private fun defaultProfiles(): List<StreamProfile> {
    return listOf(
        StreamProfile("pass", "pass (원본)"),
        StreamProfile("htsp", "htsp (기본)"),
    )
}
