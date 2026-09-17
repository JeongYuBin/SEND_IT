// UI regression check with isolated browser storage and mocked APIs (no real shares).
// Start frontend on port 5174, then: node scripts/check-mobile-share.mjs
import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { mkdtemp, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const base = 'http://127.0.0.1:5174'
const port = 9337
const width = Number(process.env.TEST_WIDTH || 390)
const height = Number(process.env.TEST_HEIGHT || 844)
const artifacts = await mkdtemp(join(tmpdir(), 'sendit-mobile-check-'))
const executable = process.env.CHROME_PATH || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
spawn(executable, ['--headless', '--disable-gpu', '--no-first-run', '--no-default-browser-check',
  `--remote-debugging-port=${port}`, `--user-data-dir=${join(artifacts, 'profile')}`, 'about:blank'],
{ windowsHide: true, stdio: 'ignore' })
const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms))
let target
for (let attempt = 0; attempt < 50; attempt++) {
  try { target = await (await fetch(`http://127.0.0.1:${port}/json/new?about:blank`, { method: 'PUT' })).json(); break }
  catch { await delay(200) }
}
assert.ok(target?.webSocketDebuggerUrl, 'Chrome debugging endpoint is available')
const socket = new WebSocket(target.webSocketDebuggerUrl)
await new Promise((resolve) => socket.addEventListener('open', resolve, { once: true }))
let sequence = 0
const pending = new Map()
const errors = []
const requests = []
function command(method, params = {}) {
  return new Promise((resolve, reject) => {
    const id = ++sequence
    pending.set(id, { resolve, reject })
    socket.send(JSON.stringify({ id, method, params }))
  })
}
let saved = false
const post = { shareId: 41, originalUrl: 'https://www.instagram.com/reel/test/', sourceType: 'INSTAGRAM',
  status: 'ANALYZING', title: null, extractedPlaceName: null, extractedCategory: null, thumbnailUrl: null,
  collectionId: 2, collectionName: '카페', extractedPlaces: [], createdAt: '2026-09-17T10:00:00Z' }
socket.addEventListener('message', async ({ data }) => {
  const event = JSON.parse(data)
  if (event.id) {
    const operation = pending.get(event.id)
    pending.delete(event.id)
    if (event.error) operation?.reject(new Error(JSON.stringify(event.error)))
    else operation?.resolve(event.result)
    return
  }
  if (event.method === 'Runtime.exceptionThrown') errors.push(event.params.exceptionDetails.text)
  if (event.method !== 'Fetch.requestPaused') return
  const { requestId, request } = event.params
  try {
    const url = new URL(request.url)
    const api = url.pathname.startsWith('/api/') && [base, 'http://localhost:8080'].includes(url.origin)
    if (!api && url.origin !== base) { await command('Fetch.failRequest', { requestId, errorReason: 'BlockedByClient' }); return }
    if (!api) { await command('Fetch.continueRequest', { requestId }); return }
    requests.push(`${request.method} ${url.pathname}`)
    let body = []
    if (url.pathname.endsWith('/collections')) body = [{ id: 1, name: '여행지' }, { id: 2, name: '카페' }, { id: 3, name: '음식점' }]
    if (url.pathname.endsWith('/unread-count')) body = { count: 0 }
    if (url.pathname.endsWith('/shares') && request.method === 'POST') body = { shareId: 41, status: 'PENDING', duplicate: false }
    if (url.pathname.endsWith('/shares/41')) body = { ...post, collectionId: null }
    if (url.pathname.endsWith('/shares/41/collection/2') && request.method === 'PATCH') saved = true
    if (url.pathname.endsWith('/shares/saved')) body = { content: saved ? [post] : [], page: 0, totalElements: saved ? 1 : 0, totalPages: 1, last: true }
    await command('Fetch.fulfillRequest', { requestId, responseCode: 200,
      responseHeaders: [{ name: 'Content-Type', value: 'application/json' },
        { name: 'Access-Control-Allow-Origin', value: base },
        { name: 'Access-Control-Allow-Methods', value: 'GET,POST,PATCH,OPTIONS' },
        { name: 'Access-Control-Allow-Headers', value: 'authorization,content-type' }], body: Buffer.from(JSON.stringify(body)).toString('base64') })
  } catch (error) { errors.push(error.message) }
})
async function evaluate(expression) {
  const result = await command('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true })
  if (result.exceptionDetails) throw new Error(JSON.stringify(result.exceptionDetails))
  return result.result.value
}
async function until(expression) {
  for (let attempt = 0; attempt < 100; attempt++) {
    if (await evaluate(expression)) return
    await delay(100)
  }
  throw new Error(`Timed out: ${expression}\n${await evaluate('document.body.innerText')}\n${JSON.stringify({errors,requests})}`)
}
async function screenshot(name) {
  const result = await command('Page.captureScreenshot', { format: 'png' })
  await writeFile(join(artifacts, `${name}.png`), Buffer.from(result.data, 'base64'))
}
try {
  await command('Page.enable')
  await command('Runtime.enable')
  await command('Fetch.enable', { patterns: [{ urlPattern: '*' }] })
  await command('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile: true })
  await command('Page.addScriptToEvaluateOnNewDocument', { source: `
    localStorage.setItem('sendit-auth', JSON.stringify({state:{accessToken:'ui-test',refreshToken:null,user:{id:1,nickname:'테스트',email:'test@example.com'}},version:0}));
    window.__nativeMessages = [];
    window.SendITNative = {postMessage: value => window.__nativeMessages.push(JSON.parse(value))};
  ` })
  await command('Page.navigate', { url: base })
  await until("!!document.querySelector('.home-map-controls') && !!document.querySelector('.mobile-menu-sheet.expanded')")
  const layout = await evaluate(`(() => {
    const search = document.querySelector('.map-search-form').getBoundingClientRect();
    const categories = document.querySelector('.home-map-filters').getBoundingClientRect();
    const menu = document.querySelector('.mobile-menu-sheet').getBoundingClientRect();
    return { searchTop: search.top, searchBottom: search.bottom, categoriesTop: categories.top, menuHeight: menu.height, overflow: document.documentElement.scrollWidth > innerWidth };
  })()`)
  assert.ok(layout.searchTop >= 0 && layout.categoriesTop >= layout.searchBottom, JSON.stringify(layout))
  assert.ok(layout.menuHeight <= 85, JSON.stringify(layout))
  assert.equal(layout.overflow, false)
  await screenshot('map-controls')
  await evaluate("document.querySelector('.mobile-bottom-nav a[href=\"/saved\"]').click()")
  await until("!!document.querySelector('.feed-shell')")
  assert.equal(await evaluate("!!document.querySelector('.mobile-menu-sheet.expanded')"), true)
  await evaluate("document.querySelector('.mobile-menu-handle').click()")
  await command('Page.navigate', { url: base })
  await until("!!document.querySelector('.home-map-controls')")
  assert.equal(await evaluate("!!document.querySelector('.mobile-menu-sheet.expanded')"), false)
  await command('Page.navigate', { url: `${base}/share-target?url=${encodeURIComponent(post.originalUrl)}` })
  await until("!!document.querySelector('.collection-picker-confirm:not(:disabled)')")
  const shareHeight = await evaluate("document.querySelector('.share-save-sheet').getBoundingClientRect().height")
  assert.ok(shareHeight <= height * .3, `Share sheet height ${shareHeight}`)
  assert.equal(await evaluate("document.body.textContent.includes('자동 추천을 분석 중')"), false)
  await screenshot('compact-share')
  await evaluate("[...document.querySelectorAll('.collection-picker-options button')].find(b=>b.textContent.includes('카페')).click()")
  await evaluate("document.querySelector('.collection-picker-confirm').click()")
  await until("window.__nativeMessages.some(m => m.type === 'close')")
  assert.equal(saved, true)
  assert.equal(requests.filter(request => request === 'POST /api/v1/shares').length, 1, 'Share registration is not duplicated by StrictMode')
  assert.ok(requests.includes('PATCH /api/v1/shares/41/collection/2'))
  await command('Page.navigate', { url: `${base}/saved` })
  await until("!!document.querySelector('.saved-shared-post-grid article')")
  assert.ok(await evaluate("document.querySelector('.saved-shared-post-grid').textContent.includes('장소 찾는 중')"))
  await screenshot('pending-post-in-feed')
  assert.deepEqual(errors, [])
  console.log(JSON.stringify({ passed: true, viewport: {width, height}, layout, shareHeight, screenshots: artifacts }, null, 2))
} finally {
  await command('Browser.close').catch(() => {})
  socket.close()
}
