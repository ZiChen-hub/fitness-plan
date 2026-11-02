package com.zichen.fitnessplan

import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.viewinterop.AndroidView
import android.app.Activity as AActivity
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

// ============ iOS 设计系统（浅色 / 深色） ============
data class Palette(
    val BG: Color, val Card: Color, val Blue: Color, val Green: Color, val Orange: Color,
    val Red: Color, val Gray: Color, val GrayLight: Color, val Sep: Color, val TextMain: Color,
    val SetBtn: Color,  // 「完成本组」进行中按钮：浅色用黑（力量感），深色用亮蓝
    val Fill2: Color    // iOS systemFill：未选中分段/输入框底色（比卡片更深一档）
)
val LightP = Palette(
    BG = Color(0xFFF2F2F7), Card = Color.White, Blue = Color(0xFF007AFF), Green = Color(0xFF34C759),
    Orange = Color(0xFFFF9500), Red = Color(0xFFFF3B30), Gray = Color(0xFF8E8E93),
    GrayLight = Color(0xFFAEAEB2), Sep = Color(0xFFE5E5EA), TextMain = Color(0xFF1C1C1E),
    SetBtn = Color(0xFF1C1C1E), Fill2 = Color(0xFFE9E9EB)
)
val DarkP = Palette(
    BG = Color(0xFF000000), Card = Color(0xFF1C1C1E), Blue = Color(0xFF0A84FF), Green = Color(0xFF30D158),
    Orange = Color(0xFFFF9F0A), Red = Color(0xFFFF453A), Gray = Color(0xFF8E8E93),
    GrayLight = Color(0xFF636366), Sep = Color(0xFF38383A), TextMain = Color(0xFFF2F2F7),
    SetBtn = Color(0xFF0A84FF), Fill2 = Color(0xFF2C2C2E)
)
val LocalP = staticCompositionLocalOf { LightP }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // targetSdk 35 强制 edge-to-edge，系统栏安全区在 Compose 内用 systemBarsPadding 处理
        val store = Store(this)
        setContent {
            // 订阅 Store 的主题状态：设置页切换后立即重组生效
            val themePref by store.themeState
            val dark = when (themePref) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
            val P = if (dark) DarkP else LightP
            CompositionLocalProvider(LocalP provides P) {
                // 状态栏图标颜色跟随主题
                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        androidx.core.view.WindowCompat.getInsetsController(window, view)
                            .isAppearanceLightStatusBars = !dark
                    }
                }
                MaterialTheme(
                    colorScheme = if (dark) darkColorScheme(primary = P.Blue, background = P.BG, surface = P.BG, surfaceVariant = P.Card, tertiary = P.Green, secondary = P.Orange)
                    else lightColorScheme(primary = P.Blue, background = P.BG, surface = P.BG, surfaceVariant = P.Card, tertiary = P.Green, secondary = P.Orange)
                ) { App(store) }
            }
        }
    }
}

@Composable
fun App(store: Store) {
    val P = LocalP.current

    var tab by remember { mutableStateOf(0) }
    var sessionDay by remember { mutableStateOf<String?>(null) }
    var overlay by remember { mutableStateOf<String?>(null) }
    var sessionResume by remember { mutableStateOf<SavedSession?>(null) }
    var resumeAsk by remember { mutableStateOf(false) }
    val tabs = listOf("📍", "今日", "📅", "计划", "🏋️", "动作", "📈", "统计", "👤", "我的")
    Box(Modifier.fillMaxSize().background(P.BG)) {
    Scaffold(containerColor = P.BG, modifier = Modifier.systemBarsPadding(), bottomBar = {
        Column {
            HorizontalDivider(color = P.Sep, thickness = 0.5.dp)
            Row(Modifier.fillMaxWidth().height(76.dp).background(P.Card)) {
                listOf(0 to "今日", 1 to "计划", 2 to "动作", 3 to "统计", 4 to "我的").forEach { (i, t) ->
                    Column(Modifier.weight(1f).fillMaxHeight().clickable { tab = i },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        TabIcon(i, tab == i)
                        Spacer(Modifier.height(3.dp))
                        Text(t, fontSize = 10.sp,
                            color = if (tab == i) P.Blue else P.Gray,
                            fontWeight = if (tab == i) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
    }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> TodayScreen(store) {
                    if (store.loadActive() != null) resumeAsk = true
                    else { store.clearActive(); sessionDay = D.dayKeyForToday() }
                }
                1 -> PlanScreen()
                2 -> ExerciseScreen()
                3 -> StatsScreen(store)
                4 -> MeScreen(store, onTools = { overlay = "tools" }, onSettings = { overlay = "settings" })
            }
        }
    }
    sessionDay?.let { day -> SessionScreen(store, day, sessionResume) { sessionDay = null; sessionResume = null } }
    if (resumeAsk) {
        val saved = remember(resumeAsk) { store.loadActive() }
        if (saved != null) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable { resumeAsk = false }, contentAlignment = Alignment.Center) {
            Column(Modifier.padding(32.dp).background(Color.White, RoundedCornerShape(18.dp)).padding(22.dp)) {
                Text("检测到上次未完成的训练", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = P.TextMain)
                Spacer(Modifier.height(6.dp))
                Text("${D.DAY_TITLES[saved.dayKey] ?: saved.dayKey}\n已完成 ${saved.ex.sumOf { it.done.size }} 组，是否继续？", fontSize = 14.sp, color = P.Gray)
                Spacer(Modifier.height(14.dp))
                Row {
                    IosButton("继续上次", P.Green) {
                        resumeAsk = false; sessionResume = saved; sessionDay = saved.dayKey
                    }
                    Spacer(Modifier.width(10.dp))
                    IosButton("重新开始", P.Blue) {
                        store.clearActive(); resumeAsk = false; sessionResume = null; sessionDay = D.dayKeyForToday()
                    }
                }
            }
        } else resumeAsk = false
    }
    overlay?.let { o ->
        Box(Modifier.fillMaxSize().background(P.BG).systemBarsPadding()) {
            Column(Modifier.fillMaxSize()) {
                // iOS 风格导航栏：固定高度 44dp，只占内容区，不会顶到状态栏
                Row(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.clickable { overlay = null }.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("‹", fontSize = 26.sp, color = P.Blue, fontWeight = FontWeight.Light)
                        Spacer(Modifier.width(2.dp))
                        Text(if (o == "tools") "工具箱" else "设置", fontSize = 17.sp, color = P.Blue)
                    }
                }
                HorizontalDivider(color = P.Sep, thickness = 0.5.dp)
                Box(Modifier.weight(1f)) {
                    if (o == "tools") ToolboxScreen(store) else SettingsScreen(store)
                }
            }
        }
    }
    }
}

// ---------- 基础组件 ----------
@Composable
fun IosCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val P = LocalP.current

    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)
        .background(P.Card, RoundedCornerShape(16.dp)).padding(16.dp), content = content)
}
@Composable
fun LargeTitle(text: String, sub: String = "") {
    val P = LocalP.current

    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(text, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = P.TextMain)
        if (sub.isNotEmpty()) Text(sub, fontSize = 14.sp, color = P.Gray)
    }
}
@Composable
fun IosButton(text: String, color: Color? = null, enabled: Boolean = true, onClick: () -> Unit) {
    val P = LocalP.current
    val btnColor = color ?: P.Blue
    Box(Modifier.fillMaxWidth().height(50.dp)
        .background(if (enabled) btnColor else btnColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
        .clickable(enabled = enabled) { onClick() }, contentAlignment = Alignment.Center) {
        Text(text, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------- 今日 ----------
@Composable
fun TodayScreen(store: Store, onStart: () -> Unit) {
    val P = LocalP.current

    val dayKey = D.dayKeyForToday()
    val label = D.todayLabel()
    val plan = D.WEEK_PLAN.first { it["day"] == label }
    LazyColumn(Modifier.fillMaxSize()) {
        item { LargeTitle("今日", store.today()) }
        if (dayKey == "rest") {
            item {
                Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF8E8E93), Color(0xFFAEAEB2))), RoundedCornerShape(20.dp)).padding(24.dp)) {
                    Text("💤 ${plan["name"]}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(plan["desc"]!!, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                    Spacer(Modifier.height(14.dp))
                    Text("好好休息，肌肉在休息中生长", color = Color.White, fontSize = 15.sp)
                }
            }
        } else {
            item {
                Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF007AFF), Color(0xFF5AC8FA))), RoundedCornerShape(20.dp)).padding(24.dp)) {
                    Text("${plan["name"]} · ${plan["focus"]}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("${plan["desc"]} · ${plan["time"]}", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                    Spacer(Modifier.height(10.dp))
                    D.WORKOUTS[dayKey]?.forEach { p ->
                        val ex = D.EXERCISES[p.key] ?: return@forEach
                        Text("· ${ex.name}（${p.sets}×${p.reps}）", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(18.dp))
                    val done = store.doneToday()
                    Box(Modifier.fillMaxWidth().height(50.dp).background(Color.White, RoundedCornerShape(14.dp))
                        .clickable { onStart() }, contentAlignment = Alignment.Center) {
                        Text(if (done != null) "已完成 ✓  再练一次" else "开始今天的训练  →", color = P.Blue, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                Spacer(Modifier.height(6.dp))
                IosCard { Text("💡 " + D.PROGRESSION_RULE, fontSize = 13.sp, color = P.Gray, lineHeight = 19.sp) }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

// ---------- 计划 ----------
@Composable
fun PlanScreen() {
    val P = LocalP.current

    LazyColumn(Modifier.fillMaxSize()) {
        item { LargeTitle("计划", "推拉分化 · 每周四练") }
        item {
            IosCard {
                D.WEEK_PLAN.forEachIndexed { idx, d ->
                    Row(Modifier.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(d["day"]!!, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.width(44.dp),
                            color = if (d["key"] == "rest") P.Gray else P.TextMain)
                        Column {
                            Text(if (d["key"] == "rest") "${d["name"]} · ${d["desc"]}" else "${d["name"]} · ${d["focus"]}", fontSize = 14.sp)
                            if (d["key"] != "rest") Text(d["time"]!!, fontSize = 12.sp, color = P.Gray)
                        }
                    }
                    if (idx < 6) HorizontalDivider(color = P.Sep, thickness = 0.5.dp)
                }
            }
        }
        D.WORKOUTS.forEach { (dayKey, list) ->
            item {
                Spacer(Modifier.height(10.dp))
                Text((D.DAY_TITLES[dayKey] ?: dayKey), fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = P.TextMain, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            }
            item {
                IosCard {
                    list.forEachIndexed { idx, p ->
                        val ex = D.EXERCISES[p.key] ?: return@forEachIndexed
                        Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(ex.name, fontSize = 15.sp, color = P.TextMain)
                                if (p.note.isNotEmpty()) Text(p.note, fontSize = 11.sp, color = P.Orange)
                            }
                            Text("${p.sets} 组 × ${p.reps}", fontSize = 13.sp, color = P.Gray)
                        }
                        if (idx < list.size - 1) HorizontalDivider(color = P.Sep, thickness = 0.5.dp)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(10.dp)); Text("全年三阶段", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = P.TextMain, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) }
        D.STAGES.forEach { s ->
            item {
                IosCard {
                    Row { Text(s["phase"]!!, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = P.TextMain); Spacer(Modifier.weight(1f)); Text(s["time"]!!, fontSize = 12.sp, color = P.Blue) }
                    Spacer(Modifier.height(4.dp))
                    Text(s["training"]!!, fontSize = 13.sp, color = P.TextMain, lineHeight = 18.sp)
                    Text("体重目标：${s["weight"]}", fontSize = 13.sp, color = P.Blue)
                    Text(s["see"]!!, fontSize = 12.sp, color = P.Gray)
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

// ---------- 动作库 ----------
@Composable
fun ExerciseScreen() {
    val P = LocalP.current

    var open by remember { mutableStateOf<String?>(null) }
    val ctx = LocalContext.current
    LazyColumn(Modifier.fillMaxSize()) {
        item { LargeTitle("动作", "点击查看做法与视频") }
        D.EXERCISES.entries.groupBy { it.value.group }.forEach { (g, list) ->
            item { Text(g, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = P.TextMain, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) }
            item {
                IosCard {
                    list.forEachIndexed { idx, (k, ex) ->
                        Row(Modifier.fillMaxWidth().clickable { open = k }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(ex.name, fontSize = 16.sp, color = P.TextMain)
                                Text(ex.targets, fontSize = 12.sp, color = P.Gray)
                            }
                            Text("›", fontSize = 22.sp, color = P.GrayLight)
                        }
                        if (idx < list.size - 1) HorizontalDivider(color = P.Sep, thickness = 0.5.dp)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
    open?.let { k ->
        val ex = D.EXERCISES[k]!!
        Dialog(onDismissRequest = { open = null }) {
            Column(Modifier.fillMaxWidth().background(P.BG, RoundedCornerShape(20.dp)).padding(20.dp)) {
                Text(ex.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = P.TextMain)
                Text(ex.targets, fontSize = 13.sp, color = P.Gray)
                Spacer(Modifier.height(14.dp))
                Text("✅ 正确做法", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = P.Green)
                Text(ex.how, fontSize = 14.sp, color = P.TextMain, lineHeight = 20.sp)
                Spacer(Modifier.height(10.dp))
                Text("⚠️ 常见错误", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = P.Orange)
                Text(ex.wrong, fontSize = 14.sp, color = P.TextMain, lineHeight = 20.sp)
                Spacer(Modifier.height(10.dp))
                Text("视频：${ex.vTitle} · UP：${ex.up}", fontSize = 12.sp, color = P.Gray)
                Spacer(Modifier.height(16.dp))
                Row {
                    Box(Modifier.weight(1f).height(46.dp).background(P.Blue, RoundedCornerShape(12.dp)).clickable {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.bilibili.com/video/${ex.bv}"))); open = null
                    }, contentAlignment = Alignment.Center) { Text("▶ 观看视频", color = Color.White, fontWeight = FontWeight.SemiBold) }
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f).height(46.dp).background(P.GrayLight.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).clickable { open = null },
                        contentAlignment = Alignment.Center) { Text("关闭", color = P.TextMain, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

// ---------- 统计 ----------
@Composable
fun StatsScreen(store: Store) {
    val P = LocalP.current

    val total = store.logs.size
    val totalMin = store.logs.sumOf { it.durationMin }
    val totalSets = store.logs.sumOf { it.sets.size }
    val thisWeek = store.logs.count {
        try { java.time.LocalDate.parse(it.date) > java.time.LocalDate.now().minusDays(7) } catch (e: Exception) { false }
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item { LargeTitle("统计", "训练数据一览") }
        item {
            Row(Modifier.padding(horizontal = 16.dp)) {
                listOf("训练" to "$total 次", "本周" to "$thisWeek 次", "总时长" to "$totalMin 分", "总组数" to "$totalSets").forEach { (t, v) ->
                    Column(Modifier.weight(1f).padding(3.dp).background(P.Card, RoundedCornerShape(14.dp)).padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(v, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = P.Blue)
                        Text(t, fontSize = 11.sp, color = P.Gray)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(10.dp)) }
        item {
            IosCard {
                Text("💡 针对性建议", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = P.TextMain)
                Spacer(Modifier.height(6.dp))
                store.suggestions().forEach { t ->
                    Text("· $t", fontSize = 13.sp, color = P.TextMain, lineHeight = 18.sp)
                    Spacer(Modifier.height(5.dp))
                }
            }
        }
        item {  // C1 本周部位分布
            val weekSets = store.logs.filter { try { java.time.LocalDate.parse(it.date) > java.time.LocalDate.now().minusDays(7) } catch (e: Exception) { false } }
                .flatMap { it.sets }
            val groups = listOf("胸", "肩", "背", "手臂", "腹", "腿")
            val cnt = groups.map { g -> g to weekSets.count { s2 -> s2.exKey.let { D.EXERCISES[it]?.group ?: s2.group } == g } }.filter { it.second > 0 }
            if (cnt.isNotEmpty()) {
                IosCard {
                    Text("📊 本周各部位训练量（组）", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = P.TextMain)
                    Spacer(Modifier.height(6.dp))
                    BarChart(cnt, goalMin = 8, goalMax = 20)
                    Text("绿色带 = 每肌群每周建议区间下限参考", fontSize = 11.sp, color = P.Gray)
                }
            }
        }
        item {
            IosCard {
                Text("🏆 各动作最大重量", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = P.TextMain)
                Spacer(Modifier.height(4.dp))
                store.logs.flatMap { it.sets }.groupBy { it.exKey }.filter { it.value.maxOf { s -> s.weight } > 0 }
                    .forEach { (ex, sets) ->
                        Row(Modifier.padding(vertical = 6.dp)) {
                            Text(D.EXERCISES[ex]?.name ?: ex, fontSize = 14.sp, color = P.TextMain, modifier = Modifier.weight(1f))
                            Text("${sets.maxOf { it.weight }} kg", fontSize = 14.sp, color = P.Blue, fontWeight = FontWeight.SemiBold)
                        }
                        HorizontalDivider(color = P.Sep, thickness = 0.5.dp)
                    }
                if (total == 0) Text("还没有训练记录，去「今日」开始第一次训练吧 💪", fontSize = 13.sp, color = P.Gray)
            }
        }
        if (total > 0) {
            item { Spacer(Modifier.height(10.dp)); Text("训练日志", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = P.TextMain, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) }
            itemsIndexed(store.logs.reversed()) { _, log ->
                IosCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${log.date} · ${(D.DAY_TITLES[log.dayKey] ?: log.dayKey).substringBefore(" ·")}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = P.TextMain)
                            val setSecs = log.sets.sumOf { it.seconds }
                            Text("${log.durationMin} 分钟 · ${log.sets.size} 组 · 组间总用时 ${setSecs / 60}分${setSecs % 60}秒", fontSize = 12.sp, color = P.Gray)
                        }
                        if (log.prs.isNotEmpty()) Text("🏆 ×${log.prs.size}", fontSize = 14.sp)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

// ---------- 我的 ----------
@Composable
fun MeScreen(store: Store, onTools: () -> Unit, onSettings: () -> Unit) {
    val P = LocalP.current
    var weightIn by remember { mutableStateOf("") }

    LazyColumn(Modifier.fillMaxSize()) {
        item { LargeTitle("我的") }
        item {
            IosCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).background(Brush.linearGradient(listOf(P.Blue, Color(0xFF5AC8FA))), CircleShape),
                        contentAlignment = Alignment.Center) { Text("肌", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("183 cm · 63 kg", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = P.TextMain)
                        Text("目标：增肌 · 薄肌 · 每周 4 练", fontSize = 13.sp, color = P.Gray)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("重点部位：肩 / 胸 / 腹 / 背 / 手臂（腿次要）", fontSize = 13.sp, color = P.Gray)
            }
        }
        item { Spacer(Modifier.height(8.dp)); Text("营养目标 · 每日", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = P.TextMain, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) }
        item {
            IosCard {
                D.NUTRITION.forEach { (k, v) -> if (k != "tips") { Text(v, fontSize = 14.sp, color = P.TextMain, modifier = Modifier.padding(vertical = 5.dp)); HorizontalDivider(color = P.Sep, thickness = 0.5.dp) } }
                Text("💡 " + D.NUTRITION["tips"]!!, fontSize = 12.sp, color = P.Gray, lineHeight = 17.sp)
            }
        }
        item { Spacer(Modifier.height(8.dp)); Text("补剂", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = P.TextMain, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) }
        item {
            IosCard {
                D.SUPPLEMENTS.forEachIndexed { idx, s ->
                    Text("• ${s["name"]}：${s["use"]}", fontSize = 14.sp, color = P.TextMain, modifier = Modifier.padding(vertical = 3.dp))
                    Text("   ${s["note"]}", fontSize = 12.sp, color = P.Gray)
                    if (idx < 2) HorizontalDivider(color = P.Sep, thickness = 0.5.dp)
                }
            }
        }
        item {  // B4 体重 + B5 饮水
            IosCard {
                Text("体重记录", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = P.TextMain)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("今天晨重 kg", fontSize = 13.sp, color = P.Gray, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = weightIn, onValueChange = { weightIn = it }, singleLine = true,
                        shape = RoundedCornerShape(10.dp), modifier = Modifier.width(100.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.background(P.Blue, RoundedCornerShape(10.dp)).clickable {
                        weightIn.toDoubleOrNull()?.let { w ->
                            store.applyPrefs(store.prefs.copy(weights = store.prefs.weights + (store.today() to w)))
                            weightIn = ""
                        }
                    }.padding(horizontal = 14.dp, vertical = 8.dp)) { Text("记录", color = Color.White, fontSize = 13.sp) }
                }
                val ws = store.prefs.weights.toSortedMap()
                if (ws.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    LineChart(ws.values.toList(), ws.keys.toList().map { it.take(5) },
                        goalMin = ws.values.lastOrNull()?.plus(0.32), goalMax = ws.values.lastOrNull()?.plus(0.64))
                    Text("最近 ${fmt(ws.values.last())} kg · 共 ${ws.size} 次记录", fontSize = 12.sp, color = P.Gray)
                }
            }
        }
        item {  // 饮水（目标按体重自动推荐，无需手填）
            val today = store.today()
            val drunk = store.prefs.water[today] ?: 0
            val weight = store.prefs.weights.toSortedMap().values.lastOrNull() ?: 63.0
            val hasTrained = store.logs.any { it.date == today }
            val goal = waterRecommend(weight, hasTrained)
            IosCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💧 今日饮水", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = P.TextMain)
                    Spacer(Modifier.weight(1f))
                    Text("$drunk / $goal ml", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = if (drunk >= goal) P.Green else P.Blue)
                }
                Spacer(Modifier.height(4.dp))
                Text("按体重 ${fmt(weight)}kg × 35ml${if (hasTrained) " + 训练日 500ml" else ""} 推荐，约 ${(goal / 250.0).let { Math.round(it) }} 杯",
                    fontSize = 12.sp, color = P.Gray)
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(8.dp).background(P.Sep, RoundedCornerShape(4.dp))) {
                    Box(Modifier.fillMaxWidth((drunk.toFloat() / goal).coerceIn(0f, 1f)).height(8.dp)
                        .background(if (drunk >= goal) P.Green else P.Blue, RoundedCornerShape(4.dp)))
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(200 to "+200", 300 to "+300").forEach { (ml, t) ->
                        Box(Modifier.weight(1f).height(42.dp).background(P.Blue.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .clickable { store.applyPrefs(store.prefs.copy(water = store.prefs.water + (today to drunk + ml))) },
                            contentAlignment = Alignment.Center) { Text("$t ml", color = P.Blue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                    }
                    Box(Modifier.size(42.dp).background(P.Fill2, RoundedCornerShape(12.dp))
                        .clickable { store.applyPrefs(store.prefs.copy(water = store.prefs.water + (today to (drunk - 200).coerceAtLeast(0)))) },
                        contentAlignment = Alignment.Center) { Text("撤销", color = P.Gray, fontSize = 12.sp) }
                }
            }
        }
        item {  // 入口
            IosCard {
                Row(Modifier.fillMaxWidth().clickable { onTools() }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🧰", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("工具箱", fontSize = 16.sp, color = P.TextMain, maxLines = 1, softWrap = false)
                    Spacer(Modifier.weight(1f))
                    Text("计算 · 计时 · 日历 ›", fontSize = 12.sp, color = P.Gray, maxLines = 1)
                }
                HorizontalDivider(color = P.Sep, thickness = 0.5.dp)
                Row(Modifier.fillMaxWidth().clickable { onSettings() }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⚙️", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("设置", fontSize = 16.sp, color = P.TextMain, maxLines = 1, softWrap = false)
                    Spacer(Modifier.weight(1f))
                    Text("休息 · 提示 · 外观 ›", fontSize = 12.sp, color = P.Gray, maxLines = 1)
                }
            }
        }
        item { IosCard { Text("RIR 是什么？", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = P.TextMain); Spacer(Modifier.height(4.dp)); Text(D.RIR_DESC, fontSize = 13.sp, color = P.Gray, lineHeight = 18.sp) } }
        item { IosCard { Text("进阶规则", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = P.TextMain); Spacer(Modifier.height(4.dp)); Text(D.PROGRESSION_RULE, fontSize = 13.sp, color = P.Gray, lineHeight = 18.sp) } }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

/* ==========================================================
 * 训练会话
 * 规则：
 *  - 每组独立「开始本组」→ 计时 → 「完成本组」记录重量/次数/用时
 *  - 本组进行中：锁定视频、锁定退出
 *  - 组间休息：可看视频、不可退出
 *  - 全部动作全部组完成后才允许结束并保存
 *  - 任何时刻可「放弃训练」（需二次确认，不保存）
 * ========================================================== */
data class ExState(
    val key: String,
    val setsTarget: Int,
    var repTarget: Int,
    var weight: Double,
    val done: MutableList<SetRec> = mutableListOf(),
    var running: Boolean = false,
    var startAt: Long = 0L,
    var restUntil: Long = 0L,
    var restTotal: Int = 0,
    var restOverride: Int = 0
)

@Composable
fun SessionScreen(store: Store, dayKey: String, resume: SavedSession? = null, onExit: () -> Unit) {
    val P = LocalP.current

    val plans = D.WORKOUTS[dayKey] ?: return
    var startedAt by remember { mutableStateOf(if (resume != null) resume.startedAt else System.currentTimeMillis()) }
    val st = remember {
        mutableStateOf(if (resume != null) {
            plans.map { p ->
                val sv = resume.ex.firstOrNull { it.key == p.key }
                val e = ExState(p.key, D.setCount(p.sets), sv?.repTarget ?: 0, sv?.weight ?: 0.0)
                if (sv != null) { e.done.addAll(sv.done) }
                e
            }
        } else plans.map { p -> ExState(p.key, D.setCount(p.sets), 0, 0.0) })
    }
    val list = st.value
    list.forEachIndexed { i, p ->
        if (p.repTarget == 0) { val pl = plans[i]; p.repTarget = D.repMax(pl.reps); p.weight = store.recommend(dayKey, pl.key) }
    }

    var secs by remember { mutableIntStateOf(0) }
    var videoOf by remember { mutableStateOf<String?>(null) }
    var editOf by remember { mutableStateOf<ExState?>(null) }
    var confirmQuit by remember { mutableStateOf(false) }
    var confirmFinish by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    // A3 热身勾选状态
    val warmList = remember {
        val parts = plans.mapNotNull { D.EXERCISES[it.key]?.group }.distinct()
        WARMUP_COMMON + parts.mapNotNull { WARMUP_BY_GROUP[it] }
    }
    val warmDone = remember { mutableStateListOf<String>() }

    // 每秒心跳：驱动总时长 / 组计时 / 休息倒计时
    LaunchedEffect(Unit) {
        while (true) {
            delay(250)
            secs = ((System.currentTimeMillis() - startedAt) / 1000).toInt()
        }
    }

    val running = list.firstOrNull { it.running }
    val restLeft = list.maxOfOrNull { e -> if (e.restUntil > System.currentTimeMillis()) ((e.restUntil - System.currentTimeMillis() + 999) / 1000).toInt() else 0 } ?: 0
    val allSets = list.sumOf { it.setsTarget }
    val doneSets = list.sumOf { it.done.size }
    val allDone = doneSets >= allSets

    Box(Modifier.fillMaxSize().background(P.BG).systemBarsPadding()) {
        Column {
            // ---- 顶部栏 ----
            Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (running != null) "🔒" else "✕", fontSize = 17.sp, color = if (running != null) P.GrayLight else P.Blue,
                    modifier = Modifier.clickable(enabled = running == null) { confirmQuit = true }.padding(6.dp))
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text((D.DAY_TITLES[dayKey] ?: dayKey).substringBefore(" ·"), fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = P.TextMain)
                    val trainSoFar = list.sumOf { e -> e.done.sumOf { sd -> sd.seconds } }
                    Text("总 ${String.format("%02d:%02d", secs / 60, secs % 60)} · 练 ${trainSoFar / 60}分${trainSoFar % 60}秒 · $doneSets/$allSets 组", fontSize = 12.sp, color = P.Gray)
                }
                Spacer(Modifier.weight(1f))
                if (allDone) Text("完成", fontSize = 15.sp, color = P.Green, fontWeight = FontWeight.SemiBold)
                else Text("$doneSets/$allSets", fontSize = 15.sp, color = P.Blue, fontWeight = FontWeight.SemiBold)
            }
            Box(Modifier.fillMaxWidth().height(3.dp).background(P.Sep)) {
                Box(Modifier.fillMaxWidth(if (allSets == 0) 0f else doneSets.toFloat() / allSets).height(3.dp)
                    .background(P.Blue))
            }
            if (running != null) {
                Text("🔴 本组进行中 · 暂不能退出或看视频，完成本组后解锁",
                    fontSize = 12.sp, color = P.Red, modifier = Modifier.fillMaxWidth()
                        .background(P.Red.copy(alpha = 0.08f)).padding(vertical = 6.dp).padding(horizontal = 16.dp))
            }

            LazyColumn(Modifier.weight(1f)) {
                item {  // A3 练前热身
                    val bodyParts = list.mapNotNull { D.EXERCISES[it.key]?.group }.distinct()
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                        .background(P.Card, RoundedCornerShape(16.dp)).padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔥 练前热身", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = P.TextMain)
                            Spacer(Modifier.weight(1f))
                            Text(if (warmDone.size >= warmList.size) "已完成 ✓" else "${warmDone.size}/${warmList.size}",
                                fontSize = 13.sp, color = if (warmDone.size >= warmList.size) P.Green else P.Gray)
                        }
                        Spacer(Modifier.height(8.dp))
                        warmList.forEach { w ->
                            Row(Modifier.fillMaxWidth().clickable {
                                if (warmDone.contains(w)) warmDone.remove(w) else warmDone.add(w)
                            }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(20.dp).background(
                                    if (warmDone.contains(w)) P.Green else Color.Transparent,
                                    RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                                    if (warmDone.contains(w)) Text("✓", color = Color.White, fontSize = 12.sp)
                                }
                                if (!warmDone.contains(w)) Box(Modifier.size(20.dp).border(1.5.dp, P.GrayLight, RoundedCornerShape(6.dp)))
                                Spacer(Modifier.width(10.dp))
                                Text(w, fontSize = 14.sp, color = if (warmDone.contains(w)) P.Gray else P.TextMain,
                                    textDecoration = if (warmDone.contains(w)) TextDecoration.LineThrough else null)
                            }
                        }
                        Text("💡 正式组前：用约 50% 重量先做 8 次热身组", fontSize = 12.sp, color = P.Orange)
                    }
                }
                itemsIndexed(list) { i, ex ->
                    val info = D.EXERCISES[ex.key]!!
                    val complete = ex.done.size >= ex.setsTarget
                    val setNo = ex.done.size + 1
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                        .background(P.Card, RoundedCornerShape(16.dp)).padding(16.dp)) {
                        // 标题行
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(26.dp).background(if (complete) P.Green else P.Blue, CircleShape),
                                contentAlignment = Alignment.Center) {
                                Text(if (complete) "✓" else "${i + 1}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(info.name, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                                    color = if (complete) P.Gray else P.TextMain,
                                    textDecoration = if (complete) TextDecoration.LineThrough else null)
                                Text("${info.targets} · ${ex.done.size}/${ex.setsTarget} 组", fontSize = 12.sp, color = P.Gray)
                            }
                            val locked = running != null
                            Text("▶ 视频", fontSize = 13.sp,
                                color = if (locked) P.GrayLight else P.Blue,
                                modifier = Modifier.padding(start = 6.dp).clickable(enabled = !locked) { videoOf = ex.key })
                        }
                        // 要领速览（始终可见，不必退出训练去查）
                        Spacer(Modifier.height(8.dp))
                        Text(info.how, fontSize = 13.sp, color = P.TextMain, lineHeight = 19.sp,
                            modifier = Modifier.background(P.BG, RoundedCornerShape(10.dp)).fillMaxWidth().padding(10.dp))
                        Spacer(Modifier.height(6.dp))
                        Text("⚠️ " + info.wrong, fontSize = 12.sp, color = P.Orange, lineHeight = 17.sp)

                        // 已完成组
                        if (ex.done.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ex.done.forEachIndexed { si, s ->
                                    Text("${si + 1}组 ${if (s.weight > 0) "${fmt(s.weight)}kg" else "自重"}×${s.reps} · ${s.seconds}s",
                                        fontSize = 11.sp, color = P.Green,
                                        modifier = Modifier.background(P.Green.copy(alpha = 0.12f), RoundedCornerShape(8.dp)).padding(horizontal = 7.dp, vertical = 3.dp))
                                }
                            }
                        }

                        if (!complete) {
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (ex.weight > 0) "第 $setNo 组 · ${fmt(ex.weight)} kg × ${ex.repTarget} 次" else "第 $setNo 组 · 自重 × ${ex.repTarget} 次",
                                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = P.Blue)
                                Spacer(Modifier.weight(1f))
                                val canEdit = running == null || running.key == ex.key
                                Text("调整", fontSize = 13.sp, color = if (canEdit) P.Blue else P.GrayLight,
                                    modifier = Modifier.clickable(enabled = canEdit) { editOf = ex })
                            }
                            Spacer(Modifier.height(10.dp))
                            if (!ex.running) {
                                IosButton(if (running != null) "先完成当前组" else if (ex.done.isEmpty()) "开始动作 · 第 $setNo 组" else "开始第 $setNo 组", color = if (running != null) P.GrayLight else P.Blue, enabled = running == null) {
                                    ex.running = true
                                    ex.startAt = System.currentTimeMillis()
                                    store.saveActive(dayKey, startedAt, list)
                                }
                            } else {
                                // 组用时 = 总心跳秒数 - 本组开始前的总秒数，随每秒心跳自动刷新
                                val setSec = (secs - ((ex.startAt - startedAt) / 1000).toInt()).coerceAtLeast(0)
                                Box(Modifier.fillMaxWidth().height(50.dp).background(P.SetBtn, RoundedCornerShape(14.dp))
                                    .clickable { finishSet(ex, store, dayKey, ctx); store.saveActive(dayKey, startedAt, list) }, contentAlignment = Alignment.Center) {
                                    Text("完成本组  ${setSec}s", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(Modifier.weight(1f).height(38.dp).background(P.GrayLight.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                        .clickable { ex.weight = (ex.weight - stepFor(ex.key)).coerceAtLeast(0.0); ex.running = false }, contentAlignment = Alignment.Center) {
                                        Text("减重重做本组", fontSize = 13.sp, color = P.Gray)
                                    }
                                    Box(Modifier.weight(1f).height(38.dp).background(P.GrayLight.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                        .clickable { ex.running = false }, contentAlignment = Alignment.Center) {
                                        Text("取消本组", fontSize = 13.sp, color = P.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(10.dp))
                    IosButton(if (allDone) "✓ 完成训练并保存" else "还有 ${allSets - doneSets} 组未完成",
                        color = if (allDone) P.Green else P.GrayLight, enabled = allDone) { confirmFinish = true }
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth().height(40.dp).clickable(enabled = running == null) { confirmQuit = true }, contentAlignment = Alignment.Center) {
                        Text("放弃本次训练", fontSize = 13.sp, color = if (running != null) P.GrayLight else P.Red)
                    }
                    Spacer(Modifier.height(30.dp))
                }
            }
        }

        // ---- 组间休息浮层 ----
        if (restLeft > 0 && running == null) {
            val slow = list.firstOrNull { it.restUntil > System.currentTimeMillis() }
            Column(Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFFFF9500), Color(0xFFFFB340))), RoundedCornerShape(20.dp))
                .padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("休息中", color = Color.White, fontSize = 15.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("${restLeft}s", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("跳过 ›", color = Color.White, fontSize = 14.sp, modifier = Modifier.clickable { slow?.restUntil = 0L })
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(6.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(3.dp))) {
                    Box(Modifier.fillMaxWidth(restLeft.toFloat() / (slow?.restTotal ?: 1).coerceAtLeast(1)).height(6.dp).background(Color.White, RoundedCornerShape(3.dp)))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f).height(40.dp).background(Color.White, RoundedCornerShape(12.dp)).clickable {
                        videoOf = slow?.key ?: list.firstOrNull { it.done.size < it.setsTarget }?.key
                    }, contentAlignment = Alignment.Center) { Text("▶ 看本动作视频", color = P.Orange, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }

    // ---- 视频弹窗 ----
    videoOf?.let { k ->
        val ex = D.EXERCISES[k]!!
        Dialog(onDismissRequest = { videoOf = null }) {
            Column(Modifier.fillMaxWidth().background(P.BG, RoundedCornerShape(20.dp)).padding(20.dp)) {
                Text("▶ ${ex.name}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = P.TextMain)
                Text("${ex.vTitle} · UP：${ex.up}", fontSize = 12.sp, color = P.Gray)
                Spacer(Modifier.height(10.dp))
                Text(ex.how, fontSize = 13.sp, color = P.TextMain, lineHeight = 19.sp)
                Spacer(Modifier.height(6.dp))
                Text("⚠️ " + ex.wrong, fontSize = 12.sp, color = P.Orange, lineHeight = 17.sp)
                Spacer(Modifier.height(14.dp))
                IosButton("在 B 站打开视频") {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.bilibili.com/video/${ex.bv}")))
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(42.dp).background(P.GrayLight.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .clickable { videoOf = null }, contentAlignment = Alignment.Center) { Text("关闭", color = P.TextMain) }
                Spacer(Modifier.height(6.dp))
                Text(if (running != null) "⚠️ 本组进行中返回请先完成本组" else "休息时间看视频不影响计时", fontSize = 11.sp, color = P.Gray)
            }
        }
    }

    // ---- 调整重量 ----
    editOf?.let { ex ->
        var w by remember(ex.key, ex.done.size) { mutableStateOf(if (ex.weight > 0) fmt(ex.weight) else "") }
        var r by remember(ex.key, ex.done.size) { mutableStateOf("${ex.repTarget}") }
        Dialog(onDismissRequest = { editOf = null }) {
            Column(Modifier.fillMaxWidth().background(P.BG, RoundedCornerShape(20.dp)).padding(20.dp)) {
                Text("调整 · ${D.EXERCISES[ex.key]!!.name}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = P.TextMain)
                Spacer(Modifier.height(12.dp))
                Text("重量 kg（自重填 0）", fontSize = 12.sp, color = P.Gray)
                OutlinedTextField(value = w, onValueChange = { w = it }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(-2.5, -1.0, 1.0, 2.5).forEach { delta ->
                        Box(Modifier.weight(1f).height(36.dp).background(P.Card, RoundedCornerShape(10.dp)).clickable {
                            val cur = w.toDoubleOrNull() ?: 0.0
                            w = fmt((cur + delta).coerceAtLeast(0.0))
                        }, contentAlignment = Alignment.Center) {
                            Text(if (delta > 0) "+${fmt(delta)}" else fmt(delta), fontSize = 13.sp, color = P.Blue)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("每组目标次数", fontSize = 12.sp, color = P.Gray)
                OutlinedTextField(value = r, onValueChange = { r = it }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("本动作组间休息（秒，0=用全局设置 ${if ((D.START_WEIGHTS[ex.key]?.compound) == true) store.prefs.restCompound else store.prefs.restIsolation}s）", fontSize = 12.sp, color = P.Gray)
                var ro by remember(ex.key) { mutableStateOf(if (ex.restOverride > 0) "${ex.restOverride}" else "0") }
                OutlinedTextField(value = ro, onValueChange = { ro = it.filter { c -> c.isDigit() } }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
                IosButton("确定") {
                    ex.weight = w.toDoubleOrNull() ?: ex.weight
                    ex.repTarget = r.toIntOrNull() ?: ex.repTarget
                    ex.restOverride = ro.toIntOrNull() ?: 0
                    editOf = null
                }
            }
        }
    }

    // ---- 放弃确认 ----
    if (confirmQuit) {
        Dialog(onDismissRequest = { confirmQuit = false }) {
            Column(Modifier.fillMaxWidth().background(P.BG, RoundedCornerShape(20.dp)).padding(20.dp)) {
                Text("放弃本次训练？", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = P.TextMain)
                Spacer(Modifier.height(6.dp))
                Text("已完成 $doneSets 组，还有 ${allSets - doneSets} 组。放弃后本次记录不会保存。", fontSize = 13.sp, color = P.Gray, lineHeight = 18.sp)
                Spacer(Modifier.height(16.dp))
                IosButton("继续训练", color = P.Blue) { confirmQuit = false }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(46.dp).background(P.Red.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                    .clickable { confirmQuit = false; store.clearActive(); onExit() }, contentAlignment = Alignment.Center) {
                    Text("放弃并退出", color = P.Red, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // ---- 完成确认 ----
    if (confirmFinish) {
        Dialog(onDismissRequest = { confirmFinish = false }) {
            Column(Modifier.fillMaxWidth().background(P.BG, RoundedCornerShape(20.dp)).padding(20.dp).heightIn(max = 560.dp)) {
                Text("🎉 全部完成", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = P.TextMain)
                Spacer(Modifier.height(6.dp))
                val trainSec = list.sumOf { e -> e.done.sumOf { sd -> sd.seconds } }
                val restSecT = (secs - trainSec).coerceAtLeast(0)
                Text("共 $allSets 组 · 总用时 ${secs / 60}分${secs % 60}秒", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = P.TextMain)
                Text("实际训练 ${trainSec / 60}分${trainSec % 60}秒 · 休息/调整 ${restSecT / 60}分${restSecT % 60}秒", fontSize = 13.sp, color = P.Gray)
                if (restSecT > trainSec) Text("💡 休息时间超过实际训练时间，下次可缩短组间休息。", fontSize = 12.sp, color = P.Orange)
                Spacer(Modifier.height(10.dp))
                Text("本次建议", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = P.TextMain)
                Text("练后拉伸（每个 30 秒）", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = P.TextMain)
                list.mapNotNull { e -> D.EXERCISES[e.key]?.group }.distinct().mapNotNull { STRETCH_BY_GROUP[it] }
                    .take(4).forEach { t -> Text("· $t", fontSize = 12.sp, color = P.Gray, lineHeight = 17.sp) }
                Spacer(Modifier.height(8.dp))
                listOf(
                    if (avgSetSecs(list) > 45) "部分组用时偏长：检查重量是否过重或组中停顿" else null,
                    if (restSecT > trainSec * 2) "组间休息偏长，控制到 75–120 秒" else null,
                    "数据已按动作/部位/重量/次数/用时记录，可在「统计」查看"
                ).filterNotNull().forEach { t -> Text("· $t", fontSize = 12.sp, color = P.Gray, lineHeight = 17.sp) }
                Spacer(Modifier.height(14.dp))
                IosButton("保存并结束", color = P.Green) {
                    val all = list.flatMap { it.done }
                    val log = store.finishLog(dayKey, startedAt, all, restSecT)
                    if (log.prs.isNotEmpty()) android.widget.Toast.makeText(ctx,
                        "🎉 新纪录：${log.prs.mapNotNull { D.EXERCISES[it]?.name }.joinToString("、")}",
                        android.widget.Toast.LENGTH_LONG).show()
                    confirmFinish = false
                    store.clearActive()
                    onExit()
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(42.dp).clickable { confirmFinish = false }, contentAlignment = Alignment.Center) {
                    Text("再看看", fontSize = 14.sp, color = P.Gray)
                }
            }
        }
    }
}

// 完成本组：记录重量/次数/用时，进入休息
fun finishSet(ex: ExState, store: Store, dayKey: String, ctx: android.content.Context? = null) {
    val sec = ((System.currentTimeMillis() - ex.startAt) / 1000).toInt().coerceAtLeast(1)
    val info = D.EXERCISES[ex.key]!!
    ex.done.add(SetRec(ex.key, info.name, info.group, ex.weight, ex.repTarget, sec, System.currentTimeMillis()))
    ex.running = false
    val compound = D.START_WEIGHTS[ex.key]?.compound ?: true
    ex.restTotal = ex.restOverride.takeIf { it > 0 }
        ?: (if (compound && ex.weight > 0) store.prefs.restCompound else store.prefs.restIsolation)
    ex.restUntil = System.currentTimeMillis() + ex.restTotal * 1000L
    store.updateLastPerf(dayKey, ex.done.toList())
    store.save()
    if (store.prefs.sound) try { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80).startTone(ToneGenerator.TONE_PROP_BEEP, 150) } catch (_: Exception) {}
    ctx?.let { if (store.prefs.vibrate) vibrate(it, 300) }
}

fun avgSetSecs(list: List<ExState>): Int =
    list.flatMap { it.done }.filter { it.exKey != "plank" }.map { it.seconds }.let { if (it.isEmpty()) 0 else it.average().toInt() }

fun fmt(d: Double): String = if (d == d.toLong().toDouble()) "${d.toLong()}" else "$d"
fun stepFor(k: String): Double = if (D.START_WEIGHTS[k]?.compound == true) 2.5 else 1.0


// ---- 线性 Tab 图标（自绘，风格统一、随主题染色） ----
@Composable
fun TabIcon(i: Int, selected: Boolean) {
    val P = LocalP.current
    val c = if (selected) P.Blue else P.Gray
    Canvas(Modifier.size(24.dp)) {
        val w = size.width; val h = size.height; val sw = 2.dp.toPx(); val cap = StrokeCap.Round
        when (i) {
            0 -> { // 房子
                drawLine(c, Offset(w * .15f, h * .5f), Offset(w * .5f, h * .14f), sw, cap)
                drawLine(c, Offset(w * .5f, h * .14f), Offset(w * .85f, h * .5f), sw, cap)
                drawLine(c, Offset(w * .22f, h * .44f), Offset(w * .22f, h * .86f), sw, cap)
                drawLine(c, Offset(w * .78f, h * .44f), Offset(w * .78f, h * .86f), sw, cap)
                drawLine(c, Offset(w * .22f, h * .86f), Offset(w * .78f, h * .86f), sw, cap)
            }
            1 -> { // 日历
                drawRoundRect(c, Offset(w * .12f, h * .18f), Size(w * .76f, h * .7f), CornerRadius(3.dp.toPx()), style = Stroke(sw))
                drawLine(c, Offset(w * .32f, h * .08f), Offset(w * .32f, h * .3f), sw, cap)
                drawLine(c, Offset(w * .68f, h * .08f), Offset(w * .68f, h * .3f), sw, cap)
                drawLine(c, Offset(w * .12f, h * .44f), Offset(w * .88f, h * .44f), sw, cap)
            }
            2 -> { // 哑铃
                drawLine(c, Offset(w * .3f, h * .5f), Offset(w * .7f, h * .5f), sw, cap)
                drawRoundRect(c, Offset(w * .06f, h * .28f), Size(w * .16f, h * .44f), CornerRadius(2.dp.toPx()), style = Stroke(sw))
                drawRoundRect(c, Offset(w * .78f, h * .28f), Size(w * .16f, h * .44f), CornerRadius(2.dp.toPx()), style = Stroke(sw))
                drawLine(c, Offset(w * .22f, h * .5f), Offset(w * .3f, h * .5f), sw, cap)
                drawLine(c, Offset(w * .7f, h * .5f), Offset(w * .78f, h * .5f), sw, cap)
            }
            3 -> { // 柱状图
                drawLine(c, Offset(w * .22f, h * .82f), Offset(w * .22f, h * .5f), sw * 1.7f, cap)
                drawLine(c, Offset(w * .5f, h * .82f), Offset(w * .5f, h * .22f), sw * 1.7f, cap)
                drawLine(c, Offset(w * .78f, h * .82f), Offset(w * .78f, h * .62f), sw * 1.7f, cap)
            }
            4 -> { // 人
                drawCircle(c, w * .15f, Offset(w * .5f, h * .3f), style = Stroke(sw))
                drawArc(c, 180f, 180f, false, Offset(w * .2f, h * .48f), Size(w * .6f, h * .55f), style = Stroke(sw, cap = cap))
            }
        }
    }
}
