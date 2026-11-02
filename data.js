/* ============================================================
 * 薄肌增肌计划 · 数据文件
 * 内容来源：《健身规划》文档（肩胸腹背手臂重点 + 腿部次要，推拉分化每周四练）
 * 视频：B站官方嵌入（已逐条核验 BV 号与标题）
 * ============================================================ */

// 个人档案
const PROFILE = {
  height: 183,          // 身高 cm
  weight: 63,           // 起始体重 kg
  goal: "增肌 · 薄肌",
  focus: ["肩", "胸", "腹", "背", "手臂"],
  secondary: ["腿"],
  sessionsPerWeek: 4,
  cycle: "全年三阶段（3 / 6 / 12 个月）"
};

// 营养目标（每日）
const NUTRITION = {
  kcal: 2700,
  protein: "110–135 g",
  proteinPerKg: "1.75–2.15 g/kg",
  fat: "60–75 g",
  carbs: "350–400 g",
  meals: "3 正餐 + 2–3 次加餐",
  tips: "先连续吃够两周，再看周平均体重调整；每周增重约 0.16–0.32 kg（体重的 0.25%–0.5%）。连续两周体重不动，就再加 100–150 kcal。"
};

// 补剂
const SUPPLEMENTS = [
  { name: "蛋白粉", use: "只在全天蛋白不足时补 1 份；看标签的“蛋白质”含量，不是粉末克数", note: "训练后或任何加餐均可，总饮食达标就不必喝" },
  { name: "一水肌酸", use: "每天 3–5 g，训练和休息日都吃，不加载、不停用", note: "时间不限，随餐和水服用；早期体重上升可能是肌肉内水分" },
  { name: "鱼油", use: "不是增肌必需品；每周吃鱼充足可不用。若用，按标签随餐", note: "看 EPA＋DHA 实际含量，勿自行用高剂量" }
];

// 每周安排（推拉分化，周二/周五休息）
const WEEK_PLAN = [
  { day: "周一", key: "pushA", name: "推 A", focus: "胸为主", time: "50–65 分钟", desc: "重点练胸肌 + 下肢推" },
  { day: "周二", key: "rest", name: "休息", focus: "—", time: "—", desc: "不做高强度训练，正常饮食" },
  { day: "周三", key: "pullA", name: "拉 A", focus: "背为主", time: "50–65 分钟", desc: "重点练背 + 下肢拉" },
  { day: "周四", key: "pushB", name: "推 B", focus: "肩为主", time: "50–65 分钟", desc: "重点练肩" },
  { day: "周五", key: "rest", name: "休息", focus: "—", time: "—", desc: "不做高强度训练，正常饮食" },
  { day: "周六", key: "pullB", name: "拉 B", focus: "背＋手臂为主", time: "50–65 分钟", desc: "背与手臂再刺激" },
  { day: "周日", key: "rest", name: "休息", focus: "复盘", time: "—", desc: "汇总周平均体重，准备下周食物" }
];

// 各训练日的动作（顺序、动作key、组数×次数）
const WORKOUTS = {
  pushA: {
    title: "推 A · 周一（胸为主）",
    exercises: [
      { key: "benchPress", sets: "3", reps: "8–12" },
      { key: "inclinePress", sets: "3", reps: "8–12" },
      { key: "shoulderPress", sets: "3", reps: "8–12" },
      { key: "lateralRaise", sets: "3", reps: "12–15" },
      { key: "tricepsPushdown", sets: "2–3", reps: "10–15" },
      { key: "crunch", sets: "2–3", reps: "12–20" },
      { key: "gobletSquat", sets: "2", reps: "8–12", note: "腿部次要" }
    ]
  },
  pullA: {
    title: "拉 A · 周三（背为主）",
    exercises: [
      { key: "latPulldown", sets: "3", reps: "8–12" },
      { key: "seatedRow", sets: "3", reps: "8–12" },
      { key: "facePull", sets: "2–3", reps: "12–15" },
      { key: "curl", sets: "2–3", reps: "10–15" },
      { key: "hammerCurl", sets: "2", reps: "10–15" },
      { key: "hangingLegRaise", sets: "2–3", reps: "8–15" },
      { key: "romanianDeadlift", sets: "2", reps: "8–12", note: "腿部次要" }
    ]
  },
  pushB: {
    title: "推 B · 周四（肩为主）",
    exercises: [
      { key: "shoulderPress", sets: "3", reps: "8–12" },
      { key: "chestFly", sets: "2–3", reps: "12–15" },
      { key: "frontRaise", sets: "2", reps: "12–15" },
      { key: "lateralRaise", sets: "3", reps: "12–15" },
      { key: "overheadExtension", sets: "2", reps: "10–15" },
      { key: "tricepsPushdown", sets: "2", reps: "10–15" },
      { key: "crunch", sets: "2–3", reps: "12–20" }
    ]
  },
  pullB: {
    title: "拉 B · 周六（背＋手臂为主）",
    exercises: [
      { key: "oneArmRow", sets: "3", reps: "每侧 8–12" },
      { key: "latPulldown", sets: "2–3", reps: "8–12" },
      { key: "reverseFly", sets: "2", reps: "12–15" },
      { key: "curl", sets: "2–3", reps: "10–15" },
      { key: "hammerCurl", sets: "2", reps: "10–15" },
      { key: "plank", sets: "2–3", reps: "20–40 秒" }
    ]
  }
};

// 动作库（正确做法 / 常见错误 / 目标肌群 / 细分肌群 / 视频）
const EXERCISES = {
  // —— 胸 ——
  benchPress: {
    name: "哑铃平板卧推",
    group: "胸", targets: ["胸", "肱三头", "肩前束"],
    muscles: ["chest", "frontDelt", "triceps"],
    how: "头、上背和臀贴凳，双脚稳定；肩胛自然后收，手腕叠在前臂上；哑铃落向胸两侧，上臂与躯干约呈 30°–60°。",
    wrong: "腰部猛拱、落点过高或弹胸：减重并控制下放；肩前侧刺痛：停止并换器械或请教练评估。",
    video: { bv: "BV1mD421J7o3", title: "哑铃平板卧推详解", up: "凯圣王" }
  },
  inclinePress: {
    name: "上斜哑铃卧推",
    group: "胸", targets: ["上胸", "肱三头"],
    muscles: ["chest", "frontDelt", "triceps"],
    how: "凳角先取较低上斜，腕肘堆叠，哑铃沿无痛弧线下降和推起。",
    wrong: "凳角过高变成肩推、肩不适：降低角度或换器械推胸。",
    video: { bv: "BV1aa4y1o7XA", title: "上斜哑铃卧推详解", up: "凯圣王" }
  },
  chestFly: {
    name: "哑铃飞鸟 / 器械夹胸",
    group: "胸", targets: ["胸肌中缝"],
    muscles: ["chest"],
    how: "肩胛自然后收，肘微屈保持固定角度，哑铃沿弧线下放至胸两侧，再用胸肌合拢。",
    wrong: "肘过度弯曲变成卧推、耸肩：减重，保持肘部角度固定。",
    video: { bv: "BV165411y72J", title: "哑铃夹胸详解", up: "凯圣王" }
  },

  // —— 肩 ——
  shoulderPress: {
    name: "坐姿哑铃推举",
    group: "肩", targets: ["肩前中束", "肱三头"],
    muscles: ["frontDelt", "midDelt", "triceps"],
    how: "臀和肋骨稳定，前臂接近垂直，向上推至无痛位置，不追求哑铃在头顶猛碰。",
    wrong: "腰椎过伸、耸肩：减重并缩小幅度；肩痛时不强推。",
    video: { bv: "BV1uw411N7MC", title: "坐姿哑铃推肩详解", up: "凯圣王" }
  },
  lateralRaise: {
    name: "哑铃侧平举",
    group: "肩", targets: ["肩中束"],
    muscles: ["midDelt"],
    how: "用轻重量，肘微屈，以肘带动向两侧抬至肩高附近，缓慢下放。",
    wrong: "甩腰、耸肩、哑铃过重：减重约一半重新学。",
    video: { bv: "BV1UN41157u9", title: "哑铃侧平举详解", up: "凯圣王" }
  },
  frontRaise: {
    name: "哑铃前平举",
    group: "肩", targets: ["肩前束"],
    muscles: ["frontDelt"],
    how: "站姿，肘微屈，哑铃向前抬至肩高附近，缓慢下放。",
    wrong: "甩腰、抬太高：减重，只抬到肩高。",
    video: { bv: "BV1af421Q7kq", title: "交替哑铃前平举详解", up: "凯圣王" }
  },
  facePull: {
    name: "面拉",
    group: "肩", targets: ["肩后束", "肩袖"],
    muscles: ["rearDelt"],
    how: "绳索或弹力带向面部拉，肘抬高并向外展开，肩胛后收。",
    wrong: "耸肩、只用手臂：减重，以肘带动练后肩。",
    video: { bv: "BV1wZ421e7K2", title: "绳索面拉详解", up: "凯圣王" }
  },
  reverseFly: {
    name: "反向飞鸟",
    group: "肩", targets: ["肩后束"],
    muscles: ["rearDelt"],
    how: "俯身或俯卧，掌心相对，肘微屈，向两侧抬至与肩同高，缓慢下放。",
    wrong: "耸肩、用腰甩：减重，以肩后束带动。",
    video: { bv: "BV1di421D7rm", title: "俯身哑铃侧平举详解", up: "凯圣王" }
  },

  // —— 背 ——
  latPulldown: {
    name: "高位下拉",
    group: "背", targets: ["背阔肌", "肱二头"],
    muscles: ["back", "biceps"],
    how: "握距略宽于肩，躯干稳定，把手拉向上胸附近，随后控制回放。",
    wrong: "拉到颈后、甩身体、只动手臂：改到颈前并减重，让肘向下走。",
    video: { bv: "BV1dC4y1r7nQ", title: "高位下拉详解", up: "凯圣王" }
  },
  seatedRow: {
    name: "坐姿绳索划船",
    group: "背", targets: ["背阔肌", "斜方肌", "菱形肌"],
    muscles: ["back", "biceps"],
    how: "躯干保持自然直立，先稳定脚和骨盆，再把把手拉向下肋附近；回程有控制，让肩胛自然前后移动。",
    wrong: "用腰前后甩：减重；一味猛夹肩胛或耸肩：放松颈部，以肘向后带动。",
    video: { bv: "BV1UG411k7ZM", title: "坐姿划船详解", up: "凯圣王" }
  },
  oneArmRow: {
    name: "单臂哑铃划船",
    group: "背", targets: ["背阔肌"],
    muscles: ["back"],
    how: "一手扶凳，背自然平直，把哑铃拉向髋侧，肘贴近身体向后上方走，控制下放。",
    wrong: "弓背、转体、耸肩：减重，保持躯干稳定，以肘带动。",
    video: { bv: "BV1ij411H773", title: "哑铃划船详解", up: "凯圣王" }
  },

  // —— 手臂 ——
  curl: {
    name: "哑铃弯举",
    group: "手臂", targets: ["肱二头"],
    muscles: ["biceps"],
    how: "肘贴体侧，前臂向上弯起，控制下放。",
    wrong: "甩腰、肘前移：减重并固定肘部。",
    video: { bv: "BV1Fp4y1d73D", title: "哑铃弯举详解", up: "凯圣王" }
  },
  hammerCurl: {
    name: "锤式弯举",
    group: "手臂", targets: ["肱二头", "肱肌"],
    muscles: ["biceps"],
    how: "掌心相对持哑铃，肘贴体侧，前臂向上弯起，控制下放。",
    wrong: "甩腰、肘前移：减重并固定肘部。",
    video: { bv: "BV1iJ411U72E", title: "如何正确地做锤式弯举", up: "蔡梓强" }
  },
  tricepsPushdown: {
    name: "绳索下压",
    group: "手臂", targets: ["肱三头"],
    muscles: ["triceps"],
    how: "肘贴身体两侧基本固定，前臂下压至伸直，缓慢回放。",
    wrong: "用肩和身体下压：减重，只动前臂。",
    video: { bv: "BV1fjMyzZEt5", title: "肱三头肌绳索下压（保姆级教程）", up: "ALEX健身频道" }
  },
  overheadExtension: {
    name: "哑铃过头臂屈伸",
    group: "手臂", targets: ["肱三头长头"],
    muscles: ["triceps"],
    how: "双手或单手持哑铃于头后，肘朝前上方固定，前臂伸展再控制回放。",
    wrong: "肘外张、用肩发力：减重，肘尽量朝前并夹紧。",
    video: { bv: "BV1wm6dBFEd8", title: "过头臂屈伸教学/技巧全解析", up: "ALEX健身频道" }
  },

  // —— 腹 ——
  crunch: {
    name: "卷腹",
    group: "腹", targets: ["腹直肌"],
    muscles: ["abs"],
    how: "仰卧屈膝，下巴微收，用腹肌把肩胛卷离地面，缓慢下放，腰不离地。",
    wrong: "用手抱头猛拉、做成仰卧起坐：双手轻贴耳侧，只卷起肩胛。",
    video: { bv: "BV1uT421Y7Lr", title: "腹肌训练详解", up: "凯圣王" }
  },
  hangingLegRaise: {
    name: "悬垂举腿",
    group: "腹", targets: ["腹直肌", "下腹"],
    muscles: ["abs"],
    how: "悬挂稳定，骨盆后倾，把腿或膝向上卷起，控制下放。",
    wrong: "摆身体借力、只抬腿不卷骨盆：先做屈膝提腿，缩小幅度。",
    video: { bv: "BV1nJ4m1e7bj", title: "悬垂举腿详解", up: "凯圣王" }
  },
  plank: {
    name: "平板支撑",
    group: "腹", targets: ["核心"],
    muscles: ["abs"],
    how: "肘在肩下，头、躯干和骨盆保持自然一线，臀轻收，持续呼吸。",
    wrong: "腰塌或臀过高：缩短至 15–20 秒，质量优先。",
    video: { bv: "BV14w411V7Yj", title: "平板支撑详解", up: "凯圣王" }
  },
  deadBug: {
    name: "死虫式",
    group: "腹", targets: ["核心"],
    muscles: ["abs"],
    how: "腰背轻贴地，对侧手脚缓慢伸远，肋骨不要翘起，回到起点换边。",
    wrong: "伸得越远越好：缩短杠杆，以躯干不晃动为准。",
    video: { bv: "BV19hP9zvE8V", title: "普通人最该练的核心训练——死虫式解析", up: "运动科学" }
  },

  // —— 腿（次要） ——
  gobletSquat: {
    name: "高脚杯深蹲",
    group: "腿", targets: ["股四头肌", "臀肌", "躯干"],
    muscles: ["quads", "glutes"],
    how: "哑铃或壶铃贴胸前；脚约与肩同宽，脚尖自然外展；吸气并轻度收紧躯干，膝与脚尖同向，双脚全掌着地，下蹲后稳定站起。",
    wrong: "膝内扣：减轻重量并让膝朝向第二脚趾；脚跟抬起：缩小深度、改善踝活动，勿硬压。",
    video: { bv: "BV1TT4y1p7YR", title: "高脚杯深蹲详解", up: "凯圣王" }
  },
  romanianDeadlift: {
    name: "哑铃罗马尼亚硬拉",
    group: "腿", targets: ["腘绳肌", "臀肌"],
    muscles: ["hamstrings", "glutes"],
    how: "膝微屈后基本固定，哑铃贴近大腿；把髋向后推，背部保持自然平直，下至大腿后侧明显牵拉的位置，再夹臀站起。",
    wrong: "做成深蹲：多想象“髋往后”；弓腰求深度：缩短行程，通常到大腿中段至膝下即可。",
    video: { bv: "BV1q3411v7cB", title: "哑铃罗马尼亚硬拉保姆教程", up: "教练Mary" }
  },
  legPress: {
    name: "腿举（倒蹬）",
    group: "腿", targets: ["股四头肌", "臀肌"],
    muscles: ["quads", "glutes"],
    how: "臀和下背贴住靠背，膝与脚尖同向；有控制地下放，再推回到膝微屈。",
    wrong: "为求深度让骨盆卷离靠背：缩小幅度；膝猛烈锁死：保留自然微屈。",
    video: { bv: "BV1mT4y1b7Z5", title: "倒蹬详解", up: "凯圣王" }
  },
  legCurl: {
    name: "坐姿腿弯举",
    group: "腿", targets: ["腘绳肌"],
    muscles: ["hamstrings"],
    how: "髋和身体贴稳垫子，小腿向臀部弯起，顶端短暂停顿，缓慢下放。",
    wrong: "用腰借力、幅度太大：减重，保持髋贴垫。",
    video: { bv: "BV1Hx4y1Y7TN", title: "坐姿腿弯举（保姆级教程）", up: "ALEX健身频道" }
  }
};

// 肌肉热力图：肌群名称与视图位置
const MUSCLE_GROUPS = {
  frontDelt: { name: "肩前束", view: "front" },
  chest: { name: "胸", view: "front" },
  biceps: { name: "肱二头", view: "front" },
  abs: { name: "腹", view: "front" },
  quads: { name: "股四头", view: "front" },
  midDelt: { name: "肩中束", view: "both" },
  rearDelt: { name: "肩后束", view: "back" },
  back: { name: "背", view: "back" },
  triceps: { name: "肱三头", view: "back" },
  glutes: { name: "臀", view: "back" },
  hamstrings: { name: "腘绳肌", view: "back" }
};

// 全年三阶段路线图
const STAGES = [
  {
    phase: "基础适应期", time: "第 1–3 个月",
    training: "推拉 4 次/周，哑铃 + 固定器械为主，线性加重（能完成就 +1.25~2.5 kg）",
    weight: "63 → 64.5–66 kg",
    see: "力量明显涨，体型变化小——这是打地基"
  },
  {
    phase: "增肌主力期", time: "第 3–6 个月",
    training: "可过渡推拉腿或保持推拉但加组数；每肌群每周 10–20 组；双进阶（先加次数、再加重量）",
    weight: "累计 65–67 kg",
    see: "肩、胸、背出现可见轮廓，衣服变紧"
  },
  {
    phase: "塑形强化期", time: "第 6–12 个月",
    training: "按短板换分化，加入减载周（每 4–6 周降量一周）；精雕肩/胸/背/手臂",
    weight: "累计 68–71 kg",
    see: "薄肌成型：穿衣有型、脱衣有线条"
  }
];

// 起步期（前 8 周）进阶细节
const PROGRESSION = [
  { week: "第 1 周", volume: "所有动作 2 组", effort: "每组约还能做 3–4 次", task: "轻重量学动作，不追酸痛" },
  { week: "第 2 周", volume: "所有动作 2 组", effort: "保留 2–3 次余力", task: "稳定轨迹，建立训练日志" },
  { week: "第 3–4 周", volume: "每天前 4 个动作 3 组，其余 2 组", effort: "保留 2–3 次余力", task: "逐渐接近每肌群每周约 9–10 组" },
  { week: "第 5–7 周", volume: "延续上述组数，最后小动作可加至 3 组", effort: "多数保留 1–3 次余力，不力竭", task: "用“先加次数、再加重量”进步" },
  { week: "第 8 周", volume: "所有动作减少约 1/3 组数", effort: "保留 3–4 次余力", task: "减量恢复，对比第 1 周，不测 1RM" }
];

// RIR 说明
const RIR_DESC = "RIR（Reps In Reserve）指“还可以规范完成的次数”。例如一组做 10 次后估计还能做 2 次，就是 RIR 2。新手多数正式组保持 RIR 2–3，比每组力竭更利于学动作和控制疲劳。";

// 杠铃片规格（杠铃片计算器用）
const PLATES = [
  { weight: 25, color: "#C0392B" },
  { weight: 20, color: "#1F4E8C" },
  { weight: 15, color: "#D4A017" },
  { weight: 10, color: "#2E7D32" },
  { weight: 5, color: "#FFFFFF" },
  { weight: 2.5, color: "#5B6B62" },
  { weight: 1.25, color: "#A0A0A0" }
];
const BARBELL_WEIGHT = 20; // 标准奥杆 20 kg

/* ============================================================
 * 起始重量推荐（START_WEIGHTS）
 * 依据：ACSM 抗阻训练立场声明——新手每组 8–12 次，选「能标准完成
 * 目标次数后仍保留 2–3 次余力」的重量；复合动作重量较大、孤立动作轻。
 * 下表为 63 kg 新手男性的保守起始值，实际以动作标准为准，可随时自定义。
 * ============================================================ */
const START_WEIGHTS = {
  // —— 胸（复合用较大重量，哑铃数值为「单只」）——
  benchPress:      { weight: 10, compound: true,  unit: "单只哑铃" },
  inclinePress:    { weight: 8,  compound: true,  unit: "单只哑铃" },
  chestFly:        { weight: 6,  compound: false, unit: "单只哑铃" },
  // —— 肩 ——
  shoulderPress:   { weight: 6,  compound: true,  unit: "单只哑铃" },
  lateralRaise:    { weight: 4,  compound: false, unit: "单只哑铃" },
  frontRaise:      { weight: 4,  compound: false, unit: "单只哑铃" },
  facePull:        { weight: 15, compound: false, unit: "绳索 kg" },
  reverseFly:      { weight: 4,  compound: false, unit: "单只哑铃" },
  // —— 背 ——
  latPulldown:     { weight: 30, compound: true,  unit: "器械 kg" },
  seatedRow:       { weight: 30, compound: true,  unit: "器械 kg" },
  oneArmRow:       { weight: 12, compound: true,  unit: "单只哑铃" },
  // —— 手臂 ——
  curl:            { weight: 6,  compound: false, unit: "单只哑铃" },
  hammerCurl:      { weight: 6,  compound: false, unit: "单只哑铃" },
  tricepsPushdown: { weight: 15, compound: false, unit: "绳索 kg" },
  overheadExtension:{ weight: 8, compound: false, unit: "单只哑铃" },
  // —— 腹（自重，不涉及配重）——
  crunch:          { weight: 0,  compound: false, unit: "自重" },
  hangingLegRaise: { weight: 0,  compound: false, unit: "自重" },
  plank:           { weight: 0,  compound: false, unit: "自重·秒" },
  deadBug:         { weight: 0,  compound: false, unit: "自重" },
  // —— 腿（次要）——
  gobletSquat:     { weight: 12, compound: true,  unit: "单只哑铃" },
  romanianDeadlift:{ weight: 12, compound: true,  unit: "单只哑铃" },
  legPress:        { weight: 60, compound: true,  unit: "器械 kg" },
  legCurl:         { weight: 25, compound: false, unit: "器械 kg" }
};

// 进阶规则说明（用于训练页提示）
const PROGRESSION_RULE = "自动推荐规则：首次用起始重量；若上次所有组都达到目标次数上限且仍有余力，下次自动 +2.5 kg（哑铃小重量 +1 kg）。重量都可手动改。";
const START_WEIGHT_NOTE = "起始重量依据 ACSM：选能标准完成 8–12 次、完成后再做 2–3 次才力竭的重量。新手先学动作，宁轻勿重。";
