package com.kallos.tvhclienttv

import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
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
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    var statusMessage by remember {
        mutableStateOf("스트림을 연결하는 중입니다.")
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
            val credentials = "$username:$password"
            val encodedCredentials = Base64.encodeToString(
                credentials.toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP,
            )

            httpFactory.setDefaultRequestProperties(
                mapOf("Authorization" to "Basic $encodedCredentials")
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

                addListener(
                    object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            statusMessage = when (playbackState) {
                                Player.STATE_BUFFERING -> "버퍼링 중입니다."
                                Player.STATE_READY -> "재생 중입니다."
                                Player.STATE_ENDED -> "재생이 종료되었습니다."
                                else -> "스트림을 연결하는 중입니다."
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            statusMessage = "재생 오류: ${error.errorCodeName}"
                        }
                    }
                )
            }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    BackHandler {
        onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    this.player = player
                    useController = true
                    controllerAutoShow = true
                    controllerShowTimeoutMs = 4_000
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                }
            },
            update = {
                it.player = player
            },
            modifier = Modifier.fillMaxSize(),
        )

        Text(
            text = "${formatChannelNumber(channel.number)}  ${channel.name}  |  $statusMessage",
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(horizontal = 24.dp, vertical = 12.dp),
        )
    }
}
