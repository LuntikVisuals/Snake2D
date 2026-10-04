package com.luntik.snake

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

internal suspend fun smoothStep(stepMs: Long, targetFps: Int = 120, onProgress: (Float) -> Unit) {
    val frameMs = (1000f / targetFps.coerceIn(60, 144)).toLong().coerceAtLeast(6L)
    val frames = (stepMs / frameMs).toInt().coerceIn(6, 24)
    val fd = (stepMs / frames).coerceAtLeast(frameMs)
    for (f in 1..frames) { onProgress(f / frames.toFloat()); delay(fd) }
}

@Composable
internal fun ControlPad(onDir: (Dir) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CtrlBtn("▲", 64) { onDir(Dir.UP) }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CtrlBtn("◀", 64) { onDir(Dir.LEFT) }
            CtrlBtn("▼", 64) { onDir(Dir.DOWN) }
            CtrlBtn("▶", 64) { onDir(Dir.RIGHT) }
        }
    }
}

@Composable
internal fun CtrlBtn(label: String, size: Int, onClick: () -> Unit) {
    Box(Modifier.size(size.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xDD1C2B3B))
        .border(1.5.dp, C.cyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center) { Text(label, color = C.text, fontSize = 26.sp, fontWeight = FontWeight.Bold) }
}

internal fun Modifier.tapDir(enabled: Boolean, onDir: (Dir) -> Unit, current: Dir): Modifier = pointerInput(enabled, current) {
    if (!enabled) return@pointerInput
    detectTapGestures { offset ->
        val cx = size.width / 2f; val cy = size.height / 2f
        val dx = offset.x - cx; val dy = offset.y - cy
        if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) onDir(if (dx > 0) Dir.RIGHT else Dir.LEFT)
        else onDir(if (dy > 0) Dir.DOWN else Dir.UP)
    }
}

internal fun Modifier.swipeDir(enabled: Boolean, onDir: (Dir) -> Unit): Modifier = pointerInput(enabled) {
    if (!enabled) return@pointerInput
    var accX = 0f; var accY = 0f; var fired = false
    detectDragGestures(
        onDragStart = { accX = 0f; accY = 0f; fired = false },
        onDrag = { change, drag ->
            change.consume(); if (fired) return@detectDragGestures
            accX += drag.x; accY += drag.y
            if (abs(accX) > 24f || abs(accY) > 24f) {
                fired = true
                if (abs(accX) > abs(accY)) onDir(if (accX > 0) Dir.RIGHT else Dir.LEFT)
                else onDir(if (accY > 0) Dir.DOWN else Dir.UP)
            }
        },
        onDragEnd = { }
    )
}

@Composable
internal fun OverlayReadyDead(phase: Phase, score: Int, onStart: () -> Unit, rewardLine: String = "", control: String = "buttons", onControl: (String) -> Unit = {}) {
    Box(Modifier.fillMaxSize().background(Color(0x99070D16)), contentAlignment = Alignment.Center) {
        Glass(Modifier.fillMaxWidth(0.85f)) {
            Text(if (phase == Phase.READY) "ГОТОВ?" else "КОНЕЦ", color = C.text, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(if (phase == Phase.READY) "Выбери управление" else "Рекорд партии: $score", color = C.muted)
            if (phase == Phase.DEAD && rewardLine.isNotEmpty()) Text(rewardLine, color = C.gold, fontSize = 13.sp)
            if (phase == Phase.DEAD) Text("XP и tix уже начислены", color = C.cyan, fontSize = 12.sp)
            if (phase == Phase.READY) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("gestures" to "Жесты", "buttons" to "Кнопки").forEach { (id, title) ->
                        Text(title, color = if (control == id) C.mint else C.text, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onControl(id) }.padding(6.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Brush.horizontalGradient(listOf(C.mint, C.cyan))).clickable(onClick = onStart).padding(14.dp),
                contentAlignment = Alignment.Center) {
                Text(if (phase == Phase.READY) "СТАРТ" else "ЕЩЁ РАЗ", color = Color(0xFF062016), fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
internal fun ClassicPlay(store: ProgressStore, mode: GameMode, onExit: () -> Unit) {
    val skin = ShopData.skin(store.selectedSkin)
    val apple = ShopData.apple(store.selectedApple)
    val cols = COLS; val rows = ROWS
    var phase by remember { mutableStateOf(Phase.READY) }
    var score by remember { mutableIntStateOf(0) }
    var dir by remember { mutableStateOf(Dir.RIGHT) }
    var next by remember { mutableStateOf(Dir.RIGHT) }
    var snake by remember { mutableStateOf(listOf(Cell(4, 10), Cell(3, 10), Cell(2, 10))) }
    var food by remember { mutableStateOf(Cell(10, 8)) }
    var progress by remember { mutableFloatStateOf(1f) }
    var rewarded by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var rewardLine by remember { mutableStateOf("") }
    var control by remember { mutableStateOf(store.controlMode) }
    val startMs = remember { System.currentTimeMillis() }
    fun spawn(body: List<Cell>): Cell {
        var c: Cell; do { c = Cell(Random.nextInt(cols), Random.nextInt(rows)) } while (c in body); return c
    }
    fun turn(d: Dir) {
        val opp = when (dir) { Dir.UP -> Dir.DOWN; Dir.DOWN -> Dir.UP; Dir.LEFT -> Dir.RIGHT; Dir.RIGHT -> Dir.LEFT }
        if (d != opp) next = d
    }
    fun reset() {
        snake = listOf(Cell(4, 10), Cell(3, 10), Cell(2, 10)); dir = Dir.RIGHT; next = Dir.RIGHT; food = spawn(snake)
        score = 0; rewarded = false; progress = 1f; paused = false; rewardLine = ""; phase = Phase.READY
    }
    fun finish() {
        if (phase == Phase.DEAD) return; phase = Phase.DEAD; progress = 1f; if (rewarded) return; rewarded = true
        val foodEaten = score / ProgressStore.POINTS_PER_APPLE
        if (AntiCheat.validateScore(score, System.currentTimeMillis() - startMs, foodEaten)) {
            val before = store.level
            val (c, gm, tx) = store.grantMatchRewards(score, mode)
            val up = store.level > before
            rewardLine = "+$c монет · +$gm гемов · +$tx tix" + if (up) " · уровень!" else ""
            store.pushScore(store.nickname, score, mode.name)
        }
        store.recordGameEnd(false, foodEaten, snake.size); store.checkAchievementsAfterGame(score, snake.size, false); store.saveBackup()
    }
    LaunchedEffect(phase, mode, paused) {
        while (phase == Phase.RUN && !paused) {
            dir = next
            val h = snake.first()
            var nx = h.x; var ny = h.y
            when (dir) { Dir.UP -> ny--; Dir.DOWN -> ny++; Dir.LEFT -> nx--; Dir.RIGHT -> nx++ }
            if (!mode.wallsKill) {
                if (nx < 0) nx = cols - 1; if (nx >= cols) nx = 0
                if (ny < 0) ny = rows - 1; if (ny >= rows) ny = 0
            }
            val nh = Cell(nx, ny)
            val eat = nh == food
            val body = if (eat) snake else snake.dropLast(1)
            val wall = mode.wallsKill && (nx !in 0 until cols || ny !in 0 until rows)
            if (wall || nh in body) { progress = 1f; finish(); break }
            progress = 0f
            snake = listOf(nh) + body
            smoothStep(mode.speedMs.coerceAtLeast(60L), store.targetFps) { progress = it }
            if (phase != Phase.RUN || paused) break
            progress = 1f
            if (eat) {
                score += ProgressStore.POINTS_PER_APPLE
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onExit) { Text("‹ МЕНЮ", color = C.text) }
            Text("${mode.title} · $score", color = C.mint, fontWeight = FontWeight.Bold)
            if (phase == Phase.RUN) {
                Text(if (paused) "▶" else "⏸", color = C.text, fontSize = 22.sp, modifier = Modifier.clickable { paused = !paused }.padding(6.dp))
            }
            Text("рек ${store.bestScore(mode.name)}", color = C.muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(6.dp))
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val w = minOf(maxWidth, maxHeight * cols / rows)
            Box(Modifier.width(w).aspectRatio(cols / rows.toFloat()).clip(RoundedCornerShape(16.dp)).background(Color(0xFF0B1420))
                .border(1.dp, C.cyan.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                .then(if (control == "gestures") Modifier.tapDir(phase == Phase.RUN && !paused, { turn(it) }, dir) else Modifier.swipeDir(phase == Phase.RUN && !paused) { turn(it) })) {
                SmoothSnakeBoard(cols, rows, snake.map { RenderCell(it.x, it.y) }, RenderCell(food.x, food.y),
                    when (dir) { Dir.UP -> RenderDir.UP; Dir.DOWN -> RenderDir.DOWN; Dir.LEFT -> RenderDir.LEFT; Dir.RIGHT -> RenderDir.RIGHT },
                    Color(skin.headColor), Color(skin.bodyColor), foodColor = Color(apple.color),
                    progress = if (phase == Phase.DEAD) 1f else progress, showGrid = store.showGrid)
                if (paused && phase == Phase.RUN) {
                    Box(Modifier.fillMaxSize().background(Color(0x88000000)), contentAlignment = Alignment.Center) {
                        Text("ПАУЗА", color = C.text, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    }
                }
                if (phase != Phase.RUN) OverlayReadyDead(phase, score, { if (phase == Phase.DEAD) reset(); phase = Phase.RUN }, rewardLine, control) { control = it; store.controlMode = it }
            }
        }
        if (control != "gestures" && phase == Phase.RUN) { Spacer(Modifier.height(10.dp)); ControlPad { turn(it) } }
        Spacer(Modifier.height(8.dp))
    }
}

internal data class EnemySnake(
    var body: List<Cell>, var dir: Dir, var score: Int, val color: Color, val name: String,
    val ultId: String = "boost", var ultCd: Int = 0
)

@Composable
internal fun FeedingPlay(store: ProgressStore, onExit: () -> Unit) {
    val ch = CharacterData.byId(store.selectedCharacter)
    val cols = FEED_COLS; val rows = FEED_ROWS
    var phase by remember { mutableStateOf(Phase.READY) }
    var score by remember { mutableIntStateOf(0) }
    var dir by remember { mutableStateOf(Dir.RIGHT) }
    var next by remember { mutableStateOf(Dir.RIGHT) }
    var snake by remember { mutableStateOf(listOf(Cell(6, 14), Cell(5, 14), Cell(4, 14))) }
    var foods by remember { mutableStateOf(listOf(Cell(12, 10), Cell(18, 20), Cell(8, 22))) }
    var progress by remember { mutableFloatStateOf(1f) }
    var enemies by remember {
        mutableStateOf(listOf(
            EnemySnake(listOf(Cell(18, 6), Cell(19, 6), Cell(20, 6)), Dir.LEFT, 0, Color(0xFFFF7043), "Оранж", "boost", 0),
            EnemySnake(listOf(Cell(6, 22), Cell(6, 23), Cell(6, 24)), Dir.UP, 0, Color(0xFF4FC3F7), "Лёд", "freeze", 0),
            EnemySnake(listOf(Cell(20, 22), Cell(19, 22), Cell(18, 22)), Dir.LEFT, 0, Color(0xFFE040FB), "Неон", "lightning", 0),
            EnemySnake(listOf(Cell(12, 4), Cell(12, 5), Cell(12, 6)), Dir.DOWN, 0, Color(0xFFFFD54F), "Голд", "dash", 0)
        ))
    }
    var ultCd by remember { mutableIntStateOf(0) }
    var invulnLeft by remember { mutableIntStateOf(0) }
    var boostLeft by remember { mutableIntStateOf(0) }
    var eventLeft by remember { mutableIntStateOf(0) }
    var eventName by remember { mutableStateOf("") }
    var rewarded by remember { mutableStateOf(false) }
    var aiTick by remember { mutableIntStateOf(0) }
    var playerSlowLeft by remember { mutableIntStateOf(0) }
    val startMs = remember { System.currentTimeMillis() }
    val ability = ch.ability
    fun wrap(n: Int, max: Int) = when { n < 0 -> max - 1; n >= max -> 0; else -> n }
    fun freeCell(blocked: Set<Cell>): Cell {
        var c: Cell; var tries = 0
        do { c = Cell(Random.nextInt(cols), Random.nextInt(rows)); tries++ } while (c in blocked && tries < 200); return c
    }
    fun turn(d: Dir) {
        val opp = when (dir) { Dir.UP -> Dir.DOWN; Dir.DOWN -> Dir.UP; Dir.LEFT -> Dir.RIGHT; Dir.RIGHT -> Dir.LEFT }
        if (d != opp) next = d
    }
    fun reset() {
        snake = listOf(Cell(6, 14), Cell(5, 14), Cell(4, 14)); dir = Dir.RIGHT; next = Dir.RIGHT
        foods = listOf(Cell(12, 10), Cell(18, 20), Cell(8, 22))
        enemies = listOf(
            EnemySnake(listOf(Cell(18, 6), Cell(19, 6), Cell(20, 6)), Dir.LEFT, 0, Color(0xFFFF7043), "Оранж", "boost", 0),
            EnemySnake(listOf(Cell(6, 22), Cell(6, 23), Cell(6, 24)), Dir.UP, 0, Color(0xFF4FC3F7), "Лёд", "freeze", 0),
            EnemySnake(listOf(Cell(20, 22), Cell(19, 22), Cell(18, 22)), Dir.LEFT, 0, Color(0xFFE040FB), "Неон", "lightning", 0),
            EnemySnake(listOf(Cell(12, 4), Cell(12, 5), Cell(12, 6)), Dir.DOWN, 0, Color(0xFFFFD54F), "Голд", "dash", 0)
        )
        score = 0; rewarded = false; progress = 1f; ultCd = 0; invulnLeft = 0; boostLeft = 0
        eventLeft = 0; eventName = ""; aiTick = 0; playerSlowLeft = 0; phase = Phase.READY
    }
    fun finish() {
        if (phase == Phase.DEAD) return; phase = Phase.DEAD; progress = 1f; if (rewarded) return; rewarded = true
        if (AntiCheat.validateScore(score, System.currentTimeMillis() - startMs, score / ProgressStore.POINTS_PER_APPLE)) {
            store.grantMatchRewards(score, GameMode.FEEDING)
            store.pushScore(store.nickname, score, GameMode.FEEDING.name)
        }
        store.recordGameEnd(false, score / 10, snake.size); store.saveBackup()
    }
    fun useUlt() {
        if (ability == null || ultCd > 0 || phase != Phase.RUN) return
        ultCd = ability.cooldownSec
        when (ability.id) {
            "boost", "dash" -> boostLeft = if (ability.id == "dash") 2 else 3
            "joke", "phase" -> invulnLeft = if (ability.id == "phase") 3 else 5
            "lightning" -> { boostLeft = 5; eventName = "Молния"; eventLeft = 2; enemies = enemies.map { e -> e.copy(score = (e.score - 10).coerceAtLeast(0)) } }
            "freeze" -> { eventName = "Холод"; eventLeft = 2 }
            else -> boostLeft = 3
        }
    }
    LaunchedEffect(phase) {
        while (phase == Phase.RUN) {
            if (ultCd > 0) ultCd--; if (invulnLeft > 0) invulnLeft--; if (boostLeft > 0) boostLeft--
            if (playerSlowLeft > 0) playerSlowLeft--
            if (eventLeft > 0) { eventLeft--; if (eventLeft == 0) eventName = "" }
            else if (Random.nextFloat() < 0.04f) {
                eventName = "Яблоки!"; eventLeft = 5
                foods = (foods + List(8) { freeCell(snake.toSet() + enemies.flatMap { it.body }) }).distinct().take(20)
            }
            dir = next
            val head = snake.first(); var pnx = head.x; var pny = head.y
            when (dir) { Dir.UP -> pny--; Dir.DOWN -> pny++; Dir.LEFT -> pnx--; Dir.RIGHT -> pnx++ }
            pnx = wrap(pnx, cols); pny = wrap(pny, rows)
            val pNext = Cell(pnx, pny); val eatIdx = foods.indexOf(pNext); val grow = eatIdx >= 0
            val pBody = if (grow) snake else snake.dropLast(1)
            if ((pNext in pBody || enemies.any { pNext in it.body }) && invulnLeft <= 0) { progress = 1f; finish(); break }
            progress = 0f
            snake = listOf(pNext) + pBody
            if (grow) {
                score += ProgressStore.POINTS_PER_APPLE; foods = foods.toMutableList().also { it.removeAt(eatIdx) }
                if (foods.isEmpty() || eventLeft > 0) foods = foods + freeCell(snake.toSet() + enemies.flatMap { it.body })
            }
            aiTick++
            enemies = enemies.map { e ->
                var e2 = e
                if (e2.ultCd > 0) e2 = e2.copy(ultCd = e2.ultCd - 1)
                else if (Random.nextFloat() < 0.08f) {
                    when (e2.ultId) {
                        "boost", "dash" -> e2 = e2.copy(ultCd = 12)
                        "freeze" -> { e2 = e2.copy(ultCd = 16); playerSlowLeft = 2; eventName = "${e2.name}: Холод"; eventLeft = 2 }
                        "lightning" -> { e2 = e2.copy(ultCd = 18); if (score > 0) score = (score - 5).coerceAtLeast(0); eventName = "${e2.name}: Молния"; eventLeft = 2 }
                        else -> e2 = e2.copy(ultCd = 12)
                    }
                }
                e2
            }
            val frozen = eventName.contains("Холод") && eventLeft > 0
            if (aiTick % 2 == 0 && !frozen) {
                enemies = enemies.map { e ->
                    val hx = e.body.first().x; val hy = e.body.first().y
                    val target = foods.minByOrNull { abs(it.x - hx) + abs(it.y - hy) }
                    var ed = e.dir
                    if (target != null) {
                        var dx = target.x - hx; var dy = target.y - hy
                        if (dx > cols / 2) dx -= cols; if (dx < -cols / 2) dx += cols
                        if (dy > rows / 2) dy -= rows; if (dy < -rows / 2) dy += rows
                        ed = when {
                            abs(dx) > abs(dy) -> if (dx > 0) Dir.RIGHT else Dir.LEFT
                            abs(dy) > 0 -> if (dy > 0) Dir.DOWN else Dir.UP
                            else -> e.dir
                        }
                        if (Random.nextFloat() < 0.18f) ed = Dir.entries.random()
                    }
                    var nx = hx; var ny = hy
                    when (ed) { Dir.UP -> ny--; Dir.DOWN -> ny++; Dir.LEFT -> nx--; Dir.RIGHT -> nx++ }
                    val nh = Cell(wrap(nx, cols), wrap(ny, rows))
                    val eEat = foods.indexOf(nh); val eGrow = eEat >= 0
                    var newBody = listOf(nh) + if (eGrow) e.body else e.body.dropLast(1)
                    if (nh in newBody.drop(1)) newBody = listOf(nh, Cell(hx, hy))
                    var sc = e.score
                    if (eGrow) {
                        sc += ProgressStore.POINTS_PER_APPLE; foods = foods.toMutableList().also { if (eEat in it.indices) it.removeAt(eEat) }
                        if (foods.size < 3) foods = foods + freeCell(snake.toSet() + enemies.flatMap { it.body })
                    }
                    e.copy(body = newBody, dir = ed, score = sc)
                }
            }
            val base = if (boostLeft > 0) 55L else if (playerSlowLeft > 0) 160L else GameMode.FEEDING.speedMs.coerceAtLeast(70L)
            smoothStep(base, store.targetFps) { progress = it }
            if (phase != Phase.RUN) break
            progress = 1f
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onExit) { Text("‹ МЕНЮ", color = C.text) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Поедание · ${ch.name} · $score", color = C.mint, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (eventLeft > 0) Text("ИВЕНТ: $eventName (${eventLeft})", color = C.gold, fontSize = 12.sp)
            }
            Text("рек ${store.bestScore(GameMode.FEEDING.name)}", color = C.muted, fontSize = 11.sp)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Text("Ты $score", color = C.mint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            enemies.forEach { e -> Text("${e.name} ${e.score}", color = e.color, fontSize = 11.sp) }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val w = minOf(maxWidth, maxHeight * cols / rows)
            Box(Modifier.width(w).aspectRatio(cols / rows.toFloat()).clip(RoundedCornerShape(16.dp)).background(Color(0xFF0B1420))
                .border(1.dp, C.cyan.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                .then(if (control == "gestures") Modifier.tapDir(phase == Phase.RUN && !paused, { turn(it) }, dir) else Modifier.swipeDir(phase == Phase.RUN && !paused) { turn(it) })) {
                val mainFood = foods.firstOrNull() ?: Cell(0, 0)
                SmoothSnakeBoard(cols, rows, snake.map { RenderCell(it.x, it.y) }, RenderCell(mainFood.x, mainFood.y),
                    when (dir) { Dir.UP -> RenderDir.UP; Dir.DOWN -> RenderDir.DOWN; Dir.LEFT -> RenderDir.LEFT; Dir.RIGHT -> RenderDir.RIGHT },
                    Color(ch.headColor), Color(ch.bodyColor), foodColor = Color(ShopData.apple(store.selectedApple).color),
                    progress = if (phase == Phase.DEAD) 1f else progress,
                    extraSnakes = enemies.map { e -> e.body.map { RenderCell(it.x, it.y) } to e.color }, showGrid = store.showGrid)
                if (foods.size > 1) Text("x${foods.size} яблок", color = C.gold, fontSize = 12.sp, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
                if (phase != Phase.RUN) OverlayReadyDead(phase, score, { if (phase == Phase.DEAD) reset(); phase = Phase.RUN })
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            ControlPad { turn(it) }
            Box(Modifier.size(88.dp).clip(RoundedCornerShape(22.dp))
                .background(if (ultCd == 0 && phase == Phase.RUN) Brush.horizontalGradient(listOf(C.gold, C.mint)) else Brush.horizontalGradient(listOf(Color(0xFF333333), Color(0xFF222222))))
                .border(2.dp, C.gold.copy(alpha = 0.5f), RoundedCornerShape(22.dp)).clickable { useUlt() },
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("УЛЬТА", color = Color(0xFF062016), fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text(if (ultCd > 0) "${ultCd}с" else (ability?.name ?: "—"), color = Color(0xFF062016), fontSize = 11.sp)
                }
            }
        }
        if (invulnLeft > 0) Text("Неуязвимость ${invulnLeft}с", color = C.cyan, fontSize = 12.sp)
        if (boostLeft > 0) Text("Ускорение ${boostLeft}с", color = C.mint, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
    }
}


@Composable
internal fun KrustyPlay(store: ProgressStore, onExit: () -> Unit) {
    val order = listOf("bun", "patty", "cheese")
    val names = mapOf("bun" to "Булочка", "patty" to "Котлета", "cheese" to "Сыр")
    val colors = mapOf("bun" to 0xFFFFE082, "patty" to 0xFF8D6E63, "cheese" to 0xFFFFD54F)
    var step by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(Phase.READY) }
    var score by remember { mutableIntStateOf(0) }
    var dir by remember { mutableStateOf(Dir.RIGHT) }
    var next by remember { mutableStateOf(Dir.RIGHT) }
    var snake by remember { mutableStateOf(listOf(Cell(4, 10), Cell(3, 10), Cell(2, 10))) }
    var food by remember { mutableStateOf(Cell(10, 8)) }
    var foodKind by remember { mutableStateOf("bun") }
    var progress by remember { mutableFloatStateOf(1f) }
    var msg by remember { mutableStateOf("Сначала булочка") }
    var rewarded by remember { mutableStateOf(false) }
    val cols = COLS; val rows = ROWS
    fun spawn(body: List<Cell>): Cell {
        var c: Cell; do { c = Cell(kotlin.random.Random.nextInt(cols), kotlin.random.Random.nextInt(rows)) } while (c in body); return c
    }
    fun turn(d: Dir) {
        val opp = when (dir) { Dir.UP -> Dir.DOWN; Dir.DOWN -> Dir.UP; Dir.LEFT -> Dir.RIGHT; Dir.RIGHT -> Dir.LEFT }
        if (d != opp) next = d
    }
    fun finish(ok: Boolean) {
        if (phase == Phase.DEAD) return
        phase = Phase.DEAD; progress = 1f
        if (!rewarded) {
            rewarded = true
            store.grantMatchRewards(score, GameMode.KRUSTY)
            store.pushScore(store.nickname, score, GameMode.KRUSTY.name)
            store.saveBackup()
        }
        msg = if (ok) "Собрал бургер" else "Не тот слой — проигрыш"
    }
    LaunchedEffect(phase) {
        while (phase == Phase.RUN) {
            dir = next
            val h = snake.first(); var nx = h.x; var ny = h.y
            when (dir) { Dir.UP -> ny--; Dir.DOWN -> ny++; Dir.LEFT -> nx--; Dir.RIGHT -> nx++ }
            val nh = Cell(nx, ny)
            val eat = nh == food
            val body = if (eat) snake else snake.dropLast(1)
            if (nx !in 0 until cols || ny !in 0 until rows || nh in body) { progress = 1f; finish(false); break }
            progress = 0f
            snake = listOf(nh) + body
            smoothStep(GameMode.KRUSTY.speedMs, store.targetFps) { progress = it }
            progress = 1f
            if (eat) {
                val need = order[step % order.size]
                if (foodKind != need) { finish(false); break }
                score += ProgressStore.POINTS_PER_APPLE
                step++
                msg = "Дальше: ${names[order[step % order.size]]}"
                foodKind = listOf("bun", "patty", "cheese").random()
                food = spawn(snake)
            }
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onExit) { Text("‹ МЕНЮ", color = C.text) }
            Text("Красти · $score · $msg", color = C.gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val w = minOf(maxWidth, maxHeight * cols / rows)
            Box(Modifier.width(w).aspectRatio(cols / rows.toFloat()).clip(RoundedCornerShape(16.dp)).background(Color(0xFF0B1420)).swipeDir(phase == Phase.RUN) { turn(it) }) {
                SmoothSnakeBoard(cols, rows, snake.map { RenderCell(it.x, it.y) }, RenderCell(food.x, food.y),
                    RenderDir.RIGHT, Color(0xFFFFCC80), Color(0xFFD84315), foodColor = Color(colors[foodKind] ?: 0xFFFFE082),
                    progress = if (phase == Phase.DEAD) 1f else progress, showGrid = store.showGrid)
                if (phase != Phase.RUN) OverlayReadyDead(phase, score, { if (phase == Phase.DEAD) { step = 0; score = 0; rewarded = false; snake = listOf(Cell(4,10),Cell(3,10),Cell(2,10)); foodKind = "bun"; msg = "Сначала булочка" }; phase = Phase.RUN }, msg)
            }
        }
        ControlPad { turn(it) }
    }
}
