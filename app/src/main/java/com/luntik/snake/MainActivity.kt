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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import org.json.JSONArray
import java.net.URL
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
        setContent { SnakeApp(ProgressStore(this)) }
    }
}

private enum class Dir { UP, DOWN, LEFT, RIGHT }
private data class Cell(val x: Int, val y: Int)
private enum class Screen { Hub, Play, Shop, Cases, Top, Profile }

private const val COLS = 16
private const val ROWS = 20

private object G {
    val Bg = Color(0xFF070A0C)
    val Panel = Color(0xFF12181C)
    val Board = Color(0xFF0E1418)
    val Grid = Color(0xFF1A242C)
    val Text = Color(0xFFE8F0F4)
    val Dim = Color.White.copy(alpha = 0.5f)
    val Mute = Color.White.copy(alpha = 0.3f)
    val Accent = Color(0xFF3DFF6E)
    val Food = Color(0xFFFF5252)
    val Border = Color.White.copy(alpha = 0.12f)
    val Gold = Color(0xFFFFD54F)
}

@Composable
fun SnakeApp(store: ProgressStore) {
    var screen by remember { mutableStateOf(Screen.Hub) }
    var mode by remember { mutableStateOf(GameMode.CLASSIC) }
    var tick by remember { mutableIntStateOf(0) }
    fun refresh() { tick++ }

    Box(Modifier.fillMaxSize().background(G.Bg)) {
        when (screen) {
            Screen.Hub -> HubScreen(
                store = store,
                tick = tick,
                onPlay = { m -> mode = m; screen = Screen.Play },
                onShop = { screen = Screen.Shop },
                onCases = { screen = Screen.Cases },
                onTop = { screen = Screen.Top },
                onProfile = { screen = Screen.Profile }
            )
            Screen.Play -> PlayScreen(store, mode) {
                refresh()
                screen = Screen.Hub
            }
            Screen.Shop -> ShopScreen(store, { refresh() }) { screen = Screen.Hub }
            Screen.Cases -> CasesScreen(store, { refresh() }) { screen = Screen.Hub }
            Screen.Top -> TopScreen(store) { screen = Screen.Hub }
            Screen.Profile -> ProfileScreen(store, { refresh() }) { screen = Screen.Hub }
        }
    }
}

@Composable
private fun HubScreen(
    store: ProgressStore,
    tick: Int,
    onPlay: (GameMode) -> Unit,
    onShop: () -> Unit,
    onCases: () -> Unit,
    onTop: () -> Unit,
    onProfile: () -> Unit
) {
    @Suppress("UNUSED_VARIABLE") val t = tick
    val skin = ShopData.skin(store.selectedSkin)
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(20.dp).verticalScroll(rememberScrollState())
    ) {
        Text("ЗМЕЙКА", color = G.Accent, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("${store.nickname} · ур. ${store.level}", color = G.Dim, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatChip("Монеты", "${store.coins}", G.Gold)
            StatChip("XP", "${store.xp}", G.Accent)
            StatChip("Скин", skin.name, Color(skin.rarity.color))
        }
        Spacer(Modifier.height(20.dp))
        Text("Режим", color = G.Mute, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        GameMode.entries.forEach { m ->
            Panel {
                Row(
                    Modifier.fillMaxWidth().clickable { onPlay(m) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(m.title, color = G.Text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text(m.desc, color = G.Dim, fontSize = 12.sp)
                    }
                    Text("▶", color = G.Accent, fontSize = 20.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallBtn("Магазин", Modifier.weight(1f), onShop)
            SmallBtn("Кейсы", Modifier.weight(1f), onCases)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallBtn("Топ", Modifier.weight(1f), onTop)
            SmallBtn("Профиль", Modifier.weight(1f), onProfile)
        }
    }
}

@Composable
private fun PlayScreen(store: ProgressStore, mode: GameMode, onExit: () -> Unit) {
    val skin = ShopData.skin(store.selectedSkin)
    var running by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var dir by remember { mutableStateOf(Dir.RIGHT) }
    var nextDir by remember { mutableStateOf(Dir.RIGHT) }
    var snake by remember { mutableStateOf(listOf(Cell(3, 10), Cell(2, 10), Cell(1, 10))) }
    var food by remember { mutableStateOf(Cell(10, 8)) }
    var rewarded by remember { mutableStateOf(false) }

    fun spawnFood(body: List<Cell>): Cell {
        var c: Cell
        do { c = Cell(Random.nextInt(COLS), Random.nextInt(ROWS)) } while (c in body)
        return c
    }

    fun reset() {
        snake = listOf(Cell(3, 10), Cell(2, 10), Cell(1, 10))
        dir = Dir.RIGHT
        nextDir = Dir.RIGHT
        food = spawnFood(snake)
        score = 0
        gameOver = false
        rewarded = false
        running = true
    }

    fun turn(d: Dir) {
        val opp = when (dir) {
            Dir.UP -> Dir.DOWN; Dir.DOWN -> Dir.UP; Dir.LEFT -> Dir.RIGHT; Dir.RIGHT -> Dir.LEFT
        }
        if (d != opp) nextDir = d
    }

    LaunchedEffect(running, gameOver, mode) {
        while (running && !gameOver) {
            delay(mode.speedMs)
            dir = nextDir
            val head = snake.first()
            val newHead = when (dir) {
                Dir.UP -> Cell(head.x, head.y - 1)
                Dir.DOWN -> Cell(head.x, head.y + 1)
                Dir.LEFT -> Cell(head.x - 1, head.y)
                Dir.RIGHT -> Cell(head.x + 1, head.y)
            }
            if (newHead.x !in 0 until COLS || newHead.y !in 0 until ROWS || newHead in snake) {
                gameOver = true
                running = false
                if (!rewarded) {
                    rewarded = true
                    val coinsGain = (score / 10 * mode.coinMul).toInt().coerceAtLeast(if (score > 0) 1 else 0)
                    val xpGain = (score / 5 * mode.xpMul).toInt()
                    store.addCoins(coinsGain)
                    store.addXp(xpGain)
                    store.pushScore(store.nickname, score, mode.name)
                }
                break
            }
            val grew = newHead == food
            snake = if (grew) listOf(newHead) + snake else listOf(newHead) + snake.dropLast(1)
            if (grew) {
                score += 10
                food = spawnFood(snake)
            }
        }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${mode.title} · $score", color = G.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("Рекорд ${store.bestScore(mode.name)}", color = G.Dim, fontSize = 13.sp)
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(COLS.toFloat() / ROWS)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF101820), Color(0xFF0A1014)))
                )
                .border(1.dp, G.Border, RoundedCornerShape(16.dp))
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
                for (x in 0..COLS) drawLine(G.Grid, Offset(x * cw, 0f), Offset(x * cw, size.height), 1f)
                for (y in 0..ROWS) drawLine(G.Grid, Offset(0f, y * ch), Offset(size.width, y * ch), 1f)
                // food glow
                drawCircle(
                    G.Food.copy(alpha = 0.25f),
                    radius = cw * 0.7f,
                    center = Offset(food.x * cw + cw / 2, food.y * ch + ch / 2)
                )
                drawRoundRect(
                    G.Food,
                    topLeft = Offset(food.x * cw + 3, food.y * ch + 3),
                    size = Size(cw - 6, ch - 6),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                val headC = Color(skin.headColor)
                val bodyC = Color(skin.bodyColor)
                snake.forEachIndexed { i, c ->
                    val col = if (i == 0) headC else bodyC.copy(alpha = (1f - i * 0.03f).coerceAtLeast(0.4f))
                    drawRoundRect(
                        col,
                        topLeft = Offset(c.x * cw + 2f, c.y * ch + 2f),
                        size = Size(cw - 4f, ch - 4f),
                        cornerRadius = CornerRadius(7f, 7f)
                    )
                    if (i == 0) {
                        drawCircle(Color.White.copy(alpha = 0.9f), 2.5f, Offset(c.x * cw + cw * 0.35f, c.y * ch + ch * 0.35f))
                        drawCircle(Color.White.copy(alpha = 0.9f), 2.5f, Offset(c.x * cw + cw * 0.65f, c.y * ch + ch * 0.35f))
                    }
                }
            }
            if (!running && !gameOver) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(0.5f)), contentAlignment = Alignment.Center) {
                    Text("Старт", color = G.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (gameOver) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(0.6f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Конец", color = G.Food, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Счёт $score", color = G.Text, fontSize = 16.sp)
                        Text("+монеты и XP зачислены", color = G.Dim, fontSize = 12.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            DirBtn("▲") { turn(Dir.UP) }
            Row {
                DirBtn("◀") { turn(Dir.LEFT) }
                Spacer(Modifier.size(44.dp))
                DirBtn("▶") { turn(Dir.RIGHT) }
            }
            DirBtn("▼") { turn(Dir.DOWN) }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionBtn(if (!running || gameOver) "Старт" else "Заново") { reset() }
            ActionBtn("Выход") { onExit() }
        }
    }
}

@Composable
private fun ShopScreen(store: ProgressStore, onChange: () -> Unit, onBack: () -> Unit) {
    var msg by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(16.dp).verticalScroll(rememberScrollState())
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Магазин", color = G.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("${store.coins} монет", color = G.Gold, fontSize = 14.sp)
        }
        Spacer(Modifier.height(12.dp))
        ShopData.skins.forEach { skin ->
            val unlocked = store.isUnlocked(skin.id)
            val selected = store.selectedSkin == skin.id
            Panel {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(skin.name, color = G.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(skin.rarity.title, color = Color(skin.rarity.color), fontSize = 12.sp)
                        if (skin.unlockOnlyCase) Text("Только из кейса", color = G.Mute, fontSize = 11.sp)
                    }
                    Box(
                        Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
                            .background(Color(skin.bodyColor))
                            .border(2.dp, Color(skin.headColor), RoundedCornerShape(6.dp))
                    )
                    Spacer(Modifier.size(10.dp))
                    when {
                        selected -> Text("Надето", color = G.Accent, fontSize = 13.sp)
                        unlocked -> Text("Надеть", color = G.Text, fontSize = 13.sp, modifier = Modifier.clickable {
                            store.selectedSkin = skin.id
                            msg = "Скин: ${skin.name}"
                            onChange()
                        })
                        skin.unlockOnlyCase -> Text("Кейс", color = G.Mute, fontSize = 13.sp)
                        else -> Text("${skin.price}", color = G.Gold, fontSize = 13.sp, modifier = Modifier.clickable {
                            if (store.spendCoins(skin.price)) {
                                store.unlockSkin(skin.id)
                                store.selectedSkin = skin.id
                                msg = "Куплено: ${skin.name}"
                                onChange()
                            } else msg = "Мало монет"
                        })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (msg.isNotEmpty()) Text(msg, color = G.Dim, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        ActionBtn("Назад") { onBack() }
    }
}

@Composable
private fun CasesScreen(store: ProgressStore, onChange: () -> Unit, onBack: () -> Unit) {
    var lastDrop by remember { mutableStateOf<SnakeSkin?>(null) }
    var msg by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(16.dp).verticalScroll(rememberScrollState())
    ) {
        Text("Кейсы", color = G.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("${store.coins} монет", color = G.Gold, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        ShopData.cases.forEach { c ->
            Panel {
                Text(c.name, color = G.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("${c.price} монет", color = G.Gold, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                ActionBtn("Открыть") {
                    if (!store.spendCoins(c.price)) {
                        msg = "Мало монет"
                        return@ActionBtn
                    }
                    val drop = ShopData.openCase(c)
                    store.unlockSkin(drop.id)
                    lastDrop = drop
                    msg = "Выпало: ${drop.name} (${drop.rarity.title})"
                    onChange()
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        lastDrop?.let { s ->
            Spacer(Modifier.height(8.dp))
            Panel {
                Text("Дроп", color = G.Mute, fontSize = 12.sp)
                Text(s.name, color = Color(s.rarity.color), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(s.rarity.title, color = G.Dim, fontSize = 13.sp)
            }
        }
        if (msg.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(msg, color = G.Dim, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        ActionBtn("Назад") { onBack() }
    }
}

@Composable
private fun TopScreen(store: ProgressStore, onBack: () -> Unit) {
    var remote by remember { mutableStateOf<List<LeaderEntry>>(emptyList()) }
    var status by remember { mutableStateOf("Загрузка…") }
    val local = store.leaderboard()

    LaunchedEffect(Unit) {
        try {
            val txt = URL("https://raw.githubusercontent.com/LuntikVisuals/Snake2D/main/leaderboard.json").readText()
            val arr = JSONArray(txt)
            remote = (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                LeaderEntry(o.getString("name"), o.getInt("score"), o.optString("mode", "CLASSIC"))
            }.sortedByDescending { it.score }.take(30)
            status = "Сеть · ${remote.size}"
        } catch (_: Exception) {
            status = "Сеть недоступна — локальный топ"
        }
    }

    val merged = (remote + local).sortedByDescending { it.score }.distinctBy { it.name + it.score }.take(30)

    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(16.dp).verticalScroll(rememberScrollState())
    ) {
        Text("Топ игроков", color = G.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(status, color = G.Dim, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        if (merged.isEmpty()) {
            Text("Пока пусто. Сыграй партию!", color = G.Mute, fontSize = 14.sp)
        } else {
            merged.forEachIndexed { i, e ->
                Panel {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${i + 1}. ${e.name}", color = G.Text, fontSize = 15.sp)
                        Text("${e.score}", color = G.Accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(e.mode, color = G.Mute, fontSize = 11.sp)
                }
                Spacer(Modifier.height(6.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Локальные очки всегда сохраняются. Глобальный список — leaderboard.json на GitHub.", color = G.Mute, fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))
        ActionBtn("Назад") { onBack() }
    }
}

@Composable
private fun ProfileScreen(store: ProgressStore, onChange: () -> Unit, onBack: () -> Unit) {
    var nick by remember { mutableStateOf(store.nickname) }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)
    ) {
        Text("Профиль", color = G.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Ник для топа", color = G.Mute, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(G.Panel)
                .border(1.dp, G.Border, RoundedCornerShape(12.dp)).padding(14.dp)
        ) {
            BasicTextField(
                value = nick,
                onValueChange = { nick = it.take(16) },
                textStyle = TextStyle(color = G.Text, fontSize = 16.sp),
                cursorBrush = SolidColor(G.Accent),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(12.dp))
        ActionBtn("Сохранить") {
            store.nickname = nick
            onChange()
            onBack()
        }
        Spacer(Modifier.height(8.dp))
        ActionBtn("Назад") { onBack() }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color) {
    Column {
        Text(label, color = G.Mute, fontSize = 11.sp)
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Panel(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(G.Panel)
            .border(1.dp, G.Border, RoundedCornerShape(14.dp)).padding(14.dp)
    ) { content() }
}

@Composable
private fun DirBtn(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(G.Panel).border(1.dp, G.Border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(label, color = G.Accent, fontSize = 18.sp) }
}

@Composable
private fun ActionBtn(text: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(12.dp)).background(G.Accent).clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp)
    ) { Text(text, color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun SmallBtn(text: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(12.dp)).background(G.Panel).border(1.dp, G.Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = G.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
}
