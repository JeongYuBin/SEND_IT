import { useMemo, useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  createCollection,
  createSavedPlace,
  deleteSavedPlace,
  getCollections,
  getSavedPlaces,
  updateSavedPlace,
} from './savedApi'
import type { VisitStatus } from './types'

const statusLabels: Record<VisitStatus, string> = {
  WANT_TO_VISIT: '가고 싶어요',
  PLANNED: '방문 예정',
  VISITED: '방문 완료',
}

export function SavedPlacesPage() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const { collectionId: collectionIdParam } = useParams()
  const selectedCollectionId = collectionIdParam ? Number(collectionIdParam) : null
  const [showForm, setShowForm] = useState(false)
  const [name, setName] = useState('')
  const [category, setCategory] = useState('')
  const [address, setAddress] = useState('')
  const [memo, setMemo] = useState('')
  const [collectionId, setCollectionId] = useState<number | undefined>()
  const [newCollection, setNewCollection] = useState('')

  const placesQuery = useQuery({ queryKey: ['saved-places'], queryFn: getSavedPlaces })
  const collectionsQuery = useQuery({ queryKey: ['collections'], queryFn: getCollections })
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
    mutationFn: ({ id, status }: { id: number; status: VisitStatus }) =>
      updateSavedPlace(id, { visitStatus: status }),
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

  const places = useMemo(
    () => (placesQuery.data ?? []).filter(
      (place) => selectedCollectionId === null || place.collectionId === selectedCollectionId,
    ),
    [placesQuery.data, selectedCollectionId],
  )

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
          <h1>{selectedCollection?.name ?? '저장한 장소'}</h1>
          <p>
            {selectedCollection
              ? `${selectedCollection.name} 컬렉션에 저장한 장소입니다.`
              : '발견한 여행지를 모으고 방문 상태를 관리해 보세요.'}
          </p>
        </div>
        <button className="primary-button" onClick={() => setShowForm((value) => !value)}>
          {showForm ? '닫기' : '+ 장소 추가'}
        </button>
      </header>

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

      {placesQuery.isLoading && <div className="empty-state">장소를 불러오고 있습니다.</div>}
      {placesQuery.isError && <div className="form-error">저장한 장소를 불러오지 못했습니다.</div>}
      {!placesQuery.isLoading && places.length === 0 && (
        <div className="empty-state"><strong>아직 저장한 장소가 없어요.</strong><span>첫 여행지를 추가해 보세요.</span></div>
      )}
      {collectionIdParam && !collectionsQuery.isLoading && !selectedCollection && (
        <div className="form-error">존재하지 않거나 접근할 수 없는 컬렉션입니다.</div>
      )}
      <section className="place-grid">
        {places.map((place) => (
          <article className="place-card" key={place.savedPlaceId}>
            <div className="place-image">
              {place.imageUrl ? <img src={place.imageUrl} alt="" /> : <span>{place.name.slice(0, 1)}</span>}
            </div>
            <div className="place-content">
              <div className="place-meta">
                <span>{place.category ?? '미분류'}</span>
                {place.collectionId && place.collectionName ? (
                  <button
                    className="collection-link"
                    onClick={() => navigate(`/saved/collections/${place.collectionId}`)}
                  >
                    {place.collectionName}
                  </button>
                ) : (
                  <span>컬렉션 없음</span>
                )}
              </div>
              <h2>{place.name}</h2>
              <p>{place.roadAddress ?? place.address ?? '주소 정보 없음'}</p>
              {place.memo && <p className="place-memo">{place.memo}</p>}
              <div className="place-actions">
                <select value={place.visitStatus} onChange={(e) => updateMutation.mutate({ id: place.savedPlaceId, status: e.target.value as VisitStatus })}>
                  {Object.entries(statusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                </select>
                <button onClick={() => deleteMutation.mutate(place.savedPlaceId)}>삭제</button>
              </div>
            </div>
          </article>
        ))}
      </section>
    </main>
  )
}
