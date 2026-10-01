package com.example.snake;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final int FILE_CHOOSER_REQUEST = 1001;
    private ValueCallback<Uri[]> filePathCallback;

    private static final String HTML_CONTENT = """
<!DOCTYPE html>
<html lang="ru">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
<meta name="theme-color" content="#0f0f0f">
<title>Змейка</title>
<style>
  * {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
    -webkit-tap-highlight-color: transparent;
    user-select: none;
    -webkit-user-select: none;
    touch-action: none;
  }

  html, body {
    width: 100%;
    height: 100%;
    overflow: hidden;
    background: #0f0f0f;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
    color: #e8e8e8;
    overscroll-behavior: none;
    position: fixed;
  }

  #app {
    position: fixed;
    inset: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: space-between;
    padding: env(safe-area-inset-top) env(safe-area-inset-right) env(safe-area-inset-bottom) env(safe-area-inset-left);
  }

  header {
    width: 100%;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 14px 20px;
    font-size: 14px;
    letter-spacing: 0.05em;
    font-variant-numeric: tabular-nums;
    color: #8a8a8a;
    flex-shrink: 0;
  }

  header .score b {
    color: #e8e8e8;
    font-weight: 600;
  }

  header .best {
    color: #5a5a5a;
  }

  #boardWrap {
    position: relative;
    flex: 1;
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 0;
    padding: 0 12px;
  }

  canvas {
    display: block;
    background: #161616;
    border-radius: 10px;
    box-shadow: 0 0 0 1px #232323;
    touch-action: none;
  }

  #overlay {
    position: absolute;
    inset: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 16px;
    background: rgba(15, 15, 15, 0.85);
    backdrop-filter: blur(6px);
    -webkit-backdrop-filter: blur(6px);
    border-radius: 10px;
    text-align: center;
    padding: 20px;
    opacity: 0;
    pointer-events: none;
    transition: opacity 0.2s ease;
  }

  #overlay.show {
    opacity: 1;
    pointer-events: auto;
  }

  #overlay h1 {
    font-size: 22px;
    font-weight: 600;
    letter-spacing: 0.02em;
  }

  #overlay p {
    font-size: 13px;
    color: #8a8a8a;
    line-height: 1.6;
    max-width: 240px;
  }

  #overlay button {
    margin-top: 4px;
    padding: 12px 32px;
    font-size: 15px;
    font-weight: 600;
    font-family: inherit;
    color: #0f0f0f;
    background: #e8e8e8;
    border: none;
    border-radius: 100px;
    cursor: pointer;
    transition: transform 0.1s ease, background 0.15s ease;
  }

  #overlay button:active {
    transform: scale(0.96);
    background: #cfcfcf;
  }

  footer {
    width: 100%;
    display: flex;
    justify-content: center;
    gap: 10px;
    padding: 14px 20px calc(14px + env(safe-area-inset-bottom));
    flex-shrink: 0;
  }

  .pad {
    display: grid;
    grid-template-columns: repeat(3, 56px);
    grid-template-rows: repeat(2, 56px);
    gap: 8px;
  }

  .pad button {
    width: 56px;
    height: 56px;
    border-radius: 14px;
    border: 1px solid #2a2a2a;
    background: #1a1a1a;
    color: #cfcfcf;
    font-size: 20px;
    font-family: inherit;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: background 0.08s ease, transform 0.08s ease;
  }

  .pad button:active {
    background: #2a2a2a;
    transform: scale(0.95);
  }

  .pad button:disabled {
    opacity: 0.35;
  }

  .pad .up    { grid-column: 2; grid-row: 1; }
  .pad .left  { grid-column: 1; grid-row: 2; }
  .pad .down  { grid-column: 2; grid-row: 2; }
  .pad .right { grid-column: 3; grid-row: 2; }

  @media (hover: hover) and (pointer: fine) {
    footer { display: none; }
  }

  @media (max-height: 560px) {
    .pad { grid-template-columns: repeat(3, 48px); grid-template-rows: repeat(2, 48px); }
    .pad button { width: 48px; height: 48px; font-size: 18px; }
    header { padding: 8px 16px; }
    footer { padding: 8px 16px calc(8px + env(safe-area-inset-bottom)); }
  }
</style>
</head>
<body>
<div id="app">
  <header>
    <div class="score">Счёт: <b id="score">0</b></div>
    <div class="best">Рекорд: <span id="best">0</span></div>
  </header>

  <div id="boardWrap">
    <canvas id="game"></canvas>
    <div id="overlay" class="show">
      <h1 id="ovTitle">Змейка</h1>
      <p id="ovText">Свайп или стрелки — управление.<br>Собирай точки, не врезайся в себя.</p>
      <button id="ovBtn">Играть</button>
    </div>
  </div>

  <footer>
    <div class="pad">
      <button class="up"    data-dir="up"    aria-label="Вверх">▲</button>
      <button class="left"  data-dir="left"  aria-label="Влево">◀</button>
      <button class="down"  data-dir="down"  aria-label="Вниз">▼</button>
      <button class="right" data-dir="right" aria-label="Вправо">▶</button>
    </div>
  </footer>
</div>

<script>
(function () {
  'use strict';

  // ---------- Константы ----------
  const COLS = 20;
  const ROWS = 20;
  const BASE_STEP_MS = 150;     // начальная скорость
  const MIN_STEP_MS = 70;       // максимальная скорость
  const STEP_DECREMENT = 4;     // ускорение за каждую съеденную точку

  // ---------- DOM ----------
  const canvas = document.getElementById('game');
  const ctx = canvas.getContext('2d');
  const scoreEl = document.getElementById('score');
  const bestEl = document.getElementById('best');
  const overlay = document.getElementById('overlay');
  const ovTitle = document.getElementById('ovTitle');
  const ovText = document.getElementById('ovText');
  const ovBtn = document.getElementById('ovBtn');
  const boardWrap = document.getElementById('boardWrap');

  // ---------- Состояние ----------
  let cell = 20;
  let dpr = 1;
  let snake = [];
  let dir = { x: 1, y: 0 };
  let nextDir = { x: 1, y: 0 };
  let food = { x: 0, y: 0 };
  let score = 0;
  let best = 0;
  let stepMs = BASE_STEP_MS;
  let acc = 0;
  let lastTime = 0;
  let running = false;
  let gameOver = false;
  let rafId = null;

  try {
    best = parseInt(localStorage.getItem('snake_best') || '0', 10) || 0;
  } catch (e) { best = 0; }
  bestEl.textContent = best;

  // ---------- Ресайз / DPR ----------
  function resize() {
    const wrapRect = boardWrap.getBoundingClientRect();
    const availW = wrapRect.width - 24;
    const availH = wrapRect.height - 8;
    if (availW <= 0 || availH <= 0) return;

    // Квадратное поле, вписанное в доступную область
    const size = Math.floor(Math.min(availW, availH));
    cell = Math.floor(size / COLS);
    const boardPx = cell * COLS;

    dpr = Math.min(window.devicePixelRatio || 1, 2);

    canvas.style.width = boardPx + 'px';
    canvas.style.height = boardPx + 'px';
    canvas.width = Math.floor(boardPx * dpr);
    canvas.height = Math.floor(boardPx * dpr);

    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    draw();
  }

  // ---------- Инициализация игры ----------
  function resetGame() {
    snake = [
      { x: 9, y: 10 },
      { x: 8, y: 10 },
      { x: 7, y: 10 }
    ];
    dir = { x: 1, y: 0 };
    nextDir = { x: 1, y: 0 };
    score = 0;
    stepMs = BASE_STEP_MS;
    acc = 0;
    lastTime = 0;
    gameOver = false;
    scoreEl.textContent = '0';
    placeFood();
  }

  function placeFood() {
    const free = [];
    for (let y = 0; y < ROWS; y++) {
      for (let x = 0; x < COLS; x++) {
        if (!snake.some(s => s.x === x && s.y === y)) {
          free.push({ x, y });
        }
      }
    }
    if (free.length === 0) {
      // Победа — вся доска заполнена
      endGame(true);
      return;
    }
    food = free[Math.floor(Math.random() * free.length)];
  }

  // ---------- Логика ----------
  function setDirection(nx, ny) {
    if (!running || gameOver) return;
    // Запрет разворота на 180°
    if (nx === -dir.x && ny === -dir.y) return;
    nextDir = { x: nx, y: ny };
  }

  function step() {
    dir = nextDir;
    const head = { x: snake[0].x + dir.x, y: snake[0].y + dir.y };

    // Столкновение со стеной
    if (head.x < 0 || head.x >= COLS || head.y < 0 || head.y >= ROWS) {
      endGame(false);
      return;
    }

    // Столкновение с собой (хвост сдвинется, если не едим — учитываем это)
    const willEat = (head.x === food.x && head.y === food.y);
    const limit = willEat ? snake.length : snake.length - 1;
    for (let i = 0; i < limit; i++) {
      if (snake[i].x === head.x && snake[i].y === head.y) {
        endGame(false);
        return;
      }
    }

    snake.unshift(head);

    if (willEat) {
      score++;
      scoreEl.textContent = score;
      if (score > best) {
        best = score;
        bestEl.textContent = best;
        try { localStorage.setItem('snake_best', String(best)); } catch (e) {}
      }
      stepMs = Math.max(MIN_STEP_MS, stepMs - STEP_DECREMENT);
      placeFood();
    } else {
      snake.pop();
    }
  }

  // ---------- Отрисовка ----------
  function roundRect(x, y, w, h, r) {
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.arcTo(x + w, y, x + w, y + h, r);
    ctx.arcTo(x + w, y + h, x, y + h, r);
    ctx.arcTo(x, y + h, x, y, r);
    ctx.arcTo(x, y, x + w, y, r);
    ctx.closePath();
  }

  function draw() {
    const boardPx = cell * COLS;
    ctx.clearRect(0, 0, boardPx, boardPx);

    // Фон
    ctx.fillStyle = '#161616';
    ctx.fillRect(0, 0, boardPx, boardPx);

    // Сетка (тонкая)
    ctx.strokeStyle = 'rgba(255,255,255,0.025)';
    ctx.lineWidth = 1;
    for (let i = 1; i < COLS; i++) {
      const p = i * cell + 0.5;
      ctx.beginPath(); ctx.moveTo(p, 0); ctx.lineTo(p, boardPx); ctx.stroke();
      ctx.beginPath(); ctx.moveTo(0, p); ctx.lineTo(boardPx, p); ctx.stroke();
    }

    // Еда
    const pad = Math.max(2, cell * 0.18);
    const fs = cell - pad * 2;
    ctx.fillStyle = '#e8e8e8';
    roundRect(food.x * cell + pad, food.y * cell + pad, fs, fs, fs * 0.35);
    ctx.fill();

    // Змейка
    for (let i = snake.length - 1; i >= 0; i--) {
      const s = snake[i];
      const p = i === 0 ? Math.max(1.5, cell * 0.1) : Math.max(1.5, cell * 0.14);
      const sz = cell - p * 2;
      if (i === 0) {
        ctx.fillStyle = '#ffffff';
      } else {
        // Плавное затухание к хвосту
        const t = i / Math.max(1, snake.length - 1);
        const lum = Math.round(210 - t * 90);
        ctx.fillStyle = `rgb(${lum},${lum},${lum})`;
      }
      roundRect(s.x * cell + p, s.y * cell + p, sz, sz, sz * 0.3);
      ctx.fill();
    }
  }

  // ---------- Игровой цикл ----------
  function loop(now) {
    rafId = requestAnimationFrame(loop);
    if (!running || gameOver) {
      lastTime = now;
      return;
    }
    if (!lastTime) lastTime = now;
    let delta = now - lastTime;
    lastTime = now;
    if (delta > 250) delta = 250; // защита от больших скачков (таб в фоне)

    acc += delta;
    let iter = 0;
    while (acc >= stepMs && iter < 5) {
      acc -= stepMs;
      step();
      iter++;
      if (gameOver) break;
    }
    draw();
  }

  // ---------- Управление игрой ----------
  function startGame() {
    resetGame();
    overlay.classList.remove('show');
    running = true;
    lastTime = 0;
    acc = 0;
    if (rafId === null) rafId = requestAnimationFrame(loop);
  }

  function endGame(won) {
    gameOver = true;
    running = false;
    draw();

    if (won) {
      ovTitle.textContent = 'Победа!';
      ovText.innerHTML = `Вы заполнили всё поле.<br>Счёт: <b>${score}</b>`;
    } else {
      ovTitle.textContent = 'Игра окончена';
      ovText.innerHTML = `Счёт: <b>${score}</b> &nbsp;·&nbsp; Рекорд: <b>${best}</b>`;
    }
    ovBtn.textContent = 'Заново';
    overlay.classList.add('show');
  }

  // ---------- Ввод: клавиатура ----------
  const keyMap = {
    ArrowUp:    [0, -1], KeyW: [0, -1],
    ArrowDown:  [0,  1], KeyS: [0,  1],
    ArrowLeft:  [-1, 0], KeyA: [-1, 0],
    ArrowRight: [1,  0], KeyD: [1,  0]
  };

  window.addEventListener('keydown', (e) => {
    if (e.code === 'Space' || e.code === 'Enter') {
      if (!running) { e.preventDefault(); startGame(); }
      return;
    }
    const d = keyMap[e.code];
    if (d) {
      e.preventDefault();
      setDirection(d[0], d[1]);
    }
  }, { passive: false });

  // ---------- Ввод: кнопки ----------
  document.querySelectorAll('.pad button').forEach(btn => {
    const handler = (e) => {
      e.preventDefault();
      const map = { up: [0,-1], down: [0,1], left: [-1,0], right: [1,0] };
      const d = map[btn.dataset.dir];
      if (d) setDirection(d[0], d[1]);
    };
    btn.addEventListener('touchstart', handler, { passive: false });
    btn.addEventListener('mousedown', handler);
    btn.addEventListener('contextmenu', e => e.preventDefault());
  });

  // ---------- Ввод: свайпы ----------
  let touchStart = null;
  const SWIPE_MIN = 24;

  canvas.addEventListener('touchstart', (e) => {
    if (e.touches.length !== 1) return;
    const t = e.touches[0];
    touchStart = { x: t.clientX, y: t.clientY };
  }, { passive: true });

  canvas.addEventListener('touchmove', (e) => {
    if (!touchStart) return;
    e.preventDefault();
    const t = e.touches[0];
    const dx = t.clientX - touchStart.x;
    const dy = t.clientY - touchStart.y;
    const adx = Math.abs(dx), ady = Math.abs(dy);
    if (Math.max(adx, ady) < SWIPE_MIN) return;

    if (adx > ady) {
      setDirection(dx > 0 ? 1 : -1, 0);
    } else {
      setDirection(0, dy > 0 ? 1 : -1);
    }
    touchStart = { x: t.clientX, y: t.clientY };
  }, { passive: false });

  canvas.addEventListener('touchend', () => { touchStart = null; }, { passive: true });
  canvas.addEventListener('touchcancel', () => { touchStart = null; }, { passive: true });

  // Свайп и по оверлею (для старта)
  ovBtn.addEventListener('click', (e) => {
    e.preventDefault();
    startGame();
  });
  ovBtn.addEventListener('touchend', (e) => {
    e.preventDefault();
    startGame();
  }, { passive: false });

  // ---------- Ресайз ----------
  let resizeTimer = null;
  function onResize() {
    if (resizeTimer) clearTimeout(resizeTimer);
    resizeTimer = setTimeout(() => {
      resize();
    }, 80);
  }
  window.addEventListener('resize', onResize);
  window.addEventListener('orientationchange', onResize);
  if (window.visualViewport) {
    window.visualViewport.addEventListener('resize', onResize);
  }

  // Пауза при уходе со страницы
  document.addEventListener('visibilitychange', () => {
    if (document.hidden) {
      lastTime = 0;
    }
  });

  // ---------- Старт ----------
  // Первичный ресайз после отрисовки layout
  requestAnimationFrame(() => {
    resize();
    resetGame();
    draw();
  });
})();
</script>
</body>
</html>
""";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createWebView();
    }

    private void createWebView() {
        WebView wv = new WebView(this);

        android.webkit.WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);

        try {
            java.io.File dir = getDir("webview", android.content.Context.MODE_PRIVATE);
            if (!dir.exists()) dir.mkdirs();
            s.setDatabasePath(dir.getAbsolutePath());
            android.webkit.WebStorage.getInstance().setQuotaForOrigin("file:///", 200L * 1024L * 1024L);
        } catch (Exception ignored) {}

        wv.addJavascriptInterface(new AndroidFileSaver(), "AndroidFileSaver");
        wv.addJavascriptInterface(new AndroidStorage(), "AndroidStorage");

        wv.setWebViewClient(new WebViewClient());

        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params) {
                MainActivity.this.filePathCallback = callback;
                Intent intent = params.createIntent();
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                } catch (ActivityNotFoundException e) {
                    MainActivity.this.filePathCallback = null;
                    return false;
                }
                return true;
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }
        });

        wv.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            } catch (Exception ignored) {}
        });

        wv.loadDataWithBaseURL(null, HTML_CONTENT, "text/html", "UTF-8", null);
        setContentView(wv);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (filePathCallback == null) return;
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = data.getClipData().getItemAt(i).getUri();
                    }
                } else if (data.getData() != null) {
                    results = new Uri[]{ data.getData() };
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    /**
     * Мост для сохранения файлов из JavaScript.
     * Принимает dataURL "data:mime;base64,..." и сохраняет в Downloads.
     */
    public class AndroidFileSaver {
        @JavascriptInterface
        public void saveBase64(final String dataUrl, final String suggestedName, final String mimetype) {
            new Thread(() -> {
                try {
                    int comma = dataUrl.indexOf(',');
                    if (comma < 0) {
                        showToast("Неверный формат данных");
                        return;
                    }
                    String b64 = dataUrl.substring(comma + 1);
                    byte[] bytes = Base64.decode(b64, Base64.DEFAULT);

                    String fileName = (suggestedName == null || suggestedName.isEmpty())
                        ? "download_" + System.currentTimeMillis()
                        : suggestedName;
                    String mime = (mimetype == null || mimetype.isEmpty())
                        ? "application/octet-stream"
                        : mimetype;

                    if (Build.VERSION.SDK_INT >= 29) {
                        // Android 10+ через MediaStore (Downloads)
                        ContentValues values = new ContentValues();
                        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                        values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                        Uri uri = getContentResolver().insert(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                        if (uri != null) {
                            OutputStream os = getContentResolver().openOutputStream(uri);
                            if (os != null) {
                                os.write(bytes);
                                os.close();
                                showToast("Сохранено в Downloads: " + fileName);
                                return;
                            }
                        }
                        showToast("Не удалось сохранить");
                    } else {
                        // Android 9 и ниже — напрямую в папку
                        File downloads = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS);
                        if (!downloads.exists()) downloads.mkdirs();
                        File out = new File(downloads, fileName);
                        FileOutputStream fos = new FileOutputStream(out);
                        fos.write(bytes);
                        fos.close();
                        showToast("Сохранено: " + out.getAbsolutePath());
                    }
                } catch (Exception e) {
                    showToast("Ошибка: " + e.getMessage());
                }
            }).start();
        }
    }

    /**
     * Мост для постоянного хранения данных из JavaScript.
     * SharedPreferences — данные не стираются при закрытии приложения.
     */
    public class AndroidStorage {
        private SharedPreferences prefs;

        AndroidStorage() {
            prefs = getSharedPreferences("apkb_storage", Context.MODE_PRIVATE);
        }

        @JavascriptInterface
        public String get(String key) {
            try { return prefs.getString(key, null); } catch (Exception e) { return null; }
        }

        @JavascriptInterface
        public void set(String key, String value) {
            try { prefs.edit().putString(key, value).apply(); } catch (Exception ignored) {}
        }

        @JavascriptInterface
        public void remove(String key) {
            try { prefs.edit().remove(key).apply(); } catch (Exception ignored) {}
        }

        @JavascriptInterface
        public String keys() {
            try {
                JSONArray arr = new JSONArray();
                for (String k : prefs.getAll().keySet()) arr.put(k);
                return arr.toString();
            } catch (Exception e) {
                return "[]";
            }
        }
    }

    private void showToast(final String msg) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show());
    }
}
