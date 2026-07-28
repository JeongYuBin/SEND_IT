import { useState, type FormEvent } from 'react'
import type { ItineraryItem, UpdateItineraryItemSchedule } from './types'

type Props = {
  item: ItineraryItem
  startDate: string
  endDate: string
  pending: boolean
  onCancel: () => void
  onSave: (request: UpdateItineraryItemSchedule) => void
}

export function PlaceScheduleEditor({
  item,
  startDate,
  endDate,
  pending,
  onCancel,
  onSave,
}: Props) {
  const [visitDate, setVisitDate] = useState(item.preferredVisitDate ?? item.visitDate)
  const [startTime, setStartTime] = useState(
    (item.preferredStartTime ?? item.arrivalTime).slice(0, 5),
  )
  const [stayMinutes, setStayMinutes] = useState(item.stayMinutes)

  const submit = (event: FormEvent) => {
    event.preventDefault()
    onSave({ visitDate, startTime, stayMinutes })
  }

  return (
    <form className="place-schedule-form" onSubmit={submit}>
      <label>
        방문일
        <input required type="date" min={startDate} max={endDate} value={visitDate} onChange={(event) => setVisitDate(event.target.value)} />
      </label>
      <label>
        방문 시작
        <input required type="time" value={startTime} onChange={(event) => setStartTime(event.target.value)} />
      </label>
      <label>
        체류 시간
        <select value={stayMinutes} onChange={(event) => setStayMinutes(Number(event.target.value))}>
          {[30, 45, 60, 90, 120, 180, 240].map((minutes) => <option key={minutes} value={minutes}>{minutes}분</option>)}
        </select>
      </label>
      <button type="button" onClick={onCancel}>취소</button>
      <button className="primary-button" disabled={pending}>적용</button>
    </form>
  )
}
