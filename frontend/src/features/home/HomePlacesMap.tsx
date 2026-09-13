import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { KakaoMap, type KakaoMapPoint } from '../../components/KakaoMap'
import type { SavedPlace } from '../saved/types'
import { MapPlaceSearch } from './MapPlaceSearch'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createCollection, getCollections } from '../saved/savedApi'

export function HomePlacesMap({ places }: { places: SavedPlace[] }) {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const focused = places.find((place) => place.savedPlaceId === Number(params.get('place')))
  const focusedCenter = useMemo(() => focused?.latitude != null && focused.longitude != null
    ? { latitude: focused.latitude, longitude: focused.longitude } : null, [focused?.latitude, focused?.longitude])
  const [filter, setFilter] = useState<number | null>(null)
  const cache = useQueryClient()
  const collections = useQuery({ queryKey: ['collections'], queryFn: getCollections })
  const activeFilter = collections.data?.some((item) => item.id === filter) ? filter : null
  const [searchPoint, setSearchPoint] = useState<KakaoMapPoint | null>(null)
  const searchCenter = useMemo(() => searchPoint ? { latitude: searchPoint.latitude, longitude: searchPoint.longitude } : null, [searchPoint])
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

  const addCollection = useMutation({ mutationFn: createCollection, onSuccess: async (collection) => {
    await cache.invalidateQueries({ queryKey: ['collections'] })
    setFilter(collection.id); setNewFilter(''); setAddingFilter(false); setFilterError('')
  }, onError: () => setFilterError('컬렉션을 만들지 못했습니다. 다시 시도해 주세요.') })
  const addFilter = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (newFilter.trim()) addCollection.mutate(newFilter.trim())
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
    return places
      .filter((place) => place.latitude !== null && place.longitude !== null)
      .filter((place) => activeFilter === null || place.collectionId === activeFilter)
      .map((place) => ({
        id: place.savedPlaceId,
        name: place.name,
        label: place.name,
        category: place.collectionName,
        latitude: place.latitude!,
        longitude: place.longitude!,
      }))
  }, [activeFilter, places])

  return (
    <section className="home-map" aria-label="저장한 장소 지도">
      <div className="home-map-filters" role="group" aria-label="컬렉션 필터">
        {[{ id: null, name: '전체' }, ...(collections.data ?? [])].map((item) => (
          <button key={item.id ?? 'all'} type="button" className={activeFilter === item.id ? 'active' : ''}
            aria-pressed={activeFilter === item.id} onClick={() => { setFilter(item.id); setSearchPoint(null) }}>
            {item.name}</button>
        ))}
        <button
          ref={addButtonRef}
          className="map-filter-add"
          type="button"
          aria-label="컬렉션 추가"
          aria-expanded={addingFilter}
          onClick={() => { setAddingFilter((open) => !open); setFilterError('') }}
        >+</button>
      </div>
      {addingFilter && (
        <form ref={filterPopoverRef} className="map-filter-popover" onSubmit={addFilter}>
          <label htmlFor="new-map-filter">새 컬렉션</label>
          <div>
            <input
              id="new-map-filter"
              autoFocus
              maxLength={100}
              value={newFilter}
              onChange={(event) => { setNewFilter(event.target.value); setFilterError('') }}
              placeholder="예: 문화시설"
            />
            <button type="submit" disabled={addCollection.isPending || !newFilter.trim()}>추가</button>
          </div>
          {filterError && <small role="alert">{filterError}</small>}
        </form>
      )}
      <KakaoMap
        ariaLabel={`저장 장소 ${points.length}곳`}
        points={searchPoint ? [searchPoint] : points}
        initialCenter={searchCenter ?? focusedCenter ?? currentLocation}
        fitPoints={false}
        onSelect={(point) => { if (!searchPoint) navigate(`/saved/places/${point.id}`) }}
      />
      <MapPlaceSearch places={places} onSelect={setSearchPoint} />
    </section>
  )
}
