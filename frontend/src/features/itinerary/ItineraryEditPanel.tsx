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
      <div className="itinerary-field-row">
        <label>
          여행 시작 일시
          <input
            required
            type="datetime-local"
            value={startDateTime}
            onChange={(event) => setStartDateTime(event.target.value)}
          />
        </label>
        <label>
          여행 종료 일시
          <input
            required
            type="datetime-local"
            min={startDateTime}
            value={endDateTime}
            onChange={(event) => setEndDateTime(event.target.value)}
          />
        </label>
      </div>
      <label>
        이동 수단
        <select value={transportType} onChange={(event) => setTransportType(event.target.value as TransportType)}>
          {Object.entries(transportLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select>
      </label>
      <div className="edit-actions">
        <button type="button" onClick={onCancel}>취소</button>
        <button type="submit" className="primary-button" disabled={pending}>
          {pending ? '저장 중…' : '변경 저장'}
        </button>
      </div>
      {errorMessage && <div className="form-error">{errorMessage}</div>}
    </form>
  )
}
