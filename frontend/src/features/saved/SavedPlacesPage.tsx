import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  createCollection,
  createSavedPlace,
  deleteSavedPlace,
  getCollections,
  getSavedPlaces,
  updateSavedPlace,
} from './savedApi'
import type { SavedPlace } from './types'
import { SavedPlacesMap } from './SavedPlacesMap'
import { getItineraries } from '../itinerary/itineraryApi'
import type { TransportType } from '../itinerary/types'
import { PlaceImage } from '../../components/PlaceImage'
import { eventPeriodState } from './eventPeriod'

const transportLabels: Record<TransportType, string> = {
  WALKING: '도보',
  PUBLIC_TRANSIT: '대중교통',
  CAR: '자동차',
}

function SavedEventBadge({ place }: { place: SavedPlace }) {
  const state = eventPeriodState(place)
  if (!state) return null
  return (
    <span className={`event-status-badge ${state.tone}`} title={state.period}>
      {state.label}
    </span>
  )
}

export function SavedPlacesPage() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const { collectionId: collectionIdParam } = useParams()
  const [searchParams] = useSearchParams()
  const selectedCollectionId = collectionIdParam ? Number(collectionIdParam) : null
  const showUncategorized = selectedCollectionId === null && searchParams.get('collection') === 'none'
  const collectionScope = selectedCollectionId === null
    ? (showUncategorized ? 'none' : 'all')
    : String(selectedCollectionId)
  const [showForm, setShowForm] = useState(false)
  const [name, setName] = useState('')
  const [category, setCategory] = useState('')
  const [address, setAddress] = useState('')
  const [memo, setMemo] = useState('')
  const [collectionId, setCollectionId] = useState<number | undefined>()
  const [newCollection, setNewCollection] = useState('')
  const [view, setView] = useState<'list' | 'map'>('list')
  const [regionFilter, setRegionFilter] = useState('all')
  const [districtFilter, setDistrictFilter] = useState('all')
  const [categoryFilter, setCategoryFilter] = useState('all')

  useEffect(() => {
    setRegionFilter('all')
    setDistrictFilter('all')
    setCategoryFilter('all')
  }, [collectionScope])

  const placesQuery = useQuery({ queryKey: ['saved-places'], queryFn: getSavedPlaces })
  const collectionsQuery = useQuery({ queryKey: ['collections'], queryFn: getCollections })
  const itinerariesQuery = useQuery({ queryKey: ['itineraries'], queryFn: getItineraries })
  const selectedCollection = collectionsQuery.data?.find((item) => item.id === selectedCollectionId)
  const refreshPlaces = () => queryClient.invalidateQueries({ queryKey: ['saved-places'] })

  const createMutation = useMutation({
    mutationFn: createSavedPlace,
    onSuccess: () => {
      refreshPlaces()
      setName(''); setCategory(''); setAddress(''); setMemo(''); setShowForm(false)
    },
  })
  const updateMutation = useMutation({
    mutationFn: ({
      id,
      request,
    }: {
      id: number
      request: { collectionId?: number; clearCollection?: boolean }
    }) => updateSavedPlace(id, request),
    onSuccess: refreshPlaces,
  })
  const deleteMutation = useMutation({
    mutationFn: deleteSavedPlace,
    onSuccess: refreshPlaces,
  })
  const collectionMutation = useMutation({
    mutationFn: createCollection,
    onSuccess: (collection) => {
      queryClient.invalidateQueries({ queryKey: ['collections'] })
      setCollectionId(collection.id)
      setNewCollection('')
    },
  })

  const collectionPlaces = useMemo(
    () => (placesQuery.data ?? []).filter(
      (place) => {
        if (selectedCollectionId !== null) return place.collectionId === selectedCollectionId
        if (showUncategorized) return place.collectionId === null
        return true
      },
    ),
    [placesQuery.data, selectedCollectionId, showUncategorized],
  )
  const categoryOptions = useMemo(
    () => [...new Set(collectionPlaces.map((place) => place.category).filter(Boolean) as string[])].sort(),
    [collectionPlaces],
  )
  const regionOptions = useMemo(
    () => [...new Set(collectionPlaces.map((place) => {
      const placeAddress = place.roadAddress ?? place.address
      return placeAddress?.trim().split(/\s+/)[0] ?? null
    }).filter(Boolean) as string[])].sort(),
    [collectionPlaces],
  )
  const districtOptions = useMemo(
    () => [...new Set(collectionPlaces.map((place) => {
      const addressParts = (place.roadAddress ?? place.address)?.trim().split(/\s+/) ?? []
      if (regionFilter !== 'all' && addressParts[0] !== regionFilter) return null
      return addressParts.slice(1).find((part) => /(?:시|군|구)$/.test(part)) ?? null
    }).filter(Boolean) as string[])].sort(),
    [collectionPlaces, regionFilter],
  )
  const places = useMemo(
    () => collectionPlaces.filter((place) => {
      const placeAddress = place.roadAddress ?? place.address
      const addressParts = placeAddress?.trim().split(/\s+/) ?? []
      const region = addressParts[0] ?? null
      const district = addressParts.slice(1).find((part) => /(?:시|군|구)$/.test(part)) ?? null
      return (regionFilter === 'all' || region === regionFilter)
        && (districtFilter === 'all' || district === districtFilter)
        && (categoryFilter === 'all' || place.category === categoryFilter)
    }),
    [categoryFilter, collectionPlaces, districtFilter, regionFilter],
  )
  const filtersActive = regionFilter !== 'all'
    || districtFilter !== 'all'
    || categoryFilter !== 'all'

  const clearFilters = () => {
    setRegionFilter('all')
    setDistrictFilter('all')
    setCategoryFilter('all')
  }

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    createMutation.mutate({
      name, category: category || undefined, address: address || undefined,
      memo: memo || undefined, collectionId,
    })
  }

  return (
    <main className="saved-shell">
      <nav className="top-nav">
        <Link className="brand-link" to="/">SEND IT</Link>
        <Link to="/">URL 저장하기</Link>
      </nav>

      <header className="saved-header">
        <div>
          <span className="eyebrow">MY PLACES</span>
          <h1>{showUncategorized ? '컬렉션 없는 장소' : (selectedCollection?.name ?? '저장한 장소')}</h1>
          <p>
            {showUncategorized
              ? '아직 컬렉션을 지정하지 않은 장소입니다.'
              : selectedCollection
              ? `${selectedCollection.name} 컬렉션에 저장한 장소입니다.`
              : '발견한 여행지를 모으고 컬렉션별로 관리해 보세요.'}
          </p>
        </div>
        <div className="saved-header-actions">
          <Link className="secondary-button" to="/itineraries/new">여행 계획 만들기</Link>
          <button className="primary-button" onClick={() => setShowForm((value) => !value)}>
            {showForm ? '닫기' : '+ 장소 추가'}
          </button>
        </div>
      </header>

      <section className="saved-itinerary-overview">
        <div className="saved-section-heading">
          <div>
            <span className="eyebrow">MY TRIPS</span>
            <h2>내 여행 계획</h2>
          </div>
          <Link to="/itineraries">전체 계획 보기</Link>
        </div>
        {itinerariesQuery.isLoading && (
          <div className="empty-state">여행 계획을 불러오고 있습니다.</div>
        )}
        {itinerariesQuery.isError && (
          <div className="form-error">여행 계획을 불러오지 못했습니다.</div>
        )}
        {!itinerariesQuery.isLoading && (itinerariesQuery.data?.length ?? 0) === 0 && (
          <div className="saved-itinerary-empty">
            <span>아직 만든 여행 계획이 없습니다.</span>
            <Link to="/itineraries/new">첫 여행 계획 만들기</Link>
          </div>
        )}
        <div className="saved-itinerary-cards">
          {itinerariesQuery.data?.slice(0, 3).map((itinerary) => (
            <Link key={itinerary.id} to={`/itineraries/${itinerary.id}`}>
              <span>{itinerary.startDate} – {itinerary.endDate}</span>
              <strong>{itinerary.title}</strong>
              <small>
                {transportLabels[itinerary.transportType]} · 장소 {itinerary.items.length}개
              </small>
            </Link>
          ))}
        </div>
      </section>

      <aside className="saved-recommendation-tip">
        <span>주변 관광지를 찾고 싶나요?</span>
        <strong>아래 장소 카드를 열면 상세 화면 하단에서 반경 5km 추천 장소를 볼 수 있습니다.</strong>
      </aside>

      {showForm && (
        <form className="place-form" onSubmit={handleSubmit}>
          <input required value={name} onChange={(e) => setName(e.target.value)} placeholder="장소명 *" />
          <input value={category} onChange={(e) => setCategory(e.target.value)} placeholder="카테고리" />
          <input value={address} onChange={(e) => setAddress(e.target.value)} placeholder="주소" />
          <input value={memo} onChange={(e) => setMemo(e.target.value)} placeholder="메모" />
          <select value={collectionId ?? ''} onChange={(e) => setCollectionId(e.target.value ? Number(e.target.value) : undefined)}>
            <option value="">컬렉션 없음</option>
            {collectionsQuery.data?.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
          </select>
          <button disabled={createMutation.isPending}>저장</button>
          {createMutation.isError && <div className="form-error">장소를 저장하지 못했습니다.</div>}
        </form>
      )}

      <section className="collection-bar">
        <div className="filter-chips">
          <button className={selectedCollectionId === null ? 'active' : ''} onClick={() => navigate('/saved')}>전체</button>
          {collectionsQuery.data?.map((item) => (
            <button
              key={item.id}
              className={selectedCollectionId === item.id ? 'active' : ''}
              onClick={() => navigate(`/saved/collections/${item.id}`)}
            >
              {item.name}
            </button>
          ))}
        </div>
        <form onSubmit={(e) => { e.preventDefault(); if (newCollection.trim()) collectionMutation.mutate(newCollection) }}>
          <input value={newCollection} onChange={(e) => setNewCollection(e.target.value)} placeholder="새 컬렉션" />
          <button disabled={collectionMutation.isPending}>추가</button>
        </form>
      </section>

      <section className="place-filters" aria-label="저장 장소 필터">
        <div>
          <label>
            컬렉션
            <select
              value={collectionScope}
              onChange={(event) => {
                const value = event.target.value
                if (value === 'all') navigate('/saved')
                else if (value === 'none') navigate('/saved?collection=none')
                else navigate(`/saved/collections/${value}`)
              }}
            >
              <option value="all">전체 컬렉션</option>
              <option value="none">컬렉션 없음</option>
              {collectionsQuery.data?.map((item) => (
                <option key={item.id} value={item.id}>{item.name}</option>
              ))}
            </select>
          </label>
          <label>
            지역
            <select
              value={regionFilter}
              onChange={(event) => {
                setRegionFilter(event.target.value)
                setDistrictFilter('all')
              }}
            >
              <option value="all">전체 지역</option>
              {regionOptions.map((region) => <option key={region} value={region}>{region}</option>)}
            </select>
          </label>
          <label>
            시·군·구
            <select
              value={districtFilter}
              onChange={(event) => setDistrictFilter(event.target.value)}
              disabled={districtOptions.length === 0}
            >
              <option value="all">전체 시·군·구</option>
              {districtOptions.map((district) => (
                <option key={district} value={district}>{district}</option>
              ))}
            </select>
          </label>
          <label>
            카테고리
            <select value={categoryFilter} onChange={(event) => setCategoryFilter(event.target.value)}>
              <option value="all">전체 카테고리</option>
              {categoryOptions.map((item) => <option key={item} value={item}>{item}</option>)}
            </select>
          </label>
        </div>
        <div className="filter-summary">
          <span>{collectionPlaces.length}개 중 {places.length}개 장소</span>
          {filtersActive && <button type="button" onClick={clearFilters}>필터 초기화</button>}
        </div>
      </section>

      {placesQuery.isLoading && <div className="empty-state">장소를 불러오고 있습니다.</div>}
      {placesQuery.isError && <div className="form-error">저장한 장소를 불러오지 못했습니다.</div>}
      {!placesQuery.isLoading && collectionPlaces.length === 0 && (
        <div className="empty-state"><strong>아직 저장한 장소가 없어요.</strong><span>첫 여행지를 추가해 보세요.</span></div>
      )}
      {!placesQuery.isLoading && collectionPlaces.length > 0 && places.length === 0 && (
        <div className="empty-state">
          <strong>조건에 맞는 장소가 없어요.</strong>
          <span>다른 필터를 선택하거나 필터를 초기화해 보세요.</span>
        </div>
      )}
      {collectionIdParam && !collectionsQuery.isLoading && !selectedCollection && (
        <div className="form-error">존재하지 않거나 접근할 수 없는 컬렉션입니다.</div>
      )}
      {places.length > 0 && (
        <div className="view-switcher" aria-label="장소 보기 방식">
          <button className={view === 'list' ? 'active' : ''} onClick={() => setView('list')}>목록</button>
          <button className={view === 'map' ? 'active' : ''} onClick={() => setView('map')}>지도</button>
        </div>
      )}
      {places.length > 0 && (view === 'map' ? (
        <SavedPlacesMap places={places} />
      ) : (
        <section className="place-grid">
          {places.map((place) => (
            <article
              className="place-card"
              key={place.savedPlaceId}
              role="link"
              tabIndex={0}
              onClick={() => navigate(`/saved/places/${place.savedPlaceId}`)}
              onKeyDown={(event) => {
                if (event.key === 'Enter' || event.key === ' ') {
                  event.preventDefault()
                  navigate(`/saved/places/${place.savedPlaceId}`)
                }
              }}
            >
              <div className="place-image">
                {place.imageUrl ? (
                  <PlaceImage
                    src={place.imageUrl}
                    alt={`${place.name} 대표 이미지`}
                    className="saved-place-image-skeleton"
                  />
                ) : (
                  <span
                    className="place-image-skeleton saved-place-image-skeleton"
                    role="img"
                    aria-label="장소 이미지 준비 중"
                  >
                    <i className="place-image-skeleton-sun" />
                    <i className="place-image-skeleton-mountain" />
                    <i className="place-image-skeleton-ground" />
                  </span>
                )}
              </div>
              <div className="place-content">
                <div className="place-meta">
                  <span>{place.category ?? '미분류'}</span>
                  {place.collectionId && place.collectionName ? (
                    <button
                      className="collection-link"
                      onClick={(event) => {
                        event.stopPropagation()
                        navigate(`/saved/collections/${place.collectionId}`)
                      }}
                    >
                      {place.collectionName}
                    </button>
                  ) : (
                    <span>컬렉션 없음</span>
                  )}
                </div>
                <SavedEventBadge place={place} />
                <h2>{place.name}</h2>
                <p>{place.roadAddress ?? place.address ?? '주소 정보 없음'}</p>
                {place.memo && <p className="place-memo">{place.memo}</p>}
                <label
                  className="place-collection-control"
                  onClick={(event) => event.stopPropagation()}
                >
                  컬렉션
                  <select
                    value={place.collectionId ?? 'none'}
                    disabled={updateMutation.isPending}
                    onChange={(event) => {
                      const value = event.target.value
                      updateMutation.mutate({
                        id: place.savedPlaceId,
                        request: value === 'none'
                          ? { clearCollection: true }
                          : { collectionId: Number(value) },
                      })
                    }}
                  >
                    <option value="none">컬렉션 없음</option>
                    {collectionsQuery.data?.map((item) => (
                      <option key={item.id} value={item.id}>{item.name}</option>
                    ))}
                  </select>
                </label>
                <div className="place-actions" onClick={(event) => event.stopPropagation()}>
                  <button onClick={() => deleteMutation.mutate(place.savedPlaceId)}>삭제</button>
                </div>
              </div>
            </article>
          ))}
        </section>
      ))}
    </main>
  )
}
