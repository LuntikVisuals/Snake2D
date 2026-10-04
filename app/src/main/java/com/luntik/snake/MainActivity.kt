package com.luntik.snake

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

internal const val COLS = 16
internal const val ROWS = 20
internal const val FEED_COLS = 24
internal const val FEED_ROWS = 28
const val APP_TITLE = "Snake2D 2.1.1.2"

internal enum class Dir { UP, DOWN, LEFT, RIGHT }
internal enum class Phase { READY, RUN, DEAD }
internal enum class Scr { HUB, PLAY, SHOP, CASES, CHARS, FEED, SETTINGS, REGISTER, BATTLEPASS, INVENTORY, PROFILE, LEADER }
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

/** Коллаба Бикини Боттом: цвет фиксирован, перекрасить нельзя */
internal val COLLAB_LOCKED = setOf(
    "patrick", "squidward", "gary", "krab", "spongebob", "doodle_bob", "plankton", "spongebob_char"
)

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
                onSettings = { scr = Scr.SETTINGS }, onBattlePass = { scr = Scr.BATTLEPASS },
                onInventory = { scr = Scr.INVENTORY }, onProfile = { scr = Scr.PROFILE },
                onLeader = { scr = Scr.LEADER }, onRefresh = { refresh() })
            Scr.PLAY -> if (mode == GameMode.KRUSTY) KrustyPlay(store) { refresh(); store.saveBackup(); scr = Scr.HUB } else ClassicPlay(store, mode) { refresh(); store.saveBackup(); scr = Scr.HUB }
            Scr.SHOP -> key(tick) { Shop(store, { refresh() }) { scr = Scr.HUB } }
            Scr.CASES -> key(tick) { Cases(store, { refresh() }) { scr = Scr.HUB } }
            Scr.CHARS -> key(tick) { Chars(store, { refresh() }, onPlay = { scr = Scr.FEED }, onBack = { scr = Scr.HUB }) }
            Scr.FEED -> FeedingPlay(store) { refresh(); store.saveBackup(); scr = Scr.HUB }
            Scr.SETTINGS -> Settings(store, onFps) { refresh(); scr = Scr.HUB }
            Scr.BATTLEPASS -> BattlePassScreen(store, onDonate = { scr = Scr.SHOP }) { refresh(); scr = Scr.HUB }
            Scr.INVENTORY -> InventoryScreen(store, { refresh() }) { scr = Scr.HUB }
            Scr.PROFILE -> ProfileScreen(store, onLogout = { store.registered = false; scr = Scr.REGISTER }) { scr = Scr.HUB }
            Scr.LEADER -> LeaderScreen(store) { scr = Scr.HUB }
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
    var pass by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
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
            Spacer(Modifier.height(8.dp))
            Text("Пароль (обязателен для ника adminka)", color = C.gold, fontSize = 12.sp)
            BasicTextField(value = pass, onValueChange = { pass = it.take(32) },
                textStyle = TextStyle(color = C.text, fontSize = 16.sp),
                cursorBrush = SolidColor(C.gold),
                modifier = Modifier.fillMaxWidth().background(Color(0x33000000), RoundedCornerShape(10.dp)).padding(12.dp))
            if (err.isNotEmpty()) Text(err, color = C.red, fontSize = 12.sp)
        }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(C.mint, C.cyan)))
            .clickable {
                val nick = name.ifBlank { "Игрок" }
                if (nick.equals("adminka", true) && pass != "змейкатоп123321") { err = "Неверный пароль"; return@clickable }
                store.nickname = nick
                if (nick.equals("adminka", true)) {
                    ShopData.skins.forEach { store.unlockSkin(it.id) }
                    CharacterData.all.forEach { store.unlockCharacter(it.id) }
                    ShopData.appleSkins.forEach { store.unlockApple(it.id) }
                    store.customFieldSlot = true
                    store.bpPremium = true
                    store.addCoins(5000, "Админка")
                    store.addGems(200, "Админка")
                }
                store.registered = true; store.saveBackup(); onDone()
            }
            .padding(16.dp), contentAlignment = Alignment.Center) {
            Text("НАЧАТЬ", color = Color(0xFF062016), fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
    }
}

@Composable
private fun Hub(store: ProgressStore, tick: Int, onPlay: (GameMode) -> Unit, onShop: () -> Unit, onCases: () -> Unit, onChars: () -> Unit, onSettings: () -> Unit, onBattlePass: () -> Unit, onInventory: () -> Unit, onProfile: () -> Unit, onLeader: () -> Unit, onRefresh: () -> Unit) {
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
            Text(store.nickname, color = C.text, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onProfile))
            Text("${store.coins} монет · ${store.gems} гемов · ${store.tix} tix", color = C.gold, fontSize = 13.sp)
            Text("BP ${store.bpLevel}/40 · ${store.xp} XP", color = C.muted, fontSize = 12.sp)
            Text("Награды за уровень — листай", color = C.muted, fontSize = 11.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                store.nextLevelRewards().forEach { line ->
                    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF1C2B3B)).padding(10.dp)) {
                        Text(line, color = C.mint, fontSize = 12.sp)
                    }
                }
            }
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Glass(Modifier.weight(1f).clickable(onClick = onBattlePass)) {
                Text("БАТЛПАСС", color = C.gold, fontWeight = FontWeight.Bold)
                Text("ур.${store.bpLevel}/40", color = C.muted, fontSize = 11.sp)
            }
            Glass(Modifier.weight(1f).clickable(onClick = onInventory)) {
                Text("ИНВЕНТАРЬ", color = C.text, fontWeight = FontWeight.Bold)
                Text("скрытые / коллаба", color = C.muted, fontSize = 11.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Glass(Modifier.weight(1f).clickable(onClick = onProfile)) { Text("ПРОФИЛЬ", color = C.text, fontWeight = FontWeight.Bold) }
            Glass(Modifier.weight(1f).clickable(onClick = onLeader)) { Text("ТОПЫ", color = C.text, fontWeight = FontWeight.Bold) }
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
private fun BattlePassScreen(store: ProgressStore, onDonate: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onBack) { Text("‹ НАЗАД", color = C.text) }
        Text("БАТЛПАСС", color = C.mint, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("${BattlePassData.SEASON_NAME} · Ур.${store.bpLevel}/40", color = C.gold, fontWeight = FontWeight.Bold)
        Text("${store.bpTix} tix · " + if (store.bpPremium) "PREMIUM" else "FREE", color = C.muted, fontSize = 13.sp)
        if (!store.bpPremium) {
            Glass(Modifier.fillMaxWidth()) {
                Text("Premium только за донат", color = C.gold, fontWeight = FontWeight.Bold)
                Text("Гемами и монетами пасс не покупается.", color = C.muted, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Brush.horizontalGradient(listOf(C.gold, C.mint)))
                    .clickable(onClick = onDonate).padding(12.dp), contentAlignment = Alignment.Center) {
                    Text("ОТКРЫТЬ ДОНАТ", color = Color(0xFF062016), fontWeight = FontWeight.Black)
                }
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
private fun InventoryScreen(store: ProgressStore, onChanged: () -> Unit, onBack: () -> Unit) {
    val unlockedSkins = store.unlockedSkins()
    val unlockedChars = store.unlockedCharacters()
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onBack) { Text("‹ НАЗАД", color = C.text) }
        Text("ИНВЕНТАРЬ", color = C.mint, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("Здесь видны коллаба и скрытые вещи, которых нет в магазине.", color = C.muted, fontSize = 12.sp)
        Text("ПЕРСОНАЖИ", color = C.gold, fontWeight = FontWeight.Bold)
        CharacterData.all.filter { it.seasonOnly || it.id in unlockedChars }.forEach { ch ->
            val have = ch.id in unlockedChars
            Glass(Modifier.fillMaxWidth()) {
                Text(ch.name + if (!have) " · нет" else "", color = if (have) C.text else C.muted, fontWeight = FontWeight.Bold)
                Text(if (ch.seasonOnly) "Только батлпасс / сезон" else ch.rarity.title, color = C.gold, fontSize = 12.sp)
                Text(ch.description, color = C.muted, fontSize = 12.sp)
                if (have) {
                    Text(if (store.selectedCharacter == ch.id) "ВЫБРАН" else "НАДЕТЬ", color = C.cyan, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { store.selectedCharacter = ch.id; store.saveBackup(); onChanged() })
                }
            }
        }
        Text("ЕДА", color = C.gold, fontWeight = FontWeight.Bold)
        ShopData.appleSkins.forEach { a ->
            val have = a.id == "apple" || a.id in store.unlockedApples() || (a.id != "krab_burger" && a.price == 0)
            val seasonal = a.id == "krab_burger"
            Glass(Modifier.fillMaxWidth()) {
                Text(a.name + if (seasonal) " · эксклюзив сезона" else "", color = if (have || !seasonal) C.text else C.muted, fontWeight = FontWeight.Bold)
                if (seasonal && !have) Text("Только сезон / инвентарь, в магазине нет", color = C.red, fontSize = 12.sp)
                if (have || !seasonal) Text(if (store.selectedApple == a.id) "НАДЕТО" else "НАДЕТЬ", color = C.cyan, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { store.selectedApple = a.id; store.saveBackup(); onChanged() })
            }
        }
        Text("ОБЛИКИ КОЛЛАБЫ", color = C.gold, fontWeight = FontWeight.Bold)
        ShopData.skins.filter { it.id in COLLAB_LOCKED || it.rarity == Rarity.EXCLUSIVE }.forEach { skin ->
            val have = skin.id in unlockedSkins
            Glass(Modifier.fillMaxWidth()) {
                Text(skin.name + if (!have) " · нет" else "", color = if (have) C.text else C.muted, fontWeight = FontWeight.Bold)
                Text("Цвет зафиксирован · перекрасить нельзя", color = C.red, fontSize = 12.sp)
                if (have) {
                    Text(if (store.selectedSkin == skin.id) "НАДЕТ" else "НАДЕТЬ", color = C.cyan, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { store.selectedSkin = skin.id; store.saveBackup(); onChanged() })
                }
            }
        }
    }
}

@Composable
private fun Settings(store: ProgressStore, onFps: (Int) -> Unit, onBack: () -> Unit) {
    var nick by remember { mutableStateOf(store.nickname) }
    var adminPass by remember { mutableStateOf("") }
    var adminMsg by remember { mutableStateOf("") }
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
            Text("Админ-вход", color = C.gold, fontWeight = FontWeight.Bold)
            Text("Пароль откроет всё и поставит ник adminka", color = C.muted, fontSize = 12.sp)
            BasicTextField(value = adminPass, onValueChange = { adminPass = it.take(32) },
                textStyle = TextStyle(color = C.text, fontSize = 16.sp),
                cursorBrush = SolidColor(C.gold),
                modifier = Modifier.fillMaxWidth().background(Color(0x33000000), RoundedCornerShape(10.dp)).padding(10.dp))
            if (adminMsg.isNotEmpty()) Text(adminMsg, color = C.mint, fontSize = 12.sp)
            Text("РАЗБЛОКИРОВАТЬ", color = Color(0xFF062016), fontWeight = FontWeight.Black,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(C.gold).clickable {
                    if (adminPass.trim() != "змейкатоп123321") { adminMsg = "Неверный пароль"; return@clickable }
                    store.nickname = "adminka"
                    ShopData.skins.forEach { store.unlockSkin(it.id) }
                    CharacterData.all.forEach { store.unlockCharacter(it.id) }
                    ShopData.appleSkins.forEach { store.unlockApple(it.id) }
                    store.customFieldSlot = true
                    store.bpPremium = true
                    store.addCoins(50000, "Админка")
                    store.addGems(500, "Админка")
                    store.selectedSkin = "default"
                    store.registered = true
                    store.saveBackup()
                    adminMsg = "Готово: ${store.unlockedSkins().size} скинов, ${store.coins} монет. Смотри инвентарь."
                }.padding(10.dp))
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
    var tab by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf(store.selectedSkin) }
    var coins by remember { mutableIntStateOf(store.coins) }
    var unlocked by remember { mutableStateOf(store.unlockedSkins()) }
    var previewId by remember { mutableStateOf(selected) }
    var donateMsg by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = onBack) { Text("‹ НАЗАД", color = C.text) }
        Text("МАГАЗИН", color = C.mint, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("$coins монет · ${store.gems} гемов", color = C.gold, fontSize = 13.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Скины", "Облики", "Яблоки", "Персонализация", "Донат").forEachIndexed { i, name ->
                val sel = tab == i
                Box(Modifier.clip(RoundedCornerShape(10.dp)).background(if (sel) C.mint.copy(alpha = 0.3f) else Color(0xFF1C2B3B))
                    .clickable { tab = i }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(name, color = if (sel) C.mint else C.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
        when (tab) {
            0 -> {
                val prev = ShopData.skin(previewId)
                val lockedColor = prev.id in COLLAB_LOCKED || prev.rarity == Rarity.EXCLUSIVE
                Glass(Modifier.fillMaxWidth()) {
                    Text("Превью: ${prev.name}", color = C.text, fontWeight = FontWeight.Bold)
                    Text(prev.rarity.title, color = Color(prev.rarity.color), fontSize = 12.sp)
                    if (lockedColor) Text("Цвет зафиксирован · коллаба", color = C.red, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    SkinPreview(Color(prev.headColor), Color(prev.bodyColor), Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF0B1420)))
                }
                ShopData.skins.filter { it.rarity != Rarity.EXCLUSIVE }.forEach { skin ->
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
            1 -> AppearanceTab(store) { coins = store.coins; onChanged() }
            2 -> {
                ShopData.appleSkins.filter { it.id != "krab_burger" }.forEach { a ->
                    Glass(Modifier.fillMaxWidth()) {
                        Text(a.name, color = C.text, fontWeight = FontWeight.Bold)
                        val owned = a.id == "apple" || a.id in store.unlockedApples()
                        Text(if (store.selectedApple == a.id) "НАДЕТО" else if (owned) "НАДЕТЬ" else "${a.price} монет", color = C.gold, fontSize = 12.sp,
                            modifier = Modifier.clickable {
                                if (owned) { store.selectedApple = a.id; store.saveBackup(); onChanged() }
                                else if (store.spendCoins(a.price, "Яблоко ${a.name}")) {
                                    store.unlockApple(a.id); store.selectedApple = a.id; store.saveBackup(); onChanged()
                                }
                            })
                    }
                }
                Text("Крабсбургер — только сезон, смотри инвентарь.", color = C.muted, fontSize = 12.sp)
            }
            3 -> PersonalizationTab(store) { coins = store.coins; onChanged() }
            else -> {
                Text("ДОНАТ", color = C.gold, fontWeight = FontWeight.Black)
                Text("Гемы и монеты — донат. Premium пасса гемами не продаётся.", color = C.muted, fontSize = 12.sp)
                Glass(Modifier.fillMaxWidth()) {
                    Text("Premium батлпасс", color = C.text, fontWeight = FontWeight.Bold)
                    Text("Только донат. Не за гемы.", color = C.red, fontSize = 12.sp)
                    if (store.bpPremium) Text("УЖЕ АКТИВЕН", color = C.mint, fontWeight = FontWeight.Bold)
                    else Text("Скоро. Нужен LuntikWallet — гемами не купить.", color = C.muted, fontSize = 12.sp)
                }
                listOf("100 гемов", "500 гемов", "1000 монет").forEach { title ->
                    Glass(Modifier.fillMaxWidth()) {
                        Text(title, color = C.text, fontWeight = FontWeight.Bold)
                        Text("Скоро через LuntikWallet", color = C.muted, fontSize = 12.sp)
                    }
                }
                if (donateMsg.isNotEmpty()) Text(donateMsg, color = C.muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun Cases(store: ProgressStore, onChanged: () -> Unit, onBack: () -> Unit) {
    var msg by remember { mutableStateOf("") }
    var spinning by remember { mutableStateOf(false) }
    var openingId by remember { mutableStateOf("") }
    var reel by remember { mutableStateOf("") }
    var coins by remember { mutableIntStateOf(store.coins) }
    LaunchedEffect(spinning) {
        if (!spinning) return@LaunchedEffect
        val names = ShopData.skins.map { it.name }
        repeat(14) {
            reel = names.random()
            delay(if (it < 8) 70 else 140)
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = onBack) { Text("‹ НАЗАД", color = C.text) }
        Text("КЕЙСЫ", color = C.mint, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("$coins монет · ${ShopData.cases.size} типов", color = C.gold)
        ShopData.cases.forEach { c ->
            val total = c.weights.values.sum().coerceAtLeast(1)
            Glass(Modifier.fillMaxWidth()) {
                Text(c.name, color = C.text, fontWeight = FontWeight.Bold)
                Text("${c.price} монет", color = C.gold, fontSize = 13.sp)
                c.weights.entries.sortedByDescending { it.value }.forEach { (r, w) ->
                    Text("  ${r.title}: ${w * 100 / total}%", color = Color(r.color), fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Brush.horizontalGradient(listOf(C.mint, C.cyan))).clickable {
                    if (spinning) return@clickable
                    if (!store.spendCoins(c.price, "Кейс ${c.name}")) { msg = "Мало монет"; return@clickable }
                    openingId = c.id
                    spinning = true
                    coins = store.coins
                }.padding(12.dp), contentAlignment = Alignment.Center) {
                    Text(if (spinning && openingId == c.id) "КРУТИМ" else "ОТКРЫТЬ", color = Color(0xFF062016), fontWeight = FontWeight.Black)
                }
                if (spinning && openingId == c.id) {
                    LaunchedEffect(c.id) {
                        delay(1600)
                        val drop = ShopData.openCase(c)
                        val dup = store.isUnlocked(drop.id)
                        if (dup) store.addCoins(20, "Дубликат ${drop.name}") else store.unlockSkin(drop.id)
                        store.recordCaseOpened(); store.logCase(c.name, drop.name, dup); store.saveBackup()
                        coins = store.coins
                        msg = if (dup) "ВЫПАЛО: дубликат ${drop.name} (+20)" else "ВЫПАЛО: ${drop.name} [${drop.rarity.title}]"
                        reel = drop.name
                        spinning = false
                        onChanged()
                    }
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
        Text("Сезонные (Спанч Боб) в магазине не продаются — только инвентарь, если уже есть.", color = C.muted, fontSize = 12.sp)
        CharacterData.visibleForSelect(store.unlockedCharacters()).filter { !it.seasonOnly }.forEach { ch ->
            val unlocked = store.isCharacterUnlocked(ch.id)
            val isSel = selected == ch.id
            Glass(Modifier.fillMaxWidth()) {
                Text(ch.name, color = C.text, fontWeight = FontWeight.Bold)
                Text(ch.rarity.title, color = Color(ch.rarity.color), fontSize = 12.sp)
                Text(ch.description, color = C.muted, fontSize = 12.sp)
                ch.ability?.let { Text("Ульта: ${it.name} (КД ${it.cooldownSec}с)", color = C.cyan, fontSize = 12.sp) }
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


@Composable
private fun ProfileScreen(store: ProgressStore, onLogout: () -> Unit, onBack: () -> Unit) {
    val st = store.getStats()
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = onBack) { Text("‹ НАЗАД", color = C.text) }
        Text("ПРОФИЛЬ", color = C.mint, fontSize = 24.sp, fontWeight = FontWeight.Black)
        val pickAvatar = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) store.avatarUri = uri.toString()
        }
        val pickBanner = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) store.bannerUri = uri.toString()
        }
        Glass(Modifier.fillMaxWidth()) {
            if (store.bannerUri.isNotBlank()) {
                AndroidView(factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
                    update = { it.setImageURI(Uri.parse(store.bannerUri)) },
                    modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(12.dp)))
                Spacer(Modifier.height(8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (store.avatarUri.isNotBlank()) {
                    AndroidView(factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
                        update = { it.setImageURI(Uri.parse(store.avatarUri)) },
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(28.dp)))
                    Spacer(Modifier.width(10.dp))
                }
                Text(store.nickname, color = C.text, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("АВАТАР", color = C.cyan, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { pickAvatar.launch("image/*") })
                Text("БАННЕР", color = C.gold, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { pickBanner.launch("image/*") })
            }
            Text("Уровень ${store.level} · ${store.xp} XP", color = C.cyan)
            Text("Батлпасс ур.${store.bpLevel} · ${BattlePassData.SEASON_NAME}", color = C.gold)
            Text("Сезон: ${BattlePassData.SEASON_ID}", color = C.muted, fontSize = 12.sp)
            Text("${store.coins} монет · ${store.gems} гемов · ${store.tix} tix", color = C.muted, fontSize = 13.sp)
        }
        Glass(Modifier.fillMaxWidth()) {
            Text("Статы", color = C.text, fontWeight = FontWeight.Bold)
            Text("Каток: ${st.gamesPlayed} · еды: ${st.totalFoodEaten} · макс длина ${st.maxLength}", color = C.muted, fontSize = 12.sp)
            Text("Кейсов: ${st.casesOpened}", color = C.muted, fontSize = 12.sp)
            Text("Скин: ${ShopData.skin(store.selectedSkin).name}", color = C.muted, fontSize = 12.sp)
            Text("Еда: ${ShopData.apple(store.selectedApple).name}", color = C.muted, fontSize = 12.sp)
            Text("ВЫЙТИ ИЗ АККАУНТА", color = C.red, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onLogout).padding(top = 8.dp))
        }
    }
}

@Composable
private fun LeaderScreen(store: ProgressStore, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onBack) { Text("‹ НАЗАД", color = C.text) }
        Text("ТОПЫ", color = C.mint, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("По режимам, локально на этом устройстве", color = C.muted, fontSize = 12.sp)
        GameMode.entries.forEach { m ->
            val board = store.leaderboard(m.name)
            Glass(Modifier.fillMaxWidth()) {
                Text(m.title, color = C.text, fontWeight = FontWeight.Bold)
                Text("Твой рекорд: ${store.bestScore(m.name)}", color = C.gold, fontSize = 12.sp)
                if (board.isEmpty()) Text("Пока пусто", color = C.muted, fontSize = 12.sp)
                board.take(8).forEachIndexed { i, e ->
                    Text("${i + 1}. ${e.name} — ${e.score}", color = C.muted, fontSize = 12.sp)
                }
            }
        }
    }
}


@Composable
private fun PersonalizationTab(store: ProgressStore, onChanged: () -> Unit) {
    val grids = listOf(
        "Мята" to 0x2867F5B4, "Циан" to 0x2865DDFB, "Золото" to 0x28FFD36E, "Красный" to 0x28FF6684, "Белый" to 0x22FFFFFF
    )
    val fields = listOf("Ночь" to 0xFF0B1420, "Бездна" to 0xFF061018, "Песок" to 0xFF1A140C, "Лагуна" to 0xFF0C1C22)
    val screens = listOf("Стандарт" to 0xFF070B12, "Изумруд" to 0xFF07140F, "Фиолет" to 0xFF120818, "Закат" to 0xFF1A0C10)
    val ctx = LocalContext.current
    val pickField = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null || !store.customFieldSlot) return@rememberLauncherForActivityResult
        val dir = ctx.getExternalFilesDir(null)?.resolve("snake2d") ?: return@rememberLauncherForActivityResult
        dir.mkdirs()
        val out = dir.resolve("field.jpg")
        ctx.contentResolver.openInputStream(uri)?.use { input -> out.outputStream().use { input.copyTo(it) } }
        store.fieldPhotoUri = out.absolutePath
        store.saveBackup()
    }
    Text("Сетка — 5 цветов", color = C.text, fontWeight = FontWeight.Bold)
    grids.forEach { (name, col) ->
        Glass(Modifier.fillMaxWidth().clickable { store.gridColor = col.toLong(); store.saveBackup(); onChanged() }) {
            Text("$name · 150", color = Color(col), fontWeight = FontWeight.Bold)
        }
    }
    Text("Фон поля", color = C.text, fontWeight = FontWeight.Bold)
    fields.forEach { (name, col) ->
        Glass(Modifier.fillMaxWidth().clickable { store.fieldBg = col.toLong(); store.saveBackup(); onChanged() }) {
            Text("$name · 300", color = Color(col), fontWeight = FontWeight.Bold)
        }
    }
    Text("Фон экрана", color = C.text, fontWeight = FontWeight.Bold)
    screens.forEach { (name, col) ->
        Glass(Modifier.fillMaxWidth().clickable { store.screenBg = col.toLong(); store.saveBackup(); onChanged() }) {
            Text("$name · 300", color = Color(col), fontWeight = FontWeight.Bold)
        }
    }
    Glass(Modifier.fillMaxWidth()) {
        Text("Своё фото поля", color = C.text, fontWeight = FontWeight.Bold)
        Text(if (store.customFieldSlot) "Слот куплен — поставь фото" else "Купить слот · 2000 монет", color = C.gold, fontSize = 12.sp,
            modifier = Modifier.clickable {
                if (!store.customFieldSlot) {
                    if (store.spendCoins(2000, "Слот фото поля")) store.customFieldSlot = true
                } else pickField.launch("image/*")
                onChanged()
            })
    }
}


@Composable
private fun AppearanceTab(store: ProgressStore, onChanged: () -> Unit) {
    val looks = listOf(
        "classic" to "Классика · гладкая",
        "retro" to "Ретро · кубики",
        "retro2" to "Ретро 2 · кружки",
        "gliist" to "Глист · чешуя",
        "spongebob" to "Спанч Боб · губка",
        "patrick" to "Патрик · звезда",
        "squidward" to "Сквидвард · нос",
        "gary" to "Гэри · раковина",
        "krabs" to "Мистер Крабс · клешни",
        "drawn" to "Нарисованный Боб · карандаш",
        "plankton" to "Планктон · глаз"
    )
    looks.forEach { (id, title) ->
        val free = id in setOf("classic", "retro", "retro2", "gliist")
        val owned = free || store.nickname.equals("adminka", true) || id in store.unlockedSkins()
        Glass(Modifier.fillMaxWidth()) {
            Text(title, color = C.text, fontWeight = FontWeight.Bold)
            Text(if (store.selectedAppearance == id) "НАДЕТО" else if (owned) "НАДЕТЬ" else "сезон / админ", color = C.gold, fontSize = 12.sp,
                modifier = Modifier.clickable { if (owned) { store.selectedAppearance = id; store.saveBackup(); onChanged() } })
        }
    }
}
