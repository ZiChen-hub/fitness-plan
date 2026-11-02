package com.zichen.fitnessplan

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============ 训练日志模型与持久化 ============

data class SetRec(
    val exKey: String,
    val exName: String = "",   // 动作名称（冗余存储，便于统计/导出）
    val group: String = "",    // 主要训练部位
    val weight: Double,
    val reps: Int,
    val seconds: Int,          // 本组用时（秒）
    val ts: Long
)
data class WorkoutLog(
    val date: String,       // yyyy-MM-dd
    val dayKey: String,     // pushA / pullA / pushB / pullB
    val startTs: Long,
    val durationMin: Int,   // 总时长（含休息）
    val trainSec: Int = 0,  // 实际训练时长（各组用时之和）
    val restSec: Int = 0,   // 休息/浪费时长 = 总 - 实际
    val sets: List<SetRec>,
    val prs: List<String>
)
data class LastPerf(val weight: Double, val reps: List<Int>, val allMaxHit: Boolean)

data class ExSaved(val key: String, val repTarget: Int, val weight: Double, val done: List<SetRec>)
data class SavedSession(val dayKey: String, val startedAt: Long, val ex: List<ExSaved>)

// 用户设置与每日小记录
data class Prefs(
    val restCompound: Int = 120,     // A1 复合动作组间休息
    val restIsolation: Int = 75,     // A1 孤立动作组间休息
    val vibrate: Boolean = true,     // A5 震动
    val sound: Boolean = true,       // A5 提示音
    val theme: Int = 0,              // D3 0跟随系统 1浅色 2深色
    val age: Int = 22,               // B3 TDEE 用（本人 22 岁）
    val waterGoal: Int = 2500,       // B5 每日饮水目标 ml
    val weights: Map<String, Double> = emptyMap(),  // B4 日期->晨重
    val water: Map<String, Int> = emptyMap(),       // B5 日期->已饮水 ml
    val activeSession: String = ""   // D2 中断恢复（JSON）
)

class Store(context: Context) {
    private val gson = Gson()
    private val file = java.io.File(context.filesDir, "logs.json")

    var logs: MutableList<WorkoutLog> = mutableListOf()
    var lastPerf: MutableMap<String, LastPerf> = mutableMapOf()
    var prefs: Prefs = Prefs()

    // D3 主题的可观察状态：MainActivity 订阅它，切换主题立即重组
    val themeState = androidx.compose.runtime.mutableIntStateOf(0)

    init {
        load()
        // 一次性修正：早期版本默认年龄 25，本机用户实际 22 岁
        if (prefs.age == 25) { prefs = prefs.copy(age = 22); save() }
        themeState.intValue = prefs.theme
    }

    private fun load() {
        if (!file.exists()) return
        try {
            val obj = com.google.gson.JsonParser.parseString(file.readText()).asJsonObject
            logs = gson.fromJson(obj.getAsJsonArray("logs"), object : TypeToken<MutableList<WorkoutLog>>() {}.type)
            lastPerf = gson.fromJson(obj.getAsJsonObject("lastPerf"), object : TypeToken<MutableMap<String, LastPerf>>() {}.type)
            obj.getAsJsonObject("prefs")?.let { prefs = gson.fromJson(it, Prefs::class.java) ?: prefs }
        } catch (_: Exception) {}
    }

    fun save() {
        val obj = com.google.gson.JsonObject()
        obj.add("logs", gson.toJsonTree(logs))
        obj.add("lastPerf", gson.toJsonTree(lastPerf))
        obj.add("prefs", gson.toJsonTree(prefs))
        file.writeText(obj.toString())
    }

    fun applyPrefs(p: Prefs) { prefs = p; themeState.intValue = p.theme; save() }

    // D2 中断恢复
    fun saveActive(dayKey: String, startedAt: Long, ex: List<ExState>) {
        val sd = SavedSession(dayKey, startedAt, ex.filter { it.done.isNotEmpty() || it.weight > 0 }
            .map { e -> ExSaved(e.key, e.repTarget, e.weight, e.done.toList()) })
        if (sd.ex.isEmpty()) { clearActive(); return }
        prefs = prefs.copy(activeSession = gson.toJson(sd)); save()
    }
    fun clearActive() { if (prefs.activeSession.isNotEmpty()) { prefs = prefs.copy(activeSession = ""); save() } }
    fun loadActive(): SavedSession? = try {
        if (prefs.activeSession.isEmpty()) null else gson.fromJson(prefs.activeSession, SavedSession::class.java)
    } catch (e: Exception) { null }

    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())

    fun doneToday(): WorkoutLog? = logs.lastOrNull { it.date == today() }

    fun updateLastPerf(dayKey: String, sets: List<SetRec>) {
        val plans = D.WORKOUTS[dayKey] ?: emptyList()
        sets.groupBy { it.exKey }.forEach { (ex, s) ->
            val target = plans.firstOrNull { it.key == ex }?.let { D.repMax(it.reps) } ?: 12
            lastPerf["$dayKey/$ex"] = LastPerf(s.maxOf { it.weight }, s.map { it.reps }, s.all { it.reps >= target })
        }
    }

    fun finishLog(dayKey: String, startTs: Long, sets: List<SetRec>, restSec: Int = 0): WorkoutLog {
        val duration = ((System.currentTimeMillis() - startTs) / 60000L).toInt().coerceAtLeast(1)
        val prs = mutableListOf<String>()
        val maxByEx = sets.groupBy { it.exKey }.mapValues { g -> g.value.maxOf { it.weight } }
        maxByEx.forEach { (ex, w) ->
            val histMax = logs.flatMap { it.sets }.filter { it.exKey == ex }.maxOfOrNull { it.weight } ?: -1.0
            if (w > histMax && w > 0) prs.add(ex)
        }
        val trainSec = sets.sumOf { it.seconds }
        val log = WorkoutLog(today(), dayKey, startTs, duration, trainSec, restSec.coerceAtLeast(0), sets, prs)
        logs.add(log)
        updateLastPerf(dayKey, sets)
        save()
        return log
    }

    // 基于历史数据的针对性建议
    fun suggestions(): List<String> {
        val out = mutableListOf<String>()
        if (logs.isEmpty()) return listOf("完成第一次训练后，这里会根据你的数据给出建议")
        // 1. 组用时分析（平板支撑等计时动作除外）
        val timedSets = logs.flatMap { it.sets }.filter { it.weight > 0 || it.exKey != "plank" }
        val avgSec = timedSets.map { it.seconds }.average().toInt()
        if (avgSec > 45) out.add("平均每组用时 ${avgSec}s，偏长。检查重量是否过重、是否组中长时间停顿；正常一组 8–12 次约 25–40 秒。")
        else if (avgSec < 15) out.add("平均每组用时仅 ${avgSec}s，可能下放太快。放慢离心（下放 2–3 秒）对增肌更有效。")
        // 2. 休息效率
        val recent = logs.lastOrNull()!!
        if (recent.restSec > recent.trainSec) out.add("上次训练休息 ${recent.restSec / 60} 分钟超过实际训练 ${recent.trainSec / 60} 分钟。把组间休息控制在 75–120 秒，训练效率会明显提升。")
        // 3. 各动作进展
        data class DS(val date: String, val rec: SetRec)
        val byEx = logs.flatMap { l -> l.sets.map { sd -> DS(l.date, sd) } }.groupBy { it.rec.exKey }
        byEx.forEach { (ex, list) ->
            if (list.size >= 2) {
                val sorted = list.sortedBy { it.date }
                val lastW = sorted.last().rec.weight
                val firstW = sorted.first().rec.weight
                if (lastW == firstW && lastW > 0 && list.size >= 3)
                    out.add("「${D.EXERCISES[ex]?.name ?: ex}」重量已 ${fmtW(lastW)}kg 连续多次未加重：优先保证动作标准与睡眠饮食，必要时减重 10% 重练动作模式。")
                else if (lastW > firstW)
                    out.add("「${D.EXERCISES[ex]?.name ?: ex}」从 ${fmtW(firstW)}kg 进步到 ${fmtW(lastW)}kg，继续保持双进阶节奏。")
            }
        }
        // 4. 频率
        val weekCount = logs.count { try { java.time.LocalDate.parse(it.date) > java.time.LocalDate.now().minusDays(7) } catch (e: Exception) { false } }
        if (weekCount < 3) out.add("本周只练了 $weekCount 次，目标每周 4 练。增肌频率比单次练到力竭更重要。")
        // C5 减载提醒：最近 4 周每周都 ≥3 练
        val wk = (1..4).count { w -> logs.count { l -> try {
            val d = java.time.LocalDate.parse(l.date)
            d > java.time.LocalDate.now().minusWeeks(w.toLong()) && d <= java.time.LocalDate.now().minusWeeks((w - 1).toLong())
        } catch (e: Exception) { false } } >= 3 }
        if (wk >= 4) out.add("已连续 4 周规律训练，本周安排一次减载：所有动作组数减少约 1/3，重量不变，让身体恢复再冲。")
        // B4 体重趋势
        val ws = prefs.weights.toSortedMap()
        if (ws.size >= 7) {
            val recent = ws.values.toList().takeLast(7).average()
            val earlier = ws.values.toList().dropLast(7).takeLast(7).ifEmpty { ws.values.toList().take(7) }.average()
            val delta = recent - earlier
            if (delta < 0.16) out.add("近两周体重仅涨 ${String.format("%.2f", delta)} kg，低于目标（每周 0.16–0.32 kg）。每日再加 100–150 kcal。")
            else if (delta > 0.64) out.add("近两周体重涨了 ${String.format("%.2f", delta)} kg，偏快。略减 100 kcal，保证增重里肌肉占比。")
            else out.add("体重增长节奏标准（两周 +${String.format("%.2f", delta)} kg），保持当前饮食。")
        }
        if (out.isEmpty()) out.add("训练节奏很好，按当前计划继续执行即可。")
        return out.distinct().take(6)
    }
    private fun fmtW(d: Double): String = if (d == d.toLong().toDouble()) "\${d.toLong()}" else "\$d"

    // 重量推荐：无历史 → 起始重量；上次全组达标 → +2.5 kg（哑铃小重量 +1 kg）
    fun recommend(dayKey: String, exKey: String): Double {
        val start = D.START_WEIGHTS[exKey]?.weight ?: 0.0
        val lp = lastPerf["$dayKey/$exKey"]
            ?: lastPerf.entries.filter { it.key.endsWith("/$exKey") }.maxByOrNull { it.key }?.value
            ?: return start
        if (!lp.allMaxHit) return lp.weight
        val compound = D.START_WEIGHTS[exKey]?.compound ?: true
        return lp.weight + if (compound) 2.5 else 1.0
    }
}
