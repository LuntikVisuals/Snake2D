package com.luntik.snake

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val store = remember { ProgressStore(this) }
            var page by remember { mutableStateOf(0) }
            Box(Modifier.fillMaxSize().background(Color(0xFF070B12)).statusBarsPadding().padding(20.dp)) {
                when (page) {
                    0 -> Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("SNAKE2D 2.0.0", color = Color(0xFF67F5B4), fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text("Привет, ${store.nickname} · ур. ${store.level} · ${store.coins} монет", color = Color.White)
                        Text("Режимы: " + GameMode.entries.joinToString { it.title }, color = Color(0xFF9AAABD), fontSize = 13.sp)
                        Text("Персонажи: " + CharacterData.all.joinToString { it.name }, color = Color(0xFF9AAABD), fontSize = 13.sp)
                        Text("Полный UI (Поедание, liquid glass) — файл MainActivity догружается.", color = Color(0xFFFFD36E), fontSize = 12.sp)
                        TextButton(onClick = { page = 1 }) { Text("Профиль", color = Color(0xFF67F5B4)) }
                    }
                    else -> Column {
                        Text("Профиль", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("XP ${store.xp} · рекорд ${store.bestScore(GameMode.CLASSIC.name)}", color = Color(0xFF9AAABD))
                        TextButton(onClick = { page = 0 }) { Text("Назад", color = Color(0xFF67F5B4)) }
                    }
                }
            }
        }
    }
}
