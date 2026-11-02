package com.zichen.fitnessplan

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ============ A5 提示音/震动 ============
fun beep(ctx: Context) {
    try { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80).startTone(ToneGenerator.TONE_PROP_BEEP, 150) } catch (_: Exception) {}
}
fun vibrate(ctx: Context, ms: Long = 300) {
    try {
        val v: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else @Suppress("DEPRECATION") ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
    } catch (_: Exception) {}
}

// ============ A3 热身 / A4 拉伸 数据 ============
val WARMUP_COMMON = listOf("开合跳 60 秒", "肩绕环 前后各 15 次", "髋部环绕 左右各 10 次", "弓步转体 每侧 8 次", "原地小跑摆臂 30 秒")
val WARMUP_BY_GROUP = mapOf("胸" to "墙壁推压 15 次", "背" to "弹力带扩胸 15 次", "肩" to "徒手推举画圈 各 8 次",
    "手臂" to "轻重量弯举 15 次", "腹" to "卷腹 10 次", "腿" to "徒手深蹲 15 次")
val STRETCH_BY_GROUP = mapOf(
    "胸" to "门框胸拉伸：前臂贴门框，身体前压，每侧 30 秒",
    "背" to "婴儿式：跪坐手臂前伸趴地，30–60 秒",
    "肩" to "跨身肩拉伸：一手横过胸前，另一手压住，每侧 30 秒",
    "手臂" to "三头拉伸：屈肘手摸上背，对侧轻压肘部，每侧 30 秒",
    "腹" to "眼镜蛇式：俯撑手撑起上身，髋贴地，30 秒",
    "腿" to "站姿体前屈：膝微屈摸脚尖，30–60 秒")

// ============ B1 杠铃片计算 ============
val PLATES = listOf(25.0 to "#C0392B", 20.0 to "#1F4E8C", 15.0 to "#D4A017", 10.0 to "#2E7D32", 5.0 to "#5B6B62", 2.5 to "#5B6B62", 1.25 to "#A0A0A0")
fun calcPlates(total: Double): Pair<String, String> {
    val side = (total - 20.0) / 2
    if (total < 20) return "" to "含 20kg 奥杆；目标重量需 ≥ 20kg 才能配片"
    if (side % 1.25 != 0.0) return "" to "每侧需为 1.25 的倍数（当前每侧 ${fmt(side)} kg），无法精确配片"
    var left = side; val out = mutableListOf<Double>()
    for ((w, _) in PLATES) { while (left >= w - 1e-9) { out.add(w); left -= w } }
    if (left > 1e-6) return "" to "无法精确配片（剩 ${fmt(left)} kg）"
    return out.joinToString(" + ") { fmt(it) } to ""
}

// ============ B2 1RM 计算 ============
fun calc1rm(w: Double, r: Int): Double = w * (1 + r / 30.0)

// ============ B5 饮水推荐（科学公式） ============
// 基础：体重 × 35ml（EFSA/运动营养通用区间 30–40ml/kg 的中位值）
// 训练日额外 +500ml 补训练汗液流失，向上取整到 250ml 便于执行
fun waterRecommend(weight: Double, trained: Boolean = false): Int {
    val base = weight * 35 + if (trained) 500 else 0
    return (Math.ceil(base / 250.0) * 250).toInt()
}

// ============ iOS 风输入框（无边框、填充底、圆角） ============
@Composable
fun IosField(value: String, onValueChange: (String) -> Unit, hint: String = "", modifier: Modifier = Modifier, num: Boolean = false) {
    val P = LocalP.current
    TextField(
        value = value, onValueChange = onValueChange, singleLine = true,
        placeholder = { Text(hint, fontSize = 15.sp, color = P.GrayLight) },
        textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = P.TextMain),
        keyboardOptions = KeyboardOptions(keyboardType = if (num) KeyboardType.Number else KeyboardType.Text),
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = P.Fill2, unfocusedContainerColor = P.Fill2,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent, cursorColor = P.Blue,
            focusedTextColor = P.TextMain, unfocusedTextColor = P.TextMain),
        modifier = modifier
    )
}

// ============ iOS 风分段选择器（胶囊滑块） ============
@Composable
fun IosSeg(options: List<Pair<Int, String>>, sel: Int, modifier: Modifier = Modifier, onSel: (Int) -> Unit) {
    val P = LocalP.current
    Row(modifier.fillMaxWidth().background(P.Fill2, RoundedCornerShape(10.dp)).padding(2.dp)) {
        options.forEach { (v, t) ->
            Box(Modifier.weight(1f)
                .background(if (sel == v) P.Card else Color.Transparent, RoundedCornerShape(8.dp))
                .clickable { onSel(v) }
                .padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Text(t, fontSize = 13.sp,
                    fontWeight = if (sel == v) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (sel == v) P.TextMain else P.Gray)
            }
        }
    }
}

// ============ 设置页分组行（图标 + 标题 + 值/控件 + 分隔线） ============
@Composable
fun IosRow(title: String, sub: String = "", trailing: @Composable RowScope.() -> Unit) {
    val P = LocalP.current
    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, color = P.TextMain)
            if (sub.isNotEmpty()) Text(sub, fontSize = 12.sp, color = P.Gray, lineHeight = 16.sp)
        }
        trailing()
    }
}
@Composable
fun IosGroupDivider() { val P = LocalP.current; HorizontalDivider(color = P.Sep, thickness = 0.5.dp, modifier = Modifier.padding(start = 0.dp)) }

// ============ 图表组件 ============
@Composable
fun LineChart(values: List<Double>, labels: List<String>, goalMin: Double? = null, goalMax: Double? = null) {
    val P = LocalP.current
    if (values.size < 2) { Text("数据不足（至少 2 次记录）", fontSize = 13.sp, color = P.Gray); return }
    Column {
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val minV = (values.min() - 0.5).coerceAtMost(goalMin?.minus(0.5) ?: values.min() - 0.5)
            val maxV = (values.max() + 0.5).coerceAtLeast(goalMax?.plus(0.5) ?: values.max() + 0.5)
            fun y(v: Double) = size.height * (1f - ((v - minV) / (maxV - minV)).toFloat()) * 0.92f
            val xs = values.indices.map { size.width * (0.08f + 0.84f * it / (values.size - 1)) }
            if (goalMin != null && goalMax != null) {
                drawRoundRect(P.Green.copy(alpha = 0.12f), Offset(0f, y(goalMax).toFloat()), Size(size.width, (y(goalMin) - y(goalMax)).coerceAtLeast(1f)), CornerRadius(8f))
            }
            // 面积渐变
            val path = Path()
            path.moveTo(xs.first(), y(values.first()).toFloat())
            values.indices.drop(1).forEach { path.lineTo(xs[it], y(values[it]).toFloat()) }
            path.lineTo(xs.last(), size.height); path.lineTo(xs.first(), size.height); path.close()
            drawPath(path, Brush.verticalGradient(listOf(P.Blue.copy(alpha = 0.25f), P.Blue.copy(alpha = 0f))))
            for (i in 0 until xs.size - 1) drawLine(P.Blue, Offset(xs[i], y(values[i]).toFloat()), Offset(xs[i + 1], y(values[i + 1]).toFloat()), 4f, StrokeCap.Round)
            values.forEachIndexed { i, v ->
                drawCircle(P.Card, 8f, Offset(xs[i], y(v).toFloat()))
                drawCircle(P.Blue, 6f, Offset(xs[i], y(v).toFloat()))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Text(labels.firstOrNull() ?: "", fontSize = 10.sp, color = P.Gray)
            Spacer(Modifier.weight(1f))
            Text(labels.lastOrNull() ?: "", fontSize = 10.sp, color = P.Gray, textAlign = TextAlign.End)
        }
    }
}
@Composable
fun BarChart(data: List<Pair<String, Int>>, goalMin: Int = -1, goalMax: Int = -1) {
    val P = LocalP.current
    val maxV = (data.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    Canvas(Modifier.fillMaxWidth().height(130.dp)) {
        val bw = size.width / (data.size * 1.6f)
        data.forEachIndexed { i, (_, v) ->
            val x = size.width * (0.1f + 0.8f * i / data.size)
            val bh = size.height * 0.75f * v / maxV
            drawRoundRect(P.Blue, Offset(x, size.height - bh - size.height * 0.08f), Size(bw, bh.coerceAtLeast(4f)), CornerRadius(6f))
        }
        if (goalMin > 0) {
            val y1 = size.height * 0.75f * goalMin / maxV; val y2 = size.height * 0.75f * goalMax / maxV
            drawRoundRect(P.Green.copy(alpha = 0.15f), Offset(0f, size.height - y2 - size.height * 0.08f), Size(size.width, (y2 - y1).coerceAtLeast(2f)), CornerRadius(6f))
        }
    }
    Row(Modifier.fillMaxWidth()) {
        data.forEach { (k, _) -> Text(k, fontSize = 10.sp, color = P.Gray, modifier = Modifier.weight(1f), textAlign = TextAlign.Center) }
    }
}

// ============ C3 训练日历 ============
@Composable
fun TrainCalendar(store: Store) {
    val P = LocalP.current
    val done = store.logs.map { it.date }.toSet()
    val today = java.time.LocalDate.now()
    val first = today.withDayOfMonth(1)
    val offset = first.dayOfWeek.value - 1
    val days = first.lengthOfMonth()
    Column {
        Text("${today.year} 年 ${today.monthValue} 月", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = P.TextMain, modifier = Modifier.padding(bottom = 8.dp))
        Row { listOf("一","二","三","四","五","六","日").forEach { Text(it, fontSize = 11.sp, color = P.Gray, modifier = Modifier.weight(1f), textAlign = TextAlign.Center) } }
        Spacer(Modifier.height(6.dp))
        val cells = offset + days
        for (row in 0 until (cells + 6) / 7) {
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                for (col in 0 until 7) {
                    val idx = row * 7 + col - offset + 1
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (idx in 1..days) {
                            val date = first.withDayOfMonth(idx).toString()
                            val isDone = date in done
                            Box(Modifier.size(30.dp).background(
                                when { isDone -> P.Green; date == store.today() -> P.Blue.copy(alpha = 0.15f); else -> Color.Transparent },
                                RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
                                Text("$idx", fontSize = 12.sp, fontWeight = if (isDone || date == store.today()) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isDone) Color.White else if (date == store.today()) P.Blue else P.TextMain)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============ A2 间隔计时器（Tabata/HIIT） ============
@Composable
fun IntervalTimerScreen(prefs: Prefs) {
    val P = LocalP.current; val ctx = LocalContext.current
    // 预设：0 Tabata 20/10×8  1 HIIT 40/20×8  2 自定义
    var preset by remember { mutableIntStateOf(0) }
    var work by remember { mutableStateOf("40") }; var rest by remember { mutableStateOf("20") }
    var rounds by remember { mutableStateOf("8") }
    var phase by remember { mutableStateOf(0) }   // 0停止 1工作 2休息
    var left by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(1) }
    var round by remember { mutableIntStateOf(1) }
    var running by remember { mutableStateOf(false) }

    fun startPreset(w: Int, r: Int, n: Int) { work = "$w"; rest = "$r"; rounds = "$n"; round = 1; phase = 1; left = w; total = w; running = true }

    LaunchedEffect(running) {
        while (running) {
            delay(1000); left--
            if (left in 1..3 && prefs.vibrate) vibrate(ctx, 60)
            if (left <= 0) {
                if (phase == 1) {
                    if (prefs.sound) beep(ctx); vibrate(ctx, 300)
                    val n = rounds.toIntOrNull() ?: 8
                    round++
                    if (round > n) { running = false }
                    else { phase = 2; left = rest.toIntOrNull() ?: 20; total = left }
                } else {
                    if (prefs.sound) beep(ctx); vibrate(ctx, 300)
                    phase = 1; left = work.toIntOrNull() ?: 40; total = left
                }
            }
        }
    }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (phase == 0) {
            IosSeg(listOf(0 to "Tabata", 1 to "HIIT", 2 to "自定义"), preset) { v ->
                preset = v
                if (v == 0) { work = "20"; rest = "10"; rounds = "8" }
                if (v == 1) { work = "40"; rest = "20"; rounds = "8" }
            }
            Spacer(Modifier.height(12.dp))
            if (preset == 2) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IosField(work, { work = it.filter { c -> c.isDigit() } }, "工作秒", Modifier.weight(1f), num = true)
                    IosField(rest, { rest = it.filter { c -> c.isDigit() } }, "休息秒", Modifier.weight(1f), num = true)
                    IosField(rounds, { rounds = it.filter { c -> c.isDigit() } }, "组数", Modifier.weight(1f), num = true)
                }
                Spacer(Modifier.height(12.dp))
            } else {
                Text("工作 ${work}s · 休息 ${rest}s · 共 ${rounds} 组", fontSize = 13.sp, color = P.Gray)
                Spacer(Modifier.height(12.dp))
            }
            IosButton("开始间隔训练") {
                val w = work.toIntOrNull() ?: 40; val r = rest.toIntOrNull() ?: 20
                round = 1; phase = 1; left = w; total = w; running = true
            }
        } else {
            val isWork = phase == 1
            val prog = if (total <= 0) 0f else left.toFloat() / total
            Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawArc(if (isWork) P.SetBtn.copy(alpha = 0.15f) else P.Orange.copy(alpha = 0.15f),
                        -90f, 360f, false, style = Stroke(10f, cap = StrokeCap.Round))
                    drawArc(if (isWork) P.SetBtn else P.Orange,
                        -90f, 360f * prog, false, style = Stroke(10f, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isWork) "训练" else "休息", fontSize = 14.sp, color = if (isWork) P.SetBtn else P.Orange, fontWeight = FontWeight.SemiBold)
                    Text("$left", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = P.TextMain)
                    Text(if (running) "第 $round / ${rounds.toIntOrNull() ?: 8} 组" else "已暂停", fontSize = 12.sp, color = P.Gray)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                if (running) IosButton("暂停", color = P.Gray) { running = false }
                else IosButton("继续", color = P.Green) { running = true }
                IosButton("停止", color = P.Red) { running = false; phase = 0; round = 1 }
            }
        }
    }
}

// ============ 工具箱页 ============
@Composable
fun ToolboxScreen(store: Store) {
    val P = LocalP.current; val ctx = LocalContext.current
    var openTool by remember { mutableStateOf(0) }
    var weightIn by remember { mutableStateOf("") }
    var rmW by remember { mutableStateOf("") }; var rmR by remember { mutableStateOf("") }
    var ageIn by remember { mutableStateOf(store.prefs.age.toString()) }
    var curveEx by remember { mutableStateOf<String?>(null) }

    fun BackBar() = {}

    LazyColumn(Modifier.fillMaxSize()) {
        when (openTool) {
        0 -> {
            item { LargeTitle("工具箱") }
            val tools = listOf(
                Triple(1, "🏋️", "杠铃片计算器") to "快速算出每侧该挂哪些片",
                Triple(2, "💯", "1RM 计算器") to "Epley 公式估算极限重量",
                Triple(3, "🔥", "热量 TDEE 计算器") to "按体重与年龄算每日消耗",
                Triple(4, "⏱️", "间隔计时器") to "Tabata / HIIT 循环训练",
                Triple(5, "📈", "动作进步曲线") to "查看各动作重量增长",
                Triple(6, "📅", "训练日历") to "本月打卡一览",
                Triple(7, "📤", "导出数据") to "CSV 备份发送给微信/QQ")
            tools.forEach { (tt, sub) ->
                val (id, icon, title) = tt
                item {
                    IosCard {
                        Row(Modifier.fillMaxWidth().clickable { openTool = id }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(38.dp).background(P.Fill2, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                Text(icon, fontSize = 19.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(title, fontSize = 16.sp, color = P.TextMain, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(2.dp))
                                Text(sub, fontSize = 12.sp, color = P.Gray)
                            }
                            Text("›", fontSize = 22.sp, color = P.GrayLight)
                        }
                    }
                }
            }
        }
        1 -> {  // 杠铃片
            item { LargeTitle("杠铃片计算", "含 20kg 奥杆") }
            item { IosCard {
                Text("目标总重量 kg", fontSize = 12.sp, color = P.Gray)
                Spacer(Modifier.height(6.dp))
                IosField(weightIn, { weightIn = it.filter { c -> c.isDigit() || c == '.' } }, "例如 60", Modifier.fillMaxWidth(), num = true)
                val v = weightIn.toDoubleOrNull()
                if (v != null && v > 0) {
                    val (plates, err) = calcPlates(v)
                    if (err.isEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text("每侧挂", fontSize = 12.sp, color = P.Gray)
                        Text("$plates kg", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = P.Blue)
                    } else { Spacer(Modifier.height(12.dp)); Text(err, fontSize = 13.sp, color = P.Red, lineHeight = 18.sp) }
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = P.Sep, thickness = 0.5.dp)
                Spacer(Modifier.height(8.dp))
                Text("片规格：25 / 20 / 15 / 10 / 5 / 2.5 / 1.25 kg", fontSize = 12.sp, color = P.Gray)
            } }
        }
        2 -> {  // 1RM
            item { LargeTitle("1RM 计算器", "Epley 公式估算") }
            item { IosCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) { Text("重量 kg", fontSize = 12.sp, color = P.Gray)
                        IosField(rmW, { rmW = it.filter { c -> c.isDigit() || c == '.' } }, "重量", Modifier.fillMaxWidth(), num = true) }
                    Column(Modifier.weight(1f)) { Text("次数", fontSize = 12.sp, color = P.Gray)
                        IosField(rmR, { rmR = it.filter { c -> c.isDigit() } }, "次数", Modifier.fillMaxWidth(), num = true) }
                }
                val w = rmW.toDoubleOrNull(); val r = rmR.toIntOrNull()
                if (w != null && r != null && w > 0 && r > 0) {
                    val rm = calc1rm(w, r)
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth().background(P.Blue.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(14.dp)) {
                        Column {
                            Text("估算 1RM", fontSize = 12.sp, color = P.Gray)
                            Text("${fmt(rm)} kg", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = P.Blue)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = P.Sep, thickness = 0.5.dp)
                    listOf("85%" to "力量", "75–85%" to "肌力+肌肥大", "65–75%" to "肌肥大（主要区间）", "50–65%" to "热身/技术").forEachIndexed { i, (pct, use) ->
                        if (i > 0) IosGroupDivider()
                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(pct, fontSize = 14.sp, color = P.TextMain, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.weight(1f))
                            Text("${fmt(rm * pct.removeSuffix("%").split("–").first().toDouble() / 100)} kg", fontSize = 14.sp, color = P.Gray)
                            Spacer(Modifier.width(14.dp))
                            Text(use, fontSize = 12.sp, color = P.Gray, textAlign = TextAlign.End)
                        }
                    }
                }
            } }
        }
        3 -> {  // TDEE
            item { LargeTitle("每日热量计算", "Mifflin-St Jeor · 男 · 183cm") }
            item { IosCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("年龄", fontSize = 15.sp, color = P.TextMain, modifier = Modifier.weight(1f))
                    IosField(ageIn, { v -> if (v.length <= 3) ageIn = v.filter { c -> c.isDigit() } }, "", Modifier.width(100.dp), num = true)
                    Spacer(Modifier.width(8.dp)); Text("岁", fontSize = 14.sp, color = P.Gray)
                }
                Spacer(Modifier.height(8.dp))
                val weight = store.prefs.weights.toSortedMap().values.lastOrNull() ?: 63.0
                val bmr = 10 * weight + 6.25 * 183 - 5 * (ageIn.toIntOrNull() ?: 25) + 5
                val tdee = bmr * 1.55
                Text("当前体重按 ${fmt(weight)} kg 计（可在「我的」录入晨重）", fontSize = 12.sp, color = P.Gray)
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = P.Sep, thickness = 0.5.dp)
                listOf("基础代谢 BMR" to "${bmr.toInt()} kcal", "日常消耗 TDEE（中等活动）" to "${tdee.toInt()} kcal").forEachIndexed { i, (k, v) ->
                    if (i > 0) IosGroupDivider()
                    IosRow(k) { Text(v, fontSize = 15.sp, color = P.TextMain) }
                }
                IosGroupDivider()
                IosRow("增肌目标（+300 kcal）", sub = "计划目标 2700 kcal ${if (tdee + 300 > 2700) "偏低，可再加 ${((tdee + 300) - 2700).toInt()} kcal" else "合理"}") {
                    Text("${(tdee + 300).toInt()} kcal", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = P.Blue)
                }
            } }
        }
        4 -> {  // 间隔计时器
            item { LargeTitle("间隔计时器", "Tabata / HIIT / 自定义循环") }
            item { IosCard { IntervalTimerScreen(store.prefs) } }
        }
        5 -> {  // 曲线
            val allExKeys = store.logs.flatMap { it.sets }.map { it.exKey }.distinct()
            // 自动选中最近练的动作：进来就能看到曲线，不用先点选
            if (curveEx == null && allExKeys.isNotEmpty()) curveEx = allExKeys.first()
            item { LargeTitle("进步曲线", if (allExKeys.isEmpty()) "暂无训练数据" else "共 ${allExKeys.size} 个动作 · 点击切换") }
            if (allExKeys.isEmpty()) {
                item { IosCard {
                    Text("📈 还没有可绘制的数据", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = P.TextMain)
                    Spacer(Modifier.height(6.dp))
                    Text("完成第一次训练并记录重量后，这里会自动生成每个动作的重量进步曲线。\n\n去「今日」页点「开始训练」试试吧！", fontSize = 13.sp, color = P.Gray, lineHeight = 19.sp)
                } }
            } else {
                item {
                    Column {
                        allExKeys.chunked(4).forEach { rowKeys ->
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowKeys.forEach { k ->
                                    Box(Modifier.weight(1f).background(if (curveEx == k) P.Blue else P.Card, RoundedCornerShape(10.dp))
                                        .clickable { curveEx = k }.padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                                        Text((D.EXERCISES[k]?.name ?: k).take(5), fontSize = 12.sp,
                                            fontWeight = if (curveEx == k) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (curveEx == k) Color.White else P.TextMain)
                                    }
                                }
                                repeat(4 - rowKeys.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                item {
                    val k = curveEx
                    if (k != null) {
                        val pts = store.logs.mapNotNull { l -> l.sets.filter { it.exKey == k }.maxOfOrNull { it.weight }?.let { l.date to it } }
                        IosCard {
                            Text((D.EXERCISES[k]?.name ?: k), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = P.TextMain)
                            Spacer(Modifier.height(6.dp))
                            LineChart(pts.map { it.second }, pts.map { it.first.take(5) })
                            if (pts.size >= 2 && pts.last().second > pts.first().second)
                                Text("已进步 +${fmt(pts.last().second - pts.first().second)} kg 🎉", fontSize = 13.sp, color = P.Green)
                        }
                    }
                }
            }
        }
        6 -> {  // 日历
            item { LargeTitle("训练日历", "绿色 = 已训练") }
            item { IosCard { TrainCalendar(store) } }
        }
        7 -> {  // 导出
            item { LargeTitle("导出数据") }
            item { IosCard {
                Text("以 CSV 文本导出全部训练记录，可通过微信/QQ 发送给自己保存。", fontSize = 13.sp, color = P.Gray, lineHeight = 19.sp)
                Spacer(Modifier.height(12.dp))
                IosButton("导出并发送") {
                    val sb = StringBuilder("日期,训练日,动作,部位,重量kg,次数,用时秒\n")
                    store.logs.forEach { l -> l.sets.forEach { s ->
                        sb.append("${l.date},${D.DAY_TITLES[l.dayKey]?.substringBefore(" ·") ?: l.dayKey},${s.exName.ifEmpty { D.EXERCISES[s.exKey]?.name ?: s.exKey }},${s.group},${s.weight},${s.reps},${s.seconds}\n") } }
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, sb.toString())
                        putExtra(android.content.Intent.EXTRA_TITLE, "健身记录备份.csv")
                    }
                    ctx.startActivity(android.content.Intent.createChooser(intent, "发送备份"))
                }
            } }
        }
        }
        if (openTool > 0) item {
            Spacer(Modifier.height(14.dp))
            Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                .background(P.Card, RoundedCornerShape(14.dp)).clickable { openTool = 0 }
                .padding(vertical = 13.dp), horizontalArrangement = Arrangement.Center) {
                Text("‹  返回工具箱", fontSize = 16.sp, color = P.Blue, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ============ 设置页 ============
@Composable
fun SettingsScreen(store: Store) {
    val P = LocalP.current
    var p by remember { mutableStateOf(store.prefs) }
    var ageTxt by remember { mutableStateOf(p.age.toString()) }
    fun upd(np: Prefs) { p = np; store.applyPrefs(np) }

    LazyColumn(Modifier.fillMaxSize()) {
        item { LargeTitle("设置") }
        item { IosCard {
            Text("组间休息", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = P.TextMain)
            Spacer(Modifier.height(10.dp))
            Text("复合动作（深蹲 / 卧推 / 硬拉）", fontSize = 12.sp, color = P.Gray)
            Spacer(Modifier.height(6.dp))
            IosSeg(listOf(90 to "90秒", 120 to "2分", 150 to "2分半", 180 to "3分"), p.restCompound) { upd(p.copy(restCompound = it)) }
            Spacer(Modifier.height(12.dp))
            Text("孤立动作（弯举 / 侧平举等）", fontSize = 12.sp, color = P.Gray)
            Spacer(Modifier.height(6.dp))
            IosSeg(listOf(45 to "45秒", 60 to "1分", 75 to "1分15", 90 to "1分半"), p.restIsolation) { upd(p.copy(restIsolation = it)) }
        } }
        item { IosCard {
            Text("提示", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = P.TextMain)
            IosRow("休息倒计时震动", sub = "最后 3 秒每秒短震一下") {
                Switch(checked = p.vibrate, onCheckedChange = { upd(p.copy(vibrate = it)) },
                    colors = SwitchDefaults.colors(checkedTrackColor = P.Green))
            }
            IosGroupDivider()
            IosRow("阶段切换提示音", sub = "训练 / 休息切换时短促提示音") {
                Switch(checked = p.sound, onCheckedChange = { upd(p.copy(sound = it)) },
                    colors = SwitchDefaults.colors(checkedTrackColor = P.Green))
            }
        } }
        item { IosCard {
            Text("外观", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = P.TextMain)
            Spacer(Modifier.height(10.dp))
            IosSeg(listOf(0 to "跟随系统", 1 to "浅色", 2 to "深色"), p.theme) { upd(p.copy(theme = it)) }
            Spacer(Modifier.height(6.dp))
            Text("切换后立即生效，无需重启", fontSize = 12.sp, color = P.Gray)
        } }
        item { IosCard {
            Text("身体数据", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = P.TextMain)
            IosRow("年龄", sub = "用于 TDEE 热量计算") {
                IosField(ageTxt, { v -> if (v.length <= 3) { ageTxt = v.filter { c -> c.isDigit() }; ageTxt.toIntOrNull()?.let { upd(p.copy(age = it)) } } },
                    "", Modifier.width(90.dp), num = true)
                Spacer(Modifier.width(8.dp)); Text("岁", fontSize = 14.sp, color = P.Gray)
            }
        } }
        item { Spacer(Modifier.height(20.dp)) }
    }
}
