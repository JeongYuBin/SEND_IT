import { useEffect, useState } from 'react'
import type { Itinerary, ItineraryItem, ReorderItineraryItem } from './types'

type Props = {
  itinerary: Itinerary
  pending: boolean
  errorMessage?: string | null
  onCancel: () => void
  onSave: (items: ReorderItineraryItem[]) => void
}

type DayItems = Record<string, ItineraryItem[]>

export function ItineraryOrderEditor({
  itinerary,
  pending,
  errorMessage,
  onCancel,
  onSave,
}: Props) {
  const [days, setDays] = useState<DayItems>({})
  const [draggingId, setDraggingId] = useState<number | null>(null)

  useEffect(() => {
    setDays(Object.fromEntries(
      itinerary.days.map((day) => [day.date, [...day.items]]),
    ))
  }, [itinerary])

  const move = (date: string, targetIndex: number) => {
    if (draggingId === null) return
    setDays((current) => {
      let dragged: ItineraryItem | undefined
      const next = Object.fromEntries(Object.entries(current).map(([day, items]) => {
        const found = items.find((item) => item.savedPlaceId === draggingId)
        if (found) dragged = found
        return [day, items.filter((item) => item.savedPlaceId !== draggingId)]
      }))
      if (!dragged) return current
      next[date].splice(Math.min(targetIndex, next[date].length), 0, dragged)
      return next
    })
  }

  const save = () => {
    let sequence = 1
    const items = itinerary.days.flatMap((day) =>
      (days[day.date] ?? []).map((item) => ({
        savedPlaceId: item.savedPlaceId,
        visitDate: day.date,
        sequence: sequence++,
      })),
    )
    onSave(items)
  }

  return (
    <section className="order-editor">
      <header>
        <div>
          <span className="eyebrow">DRAG & DROP</span>
          <h2>날짜와 방문 순서 편집</h2>
          <p>카드를 끌어서 같은 날짜의 순서를 바꾸거나 다른 날짜로 옮겨보세요.</p>
        </div>
        <div className="edit-actions">
          <button type="button" onClick={onCancel}>취소</button>
          <button type="button" className="primary-button" disabled={pending} onClick={save}>
            {pending ? '순서 저장 중…' : '순서 저장'}
          </button>
        </div>
      </header>
      <div className="order-editor-days">
        {itinerary.days.map((day) => (
          <div
            className="order-editor-day"
            key={day.date}
            onDragOver={(event) => event.preventDefault()}
            onDrop={() => move(day.date, (days[day.date] ?? []).length)}
          >
            <strong>DAY {day.dayNumber} · {day.date}</strong>
            <div>
              {(days[day.date] ?? []).map((item, index) => (
                <article
                  draggable
                  className={draggingId === item.savedPlaceId ? 'dragging' : ''}
                  key={item.savedPlaceId}
                  onDragStart={() => setDraggingId(item.savedPlaceId)}
                  onDragEnd={() => setDraggingId(null)}
                  onDragOver={(event) => event.preventDefault()}
                  onDrop={(event) => {
                    event.stopPropagation()
                    move(day.date, index)
                  }}
                >
                  <span className="drag-handle" aria-hidden="true">⠿</span>
                  <span>{index + 1}</span>
                  <div>
                    <strong>{item.name}</strong>
                    <small>{item.address ?? '주소 정보 없음'}</small>
                  </div>
                </article>
              ))}
              {(days[day.date] ?? []).length === 0 && <p>이 날짜로 카드를 놓으세요.</p>}
            </div>
          </div>
        ))}
      </div>
      {errorMessage && <div className="form-error">{errorMessage}</div>}
    </section>
  )
}
