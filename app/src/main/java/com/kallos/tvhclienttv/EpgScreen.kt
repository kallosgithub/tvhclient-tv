package com.kallos.tvhclienttv

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val EpgBackground = Color(0xFF0B1020)
private val EpgCard = Color(0xFF18263B)
private val EpgInfoCard = Color(0xFF121E31)
private val EpgFocus = Color(0xFF2D9CDB)
private val EpgSubText = Color(0xFFB8C6DA)
private val EpgNow = Color(0xFF177E7E)

@Composable
fun EpgScreen(
    serverUrl: String,
    username: String,
    password: String,
    preferences: SharedPreferences,
    onBack: () -> Unit,
    onPlayChannel: (TvhChannel) -> Unit,
) {
    val context = LocalContext.current

    var events by remember { mutableStateOf<List<TvhEpgEvent>>(emptyList()) }
    var channels by remember { mutableStateOf<List<TvhChannel>>(emptyList()) }
    var selectedEvent by remember { mutableStateOf<TvhEpgEvent?>(null) }
    var statusMessage by remember { mutableStateOf("현재 방송 정보를 불러오는 중입니다.") }
    var isLoading by remember { mutableStateOf(true) }

    fun reload() {
        if (serverUrl.isBlank()) {
            statusMessage = "서버 주소가 없습니다."
            isLoading = false
            return
        }

        isLoading = true
        statusMessage = "현재 방송 정보를 불러오는 중입니다."

        Thread {
            val channelResult = loadTvhChannels(serverUrl, username, password)
            val epgResult = loadCurrentEpg(serverUrl, username, password)

            context.mainExecutor.execute {
                isLoading = false

                if (channelResult.error != null) {
                    statusMessage = "채널 불러오기 실패: ${channelResult.error}"
                    return@execute
                }

                if (epgResult.error != null) {
                    statusMessage = "EPG 불러오기 실패: ${epgResult.error}"
                    return@execute
                }

                channels = channelResult.channels
                events = epgResult.events.sortedBy { it.channelUuid }
                statusMessage = "현재 방송 ${events.size}개"
            }
        }.start()
    }

    LaunchedEffect(serverUrl) {
        reload()
    }

    fun findChannel(event: TvhEpgEvent): TvhChannel? {
        return channels.firstOrNull { it.uuid == event.channelUuid }
    }

    fun channelTitle(event: TvhEpgEvent): String {
        val channel = findChannel(event)

        return if (channel == null) {
            "알 수 없는 채널"
        } else {
            "${formatChannelNumber(channel.number)}  ${channel.name}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EpgBackground)
            .padding(horizontal = 34.dp, vertical = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "현재 방송 EPG",
                color = Color.White,
                fontSize = 29.sp,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = statusMessage,
                color = EpgSubText,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 16.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            TvMenuButton(
                text = if (isLoading) "불러오는 중" else "새로고침",
                enabled = !isLoading,
                onClick = { reload() },
            )

            Spacer(modifier = Modifier.width(8.dp))

            TvMenuButton(
                text = "뒤로 가기",
                onClick = onBack,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(
                modifier = Modifier.width(620.dp),
            ) {
                Text(
                    text = "방송 중인 채널",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 10.dp),
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    items(
                        items = events,
                        key = { "${it.channelUuid}_${it.start}_${it.title}" },
                    ) { event ->
                        val channel = findChannel(event)

                        Button(
                            onClick = {
                                channel?.let(onPlayChannel)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(66.dp),
                            colors = ButtonDefaults.colors(
                                containerColor = EpgCard,
                                focusedContainerColor = EpgFocus,
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (channel != null) {
                                    ChannelLogo(
                                        serverUrl = serverUrl,
                                        iconPath = channel.iconUrl,
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 12.dp),
                                ) {
                                    Text(
                                        text = channelTitle(event),
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )

                                    Text(
                                        text = event.title,
                                        color = EpgSubText,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 3.dp),
                                    )
                                }

                                Text(
                                    text = "재생",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EpgInfoCard)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "현재 방송",
                    color = EpgSubText,
                    fontSize = 15.sp,
                )

                val event = selectedEvent

                if (event == null) {
                    Text(
                        text = "왼쪽 채널을 누르면 바로 재생됩니다.",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Text(
                        text = "리모컨 방향키로 채널을 선택하고 확인 버튼을 누르세요.",
                        color = EpgSubText,
                        fontSize = 14.sp,
                    )
                } else {
                    val channel = findChannel(event)

                    if (channel != null) {
                        ChannelLogo(
                            serverUrl = serverUrl,
                            iconPath = channel.iconUrl,
                        )
                    }

                    Text(
                        text = channelTitle(event),
                        color = Color(0xFF70C7FF),
                        fontSize = 16.sp,
                    )

                    Text(
                        text = event.title,
                        color = Color.White,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    Text(
                        text = "${formatEpgTime(event.start)} ~ ${formatEpgTime(event.stop)}",
                        color = EpgSubText,
                        fontSize = 14.sp,
                    )

                    if (event.subtitle.isNotBlank()) {
                        Text(
                            text = event.subtitle,
                            color = EpgSubText,
                            fontSize = 15.sp,
                        )
                    }

                    TvMenuButton(
                        text = "이 채널 재생",
                        enabled = channel != null,
                        onClick = {
                            channel?.let(onPlayChannel)
                        },
                    )
                }
            }
        }
    }
}

private fun formatEpgTime(seconds: Long): String {
    return SimpleDateFormat(
        "HH:mm",
        Locale.KOREA,
    ).format(Date(seconds * 1000L))
}
