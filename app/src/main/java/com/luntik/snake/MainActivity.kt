package com.luntik.snake

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal const val COLS = 16
internal const val ROWS = 20
internal const val FEED_COLS = 24
internal const val FEED_ROWS = 28
const val APP_TITLE = "Snake2D 2.0.0.1 Beta"

internal enum class Dir { UP, DOWN, LEFT, RIGHT }
internal enum class Phase { READY, RUN, DEAD }
internal enum class Scr { HUB, PLAY, SHOP, CASES, CHARS, FEED }
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
                onShop = { scr = Scr.SHOP }, onCases = { scr = Scr.CASES }, onChars = { scr = Scr.CHARS },
                onRefresh = { tick++ })
            Scr.PLAY -> ClassicPlay(store, mode) { tick++; store.saveBackup(); scr = Scr.HUB }
            Scr.SHOP -> Shop(store, { tick++ }) { scr = Scr.HUB }
            Scr.CASES -> Cases(store, { tick++ }) { scr = Scr.HUB }
            Scr.CHARS -> Chars(store, { tick++ }, onPlay = { scr = Scr.FEED }, onBack = { scr = Scr.HUB })
            Scr.FEED -> FeedingPlay(store) { tick++; store.saveBackup(); scr = Scr.HUB }
        }
    }
}

@Composable
internal fun Glass(mod: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
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
        Text("liquid glass · smooth body", color = C.muted, fontSize = 12.sp)
        Glass(Modifier.fillMaxWidth()) {
            Text("${store.nickname} · ур.${store.level}", color = C.text, fontWeight = FontWeight.Bold)
            Text("${store.coins} монет · ${store.xp} XP", color = C.gold, fontSize = 14.sp)
            Text("Бэкап: Android/data/…/files/snake2d/", color = C.muted, fontSize = 10.sp)
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
        Glass(Modifier.fillMaxWidth().clickable(onClick = onChars)) {
            Text("ПЕРСОНАЖИ · ПОЕДАНИЕ", color = C.text, fontWeight = FontWeight.Bold)
            Text("Враги · ульта · ивенты", color = C.muted, fontSize = 12.sp)
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
        Text("${store.coins} монет", color = C.gold, fontSize = 13.sp)
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
                            store.selectedSkin = skin.id; store.saveBackup(); onChanged()
                        })
                        skin.unlockOnlyCase -> Text("ТОЛЬКО КЕЙС", color = C.muted, fontSize = 11.sp)
                        else -> Text("${skin.price}", color = C.gold, fontSize = 13.sp, modifier = Modifier.clickable {
                            if (store.spendCoins(skin.price, "Скин ${skin.name}")) {
                                store.unlockSkin(skin.id); store.selectedSkin = skin.id
                                store.logPurchase(skin.name, skin.price); store.saveBackup(); onChanged()
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
        Text("${store.coins} монет · ${ShopData.cases.size} типов", color = C.gold)
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
                            store.recordCaseOpened(); store.logCase(c.name, drop.name, dup); store.saveBackup(); onChanged()
                        }.padding(12.dp),
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
                            Text("Ульта: ${ab.name} (КД ${ab.cooldownSec}с)", color = C.cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            ab.effects.forEach { e -> Text("· ${e.title}: ${e.description}", color = C.muted, fontSize = 11.sp) }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                when {
                    selected -> Text("ВЫБРАН", color = C.mint, fontWeight = FontWeight.Bold)
                    unlocked -> Text("ВЫБРАТЬ", color = C.cyan, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                        store.selectedCharacter = ch.id; store.saveBackup(); onChanged()
                    })
                    else -> Text("КУПИТЬ ${ch.unlockPrice}", color = C.gold, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                        if (store.spendCoins(ch.unlockPrice, "Персонаж ${ch.name}")) {
                            store.unlockCharacter(ch.id); store.selectedCharacter = ch.id; store.saveBackup(); onChanged()
                        }
                    })
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
