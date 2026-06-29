package com.kallos.tvhclienttv

import android.content.Context
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import java.net.HttpURLConnection
import java.net.URL

private enum class AppScreen {
    Home,
    Settings,
}

private enum class InputField {
    ServerUrl,
    Username,
    Password,
}

private enum class KeyboardMode {
    Lower,
    Upper,
    Symbol,
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
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("tvh_settings", Context.MODE_PRIVATE)
    }

    var screen by remember { mutableStateOf(AppScreen.Home) }
    var connectionMessage by remember {
        mutableStateOf(
            if (preferences.getString("server_url", "").isNullOrBlank()) {
                "서버가 설정되지 않았습니다."
            } else {
                "저장된 서버: ${preferences.getString("server_url", "")}"
            }
        )
    }

    BackHandler(enabled = screen != AppScreen.Home) {
        screen = AppScreen.Home
    }

    when (screen) {
        AppScreen.Home -> HomeScreen(
            connectionMessage = connectionMessage,
            onOpenSettings = { screen = AppScreen.Settings },
        )

        AppScreen.Settings -> SettingsScreen(
            preferences = preferences,
            onConnectionChanged = { message ->
                connectionMessage = message
            },
            onBack = { screen = AppScreen.Home },
        )
    }
}

@Composable
private fun HomeScreen(
    connectionMessage: String,
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
            text = connectionMessage,
            color = Color(0xFF9AA4B2),
            fontSize = 18.sp,
            modifier = Modifier.padding(top = 12.dp),
        )

        Spacer(modifier = Modifier.height(40.dp))

        TvMenuButton(
            text = "채널 보기",
            onClick = {
                statusMessage = "채널 목록은 서버 연결 기능 확인 후 추가합니다."
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        TvMenuButton(
            text = "EPG 편성표",
            onClick = {
                statusMessage = "EPG는 채널 목록 다음 단계에서 추가합니다."
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        TvMenuButton(
            text = "서버 설정",
            onClick = onOpenSettings,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = statusMessage,
            color = Color(0xFF9AA4B2),
            fontSize = 18.sp,
        )
    }
}

@Composable
private fun SettingsScreen(
    preferences: android.content.SharedPreferences,
    onConnectionChanged: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var serverUrl by remember {
        mutableStateOf(preferences.getString("server_url", "") ?: "")
    }
    var username by remember {
        mutableStateOf(preferences.getString("username", "") ?: "")
    }
    var password by remember {
        mutableStateOf(preferences.getString("password", "") ?: "")
    }

    var activeField by remember { mutableStateOf(InputField.ServerUrl) }
    var keyboardMode by remember { mutableStateOf(KeyboardMode.Lower) }
    var statusMessage by remember {
        mutableStateOf("주소를 입력한 뒤 연결 테스트를 실행하세요.")
    }
    var isTesting by remember { mutableStateOf(false) }

    fun currentValue(): String {
        return when (activeField) {
            InputField.ServerUrl -> serverUrl
            InputField.Username -> username
            InputField.Password -> password
        }
    }

    fun appendText(text: String) {
        when (activeField) {
            InputField.ServerUrl -> serverUrl += text
            InputField.Username -> username += text
            InputField.Password -> password += text
        }
    }

    fun deleteLastCharacter() {
        when (activeField) {
            InputField.ServerUrl -> {
                if (serverUrl.isNotEmpty()) serverUrl = serverUrl.dropLast(1)
            }

            InputField.Username -> {
                if (username.isNotEmpty()) username = username.dropLast(1)
            }

            InputField.Password -> {
                if (password.isNotEmpty()) password = password.dropLast(1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1020))
            .padding(horizontal = 58.dp, vertical = 34.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "TVHeadend 서버 설정",
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = statusMessage,
            color = Color(0xFF9AA4B2),
            fontSize = 17.sp,
            modifier = Modifier.padding(top = 10.dp, bottom = 20.dp),
        )

        SettingFieldButton(
            label = "서버 주소",
            value = serverUrl.ifBlank { "예: http://192.168.0.10:9981" },
            selected = activeField == InputField.ServerUrl,
            onClick = { activeField = InputField.ServerUrl },
        )

        Spacer(modifier = Modifier.height(10.dp))

        SettingFieldButton(
            label = "사용자 이름",
            value = username.ifBlank { "인증이 없으면 비워 두세요." },
            selected = activeField == InputField.Username,
            onClick = { activeField = InputField.Username },
        )

        Spacer(modifier = Modifier.height(10.dp))

        SettingFieldButton(
            label = "비밀번호",
            value = if (password.isBlank()) {
                "인증이 없으면 비워 두세요."
            } else {
                "•".repeat(password.length)
            },
            selected = activeField == InputField.Password,
            onClick = { activeField = InputField.Password },
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "입력 중: ${
                when (activeField) {
                    InputField.ServerUrl -> "서버 주소"
                    InputField.Username -> "사용자 이름"
                    InputField.Password -> "비밀번호"
                }
            }",
            color = Color(0xFF78B9FF),
            fontSize = 17.sp,
        )

        Spacer(modifier = Modifier.height(12.dp))

        TvKeyboard(
            mode = keyboardMode,
            onModeChange = { keyboardMode = it },
            onKeyClick = { appendText(it) },
            onBackspace = { deleteLastCharacter() },
            onSpace = { appendText(" ") },
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row {
            TvMenuButton(
                text = if (isTesting) "연결 확인 중..." else "저장 및 연결 테스트",
                enabled = !isTesting,
                onClick = {
                    val normalizedUrl = serverUrl.trim().trimEnd('/')

                    if (normalizedUrl.isBlank()) {
                        statusMessage = "서버 주소를 먼저 입력하세요."
                        return@TvMenuButton
                    }

                    if (!normalizedUrl.startsWith("http://") &&
                        !normalizedUrl.startsWith("https://")
                    ) {
                        statusMessage = "주소는 http:// 또는 https:// 로 시작해야 합니다."
                        return@TvMenuButton
                    }

                    isTesting = true
                    statusMessage = "TVHeadend 서버에 연결하는 중입니다."

                    Thread {
                        val result = testTvhConnection(
                            serverUrl = normalizedUrl,
                            username = username.trim(),
                            password = password,
                        )

                        context.mainExecutor.execute {
                            isTesting = false

                            if (result.success) {
                                preferences.edit()
                                    .putString("server_url", normalizedUrl)
                                    .putString("username", username.trim())
                                    .putString("password", password)
                                    .apply()

                                statusMessage = "연결 성공: ${result.message}"
                                onConnectionChanged("연결됨: $normalizedUrl")
                            } else {
                                statusMessage = "연결 실패: ${result.message}"
                            }
                        }
                    }.start()
                },
            )

            Spacer(modifier = Modifier.height(1.dp))

            TvMenuButton(
                text = "뒤로 가기",
                onClick = onBack,
            )
        }
    }
}

@Composable
private fun SettingFieldButton(
    label: String,
    value: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.colors(
            containerColor = if (selected) Color(0xFF20385C) else Color(0xFF18243A),
            focusedContainerColor = Color(0xFF4EA1FF),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                text = label,
                fontSize = 16.sp,
                color = Color(0xFFB8C6DA),
            )

            Text(
                text = value,
                fontSize = 20.sp,
                color = Color.White,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun TvKeyboard(
    mode: KeyboardMode,
    onModeChange: (KeyboardMode) -> Unit,
    onKeyClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
) {
    val rows = when (mode) {
        KeyboardMode.Lower -> listOf(
            listOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"),
            listOf("k", "l", "m", "n", "o", "p", "q", "r", "s", "t"),
            listOf("u", "v", "w", "x", "y", "z", "0", "1", "2", "3"),
            listOf("4", "5", "6", "7", "8", "9", ".", ":", "/", "-"),
        )

        KeyboardMode.Upper -> listOf(
            listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J"),
            listOf("K", "L", "M", "N", "O", "P", "Q", "R", "S", "T"),
            listOf("U", "V", "W", "X", "Y", "Z", "0", "1", "2", "3"),
            listOf("4", "5", "6", "7", "8", "9", ".", ":", "/", "-"),
        )

        KeyboardMode.Symbol -> listOf(
            listOf(":", "/", ".", "-", "_", "@", "?", "&", "=", "%"),
            listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                row.forEach { key ->
                    KeyboardKey(
                        text = key,
                        onClick = { onKeyClick(key) },
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KeyboardKey(
                text = "abc",
                onClick = { onModeChange(KeyboardMode.Lower) },
            )
            KeyboardKey(
                text = "ABC",
                onClick = { onModeChange(KeyboardMode.Upper) },
            )
            KeyboardKey(
                text = "123",
                onClick = { onModeChange(KeyboardMode.Symbol) },
            )
            KeyboardKey(
                text = "공백",
                onClick = onSpace,
            )
            KeyboardKey(
                text = "지우기",
                onClick = onBackspace,
            )
        }
    }
}

@Composable
private fun KeyboardKey(
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
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun TvMenuButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.colors(
            containerColor = Color(0xFF18243A),
            focusedContainerColor = Color(0xFF4EA1FF),
        ),
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
        )
    }
}

private data class ConnectionResult(
    val success: Boolean,
    val message: String,
)

private fun testTvhConnection(
    serverUrl: String,
    username: String,
    password: String,
): ConnectionResult {
    return try {
        val connection = URL("$serverUrl/api/serverinfo").openConnection() as HttpURLConnection

        connection.requestMethod = "GET"
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        connection.setRequestProperty("Accept", "application/json")

        if (username.isNotBlank()) {
            val credentials = "$username:$password"
            val encoded = Base64.encodeToString(
                credentials.toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP,
            )
            connection.setRequestProperty("Authorization", "Basic $encoded")
        }

        val code = connection.responseCode
        connection.disconnect()

        when (code) {
            in 200..299 -> ConnectionResult(
                success = true,
                message = "TVHeadend 서버 응답 확인",
            )

            401, 403 -> ConnectionResult(
                success = false,
                message = "인증에 실패했습니다. 사용자 이름과 비밀번호를 확인하세요.",
            )

            else -> ConnectionResult(
                success = false,
                message = "HTTP 응답 코드 $code",
            )
        }
    } catch (error: Exception) {
        ConnectionResult(
            success = false,
            message = error.message ?: error.javaClass.simpleName,
        )
    }
}
