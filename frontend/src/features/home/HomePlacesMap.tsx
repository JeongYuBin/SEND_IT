import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { KakaoMap, type KakaoMapPoint } from '../../components/KakaoMap'
import type { SavedPlace } from '../saved/types'

const defaultFilters = [
  { label: '전체', pattern: /.*/ },
  { label: '음식점', pattern: /음식|식당|맛집|한식|중식|일식|양식/ },
  { label: '카페', pattern: /카페|커피|디저트/ },
  { label: '숙소', pattern: /숙소|호텔|펜션|게스트|리조트/ },
]

const CUSTOM_FILTERS_KEY = 'sendit-map-custom-filters'

function storedCustomFilters() {
  try {
    const value = JSON.parse(localStorage.getItem(CUSTOM_FILTERS_KEY) ?? '[]')
    return Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string') : []
  } catch {
    return []
  }
}

export function HomePlacesMap({ places }: { places: SavedPlace[] }) {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const focused = places.find((place) => place.savedPlaceId === Number(params.get('place')))
  const focusedCenter = useMemo(() => focused?.latitude != null && focused.longitude != null
    ? { latitude: focused.latitude, longitude: focused.longitude } : null, [focused?.latitude, focused?.longitude])
  const [filter, setFilter] = useState('전체')
  const [customFilters, setCustomFilters] = useState<string[]>(storedCustomFilters)
  const [addingFilter, setAddingFilter] = useState(false)
  const [newFilter, setNewFilter] = useState('')
  const [filterError, setFilterError] = useState('')
  const addButtonRef = useRef<HTMLButtonElement>(null)
  const filterPopoverRef = useRef<HTMLFormElement>(null)
  const [currentLocation, setCurrentLocation] = useState({ latitude: 37.5665, longitude: 126.978 })

  useEffect(() => {
    if (!addingFilter) return
    const closeOnOutside = (event: PointerEvent) => {
      const target = event.target as Node
      if (addButtonRef.current?.contains(target) || filterPopoverRef.current?.contains(target)) return
      setAddingFilter(false)
      setNewFilter('')
      setFilterError('')
    }
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key !== 'Escape') return
      setAddingFilter(false)
      setNewFilter('')
      setFilterError('')
      addButtonRef.current?.focus()
    }
    document.addEventListener('pointerdown', closeOnOutside)
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('pointerdown', closeOnOutside)
      document.removeEventListener('keydown', closeOnEscape)
    }
  }, [addingFilter])

  const addFilter = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const name = newFilter.trim().replace(/\s+/g, ' ').slice(0, 20)
    if (!name) {
      setFilterError('카테고리 이름을 입력해 주세요.')
      return
    }
    if ([...defaultFilters.map((item) => item.label), ...customFilters].includes(name)) {
      setFilter(name)
      setAddingFilter(false)
      setFilterError('')
      return
    }
    const next = [...customFilters, name]
    setCustomFilters(next)
    localStorage.setItem(CUSTOM_FILTERS_KEY, JSON.stringify(next))
    setFilter(name)
    setNewFilter('')
    setFilterError('')
    setAddingFilter(false)
  }

  useEffect(() => {
    if (params.has('place') || !navigator.geolocation) return
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => setCurrentLocation({ latitude: coords.latitude, longitude: coords.longitude }),
      () => undefined,
      { enableHighAccuracy: true, timeout: 8000, maximumAge: 60_000 },
    )
  }, [params])
  const points = useMemo<KakaoMapPoint[]>(() => {
    const selected = defaultFilters.find((item) => item.label === filter)
    return places
      .filter((place) => place.latitude !== null && place.longitude !== null)
      .filter((place) => selected
        ? selected.pattern.test(place.category ?? '')
        : (place.category ?? '').toLocaleLowerCase('ko').includes(filter.toLocaleLowerCase('ko')))
      .map((place) => ({
        id: place.savedPlaceId,
        name: place.name,
        label: place.name,
        category: place.category,
        latitude: place.latitude!,
        longitude: place.longitude!,
      }))
  }, [filter, places])

  return (
    <section className="home-map" aria-label="저장한 장소 지도">
      <div className="home-map-filters" role="group" aria-label="장소 카테고리 필터">
        {[...defaultFilters.map((item) => item.label), ...customFilters].map((label) => (
          <button
            key={label}
            type="button"
            className={filter === label ? 'active' : ''}
            onClick={() => setFilter(label)}
          >{label}</button>
        ))}
        <button
          ref={addButtonRef}
          className="map-filter-add"
          type="button"
          aria-label="지도 카테고리 추가"
          aria-expanded={addingFilter}
          onClick={() => { setAddingFilter((open) => !open); setFilterError('') }}
        >+</button>
      </div>
      {addingFilter && (
        <form ref={filterPopoverRef} className="map-filter-popover" onSubmit={addFilter}>
          <label htmlFor="new-map-filter">지도에서 볼 카테고리</label>
          <div>
            <input
              id="new-map-filter"
              autoFocus
              maxLength={20}
              value={newFilter}
              onChange={(event) => { setNewFilter(event.target.value); setFilterError('') }}
              placeholder="예: 문화시설"
            />
            <button type="submit">추가</button>
          </div>
          {filterError && <small role="alert">{filterError}</small>}
        </form>
      )}
      <KakaoMap
        ariaLabel={`${filter} 저장 장소 ${points.length}곳`}
        points={points}
        initialCenter={focusedCenter ?? currentLocation}
        fitPoints={false}
        onSelect={(point) => navigate(`/saved/places/${point.id}`)}
      />
      <div className="home-map-summary">
        <span>MY PLACES</span>
        <strong>{focused?.name ?? `저장한 장소 ${points.length}곳`}</strong>
        <button type="button" onClick={() => navigate(focused ? `/saved/places/${focused.savedPlaceId}` : '/saved')}>{focused ? '장소 상세' : '게시물 보기'}</button>
      </div>
    </section>
  )
}
