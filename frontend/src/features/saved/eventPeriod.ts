import type { SavedPlace } from './types'

const dayMilliseconds = 24 * 60 * 60 * 1000

function todayIso() {
  const now = new Date()
  const offset = now.getTimezoneOffset() * 60_000
  return new Date(now.getTime() - offset).toISOString().slice(0, 10)
}

function differenceInDays(from: string, to: string) {
  return Math.round(
    (Date.parse(`${to}T00:00:00Z`) - Date.parse(`${from}T00:00:00Z`)) / dayMilliseconds,
  )
}

export type EventPeriodState = {
  tone: 'upcoming' | 'active' | 'ending' | 'ended'
  label: string
  period: string
  daysUntilEnd: number | null
}

export function eventPeriodState(
  place: Pick<SavedPlace, 'eventStartDate' | 'eventEndDate'>,
  today = todayIso(),
): EventPeriodState | null {
  const { eventStartDate: startDate, eventEndDate: endDate } = place
  if (!startDate && !endDate) return null

  const period = startDate === endDate || !endDate
    ? (startDate ?? endDate!)
    : `${startDate ?? endDate} – ${endDate}`

  if (endDate && endDate < today) {
    return { tone: 'ended', label: '종료된 행사', period, daysUntilEnd: differenceInDays(today, endDate) }
  }
  if (startDate && startDate > today) {
    const daysUntilStart = differenceInDays(today, startDate)
    return {
      tone: 'upcoming',
      label: daysUntilStart === 1 ? '내일 시작' : `${daysUntilStart}일 후 시작`,
      period,
      daysUntilEnd: endDate ? differenceInDays(today, endDate) : null,
    }
  }
  if (endDate) {
    const daysUntilEnd = differenceInDays(today, endDate)
    return {
      tone: daysUntilEnd <= 7 ? 'ending' : 'active',
      label: daysUntilEnd === 0 ? '오늘 종료' : daysUntilEnd <= 7 ? `종료 D-${daysUntilEnd}` : '진행 중',
      period,
      daysUntilEnd,
    }
  }
  return { tone: 'active', label: '진행 중', period, daysUntilEnd: null }
}
