package com.luntik.snake

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
private const val FEED_COLS = 24
private const val FEED_ROWS = 28
const val APP_TITLE = "Snake2D 2.0.0.1 Beta"

private enum class Dir { UP, DOWN, LEFT, RIGHT }
private enum class Phase { READY, RUN, DEAD }
private enum class Scr { HUB, PLAY, SHOP, CASES, CHARS, FEED }
private data class Cell(val x: Int, val y: Int)

private object C {
    val bg = Color(0xFF070B12)
    val text = Color(0xFFF2F7FF)
    val muted = Color(0xFF9AAABD)
    val mint = Color(0xFF67F5B4)
    val cyan = Color(0xFF65DDFB)
    val gold = Color(0xFFFFD36E)
    val line = Color.White.copy(alpha = 0.12f)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        GameFiles.ensureLayout(this)
        val store = ProgressStore(this)
        AntiCheat.scan(this, store).also { GameFiles.writeSessionLog(this, "boot ${it.detail}") }
        setContent { App(store) }
    }
}

@Composable
private fun App(store: ProgressStore) {
    var scr by remember { mutableStateOf(Scr.HUB) }
    var mode by remember { mutableStateOf(GameMode.CLASSIC) }
    var tick by remember { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0A1320), C.bg, Color(0xFF090D16))))) {
        when (scr) {
            Scr.HUB -> Hub(store, tick,
                onPlay = { mode = it; if (it == GameMode.FEEDING) scr = Scr.CHARS else scr = Scr.PLAY },
                onShop = { scr = Scr.SHOP },
                onCases = { scr = Scr.CASES },
                onChars = { scr = Scr.CHARS },
                onRefresh = { tick++ })
            Scr.PLAY -> Play(store, mode) { tick++; scr = Scr.HUB }
            Scr.SHOP -> Shop(store, { tick++ }) { scr = Scr.HUB }
            Scr.CASES -> Cases(store, { tick++ }) { scr = Scr.HUB }
            Scr.CHARS -> Chars(store, { tick++ }, onPlay = { scr = Scr.FEED }, onBack = { scr = Scr.HUB })
            Scr.FEED -> FeedPlay(store) { tick++; scr = Scr.HUB }
        }
    }
}

@Composable
private fun Glass(mod: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        mod.clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.04f))))
            .border(1.dp, C.line, RoundedCornerShape(18.dp)).padding(14.dp),
        content = content
    )
}

@Composable
private fun Hub(
    store: ProgressStore, tick: Int,
    onPlay: (GameMode) -> Unit, onShop: () -> Unit, onCases: () -> Unit, onChars: () -> Unit, onRefresh: () -> Unit
) {
    @Suppress("UNUSED_VARIABLE") val t = tick
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(18.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(APP_TITLE, color = C.mint, fontSize = 26.sp, fontWeight = FontWeight.Black)
        Text("liquid glass · full body snake", color = C.muted, fontSize = 12.sp)
        Glass(Modifier.fillMaxWidth()) {
            Text("${store.nickname} · ур.${store.level}", color = C.text, fontWeight = FontWeight.Bold)
            Text("${store.coins} монет · ${store.xp} XP", color = C.gold, fontSize = 14.sp)
        }
        if (store.canClaimDaily()) {
            Glass(Modifier.fillMaxWidth().clickable { store.claimDaily(); onRefresh() }) {
                Text("ЕЖЕДНЕВНАЯ НАГРАДА — ЗАБРАТЬ", color = C.gold, fontWeight = FontWeight.Bold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Glass(Modifier.weight(1f).clickable(onClick = onShop)) { Text("МАГАЗИН", color = C.text, fontWeight = FontWeight.Bold) }
            Glass(Modifier.weight(1f).clickable(onClick = onCases)) { Text("КЕЙСЫ", color = C.text, fontWeight = FontWeight.Bold) }
        }
        Glass(Modifier.fillMaxWidth().clickable(onClick = onChars)) {
            Text("ПЕРСОНАЖИ · ПОЕДАНИЕ", color = C.text, fontWeight = FontWeight.Bold)
            Text("Превью и способности", color = C.muted, fontSize = 12.sp)
        }
        Text("РЕЖИМЫ", color = C.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        GameMode.entries.forEach { m ->
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
    }
}

@Composable
private fun Shop(store: ProgressStore, onChanged: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = onBack) { Text("‹ НАЗАД", color = C.text) }
        Text("МАГАЗИН", color = C.mint, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("${store.coins} монет · у каждого скина своя редкость", color = C.gold, fontSize = 13.sp)
        ShopData.skins.forEach { skin ->
            val unlocked = store.isUnlocked(skin.id)
            val selected = store.selectedSkin == skin.id
            Glass(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Color(skin.bodyColor)).border(2.dp, Color(skin.headColor), RoundedCornerShape(10.dp)))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(skin.name, color = C.text, fontWeight = FontWeight.Bold)
                        Text(skin.rarity.title, color = Color(skin.rarity.color), fontSize = 12.sp)
                    }
                    when {
                        selected -> Text("НАДЕТ", color = C.mint, fontSize = 12.sp)
                        unlocked -> Text("НАДЕТЬ", color = C.cyan, fontSize = 12.sp, modifier = Modifier.clickable {
                            store.selectedSkin = skin.id; onChanged()
                        })
                        skin.unlockOnlyCase -> Text("ТОЛЬКО КЕЙС", color = C.muted, fontSize = 11.sp)
                        else -> Text("${skin.price}", color = C.gold, fontSize = 13.sp, modifier = Modifier.clickable {
                            if (store.spendCoins(skin.price, "Скин ${skin.name}")) {
                                store.unlockSkin(skin.id); store.selectedSkin = skin.id
                                store.logPurchase(skin.name, skin.price); onChanged()
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun Cases(store: ProgressStore, onChanged: () -> Unit, onBack: () -> Unit) {
    var msg by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = onBack) { Text("‹ НАЗАД", color = C.text) }
        Text("КЕЙСЫ", color = C.mint, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("${store.coins} монет", color = C.gold)
        ShopData.cases.forEach { c ->
            Glass(Modifier.fillMaxWidth()) {
                Text(c.name, color = C.text, fontWeight = FontWeight.Bold)
                Text("${c.price} монет", color = C.gold, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(listOf(C.mint, C.cyan)))
                        .clickable {
                            if (!store.spendCoins(c.price, "Кейс ${c.name}")) { msg = "Мало монет"; return@clickable }
                            val drop = ShopData.openCase(c)
                            val dup = store.isUnlocked(drop.id)
                            if (dup) {
                                store.addCoins(20, "Дубликат ${drop.name}")
                                msg = "Дубликат ${drop.name} (+20)"
                            } else {
                                store.unlockSkin(drop.id)
                                msg = "Выпало: ${drop.name} [${drop.rarity.title}]"
                            }
                            store.recordCaseOpened(); store.logCase(c.name, drop.name, dup); onChanged()
                        }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) { Text("ОТКРЫТЬ", color = Color(0xFF062016), fontWeight = FontWeight.Black) }
            }
        }
        if (msg.isNotEmpty()) Text(msg, color = C.muted, fontSize = 13.sp)
    }
}

@Composable
private fun Chars(store: ProgressStore, onChanged: () -> Unit, onPlay: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = onBack) { Text("‹ НАЗАД", color = C.text) }
        Text("ПЕРСОНАЖИ", color = C.mint, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("Осмотр перед Поеданием · ${store.coins} монет", color = C.muted, fontSize = 13.sp)
        CharacterData.all.forEach { ch ->
            val unlocked = store.isCharacterUnlocked(ch.id)
            val selected = store.selectedCharacter == ch.id
            Glass(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color(ch.bodyColor)).border(2.dp, Color(ch.headColor), RoundedCornerShape(12.dp)))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(ch.name, color = C.text, fontWeight = FontWeight.Bold)
                        Text(ch.rarity.title, color = Color(ch.rarity.color), fontSize = 12.sp)
                        Text(ch.description, color = C.muted, fontSize = 12.sp)
                        ch.ability?.let { ab ->
                            Text("Способность: ${ab.name} (КД ${ab.cooldownSec}с)", color = C.cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            ab.effects.forEach { e ->
                                Text("· ${e.title}: ${e.description}", color = C.muted, fontSize = 11.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    when {
                        selected -> Text("ВЫБРАН", color = C.mint, fontWeight = FontWeight.Bold)
                        unlocked -> Text("ВЫБРАТЬ", color = C.cyan, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                            store.selectedCharacter = ch.id; onChanged()
                        })
                        else -> Text("КУПИТЬ ${ch.unlockPrice}", color = C.gold, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                            if (store.spendCoins(ch.unlockPrice, "Персонаж ${ch.name}")) {
                                store.unlockCharacter(ch.id); store.selectedCharacter = ch.id; onChanged()
                            }
                        })
                    }
                }
            }
        }
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Brush.horizontalGradient(listOf(C.mint, C.cyan)))
                .clickable(onClick = onPlay).padding(14.dp),
            contentAlignment = Alignment.Center
        ) { Text("ИГРАТЬ В ПОЕДАНИЕ", color = Color(0xFF062016), fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun Play(store: ProgressStore, mode: GameMode, onExit: () -> Unit) {
    SnakeGame(
        store = store, cols = COLS, rows = ROWS, mode = mode, title = mode.title,
        headColor = Color(ShopData.skin(store.selectedSkin).headColor),
        bodyColor = Color(ShopData.skin(store.selectedSkin).bodyColor),
        onExit = onExit
    )
}

@Composable
private fun FeedPlay(store: ProgressStore, onExit: () -> Unit) {
    val ch = CharacterData.all.find { it.id == store.selectedCharacter } ?: CharacterData.all.first()
    SnakeGame(
        store = store, cols = FEED_COLS, rows = FEED_ROWS, mode = GameMode.FEEDING,
        title = "Поедание · ${ch.name}",
        headColor = Color(ch.headColor), bodyColor = Color(ch.bodyColor),
        onExit = onExit,
        abilityLabel = ch.ability?.let { "${it.name} (КД ${it.cooldownSec}с)" }
    )
}

@Composable
private fun SnakeGame(
    store: ProgressStore, cols: Int, rows: Int, mode: GameMode, title: String,
    headColor: Color, bodyColor: Color, onExit: () -> Unit, abilityLabel: String? = null
) {
    var phase by remember { mutableStateOf(Phase.READY) }
    var score by remember { mutableIntStateOf(0) }
    var dir by remember { mutableStateOf(Dir.RIGHT) }
    var next by remember { mutableStateOf(Dir.RIGHT) }
    var snake by remember { mutableStateOf(listOf(Cell(4, rows / 2), Cell(3, rows / 2), Cell(2, rows / 2))) }
    var food by remember { mutableStateOf(Cell(cols / 2, rows / 3)) }
    var rewarded by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(1f) }
    val startMs = remember { System.currentTimeMillis() }

    fun spawn(body: List<Cell>): Cell {
        var c: Cell
        do { c = Cell(Random.nextInt(cols), Random.nextInt(rows)) } while (c in body)
        return c
    }
    fun reset() {
        snake = listOf(Cell(4, rows / 2), Cell(3, rows / 2), Cell(2, rows / 2))
        dir = Dir.RIGHT; next = Dir.RIGHT; food = spawn(snake)
        score = 0; rewarded = false; progress = 1f; phase = Phase.READY
    }
    fun finish() {
        phase = Phase.DEAD
        if (rewarded) return
        rewarded = true
        val foodEaten = score / 10
        val fair = AntiCheat.validateScore(score, System.currentTimeMillis() - startMs, foodEaten)
        val coins = (score * mode.coinMul).toInt().coerceAtLeast(if (score > 0) 3 else 0)
        if (fair) {
            store.addCoins(coins, "Партия $title")
            store.addXp((score * mode.xpMul).toInt().coerceAtLeast(3))
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
            val step = mode.speedMs.coerceAtLeast(55L)
            val frames = 5
            val fd = (step / frames).coerceAtLeast(10L)
            for (f in 1..frames) {
                progress = f / frames.toFloat()
                delay(fd)
                if (phase != Phase.RUN) break
            }
            progress = 0f
            if (phase != Phase.RUN) break
            dir = next
            val h = snake.first()
            var nx = h.x; var ny = h.y
            when (dir) {
                Dir.UP -> ny--; Dir.DOWN -> ny++; Dir.LEFT -> nx--; Dir.RIGHT -> nx++
            }
            if (!mode.wallsKill) {
                if (nx < 0) nx = cols - 1; if (nx >= cols) nx = 0
                if (ny < 0) ny = rows - 1; if (ny >= rows) ny = 0
            }
            val nh = Cell(nx, ny)
            val eat = nh == food
            val body = if (eat) snake else snake.dropLast(1)
            val wall = mode.wallsKill && (nx !in 0 until cols || ny !in 0 until rows)
            if (wall || nh in body) { finish(); break }
            snake = listOf(nh) + body
            if (eat) {
                score += 10
                val free = buildList {
                    for (y in 0 until rows) for (x in 0 until cols) {
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$title · $score", color = C.mint, fontWeight = FontWeight.Bold)
                abilityLabel?.let { Text(it, color = C.muted, fontSize = 11.sp) }
            }
            Text("рек ${store.bestScore(mode.name)}", color = C.muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val w = minOf(maxWidth, maxHeight * cols / rows)
            Box(
                Modifier.width(w).aspectRatio(cols / rows.toFloat())
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
                SmoothSnakeBoard(
                    cols = cols, rows = rows,
                    snake = snake.map { RenderCell(it.x, it.y) },
                    food = RenderCell(food.x, food.y),
                    dir = when (dir) {
                        Dir.UP -> RenderDir.UP; Dir.DOWN -> RenderDir.DOWN
                        Dir.LEFT -> RenderDir.LEFT; Dir.RIGHT -> RenderDir.RIGHT
                    },
                    headColor = headColor, bodyColor = bodyColor, progress = progress
                )
                if (phase != Phase.RUN) {
                    Box(Modifier.fillMaxSize().background(Color(0x99070D16)), contentAlignment = Alignment.Center) {
                        Glass(Modifier.fillMaxWidth(0.85f)) {
                            Text(if (phase == Phase.READY) "ГОТОВ?" else "КОНЕЦ", color = C.text, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Text(if (phase == Phase.READY) "Свайп или кнопки" else "Счёт $score", color = C.muted)
                            Spacer(Modifier.height(12.dp))
                            Box(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                    .background(Brush.horizontalGradient(listOf(C.mint, C.cyan)))
                                    .clickable { if (phase == Phase.DEAD) reset(); phase = Phase.RUN }
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
