import { useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { SavedPlaceDetailPage } from './SavedPlaceDetailPage'
import './saved-place-sheet.css'

export function SavedPlaceSheet({ placeId, onClose }: { placeId: number; onClose: () => void }) {
  const [expanded, setExpanded] = useState(false)
  const start = useRef<number | null>(null)
  const dragged = useRef(false)
  const closeButton = useRef<HTMLButtonElement>(null)
  useEffect(() => {
    const previousFocus = document.activeElement as HTMLElement | null
    closeButton.current?.focus({ preventScroll: true })
    return () => { previousFocus?.focus({ preventScroll: true }) }
  }, [])
  return createPortal(<section className={`saved-place-sheet${expanded ? ' expanded' : ''}`} role="region" aria-label="선택한 장소 상세 정보">
    <header>
      <button className="saved-place-sheet-handle" type="button" aria-label={expanded ? '장소 정보 접기' : '장소 정보 펼치기'} aria-expanded={expanded}
        onClick={() => { if (!dragged.current) setExpanded(!expanded) }}
        onPointerDown={(event) => { start.current = event.clientY; dragged.current = false; event.currentTarget.setPointerCapture(event.pointerId) }}
        onPointerMove={(event) => {
          if (start.current !== null && Math.abs(event.clientY - start.current) > 30) dragged.current = true
        }}
        onPointerUp={(event) => {
          if (start.current !== null && dragged.current) {
            const delta = event.clientY - start.current
            if (delta > 30) { if (expanded) setExpanded(false); else onClose() }
            else if (delta < -30) setExpanded(true)
          }
          start.current = null
        }} onPointerCancel={() => { start.current = null }}>
        <span /><small>위로 펼치기 · 아래로 내려 접기</small>
      </button>
      <button ref={closeButton} className="saved-place-sheet-close" type="button" onClick={onClose} aria-label="장소 정보 닫고 지도 보기">×</button>
    </header>
    <div className="saved-place-sheet-body" key={placeId}>
      <SavedPlaceDetailPage placeId={placeId} embedded onDeleted={onClose} />
    </div>
  </section>, document.body)
}
