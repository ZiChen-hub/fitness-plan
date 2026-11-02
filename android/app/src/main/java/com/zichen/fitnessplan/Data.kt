package com.zichen.fitnessplan

// ============ 静态计划数据（与网站 data.js 同源） ============

object D {
    val PROFILE = mapOf("height" to 183, "weight" to 63, "goal" to "增肌 · 薄肌", "sessions" to 4)
    val NUTRITION = mapOf(
        "kcal" to "2700 kcal", "protein" to "蛋白质 110–135 g（1.75–2.15 g/kg）",
        "fat" to "脂肪 60–75 g", "carbs" to "碳水 350–400 g", "meals" to "3 正餐 + 2–3 次加餐",
        "tips" to "先连续吃够两周，再看周平均体重调整；每周增重约 0.16–0.32 kg。连续两周体重不动，再加 100–150 kcal。"
    )
    val WEEK_PLAN = listOf(
        mapOf("day" to "周一", "key" to "pushA", "name" to "推 A", "focus" to "胸为主", "time" to "50–65 分钟", "desc" to "重点练胸肌 + 下肢推"),
        mapOf("day" to "周二", "key" to "rest", "name" to "休息", "focus" to "—", "time" to "—", "desc" to "不做高强度训练，正常饮食"),
        mapOf("day" to "周三", "key" to "pullA", "name" to "拉 A", "focus" to "背为主", "time" to "50–65 分钟", "desc" to "重点练背 + 下肢拉"),
        mapOf("day" to "周四", "key" to "pushB", "name" to "推 B", "focus" to "肩为主", "time" to "50–65 分钟", "desc" to "重点练肩"),
        mapOf("day" to "周五", "key" to "rest", "name" to "休息", "focus" to "—", "time" to "—", "desc" to "不做高强度训练，正常饮食"),
        mapOf("day" to "周六", "key" to "pullB", "name" to "拉 B", "focus" to "背＋手臂为主", "time" to "50–65 分钟", "desc" to "背与手臂再刺激"),
        mapOf("day" to "周日", "key" to "rest", "name" to "休息", "focus" to "复盘", "time" to "—", "desc" to "汇总周平均体重，准备下周食物")
    )
    val DAY_TITLES = mapOf(
        "pushA" to "推 A · 周一（胸为主）", "pullA" to "拉 A · 周三（背为主）",
        "pushB" to "推 B · 周四（肩为主）", "pullB" to "拉 B · 周六（背＋手臂为主）"
    )
    // 每个训练日：动作 key, 组数, 次数, 备注
    data class Plan(val key: String, val sets: String, val reps: String, val note: String = "")
    val WORKOUTS: Map<String, List<Plan>> = mapOf(
        "pushA" to listOf(
            Plan("benchPress", "3", "8–12"), Plan("inclinePress", "3", "8–12"),
            Plan("shoulderPress", "3", "8–12"), Plan("lateralRaise", "3", "12–15"),
            Plan("tricepsPushdown", "2–3", "10–15"), Plan("crunch", "2–3", "12–20"),
            Plan("gobletSquat", "2", "8–12", "腿部次要")),
        "pullA" to listOf(
            Plan("latPulldown", "3", "8–12"), Plan("seatedRow", "3", "8–12"),
            Plan("facePull", "2–3", "12–15"), Plan("curl", "2–3", "10–15"),
            Plan("hammerCurl", "2", "10–15"), Plan("hangingLegRaise", "2–3", "8–15"),
            Plan("romanianDeadlift", "2", "8–12", "腿部次要")),
        "pushB" to listOf(
            Plan("shoulderPress", "3", "8–12"), Plan("chestFly", "2–3", "12–15"),
            Plan("frontRaise", "2", "12–15"), Plan("lateralRaise", "3", "12–15"),
            Plan("overheadExtension", "2", "10–15"), Plan("tricepsPushdown", "2", "10–15"),
            Plan("crunch", "2–3", "12–20")),
        "pullB" to listOf(
            Plan("oneArmRow", "3", "每侧 8–12"), Plan("latPulldown", "2–3", "8–12"),
            Plan("reverseFly", "2", "12–15"), Plan("curl", "2–3", "10–15"),
            Plan("hammerCurl", "2", "10–15"), Plan("plank", "2–3", "20–40 秒"))
    )
    data class Ex(val name: String, val group: String, val targets: String, val how: String, val wrong: String,
                  val bv: String, val vTitle: String, val up: String)
    val EXERCISES: Map<String, Ex> = mapOf(
        "benchPress" to Ex("哑铃平板卧推", "胸", "胸 / 肱三头 / 肩前束", "头、上背和臀贴凳，双脚稳定；肩胛自然后收，手腕叠在前臂上；哑铃落向胸两侧，上臂与躯干约呈 30°–60°。", "腰部猛拱、落点过高或弹胸：减重并控制下放；肩前侧刺痛：停止并换器械。", "BV1mD421J7o3", "哑铃平板卧推详解", "凯圣王"),
        "inclinePress" to Ex("上斜哑铃卧推", "胸", "上胸 / 肱三头", "凳角先取较低上斜，腕肘堆叠，哑铃沿无痛弧线下降和推起。", "凳角过高变成肩推、肩不适：降低角度或换器械推胸。", "BV1aa4y1o7XA", "上斜哑铃卧推详解", "凯圣王"),
        "chestFly" to Ex("哑铃飞鸟 / 器械夹胸", "胸", "胸肌中缝", "肩胛自然后收，肘微屈保持固定角度，哑铃沿弧线下放至胸两侧，再用胸肌合拢。", "肘过度弯曲变成卧推、耸肩：减重，保持肘部角度固定。", "BV165411y72J", "哑铃夹胸详解", "凯圣王"),
        "shoulderPress" to Ex("坐姿哑铃推举", "肩", "肩前中束 / 肱三头", "臀和肋骨稳定，前臂接近垂直，向上推至无痛位置。", "腰椎过伸、耸肩：减重并缩小幅度；肩痛时不强推。", "BV1uw411N7MC", "坐姿哑铃推肩详解", "凯圣王"),
        "lateralRaise" to Ex("哑铃侧平举", "肩", "肩中束", "用轻重量，肘微屈，以肘带动向两侧抬至肩高附近，缓慢下放。", "甩腰、耸肩、哑铃过重：减重约一半重新学。", "BV1UN41157u9", "哑铃侧平举详解", "凯圣王"),
        "frontRaise" to Ex("哑铃前平举", "肩", "肩前束", "站姿，肘微屈，哑铃向前抬至肩高附近，缓慢下放。", "甩腰、抬太高：减重，只抬到肩高。", "BV1af421Q7kq", "交替哑铃前平举详解", "凯圣王"),
        "facePull" to Ex("面拉", "肩", "肩后束 / 肩袖", "绳索或弹力带向面部拉，肘抬高并向外展开，肩胛后收。", "耸肩、只用手臂：减重，以肘带动练后肩。", "BV1wZ421e7K2", "绳索面拉详解", "凯圣王"),
        "reverseFly" to Ex("反向飞鸟", "肩", "肩后束", "俯身或俯卧，掌心相对，肘微屈，向两侧抬至与肩同高，缓慢下放。", "耸肩、用腰甩：减重，以肩后束带动。", "BV1di421D7rm", "俯身哑铃侧平举详解", "凯圣王"),
        "latPulldown" to Ex("高位下拉", "背", "背阔肌 / 肱二头", "握距略宽于肩，躯干稳定，把手拉向上胸附近，随后控制回放。", "拉到颈后、甩身体、只动手臂：改到颈前并减重，让肘向下走。", "BV1dC4y1r7nQ", "高位下拉详解", "凯圣王"),
        "seatedRow" to Ex("坐姿绳索划船", "背", "背阔肌 / 斜方肌 / 菱形肌", "躯干保持自然直立，先稳定脚和骨盆，再把把手拉向下肋附近；回程有控制。", "用腰前后甩：减重；一味猛夹肩胛或耸肩：放松颈部，以肘向后带动。", "BV1UG411k7ZM", "坐姿划船详解", "凯圣王"),
        "oneArmRow" to Ex("单臂哑铃划船", "背", "背阔肌", "一手扶凳，背自然平直，把哑铃拉向髋侧，肘贴近身体向后上方走，控制下放。", "弓背、转体、耸肩：减重，保持躯干稳定，以肘带动。", "BV1ij411H773", "哑铃划船详解", "凯圣王"),
        "curl" to Ex("哑铃弯举", "手臂", "肱二头", "肘贴体侧，前臂向上弯起，控制下放。", "甩腰、肘前移：减重并固定肘部。", "BV1Fp4y1d73D", "哑铃弯举详解", "凯圣王"),
        "hammerCurl" to Ex("锤式弯举", "手臂", "肱二头 / 肱肌", "掌心相对持哑铃，肘贴体侧，前臂向上弯起，控制下放。", "甩腰、肘前移：减重并固定肘部。", "BV1iJ411U72E", "如何正确地做锤式弯举", "蔡梓强"),
        "tricepsPushdown" to Ex("绳索下压", "手臂", "肱三头", "肘贴身体两侧基本固定，前臂下压至伸直，缓慢回放。", "用肩和身体下压：减重，只动前臂。", "BV1fjMyzZEt5", "肱三头肌绳索下压（保姆级教程）", "ALEX健身频道"),
        "overheadExtension" to Ex("哑铃过头臂屈伸", "手臂", "肱三头长头", "双手或单手持哑铃于头后，肘朝前上方固定，前臂伸展再控制回放。", "肘外张、用肩发力：减重，肘尽量朝前并夹紧。", "BV1wm6dBFEd8", "过头臂屈伸教学/技巧全解析", "ALEX健身频道"),
        "crunch" to Ex("卷腹", "腹", "腹直肌", "仰卧屈膝，下巴微收，用腹肌把肩胛卷离地面，缓慢下放，腰不离地。", "用手抱头猛拉：双手轻贴耳侧，只卷起肩胛。", "BV1uT421Y7Lr", "腹肌训练详解", "凯圣王"),
        "hangingLegRaise" to Ex("悬垂举腿", "腹", "腹直肌 / 下腹", "悬挂稳定，骨盆后倾，把腿或膝向上卷起，控制下放。", "摆身体借力：先做屈膝提腿，缩小幅度。", "BV1nJ4m1e7bj", "悬垂举腿详解", "凯圣王"),
        "plank" to Ex("平板支撑", "腹", "核心", "肘在肩下，头、躯干和骨盆保持自然一线，臀轻收，持续呼吸。", "腰塌或臀过高：缩短至 15–20 秒，质量优先。", "BV14w411V7Yj", "平板支撑详解", "凯圣王"),
        "gobletSquat" to Ex("高脚杯深蹲", "腿", "股四头肌 / 臀肌", "哑铃贴胸前；脚约与肩同宽，膝与脚尖同向，下蹲后稳定站起。", "膝内扣：减轻重量并让膝朝向第二脚趾；脚跟抬起：缩小深度。", "BV1TT4y1p7YR", "高脚杯深蹲详解", "凯圣王"),
        "romanianDeadlift" to Ex("哑铃罗马尼亚硬拉", "腿", "腘绳肌 / 臀肌", "膝微屈后基本固定，哑铃贴近大腿；把髋向后推，背部平直，再夹臀站起。", "做成深蹲：多想象「髋往后」；弓腰：缩短行程。", "BV1q3411v7cB", "哑铃罗马尼亚硬拉保姆教程", "教练Mary")
    )
    data class Start(val weight: Double, val compound: Boolean, val unit: String)
    val START_WEIGHTS: Map<String, Start> = mapOf(
        "benchPress" to Start(10.0, true, "单只哑铃"), "inclinePress" to Start(8.0, true, "单只哑铃"),
        "chestFly" to Start(6.0, false, "单只哑铃"), "shoulderPress" to Start(6.0, true, "单只哑铃"),
        "lateralRaise" to Start(4.0, false, "单只哑铃"), "frontRaise" to Start(4.0, false, "单只哑铃"),
        "facePull" to Start(15.0, false, "绳索 kg"), "reverseFly" to Start(4.0, false, "单只哑铃"),
        "latPulldown" to Start(30.0, true, "器械 kg"), "seatedRow" to Start(30.0, true, "器械 kg"),
        "oneArmRow" to Start(12.0, true, "单只哑铃"), "curl" to Start(6.0, false, "单只哑铃"),
        "hammerCurl" to Start(6.0, false, "单只哑铃"), "tricepsPushdown" to Start(15.0, false, "绳索 kg"),
        "overheadExtension" to Start(8.0, false, "单只哑铃"), "crunch" to Start(0.0, false, "自重"),
        "hangingLegRaise" to Start(0.0, false, "自重"), "plank" to Start(0.0, false, "自重·秒"),
        "gobletSquat" to Start(12.0, true, "单只哑铃"), "romanianDeadlift" to Start(12.0, true, "单只哑铃")
    )
    val STAGES = listOf(
        mapOf("phase" to "基础适应期", "time" to "第 1–3 个月", "training" to "推拉 4 次/周，哑铃 + 固定器械为主，线性加重", "weight" to "63 → 64.5–66 kg", "see" to "力量明显涨，体型变化小——这是打地基"),
        mapOf("phase" to "增肌主力期", "time" to "第 3–6 个月", "training" to "每肌群每周 10–20 组；双进阶（先加次数、再加重量）", "weight" to "累计 65–67 kg", "see" to "肩、胸、背出现可见轮廓，衣服变紧"),
        mapOf("phase" to "塑形强化期", "time" to "第 6–12 个月", "training" to "按短板换分化，加入减载周；精雕肩/胸/背/手臂", "weight" to "累计 68–71 kg", "see" to "薄肌成型：穿衣有型、脱衣有线条")
    )
    val RIR_DESC = "RIR（Reps In Reserve）指「还可以规范完成的次数」。例如一组做 10 次后估计还能做 2 次，就是 RIR 2。新手多数正式组保持 RIR 2–3。"
    val PROGRESSION_RULE = "自动推荐：首次用起始重量；若上次所有组都达到目标次数上限且有余力，下次自动 +2.5 kg（哑铃小重量 +1 kg）。重量随时可手动改。"
    val SUPPLEMENTS = listOf(
        mapOf("name" to "蛋白粉", "use" to "全天蛋白不足时补 1 份", "note" to "总饮食达标就不必喝"),
        mapOf("name" to "一水肌酸", "use" to "每天 3–5 g，训练和休息日都吃", "note" to "时间不限，随餐和水服用"),
        mapOf("name" to "鱼油", "use" to "每周吃鱼充足可不用", "note" to "看 EPA＋DHA 实际含量")
    )
    fun dayKeyForToday(): String {
        val d = java.time.LocalDate.now().dayOfWeek.value // 1=周一
        return WEEK_PLAN[d - 1]["key"] ?: "rest"
    }
    fun todayLabel(): String {
        val d = java.time.LocalDate.now().dayOfWeek.value
        return WEEK_PLAN[d - 1]["day"] ?: ""
    }
    fun repMax(reps: String): Int {
        val m = Regex("(\\d+)\\s*–\\s*(\\d+)").find(reps) ?: return Regex("(\\d+)").find(reps)?.value?.toIntOrNull() ?: 10
        return m.groupValues[2].toInt()
    }
    fun setCount(sets: String): Int = sets.split("–").lastOrNull()?.toIntOrNull() ?: 3
}
