import { useState, type FormEvent } from 'react'
import type { ItineraryItem, UpdateItineraryItemSchedule } from './types'

type Props = {
  item: ItineraryItem
  startDate: string
  endDate: string
  pending: boolean
  errorMessage?: string | null
  onCancel: () => void
  onSave: (request: UpdateItineraryItemSchedule) => void
}

export function PlaceScheduleEditor({
  item,
  startDate,
  endDate,
  pending,
  errorMessage,
  onCancel,
  onSave,
}: Props) {
  const [visitDate, setVisitDate] = useState(item.preferredVisitDate ?? item.visitDate)
  const [startTime, setStartTime] = useState(
    (item.preferredStartTime ?? item.arrivalTime).slice(0, 5),
  )
  const [stayMinutes, setStayMinutes] = useState(String(item.stayMinutes || 60))

  const submit = (event: FormEvent) => {
    event.preventDefault()
    onSave({ visitDate, startTime, stayMinutes: Number(stayMinutes) })
  }

  return (
    <form className="place-schedule-form" onSubmit={submit}>
      <label>
        방문일
        <input required type="date" min={startDate} max={endDate} value={visitDate} onChange={(event) => setVisitDate(event.target.value)} />
      </label>
      <label>
        방문 시작
        <input required type="text" inputMode="text" pattern="([01][0-9]|2[0-3]):[0-5][0-9]" placeholder="14:40" title="24시간 형식으로 입력해 주세요. 예: 14:40" value={startTime} onChange={(event) => setStartTime(event.target.value)} />
      </label>
      <label>
        체류 시간
        <span className="stay-minutes-input">
          <input
            required
            type="number"
            min={15}
            max={720}
            step={1}
            inputMode="numeric"
            value={stayMinutes}
            onChange={(event) => setStayMinutes(event.target.value)}
            aria-describedby={`stay-minutes-help-${item.savedPlaceId}`}
          />
          <span aria-hidden="true">분</span>
        </span>
        <small id={`stay-minutes-help-${item.savedPlaceId}`}>15분부터 720분까지 입력할 수 있습니다.</small>
      </label>
      <div className="schedule-form-actions">
        <button type="button" onClick={onCancel}>취소</button>
        <button type="submit" className="primary-button" disabled={pending}>
          {pending ? '적용 중…' : '적용'}
        </button>
      </div>
      {errorMessage && <div className="form-error schedule-form-error">{errorMessage}</div>}
    </form>
  )
}
