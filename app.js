/* ============================================================
 * MyGymPlane · 交互与统计逻辑
 * v6.2.2 同步版：组级计时 / 科学饮水 / 工具箱 / 进步曲线自动选中
 * 数据存储：localStorage（仅本地，不上传）
 * ============================================================ */

/* ---------- iOS 风提示音 ---------- */
function beep(freq = 880, dur = 0.09) {
  try {
    const AC = window.AudioContext || window.webkitAudioContext;
    if (!AC) return;
    if (!window.__ac) window.__ac = new AC();
    const ac = window.__ac;
    if (ac.state === "suspended") ac.resume();
    const o = ac.createOscillator();
    const g = ac.createGain();
    o.type = "sine";
    o.frequency.value = freq;
    g.gain.setValueAtTime(0.001, ac.currentTime);
    g.gain.exponentialRampToValueAtTime(0.18, ac.currentTime + 0.01);
    g.gain.exponentialRampToValueAtTime(0.001, ac.currentTime + dur);
    o.connect(g); g.connect(ac.destination);
    o.start(); o.stop(ac.currentTime + dur + 0.02);
  } catch (e) {}
}
function vibrate(ms = 40) {
  if (Store.prefs().vibrate && navigator.vibrate) navigator.vibrate(ms);
}

/* ---------- iOS 风输入框（无边框填充底） ---------- */
function iosField(id, placeholder, extra = "") {
  return `<input class="ios-field" id="${id}" ${extra || 'type="number" inputmode="decimal"'} placeholder="${placeholder}">`;
}

/* ---------- 存储封装 ---------- */
const Store = {
  KEY_WEIGHT: "fit_weight_log",
  KEY_WORKOUT: "fit_workout_log",
  KEY_AGE: "fit_age",
  KEY_THEME: "fit_theme",
  KEY_WATER: "fit_water_log",
  KEY_PREFS: "fit_prefs",

  get(key, fallback) {
    try {
      const raw = localStorage.getItem(key);
      return raw ? JSON.parse(raw) : fallback;
    } catch (e) { return fallback; }
  },
  set(key, val) { localStorage.setItem(key, JSON.stringify(val)); },
  weightLog() { return this.get(this.KEY_WEIGHT, []); },
  workoutLog() { return this.get(this.KEY_WORKOUT, []); },

  /* ---------- 饮水（按体重科学推荐） ---------- */
  waterLog() { return this.get(this.KEY_WATER, {}); },
  waterToday() {
    const d = new Date().toISOString().slice(0, 10);
    return this.waterLog()[d] || 0;
  },
  addWater(ml) {
    const all = this.waterLog();
    const d = new Date().toISOString().slice(0, 10);
    all[d] = Math.max(0, (all[d] || 0) + ml);
    this.set(this.KEY_WATER, all);
  },

  /* ---------- 设置 ---------- */
  prefs() { return Object.assign({ vibrate: true, sound: true, theme: 0 }, this.get(this.KEY_PREFS, {})); },
  setPrefs(p) { this.set(this.KEY_PREFS, p); },
  age() { return this.get(this.KEY_AGE, 22); },
  setAge(v) { this.set(this.KEY_AGE, v); },

  addWeight(weight) {
    const list = this.weightLog();
    const today = new Date().toISOString().slice(0, 10);
    const idx = list.findIndex((x) => x.date === today);
    if (idx >= 0) list[idx].weight = weight;
    else list.push({ date: today, weight });
    list.sort((a, b) => a.date.localeCompare(b.date));
    this.set(this.KEY_WEIGHT, list);
  },

  addWorkout(rec) {
    const list = this.workoutLog();
    list.push({ id: Date.now(), ...rec });
    list.sort((a, b) => b.date.localeCompare(a.date));
    this.set(this.KEY_WORKOUT, list);
  },

  deleteWorkout(id) {
    const list = this.workoutLog().filter((x) => x.id !== id);
    this.set(this.KEY_WORKOUT, list);
  }
};

/* ---------- 工具函数 ---------- */
const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => Array.from(document.querySelectorAll(sel));

function esc(s) {
  return String(s ?? "")
    .replace(/&/g, "&amp;").replace(/</g, "&lt;")
    .replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

function planIndexForToday() {
  const d = new Date().getDay(); // 0=周日,1=周一
  return (d + 6) % 7;
}

function todayISO() { return new Date().toISOString().slice(0, 10); }
function fmtDate(s) { return s.replace(/-/g, "/"); }

function daysAgoISO(n) {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return d.toISOString().slice(0, 10);
}

// 从 "8–12" / "每侧 8–12" / "20–40 秒" 提取最大次数
function parseRepMax(s) {
  const nums = String(s || "").match(/\d+/g);
  if (!nums) return 12;
  return Math.max(...nums.map(Number));
}
// 从 "3" / "2–3" 提取目标组数（取上限）
function parseSetTarget(s) {
  const nums = String(s || "").match(/\d+/g);
  if (!nums) return 2;
  return Math.max(...nums.map(Number));
}
// 从 "10、10、12" 解析完成次数列表
function parseRepsList(s) {
  return String(s || "").split(/[、,，/]/).map((x) => parseInt(x)).filter((n) => !isNaN(n));
}
function roundHalf(n) { return Math.round(n * 2) / 2; }

// 动作起始重量信息
function startInfo(key) {
  return START_WEIGHTS[key] || { weight: 0, compound: false, unit: "" };
}

// 取某训练日某动作的目标组数×次数
function targetFor(dayKey, exKey) {
  const wo = WORKOUTS[dayKey];
  const e = wo.exercises.find((x) => x.key === exKey);
  return { sets: e ? e.sets : "3", reps: e ? e.reps : "8–12" };
}

/* ---------- 重量推荐算法 ---------- */
// 依据：无历史→起始重量；有历史且上次所有组达目标上限→自动 +2.5kg（孤立+1kg）
function recommendWeight(exKey, dayKey) {
  const sw = startInfo(exKey);
  const target = targetFor(dayKey, exKey);
  const targetMax = parseRepMax(target.reps);
  const log = Store.workoutLog();

  let last = null;
  for (const rec of log) {
    for (const e of rec.exercises) {
      if (e.key === exKey) { last = e; break; }
    }
    if (last) break;
  }

  if (!last) {
    return { weight: sw.weight, reason: "新手起始重量", unit: sw.unit, compound: sw.compound, targetSets: target.sets, targetReps: target.reps };
  }

  const prevWeight = last.weight || 0;
  const repsList = parseRepsList(last.reps);
  const minRep = repsList.length ? Math.min(...repsList) : 0;

  let weight = prevWeight;
  let reason = "保持上次重量";
  if (prevWeight > 0 && repsList.length > 0 && minRep >= targetMax) {
    const inc = sw.compound ? 2.5 : 1;
    weight = roundHalf(prevWeight + inc);
    reason = `上次达到目标上限，自动 +${inc} kg`;
  }

  return { weight, reason, unit: sw.unit, compound: sw.compound, targetSets: target.sets, targetReps: target.reps };
}

/* ---------- 导航 ---------- */
function initNav() {
  $$("#tabbar .tab-item").forEach((btn) => {
    btn.addEventListener("click", () => {
      $$("#tabbar .tab-item").forEach((b) => b.classList.remove("active"));
      btn.classList.add("active");
      $$(".page").forEach((p) => p.classList.remove("active"));
      $("#page-" + btn.dataset.page).classList.add("active");
      window.scrollTo({ top: 0 });
    });
  });
}

/* ---------- 首页 ---------- */
function renderHome() {
  $("#head-date").textContent =
    new Date().toLocaleDateString("zh-CN", { month: "long", day: "numeric", weekday: "long" });

  const todayPlan = WEEK_PLAN[planIndexForToday()];
  const log = Store.workoutLog();
  const todayDone = log.filter((r) => r.date === todayISO()).length > 0;

  // 今日卡
  if (todayPlan.key === "rest") {
    $("#today-label").textContent = "今日休息";
    $("#today-name").textContent = "恢复日";
    $("#today-sub").textContent = "好好吃、好好睡，肌肉在休息时长。可选低强度有氧 20–30 分钟。";
    $("#today-done").textContent = "";
    $("#btn-start").style.display = "none";
    $("#btn-start-rest").style.display = "block";
  } else {
    const wo = WORKOUTS[todayPlan.key];
    $("#today-label").textContent = "今日训练";
    $("#today-name").textContent = `${todayPlan.name} · ${todayPlan.focus}`;
    $("#today-sub").textContent = `${wo.exercises.length} 个动作 · 约 ${todayPlan.time} · ${todayPlan.desc}`;
    $("#today-done").textContent = todayDone ? "今日已完成" : "";
    $("#btn-start").style.display = "block";
    $("#btn-start-rest").style.display = "none";
    $("#btn-start").textContent = todayDone ? "再练一次" : "开始训练";
  }

  // 本周安排
  $("#home-week").innerHTML = WEEK_PLAN.map((w, i) => {
    const isToday = i === planIndexForToday();
    const rest = w.key === "rest";
    const name = rest ? "休息" : w.name;
    return `<div class="week-item${rest ? " rest" : ""}${isToday ? " today" : ""}">
      <span class="dot"></span>
      <span class="day">${w.day}</span>
      <span class="wname">${name}</span>
      <span class="wfocus">${rest ? "—" : esc(w.focus)}</span>
    </div>`;
  }).join("");

  renderOverview();
}

function renderOverview() {
  const log = Store.workoutLog();
  const weights = Store.weightLog();
  const weekCount = log.filter((r) => r.date >= daysAgoISO(6)).length;
  let streak = 0;
  const done = new Set(log.map((r) => r.date));
  for (let i = 0; i < 365; i++) {
    if (done.has(daysAgoISO(i))) streak++;
    else break;
  }
  const latestWeight = weights.length ? weights[weights.length - 1].weight : PROFILE.weight;
  $("#home-overview").innerHTML = `
    <div class="stat-box"><div class="stat-num">${weekCount}</div><div class="stat-label">本周训练</div></div>
    <div class="stat-box"><div class="stat-num">${streak}</div><div class="stat-label">连续天数</div></div>
    <div class="stat-box"><div class="stat-num">${latestWeight}</div><div class="stat-label">体重 kg</div></div>`;
}

/* ============================================================
 * 训练会话（核心）
 * ============================================================ */
let session = null;       // { dayKey, exercises: [{key,name,weight,targetSets,targetReps,unit,compound,sets:[{reps}] }] }
let sessionStart = null;  // 开始时间戳
let restTimerId = null;   // 组间休息计时器

function startSession(dayKey) {
  const wo = WORKOUTS[dayKey];
  if (!wo) return;

  session = {
    dayKey,
    exercises: wo.exercises.map((e) => {
      const ex = EXERCISES[e.key];
      const rec = recommendWeight(e.key, dayKey);
      return {
        key: e.key, name: ex.name,
        weight: rec.weight, unit: rec.unit, compound: rec.compound,
        targetSets: parseSetTarget(rec.targetSets), targetReps: rec.targetReps,
        targetRepsMax: parseRepMax(rec.targetReps),
        reason: rec.reason,
        note: e.note || "",
        sets: []
      };
    })
  };
  sessionStart = Date.now();

  $("#session-title").textContent = wo.title.split("·")[0].trim();
  $("#session-sub").textContent = wo.title;
  $("#session").classList.add("show");
  document.body.style.overflow = "hidden";

  renderSession();
}

function renderSession() {
  const total = session.exercises.length;
  const doneCount = session.exercises.filter((e) => e.sets.length >= e.targetSets).length;
  $("#session-progress").textContent = `${doneCount}/${total}`;
  $("#session-bar").style.width = `${Math.round((doneCount / total) * 100)}%`;

  $("#session-body").innerHTML = session.exercises.map((e, i) => {
    const done = e.sets.length >= e.targetSets;
    const active = e.sets.length > 0 && !done;
    const isBodyweight = e.unit.includes("自重");
    return `<div class="exercise-item${done ? " done" : active ? " active" : ""}" data-idx="${i}">
      <div class="ex-item-top">
        <span class="ex-item-order">${done ? "✓" : i + 1}</span>
        <span class="ex-item-name">${esc(e.name)}${e.note ? `<span style="font-size:12px;color:var(--muted);font-weight:400;">（${esc(e.note)}）</span>` : ""}</span>
        ${done ? `<span class="ex-item-check">✓</span>` : ""}
      </div>
      <div class="ex-item-meta">目标 ${e.targetSets} 组 × ${esc(e.targetReps)} · ${esc(e.unit)}</div>
      ${isBodyweight
        ? `<div class="rec-row"><span class="rec-weight">自重</span><span class="rec-reason">按次数/时长记录，不做配重推荐</span></div>`
        : `<div class="rec-row">
            <span class="rec-weight">${e.weight}</span><span class="rec-unit">kg · ${esc(e.unit)}</span>
            <span class="rec-reason">${esc(e.reason)}</span>
          </div>`}
      ${done
        ? `<div class="rec-row"><span class="rec-reason">已完成：${e.sets.map((s) => s.reps).join(" / ")} 次</span></div>`
        : `<button class="btn-ios primary" data-start="${i}">${active ? "继续" : "开始此动作"}</button>
           <button class="btn-ios small" data-skip="${i}" style="margin-left:36px;margin-top:6px;">跳过 · 标记完成</button>`}
      <div class="set-area" id="set-area-${i}" style="${active ? "" : "display:none;"}"></div>
    </div>`;
  }).join("");

  // 绑定开始按钮
  $$("#session-body [data-start]").forEach((b) => {
    b.addEventListener("click", () => openSetArea(parseInt(b.dataset.start)));
  });
  $$("#session-body [data-skip]").forEach((b) => {
    b.addEventListener("click", () => markExerciseDone(parseInt(b.dataset.skip)));
  });

  // 渲染正在进行的动作的组记录区
  session.exercises.forEach((e, i) => {
    if (e.sets.length > 0 && e.sets.length < e.targetSets) renderSetArea(i);
  });
}

function openSetArea(i) {
  // 关闭其他进行中的
  renderSetArea(i);
}

function renderSetArea(i) {
  const e = session.exercises[i];
  const area = $("#set-area-" + i);
  if (!area) return;
  area.style.display = "block";

  const cur = e.sets.length + 1;
  const isBodyweight = e.unit.includes("自重");
  const prefill = isBodyweight ? "" : e.targetRepsMax;

  area.innerHTML = `
    <div class="set-head"><b>第 ${cur} 组 / 共 ${e.targetSets} 组</b><span class="set-count">目标 ${esc(e.targetReps)}</span></div>
    <div class="set-row">
      <input type="number" class="set-reps" inputmode="numeric" min="0" placeholder="次数" value="${prefill}">
      <button class="btn-ios green set-done" data-idx="${i}">完成本组</button>
    </div>
    ${e.sets.length ? `<div class="set-log">${e.sets.map((s, k) => `<span class="chip-set">第${k + 1}组 ${s.reps} 次</span>`).join("")}</div>` : ""}
    <div class="rest-timer" id="rest-${i}" style="display:none;"><span class="rest-label">组间休息</span>02:00</div>`;

  area.querySelector(".set-done").addEventListener("click", () => completeSet(i));
}

function completeSet(i) {
  const e = session.exercises[i];
  const area = $("#set-area-" + i);
  const reps = parseInt(area.querySelector(".set-reps").value) || 0;
  if (reps <= 0) { alert("请填写本组完成次数"); return; }

  e.sets.push({ reps });

  if (e.sets.length >= e.targetSets) {
    markExerciseDone(i, true);
  } else {
    renderSession();
    // 重新聚焦当前动作并开始休息计时
    const idx = i;
    startRestTimer(idx, e.compound ? 120 : 75);
    // 滚动到当前动作
    const el = $(`#session-body [data-idx="${idx}"]`);
    if (el) el.scrollIntoView({ behavior: "smooth", block: "center" });
  }
}

function startRestTimer(i, seconds) {
  if (restTimerId) clearInterval(restTimerId);
  let left = seconds;
  const el = $("#rest-" + i);
  if (!el) return;
  el.style.display = "block";

  function tick() {
    const m = Math.floor(left / 60);
    const s = left % 60;
    el.innerHTML = `<span class="rest-label">组间休息</span>${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
    if (left <= 0) {
      clearInterval(restTimerId);
      restTimerId = null;
      el.innerHTML = `<span class="rest-label">休息结束</span>开始下一组 💪`;
    }
    left--;
  }
  tick();
  restTimerId = setInterval(tick, 1000);
}

function markExerciseDone(i, fromComplete) {
  const e = session.exercises[i];
  if (e.sets.length === 0) {
    // 跳过的动作，补一条空记录防止计入统计
    e.sets.push({ reps: 0 });
  }
  if (restTimerId) { clearInterval(restTimerId); restTimerId = null; }
  renderSession();

  // 滚动到下一个未完成动作
  const next = session.exercises.findIndex((x, k) => k > i && x.sets.length < x.targetSets);
  if (next >= 0) {
    const el = $(`#session-body [data-idx="${next}"]`);
    if (el) setTimeout(() => el.scrollIntoView({ behavior: "smooth", block: "center" }), 80);
  } else {
    // 全部完成
    if (session.exercises.every((x) => x.sets.length >= x.targetSets)) {
      $("#session-finish").classList.add("green");
      $("#session-finish").textContent = "全部完成 · 保存训练 ✓";
    }
  }
}

function finishSession() {
  if (!session) return;
  const doneExercises = session.exercises.filter((e) => e.sets.some((s) => s.reps > 0));
  if (!doneExercises.length) {
    if (!confirm("还没有完成任何动作，确定结束并丢弃本次训练？")) return;
  }

  // 检测 PR
  const prs = detectPRs(session.dayKey, doneExercises);

  const record = {
    date: todayISO(),
    day: session.dayKey,
    duration: Math.round((Date.now() - sessionStart) / 60000),
    exercises: doneExercises.map((e) => ({
      key: e.key, name: e.name, weight: e.weight,
      sets: e.sets.filter((s) => s.reps > 0).length,
      reps: e.sets.filter((s) => s.reps > 0).map((s) => s.reps).join("、"),
      targetReps: e.targetReps
    })),
    prs
  };

  Store.addWorkout(record);
  closeSession();

  // 刷新各页
  renderHome();
  renderStats();
  renderMine();

  if (prs.length) {
    setTimeout(() => alert("🎉 新个人纪录（PR）！\n" + prs.map((p) => `${p.name}：${p.weight} kg`).join("\n")), 150);
  } else if (doneExercises.length) {
    setTimeout(() => alert("✅ 训练已保存，干得漂亮！"), 150);
  }
}

function closeSession() {
  if (restTimerId) { clearInterval(restTimerId); restTimerId = null; }
  $("#session").classList.remove("show");
  document.body.style.overflow = "";
  session = null;
}

function detectPRs(day, exercises) {
  const log = Store.workoutLog();
  const prs = [];
  exercises.forEach((e) => {
    if (e.weight <= 0) return;
    let best = 0;
    log.forEach((rec) => {
      rec.exercises.forEach((x) => {
        if (x.key === e.key && x.weight > best) best = x.weight;
      });
    });
    if (e.weight > best) prs.push({ key: e.key, name: e.name, weight: e.weight });
  });
  return prs;
}

/* ---------- 计划页 ---------- */
let currentPlanDay = "pushA";

function renderPlan() {
  const dayKeys = ["pushA", "pullA", "pushB", "pullB"];
  $("#plan-seg").innerHTML = dayKeys.map((k) => {
    const info = WEEK_PLAN.find((w) => w.key === k);
    return `<button class="${k === currentPlanDay ? "active" : ""}" data-day="${k}">${info.name}</button>`;
  }).join("");

  $$("#plan-seg button").forEach((b) => {
    b.addEventListener("click", () => {
      currentPlanDay = b.dataset.day;
      renderPlan();
    });
  });

  renderPlanDetail(currentPlanDay);

  // 三阶段
  $("#stage-list").innerHTML = STAGES.map((s) => `
    <div class="stage">
      <span class="stage-time">${esc(s.time)}</span>
      <strong>${esc(s.phase)}</strong>
      <span>${esc(s.training)}</span>
      <span>体重：${esc(s.weight)}</span>
      <div class="stage-see">你会看到：${esc(s.see)}</div>
    </div>`).join("");

  // 进阶细节
  $("#progression-box").innerHTML =
    `<div class="ios-table-wrap" style="overflow-x:auto;">` +
    `<table class="ios-table"><thead><tr><th>周数</th><th>组数安排</th><th>努力程度</th><th>任务</th></tr></thead><tbody>` +
    PROGRESSION.map((p) => `<tr><td>${esc(p.week)}</td><td>${esc(p.volume)}</td><td>${esc(p.effort)}</td><td>${esc(p.task)}</td></tr>`).join("") +
    `</tbody></table></div>` +
    `<p class="head-sub" style="margin-top:10px;">${esc(PROGRESSION_RULE)}</p>` +
    `<p class="head-sub">${esc(START_WEIGHT_NOTE)}</p>`;
}

function renderPlanDetail(dayKey) {
  const wo = WORKOUTS[dayKey];
  const info = WEEK_PLAN.find((w) => w.key === dayKey);

  const rows = wo.exercises.map((e, i) => {
    const ex = EXERCISES[e.key];
    const rec = recommendWeight(e.key, dayKey);
    const isBodyweight = rec.unit.includes("自重");
    const wText = isBodyweight ? "自重" : `${rec.weight} kg`;
    return `<div class="week-item">
      <span class="day" style="min-width:26px;color:var(--muted);font-weight:600;">${i + 1}</span>
      <span class="wname">${esc(ex.name)}</span>
      <span class="wfocus">${esc(e.sets)}×${esc(e.reps)}<br><b style="color:var(--blue);font-weight:700;">${wText}</b></span>
    </div>`;
  }).join("");

  $("#plan-detail").innerHTML = `
    <div class="sub-title">${esc(wo.title)}</div>
    ${rows}
    <p class="head-sub" style="margin-top:12px;">重量为自动推荐（${esc(info.time)}），可点下方开始训练后逐项调整。</p>
    <button class="btn-ios primary big" data-start-day="${dayKey}" style="margin-top:14px;">开始此训练</button>`;

  $("#plan-detail [data-start-day]").addEventListener("click", (ev) => {
    startSession(ev.target.dataset.startDay);
  });
}

/* ---------- 动作库 ---------- */
let currentFilter = "全部";

function renderLibrary() {
  const groups = ["全部", "胸", "肩", "背", "手臂", "腹", "腿"];
  $("#lib-filter").innerHTML = groups.map((g) =>
    `<button class="filter-chip ${g === currentFilter ? "active" : ""}" data-g="${g}">${g}</button>`
  ).join("");

  $$("#lib-filter .filter-chip").forEach((c) => {
    c.addEventListener("click", () => {
      currentFilter = c.dataset.g;
      renderLibrary();
    });
  });

  const list = Object.entries(EXERCISES).filter(([k, ex]) =>
    currentFilter === "全部" || ex.group === currentFilter
  );

  $("#lib-list").innerHTML = list.map(([k, ex]) =>
    `<div class="ex-card" data-key="${k}">
      <div class="ex-top">
        <span class="ex-name">${esc(ex.name)}</span>
        <span class="ex-video-tag">▶ 视频</span>
      </div>
      <div class="ex-group">${esc(ex.group)} · ${esc(ex.video.up)}</div>
      <div class="target-tags">${ex.targets.map((t) => `<span class="ttag">${esc(t)}</span>`).join("")}</div>
    </div>`
  ).join("");

  $$("#lib-list .ex-card").forEach((c) => {
    c.addEventListener("click", () => openModal(c.dataset.key));
  });
}

function openModal(key) {
  const ex = EXERCISES[key];
  if (!ex) return;
  $("#modal").classList.add("show");
  const v = ex.video || {};

  $("#modal-panel").innerHTML = `
    <button class="modal-close" id="modal-close">✕</button>
    <h2>${esc(ex.name)}</h2>
    <div class="ex-group">目标：${esc(ex.targets.join("、"))}</div>
    <div class="target-tags">${ex.targets.map((t) => `<span class="ttag">${esc(t)}</span>`).join("")}</div>
    <h3>✅ 正确做法</h3>
    <div class="how-box"><p style="margin:0;">${esc(ex.how)}</p></div>
    <h3>⚠️ 常见错误与修正</h3>
    <div class="wrong-box"><p style="margin:0;">${esc(ex.wrong)}</p></div>
    <div class="video-box">
      <h3>🎬 教学视频（B站官方嵌入）</h3>
      <p class="video-hint">视频仍存 B站，本站不下载、不剪辑。</p>
      ${v.bv ? `
        <button class="btn-ios primary" id="video-play" style="width:100%;margin-top:8px;">▶ 播放：${esc(v.title || ex.name)}</button>
        <div id="video-frame-wrap"></div>
        <p class="video-source">来源：B站 · ${esc(v.up || "健身UP主")}｜<a href="https://www.bilibili.com/video/${esc(v.bv)}/" target="_blank" rel="noopener">打不开？点此看原视频</a></p>`
      : `<p class="video-hint">该动作暂无预置视频，请到 B站搜索「${esc(ex.name)} 教学」。</p>`}
    </div>`;

  $("#modal-close").addEventListener("click", closeModal);
  $("#modal-mask").addEventListener("click", closeModal);

  if (v.bv) {
    $("#video-play").addEventListener("click", () => {
      const wrap = $("#video-frame-wrap");
      wrap.innerHTML = `<iframe class="video-frame" src="https://player.bilibili.com/player.html?bvid=${v.bv}&page=1&high_quality=1&danmaku=0" scrolling="no" border="0" frameborder="no" framespacing="0" allowfullscreen="true"></iframe>`;
      $("#video-play").style.display = "none";
    });
  }
}

function closeModal() { $("#modal").classList.remove("show"); }

/* ---------- 统计 ---------- */
function renderStats() {
  const pc = $("#progress-chart");
  if (pc) { renderWeightChart(); renderCheckin(); renderMuscleMap(); renderProgressSelect(); renderProgressChart(); }
}

function renderWeightChart() {
  const data = Store.weightLog();
  const el = $("#weight-chart");
  if (data.length < 2) {
    el.innerHTML = `<div class="chart-empty">记录 2 次体重后出现趋势线</div>`;
    return;
  }
  const points = data.map((d) => ({ x: fmtDate(d.date).slice(5), y: d.weight }));
  el.innerHTML = drawLineChart(points, { unit: "kg", color: "var(--blue)" });
}

function renderCheckin() {
  const el = $("#checkin-chart");
  const log = Store.workoutLog();
  const doneDays = new Set(log.map((r) => r.date));
  const today = new Date();
  const monday = new Date(today);
  monday.setDate(today.getDate() - ((today.getDay() + 6) % 7));

  let cells = [];
  for (let w = 11; w >= 0; w--) {
    for (let d = 0; d < 7; d++) {
      const dt = new Date(monday);
      dt.setDate(monday.getDate() - w * 7 + d);
      const key = dt.toISOString().slice(0, 10);
      const future = dt > today;
      const done = doneDays.has(key);
      cells.push(`<div class="heat-cell ${future ? "" : done ? "done" : ""}" title="${key}${done ? "（已训练）" : ""}"></div>`);
    }
  }
  el.innerHTML = `<div class="heatmap">${cells.join("")}</div>`;
  el.innerHTML += `<p class="head-sub" style="margin-top:8px;">每格一天 · 绿色＝当天有训练</p>`;
}

/* ---------- 肌肉热力图 ---------- */
function renderMuscleMap() {
  const el = $("#muscle-map");
  const log = Store.workoutLog();
  const cutoff = daysAgoISO(6);
  const sets = {};
  Object.keys(MUSCLE_GROUPS).forEach((k) => (sets[k] = 0));
  log.forEach((rec) => {
    if (rec.date < cutoff) return;
    rec.exercises.forEach((e) => {
      const ex = EXERCISES[e.key];
      if (!ex || !ex.muscles) return;
      const s = e.sets || 0;
      ex.muscles.forEach((m) => { if (sets[m] !== undefined) sets[m] += s; });
    });
  });

  el.innerHTML = `
    <div class="muscle-fig"><div class="fig-title">正面</div>${drawBody("front", sets)}</div>
    <div class="muscle-fig"><div class="fig-title">背面</div>${drawBody("back", sets)}</div>
    <div class="muscle-legend">
      ${[0, 2, 4, 6, 8].map((n) => `<span class="legend-item"><span class="legend-dot" style="background:${colorForSets(n)};"></span>${n} 组</span>`).join("")}
    </div>`;
}

function colorForSets(s) {
  if (s <= 0) return "#E5E5EA";
  if (s <= 2) return "#B7E0C2";
  if (s <= 4) return "#6FCE8A";
  if (s <= 6) return "#34C759";
  return "#1E8E3E";
}

const MUSCLE_POS = {
  front: {
    frontDelt: { cx: 62, cy: 76, rx: 13, ry: 10 },
    midDelt: { cx: 52, cy: 88, rx: 8, ry: 9 },
    chest: { cx: 84, cy: 104, rx: 16, ry: 13 },
    biceps: { cx: 68, cy: 122, rx: 11, ry: 15 },
    abs: { cx: 110, cy: 142, rx: 18, ry: 22 },
    quads: { cx: 86, cy: 212, rx: 13, ry: 22 }
  },
  back: {
    rearDelt: { cx: 62, cy: 76, rx: 13, ry: 10 },
    back: { cx: 110, cy: 100, rx: 25, ry: 30 },
    triceps: { cx: 68, cy: 122, rx: 11, ry: 15 },
    glutes: { cx: 110, cy: 184, rx: 18, ry: 14 },
    hamstrings: { cx: 86, cy: 228, rx: 13, ry: 20 }
  }
};

function drawBody(view, sets) {
  const body = view === "front"
    ? `<circle cx="110" cy="34" r="18" fill="none" stroke="#C9C9CE" stroke-width="3"/>
       <rect x="84" y="56" width="52" height="112" rx="24" fill="none" stroke="#C9C9CE" stroke-width="3"/>
       <rect x="56" y="60" width="20" height="78" rx="10" fill="none" stroke="#C9C9CE" stroke-width="3"/>
       <rect x="144" y="60" width="20" height="78" rx="10" fill="none" stroke="#C9C9CE" stroke-width="3"/>
       <rect x="72" y="172" width="26" height="100" rx="13" fill="none" stroke="#C9C9CE" stroke-width="3"/>
       <rect x="122" y="172" width="26" height="100" rx="13" fill="none" stroke="#C9C9CE" stroke-width="3"/>`
    : `<circle cx="110" cy="34" r="18" fill="none" stroke="#C9C9CE" stroke-width="3"/>
       <rect x="84" y="56" width="52" height="112" rx="24" fill="none" stroke="#C9C9CE" stroke-width="3"/>
       <rect x="56" y="60" width="20" height="78" rx="10" fill="none" stroke="#C9C9CE" stroke-width="3"/>
       <rect x="144" y="60" width="20" height="78" rx="10" fill="none" stroke="#C9C9CE" stroke-width="3"/>
       <rect x="72" y="172" width="26" height="100" rx="13" fill="none" stroke="#C9C9CE" stroke-width="3"/>
       <rect x="122" y="172" width="26" height="100" rx="13" fill="none" stroke="#C9C9CE" stroke-width="3"/>`;

  const positions = MUSCLE_POS[view] || {};
  let muscles = "";
  Object.entries(positions).forEach(([mkey, p]) => {
    const name = MUSCLE_GROUPS[mkey].name;
    const c = colorForSets(sets[mkey] || 0);
    muscles += `<ellipse cx="${p.cx}" cy="${p.cy}" rx="${p.rx}" ry="${p.ry}" fill="${c}" stroke="#fff" stroke-width="1.5"><title>${name}：${sets[mkey] || 0} 组</title></ellipse>`;
    if (!["back", "abs", "glutes"].includes(mkey)) {
      muscles += `<ellipse cx="${220 - p.cx}" cy="${p.cy}" rx="${p.rx}" ry="${p.ry}" fill="${c}" stroke="#fff" stroke-width="1.5"><title>${name}：${sets[mkey] || 0} 组</title></ellipse>`;
    }
  });

  return `<svg viewBox="0 0 220 320" width="170" xmlns="http://www.w3.org/2000/svg">${body}${muscles}</svg>`;
}

function renderProgressSelect() {
  $("#progress-exercise").innerHTML = Object.entries(EXERCISES).map(([k, ex]) =>
    `<option value="${k}">${esc(ex.name)}</option>`
  ).join("");
  $("#progress-exercise").addEventListener("change", renderProgressChart);
}

function renderProgressChart() {
  const el = $("#progress-chart");
  const key = $("#progress-exercise").value;
  const ex = EXERCISES[key];
  const log = Store.workoutLog();
  const points = [];
  log.forEach((rec) => {
    rec.exercises.forEach((e) => {
      if (e.key === key && e.weight > 0) points.push({ x: rec.date, y: e.weight, reps: e.reps });
    });
  });
  points.sort((a, b) => a.x.localeCompare(b.x));

  if (points.length < 2) {
    el.innerHTML = `<div class="chart-empty">该动作重量记录不足 2 次，练两次后出现曲线</div>`;
    $("#progress-1rm").textContent = "";
    return;
  }
  el.innerHTML = drawLineChart(points.map((p) => ({ x: fmtDate(p.x).slice(5), y: p.y })), {
    unit: "kg", color: "var(--orange)"
  });

  const last = points[points.length - 1];
  const reps = parseRepMax(last.reps);
  if (reps > 0 && reps <= 15) {
    const e1rm = Math.round(last.y * (1 + reps / 30) * 10) / 10;
    $("#progress-1rm").textContent = `最近 ${last.y} kg × ${reps} 次，估算 1RM ≈ ${e1rm} kg（仅供参考）`;
  } else {
    $("#progress-1rm").textContent = "";
  }
}

/* SVG 折线图 */
function drawLineChart(points, { unit = "", color = "#007AFF" } = {}) {
  const w = 460, h = 190;
  const padL = 46, padR = 14, padT = 18, padB = 30;
  const ys = points.map((p) => p.y);
  const minY = Math.min(...ys), maxY = Math.max(...ys);
  const rangeY = maxY - minY || 1;
  const n = points.length;
  const xAt = (i) => padL + (w - padL - padR) * (n === 1 ? 0.5 : i / (n - 1));
  const yAt = (v) => padT + (h - padT - padB) * (1 - (v - minY) / rangeY);

  let path = "";
  points.forEach((p, i) => {
    path += (i === 0 ? "M" : " L") + xAt(i).toFixed(1) + " " + yAt(p.y).toFixed(1);
  });

  const dots = points.map((p, i) =>
    `<circle cx="${xAt(i).toFixed(1)}" cy="${yAt(p.y).toFixed(1)}" r="3.5" fill="${color}"><title>${esc(p.x)}：${p.y} ${unit}</title></circle>`
  ).join("");

  const yTicks = [maxY, minY].map((v) =>
    `<text x="${padL - 6}" y="${yAt(v).toFixed(1) + 4}" text-anchor="end" font-size="11" fill="#8E8E93">${v}</text>`
  ).join("");

  const xLabels = [points[0], points[n - 1]].map((p, i) =>
    `<text x="${xAt(i === 0 ? 0 : n - 1).toFixed(1)}" y="${h - 8}" text-anchor="${i === 0 ? "start" : "end"}" font-size="11" fill="#8E8E93">${esc(p.x)}</text>`
  ).join("");

  return `<svg viewBox="0 0 ${w} ${h}" width="100%" xmlns="http://www.w3.org/2000/svg">
    <line x1="${padL}" y1="${h - padB}" x2="${w - padR}" y2="${h - padB}" stroke="#E5E5EA" stroke-width="1"/>
    <line x1="${padL}" y1="${padT}" x2="${padL}" y2="${h - padB}" stroke="#E5E5EA" stroke-width="1"/>
    ${yTicks}
    <path d="${path}" fill="none" stroke="${color}" stroke-width="2.5" stroke-linejoin="round" stroke-linecap="round"/>
    ${dots}
    ${xLabels}
  </svg>`;
}

/* ---------- 我的页 ---------- */
function renderMine() {
  renderWater();
  renderEntries();
  renderBody();
  renderLogHistory();
  renderDiet();
}

/* ---------- 饮水：体重 × 35ml，训练日 +500ml，取整到 250 ---------- */
function waterGoal() {
  const ws = Store.weightLog();
  const weight = ws.length ? ws[ws.length - 1].weight : 63;
  const trained = Store.workoutLog().some((r) => r.date === new Date().toISOString().slice(0, 10));
  return { weight, goal: Math.ceil((weight * 35 + (trained ? 500 : 0)) / 250) * 250, trained };
}
function renderWater() {
  const { weight, goal, trained } = waterGoal();
  const drunk = Store.waterToday();
  const pct = Math.min(1, drunk / goal);
  $("#water-card").innerHTML = `
    <div class="row-line">
      <b>💧 今日饮水</b>
      <span class="val ${drunk >= goal ? "ok" : "blue"}">${drunk} / ${goal} ml</span>
    </div>
    <div class="muted small" style="margin:6px 0 10px;">按体重 ${weight}kg × 35ml${trained ? " + 训练日 500ml" : ""} 推荐，约 ${Math.round(goal / 250)} 杯</div>
    <div class="bar-track"><div class="bar-fill ${drunk >= goal ? "green" : ""}" style="width:${pct * 100}%"></div></div>
    <div class="btn-row" style="margin-top:12px;">
      <button class="btn-ios tinted" data-water="200">+200 ml</button>
      <button class="btn-ios tinted" data-water="300">+300 ml</button>
      <button class="btn-ios fill small-btn" data-water="-200">撤销</button>
    </div>`;
  $$("#water-card [data-water]").forEach((b) => b.addEventListener("click", () => {
    Store.addWater(parseInt(b.dataset.water));
    renderWater();
  }));
}

/* ---------- 我的页入口列表 ---------- */
function renderEntries() {
  $("#mine-entries").innerHTML = `
    <div class="entry-row" id="entry-toolbox">
      <div class="entry-ico">🧰</div>
      <div class="entry-txt"><b>工具箱</b><span>计算 · 计时 · 日历</span></div>
      <span class="chev">›</span>
    </div>
    <div class="entry-row" id="entry-settings">
      <div class="entry-ico">⚙️</div>
      <div class="entry-txt"><b>设置</b><span>休息 · 提示 · 外观</span></div>
      <span class="chev">›</span>
    </div>`;
  $("#entry-toolbox").addEventListener("click", () => openToolbox());
  $("#entry-settings").addEventListener("click", () => openToolbox("settings"));
}

/* ---------- 身体数据 ---------- */
function renderBody() {
  $("#body-card").innerHTML = `
    <div class="row-line">
      <div><b>年龄</b><div class="muted small">用于 TDEE 热量计算</div></div>
      <div class="inline-mini">${iosField("age-input", "", 'type="number" inputmode="numeric" min="10" max="90"')}<span class="unit">岁</span></div>
    </div>`;
  $("#age-input").value = Store.age();
  $("#age-input").addEventListener("change", () => {
    const v = parseInt($("#age-input").value);
    if (v >= 10 && v <= 90) Store.setAge(v);
  });
}

/* ============================================================
 * 工具箱（全屏二级页）：杠铃片 / 1RM / TDEE / 间隔计时器 / 进步曲线 / 日历 / 导出 / 设置
 * ============================================================ */
const TOOLS = [
  { id: "plates",   ico: "🏋️", name: "杠铃片计算器", sub: "快速算出每侧该挂哪些片" },
  { id: "rm",       ico: "💯", name: "1RM 计算器",    sub: "Epley 公式估算极限重量" },
  { id: "tdee",     ico: "🔥", name: "热量 TDEE 计算器", sub: "按体重与年龄算每日消耗" },
  { id: "interval", ico: "⏱️", name: "间隔计时器",     sub: "Tabata / HIIT 循环训练" },
  { id: "progress", ico: "📈", name: "动作进步曲线",   sub: "查看各动作重量增长" },
  { id: "calendar", ico: "📅", name: "训练日历",       sub: "本月打卡一览" },
  { id: "export",   ico: "📤", name: "导出数据",       sub: "CSV 备份发送给微信/QQ" },
  { id: "settings", ico: "⚙️", name: "设置",           sub: "休息 · 提示 · 外观" }
];

function openToolbox(toolId) {
  $("#toolbox").classList.add("open");
  document.body.classList.add("no-scroll");
  renderToolList();
  renderToolDetail(toolId || "");
}
function closeToolbox() {
  $("#toolbox").classList.remove("open");
  document.body.classList.remove("no-scroll");
  stopInterval();
}

function renderToolList() {
  $("#tool-list").innerHTML = TOOLS.map((t) => `
    <div class="entry-row" data-tool="${t.id}">
      <div class="entry-ico">${t.ico}</div>
      <div class="entry-txt"><b>${t.name}</b><span>${t.sub}</span></div>
      <span class="chev">›</span>
    </div>`).join("");
  $$("#tool-list [data-tool]").forEach((r) => r.addEventListener("click", () => renderToolDetail(r.dataset.tool)));
  $("#tool-detail").innerHTML = "";
}

function renderToolDetail(id) {
  if (!id) { $("#tool-detail").innerHTML = ""; return; }
  const wrap = (title, sub, html) => `
    <div class="detail-nav"><button class="nav-back" id="detail-back">‹ 工具箱</button></div>
    <div class="page-head"><h1 class="large-title">${title}</h1>${sub ? `<p class="head-sub">${sub}</p>` : ""}</div>
    <div class="group-card">${html}</div>`;
  const D = $("#tool-detail");

  if (id === "plates") {
    D.innerHTML = wrap("杠铃片计算", "含 20kg 奥杆", `
      <div class="muted small">目标总重量 kg</div>
      ${iosField("plate-in", "例如 60")}
      <div id="plate-out" style="margin-top:12px;"></div>
      <div class="sep"></div>
      <div class="muted small">片规格：25 / 20 / 15 / 10 / 5 / 2.5 / 1.25 kg</div>`);
    const calc = () => {
      const v = parseFloat($("#plate-in").value);
      const out = $("#plate-out");
      if (!v) { out.innerHTML = ""; return; }
      const side = (v - 20) / 2;
      if (v < 20) { out.innerHTML = `<div class="err">含 20kg 奥杆；目标重量需 ≥ 20kg 才能配片</div>`; return; }
      if (Math.abs(side / 1.25 - Math.round(side / 1.25)) > 1e-9) {
        out.innerHTML = `<div class="err">每侧需为 1.25 的倍数（当前每侧 ${round1(side)} kg）</div>`; return;
      }
      let left = side; const list = [];
      [25, 20, 15, 10, 5, 2.5, 1.25].forEach((w) => { while (left >= w - 1e-9) { list.push(w); left -= w; } });
      out.innerHTML = `<div class="muted small">每侧挂</div><div class="big-val">${list.join(" + ")} kg</div>`;
    };
    $("#plate-in").addEventListener("input", calc);
  }

  else if (id === "rm") {
    D.innerHTML = wrap("1RM 计算器", "Epley 公式估算", `
      <div class="btn-row" style="gap:8px;">
        <div style="flex:1"><div class="muted small">重量 kg</div>${iosField("rm-w", "重量")}</div>
        <div style="flex:1"><div class="muted small">次数</div>${iosField("rm-r", "次数", 'type="number" inputmode="numeric" min="1" max="20"')}</div>
      </div>
      <div id="rm-out" style="margin-top:12px;"></div>`);
    const calc = () => {
      const w = parseFloat($("#rm-w").value), r = parseInt($("#rm-r").value);
      const out = $("#rm-out");
      if (!w || !r || r < 1 || r > 20) { out.innerHTML = ""; return; }
      const rm = w * (1 + r / 30);
      const rows = [["85%", "力量"], ["75–85%", "肌力+肌肥大"], ["65–75%", "肌肥大（主要区间）"], ["50–65%", "热身/技术"]]
        .map(([pct, use]) => `<div class="row-line"><b>${pct}</b><span class="val">${round1(rm * parseFloat(pct.replace("%", "").split("–")[0]) / 100)} kg</span><span class="muted small" style="width:120px;text-align:right;">${use}</span></div>`).join('<div class="sep"></div>');
      out.innerHTML = `<div class="hl-box"><div class="muted small">估算 1RM</div><div class="big-val blue">${round1(rm)} kg</div></div>
        <div class="sep"></div>${rows}`;
    };
    $("#rm-w").addEventListener("input", calc);
    $("#rm-r").addEventListener("input", calc);
  }

  else if (id === "tdee") {
    const ws = Store.weightLog();
    const weight = ws.length ? ws[ws.length - 1].weight : 63;
    const age = Store.age();
    const bmr = 10 * weight + 6.25 * 183 - 5 * age + 5;
    const tdee = bmr * 1.55;
    const goal = Math.round(tdee + 300);
    const gap = goal - 2700;
    D.innerHTML = wrap("每日热量计算", "Mifflin-St Jeor · 男 · 183cm", `
      <div class="row-line"><b>年龄</b><span class="val">${age} 岁</span></div>
      <div class="muted small" style="margin-top:4px;">可在「我的 → 身体数据」修改</div>
      <div class="sep"></div>
      <div class="row-line"><span>基础代谢 BMR</span><span class="val">${Math.round(bmr)} kcal</span></div>
      <div class="sep"></div>
      <div class="row-line"><span>日常消耗 TDEE（中等活动）</span><span class="val">${Math.round(tdee)} kcal</span></div>
      <div class="sep"></div>
      <div class="row-line"><div><b>增肌目标 +300 kcal</b><div class="muted small">计划目标 2700 kcal ${gap > 0 ? `偏低，可再加 ${gap} kcal` : "合理"}</div></div><span class="val blue big-val-sm">${goal} kcal</span></div>`);
  }

  else if (id === "interval") {
    D.innerHTML = wrap("间隔计时器", "Tabata / HIIT / 自定义循环", `
      <div class="seg" id="iv-seg">
        <button data-iv="tabata" class="active">Tabata</button>
        <button data-iv="hiit">HIIT</button>
        <button data-iv="custom">自定义</button>
      </div>
      <div id="iv-custom" style="margin-top:12px;display:none;">
        <div class="btn-row" style="gap:8px;">
          <div style="flex:1"><div class="muted small">工作秒</div>${iosField("iv-work", "40", 'type="number" inputmode="numeric"')}</div>
          <div style="flex:1"><div class="muted small">休息秒</div>${iosField("iv-rest", "20", 'type="number" inputmode="numeric"')}</div>
          <div style="flex:1"><div class="muted small">组数</div>${iosField("iv-rounds", "8", 'type="number" inputmode="numeric"')}</div>
        </div>
      </div>
      <div id="iv-hint" class="muted small" style="margin-top:12px;text-align:center;"></div>
      <div id="iv-display-wrap" style="display:none;margin-top:14px;text-align:center;">
        <div class="ring-wrap">
          <svg class="ring" viewBox="0 0 200 200">
            <circle cx="100" cy="100" r="88" class="ring-bg"/>
            <circle cx="100" cy="100" r="88" class="ring-fg" id="ring-fg"/>
          </svg>
          <div class="ring-center">
            <div id="iv-phase" class="iv-phase">训练</div>
            <div id="iv-left" class="iv-left">20</div>
            <div id="iv-round" class="muted small">第 1 / 8 组</div>
          </div>
        </div>
      </div>
      <button class="btn-ios primary big" id="iv-start" style="margin-top:14px;">开始间隔训练</button>
      <div class="btn-row" id="iv-ctrls" style="display:none;margin-top:10px;">
        <button class="btn-ios fill" id="iv-pause">暂停</button>
        <button class="btn-ios danger" id="iv-stop">停止</button>
      </div>`);
    initInterval();
  }

  else if (id === "progress") {
    const logs = Store.workoutLog();
    const allKeys = [];
    logs.forEach((r) => (r.exercises || []).forEach((e) => { if (!allKeys.some((k) => k.key === e.key)) allKeys.push({ key: e.key, name: e.name }); }));
    if (!allKeys.length) {
      D.innerHTML = wrap("进步曲线", "暂无训练数据", `
        <b>📈 还没有可绘制的数据</b>
        <div class="muted small" style="margin-top:6px;line-height:1.6;">完成第一次训练并记录重量后，这里会自动生成每个动作的重量进步曲线。<br><br>去「今天」页点「开始训练」试试吧！</div>`);
    } else {
      // 自动选中最近练的动作，进来直接出图
      const first = allKeys[0];
      const pts = progressPoints(first.key);
      D.innerHTML = wrap("进步曲线", `共 ${allKeys.length} 个动作 · 点击切换`, `
        <div class="chip-grid" id="prog-chips">${allKeys.map((k, i) =>
          `<button class="chip ${i === 0 ? "active" : ""}" data-k="${k.key}" data-n="${k.name}">${k.name}</button>`).join("")}</div>
        <div style="height:14px"></div>
        <div id="prog-chart"></div>`);
      const draw = (key, name) => $("#prog-chart").innerHTML =
        `<b>${name}</b><div style="height:8px"></div>` + lineChart(ptsFor(key), { height: 150 });
      draw(first.key, first.name);
      $$("#prog-chips .chip").forEach((c) => c.addEventListener("click", () => {
        $$("#prog-chips .chip").forEach((x) => x.classList.remove("active"));
        c.classList.add("active");
        draw(c.dataset.k, c.dataset.n);
      }));
    }
  }

  else if (id === "calendar") {
    D.innerHTML = wrap("训练日历", "绿色 = 已训练", `<div id="cal-wrap"></div>`);
    $("#cal-wrap").innerHTML = trainCalendar();
  }

  else if (id === "export") {
    D.innerHTML = wrap("导出数据", "", `
      <div class="muted small" style="line-height:1.6;">以 CSV 文本导出全部训练记录，可复制或分享给自己保存。</div>
      <button class="btn-ios primary big" id="exp-btn" style="margin-top:12px;">导出并分享</button>
      <textarea class="csv-out" id="exp-out" readonly style="display:none;"></textarea>`);
    $("#exp-btn").addEventListener("click", () => {
      const head = "日期,训练日,动作,部位,重量kg,次数,用时秒\n";
      const body = Store.workoutLog().flatMap((r) => (r.exercises || []).map((e) =>
        [r.date, r.day, e.name, e.group || "", e.weight || 0, e.reps || "", e.seconds || ""].join(","))).join("\n");
      const csv = head + body;
      const ta = $("#exp-out");
      ta.style.display = "block"; ta.value = csv;
      if (navigator.share) {
        navigator.share({ title: "MyGymPlane 训练记录.csv", text: csv }).catch(() => {});
      } else if (navigator.clipboard) {
        navigator.clipboard.writeText(csv).then(() => alert("已复制到剪贴板"));
      }
    });
  }

  else if (id === "settings") {
    const p = Store.prefs();
    const seg = (name, opts, cur) => `<div class="seg" data-seg="${name}">
      ${opts.map(([v, t]) => `<button data-v="${v}" class="${String(cur) === String(v) ? "active" : ""}">${t}</button>`).join("")}</div>`;
    D.innerHTML = wrap("设置", "", `
      <b>组间休息</b>
      <div class="muted small" style="margin:10px 0 6px;">复合动作（深蹲 / 卧推 / 硬拉）</div>
      ${seg("restCompound", [[90, "90秒"], [120, "2分"], [150, "2分半"], [180, "3分"]], 120)}
      <div class="muted small" style="margin:14px 0 6px;">孤立动作（弯举 / 侧平举等）</div>
      ${seg("restIsolation", [[45, "45秒"], [60, "1分"], [75, "1分15"], [90, "1分半"]], 75)}
      <div class="sep"></div>
      <b>提示</b>
      <div class="row-line" style="margin-top:10px;"><div><b>倒计时震动</b><div class="muted small">休息最后 3 秒短震</div></div>
        <label class="switch"><input type="checkbox" id="sw-vibrate" ${p.vibrate ? "checked" : ""}><span></span></label></div>
      <div class="sep"></div>
      <div class="row-line"><div><b>阶段切换提示音</b><div class="muted small">训练 / 休息切换时提示</div></div>
        <label class="switch"><input type="checkbox" id="sw-sound" ${p.sound ? "checked" : ""}><span></span></label></div>
      <div class="sep"></div>
      <b>外观</b>
      <div style="height:10px"></div>
      ${seg("theme", [[0, "跟随系统"], [1, "浅色"], [2, "深色"]], p.theme)}
      <div class="muted small" style="margin-top:8px;">切换后立即生效，无需重启</div>`);

    $$("#tool-detail [data-seg]").forEach((g) => g.querySelectorAll("button").forEach((b) =>
      b.addEventListener("click", () => {
        g.querySelectorAll("button").forEach((x) => x.classList.remove("active"));
        b.classList.add("active");
        const np = Store.prefs(); np[g.dataset.seg] = b.dataset.v; Store.setPrefs(np);
        if (g.dataset.seg === "theme") applyTheme();
      })));
    $("#sw-vibrate").addEventListener("change", (e) => { const np = Store.prefs(); np.vibrate = e.target.checked; Store.setPrefs(np); });
    $("#sw-sound").addEventListener("change", (e) => { const np = Store.prefs(); np.sound = e.target.checked; Store.setPrefs(np); });
  }

  $("#detail-back").addEventListener("click", () => { stopInterval(); renderToolList(); });
}

/* ---------- 间隔计时器状态 ---------- */
let iv = { phase: 0, left: 0, total: 1, round: 1, rounds: 8, work: 20, rest: 10, running: false, timer: null };

function initInterval() {
  const segEl = $("#iv-seg");
  const setPreset = (k) => {
    if (k === "tabata") { iv.work = 20; iv.rest = 10; iv.rounds = 8; }
    else if (k === "hiit") { iv.work = 40; iv.rest = 20; iv.rounds = 8; }
    $("#iv-custom").style.display = k === "custom" ? "block" : "none";
    if (k === "custom") { $("#iv-work").value = iv.work; $("#iv-rest").value = iv.rest; $("#iv-rounds").value = iv.rounds; }
    $("#iv-hint").textContent = `工作 ${iv.work}s · 休息 ${iv.rest}s · 共 ${iv.rounds} 组`;
  };
  segEl.querySelectorAll("button").forEach((b) => b.addEventListener("click", () => {
    segEl.querySelectorAll("button").forEach((x) => x.classList.remove("active"));
    b.classList.add("active"); setPreset(b.dataset.iv);
  }));
  setPreset("tabata");

  $("#iv-start").addEventListener("click", () => {
    if ($("#iv-custom").style.display !== "none") {
      iv.work = parseInt($("#iv-work").value) || 40;
      iv.rest = parseInt($("#iv-rest").value) || 20;
      iv.rounds = parseInt($("#iv-rounds").value) || 8;
    }
    iv.round = 1; iv.phase = 1; iv.left = iv.work; iv.total = iv.work; iv.running = true;
    $("#iv-display-wrap").style.display = "block";
    $("#iv-ctrls").style.display = "flex";
    $("#iv-start").style.display = "none";
    tickRender();
    clearInterval(iv.timer);
    iv.timer = setInterval(() => {
      if (!iv.running) return;
      iv.left--;
      if (iv.left <= 3 && iv.left > 0) vibrate(30);
      if (iv.left <= 0) {
        if (iv.phase === 1) {
          if (Store.prefs().sound) beep(1000);
          vibrate(80);
          iv.round++;
          if (iv.round > iv.rounds) { stopInterval(); return; }
          iv.phase = 2; iv.left = iv.rest; iv.total = iv.rest;
        } else {
          if (Store.prefs().sound) beep(760);
          vibrate(60);
          iv.phase = 1; iv.left = iv.work; iv.total = iv.work;
        }
      }
      tickRender();
    }, 1000);
  });
  const pauseBtn = $("#iv-pause");
  if (pauseBtn) pauseBtn.addEventListener("click", () => {
    iv.running = !iv.running;
    pauseBtn.textContent = iv.running ? "暂停" : "继续";
    pauseBtn.classList.toggle("primary", iv.running);
  });
  const stopBtn = $("#iv-stop");
  if (stopBtn) stopBtn.addEventListener("click", stopInterval);
}

function tickRender() {
  const left = $("#iv-left"); if (!left) return;
  left.textContent = iv.left;
  $("#iv-phase").textContent = iv.phase === 1 ? "训练" : "休息";
  $("#iv-phase").className = "iv-phase " + (iv.phase === 1 ? "work" : "rest");
  $("#iv-round").textContent = `第 ${iv.round} / ${iv.rounds} 组`;
  const circ = 2 * Math.PI * 88;
  const fg = $("#ring-fg");
  fg.style.strokeDasharray = circ;
  fg.style.strokeDashoffset = circ * (1 - (iv.total ? iv.left / iv.total : 0));
  fg.classList.toggle("rest", iv.phase === 2);
}
function stopInterval() {
  iv.running = false;
  clearInterval(iv.timer); iv.timer = null;
  const s = $("#iv-start"); if (s) s.style.display = "block";
  const c = $("#iv-ctrls"); if (c) c.style.display = "none";
  const w = $("#iv-display-wrap"); if (w) w.style.display = "none";
}
iv.rounds = 8; iv.work = 20; iv.rest = 10;
iv.total = 1; iv.round = 1; iv.phase = 1;

/* ---------- 训练日历 ---------- */
function trainCalendar() {
  const done = new Set(Store.workoutLog().map((r) => r.date));
  const now = new Date();
  const y = now.getFullYear(), m = now.getMonth();
  const first = new Date(y, m, 1);
  const offset = (first.getDay() + 6) % 7; // 周一为一周开始
  const days = new Date(y, m + 1, 0).getDate();
  const todayStr = new Date().toISOString().slice(0, 10);
  let html = `<div style="font-weight:600;font-size:16px;margin-bottom:8px;">${y} 年 ${m + 1} 月</div>`;
  html += `<div class="cal-row">${["一", "二", "三", "四", "五", "六", "日"].map((d) => `<span class="cal-w">${d}</span>`).join("")}</div>`;
  const rows = Math.ceil((offset + days) / 7);
  for (let r = 0; r < rows; r++) {
    html += `<div class="cal-row">`;
    for (let c = 0; c < 7; c++) {
      const idx = r * 7 + c - offset + 1;
      if (idx < 1 || idx > days) { html += `<span class="cal-cell"></span>`; continue; }
      const ds = `${y}-${String(m + 1).padStart(2, "0")}-${String(idx).padStart(2, "0")}`;
      const cls = done.has(ds) ? "done" : (ds === todayStr ? "today" : "");
      html += `<span class="cal-cell"><span class="cal-day ${cls}">${idx}</span></span>`;
    }
    html += `</div>`;
  }
  return html;
}

/* ---------- 进步曲线数据 ---------- */
function progressPoints(key) {
  return Store.workoutLog().slice().reverse().map((r) => {
    const ex = (r.exercises || []).find((e) => e.key === key);
    return ex && ex.weight > 0 ? { date: r.date, weight: ex.weight } : null;
  }).filter(Boolean);
}
function ptsFor(key) {
  const p = progressPoints(key);
  return p.length ? p.map((x) => ({ label: x.date.slice(5), value: x.weight })) : [];
}

/* ---------- 简易 SVG 折线图（带渐变填充） ---------- */
function lineChart(points, opt = {}) {
  if (!points || points.length < 2) return `<div class="empty">数据不足（至少 2 次记录）</div>`;
  const W = 320, H = opt.height || 140, pad = 26, grad = "g" + Math.random().toString(36).slice(2, 7);
  const vals = points.map((p) => p.value);
  const minV = Math.min(...vals) - 0.5, maxV = Math.max(...vals) + 0.5;
  const xs = points.map((_, i) => pad + (W - pad * 2) * i / (points.length - 1));
  const ys = points.map((p) => H - pad - (H - pad * 1.6) * (p.value - minV) / (maxV - minV));
  const path = xs.map((x, i) => `${i ? "L" : "M"}${x.toFixed(1)},${ys[i].toFixed(1)}`).join(" ");
  const area = `${path} L${xs[xs.length - 1].toFixed(1)},${H - 6} L${xs[0].toFixed(1)},${H - 6} Z`;
  const dots = xs.map((x, i) => `<circle cx="${x.toFixed(1)}" cy="${ys[i].toFixed(1)}" r="3.6" fill="var(--card)" stroke="var(--blue)" stroke-width="2.4"/>`).join("");
  return `<svg class="line-chart" viewBox="0 0 ${W} ${H}" preserveAspectRatio="none">
    <defs><linearGradient id="${grad}" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0%" stop-color="var(--blue)" stop-opacity=".26"/>
      <stop offset="100%" stop-color="var(--blue)" stop-opacity="0"/>
    </linearGradient></defs>
    <path d="${area}" fill="url(#${grad})"/>
    <path d="${path}" fill="none" stroke="var(--blue)" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"/>
    ${dots}
    <text x="${pad}" y="${H - 4}" class="chart-lbl">${points[0].label}</text>
    <text x="${W - pad}" y="${H - 4}" class="chart-lbl" text-anchor="end">${points[points.length - 1].label}</text>
  </svg>`;
}

function round1(v) { return Math.round(v * 10) / 10; }

/* ---------- 主题 ---------- */
function applyTheme() {
  const t = String(Store.prefs().theme);
  const dark = t === "2" || (t === "0" && window.matchMedia("(prefers-color-scheme: dark)").matches);
  document.documentElement.classList.toggle("dark", dark);
  const meta = document.querySelector('meta[name="theme-color"]');
  if (meta) meta.setAttribute("content", dark ? "#000000" : "#F2F2F7");
}

function renderLogHistory() {
  const list = Store.workoutLog();
  const dayLabel = { pushA: "推 A", pullA: "拉 A", pushB: "推 B", pullB: "拉 B" };
  if (!list.length) {
    $("#log-history").innerHTML = `<div class="empty">还没有训练记录，去「今天」页开始第一次训练吧。</div>`;
    return;
  }
  $("#log-history").innerHTML = list.map((rec) => {
    const prSet = new Set((rec.prs || []).map((p) => p.key));
    const rows = rec.exercises.map((e) => {
      const prBadge = prSet.has(e.key) ? `<span class="pr-badge">PR</span>` : "";
      return `${esc(e.name)}${prBadge}：${e.weight > 0 ? e.weight + " kg · " : ""}${e.sets} 组${e.reps ? " × " + esc(e.reps) + " 次" : ""}`;
    }).join("<br>");
    return `<div class="log-item">
      <div class="log-head">
        <span class="log-day">${dayLabel[rec.day] || esc(rec.day)}</span>
        <span class="log-date">${fmtDate(rec.date)}</span>
        ${rec.duration ? `<span class="log-date">${rec.duration} 分钟</span>` : ""}
        <button class="btn-del" data-del="${rec.id}">删除</button>
      </div>
      <div class="log-rows">${rows}</div>
    </div>`;
  }).join("");

  $$("#log-history [data-del]").forEach((b) => {
    b.addEventListener("click", () => {
      if (confirm("删除这条训练记录？")) {
        Store.deleteWorkout(parseInt(b.dataset.del));
        renderMine();
        renderStats();
        renderHome();
      }
    });
  });
}

function renderDiet() {
  const nutRows = `
    <tr><td>总能量</td><td>约 ${NUTRITION.kcal} kcal/天</td><td>训练/休息日先一致</td></tr>
    <tr><td>蛋白质</td><td>${esc(NUTRITION.protein)}</td><td>先到约 110 g</td></tr>
    <tr><td>脂肪</td><td>${esc(NUTRITION.fat)}</td><td>奶蛋鱼坚果油</td></tr>
    <tr><td>碳水</td><td>${esc(NUTRITION.carbs)}</td><td>米饭面燕麦薯类</td></tr>
    <tr><td>餐次</td><td>${esc(NUTRITION.meals)}</td><td>每 3–4 小时一次</td></tr>`;
  $("#nutrition-table").innerHTML =
    `<div style="overflow-x:auto;"><table class="ios-table"><thead><tr><th>项目</th><th>起步目标</th><th>说明</th></tr></thead><tbody>${nutRows}</tbody></table></div>` +
    `<p class="head-sub" style="margin-top:10px;">${esc(NUTRITION.tips)}</p>`;

  $("#supplement-list").innerHTML = SUPPLEMENTS.map((s) =>
    `<div class="supp-item"><strong>${esc(s.name)}</strong><p><b>用法：</b>${esc(s.use)}</p><p><b>提醒：</b>${esc(s.note)}</p></div>`
  ).join("");
}

/* ---------- 工具页（计时器/杠铃片/1RM） ---------- */
let timerTotal = 120;
let timerLeft = 120;
let toolTimerId = null;

function initTools() {
  const display = $("#timer-display");
  if (!display) return;  // 旧版计时器已迁入工具箱，主页面无此节点时跳过

  function updateDisplay() {
    const m = Math.floor(timerLeft / 60);
    const s = timerLeft % 60;
    display.textContent = String(m).padStart(2, "0") + ":" + String(s).padStart(2, "0");
  }

  function stopTimer() {
    if (toolTimerId) { clearInterval(toolTimerId); toolTimerId = null; }
    display.classList.remove("running");
  }

  $$("[data-min]").forEach((b) => {
    b.addEventListener("click", () => {
      stopTimer();
      timerTotal = Math.round(parseFloat(b.dataset.min) * 60);
      timerLeft = timerTotal;
      updateDisplay();
    });
  });

  $("#timer-start").addEventListener("click", () => {
    if (toolTimerId) return;
    if (timerLeft <= 0) timerLeft = timerTotal;
    display.classList.add("running");
    toolTimerId = setInterval(() => {
      timerLeft--;
      updateDisplay();
      if (timerLeft <= 0) {
        stopTimer();
        timerLeft = timerTotal;
        updateDisplay();
        alert("⏰ 休息结束，开始下一组！");
      }
    }, 1000);
  });

  $("#timer-pause").addEventListener("click", stopTimer);
  $("#timer-reset").addEventListener("click", () => {
    stopTimer();
    timerLeft = timerTotal;
    updateDisplay();
  });

  updateDisplay();

  // 杠铃片
  $("#plate-calc").addEventListener("click", () => {
    const total = parseFloat($("#plate-input").value);
    const result = $("#plate-result");
    if (!total || total < BARBELL_WEIGHT) {
      result.innerHTML = `<p class="head-sub">请输入不小于 ${BARBELL_WEIGHT} kg 的目标总重。</p>`;
      return;
    }
    if (total % 2.5 !== 0) {
      result.innerHTML = `<p class="head-sub">重量请按 2.5 kg 递增。</p>`;
      return;
    }
    const perSide = (total - BARBELL_WEIGHT) / 2;
    let remaining = perSide;
    const picks = [];
    PLATES.forEach((p) => {
      while (remaining + 1e-9 >= p.weight) {
        picks.push(p);
        remaining = Math.round((remaining - p.weight) * 100) / 100;
      }
    });
    if (remaining > 0.01) {
      result.innerHTML = `<p class="head-sub">无法用标准片精确配出，剩余每侧 ${remaining} kg。</p>`;
      return;
    }
    result.innerHTML = `
      <div class="plate-side-title">每侧 ${perSide} kg（${picks.length} 片）</div>
      <div class="plate-stack">${picks.length ? picks.map((p) => `<span class="plate-disk ${p.color === "#FFFFFF" ? "light" : ""}" style="background:${p.color};">${p.weight}</span>`).join("") : `<span class="head-sub">无需加片，仅空杆</span>`}</div>
      <p class="head-sub" style="margin-top:8px;">杆 ${BARBELL_WEIGHT} kg + 两侧 ${perSide * 2} kg = ${total} kg</p>`;
  });

  // 1RM
  $("#rm-calc").addEventListener("click", () => {
    const w = parseFloat($("#rm-weight").value);
    const r = parseInt($("#rm-reps").value);
    const out = $("#rm-result");
    if (!w || !r || r < 1 || r > 20) {
      out.textContent = "输入有效重量和次数";
      return;
    }
    const epley = w * (1 + r / 30);
    out.textContent = "≈ " + Math.round(epley * 10) / 10 + " kg";
  });
}

/* ---------- 初始化 ---------- */
function init() {
  applyTheme();
  window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", applyTheme);
  initNav();
  renderHome();
  renderPlan();
  renderLibrary();
  renderStats();
  renderMine();
  initTools();

  // 工具箱开关
  $("#toolbox-back").addEventListener("click", closeToolbox);

  // 首页开始训练
  $("#btn-start").addEventListener("click", () => {
    const todayPlan = WEEK_PLAN[planIndexForToday()];
    if (todayPlan.key !== "rest") startSession(todayPlan.key);
  });
  $("#btn-start-rest").addEventListener("click", () => {
    // 休息日想自由练 → 去计划页选训练日
    $$("#tabbar .tab-item").forEach((b) => b.classList.remove("active"));
    $$("#tabbar .tab-item").forEach((b) => { if (b.dataset.page === "plan") b.classList.add("active"); });
    $$(".page").forEach((p) => p.classList.remove("active"));
    $("#page-plan").classList.add("active");
    window.scrollTo({ top: 0 });
  });

  // 训练会话
  $("#session-close").addEventListener("click", () => {
    if (session && session.exercises.some((e) => e.sets.length > 0)) {
      if (!confirm("训练尚未完成，确定退出？已完成的动作不会保存。")) return;
    }
    closeSession();
  });
  $("#session-finish").addEventListener("click", finishSession);

  // 记录体重
  $("#weight-add").addEventListener("click", () => {
    const v = parseFloat($("#weight-input").value);
    if (!v || v < 30 || v > 200) return alert("请输入有效体重（30–200 kg）");
    Store.addWeight(v);
    $("#weight-input").value = "";
    renderWeightChart();
    renderHome();
  });

  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
      closeModal();
      if (session) closeSession();
    }
  });
}

document.addEventListener("DOMContentLoaded", init);
