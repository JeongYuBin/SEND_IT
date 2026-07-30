import { useMemo, useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AxiosError } from 'axios'
import { Link, useNavigate } from 'react-router-dom'
import { getSavedPlaces } from '../saved/savedApi'
import { createItinerary } from './itineraryApi'
import type { TransportType } from './types'

const transportLabels: Record<TransportType, string> = {
  WALKING: '도보',
  PUBLIC_TRANSIT: '대중교통',
  CAR: '자동차',
}

function localDate(offset = 0) {
  const date = new Date()
  date.setDate(date.getDate() + offset)
  const timezoneOffset = date.getTimezoneOffset() * 60_000
  return new Date(date.getTime() - timezoneOffset).toISOString().slice(0, 10)
}

function errorMessage(error: unknown) {
  const response = (error as AxiosError<{ message?: string }>).response
  return response?.data?.message ?? '여행 계획을 만들지 못했습니다. 입력 내용을 확인해 주세요.'
}

export function ItinerariesPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [title, setTitle] = useState('')
  const [startDateTime, setStartDateTime] = useState(`${localDate()}T10:00`)
  const [endDateTime, setEndDateTime] = useState(`${localDate(1)}T18:00`)
  const [transportType, setTransportType] = useState<TransportType>('PUBLIC_TRANSIT')
  const [selectedIds, setSelectedIds] = useState<number[]>([])

  const placesQuery = useQuery({ queryKey: ['saved-places'], queryFn: getSavedPlaces })
  const placesById = useMemo(
    () => new Map((placesQuery.data ?? []).map((place) => [place.savedPlaceId, place])),
    [placesQuery.data],
  )
  const createMutation = useMutation({
    mutationFn: createItinerary,
    onSuccess: (itinerary) => {
      queryClient.invalidateQueries({ queryKey: ['itineraries'] })
      queryClient.invalidateQueries({ queryKey: ['saved-places'] })
      navigate(`/itineraries/${itinerary.id}`)
    },
  })

  const togglePlace = (id: number) => {
    setSelectedIds((current) => current.includes(id)
      ? current.filter((savedId) => savedId !== id)
      : [...current, id])
  }

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    const [startDate, dailyStartTime] = startDateTime.split('T')
    const [endDate, dailyEndTime] = endDateTime.split('T')
    createMutation.mutate({
      title,
      startDate,
      endDate,
      dailyStartTime,
      dailyEndTime,
      transportType,
      savedPlaceIds: selectedIds,
    })
  }

  return (
    <main className="itinerary-shell">
      <nav className="top-nav">
        <Link className="brand-link" to="/">SEND IT</Link>
        <div>
          <Link to="/saved">저장한 장소</Link>
          <Link to="/">URL 저장하기</Link>
        </div>
      </nav>

      <header className="itinerary-header">
        <span className="eyebrow">TRIP PLANNER</span>
        <h1>여행 계획 만들기</h1>
        <p>저장한 장소를 고르고 기본 일정을 정해 보세요. 장소는 선택한 순서대로 계획에 담깁니다.</p>
      </header>

      <div className="itinerary-layout itinerary-create-layout">
        <form className="itinerary-form" onSubmit={handleSubmit}>
          <h2>기본 일정</h2>
          <label>
            계획 이름
            <input required maxLength={150} value={title} onChange={(event) => setTitle(event.target.value)} placeholder="예: 서울 주말 나들이" />
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

          <div className="itinerary-place-heading">
            <div>
              <h2>장소 선택</h2>
              <p>최대 20개 · 현재 {selectedIds.length}개 선택</p>
            </div>
            {selectedIds.length > 0 && <button type="button" onClick={() => setSelectedIds([])}>선택 해제</button>}
          </div>

          {placesQuery.isLoading && <div className="empty-state">저장 장소를 불러오고 있습니다.</div>}
          {!placesQuery.isLoading && (placesQuery.data?.length ?? 0) === 0 && (
            <div className="empty-state">
              <strong>먼저 장소를 저장해 주세요.</strong>
              <Link to="/">URL로 장소 찾기</Link>
            </div>
          )}
          <div className="itinerary-place-options">
            {placesQuery.data?.map((place) => {
              const order = selectedIds.indexOf(place.savedPlaceId)
              const selected = order >= 0
              return (
                <button
                  className={selected ? 'selected' : ''}
                  disabled={!selected && selectedIds.length >= 20}
                  key={place.savedPlaceId}
                  type="button"
                  onClick={() => togglePlace(place.savedPlaceId)}
                >
                  <span className="selection-order">{selected ? order + 1 : '+'}</span>
                  <span>
                    <strong>{place.name}</strong>
                    <small>{place.roadAddress ?? place.address ?? place.category ?? '장소 정보 없음'}</small>
                  </span>
                </button>
              )
            })}
          </div>

          {selectedIds.length > 0 && (
            <ol className="selected-place-order">
              {selectedIds.map((id) => <li key={id}>{placesById.get(id)?.name}</li>)}
            </ol>
          )}
          {createMutation.isError && <div className="form-error">{errorMessage(createMutation.error)}</div>}
          <button className="primary-button itinerary-submit" disabled={createMutation.isPending || selectedIds.length === 0}>
            {createMutation.isPending ? '계획 만드는 중…' : '여행 계획 만들기'}
          </button>
        </form>

      </div>
    </main>
  )
}
