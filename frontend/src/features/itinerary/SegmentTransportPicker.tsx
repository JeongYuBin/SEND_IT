import { useEffect, useId, useRef, useState } from 'react'
import type { TransportType } from './types'

const options: { value: TransportType; label: string }[] = [
  { value: 'PUBLIC_TRANSIT', label: '대중교통' },
  { value: 'CAR', label: '자동차' },
  { value: 'WALKING', label: '도보' },
]

export function SegmentTransportPicker({ value, disabled, onChange }: {
  value: TransportType
  disabled: boolean
  onChange: (value: TransportType) => void
}) {
  const [open, setOpen] = useState(false)
  const root = useRef<HTMLDivElement>(null)
  const trigger = useRef<HTMLButtonElement>(null)
  const menuId = useId()
  useEffect(() => {
    if (!open) return
    const dismiss = (event: PointerEvent) => {
      if (!root.current?.contains(event.target as Node)) setOpen(false)
    }
    document.addEventListener('pointerdown', dismiss)
    root.current?.querySelector<HTMLButtonElement>('[aria-checked="true"]')?.focus({ preventScroll: true })
    return () => document.removeEventListener('pointerdown', dismiss)
  }, [open])

  return <div className="segment-transport-select">
    <span>이 구간 이동수단</span>
    <div className="segment-transport-picker" ref={root}
      onBlur={(event) => {
        if (!event.currentTarget.contains(event.relatedTarget)) setOpen(false)
      }}
      onKeyDown={(event) => {
        if (event.key === 'Escape') {
          event.stopPropagation()
          setOpen(false)
          trigger.current?.focus({ preventScroll: true })
        }
        if (['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) {
          event.preventDefault()
          if (!open) { setOpen(true); return }
          const buttons = Array.from(root.current?.querySelectorAll<HTMLButtonElement>('[role="menuitemradio"]') ?? [])
          const index = buttons.indexOf(document.activeElement as HTMLButtonElement)
          const next = event.key === 'Home' ? 0 : event.key === 'End' ? buttons.length - 1
            : (index + (event.key === 'ArrowDown' ? 1 : -1) + buttons.length) % buttons.length
          buttons[next]?.focus({ preventScroll: true })
        }
      }}>
      <button type="button" className="segment-transport-trigger" ref={trigger}
        disabled={disabled} aria-haspopup="menu" aria-expanded={open}
        aria-controls={menuId} aria-label={`이 구간 이동수단: ${options.find((option) => option.value === value)?.label}`}
        onClick={() => setOpen((previous) => !previous)}>
        {options.find((option) => option.value === value)?.label}
        <svg aria-hidden="true" viewBox="0 0 16 16"><path d={open ? 'm4 10 4-4 4 4' : 'm4 6 4 4 4-4'} /></svg>
      </button>
      {open && <div className="segment-transport-menu" id={menuId} role="menu" aria-label="이동수단 선택">
        {options.map((option) => <button type="button" key={option.value}
          role="menuitemradio" aria-checked={value === option.value} disabled={disabled}
          tabIndex={value === option.value ? 0 : -1}
          onClick={() => {
            setOpen(false)
            trigger.current?.focus({ preventScroll: true })
            if (value !== option.value) onChange(option.value)
          }}>
          <span>{option.label}</span><span aria-hidden="true">{value === option.value ? '✓' : ''}</span>
        </button>)}
      </div>}
    </div>
  </div>
}
