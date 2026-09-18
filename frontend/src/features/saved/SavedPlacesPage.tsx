import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react'
import { createPortal } from 'react-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  createCollection,
  createSavedPlace,
  deleteCollection,
  deleteSavedPlace,
  getCollections,
  getSavedPlaces,
  searchKakaoPlaces,
  updateSavedPlace,
} from './savedApi'
import type { KakaoPlaceSearchResult, SavedPlace } from './types'
import { getItineraries } from '../itinerary/itineraryApi'
import type { TransportType } from '../itinerary/types'
import { PlaceImage } from '../../components/PlaceImage'
import { eventPeriodState } from './eventPeriod'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { FeedDiscovery } from './FeedDiscovery'
import { CollectionChoiceCancelled } from './collectionChoice'
import { PendingSavedPost } from './SavedSharedPosts'
import { usePendingSavedPosts } from './usePendingSavedPosts'

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
  const selectedCollectionId = collectionIdParam ? Number(collectionIdParam) : null
  const [showCollectionDeleteConfirm, setShowCollectionDeleteConfirm] = useState(false)
  const [showForm, setShowForm] = useState(false)
  const [addMode, setAddMode] = useState<'search' | 'direct'>('search')
  const [placeSearch, setPlaceSearch] = useState('')
  const [submittedPlaceSearch, setSubmittedPlaceSearch] = useState('')
  const [placeSearchPage, setPlaceSearchPage] = useState(1)
  const [name, setName] = useState('')
  const [address, setAddress] = useState('')
  const [memo, setMemo] = useState('')
  const [collectionId, setCollectionId] = useState<number | undefined>()
  const [newCollection, setNewCollection] = useState('')
  const [showCollectionAdd, setShowCollectionAdd] = useState(false)
  const [search, setSearch] = useState('')
  const [regionFilter, setRegionFilter] = useState('all')
  const [districtFilter, setDistrictFilter] = useState('all')
  const [categoryFilter, setCategoryFilter] = useState('all')
  const [collectionMenuPlaceId, setCollectionMenuPlaceId] = useState<number | null>(null)
  const addPlaceSlotRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    setRegionFilter('all')
    setDistrictFilter('all')
    setCategoryFilter('all')
  }, [selectedCollectionId])

  useEffect(() => {
    if (!showForm) return
    addPlaceSlotRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }, [showForm])

  const placesQuery = useQuery({ queryKey: ['saved-places'], queryFn: getSavedPlaces })
  const pendingPosts = usePendingSavedPosts(selectedCollectionId, placesQuery.data ?? [])
  const feedEndRef = useRef<HTMLDivElement>(null)
  const { hasNextPage, isFetching, isError, fetchNextPage } = pendingPosts
  useEffect(() => {
    const target = feedEndRef.current
    if (!target || !hasNextPage || isFetching || isError) return
    const observer = new IntersectionObserver(entries => {
      if (entries.some(entry => entry.isIntersecting)) void fetchNextPage()
    }, { rootMargin: '0px 0px 320px 0px' })
    observer.observe(target)
    return () => observer.disconnect()
  }, [hasNextPage, isFetching, isError, fetchNextPage])
  const collectionsQuery = useQuery({ queryKey: ['collections'], queryFn: getCollections })
  const itinerariesQuery = useQuery({ queryKey: ['itineraries'], queryFn: getItineraries })
  const placeSearchQuery = useQuery({
    queryKey: ['kakao-place-search', submittedPlaceSearch, placeSearchPage],
    queryFn: () => searchKakaoPlaces(submittedPlaceSearch, placeSearchPage),
    enabled: submittedPlaceSearch.length >= 2,
  })
  const selectedCollection = collectionsQuery.data?.find((item) => item.id === selectedCollectionId)
  const refreshPlaces = () => queryClient.invalidateQueries({ queryKey: ['saved-places'] })

  const createMutation = useMutation({
    mutationFn: createSavedPlace,
    onSuccess: () => {
      refreshPlaces()
      setName(''); setAddress(''); setMemo(''); setShowForm(false)
      setPlaceSearch(''); setSubmittedPlaceSearch(''); setPlaceSearchPage(1)
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
      setShowCollectionAdd(false)
    },
  })
  const deleteCollectionMutation = useMutation({
    mutationFn: deleteCollection,
    onSuccess: () => {
      setShowCollectionDeleteConfirm(false)
      queryClient.invalidateQueries({ queryKey: ['collections'] })
      refreshPlaces()
      navigate('/saved')
    },
  })

  const collectionPlaces = useMemo(
    () => (placesQuery.data ?? []).filter(
      (place) => {
        if (selectedCollectionId !== null) return place.collectionId === selectedCollectionId
        return true
      },
    ),
    [placesQuery.data, selectedCollectionId],
  )
  const categoryOptions = useMemo(
    () => [...new Set(collectionPlaces.map((place) => place.collectionName).filter(Boolean) as string[])].sort(),
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
        && [place.name, place.address, place.roadAddress, place.collectionName, place.memo]
          .filter(Boolean).join(' ').toLocaleLowerCase().includes(search.trim().toLocaleLowerCase())
        && (districtFilter === 'all' || district === districtFilter)
        && (categoryFilter === 'all' || place.collectionName === categoryFilter)
    }),
    [categoryFilter, collectionPlaces, districtFilter, regionFilter, search],
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
      name, address: address || undefined,
      memo: memo || undefined, collectionId,
    })
  }

  const handlePlaceSearch = (event: FormEvent) => {
    event.preventDefault()
    const query = placeSearch.trim()
    if (query.length < 2) return
    setPlaceSearchPage(1)
    setSubmittedPlaceSearch(query)
  }

  const saveSearchResult = (result: KakaoPlaceSearchResult) => {
    createMutation.mutate({
      name: result.name,
      category: result.categoryGroup ?? result.category ?? undefined,
      address: result.address ?? undefined,
      roadAddress: result.roadAddress ?? undefined,
      latitude: result.latitude ?? undefined,
      longitude: result.longitude ?? undefined,
      phone: result.phone ?? undefined,
      kakaoPlaceId: result.kakaoPlaceId,
      kakaoPlaceUrl: result.kakaoPlaceUrl ?? undefined,
      memo: memo || undefined,
      collectionId,
    })
  }

  const isSearchResultSaved = (result: KakaoPlaceSearchResult) => placesQuery.data?.some(
    (saved) => saved.kakaoPlaceId === result.kakaoPlaceId
      || (saved.name.replaceAll(/\s/g, '').toLowerCase()
        === result.name.replaceAll(/\s/g, '').toLowerCase()
        && (saved.roadAddress ?? saved.address) === (result.roadAddress ?? result.address)),
  ) ?? false

  return (
    <main className="saved-shell feed-shell">
      <nav className="top-nav">
        <Link className="brand-link" to="/">SEND IT</Link>
        <div>
          <Link to="/">URL 저장하기</Link>
          <Link to="/profile">내 정보</Link>
          <Link to="/settings">설정</Link>
          <Link to="/notifications">알림</Link>
        </div>
      </nav>

      <header className="saved-header">
        <div>
          <span className="eyebrow">MY PLACES</span>
          <h1>{selectedCollection?.name ?? '나의 게시물'}</h1>
          <p>
            {selectedCollection
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
      <div ref={addPlaceSlotRef} className="place-add-slot" />
      <FeedDiscovery />

      <details className="saved-itinerary-overview">
        <summary>내 여행 계획 · {itinerariesQuery.data?.length ?? 0}개</summary>
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
      </details>


      {showForm && addPlaceSlotRef.current && createPortal((
        <section className="place-add-panel">
          <header>
            <div>
              <span className="eyebrow">ADD PLACE</span>
              <h2>장소 추가</h2>
            </div>
            <div className="place-add-tabs" role="tablist" aria-label="장소 추가 방식">
              <button
                type="button"
                className={addMode === 'search' ? 'active' : ''}
                onClick={() => setAddMode('search')}
              >장소 검색</button>
              <button
                type="button"
                className={addMode === 'direct' ? 'active' : ''}
                onClick={() => setAddMode('direct')}
              >직접 입력</button>
            </div>
          </header>
          {addMode === 'search' ? (
            <>
              <form className="kakao-place-search" onSubmit={handlePlaceSearch}>
                <label>
                  <span>카카오맵 장소 검색</span>
                  <div>
                    <input
                      value={placeSearch}
                      onChange={(event) => setPlaceSearch(event.target.value)}
                      placeholder="장소명과 지역을 입력하세요. 예: 스시화 잠실"
                      aria-label="카카오맵 장소 검색어"
                    />
                    <button type="submit" disabled={placeSearch.trim().length < 2}>검색</button>
                  </div>
                </label>
              </form>
              <div className="place-add-options">
                <input value={memo} onChange={(e) => setMemo(e.target.value)} placeholder="공통 메모 (선택)" />
                <select value={collectionId ?? ''} onChange={(e) => setCollectionId(e.target.value ? Number(e.target.value) : undefined)}>
                  <option value="">저장할 때 선택</option>
                  {collectionsQuery.data?.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
                </select>
              </div>
              {placeSearchQuery.isLoading && <div className="analysis-state"><span className="spinner" />장소를 검색하고 있습니다.</div>}
              {placeSearchQuery.isError && <div className="form-error">카카오 장소 검색을 완료하지 못했습니다.</div>}
              {placeSearchQuery.data && placeSearchQuery.data.places.length === 0 && (
                <div className="empty-state">검색 결과가 없습니다. 검색어를 바꾸거나 직접 입력해 주세요.</div>
              )}
              {placeSearchQuery.data && placeSearchQuery.data.places.length > 0 && (
                <div className="place-results-heading">
                  <strong>검색 결과</strong>
                  <span>{placeSearchQuery.data.places.length}곳</span>
                </div>
              )}
              <div className="kakao-place-results">
                {placeSearchQuery.data?.places.map((result) => {
                  const saved = isSearchResultSaved(result)
                  const saving = createMutation.isPending
                    && createMutation.variables?.kakaoPlaceId === result.kakaoPlaceId
                  return (
                    <article key={result.kakaoPlaceId}>
                      <div>
                        <span>{result.category ?? result.categoryGroup ?? '추천 없음'}</span>
                        <h3>{result.name}</h3>
                        <p>{result.roadAddress ?? result.address ?? '주소 정보 없음'}</p>
                        {result.phone && <small>{result.phone}</small>}
                      </div>
                      <div className="kakao-result-actions">
                        {result.kakaoPlaceUrl && (
                          <a href={result.kakaoPlaceUrl} target="_blank" rel="noreferrer">지도 보기 ↗</a>
                        )}
                        <button
                          type="button"
                          disabled={saved || createMutation.isPending}
                          onClick={() => saveSearchResult(result)}
                        >
                          {saved ? '저장됨' : saving ? '저장 중...' : '+ 내 장소에 저장'}
                        </button>
                      </div>
                    </article>
                  )
                })}
              </div>
              {placeSearchQuery.data && placeSearchQuery.data.places.length > 0 && (
                <div className="place-search-pagination">
                  <button type="button" disabled={placeSearchPage === 1} onClick={() => setPlaceSearchPage((page) => page - 1)}>이전</button>
                  <span>{placeSearchPage} 페이지</span>
                  <button type="button" disabled={placeSearchQuery.data.last} onClick={() => setPlaceSearchPage((page) => page + 1)}>다음</button>
                </div>
              )}
            </>
          ) : (
            <form className="place-form" onSubmit={handleSubmit}>
              <input required value={name} onChange={(e) => setName(e.target.value)} placeholder="장소명 *" />
              <input value={address} onChange={(e) => setAddress(e.target.value)} placeholder="주소" />
              <input value={memo} onChange={(e) => setMemo(e.target.value)} placeholder="메모" />
              <select value={collectionId ?? ''} onChange={(e) => setCollectionId(e.target.value ? Number(e.target.value) : undefined)}>
                <option value="">저장할 때 선택</option>
                {collectionsQuery.data?.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
              </select>
              <button disabled={createMutation.isPending}>저장</button>
            </form>
          )}
          {createMutation.isError && !(createMutation.error instanceof CollectionChoiceCancelled) && <div className="form-error">장소를 저장하지 못했습니다.</div>}
        </section>
      ), addPlaceSlotRef.current)}

      <section className="collection-bar">
        <div className="collection-toolbar">
          <div className="filter-chips" aria-label="컬렉션 목록">
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
          <div className="collection-quick-actions">
            <button
              type="button"
              aria-expanded={showCollectionAdd}
              onClick={() => setShowCollectionAdd((open) => !open)}
            ><span aria-hidden="true">＋</span> 추가</button>
            <button
              className="collection-delete-button"
              type="button"
              disabled={selectedCollection?.name === '기타' || !selectedCollection}
              title={selectedCollection ? `${selectedCollection.name} 컬렉션 삭제` : '삭제할 컬렉션을 먼저 선택해 주세요'}
              onClick={() => setShowCollectionDeleteConfirm(true)}
            >삭제</button>
          </div>
        </div>
        {showCollectionAdd && (
          <form className="collection-inline-create" onSubmit={(e) => { e.preventDefault(); if (newCollection.trim()) collectionMutation.mutate(newCollection) }}>
            <input autoFocus maxLength={40} value={newCollection} onChange={(e) => setNewCollection(e.target.value)} placeholder="새 컬렉션 이름" aria-label="새 컬렉션 이름" />
            <button disabled={!newCollection.trim() || collectionMutation.isPending}>{collectionMutation.isPending ? '추가 중…' : '완료'}</button>
            <button type="button" onClick={() => { setShowCollectionAdd(false); setNewCollection('') }}>취소</button>
          </form>
        )}
      </section>

      <div className="feed-search">
        <input type="search" aria-label="저장한 장소 검색" placeholder="장소, 지역, 메모 검색" value={search} onChange={(event) => setSearch(event.target.value)} />
        <span aria-live="polite">{places.length}개</span>
      </div>
      <details className="place-filters" aria-label="저장 장소 필터">
        <summary>상세 필터{filtersActive ? ' · 적용 중' : ''}</summary>
        <div>
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
            컬렉션
            <select value={categoryFilter} onChange={(event) => setCategoryFilter(event.target.value)}>
              <option value="all">전체 컬렉션</option>
              {categoryOptions.map((item) => <option key={item} value={item}>{item}</option>)}
            </select>
          </label>
        </div>
        <div className="filter-summary">
          <span>{collectionPlaces.length}개 중 {places.length}개 장소</span>
          {filtersActive && <button type="button" onClick={clearFilters}>필터 초기화</button>}
        </div>
      </details>

      {placesQuery.isLoading && <div className="empty-state">장소를 불러오고 있습니다.</div>}
      {placesQuery.isError && <div className="form-error">저장한 장소를 불러오지 못했습니다.</div>}
      {!placesQuery.isLoading && collectionPlaces.length === 0 && pendingPosts.posts.length === 0 && (
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
      {(places.length > 0 || pendingPosts.posts.length > 0) && (
        <section className="place-grid">
          {pendingPosts.posts.filter(post =>
            (!search || `${post.title ?? ''} ${post.extractedPlaceName ?? ''}`.includes(search))
            && (categoryFilter === 'all' || post.collectionName === categoryFilter)
            && regionFilter === 'all' && districtFilter === 'all'
          ).map(post => <PendingSavedPost key={`share-${post.shareId}`} post={post} />)}
          {places.map((place) => (
            <article
              className={`place-card${collectionMenuPlaceId === place.savedPlaceId ? ' collection-menu-open' : ''}`}
              key={place.savedPlaceId}
              role="link"
              tabIndex={0}
              onClick={() => navigate(place.latitude != null && place.longitude != null ? `/?place=${place.savedPlaceId}` : `/saved/places/${place.savedPlaceId}`)}
              onKeyDown={(event) => {
                if (event.key === 'Enter' || event.key === ' ') {
                  event.preventDefault()
                  navigate(place.latitude != null && place.longitude != null ? `/?place=${place.savedPlaceId}` : `/saved/places/${place.savedPlaceId}`)
                }
              }}
            >
              <div className="place-image">
                  <PlaceImage
                    src={place.sources.find((source) => source.thumbnailUrl)?.thumbnailUrl ?? place.imageUrl}
                    fallbackSources={[place.imageUrl, ...place.sources.map((source) => source.thumbnailUrl)]}
                    category={place.collectionName}
                    alt={`${place.name} 대표 이미지`}
                    className="saved-place-image-skeleton"
                  />
              </div>
              <div className="place-content">
                <div className="place-meta">
                  <span>{place.collectionName ?? '기타'}</span>
                </div>
                <SavedEventBadge place={place} />
                <h2>{place.name}</h2>
                <p>{place.roadAddress ?? place.address ?? '주소 정보 없음'}</p>
                {place.memo && <p className="place-memo">{place.memo}</p>}
                <div className="feed-card-tools" onClick={(event) => event.stopPropagation()} onKeyDown={(event) => event.stopPropagation()}>
                  <div className="feed-card-collection-wrap">
                    <button className="feed-card-collection" type="button"
                      aria-expanded={collectionMenuPlaceId === place.savedPlaceId}
                      aria-label={`${place.name} 컬렉션 변경`}
                      onClick={() => setCollectionMenuPlaceId(current => current === place.savedPlaceId ? null : place.savedPlaceId)}>
                      <span>컬렉션</span><strong>{place.collectionName ?? '기타'}</strong><i aria-hidden="true">⌄</i>
                    </button>
                    {collectionMenuPlaceId === place.savedPlaceId && <div className="feed-card-collection-menu" role="listbox" aria-label="컬렉션 선택">
                      {collectionsQuery.data?.map(item => <button key={item.id} type="button" role="option"
                        aria-selected={place.collectionId === item.id} disabled={updateMutation.isPending}
                        onClick={() => updateMutation.mutate({ id: place.savedPlaceId, request: { collectionId: item.id } }, {
                          onSuccess: () => setCollectionMenuPlaceId(null),
                        })}>{place.collectionId === item.id && <span aria-hidden="true">✓</span>}{item.name}</button>)}
                    </div>}
                  </div>
                  <button
                    className="feed-card-delete"
                    type="button"
                    disabled={deleteMutation.isPending}
                    onClick={() => deleteMutation.mutate(place.savedPlaceId)}
                  >삭제</button>
                </div>
              </div>
            </article>
          ))}
        </section>
      )}
      {pendingPosts.isError && <p role="alert">게시물 상태를 불러오지 못했습니다. <button onClick={() => void pendingPosts.refetch()}>다시 시도</button></p>}
      <div ref={feedEndRef} className="feed-load-status" role="status" aria-live="polite">
        {pendingPosts.isFetchingNextPage && '게시물을 불러오고 있어요…'}
      </div>
      <ConfirmDialog
        open={showCollectionDeleteConfirm && selectedCollection !== undefined}
        title="컬렉션을 삭제할까요?"
        description={`‘${selectedCollection?.name ?? ''}’ 컬렉션만 삭제됩니다. 컬렉션에 담긴 장소는 삭제되지 않고 ‘기타’ 컬렉션으로 이동합니다.`}
        confirmLabel="컬렉션 삭제"
        pending={deleteCollectionMutation.isPending}
        onCancel={() => setShowCollectionDeleteConfirm(false)}
        onConfirm={() => {
          if (selectedCollection) deleteCollectionMutation.mutate(selectedCollection.id)
        }}
      />
    </main>
  )
}
