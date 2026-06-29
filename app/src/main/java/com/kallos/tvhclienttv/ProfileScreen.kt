package com.kallos.tvhclienttv

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileScreen(
    serverUrl: String,
    username: String,
    password: String,
    preferences: SharedPreferences,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    var profiles by remember { mutableStateOf<List<StreamProfile>>(emptyList()) }
    var selectedProfileId by remember {
        mutableStateOf(preferences.getString("stream_profile", "pass") ?: "pass")
    }
    var statusMessage by remember { mutableStateOf("스트림 프로파일을 불러오는 중입니다.") }
    var isLoading by remember { mutableStateOf(true) }

    fun reloadProfiles() {
        if (serverUrl.isBlank()) {
            statusMessage = "서버 주소가 없습니다."
            isLoading = false
            return
        }

        isLoading = true
        statusMessage = "스트림 프로파일을 불러오는 중입니다."

        Thread {
            val result = loadTvhProfiles(
                serverUrl = serverUrl,
                username = username,
                password = password,
            )

            context.mainExecutor.execute {
                isLoading = false
                profiles = result.profiles

                statusMessage = if (result.error == null) {
                    "프로파일 ${result.profiles.size}개"
                } else {
                    "기본 프로파일 사용: ${result.error}"
                }
            }
        }.start()
    }

    LaunchedEffect(serverUrl) {
        reloadProfiles()
    }

    val selectedName = profiles
        .firstOrNull { it.uuid == selectedProfileId }
        ?.name
        ?: "선택 없음"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1020))
            .padding(horizontal = 42.dp, vertical = 28.dp),
    ) {
        Text(
            text = "스트림 프로파일",
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = statusMessage,
            color = Color(0xFF9AA4B2),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 6.dp),
        )

        Text(
            text = "현재 선택: $selectedName",
            color = Color(0xFF78B9FF),
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 12.dp, bottom = 16.dp),
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = profiles,
                key = { it.uuid },
            ) { profile ->
                ChannelFilterButton(
                    text = if (profile.uuid == selectedProfileId) {
                        "✓ ${profile.name}"
                    } else {
                        profile.name
                    },
                    selected = profile.uuid == selectedProfileId,
                    onClick = {
                        selectedProfileId = profile.uuid

                        preferences.edit()
                            .putString("stream_profile", profile.uuid)
                            .apply()

                        statusMessage = "${profile.name} 프로파일을 선택했습니다."
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        TvMenuButton(
            text = if (isLoading) "불러오는 중..." else "새로고침",
            enabled = !isLoading,
            onClick = { reloadProfiles() },
        )

        Spacer(modifier = Modifier.height(8.dp))

        TvMenuButton(
            text = "뒤로 가기",
            onClick = onBack,
        )
    }
}
