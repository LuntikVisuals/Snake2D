package com.luntik.snake

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal const val COLS = 16
internal const val ROWS = 20
internal const val FEED_COLS = 24
internal const val FEED_ROWS = 28
const val APP_TITLE = "Snake2D 2.1.1.2"

internal enum class Dir { UP, DOWN, LEFT, RIGHT }
internal enum class Phase { READY, RUN, DEAD }
internal enum class Scr { HUB, PLAY, SHOP, CASES, CHARS, FEED, SETTINGS, REGISTER, BATTLEPASS }
internal data class Cell(val x: Int, val y: Int)

internal object C {
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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        GameFiles.ensureLayout(this)
        val store = ProgressStore(this)
        applyFrameRate(store.targetFps)
        AntiCheat.scan(this, store).also { GameFiles.writeSessionLog(this, "boot ${it.detail}") }
        setContent { App(store, onFps = { applyFrameRate(it) }) }
    }

    private fun applyFrameRate(fps: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val rates = display?.supportedModes?.map { it.refreshRate } ?: emptyList()
                val best = rates.minByOrNull { kotlin.math.abs(it - fps.toFloat()) }
                val mode = display?.supportedModes?.firstOrNull { it.refreshRate == best }
                if (mode != null) window.attributes = window.attributes.apply { preferredDisplayModeId = mode.modeId }
            } catch (_: Exception) { }
        }
    }
}

@Composable
private fun App(store: ProgressStore, onFps: (Int) -> Unit) {
    var scr by remember { mutableStateOf(if (!store.registered) Scr.REGISTER else Scr.HUB) }
    var mode by remember { mutableStateOf(GameMode.CLASSIC) }
    var tick by remember { mutableIntStateOf(0) }
    fun refresh() { tick++ }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0A1320), C.bg, Color(0xFF090D16))))) {
        when (scr) {
            Scr.REGISTER -> Register(store) { store.registered = true; store.saveBackup(); scr = Scr.HUB }
            Scr.HUB -> Hub(store, tick,
                onPlay = { mode = it; if (it == GameMode.FEEDING) scr = Scr.CHARS else scr = Scr.PLAY },
                onShop = { scr = Scr.SHOP }, onCases = { scr = Scr.CASES }, onChars = { scr = Scr.CHARS },
                onSettings = { scr = Scr.SETTINGS }, onBattlePass = { scr = Scr.BATTLEPASS }, onRefresh = { refresh() })
            Scr.PLAY -> ClassicPlay(store, mode) { refresh(); store.saveBackup(); scr = Scr.HUB }
            Scr.SHOP -> key(tick) { Shop(store, { refresh() }) { scr = Scr.HUB } }
            Scr.CASES -> key(tick) { Cases(store, { refresh() }) { scr = Scr.HUB } }
            Scr.CHARS -> key(tick) { Chars(store, { refresh() }, onPlay = { scr = Scr.FEED }, onBack = { scr = Scr.HUB }) }
            Scr.FEED -> FeedingPlay(store) { refresh(); store.saveBackup(); scr = Scr.HUB }
            Scr.SETTINGS -> Settings(store, onFps) { refresh(); scr = Scr.HUB }
            Scr.BATTLEPASS -> BattlePassScreen(store) { refresh(); scr = Scr.HUB }
        }
    }
}

@Composable
internal fun Glass(mod: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(mod.clip(RoundedCornerShape(18.dp))
        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.04f))))
        .border(1.dp, C.line, RoundedCornerShape(18.dp)).padding(14.dp), content = content)
}

@Composable
private fun Register(store: ProgressStore, onDone: () -> Unit) {
    var name by remember { mutableStateOf(store.nickname) }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(APP_TITLE, color = C.mint, fontSize = 26.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text("Регистрация игрока", color = C.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Glass(Modifier.fillMaxWidth()) {
            Text("Никнейм", color = C.muted, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            BasicTextField(value = name, onValueChange = { name = it.take(16) },
                textStyle = TextStyle(color = C.text, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                cursorBrush = SolidColor(C.mint),
                modifier = Modifier.fillMaxWidth().background(Color(0x33000000), RoundedCornerShape(10.dp)).padding(12.dp))
        }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(C.mint, C.cyan)))
            .clickable { store.nickname = name.ifBlank { "Игрок" }; store.registered = true; store.saveBackup(); onDone() }
            .padding(16.dp), contentAlignment = Alignment.Center) {
            Text("НАЧАТЬ", color = Color(0xFF062016), fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
    }
}

@Composable
private fun Hub(store: ProgressStore, tick: Int, onPlay: (GameMode) -> Unit, onShop: () -> Unit, onCases: () -> Unit, onChars: () -> Unit, onSettings: () -> Unit, onBattlePass: () -> Unit, onRefresh: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val t = tick
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(APP_TITLE, color = C.mint, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("${store.targetFps} Hz · ур.${store.level}", color = C.muted, fontSize = 11.sp)
            }
            Text("⚙", color = C.text, fontSize = 26.sp, modifier = Modifier.clickable(onClick = onSettings))
        }
        Glass(Modifier.fillMaxWidth()) {
            Text(store.nickname, color = C.text, fontWeight = FontWeight.Bold)
            Text("${store.coins} монет · ${store.gems} гемов · ${store.tix} tix", color = C.gold, fontSize = 13.sp)
            Text("BP ${store.bpLevel}/40 · ${store.xp} XP", color = C.muted, fontSize = 12.sp)
        }
        if (store.canClaimDaily()) {
            Glass(Modifier.fillMaxWidth().clickable { store.claimDaily(); store.saveBackup(); onRefresh() }) {
                Text("ЕЖЕДНЕВНАЯ НАГРАДА — ЗАБРАТЬ", color = C.gold, fontWeight = FontWeight.Bold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Glass(Modifier.weight(1f).clickable(onClick = onShop)) { Text("МАГАЗИН", color = C.text, fontWeight = FontWeight.Bold) }
            Glass(Modifier.weight(1f).clickable(onClick = onCases)) { Text("КЕЙСЫ", color = C.text, fontWeight = FontWeight.Bold) }
        }
        Glass(Modifier.fillMaxWidth().clickable(onClick = onBattlePass)) {
            Text("БАТЛПАСС · ${BattlePassData.SEASON_NAME}", color = C.gold, fontWeight = FontWeight.Bold)
            Text("Ур.${store.bpLevel}/40 · ${store.bpTix} tix" + if (store.bpPremium) " · PREMIUM" else "", color = C.muted, fontSize = 12.sp)
        }
        Glass(Modifier.fillMaxWidth().clickable(onClick = onChars)) {
            Text("ПЕРСОНАЖИ · ПОЕДАНИЕ", color = C.text, fontWeight = FontWeight.Bold)
            Text("Превью · ульта · враги", color = C.muted, fontSize = 12.sp)
        }
        Text("РЕЖИМЫ", color = C.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        GameMode.entries.forEach { m ->
            val locked = store.level < m.unlockLevel
            Glass(Modifier.fillMaxWidth().clickable { if (!locked) onPlay(m) }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(m.title + if (locked) " 🔒 ур.${m.unlockLevel}" else "", color = if (locked) C.muted else C.text, fontWeight = FontWeight.Bold)
                        Text(m.desc, color = C.muted, fontSize = 12.sp)
                    }
                    Text(if (locked) "🔒" else "▶", color = if (locked) C.muted else C.mint, fontSize = 20.sp)
                }
            }
        }
    }
}
@Composable
private fun BattlePassScreen(store: ProgressStore, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onBack) { Text("‹ НАЗАД", color = C.text) }
        Text("БАТЛПАСС", color = C.mint, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("${BattlePassData.SEASON_NAME} · Ур.${store.bpLevel}/40", color = C.gold, fontWeight = FontWeight.Bold)
        Text("${store.bpTix} tix · " + if (store.bpPremium) "PREMIUM" else "FREE", color = C.muted, fontSize = 13.sp)
        if (!store.bpPremium) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Brush.horizontalGradient(listOf(C.gold, C.mint)))
                .clickable { if (store.spendGems(50, "BP Premium")) store.bpPremium = true }.padding(12.dp), contentAlignment = Alignment.Center) {
                Text("КУПИТЬ PREMIUM · 50 гемов", color = Color(0xFF062016), fontWeight = FontWeight.Black)
            }
        }
        val nextLv = (store.bpLevel + 1).coerceAtMost(BattlePassData.MAX_LEVEL)
        val next = BattlePassData.levels.find { it.level == nextLv }
        if (next != null && store.bpLevel < BattlePassData.MAX_LEVEL) {
            Glass(Modifier.fillMaxWidth()) {
                Text("Следующая награда (ур.$nextLv):", color = C.text, fontWeight = FontWeight.Bold)
                Text("FREE: ${next.free.title}", color = C.muted, fontSize = 12.sp)
                Text("PREMIUM: ${next.premium.title}", color = C.gold, fontSize = 12.sp)
            }
        }
        BattlePassData.levels.forEach { lv ->
            val unlocked = store.bpLevel >= lv.level
            Glass(Modifier.fillMaxWidth()) {
                Text("Ур.${lv.level}", color = if (unlocked) C.mint else C.muted, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("FREE: ${lv.free.title}", color = C.text, fontSize = 12.sp)
                        if (unlocked && !store.claimedBp(lv.level, false))
                            Text("ЗАБРАТЬ", color = C.cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { store.claimBp(lv.level, false) })
                        else if (store.claimedBp(lv.level, false)) Text("✓", color = C.mint, fontSize = 12.sp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("PREMIUM: ${lv.premium.title}", color = C.gold, fontSize = 12.sp)
                        if (unlocked && store.bpPremium && !store.claimedBp(lv.level, true))
                            Text("ЗАБРАТЬ", color = C.cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { store.claimBp(lv.level, true) })
                        else if (store.claimedBp(lv.level, true)) Text("✓", color = C.mint, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Settings(store: ProgressStore, onFps: (Int) -> Unit, onBack: () -> Unit) {
    var nick by remember { mutableStateOf(store.nickname) }
    var grid by remember { mutableStateOf(store.showGrid) }
    var hit by remember { mutableStateOf(store.showHitboxes) }
    var fps by remember { mutableIntStateOf(store.targetFps) }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("‹ НАЗАД", color = C.text) }
        Text("НАСТРОЙКИ", color = C.mint, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Glass(Modifier.fillMaxWidth()) {
            Text("Никнейм", color = C.muted, fontSize = 12.sp)
            BasicTextField(value = nick, onValueChange = { nick = it.take(16) }, textStyle = TextStyle(color = C.text, fontSize = 16.sp), cursorBrush = SolidColor(C.mint), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            Box(Modifier.clip(RoundedCornerShape(10.dp)).background(C.cyan.copy(alpha = 0.25f)).clickable { store.nickname = nick; store.saveBackup() }.padding(10.dp)) {
                Text("Сохранить ник", color = C.cyan, fontWeight = FontWeight.Bold)
            }
        }
        Glass(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Сетка", color = C.text, fontWeight = FontWeight.Bold) }
                Switch(checked = grid, onCheckedChange = { grid = it; store.showGrid = it }, colors = SwitchDefaults.colors(checkedTrackColor = C.mint, checkedThumbColor = Color.White))
            }
        }
        Glass(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Хитбоксы", color = C.text, fontWeight = FontWeight.Bold); Text("Контуры еды/змейки", color = C.muted, fontSize = 12.sp) }
                Switch(checked = hit, onCheckedChange = { hit = it; store.showHitboxes = it }, colors = SwitchDefaults.colors(checkedTrackColor = C.mint, checkedThumbColor = Color.White))
            }
        }
        Glass(Modifier.fillMaxWidth()) {
            Text("Частота кадров", color = C.text, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(60, 120, 144).forEach { v ->
                    val sel = fps == v
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .background(if (sel) Brush.horizontalGradient(listOf(C.mint, C.cyan)) else Brush.horizontalGradient(listOf(Color(0xFF1C2B3B), Color(0xFF1C2B3B))))
                        .clickable { fps = v; store.targetFps = v; onFps(v) }.padding(12.dp), contentAlignment = Alignment.Center) {
                        Text("${v} Hz", color = if (sel) Color(0xFF062016) else C.text, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun Shop(store: ProgressStore, onChanged: () -> Unit, onBack: () -> Unit) {
    var selected by remember { mutableStateOf(store.selectedSkin) }
    var coins by remember { mutableIntStateOf(store.coins) }
    var unlocked by remember { mutableStateOf(store.unlockedSkins()) }
    var previewId by remember { mutableStateOf(selected) }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = onBack) { Text("‹ НАЗАД", color = C.text) }
        Text("МАГАЗИН", color = C.mint, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("$coins монет · ${store.gems} гемов", color = C.gold, fontSize = 13.sp)
        val prev = ShopData.skin(previewId)
        Glass(Modifier.fillMaxWidth()) {
            Text("Превью: ${prev.name}", color = C.text, fontWeight = FontWeight.Bold)
            Text(prev.rarity.title, color = Color(prev.rarity.color), fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            SkinPreview(Color(prev.headColor), Color(prev.bodyColor), Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF0B1420)))
        }
        ShopData.skins.filter { it.rarity != Rarity.EXCLUSIVE || it.id in unlocked }.forEach { skin ->
            val isUnlocked = skin.id in unlocked
            val isSelected = selected == skin.id
            Glass(Modifier.fillMaxWidth().clickable { previewId = skin.id }) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Color(skin.bodyColor)).border(2.dp, Color(skin.headColor), RoundedCornerShape(10.dp)))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(skin.name, color = C.text, fontWeight = FontWeight.Bold)
                        Text(skin.rarity.title, color = Color(skin.rarity.color), fontSize = 12.sp)
                    }
                    when {
                        isSelected -> Text("НАДЕТ", color = C.mint, fontSize = 12.sp)
                        isUnlocked -> Text("НАДЕТЬ", color = C.cyan, fontSize = 12.sp, modifier = Modifier.clickable {
                            store.selectedSkin = skin.id; selected = skin.id; previewId = skin.id; store.saveBackup(); onChanged()
                        })
                        skin.unlockOnlyCase -> Text("КЕЙС", color = C.muted, fontSize = 11.sp)
                        else -> Text("${skin.price}", color = C.gold, fontSize = 13.sp, modifier = Modifier.clickable {
                            if (store.spendCoins(skin.price, "Скин ${skin.name}")) {
                                store.unlockSkin(skin.id); store.selectedSkin = skin.id; store.logPurchase(skin.name, skin.price); store.saveBackup()
                                unlocked = store.unlockedSkins(); selected = skin.id; coins = store.coins; previewId = skin.id; onChanged()
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
    var coins by remember { mutableIntStateOf(store.coins) }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = onBack) { Text("‹ НАЗАД", color = C.text) }
        Text("КЕЙСЫ", color = C.mint, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("$coins монет · ${ShopData.cases.size} типов", color = C.gold)
        ShopData.cases.forEach { c ->
            val total = c.weights.values.sum().coerceAtLeast(1)
            Glass(Modifier.fillMaxWidth()) {
                Text(c.name, color = C.text, fontWeight = FontWeight.Bold)
                Text("${c.price} монет", color = C.gold, fontSize = 13.sp)
                Text("Шансы:", color = C.muted, fontSize = 11.sp)
                c.weights.entries.sortedByDescending { it.value }.forEach { (r, w) ->
                    Text("  ${r.title}: ${w * 100 / total}%", color = Color(r.color), fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Brush.horizontalGradient(listOf(C.mint, C.cyan))).clickable {
                    if (!store.spendCoins(c.price, "Кейс ${c.name}")) { msg = "Мало монет"; return@clickable }
                    val drop = ShopData.openCase(c)
                    val dup = store.isUnlocked(drop.id)
                    if (dup) { store.addCoins(20, "Дубликат ${drop.name}"); msg = "Дубликат ${drop.name} (+20)" }
                    else { store.unlockSkin(drop.id); msg = "Выпало: ${drop.name} [${drop.rarity.title}]" }
                    store.recordCaseOpened(); store.logCase(c.name, drop.name, dup); store.saveBackup(); coins = store.coins; onChanged()
                }.padding(12.dp), contentAlignment = Alignment.Center) {
                    Text("ОТКРЫТЬ", color = Color(0xFF062016), fontWeight = FontWeight.Black)
                }
            }
        }
        if (msg.isNotEmpty()) Text(msg, color = C.muted, fontSize = 13.sp)
    }
}

@Composable
private fun Chars(store: ProgressStore, onChanged: () -> Unit, onPlay: () -> Unit, onBack: () -> Unit) {
    var selected by remember { mutableStateOf(store.selectedCharacter) }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = onBack) { Text("‹ НАЗАД", color = C.text) }
        Text("ПЕРСОНАЖИ", color = C.mint, fontSize = 24.sp, fontWeight = FontWeight.Black)
        // Спанч Боб и сезонные — только если уже разблокированы через баттлпасс
        CharacterData.visibleForSelect(store.unlockedCharacters()).forEach { ch ->
            val unlocked = store.isCharacterUnlocked(ch.id)
            val isSel = selected == ch.id
            Glass(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color(ch.bodyColor)).border(2.dp, Color(ch.headColor), RoundedCornerShape(12.dp)))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(ch.name, color = C.text, fontWeight = FontWeight.Bold)
                        Text(ch.rarity.title, color = Color(ch.rarity.color), fontSize = 12.sp)
                        Text(ch.description, color = C.muted, fontSize = 12.sp)
                        ch.ability?.let { ab ->
                            Text("Ульта: ${ab.name} (КД ${ab.cooldownSec}с)", color = C.cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                when {
                    isSel -> Text("ВЫБРАН", color = C.mint, fontWeight = FontWeight.Bold)
                    unlocked -> Text("ВЫБРАТЬ", color = C.cyan, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                        store.selectedCharacter = ch.id; selected = ch.id; store.saveBackup(); onChanged()
                    })
                    else -> Text("КУПИТЬ ${ch.unlockPrice}", color = C.gold, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                        if (store.spendCoins(ch.unlockPrice, "Персонаж ${ch.name}")) {
                            store.unlockCharacter(ch.id); store.selectedCharacter = ch.id; selected = ch.id; store.saveBackup(); onChanged()
                        }
                    })
                }
            }
        }
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Brush.horizontalGradient(listOf(C.mint, C.cyan))).clickable(onClick = onPlay).padding(14.dp), contentAlignment = Alignment.Center) {
            Text("ИГРАТЬ В ПОЕДАНИЕ", color = Color(0xFF062016), fontWeight = FontWeight.Black)
        }
    }
}
