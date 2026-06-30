package com.kallos.tvhclienttv

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

@Composable
fun GuideScreen(
    serverUrl: String,
    username: String,
    password: String,
    preferences: android.content.SharedPreferences,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onPlayChannel: (TvhChannel) -> Unit,
) {
    val context = LocalContext.current

    var channels by remember { mutableStateOf<List<TvhChannel>>(emptyList()) }
    var tags by remember { mutableStateOf<List<TvhTag>>(emptyList()) }
    var events by remember { mutableStateOf<List<TvhEpgEvent>>(emptyList()) }

    var selectedTagId by remember { mutableStateOf<String?>(null) }
    var selectedChannel by remember { mutableStateOf<TvhChannel?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf("TV 가이드를 불러오는 중입니다.") }

    fun reloadGuide() {
        if (serverUrl.isBlank()) {
            isLoading = false
            statusMessage = "서버 주소가 없습니다."
            return
        }

        isLoading = true
        statusMessage = "채널과 EPG 정보를 불러오는 중입니다."

        Thread {
            val channelResult = loadTvhChannels(serverUrl, username, password)
            val epgResult = loadGuideEpg(serverUrl, username, password)

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
                tags = channelResult.tags
                events = epgResult.events
                statusMessage = "채널 ${channels.size}개 · EPG ${events.size}개"
            }
        }.start()
    }

    LaunchedEffect(serverUrl) {
        reloadGuide()
    }

    val guideStart = remember {
        val nowSeconds = System.currentTimeMillis() / 1000L
        nowSeconds - (nowSeconds % 1800L)
    }

    val filteredChannels = channels.filter { channel ->
        selectedTagId == null || selectedTagId in channel.tagIds
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1020))
            .padding(horizontal = 28.dp, vertical = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "TV 가이드",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = statusMessage,
                color = Color(0xFF9AA4B2),
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 16.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GuideTagButton(
                text = "전체",
                selected = selectedTagId == null,
                onClick = { selectedTagId = null },
            )

            tags.take(8).forEach { tag ->
                GuideTagButton(
                    text = tag.name,
                    selected = selectedTagId == tag.uuid,
                    onClick = { selectedTagId = tag.uuid },
                )
            }

            GuideTagButton(
                text = "새로고침",
                selected = false,
                onClick = { reloadGuide() },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "채널",
                color = Color(0xFFB8C6DA),
                fontSize = 14.sp,
                modifier = Modifier.width(270.dp),
            )

            repeat(4) { index ->
                Text(
                    text = formatGuideTime(guideStart + (index * 1800L)),
                    color = Color(0xFFB8C6DA),
                    fontSize = 14.sp,
                    modifier = Modifier.width(250.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(690.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(
                items = filteredChannels,
                key = { it.uuid },
            ) { channel ->
                GuideChannelRow(
                    channel = channel,
                    serverUrl = serverUrl,
                    events = events,
                    guideStart = guideStart,
                    selected = selectedChannel?.uuid == channel.uuid,
                    onClick = {
                        selectedChannel = channel
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val selected = selectedChannel

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = selected?.let {
                    "${formatChannelNumber(it.number)}  ${it.name}"
                } ?: "채널을 선택하세요.",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(420.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            TvMenuButton(
                text = "재생",
                enabled = selected != null,
                onClick = {
                    selected?.let(onPlayChannel)
                },
            )

            TvMenuButton(
                text = "서버 설정",
                onClick = onOpenSettings,
            )

            TvMenuButton(
                text = "뒤로 가기",
                onClick = onBack,
            )
        }
    }
}

@Composable
private fun GuideTagButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(116.dp)
            .height(40.dp),
        colors = ButtonDefaults.colors(
            containerColor = if (selected) Color(0xFF28547E) else Color(0xFF18243A),
            focusedContainerColor = Color(0xFF4EA1FF),
        ),
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun GuideChannelRow(
    channel: TvhChannel,
    serverUrl: String,
    events: List<TvhEpgEvent>,
    guideStart: Long,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val channelEvents = remember(events, channel.uuid) {
        events
            .filter { it.channelUuid == channel.uuid }
            .sortedBy { it.start }
    }

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp),
        colors = ButtonDefaults.colors(
            containerColor = if (selected) Color(0xFF274D6D) else Color(0xFF111B2B),
            focusedContainerColor = Color(0xFF2E9BD6),
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.width(270.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChannelLogo(
                    serverUrl = serverUrl,
                    iconPath = channel.iconUrl,
                )

                Text(
                    text = "${formatChannelNumber(channel.number)}  ${channel.name}",
                    color = Color.White,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(start = 10.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            repeat(4) { slot ->
                val slotStart = guideStart + (slot * 1800L)
                val event = channelEvents.firstOrNull {
                    it.start <= slotStart && it.stop > slotStart
                }

                val isNow = event != null &&
                    event.start <= (System.currentTimeMillis() / 1000L) &&
                    event.stop > (System.currentTimeMillis() / 1000L)

                Column(
                    modifier = Modifier
                        .width(250.dp)
                        .height(50.dp)
                        .background(
                            when {
                                isNow -> Color(0xFF146E74)
                                event != null -> Color(0xFF263B57)
                                else -> Color(0xFF18243A)
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    if (event != null) {
                        Text(
                            text = if (slotStart == event.start || slot == 0) event.title else "계속",
                            color = Color.White,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        if (slotStart == event.start || slot == 0) {
                            Text(
                                text = "${formatGuideTime(event.start)} ~ ${formatGuideTime(event.stop)}",
                                color = Color(0xFFB8C6DA),
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatGuideTime(seconds: Long): String {
    return SimpleDateFormat(
        "HH:mm",
        Locale.KOREA,
    ).format(Date(seconds * 1000L))
}
