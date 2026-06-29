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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EpgScreen(
    serverUrl: String,
    username: String,
    password: String,
    preferences: SharedPreferences,
    onBack: () -> Unit,
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
                events = epgResult.events
                statusMessage = "현재 방송 ${events.size}개"
            }
        }.start()
    }

    LaunchedEffect(serverUrl) {
        reload()
    }

    fun channelName(channelUuid: String): String {
        val channel = channels.firstOrNull { it.uuid == channelUuid }
        return if (channel == null) {
            "알 수 없는 채널"
        } else {
            "${formatChannelNumber(channel.number)}  ${channel.name}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1020))
            .padding(horizontal = 38.dp, vertical = 24.dp),
    ) {
        Text(
            text = "현재 방송 EPG",
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = statusMessage,
            color = Color(0xFF9AA4B2),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(
                modifier = Modifier.width(620.dp),
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(
                        items = events,
                        key = { "${it.channelUuid}_${it.start}_${it.title}" },
                    ) { event ->
                        TvMenuButton(
                            text = "${channelName(event.channelUuid)}  |  ${event.title}",
                            onClick = {
                                selectedEvent = event
                            },
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "방송 정보",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )

                if (selectedEvent == null) {
                    Text(
                        text = "왼쪽 목록에서 방송을 선택하세요.",
                        color = Color(0xFF9AA4B2),
                        fontSize = 15.sp,
                    )
                } else {
                    val event = selectedEvent!!

                    Text(
                        text = channelName(event.channelUuid),
                        color = Color(0xFF78B9FF),
                        fontSize = 17.sp,
                    )

                    Text(
                        text = event.title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    if (event.subtitle.isNotBlank()) {
                        Text(
                            text = event.subtitle,
                            color = Color(0xFFB8C6DA),
                            fontSize = 16.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TvMenuButton(
                    text = if (isLoading) "불러오는 중..." else "새로고침",
                    enabled = !isLoading,
                    onClick = { reload() },
                )

                TvMenuButton(
                    text = "뒤로 가기",
                    onClick = onBack,
                )
            }
        }
    }
}
