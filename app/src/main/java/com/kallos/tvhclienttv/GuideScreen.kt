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
import androidx.activity.compose.BackHandler
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
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

private val GuideBackground = Color(0xFF0B1020)
private val GuidePanel = Color(0xFF131E31)
private val GuidePanelSoft = Color(0xFF18263B)
private val GuideProgram = Color(0xFF1D3854)
private val GuideNowProgram = Color(0xFF137B7B)
private val GuideSelected = Color(0xFF1B4265)
private val GuideFocus = Color(0xFF2D9CDB)
private val GuideSubText = Color(0xFF9EADC2)

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

    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchKeyboardMode by remember { mutableStateOf(KeyboardMode.Lower) }

    var statusMessage by remember {
        mutableStateOf("TV 가이드를 불러오는 중입니다.")
    }

    fun reloadGuide() {
        if (serverUrl.isBlank()) {
            statusMessage = "서버 주소가 없습니다."
            return
        }

        statusMessage = "채널과 편성표를 불러오는 중입니다."

        Thread {
            val channelResult = loadTvhChannels(serverUrl, username, password)
            val epgResult = loadGuideEpg(serverUrl, username, password)

            context.mainExecutor.execute {
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
                statusMessage = "채널 ${channels.size}개 · 편성표 ${events.size}개"
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

    val searchKeyword = searchQuery.trim()

    val searchResults = channels.filter { channel ->
        searchKeyword.isBlank() ||
            channel.name.contains(searchKeyword, ignoreCase = true) ||
            channel.number.contains(searchKeyword)
    }.take(30)

    BackHandler(enabled = showSearch) {
        showSearch = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GuideBackground)
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
                color = GuideSubText,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 14.dp),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GuideTagButton(
                text = "전체",
                selected = selectedTagId == null,
                onClick = { selectedTagId = null },
            )

            tags.take(5).forEach { tag ->
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

            GuideTagButton(
                text = "검색",
                selected = showSearch,
                onClick = {
                    searchQuery = ""
                    searchKeyboardMode = KeyboardMode.Lower
                    showSearch = true
                },
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GuidePanel)
                .padding(vertical = 10.dp),
        ) {
            Text(
                text = "채널",
                color = GuideSubText,
                fontSize = 13.sp,
                modifier = Modifier
                    .width(250.dp)
                    .padding(start = 14.dp),
            )

            repeat(4) { index ->
                Text(
                    text = formatGuideTime(guideStart + (index * 1800L)),
                    color = GuideSubText,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .width(200.dp)
                        .padding(start = 10.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(610.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
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
                        onPlayChannel(channel)
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val selected = selectedChannel

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GuidePanel)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = selected?.let {
                    "${formatChannelNumber(it.number)}  ${it.name}"
                } ?: "채널을 선택하세요.",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(360.dp),
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
                text = "설정",
                onClick = onOpenSettings,
            )

            TvMenuButton(
                text = "뒤로",
                onClick = onBack,
            )
        }

        if (showSearch) {
            Popup(
                alignment = Alignment.Center,
                properties = PopupProperties(
                    focusable = true,
                ),
                onDismissRequest = {
                    showSearch = false
                },
            ) {
                Column(
                    modifier = Modifier
                        .width(620.dp)
                        .background(Color(0xFF101B2D))
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "채널 검색",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    Text(
                        text = if (searchQuery.isBlank()) {
                            "채널 번호 또는 이름을 입력하세요."
                        } else {
                            searchQuery
                        },
                        color = if (searchQuery.isBlank()) GuideSubText else Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1B2A42))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    TvKeyboard(
                        mode = searchKeyboardMode,
                        onModeChange = { searchKeyboardMode = it },
                        onKeyClick = { key ->
                            searchQuery = if (searchKeyboardMode == KeyboardMode.Korean) {
                                appendHangulInput(searchQuery, key)
                            } else {
                                searchQuery + key
                            }
                        },
                        onBackspace = {
                            if (searchQuery.isNotEmpty()) {
                                searchQuery = searchQuery.dropLast(1)
                            }
                        },
                        onSpace = {
                            searchQuery += " "
                        },
                    )

                    Text(
                        text = "검색 결과 ${searchResults.size}개 · 결과 선택 시 바로 재생",
                        color = GuideSubText,
                        fontSize = 13.sp,
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(118.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        items(
                            items = searchResults,
                            key = { it.uuid },
                        ) { channel ->
                            Button(
                                onClick = {
                                    showSearch = false
                                    onPlayChannel(channel)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                colors = ButtonDefaults.colors(
                                    containerColor = Color(0xFF1B2A42),
                                    focusedContainerColor = GuideFocus,
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(start = 10.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }

                    GuideTagButton(
                        text = "닫기",
                        selected = false,
                        onClick = {
                            showSearch = false
                        },
                    )
                }
            }
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
            .width(118.dp)
            .height(38.dp),
        colors = ButtonDefaults.colors(
            containerColor = if (selected) GuideSelected else GuidePanel,
            focusedContainerColor = GuideFocus,
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
            .height(58.dp),
        colors = ButtonDefaults.colors(
            containerColor = if (selected) GuideSelected else GuidePanelSoft,
            focusedContainerColor = GuideFocus,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .width(250.dp)
                    .padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChannelLogo(
                    serverUrl = serverUrl,
                    iconPath = channel.iconUrl,
                )

                Text(
                    text = "${formatChannelNumber(channel.number)}  ${channel.name}",
                    color = Color.White,
                    fontSize = 14.sp,
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

                val nowSeconds = System.currentTimeMillis() / 1000L

                val isNow = event != null &&
                    event.start <= nowSeconds &&
                    event.stop > nowSeconds

                val showTitle = event != null &&
                    (
                        slot == 0 ||
                        event.start >= slotStart
                    )

                Column(
                    modifier = Modifier
                        .width(200.dp)
                        .height(44.dp)
                        .background(
                            when {
                                isNow -> GuideNowProgram
                                event != null -> GuideProgram
                                else -> GuidePanel
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    if (event != null && showTitle) {
                        Text(
                            text = event.title,
                            color = Color.White,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Text(
                            text = "${formatGuideTime(event.start)} ~ ${formatGuideTime(event.stop)}",
                            color = Color(0xFFD0DEEA),
                            fontSize = 10.sp,
                        )
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
