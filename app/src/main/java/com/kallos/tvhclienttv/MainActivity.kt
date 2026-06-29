package com.kallos.tvhclienttv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults

private enum class AppScreen {
    Home,
    Settings,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TvhClientTvApp()
        }
    }
}

@Composable
private fun TvhClientTvApp() {
    var screen by remember { mutableStateOf(AppScreen.Home) }

    BackHandler(enabled = screen != AppScreen.Home) {
        screen = AppScreen.Home
    }

    when (screen) {
        AppScreen.Home -> HomeScreen(
            onOpenSettings = { screen = AppScreen.Settings },
        )

        AppScreen.Settings -> SettingsScreen(
            onBack = { screen = AppScreen.Home },
        )
    }
}

@Composable
private fun HomeScreen(
    onOpenSettings: () -> Unit,
) {
    var statusMessage by remember {
        mutableStateOf("리모컨으로 메뉴를 선택하세요.")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1020))
            .padding(horizontal = 72.dp, vertical = 56.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "TVH Client TV",
            color = Color.White,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = "Android TV 전용 TVHeadend 클라이언트",
            color = Color(0xFF9AA4B2),
            fontSize = 19.sp,
            modifier = Modifier.padding(top = 10.dp),
        )

        Spacer(modifier = Modifier.height(42.dp))

        TvMenuButton(
            text = "채널 보기",
            onClick = {
                statusMessage = "채널 목록은 서버 연결 후 사용할 수 있습니다."
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        TvMenuButton(
            text = "EPG 편성표",
            onClick = {
                statusMessage = "EPG는 서버 연결 후 사용할 수 있습니다."
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        TvMenuButton(
            text = "서버 설정",
            onClick = onOpenSettings,
        )

        Spacer(modifier = Modifier.height(34.dp))

        Text(
            text = statusMessage,
            color = Color(0xFF9AA4B2),
            fontSize = 18.sp,
        )
    }
}

@Composable
private fun SettingsScreen(
    onBack: () -> Unit,
) {
    var statusMessage by remember {
        mutableStateOf("아직 TVHeadend 서버가 등록되지 않았습니다.")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1020))
            .padding(horizontal = 72.dp, vertical = 56.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "서버 설정",
            color = Color.White,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = statusMessage,
            color = Color(0xFF9AA4B2),
            fontSize = 19.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 40.dp),
        )

        TvMenuButton(
            text = "서버 주소 입력",
            onClick = {
                statusMessage = "다음 단계에서 TV 리모컨 전용 주소 입력 화면을 추가합니다."
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        TvMenuButton(
            text = "뒤로 가기",
            onClick = onBack,
        )
    }
}

@Composable
private fun TvMenuButton(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = Color(0xFF18243A),
            focusedContainerColor = Color(0xFF4EA1FF),
        ),
    ) {
        Text(
            text = text,
            fontSize = 22.sp,
            modifier = Modifier.padding(horizontal = 34.dp, vertical = 12.dp),
        )
    }
}
