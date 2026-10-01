package com.alexeevprog.a2048game;

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
<html lang="ru" data-theme="light">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
<title>2048</title>
<style>
  /* ============ RESET ============ */
  *, *::before, *::after { box-sizing: border-box; }

  html, body { margin: 0; padding: 0; }

  /* ============ THEME TOKENS ============ */
  :root {
    color-scheme: light;

    --gap: 12px;
    --radius: 16px;
    --radius-tile: 11px;

    --bg: #f1f4fa;
    --bg-1: rgba(129, 140, 248, .30);
    --bg-2: rgba(244, 114, 182, .22);

    --text: #1e293b;
    --text-dim: #64748b;

    --board-bg: rgba(255, 255, 255, .62);
    --board-border: rgba(15, 23, 42, .07);
    --cell-bg: rgba(100, 116, 139, .12);

    --chip-bg: rgba(255, 255, 255, .78);
    --chip-border: rgba(15, 23, 42, .07);
    --chip-shadow: 0 8px 24px rgba(15, 23, 42, .07);

    --btn-bg: #ffffff;
    --btn-border: rgba(15, 23, 42, .09);
    --btn-text: #334155;
    --btn-hover: #f8fafc;

    --accent: #6366f1;
    --accent-soft: rgba(99, 102, 241, .12);
    --overlay-bg: rgba(248, 250, 252, .84);

    /* tile palette — light */
    --t2-bg:#eef1f8;    --t2-fg:#64748b;
    --t4-bg:#dfe6f5;    --t4-fg:#475569;
    --t8-bg:#c7d2fe;    --t8-fg:#3730a3;
    --t16-bg:#a5b4fc;   --t16-fg:#312e81;
    --t32-bg:#818cf8;   --t32-fg:#ffffff;
    --t64-bg:#6366f1;   --t64-fg:#ffffff;
    --t128-bg:#8b5cf6;  --t128-fg:#ffffff;
    --t256-bg:#a855f7;  --t256-fg:#ffffff;
    --t512-bg:#d946ef;  --t512-fg:#ffffff;
    --t1024-bg:#ec4899; --t1024-fg:#ffffff;
    --t2048-bg:#f59e0b; --t2048-fg:#ffffff;
    --tsuper-bg:#0f172a;--tsuper-fg:#ffffff;
  }

  html[data-theme="dark"] {
    color-scheme: dark;

    --bg: #0b0e17;
    --bg-1: rgba(99, 102, 241, .22);
    --bg-2: rgba(236, 72, 153, .14);

    --text: #e2e8f0;
    --text-dim: #94a3b8;

    --board-bg: rgba(30, 36, 52, .70);
    --board-border: rgba(255, 255, 255, .06);
    --cell-bg: rgba(255, 255, 255, .045);

    --chip-bg: rgba(30, 36, 52, .75);
    --chip-border: rgba(255, 255, 255, .07);
    --chip-shadow: 0 8px 24px rgba(0, 0, 0, .35);

    --btn-bg: #1e2434;
    --btn-border: rgba(255, 255, 255, .08);
    --btn-text: #cbd5e1;
    --btn-hover: #262d40;

    --accent: #818cf8;
    --accent-soft: rgba(129, 140, 248, .16);
    --overlay-bg: rgba(11, 14, 23, .84);

    /* tile palette — dark */
    --t2-bg:#252b3a;    --t2-fg:#94a3b8;
    --t4-bg:#2e3648;    --t4-fg:#cbd5e1;
    --t8-bg:#3730a3;    --t8-fg:#c7d2fe;
    --t16-bg:#4338ca;   --t16-fg:#e0e7ff;
    --t32-bg:#4f46e5;   --t32-fg:#ffffff;
    --t64-bg:#6366f1;   --t64-fg:#ffffff;
    --t128-bg:#7c3aed;  --t128-fg:#ffffff;
    --t256-bg:#9333ea;  --t256-fg:#ffffff;
    --t512-bg:#c026d3;  --t512-fg:#ffffff;
    --t1024-bg:#db2777; --t1024-fg:#ffffff;
    --t2048-bg:#f59e0b; --t2048-fg:#ffffff;
    --tsuper-bg:#e2e8f0;--tsuper-fg:#0f172a;
  }

  /* ============ LAYOUT ============ */
  body {
    min-height: 100dvh;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 18px 16px calc(18px + env(safe-area-inset-bottom));
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
                 'Helvetica Neue', Arial, sans-serif;
    color: var(--text);
    background: var(--bg);
    transition: background .35s ease, color .35s ease;
    -webkit-tap-highlight-color: transparent;
    touch-action: manipulation;
    overflow-x: hidden;
  }

  body::before {
    content: '';
    position: fixed;
    inset: 0;
    z-index: -1;
    pointer-events: none;
    background:
      radial-gradient(circle at 12% 8%,  var(--bg-1), transparent 46%),
      radial-gradient(circle at 88% 92%, var(--bg-2), transparent 46%);
    transition: background .35s ease;
  }

  .game {
    width: 100%;
    max-width: 460px;
    display: flex;
    flex-direction: column;
    gap: 14px;
  }

  /* ============ HEADER ============ */
  .header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
  }

  .title {
    margin: 0;
    font-size: clamp(1.9rem, 7vw, 2.4rem);
    font-weight: 900;
    letter-spacing: -.045em;
    line-height: 1;
    background: linear-gradient(135deg, var(--accent), #ec4899 90%);
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent;
    -webkit-text-fill-color: transparent;
  }

  .subtitle {
    margin: 6px 0 0;
    font-size: .78rem;
    font-weight: 500;
    color: var(--text-dim);
    letter-spacing: .01em;
  }

  .controls { display: flex; gap: 8px; flex-shrink: 0; }

  .btn, .icon-btn {
    font: inherit;
    font-weight: 700;
    color: var(--btn-text);
    background: var(--btn-bg);
    border: 1px solid var(--btn-border);
    border-radius: 11px;
    cursor: pointer;
    transition: background .2s ease, transform .12s ease, border-color .2s ease, box-shadow .2s ease;
    -webkit-user-select: none;
    user-select: none;
  }

  .btn { height: 40px; padding: 0 16px; font-size: .82rem; white-space: nowrap; }

  .icon-btn { width: 40px; height: 40px; padding: 0; display: grid; place-items: center; }

  .btn:hover, .icon-btn:hover { background: var(--btn-hover); }

  .btn:active, .icon-btn:active { transform: scale(.94); }

  .btn:focus-visible, .icon-btn:focus-visible {
    outline: 2px solid var(--accent);
    outline-offset: 2px;
  }

  .icon-btn svg { width: 18px; height: 18px; display: block; }

  html[data-theme="light"] .icon-sun  { display: none; }
  html[data-theme="dark"]  .icon-moon { display: none; }

  /* ============ STATS ============ */
  .stats { display: flex; gap: 10px; }

  .stat {
    flex: 1;
    position: relative;
    overflow: hidden;
    padding: 8px 12px 9px;
    text-align: center;
    background: var(--chip-bg);
    border: 1px solid var(--chip-border);
    border-radius: 13px;
    box-shadow: var(--chip-shadow);
    transition: background .35s ease, border-color .35s ease;
  }

  .stat > span {
    display: block;
    font-size: .64rem;
    font-weight: 700;
    letter-spacing: .1em;
    text-transform: uppercase;
    color: var(--text-dim);
  }

  .stat > strong {
    display: block;
    margin-top: 2px;
    font-size: 1.18rem;
    font-weight: 800;
    font-variant-numeric: tabular-nums;
    line-height: 1.2;
  }

  .score-add {
    position: absolute;
    left: 50%;
    top: 55%;
    font-size: 1rem;
    font-weight: 800;
    color: var(--accent);
    pointer-events: none;
    animation: float-up .72s cubic-bezier(.22,.8,.3,1) forwards;
  }

  @keyframes float-up {
    0%   { opacity: 0; transform: translate(-50%, -20%) scale(.75); }
    22%  { opacity: 1; transform: translate(-50%, -60%) scale(1.05); }
    100% { opacity: 0; transform: translate(-50%, -190%) scale(1); }
  }

  /* ============ BOARD ============ */
  .board {
    position: relative;
    width: 100%;
    aspect-ratio: 1 / 1;
    padding: var(--gap);
    background: var(--board-bg);
    border: 1px solid var(--board-border);
    border-radius: var(--radius);
    box-shadow: var(--chip-shadow);
    container-type: inline-size;
    overflow: hidden;
    touch-action: none;
    -webkit-user-select: none;
    user-select: none;
    transition: background .35s ease, border-color .35s ease;
  }

  .grid-bg {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    grid-template-rows: repeat(4, 1fr);
    gap: var(--gap);
    width: 100%;
    height: 100%;
  }

  .cell {
    background: var(--cell-bg);
    border-radius: var(--radius-tile);
    transition: background .35s ease;
  }

  .tiles { position: absolute; inset: var(--gap); }

  .tile {
    position: absolute;
    top: 0;
    left: 0;
    width: calc((100% - 3 * var(--gap)) / 4);
    height: calc((100% - 3 * var(--gap)) / 4);
    transition: transform .11s ease-in-out;
    will-change: transform;
  }

  .tile-inner {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: var(--radius-tile);
    font-size: 10.5cqw;
    font-weight: 800;
    letter-spacing: -.045em;
    line-height: 1;
    font-variant-numeric: tabular-nums;
    will-change: transform;
    transition: background-color .25s ease, color .25s ease, box-shadow .25s ease;
  }

  .tile-inner.len-3 { font-size: 8.6cqw; }
  .tile-inner.len-4 { font-size: 6.9cqw; }
  .tile-inner.len-5 { font-size: 5.6cqw; }
  .tile-inner.len-6 { font-size: 4.7cqw; }

  /* tile colors */
  .tile-inner[data-v="2"]    { background: var(--t2-bg);    color: var(--t2-fg); }
  .tile-inner[data-v="4"]    { background: var(--t4-bg);    color: var(--t4-fg); }
  .tile-inner[data-v="8"]    { background: var(--t8-bg);    color: var(--t8-fg); }
  .tile-inner[data-v="16"]   { background: var(--t16-bg);   color: var(--t16-fg); }
  .tile-inner[data-v="32"]   { background: var(--t32-bg);   color: var(--t32-fg); }
  .tile-inner[data-v="64"]   { background: var(--t64-bg);   color: var(--t64-fg); }
  .tile-inner[data-v="128"]  { background: var(--t128-bg);  color: var(--t128-fg); }
  .tile-inner[data-v="256"]  { background: var(--t256-bg);  color: var(--t256-fg); }
  .tile-inner[data-v="512"]  { background: var(--t512-bg);  color: var(--t512-fg); }
  .tile-inner[data-v="1024"] { background: var(--t1024-bg); color: var(--t1024-fg); }
  .tile-inner[data-v="2048"] {
    background: var(--t2048-bg);
    color: var(--t2048-fg);
    box-shadow: 0 0 0 2px rgba(245, 158, 11, .30), 0 0 26px rgba(245, 158, 11, .45);
  }
  .tile-inner[data-v="super"] { background: var(--tsuper-bg); color: var(--tsuper-fg); }

  @keyframes tile-appear {
    from { transform: scale(0); opacity: 0; }
    to   { transform: scale(1); opacity: 1; }
  }

  @keyframes tile-pop {
    0%   { transform: scale(1); }
    50%  { transform: scale(1.16); }
    100% { transform: scale(1); }
  }

  .tile-inner.appear { animation: tile-appear .16s ease-out; }
  .tile-inner.pop    { animation: tile-pop .18s ease-out; }

  /* ============ OVERLAY ============ */
  .overlay {
    position: absolute;
    inset: 0;
    z-index: 20;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 10px;
    padding: 24px;
    text-align: center;
    background: var(--overlay-bg);
    -webkit-backdrop-filter: blur(8px);
    backdrop-filter: blur(8px);
    border-radius: var(--radius);
    opacity: 0;
    pointer-events: none;
    transition: opacity .3s ease;
  }

  .overlay.show { opacity: 1; pointer-events: auto; }

  .overlay h2 {
    margin: 0;
    font-size: clamp(1.35rem, 6vw, 1.7rem);
    font-weight: 900;
    letter-spacing: -.03em;
  }

  .overlay[data-type="win"] h2 {
    background: linear-gradient(135deg, #f59e0b, #ec4899);
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent;
    -webkit-text-fill-color: transparent;
  }

  .overlay p {
    margin: 0;
    max-width: 260px;
    font-size: .88rem;
    line-height: 1.45;
    color: var(--text-dim);
  }

  .overlay-btns {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    justify-content: center;
    margin-top: 8px;
  }

  .overlay-btns .btn { height: 42px; padding: 0 20px; font-size: .85rem; }

  .btn.primary {
    background: var(--accent);
    border-color: transparent;
    color: #fff;
    box-shadow: 0 8px 20px -6px var(--accent);
  }

  .btn.primary:hover { background: var(--accent); filter: brightness(1.08); }

  /* ============ HINT ============ */
  .hint {
    margin: 0;
    text-align: center;
    font-size: .74rem;
    font-weight: 500;
    color: var(--text-dim);
    letter-spacing: .01em;
  }

  /* ============ RESPONSIVE ============ */
  @media (max-width: 480px) {
    :root { --gap: 9px; --radius: 14px; --radius-tile: 9px; }
    .game { gap: 12px; }
    .btn { padding: 0 13px; font-size: .78rem; }
  }

  @media (max-height: 700px) {
    .subtitle { display: none; }
    .game { gap: 10px; }
  }

  @media (prefers-reduced-motion: reduce) {
    *, *::before, *::after {
      animation-duration: .01ms !important;
      animation-iteration-count: 1 !important;
      transition-duration: .01ms !important;
    }
  }
</style>
</head>
<body>

<div class="game">
  <header class="header">
    <div>
      <h1 class="title">2048</h1>
      <p class="subtitle">Собери плитку 2048</p>
    </div>
    <div class="controls">
      <button class="icon-btn" id="themeBtn" type="button" aria-label="Переключить тему">
        <svg class="icon-moon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
        </svg>
        <svg class="icon-sun" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <circle cx="12" cy="12" r="4"/>
          <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"/>
        </svg>
      </button>
      <button class="btn" id="newGameBtn" type="button">Новая игра</button>
    </div>
  </header>

  <div class="stats">
    <div class="stat" id="scoreBox">
      <span>Очки</span>
      <strong id="score">0</strong>
    </div>
    <div class="stat">
      <span>Рекорд</span>
      <strong id="best">0</strong>
    </div>
  </div>

  <div class="board" id="board">
    <div class="grid-bg" id="gridBg"></div>
    <div class="tiles" id="tiles"></div>
    <div class="overlay" id="overlay">
      <h2 id="overlayTitle"></h2>
      <p id="overlayText"></p>
      <div class="overlay-btns" id="overlayBtns"></div>
    </div>
  </div>

  <p class="hint">Свайпайте по полю или используйте стрелки ← ↑ → ↓</p>
</div>

<script>
(() => {
  'use strict';

  /* ================= CONSTANTS ================= */
  const SIZE = 4;
  const ANIM_MS = 110;
  const SWIPE_THRESHOLD = 24;
  const STORAGE_BEST = 'game2048.best';
  const STORAGE_THEME = 'game2048.theme';

  const DIRS = {
    up:    { x: 0,  y: -1 },
    down:  { x: 0,  y: 1  },
    left:  { x: -1, y: 0  },
    right: { x: 1,  y: 0  }
  };

  /* ================= DOM ================= */
  const boardEl        = document.getElementById('board');
  const tilesEl        = document.getElementById('tiles');
  const gridBgEl       = document.getElementById('gridBg');
  const scoreEl        = document.getElementById('score');
  const bestEl         = document.getElementById('best');
  const scoreBoxEl     = document.getElementById('scoreBox');
  const overlayEl      = document.getElementById('overlay');
  const overlayTitleEl = document.getElementById('overlayTitle');
  const overlayTextEl  = document.getElementById('overlayText');
  const overlayBtnsEl  = document.getElementById('overlayBtns');
  const newGameBtn     = document.getElementById('newGameBtn');
  const themeBtn       = document.getElementById('themeBtn');

  /* ================= STATE ================= */
  let tiles = [];
  let idCounter = 0;
  let score = 0;
  let best = 0;
  let gameOver = false;
  let won = false;
  let pendingFn = null;
  let pendingTimer = null;
  let overlayVisible = false;

  /* ================= STORAGE HELPERS ================= */
  function readStorage(key) {
    try { return localStorage.getItem(key); } catch (e) { return null; }
  }
  function writeStorage(key, value) {
    try { localStorage.setItem(key, value); } catch (e) { /* ignore */ }
  }

  /* ================= GRID HELPERS ================= */
  function tileAt(x, y) {
    for (let i = 0; i < tiles.length; i++) {
      const t = tiles[i];
      if (!t.absorbed && t.x === x && t.y === y) return t;
    }
    return null;
  }

  function emptyCells() {
    const cells = [];
    for (let y = 0; y < SIZE; y++) {
      for (let x = 0; x < SIZE; x++) {
        if (!tileAt(x, y)) cells.push({ x, y });
      }
    }
    return cells;
  }

  function inBounds(x, y) {
    return x >= 0 && x < SIZE && y >= 0 && y < SIZE;
  }

  /* ================= RENDERING ================= */
  function placeTile(tile) {
    tile.el.style.transform =
      'translate(calc(' + tile.x + ' * 100% + ' + tile.x + ' * var(--gap)), ' +
                'calc(' + tile.y + ' * 100% + ' + tile.y + ' * var(--gap)))';
  }

  function styleTile(tile) {
    const v = tile.value;
    const inner = tile.inner;
    inner.textContent = String(v);
    inner.dataset.v = v > 2048 ? 'super' : String(v);

    const len = String(v).length;
    inner.classList.remove('len-3', 'len-4', 'len-5', 'len-6');
    if (len === 3)      inner.classList.add('len-3');
    else if (len === 4) inner.classList.add('len-4');
    else if (len === 5) inner.classList.add('len-5');
    else if (len >= 6)  inner.classList.add('len-6');
  }

  function createTile(x, y, value) {
    const tile = {
      id: ++idCounter,
      x: x,
      y: y,
      value: value,
      merged: false,
      absorbed: false,
      willMerge: false,
      pendingValue: 0,
      el: null,
      inner: null
    };

    const el = document.createElement('div');
    el.className = 'tile';

    const inner = document.createElement('div');
    inner.className = 'tile-inner appear';

    el.appendChild(inner);
    tile.el = el;
    tile.inner = inner;

    styleTile(tile);
    placeTile(tile);
    tilesEl.appendChild(el);
    tiles.push(tile);

    return tile;
  }

  function addRandomTile() {
    const cells = emptyCells();
    if (cells.length === 0) return null;
    const cell = cells[Math.floor(Math.random() * cells.length)];
    const value = Math.random() < 0.9 ? 2 : 4;
    return createTile(cell.x, cell.y, value);
  }

  function buildGridBackground() {
    gridBgEl.innerHTML = '';
    const frag = document.createDocumentFragment();
    for (let i = 0; i < SIZE * SIZE; i++) {
      const cell = document.createElement('div');
      cell.className = 'cell';
      frag.appendChild(cell);
    }
    gridBgEl.appendChild(frag);
  }

  /* ================= SCORE ================= */
  function addScore(points) {
    if (points <= 0) return;
    score += points;
    scoreEl.textContent = String(score);

    if (score > best) {
      best = score;
      bestEl.textContent = String(best);
      writeStorage(STORAGE_BEST, String(best));
    }

    const float = document.createElement('span');
    float.className = 'score-add';
    float.textContent = '+' + points;
    scoreBoxEl.appendChild(float);
    window.setTimeout(() => {
      if (float.parentNode) float.parentNode.removeChild(float);
    }, 750);
  }

  /* ================= PENDING ANIMATION QUEUE ================= */
  function flushPending() {
    if (pendingTimer !== null) {
      clearTimeout(pendingTimer);
      pendingTimer = null;
    }
    if (pendingFn) {
      const fn = pendingFn;
      pendingFn = null;
      fn();
    }
  }

  function cancelPending() {
    if (pendingTimer !== null) {
      clearTimeout(pendingTimer);
      pendingTimer = null;
    }
    pendingFn = null;
  }

  /* ================= GAME OVER / WIN CHECKS ================= */
  function hasMoves() {
    if (tiles.length < SIZE * SIZE) return true;

    const grid = [];
    for (let y = 0; y < SIZE; y++) {
      grid[y] = [];
      for (let x = 0; x < SIZE; x++) {
        const t = tileAt(x, y);
        grid[y][x] = t ? t.value : 0;
      }
    }

    for (let y = 0; y < SIZE; y++) {
      for (let x = 0; x < SIZE; x++) {
        const v = grid[y][x];
        if (x < SIZE - 1 && grid[y][x + 1] === v) return true;
        if (y < SIZE - 1 && grid[y + 1][x] === v) return true;
      }
    }
    return false;
  }

  function checkGameOver() {
    if (hasMoves()) return;
    gameOver = true;
    showOverlay('lose');
  }

  /* ================= OVERLAY ================= */
  function addOverlayButton(label, handler, primary) {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'btn' + (primary ? ' primary' : '');
    btn.textContent = label;
    btn.addEventListener('click', handler);
    overlayBtnsEl.appendChild(btn);
  }

  function showOverlay(type) {
    overlayVisible = true;
    overlayEl.dataset.type = type;
    overlayBtnsEl.innerHTML = '';

    if (type === 'win') {
      overlayTitleEl.textContent = '🎉 Победа!';
      overlayTextEl.textContent = 'Вы собрали плитку 2048. Продолжите игру или начните заново.';
      addOverlayButton('Продолжить', hideOverlay, false);
      addOverlayButton('Новая игра', newGame, true);
    } else {
      overlayTitleEl.textContent = 'Игра окончена';
      overlayTextEl.textContent = 'Ходов больше нет. Попробуйте ещё раз!';
      addOverlayButton('Новая игра', newGame, true);
    }

    overlayEl.classList.add('show');
  }

  function hideOverlay() {
    overlayVisible = false;
    overlayEl.classList.remove('show');
  }

  /* ================= CORE MOVE ================= */
  function move(dir) {
    if (gameOver) return;
    if (overlayVisible) return;

    flushPending();

    if (gameOver || overlayVisible) return;

    const vec = DIRS[dir];
    if (!vec) return;

    const travX = vec.x === 1 ? [3, 2, 1, 0] : [0, 1, 2, 3];
    const travY = vec.y === 1 ? [3, 2, 1, 0] : [0, 1, 2, 3];

    let moved = false;
    let gained = 0;
    const absorbed = [];

    for (let i = 0; i < tiles.length; i++) tiles[i].merged = false;

    for (let yi = 0; yi < SIZE; yi++) {
      for (let xi = 0; xi < SIZE; xi++) {
        const x = travX[xi];
        const y = travY[yi];

        const tile = tileAt(x, y);
        if (!tile) continue;

        /* find the farthest free cell */
        let cx = x;
        let cy = y;
        for (;;) {
          const nx = cx + vec.x;
          const ny = cy + vec.y;
          if (!inBounds(nx, ny)) break;
          if (tileAt(nx, ny)) break;
          cx = nx;
          cy = ny;
        }

        const nx = cx + vec.x;
        const ny = cy + vec.y;
        const target = inBounds(nx, ny) ? tileAt(nx, ny) : null;

        if (target && target.value === tile.value && !target.merged) {
          /* merge */
          target.merged = true;
          target.willMerge = true;
          target.pendingValue = target.value * 2;
          gained += target.pendingValue;

          tile.absorbed = true;
          tile.x = target.x;
          tile.y = target.y;
          absorbed.push(tile);

          moved = true;
        } else if (cx !== x || cy !== y) {
          /* slide */
          tile.x = cx;
          tile.y = cy;
          moved = true;
        }
      }
    }

    if (!moved) return;

    for (let i = 0; i < tiles.length; i++) placeTile(tiles[i]);

    if (gained > 0) addScore(gained);

    pendingFn = () => {
      /* remove absorbed tiles */
      for (let i = 0; i < absorbed.length; i++) {
        const t = absorbed[i];
        if (t.el && t.el.parentNode) t.el.parentNode.removeChild(t.el);
        const idx = tiles.indexOf(t);
        if (idx !== -1) tiles.splice(idx, 1);
      }

      /* apply merged values + pop animation */
      for (let i = 0; i < tiles.length; i++) {
        const t = tiles[i];
        if (t.willMerge) {
          t.willMerge = false;
          t.value = t.pendingValue;
          styleTile(t);
          t.inner.classList.remove('pop');
          void t.inner.offsetWidth;
          t.inner.classList.add('pop');
        }
      }

      /* spawn new tile */
      addRandomTile();

      /* win check */
      if (!won) {
        for (let i = 0; i < tiles.length; i++) {
          if (tiles[i].value >= 2048) {
            won = true;
            showOverlay('win');
            return;
          }
        }
      }

      checkGameOver();
    };

    pendingTimer = window.setTimeout(() => {
      pendingTimer = null;
      const fn = pendingFn;
      pendingFn = null;
      if (fn) fn();
    }, ANIM_MS);
  }

  /* ================= NEW GAME ================= */
  function newGame() {
    cancelPending();

    hideOverlay();
    tilesEl.innerHTML = '';
    tiles = [];
    score = 0;
    gameOver = false;
    won = false;

    scoreEl.textContent = '0';
    bestEl.textContent = String(best);

    addRandomTile();
    addRandomTile();
  }

  /* ================= THEME ================= */
  function setTheme(theme) {
    document.documentElement.dataset.theme = theme;
    writeStorage(STORAGE_THEME, theme);
    themeBtn.setAttribute(
      'aria-label',
      theme === 'dark' ? 'Включить светлую тему' : 'Включить тёмную тему'
    );
  }

  function initTheme() {
    const saved = readStorage(STORAGE_THEME);
    let theme = saved;

    if (theme !== 'light' && theme !== 'dark') {
      const prefersDark =
        window.matchMedia &&
        window.matchMedia('(prefers-color-scheme: dark)').matches;
      theme = prefersDark ? 'dark' : 'light';
    }

    setTheme(theme);
  }

  /* ================= INPUT: KEYBOARD ================= */
  function handleKeyDown(e) {
    if (e.ctrlKey || e.metaKey || e.altKey) return;

    let dir = null;
    switch (e.key) {
      case 'ArrowUp':    case 'w': case 'W': case 'ц': case 'Ц': dir = 'up';    break;
      case 'ArrowDown':  case 's': case 'S': case 'ы': case 'Ы': dir = 'down';  break;
      case 'ArrowLeft':  case 'a': case 'A': case 'ф': case 'Ф': dir = 'left';  break;
      case 'ArrowRight': case 'd': case 'D': case 'в': case 'В': dir = 'right'; break;
      default: return;
    }

    e.preventDefault();
    move(dir);
  }

  /* ================= INPUT: TOUCH ================= */
  let touchStartX = 0;
  let touchStartY = 0;
  let touchActive = false;
  let touchHandled = false;

  function onTouchStart(e) {
    if (e.touches.length !== 1) {
      touchActive = false;
      return;
    }
    const t = e.touches[0];
    touchStartX = t.clientX;
    touchStartY = t.clientY;
    touchActive = true;
    touchHandled = false;
  }

  function onTouchMove(e) {
    if (!touchActive || touchHandled) return;

    const t = e.touches[0];
    const dx = t.clientX - touchStartX;
    const dy = t.clientY - touchStartY;
    const adx = Math.abs(dx);
    const ady = Math.abs(dy);

    if (Math.max(adx, ady) < SWIPE_THRESHOLD) return;

    if (e.cancelable) e.preventDefault();

    touchHandled = true;
    touchActive = false;

    if (adx > ady) {
      move(dx > 0 ? 'right' : 'left');
    } else {
      move(dy > 0 ? 'down' : 'up');
    }
  }

  function onTouchEnd() {
    touchActive = false;
  }

  /* ================= INIT ================= */
  function init() {
    /* best score */
    const storedBest = parseInt(readStorage(STORAGE_BEST), 10);
    best = Number.isFinite(storedBest) && storedBest > 0 ? storedBest : 0;
    bestEl.textContent = String(best);

    initTheme();
    buildGridBackground();
    newGame();

    /* listeners */
    document.addEventListener('keydown', handleKeyDown);

    boardEl.addEventListener('touchstart', onTouchStart, { passive: true });
    boardEl.addEventListener('touchmove', onTouchMove, { passive: false });
    boardEl.addEventListener('touchend', onTouchEnd, { passive: true });
    boardEl.addEventListener('touchcancel', onTouchEnd, { passive: true });

    newGameBtn.addEventListener('click', newGame);

    themeBtn.addEventListener('click', () => {
      const current = document.documentElement.dataset.theme;
      setTheme(current === 'dark' ? 'light' : 'dark');
    });

    /* pause safety: apply pending state when tab is hidden */
    document.addEventListener('visibilitychange', () => {
      if (document.hidden) flushPending();
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
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
