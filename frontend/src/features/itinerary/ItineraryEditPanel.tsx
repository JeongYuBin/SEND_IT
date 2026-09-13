import { useState, type FormEvent } from 'react'
import { DirectTimeInput } from './DirectTimeInput'
import type { Itinerary, TransportType, UpdateItinerary } from './types'

const transportLabels: Record<TransportType, string> = {
  WALKING: '도보',
  PUBLIC_TRANSIT: '대중교통',
  CAR: '자동차',
}

type Props = {
  itinerary: Itinerary
  pending: boolean
  errorMessage?: string | null
  onCancel: () => void
  onSave: (request: UpdateItinerary) => void
}

export function ItineraryEditPanel({
  itinerary,
  pending,
  errorMessage,
  onCancel,
  onSave,
}: Props) {
  const [title, setTitle] = useState(itinerary.title)
  const [startDateTime, setStartDateTime] = useState(
    `${itinerary.startDate}T${itinerary.dailyStartTime.slice(0, 5)}`,
  )
  const [endDateTime, setEndDateTime] = useState(
    `${itinerary.endDate}T${itinerary.dailyEndTime.slice(0, 5)}`,
  )
  const [transportType, setTransportType] = useState(itinerary.transportType)

  const submit = (event: FormEvent) => {
    event.preventDefault()
    const [startDate, dailyStartTime] = startDateTime.split('T')
    const [endDate, dailyEndTime] = endDateTime.split('T')
    onSave({ title, startDate, endDate, dailyStartTime, dailyEndTime, transportType })
  }

  return (
    <form className="itinerary-edit-form" onSubmit={submit}>
      <label>
        계획 이름
        <input required maxLength={150} value={title} onChange={(event) => setTitle(event.target.value)} />
      </label>
      {[{ title: '출발 · 여행 시작', value: startDateTime, set: setStartDateTime }, { title: '도착 · 여행 종료', value: endDateTime, set: setEndDateTime }].map((field, index) => (
        <fieldset className="trip-date-block" key={field.title}>
          <legend>{field.title}</legend>
          <div className="trip-date-time-grid">
            <label>날짜<input type="date" required min={index === 1 ? startDateTime.split('T')[0] : undefined} value={field.value.split('T')[0]} onChange={event => field.set(`${event.target.value}T${field.value.split('T')[1]}`)} /></label>
            <div className="trip-time-field"><span>시간 직접 입력</span><DirectTimeInput value={field.value.split('T')[1]} label={field.title} onChange={value => field.set(`${field.value.split('T')[0]}T${value}`)} /></div>
          </div>
        </fieldset>
      ))}
      <label>
        이동 수단
        <select value={transportType} onChange={(event) => setTransportType(event.target.value as TransportType)}>
          {Object.entries(transportLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select>
      </label>
      <div className="edit-actions">
        <button type="button" onClick={onCancel}>취소</button>
        <button type="submit" className="primary-button" disabled={pending || endDateTime <= startDateTime}>
          {pending ? '저장 중…' : '변경 저장'}
        </button>
      </div>
      {endDateTime <= startDateTime && <div className="form-error">종료 일시는 시작 일시보다 늦게 선택해 주세요.</div>}
      {errorMessage && <div className="form-error">{errorMessage}</div>}
    </form>
  )
}
