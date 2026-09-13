import { useMemo, useState, type FormEvent } from 'react'
import { DirectTimeInput } from './DirectTimeInput'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AxiosError } from 'axios'
import { Link, useNavigate } from 'react-router-dom'
import { getSavedPlaces } from '../saved/savedApi'
import { createItinerary } from './itineraryApi'
import type { TransportType } from './types'
import { PlaceImage } from '../../components/PlaceImage'

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

function addDays(dateValue: string, offset: number) {
  const date = new Date(`${dateValue}T12:00:00`)
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
  const [regionFilter, setRegionFilter] = useState('all')
  const [districtFilter, setDistrictFilter] = useState('all')
  const [categoryFilter, setCategoryFilter] = useState('all')
  const [search, setSearch] = useState('')
  const [filtersOpen, setFiltersOpen] = useState(false)

  const [startDate, startTime] = startDateTime.split('T')
  const [endDate, endTime] = endDateTime.split('T')
  const dateRangeIsValid = new Date(endDateTime).getTime() > new Date(startDateTime).getTime()

  const updateStartDateTime = (nextDate: string, nextTime: string) => {
    const nextStart = `${nextDate}T${nextTime}`
    setStartDateTime(nextStart)
    if (new Date(nextStart).getTime() >= new Date(endDateTime).getTime()) {
      setEndDateTime(`${addDays(nextDate, 1)}T${endTime}`)
    }
  }

  const placesQuery = useQuery({ queryKey: ['saved-places'], queryFn: getSavedPlaces })
  const placesById = useMemo(
    () => new Map((placesQuery.data ?? []).map((place) => [place.savedPlaceId, place])),
    [placesQuery.data],
  )
  const categoryOptions = useMemo(
    () => [...new Set((placesQuery.data ?? []).map((place) => place.collectionName).filter(Boolean) as string[])].sort(),
    [placesQuery.data],
  )
  const regionOptions = useMemo(
    () => [...new Set((placesQuery.data ?? []).map((place) => {
      const address = place.roadAddress ?? place.address
      return address?.trim().split(/\s+/)[0] ?? null
    }).filter(Boolean) as string[])].sort(),
    [placesQuery.data],
  )
  const districtOptions = useMemo(
    () => [...new Set((placesQuery.data ?? []).map((place) => {
      const parts = (place.roadAddress ?? place.address)?.trim().split(/\s+/) ?? []
      if (regionFilter !== 'all' && parts[0] !== regionFilter) return null
      return parts.slice(1).find((part) => /(?:시|군|구)$/.test(part)) ?? null
    }).filter(Boolean) as string[])].sort(),
    [placesQuery.data, regionFilter],
  )
  const filteredPlaces = useMemo(
    () => (placesQuery.data ?? []).filter((place) => {
      const parts = (place.roadAddress ?? place.address)?.trim().split(/\s+/) ?? []
      const region = parts[0] ?? null
      const district = parts.slice(1).find((part) => /(?:시|군|구)$/.test(part)) ?? null
      return (regionFilter === 'all' || region === regionFilter)
        && (districtFilter === 'all' || district === districtFilter)
        && (categoryFilter === 'all' || place.collectionName === categoryFilter)
        && `${place.name} ${place.roadAddress ?? place.address ?? ''} ${place.collectionName ?? ''}`.toLocaleLowerCase().includes(search.trim().toLocaleLowerCase())
    }),
    [categoryFilter, districtFilter, placesQuery.data, regionFilter, search],
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
      : current.length < 20 ? [...current, id] : current)
  }

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    const [, dailyStartTime] = startDateTime.split('T')
    const [, dailyEndTime] = endDateTime.split('T')
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
    <main className="itinerary-shell trip-create">
      <nav className="top-nav">
        <Link className="brand-link" to="/">SEND IT</Link>
        <div>
          <Link to="/saved">저장한 장소</Link>
          <Link to="/profile">내 정보</Link>
          <Link to="/settings">설정</Link>
          <Link to="/">URL 저장하기</Link>
        </div>
      </nav>

      <header className="itinerary-header">
        <span className="eyebrow">TRIP PLANNER</span>
        <h1>여행 계획 만들기</h1>
        <p>가고 싶은 곳을 모아, 나만의 여행으로.</p>
      </header>

      <div className="itinerary-layout itinerary-create-layout">
        <form className="itinerary-form" onSubmit={handleSubmit}>
          <section className="trip-basics">
          <h2><span className="trip-step">01</span> 어떤 여행을 떠날까요?</h2>
          <label>
            계획 이름
            <input required maxLength={150} value={title} onChange={(event) => setTitle(event.target.value)} placeholder="예: 서울 주말 나들이" />
          </label>
          <div className="itinerary-field-row trip-schedule">
            <fieldset className="trip-date-block">
              <legend><span>출발</span> 여행 시작</legend>
              <div className="trip-date-time-grid">
                <label><span>날짜</span><input required aria-label="여행 시작 날짜" type="date" value={startDate}
                  onChange={(event) => updateStartDateTime(event.target.value, startTime)} /></label>
                <div className="trip-time-field"><span>시간 직접 입력</span><DirectTimeInput label="여행 시작 시간" value={startTime}
                  onChange={(time) => updateStartDateTime(startDate, time)} /></div>
              </div>
            </fieldset>
            <fieldset className="trip-date-block">
              <legend><span>도착</span> 여행 종료</legend>
              <div className="trip-date-time-grid">
                <label><span>날짜</span><input required aria-label="여행 종료 날짜" type="date" min={startDate} value={endDate}
                  onChange={(event) => setEndDateTime(`${event.target.value}T${endTime}`)} /></label>
                <div className="trip-time-field"><span>시간 직접 입력</span><DirectTimeInput label="여행 종료 시간" value={endTime}
                  onChange={(time) => setEndDateTime(`${endDate}T${time}`)} /></div>
              </div>
            </fieldset>
            {!dateRangeIsValid && <p className="trip-date-error" role="alert">종료 일시는 시작 일시보다 늦게 선택해 주세요.</p>}
          </div>
          <fieldset className="trip-transport">
            <legend>이동 수단</legend>
            <div>{Object.entries(transportLabels).map(([value, label]) => (
              <button type="button" key={value} aria-pressed={transportType === value}
                onClick={() => setTransportType(value as TransportType)}>{label}</button>
            ))}</div>
          </fieldset>
          </section>

          <section className="trip-places">
          <div className="itinerary-place-heading">
            <div>
              <h2><span className="trip-step">02</span> 어디로 갈까요?</h2>
              <p>선택한 순서로 담겨요 · {selectedIds.length}/20곳</p>
            </div>
            {selectedIds.length > 0 && <button type="button" onClick={() => setSelectedIds([])}>선택 해제</button>}
          </div>
          {selectedIds.length > 0 && (
            <ol className="trip-selected" aria-label="선택한 장소 순서">
              {selectedIds.map((id, index) => <li key={id}>
                <button type="button" onClick={() => togglePlace(id)} aria-label={`${placesById.get(id)?.name} 선택 해제`}>
                  <span>{index + 1}</span>{placesById.get(id)?.name}<span aria-hidden="true">×</span>
                </button>
              </li>)}
            </ol>
          )}

          {(placesQuery.data?.length ?? 0) > 0 && (
            <div className="trip-search-tools">
            <label className="trip-search"><span className="sr-only">저장한 장소 검색</span>
              <input type="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="장소 이름, 지역으로 검색" />
            </label>
            <button className="trip-filter-toggle" type="button" aria-expanded={filtersOpen} onClick={() => setFiltersOpen(open => !open)}>
              {filtersOpen ? '필터 닫기' : '필터'}{[regionFilter, districtFilter, categoryFilter].filter(value => value !== 'all').length > 0 ? ' · 적용 중' : ''}
            </button>
            {filtersOpen && (
            <section className="itinerary-place-filters" aria-label="장소 선택 필터">
              <label>
                지역
                <select value={regionFilter} onChange={(event) => {
                  setRegionFilter(event.target.value)
                  setDistrictFilter('all')
                }}>
                  <option value="all">전체 지역</option>
                  {regionOptions.map((region) => <option key={region} value={region}>{region}</option>)}
                </select>
              </label>
              <label>
                시·군·구
                <select value={districtFilter} onChange={(event) => setDistrictFilter(event.target.value)}>
                  <option value="all">전체 시·군·구</option>
                  {districtOptions.map((district) => <option key={district} value={district}>{district}</option>)}
                </select>
              </label>
              <label>
                컬렉션
                <select value={categoryFilter} onChange={(event) => setCategoryFilter(event.target.value)}>
                  <option value="all">전체 컬렉션</option>
                  {categoryOptions.map((category) => <option key={category} value={category}>{category}</option>)}
                </select>
              </label>
              <div className="itinerary-place-filter-summary">
                {(placesQuery.data?.length ?? 0)}개 중 {filteredPlaces.length}개
              </div>
            </section>
            )}
            <span className="trip-result-count">저장한 장소 {filteredPlaces.length}곳</span>
            </div>
          )}

          {placesQuery.isError && <div className="empty-state" role="alert">장소를 불러오지 못했어요. <button type="button" onClick={() => void placesQuery.refetch()}>다시 시도</button></div>}
          {placesQuery.isLoading && <div className="empty-state">저장 장소를 불러오고 있습니다.</div>}
          {!placesQuery.isLoading && !placesQuery.isError && (placesQuery.data?.length ?? 0) === 0 && (
            <div className="empty-state">
              <strong>먼저 장소를 저장해 주세요.</strong>
              <Link to="/">URL로 장소 찾기</Link>
            </div>
          )}
          <div className="itinerary-place-options">
            {filteredPlaces.map((place) => {
              const order = selectedIds.indexOf(place.savedPlaceId)
              const selected = order >= 0
              return (
                <button
                  className={selected ? 'selected' : ''}
                  disabled={!selected && selectedIds.length >= 20}
                  key={place.savedPlaceId}
                  type="button"
                  aria-pressed={selected}
                  onClick={() => togglePlace(place.savedPlaceId)}
                >
                  <span className="selection-order">{selected ? order + 1 : '+'}</span>
                  <PlaceImage src={place.imageUrl} alt={`${place.name} 대표 이미지`} category={place.collectionName} className="itinerary-option-image" />
                  <span className="itinerary-option-copy">
                    <small>{place.collectionName ?? '기타'}</small>
                    <strong>{place.name}</strong>
                    <small>{place.roadAddress ?? place.address ?? '주소 정보 없음'}</small>
                  </span>
                </button>
              )
            })}
          </div>
          {!placesQuery.isLoading && (placesQuery.data?.length ?? 0) > 0 && filteredPlaces.length === 0 && (
            <div className="empty-state">선택한 필터에 맞는 장소가 없습니다.</div>
          )}

          </section>
          <footer className="trip-create-footer">
          <p aria-live="polite">{selectedIds.length ? `${selectedIds.length}곳을 담았어요` : '함께할 장소를 선택해 주세요'}</p>
          {createMutation.isError && <div className="form-error">{errorMessage(createMutation.error)}</div>}
          <button className="primary-button itinerary-submit" disabled={createMutation.isPending || selectedIds.length === 0 || !dateRangeIsValid}>
            {createMutation.isPending ? '계획 만드는 중…' : '여행 계획 만들기'}
          </button>
          </footer>
        </form>

      </div>
    </main>
  )
}
