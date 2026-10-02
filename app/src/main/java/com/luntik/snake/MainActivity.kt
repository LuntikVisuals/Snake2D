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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

private const val COLS = 16
private const val ROWS = 20

private enum class Dir { UP, DOWN, LEFT, RIGHT }
private enum class Phase { READY, RUN, DEAD }
private data class Cell(val x: Int, val y: Int)

private object C {
    val bg = Color(0xFF070B12)
    val text = Color(0xFFF2F7FF)
    val muted = Color(0xFF9AAABD)
    val mint = Color(0xFF67F5B4)
    val cyan = Color(0xFF65DDFB)
    val gold = Color(0xFFFFD36E)
    val red = Color(0xFFFF6684)
    val line = Color.White.copy(alpha = 0.12f)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        GameFiles.ensureLayout(this)
        val store = ProgressStore(this)
        AntiCheat.scan(this, store).also {
            GameFiles.writeSessionLog(this, "boot ${it.detail}")
        }
        setContent { App(store) }
    }
}

@Composable
private fun App(store: ProgressStore) {
    var screen by remember { mutableStateOf(0) }
    var mode by remember { mutableStateOf(GameMode.CLASSIC) }
    var tick by remember { mutableIntStateOf(0) }
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF0A1320), C.bg, Color(0xFF090D16)))
        )
    ) {
        when (screen) {
            0 -> Hub(store, tick, onPlay = { mode = it; screen = 1 }, onRefresh = { tick++ })
            else -> Play(store, mode) { tick++; screen = 0 }
        }
    }
}

@Composable
private fun Glass(mod: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        mod.clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.04f))))
            .border(1.dp, C.line, RoundedCornerShape(18.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
private fun Hub(store: ProgressStore, tick: Int, onPlay: (GameMode) -> Unit, onRefresh: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val t = tick
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(18.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("SNAKE2D", color = C.mint, fontSize = 32.sp, fontWeight = FontWeight.Black)
        Text("BETA · liquid glass", color = C.muted, fontSize = 12.sp)
        Glass(Modifier.fillMaxWidth()) {
            Text("${store.nickname} · ур.${store.level}", color = C.text, fontWeight = FontWeight.Bold)
            Text("${store.coins} монет · ${store.xp} XP", color = C.gold, fontSize = 14.sp)
        }
        if (store.canClaimDaily()) {
            Glass(Modifier.fillMaxWidth().clickable {
                store.claimDaily(); onRefresh()
            }) {
                Text("ЕЖЕДНЕВНАЯ НАГРАДА — ЗАБРАТЬ", color = C.gold, fontWeight = FontWeight.Bold)
            }
        }
        Text("РЕЖИМЫ", color = C.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        GameMode.entries.filter { it != GameMode.FEEDING }.forEach { m ->
            Glass(Modifier.fillMaxWidth().clickable { onPlay(m) }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(m.title, color = C.text, fontWeight = FontWeight.Bold)
                        Text(m.desc, color = C.muted, fontSize = 12.sp)
                    }
                    Text("▶", color = C.mint, fontSize = 20.sp)
                }
            }
        }
        Glass(Modifier.fillMaxWidth()) {
            Text("Поедание / DeltaSnake2D — следующий этап. Античит и файлы игры уже активны.", color = C.muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun Play(store: ProgressStore, mode: GameMode, onExit: () -> Unit) {
    val skin = ShopData.skin(store.selectedSkin)
    var phase by remember { mutableStateOf(Phase.READY) }
    var score by remember { mutableIntStateOf(0) }
    var dir by remember { mutableStateOf(Dir.RIGHT) }
    var next by remember { mutableStateOf(Dir.RIGHT) }
    var snake by remember { mutableStateOf(listOf(Cell(4, 10), Cell(3, 10), Cell(2, 10))) }
    var food by remember { mutableStateOf(Cell(10, 8)) }
    var rewarded by remember { mutableStateOf(false) }
    val startMs = remember { System.currentTimeMillis() }

    fun spawn(body: List<Cell>): Cell {
        var c: Cell
        do { c = Cell(Random.nextInt(COLS), Random.nextInt(ROWS)) } while (c in body)
        return c
    }
    fun reset() {
        snake = listOf(Cell(4, 10), Cell(3, 10), Cell(2, 10))
        dir = Dir.RIGHT; next = Dir.RIGHT
        food = spawn(snake); score = 0; rewarded = false; phase = Phase.READY
    }
    fun finish() {
        phase = Phase.DEAD
        if (rewarded) return
        rewarded = true
        val foodEaten = score / 10
        val dur = System.currentTimeMillis() - startMs
        val fair = AntiCheat.validateScore(score, dur, foodEaten)
        val coins = (score * mode.coinMul).toInt().coerceAtLeast(if (score > 0) 5 else 0)
        if (fair) {
            store.addCoins(coins, "Партия ${mode.title}")
            store.addXp((score * mode.xpMul).toInt().coerceAtLeast(5))
            store.pushScore(store.nickname, score, mode.name)
        }
        store.recordGameEnd(false, foodEaten, snake.size)
        store.checkAchievementsAfterGame(score, snake.size, false)
    }
    fun turn(d: Dir) {
        val opp = when (dir) {
            Dir.UP -> Dir.DOWN; Dir.DOWN -> Dir.UP; Dir.LEFT -> Dir.RIGHT; Dir.RIGHT -> Dir.LEFT
        }
        if (d != opp) next = d
    }

    LaunchedEffect(phase, mode) {
        while (phase == Phase.RUN) {
            delay(mode.speedMs.coerceAtLeast(50L))
            dir = next
            val h = snake.first()
            var nx = h.x; var ny = h.y
            when (dir) {
                Dir.UP -> ny--; Dir.DOWN -> ny++; Dir.LEFT -> nx--; Dir.RIGHT -> nx++
            }
            if (!mode.wallsKill) {
                if (nx < 0) nx = COLS - 1; if (nx >= COLS) nx = 0
                if (ny < 0) ny = ROWS - 1; if (ny >= ROWS) ny = 0
            }
            val nh = Cell(nx, ny)
            val eat = nh == food
            val body = if (eat) snake else snake.dropLast(1)
            val wall = mode.wallsKill && (nx !in 0 until COLS || ny !in 0 until ROWS)
            if (wall || nh in body) { finish(); break }
            snake = listOf(nh) + body
            if (eat) {
                score += 10
                val free = buildList {
                    for (y in 0 until ROWS) for (x in 0 until COLS) {
                        val c = Cell(x, y); if (c !in snake) add(c)
                    }
                }
                if (free.isEmpty()) finish() else food = free.random()
            }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onExit) { Text("‹ МЕНЮ", color = C.text) }
            Text("${mode.title} · $score", color = C.mint, fontWeight = FontWeight.Bold)
            Text("рек ${store.bestScore(mode.name)}", color = C.muted)
        }
        Spacer(Modifier.height(8.dp))
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val w = minOf(maxWidth, maxHeight * COLS / ROWS)
            Box(
                Modifier.width(w).aspectRatio(COLS / ROWS.toFloat())
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0B1420))
                    .border(1.dp, C.cyan.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .pointerInput(phase) {
                        var dx = 0f; var dy = 0f
                        detectDragGestures(
                            onDragStart = { dx = 0f; dy = 0f },
                            onDrag = { c, d -> c.consume(); dx += d.x; dy += d.y },
                            onDragEnd = {
                                if (abs(dx) > abs(dy)) turn(if (dx > 0) Dir.RIGHT else Dir.LEFT)
                                else turn(if (dy > 0) Dir.DOWN else Dir.UP)
                            }
                        )
                    }
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val cw = size.width / COLS; val ch = size.height / ROWS
                    drawCircle(C.red, minOf(cw, ch) * 0.3f, Offset((food.x + 0.5f) * cw, (food.y + 0.5f) * ch))
                    snake.forEachIndexed { i, s ->
                        val col = if (i == 0) Color(skin.headColor) else Color(skin.bodyColor)
                        drawCircle(col, minOf(cw, ch) * (if (i == 0) 0.42f else 0.34f), Offset((s.x + 0.5f) * cw, (s.y + 0.5f) * ch))
                    }
                }
                if (phase != Phase.RUN) {
                    Box(Modifier.fillMaxSize().background(Color(0x99070D16)), contentAlignment = Alignment.Center) {
                        Glass(Modifier.fillMaxWidth(0.85f)) {
                            Text(if (phase == Phase.READY) "ГОТОВ?" else "КОНЕЦ", color = C.text, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Text(if (phase == Phase.READY) "Свайп или кнопки" else "Счёт $score", color = C.muted)
                            Spacer(Modifier.height(12.dp))
                            Box(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                    .background(Brush.horizontalGradient(listOf(C.mint, C.cyan)))
                                    .clickable {
                                        if (phase == Phase.DEAD) reset()
                                        phase = Phase.RUN
                                    }
                                    .padding(14.dp),
                                contentAlignment = Alignment.Center
                            ) { Text(if (phase == Phase.READY) "СТАРТ" else "ЕЩЁ РАЗ", color = Color(0xFF062016), fontWeight = FontWeight.Black) }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("←" to Dir.LEFT, "↑" to Dir.UP, "↓" to Dir.DOWN, "→" to Dir.RIGHT).forEach { (l, d) ->
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xCC1C2B3B))
                        .border(1.dp, C.cyan.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                        .clickable { turn(d) },
                    contentAlignment = Alignment.Center
                ) { Text(l, color = C.text, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
