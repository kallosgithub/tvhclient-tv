package com.kallos.tvhclienttv

import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import java.net.URLEncoder

@Composable
fun PlayerScreen(
    channel: TvhChannel,
    serverUrl: String,
    username: String,
    password: String,
    profileId: String,
    onProfileSelected: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    var profiles by remember { mutableStateOf<List<StreamProfile>>(emptyList()) }
    var showProfileMenu by remember { mutableStateOf(false) }

    LaunchedEffect(serverUrl, username, password) {
        Thread {
            val result = loadTvhProfiles(
                serverUrl = serverUrl,
                username = username,
                password = password,
            )

            context.mainExecutor.execute {
                profiles = result.profiles
            }
        }.start()
    }

    val streamUrl = remember(serverUrl, channel.uuid, profileId) {
        val baseUrl = serverUrl.trimEnd('/')
        val encodedProfile = URLEncoder.encode(profileId, "UTF-8")
        "$baseUrl/stream/channel/${channel.uuid}?profile=$encodedProfile"
    }

    val player = remember(streamUrl, username, password) {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(12_000)
            .setReadTimeoutMs(20_000)
            .setAllowCrossProtocolRedirects(true)

        if (username.isNotBlank()) {
            val encodedCredentials = Base64.encodeToString(
                "$username:$password".toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP,
            )

            httpFactory.setDefaultRequestProperties(
                mapOf("Authorization" to "Basic $encodedCredentials"),
            )
        }

        val mediaSource = ProgressiveMediaSource.Factory(httpFactory)
            .createMediaSource(MediaItem.fromUri(streamUrl))

        ExoPlayer.Builder(context)
            .build()
            .apply {
                setMediaSource(mediaSource)
                prepare()
                playWhenReady = true
            }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    BackHandler {
        if (showProfileMenu) {
            showProfileMenu = false
        } else {
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) {
                    showProfileMenu = true
                    true
                } else {
                    false
                }
            },
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    this.player = player
                    useController = false
                }
            },
            update = {
                it.player = player
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (showProfileMenu) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xDD0B1020))
                    .padding(48.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                androidx.compose.material3.Text(
                    text = "스트림 프로파일 선택",
                    color = Color.White,
                )

                profiles.forEach { profile ->
                    TvMenuButton(
                        text = if (profile.uuid == profileId) {
                            "✓ ${profile.name}"
                        } else {
                            profile.name
                        },
                        onClick = {
                            showProfileMenu = false
                            onProfileSelected(profile.uuid)
                        },
                    )
                }

                TvMenuButton(
                    text = "닫기",
                    onClick = {
                        showProfileMenu = false
                    },
                )
            }
        }
    }
}
