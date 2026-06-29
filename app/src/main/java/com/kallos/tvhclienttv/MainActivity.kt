package com.kallos.tvhclienttv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults

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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1020))
            .padding(64.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "TVH Client TV",
            color = Color.White,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = "Android TV 전용 버전",
            color = Color(0xFF9AA4B2),
            fontSize = 20.sp,
            modifier = Modifier.padding(top = 14.dp, bottom = 42.dp),
        )

        Button(
            onClick = {},
            colors = ButtonDefaults.colors(
                containerColor = Color(0xFF1D6FD8),
                focusedContainerColor = Color(0xFF4EA1FF),
            ),
        ) {
            Text(
                text = "리모컨 테스트",
                fontSize = 22.sp,
                modifier = Modifier.padding(horizontal = 26.dp, vertical = 12.dp),
            )
        }
    }
}
