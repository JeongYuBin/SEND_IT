import { useState, type FormEvent } from 'react'
import type { Itinerary, TransportType, UpdateItinerary } from './types'

const transportLabels: Record<TransportType, string> = {
  WALKING: '도보',
  PUBLIC_TRANSIT: '대중교통',
  CAR: '자동차',
}

type Props = {
  itinerary: Itinerary
  pending: boolean
  onCancel: () => void
  onSave: (request: UpdateItinerary) => void
}

export function ItineraryEditPanel({ itinerary, pending, onCancel, onSave }: Props) {
  const [title, setTitle] = useState(itinerary.title)
  const [startDate, setStartDate] = useState(itinerary.startDate)
  const [endDate, setEndDate] = useState(itinerary.endDate)
  const [dailyStartTime, setDailyStartTime] = useState(itinerary.dailyStartTime.slice(0, 5))
  const [dailyEndTime, setDailyEndTime] = useState(itinerary.dailyEndTime.slice(0, 5))
  const [transportType, setTransportType] = useState(itinerary.transportType)

  const submit = (event: FormEvent) => {
    event.preventDefault()
    onSave({ title, startDate, endDate, dailyStartTime, dailyEndTime, transportType })
  }

  return (
    <form className="itinerary-edit-form" onSubmit={submit}>
      <label>
        계획 이름
        <input required maxLength={150} value={title} onChange={(event) => setTitle(event.target.value)} />
      </label>
      <div className="itinerary-field-row">
        <label>시작일<input required type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)} /></label>
        <label>종료일<input required type="date" min={startDate} value={endDate} onChange={(event) => setEndDate(event.target.value)} /></label>
      </div>
      <div className="itinerary-field-row">
        <label>하루 시작<input required type="time" value={dailyStartTime} onChange={(event) => setDailyStartTime(event.target.value)} /></label>
        <label>하루 종료<input required type="time" value={dailyEndTime} onChange={(event) => setDailyEndTime(event.target.value)} /></label>
      </div>
      <label>
        이동 수단
        <select value={transportType} onChange={(event) => setTransportType(event.target.value as TransportType)}>
          {Object.entries(transportLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select>
      </label>
      <div className="edit-actions">
        <button type="button" onClick={onCancel}>취소</button>
        <button className="primary-button" disabled={pending}>{pending ? '저장 중…' : '변경 저장'}</button>
      </div>
    </form>
  )
}
