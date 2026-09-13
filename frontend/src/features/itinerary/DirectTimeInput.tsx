import { useEffect, useState } from 'react'

type Period = 'AM' | 'PM'

export function DirectTimeInput({ value, onChange, label }: { value: string; onChange: (value: string) => void; label: string }) {
  const [valueHour = '10', valueMinute = '00'] = value.split(':')
  const toDisplayHour = (hour: string) => String((Number(hour) % 12) || 12).padStart(2, '0')
  const [hour, setHour] = useState(toDisplayHour(valueHour))
  const [minute, setMinute] = useState(valueMinute)
  const [period, setPeriod] = useState<Period>(Number(valueHour) >= 12 ? 'PM' : 'AM')

  useEffect(() => {
    setHour(toDisplayHour(valueHour))
    setMinute(valueMinute)
    setPeriod(Number(valueHour) >= 12 ? 'PM' : 'AM')
  }, [valueHour, valueMinute])

  const commit = (nextHour = hour, nextMinute = minute, nextPeriod = period) => {
    const safeHour = Math.min(12, Math.max(1, Number(nextHour) || 1))
    const safeMinute = Math.min(59, Math.max(0, Number(nextMinute) || 0))
    const hour24 = nextPeriod === 'AM' ? safeHour % 12 : (safeHour % 12) + 12
    const normalizedHour = String(safeHour).padStart(2, '0')
    const normalizedMinute = String(safeMinute).padStart(2, '0')
    setHour(normalizedHour)
    setMinute(normalizedMinute)
    onChange(`${String(hour24).padStart(2, '0')}:${normalizedMinute}`)
  }

  const digitsOnly = (input: string) => input.replace(/\D/g, '').slice(0, 2)

  return (
    <div className="trip-direct-time" aria-label={label}>
      <div className="trip-period" aria-label="오전 또는 오후">
        {(['AM', 'PM'] as Period[]).map((item) => (
          <button key={item} type="button" aria-pressed={period === item} onClick={() => {
            setPeriod(item)
            commit(hour, minute, item)
          }}>{item === 'AM' ? '오전' : '오후'}</button>
        ))}
      </div>
      <label><span className="sr-only">시</span><input type="text" inputMode="numeric" pattern="[0-9]*" maxLength={2}
        value={hour} onFocus={(event) => event.currentTarget.select()} onChange={(event) => setHour(digitsOnly(event.target.value))}
        onBlur={() => commit()} aria-label={`${label} 시`} /></label>
      <span aria-hidden="true">:</span>
      <label><span className="sr-only">분</span><input type="text" inputMode="numeric" pattern="[0-9]*" maxLength={2}
        value={minute} onFocus={(event) => event.currentTarget.select()} onChange={(event) => setMinute(digitsOnly(event.target.value))}
        onBlur={() => commit()} aria-label={`${label} 분`} /></label>
    </div>
  )
}
