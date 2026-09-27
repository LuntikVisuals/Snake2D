package com.luntik.snake

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent { SnakeApp() }
    }
}

private enum class Dir { UP, DOWN, LEFT, RIGHT }
private data class Cell(val x: Int, val y: Int)

private const val COLS = 16
private const val ROWS = 20

private object G {
    val Bg = Color(0xFF0A0F0A)
    val Board = Color(0xFF121A12)
    val Grid = Color(0xFF1A241A)
    val Snake = Color(0xFF3DFF6E)
    val Head = Color(0xFF9AFFB0)
    val Food = Color(0xFFFF4D4D)
    val Text = Color(0xFFE8FFE8)
    val Dim = Color.White.copy(alpha = 0.5f)
    val Btn = Color(0xFF1E2E1E)
    val Border = Color.White.copy(alpha = 0.12f)
}

@Composable
fun SnakeApp() {
    var running by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(0) }
    var dir by remember { mutableStateOf(Dir.RIGHT) }
    var nextDir by remember { mutableStateOf(Dir.RIGHT) }
    var snake by remember {
        mutableStateOf(listOf(Cell(3, 10), Cell(2, 10), Cell(1, 10)))
    }
    var food by remember { mutableStateOf(Cell(10, 8)) }

    fun spawnFood(body: List<Cell>): Cell {
        var c: Cell
        do {
            c = Cell(Random.nextInt(COLS), Random.nextInt(ROWS))
        } while (c in body)
        return c
    }

    fun reset() {
        snake = listOf(Cell(3, 10), Cell(2, 10), Cell(1, 10))
        dir = Dir.RIGHT
        nextDir = Dir.RIGHT
        food = spawnFood(snake)
        score = 0
        gameOver = false
        running = true
    }

    fun turn(d: Dir) {
        val opposite = when (dir) {
            Dir.UP -> Dir.DOWN
            Dir.DOWN -> Dir.UP
            Dir.LEFT -> Dir.RIGHT
            Dir.RIGHT -> Dir.LEFT
        }
        if (d != opposite) nextDir = d
    }

    LaunchedEffect(running, gameOver) {
        while (running && !gameOver) {
            delay(140L)
            dir = nextDir
            val head = snake.first()
            val newHead = when (dir) {
                Dir.UP -> Cell(head.x, head.y - 1)
                Dir.DOWN -> Cell(head.x, head.y + 1)
                Dir.LEFT -> Cell(head.x - 1, head.y)
                Dir.RIGHT -> Cell(head.x + 1, head.y)
            }
            if (newHead.x !in 0 until COLS ||
                newHead.y !in 0 until ROWS ||
                newHead in snake
            ) {
                gameOver = true
                running = false
                if (score > best) best = score
                break
            }
            val grew = newHead == food
            val body = if (grew) listOf(newHead) + snake else listOf(newHead) + snake.dropLast(1)
            snake = body
            if (grew) {
                score += 10
                food = spawnFood(body)
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(G.Bg)
            .statusBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("ЗМЕЙКА", color = G.Snake, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Счёт: $score", color = G.Text, fontSize = 16.sp)
            Text("Рекорд: $best", color = G.Dim, fontSize = 16.sp)
        }
        Spacer(Modifier.height(12.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(COLS.toFloat() / ROWS)
                .clip(RoundedCornerShape(12.dp))
                .background(G.Board)
                .border(1.dp, G.Border, RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectDragGestures { _, drag ->
                        if (abs(drag.x) > abs(drag.y)) {
                            if (drag.x > 0) turn(Dir.RIGHT) else turn(Dir.LEFT)
                        } else {
                            if (drag.y > 0) turn(Dir.DOWN) else turn(Dir.UP)
                        }
                    }
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val cw = size.width / COLS
                val ch = size.height / ROWS
                // grid
                for (x in 0..COLS) {
                    drawLine(G.Grid, Offset(x * cw, 0f), Offset(x * cw, size.height), 1f)
                }
                for (y in 0..ROWS) {
                    drawLine(G.Grid, Offset(0f, y * ch), Offset(size.width, y * ch), 1f)
                }
                // food
                drawRoundRect(
                    G.Food,
                    topLeft = Offset(food.x * cw + 2, food.y * ch + 2),
                    size = Size(cw - 4, ch - 4),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )
                // snake
                snake.forEachIndexed { i, c ->
                    drawRoundRect(
                        if (i == 0) G.Head else G.Snake,
                        topLeft = Offset(c.x * cw + 1.5f, c.y * ch + 1.5f),
                        size = Size(cw - 3f, ch - 3f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
                    )
                }
            }

            if (!running && !gameOver) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Нажми «Старт»", color = G.Text, fontSize = 18.sp)
                }
            }
            if (gameOver) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Конец игры", color = G.Food, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Счёт: $score", color = G.Text, fontSize = 16.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // D-pad
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            DirBtn("▲") { turn(Dir.UP) }
            Row {
                DirBtn("◀") { turn(Dir.LEFT) }
                Spacer(Modifier.size(48.dp))
                DirBtn("▶") { turn(Dir.RIGHT) }
            }
            DirBtn("▼") { turn(Dir.DOWN) }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionBtn(if (gameOver || !running) "Старт" else "Заново") { reset() }
            if (running) {
                ActionBtn("Пауза") { running = false }
            } else if (!gameOver && snake.isNotEmpty()) {
                ActionBtn("Продолжить") { running = true }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Свайп по полю или кнопки", color = G.Dim, fontSize = 12.sp)
    }
}

@Composable
private fun DirBtn(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(G.Btn)
            .border(1.dp, G.Border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = G.Snake, fontSize = 20.sp)
    }
}

@Composable
private fun ActionBtn(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(G.Snake)
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 12.dp)
    ) {
        Text(text, color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}
