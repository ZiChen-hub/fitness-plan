/* 薄肌增肌 · Service Worker
 * 核心资源缓存优先（离线也能看计划和动作库），
 * 页面网络优先、失败回退缓存；B站播放器不缓存。 */
const CACHE = "mygymplane-v622";
const CORE = [
  "./", "./index.html", "./style.css", "./data.js", "./app.js",
  "./manifest.json", "./icon-192.png", "./icon-512.png"
];

self.addEventListener("install", (e) => {
  e.waitUntil(
    caches.open(CACHE).then((c) => c.addAll(CORE)).then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", (e) => {
  e.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k)))
    ).then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (e) => {
  const url = new URL(e.request.url);
  // B站播放器与跨域请求：直接走网络，不缓存
  if (url.origin !== self.location.origin) return;

  // 核心资源：缓存优先，后台更新
  e.respondWith(
    caches.match(e.request).then((cached) => {
      const fetching = fetch(e.request).then((resp) => {
        if (resp && resp.ok) {
          const copy = resp.clone();
          caches.open(CACHE).then((c) => c.put(e.request, copy));
        }
        return resp;
      }).catch(() => cached);
      return cached || fetching;
    })
  );
});
