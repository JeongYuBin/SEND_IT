import { useEffect, useState } from 'react'
import type { Itinerary } from './types'

const modes = { WALKING: '도보', CAR: '자동차', PUBLIC_TRANSIT: '대중교통' }

export function TripReminders({ itinerary }: { itinerary: Itinerary }) {
  const [enabled, setEnabled] = useState(false)
  const [message, setMessage] = useState('')
  useEffect(() => {
    if (!enabled) return
    let disposed = false
    const delivered = new Set<string>()
    const check = async () => {
      const now = Date.now()
      for (const day of itinerary.days) {
        for (const item of day.items) {
          const arrival = new Date(`${day.date}T${item.arrivalTime}+09:00`).getTime()
          const departure = arrival - item.travelMinutesFromPrevious * 60_000
          const events = [
            { at: arrival, kind: 'visit', title: `${item.name} 방문 시간`, body: `예정된 방문 ${item.arrivalTime.slice(0, 5)} · 체류 ${item.stayMinutes}분` },
            ...(item.travelMinutesFromPrevious > 0 ? [{ at: departure, kind: 'move', title: `${item.name}으로 이동할 시간`, body: `${modes[item.transportTypeFromPrevious]} · 예상 ${item.travelMinutesFromPrevious}분${item.distanceKmFromPrevious ? `, ${item.distanceKmFromPrevious}km` : ''}` }] : []),
          ]
          for (const event of events) {
            const key = `${itinerary.id}-${item.savedPlaceId}-${event.kind}-${event.at}`
            if (disposed || now < event.at || now - event.at > 60_000 || delivered.has(key)) continue
            delivered.add(key)
            try {
              const registration = await navigator.serviceWorker.getRegistration()
              if (!disposed && registration) await registration.showNotification(event.title, { body: event.body, tag: key, data: { url: `/itineraries/${itinerary.id}` } })
            } catch { if (!disposed) { setEnabled(false); setMessage('알림을 표시하지 못했어요. 브라우저의 알림 권한을 확인해 주세요.') } }
          }
        }
      }
    }
    void check()
    const timer = window.setInterval(() => void check(), 15_000)
    return () => { disposed = true; window.clearInterval(timer) }
  }, [enabled, itinerary])

  const toggle = async () => {
    if (enabled) { setEnabled(false); return }
    try {
      if (!('Notification' in window) || !('serviceWorker' in navigator)) {
        setMessage('이 브라우저는 기기 알림을 지원하지 않아요. 홈 화면에 설치한 앱에서 확인해 주세요.')
        return
      }
      const permission = await Notification.requestPermission()
      if (permission !== 'granted') { setMessage('기기 알림 권한을 허용해야 사용할 수 있어요.'); return }
      const registration = await navigator.serviceWorker.getRegistration()
      if (!registration) { setMessage('앱을 새로고침한 뒤 다시 켜 주세요.'); return }
      setEnabled(true)
      setMessage('')
    } catch { setMessage('알림 설정을 열지 못했어요. 브라우저 설정을 확인해 주세요.') }
  }
  return <section className="trip-reminders">
    <div><strong>여행 알림</strong><small>일정 화면이 열려 있는 동안 방문·이동 시간을 알려드려요.</small></div>
    <button type="button" role="switch" aria-checked={enabled} onClick={() => void toggle()}>{enabled ? '켜짐' : '꺼짐'}</button>
    <small className="trip-reminder-limit">화면을 닫거나 잠그면 알림이 중단될 수 있어요. 실제 위치 추적은 하지 않습니다.</small>
    {message && <small role="status">{message}</small>}
  </section>
}
