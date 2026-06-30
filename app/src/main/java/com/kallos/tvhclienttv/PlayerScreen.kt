package com.kallos.tvhclienttv

import android.util.Base64
import android.view.KeyEvent as AndroidKeyEvent
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
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

    val profileMenuFocusRequester = remember { FocusRequester() }

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

    /*
     * preferences에는 TVHeadend 내부 UUID를 저장한다.
     * 실제 스트림 URL에는 TVHeadend 프로파일 이름(pass, androidtv-aac 등)을 사용한다.
     */
    val selectedProfile = profiles.firstOrNull {
        it.uuid == profileId || it.name == profileId
    }

    val selectedProfileName = selectedProfile?.name ?: profileId.ifBlank { "pass" }

    val streamUrl = remember(serverUrl, channel.uuid, selectedProfileName) {
        val baseUrl = serverUrl.trimEnd('/')
        val encodedProfile = URLEncoder.encode(selectedProfileName, "UTF-8")

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

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        ExoPlayer.Builder(context)
            .build()
            .apply {
                setAudioAttributes(audioAttributes, true)
                setHandleAudioBecomingNoisy(true)
                volume = 1f
                setMediaSource(mediaSource)
                prepare()
                playWhenReady = true
            }
    }

    DisposableEffect(player) {
        onDispose {
            player.stop()
            player.release()
        }
    }

    val selectedProfileIndex = profiles.indexOfFirst {
        it.uuid == profileId || it.name == selectedProfileName
    }.let {
        if (it >= 0) it else 0
    }

    LaunchedEffect(showProfileMenu, profiles, selectedProfileIndex) {
        if (showProfileMenu && profiles.isNotEmpty()) {
            profileMenuFocusRequester.requestFocus()
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
                    !showProfileMenu &&
                    event.type == KeyEventType.KeyDown &&
                    (
                        event.key == Key.DirectionCenter ||
                            event.key == Key.Enter
                        )
                ) {
                    showProfileMenu = true
                    true
                } else {
                    false
                }
            },
    ) {
        key(streamUrl) {
            AndroidView(
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        this.player = player
                        useController = false
                        setKeepContentOnPlayerReset(false)

                        isFocusable = true
                        isFocusableInTouchMode = true

                        setOnKeyListener { _, keyCode, event ->
                            if (
                                !showProfileMenu &&
                                event.action == AndroidKeyEvent.ACTION_DOWN &&
                                (
                                    keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
                                        keyCode == AndroidKeyEvent.KEYCODE_ENTER ||
                                        keyCode == AndroidKeyEvent.KEYCODE_MENU
                                    )
                            ) {
                                showProfileMenu = true
                                true
                            } else {
                                false
                            }
                        }
                    }
                },
                update = {
                    it.player = player
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(22.dp)
                .background(Color(0xB3111D2E))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            androidx.compose.material3.Text(
                text = "${formatChannelNumber(channel.number)}  ${channel.name}",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )

            androidx.compose.material3.Text(
                text = "현재 프로파일: $selectedProfileName",
                color = Color(0xFF65E6C5),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 3.dp),
            )
        }

        if (showProfileMenu) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE608101C))
                    .padding(horizontal = 64.dp, vertical = 52.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                androidx.compose.material3.Text(
                    text = "스트림 프로파일 선택",
                    color = Color.White,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                )

                androidx.compose.material3.Text(
                    text = "선택하면 현재 재생을 종료하고 새 프로파일로 다시 연결합니다.",
                    color = Color(0xFFB9C8D8),
                    fontSize = 14.sp,
                )

                if (profiles.isEmpty()) {
                    androidx.compose.material3.Text(
                        text = "프로파일 목록을 불러오는 중입니다.",
                        color = Color(0xFFB9C8D8),
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 18.dp),
                    )
                } else {
                    profiles.forEachIndexed { index, profile ->
                        TvMenuButton(
                            text = if (
                                profile.uuid == profileId ||
                                profile.name == selectedProfileName
                            ) {
                                "✓ ${profile.name}"
                            } else {
                                profile.name
                            },
                            modifier = if (index == selectedProfileIndex) {
                                Modifier.focusRequester(profileMenuFocusRequester)
                            } else {
                                Modifier
                            },
                            onClick = {
                                showProfileMenu = false

                                /*
                                 * UUID를 저장하고, 화면에서는 이름을 URL profile 값으로 사용한다.
                                 * MainActivity의 playerProfileId가 변경되면 streamUrl이 달라지고,
                                 * 기존 player는 release된 뒤 새 스트림으로 재생된다.
                                 */
                                onProfileSelected(profile.uuid)
                            },
                        )
                    }
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
