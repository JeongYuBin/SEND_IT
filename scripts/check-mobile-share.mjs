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
const tripOnly = process.env.TEST_TRIP === '1'
const tripItems = Array.from({ length: 12 }, (_, index) => ({
  savedPlaceId: index + 1, sequence: index + 1, daySequence: index + 1,
  visitDate: '2026-09-25', arrivalTime: '10:00', departureTime: '11:00',
  name: `장소 ${index + 1}`, category: '관광지', address: '서울', imageUrl: null,
  latitude: null, longitude: null, stayMinutes: 60, transit: null,
  travelMinutesFromPrevious: index ? 31 : 0, distanceKmFromPrevious: null,
  coordinateAvailable: false, routePathFromPrevious: [], transportTypeFromPrevious: 'PUBLIC_TRANSIT',
  crossDayTransfer: false, operatingHours: null, restDays: null, visitWarning: null,
}))
const trip = { id: 77, title: '스크롤 확인 여행', startDate: '2026-09-25', endDate: '2026-09-25',
  dailyStartTime: '09:00', dailyEndTime: '23:00', transportType: 'PUBLIC_TRANSIT', status: 'GENERATED',
  items: tripItems, days: [{ date: '2026-09-25', dayNumber: 1, exceedsDailyWindow: false, items: tripItems }] }
function command(method, params = {}) {
  return new Promise((resolve, reject) => {
    const id = ++sequence
    pending.set(id, { resolve, reject })
    socket.send(JSON.stringify({ id, method, params }))
  })
}
let saved = false
let nextPageRequests = 0
const savedPlace = { savedPlaceId: 88, placeId: 188, name: '테스트 장소', category: '카페', address: '서울 종로구 테스트로 1', roadAddress: null,
  latitude: 37.5, longitude: 127, description: null, imageUrl: null, phone: null, homepageUrl: null,
  tourismContentId: null, tourismContentTypeId: null, operatingHours: null, restDays: null, parkingInfo: null,
  eventStartDate: null, eventEndDate: null, collectionId: 2, collectionName: '카페', memo: null, priority: 0,
  savedAt: '2026-09-17T10:00:00Z', originalUrl: null, kakaoPlaceId: null, kakaoPlaceUrl: null, sources: [] }
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
    if (url.pathname.includes('/itineraries/77')) {
      if (request.method === 'PUT' && url.pathname.endsWith('/transport')) {
        const placeId = Number(url.pathname.split('/').at(-2))
        tripItems.find((item) => item.savedPlaceId === placeId).transportTypeFromPrevious = JSON.parse(request.postData).transportType
      }
      body = trip
    }
    if (url.pathname.endsWith('/collections')) body = [{ id: 1, name: '여행지' }, { id: 2, name: '카페' }, { id: 3, name: '음식점' }]
    if (url.pathname.endsWith('/saved-places') && request.method === 'GET') body = [savedPlace]
    if (url.pathname.endsWith('/saved-places/88')) body = { ...savedPlace, address: null }
    if (url.pathname.endsWith('/places/search')) body = { places: [{ kakaoPlaceId: 'test-88', name: '테스트 장소',
      roadAddress: '서울 종로구 테스트로 1', address: null }], page: 1, last: true }
    if (url.pathname.endsWith('/unread-count')) body = { count: 0 }
    if (url.pathname.endsWith('/shares') && request.method === 'POST') body = { shareId: 41, status: 'PENDING', duplicate: false }
    if (url.pathname.endsWith('/shares/41')) body = { ...post, collectionId: null }
    if (url.pathname.endsWith('/shares/41/collection/2') && request.method === 'PATCH') saved = true
    if (url.pathname.endsWith('/shares/saved')) {
      const page = Number(url.searchParams.get('page') || 0)
      if (page === 1) nextPageRequests++
      body = { content: saved ? [{ ...post, shareId: page ? 42 : 41 }] : [], page,
        totalElements: saved ? 2 : 0, totalPages: saved ? 2 : 1, last: !saved || page === 1 }
    }
    await command('Fetch.fulfillRequest', { requestId, responseCode: 200,
      responseHeaders: [{ name: 'Content-Type', value: 'application/json' },
        { name: 'Access-Control-Allow-Origin', value: base },
        { name: 'Access-Control-Allow-Methods', value: 'GET,POST,PUT,PATCH,OPTIONS' },
        { name: 'Access-Control-Allow-Headers', value: 'authorization,content-type' }], body: Buffer.from(JSON.stringify(body)).toString('base64') })
  } catch (error) { errors.push(error.message) }
})
async function evaluate(expression) {
  const result = await command('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true })
  if (result.exceptionDetails) throw new Error(JSON.stringify(result.exceptionDetails))
  return result.result.value
}
async function until(expression) {
  for (let attempt = 0; attempt < 400; attempt++) {
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
  if (tripOnly) await command('Emulation.setTouchEmulationEnabled', { enabled: true, maxTouchPoints: 1 })
  await command('Page.addScriptToEvaluateOnNewDocument', { source: `
    localStorage.setItem('sendit-auth', JSON.stringify({state:{accessToken:'ui-test',refreshToken:null,user:{id:1,nickname:'테스트',email:'test@example.com'}},version:0}));
    window.__nativeMessages = [];
    window.SendITNative = {postMessage: value => window.__nativeMessages.push(JSON.parse(value))};
  ` })
  if (tripOnly) {
    await command('Page.navigate', { url: `${base}/itineraries/77` })
    await until("document.querySelectorAll('.timeline-draggable').length === 12")
    assert.ok(await evaluate("matchMedia('(pointer: coarse)').matches"))
    assert.match(await evaluate("getComputedStyle(document.querySelector('.timeline-draggable')).touchAction"), /pan-y/)
    await evaluate("document.querySelector('.timeline-draggable').scrollIntoView({block:'start'})")
    async function swipe() {
      await command('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [{ x: width / 2, y: 550 }] })
      for (let y = 520; y >= 250; y -= 30) {
        await command('Input.dispatchTouchEvent', { type: 'touchMove', touchPoints: [{ x: width / 2, y }] })
        await delay(20)
      }
      await command('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] })
      await delay(500)
    }
    const before = await evaluate('scrollY')
    await swipe()
    const first = await evaluate('scrollY')
    await swipe()
    const second = await evaluate('scrollY')
    assert.ok(first > before && second > first, JSON.stringify({ before, first, second }))
    await evaluate("const transfer = document.querySelector('.trip-transfer-details'); transfer.open = true; transfer.scrollIntoView({block:'center'})")
    await delay(500)
    const cardTop = await evaluate("document.querySelectorAll('.timeline-card-shell')[1].getBoundingClientRect().top + scrollY")
    await evaluate("document.querySelector('.segment-transport-trigger').click()")
    await until("document.querySelectorAll('.segment-transport-menu button').length === 3")
    assert.equal(await evaluate("document.querySelectorAll('.timeline-card-shell')[1].getBoundingClientRect().top + scrollY"), cardTop)
    assert.equal(await evaluate("!!document.querySelector('.segment-transport-select select')"), false)
    await screenshot('trip-transport-menu')
    await evaluate("document.querySelectorAll('.segment-transport-menu button')[1].click()")
    await until("document.querySelector('.segment-transport-trigger').textContent.includes('자동차') && !document.querySelector('.segment-transport-trigger').disabled")
    assert.ok(requests.some((request) => request.startsWith('PUT ') && request.endsWith('/itineraries/77/items/2/transport')), JSON.stringify(requests))
    assert.equal(await evaluate("!!document.querySelector('.segment-transport-menu')"), false)
    assert.deepEqual(errors, [])
    console.log(JSON.stringify({ passed: true, before, first, second, screenshots: artifacts }))
  } else {
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
  if (width > 760) {
    const frame = await evaluate(`(() => {
      const root = document.querySelector('#root').getBoundingClientRect();
      const nav = document.querySelector('.mobile-menu-sheet').getBoundingClientRect();
      return {width: root.width, left: root.left, navWidth: nav.width, navLeft: nav.left};
    })()`)
    assert.equal(frame.width, 430)
    assert.equal(frame.navWidth, frame.width)
    assert.equal(frame.navLeft, frame.left)
  }
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
  await until("!!document.querySelector('.place-grid .pending-saved-post')")
  assert.equal(await evaluate("document.body.textContent.includes('공유한 게시물')"), false)
  if (width > 760) assert.equal(await evaluate("getComputedStyle(document.querySelector('.place-grid')).gridTemplateColumns.split(' ').length"), 2)
  assert.ok(await evaluate("document.querySelector('.place-grid').textContent.includes('저장 완료')"))
  assert.equal(await evaluate("document.querySelector('.pending-post-delete')?.textContent.trim()"), '삭제')
  await evaluate("document.querySelector('.pending-post-delete').click()")
  assert.equal(await evaluate("document.querySelector('.confirm-dialog')?.textContent.includes('이 게시물을 삭제할까요?')"), true)
  await evaluate("document.querySelector('.confirm-dialog-actions button').click()")
  await until("!!document.querySelector('.feed-card-collection') && !document.querySelector('.confirm-dialog')")
  const cardHeightBeforeMenu = await evaluate("document.querySelector('.feed-card-collection').closest('.place-card').getBoundingClientRect().height")
  await evaluate("document.querySelector('.feed-card-collection').click()")
  assert.equal(await evaluate("!!document.querySelector('.feed-card-collection-menu')"), true)
  assert.equal(await evaluate("!!document.querySelector('.feed-card-collection select')"), false)
  const cardHeightAfterMenu = await evaluate("document.querySelector('.feed-card-collection').closest('.place-card').getBoundingClientRect().height")
  assert.equal(cardHeightAfterMenu, cardHeightBeforeMenu, 'Collection overlay must not resize or push feed cards')
  await screenshot('pending-post-in-feed')
  await evaluate("document.querySelector('.feed-load-status').scrollIntoView()")
  await until("document.querySelectorAll('.pending-saved-post').length === 2")
  assert.ok(nextPageRequests > 0, 'Scrolling loads the next page without a button')
  assert.equal(await evaluate("document.body.textContent.includes('이전 게시물 불러오기')"), false)
  await command('Page.navigate', { url: `${base}/saved/places/88` })
  await until("!!document.querySelector('.place-detail-edit-toggle')")
  await evaluate("document.querySelector('.place-detail-edit-toggle').click()")
  await until("document.querySelector('.wide-field input')?.value === '서울 종로구 테스트로 1'")
  assert.ok(await evaluate("document.querySelector('.place-name-lookup-status')?.textContent.includes('일치하는 주소')"))
  assert.deepEqual(errors, [])
  console.log(JSON.stringify({ passed: true, viewport: {width, height}, layout, shareHeight, screenshots: artifacts }, null, 2))
  }
} finally {
  await command('Browser.close').catch(() => {})
  socket.close()
}
