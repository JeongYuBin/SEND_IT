import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { getCollections, getSavedPlace, updateSavedPlace } from './savedApi'
import type { VisitStatus } from './types'

const statusLabels: Record<VisitStatus, string> = {
  WANT_TO_VISIT: '가고 싶어요',
  PLANNED: '방문 예정',
  VISITED: '방문 완료',
}

export function SavedPlaceDetailPage() {
  const { savedPlaceId: savedPlaceIdParam } = useParams()
  const savedPlaceId = Number(savedPlaceIdParam)
  const queryClient = useQueryClient()
  const placeQuery = useQuery({
    queryKey: ['saved-place', savedPlaceId],
    queryFn: () => getSavedPlace(savedPlaceId),
    enabled: Number.isFinite(savedPlaceId),
  })
  const collectionsQuery = useQuery({ queryKey: ['collections'], queryFn: getCollections })
  const updateMutation = useMutation({
    mutationFn: (request: {
      visitStatus?: VisitStatus
      collectionId?: number
      clearCollection?: boolean
    }) => updateSavedPlace(savedPlaceId, request),
    onSuccess: (place) => {
      queryClient.setQueryData(['saved-place', savedPlaceId], place)
      queryClient.invalidateQueries({ queryKey: ['saved-places'] })
    },
  })

  if (placeQuery.isLoading) {
    return <main className="place-detail-shell"><div className="analysis-state"><span className="spinner" />장소 정보를 불러오고 있습니다.</div></main>
  }
  if (placeQuery.isError || !placeQuery.data) {
    return <main className="place-detail-shell"><div className="analysis-state error">장소를 찾을 수 없습니다.<Link to="/saved">목록으로 돌아가기</Link></div></main>
  }

  const place = placeQuery.data
  const backUrl = place.collectionId ? `/saved/collections/${place.collectionId}` : '/saved'

  return (
    <main className="place-detail-shell">
      <nav className="top-nav">
        <Link className="brand-link" to="/">SEND IT</Link>
        <Link to={backUrl}>← 저장한 장소</Link>
      </nav>
      <article className="place-detail">
        <div className="place-detail-visual">
          {place.imageUrl
            ? <img src={place.imageUrl} alt={place.name} />
            : <div className="preview-placeholder">{place.name.slice(0, 1)}</div>}
        </div>
        <div className="place-detail-content">
          <div className="place-meta">
            <span>{place.category ?? '미분류'}</span>
            <span>{place.collectionName ?? '컬렉션 없음'}</span>
          </div>
          <h1>{place.name}</h1>
          <p className="place-detail-address">{place.roadAddress ?? place.address ?? '주소 정보 없음'}</p>

          <section>
            <h2>장소 설명</h2>
            <p>{place.description ?? '저장된 설명이 없습니다.'}</p>
          </section>
          {place.memo && (
            <section>
              <h2>내 메모</h2>
              <p className="place-memo">{place.memo}</p>
            </section>
          )}
          <dl className="place-detail-facts">
            <div><dt>방문 상태</dt><dd>
              <select
                value={place.visitStatus}
                disabled={updateMutation.isPending}
                onChange={(event) => updateMutation.mutate({
                  visitStatus: event.target.value as VisitStatus,
                })}
              >
                {Object.entries(statusLabels).map(([value, label]) => (
                  <option key={value} value={value}>{label}</option>
                ))}
              </select>
            </dd></div>
            <div><dt>컬렉션</dt><dd>
              <select
                value={place.collectionId ?? 'none'}
                disabled={updateMutation.isPending}
                onChange={(event) => {
                  const value = event.target.value
                  updateMutation.mutate(value === 'none'
                    ? { clearCollection: true }
                    : { collectionId: Number(value) })
                }}
              >
                <option value="none">컬렉션 없음</option>
                {collectionsQuery.data?.map((item) => (
                  <option key={item.id} value={item.id}>{item.name}</option>
                ))}
              </select>
            </dd></div>
            <div><dt>저장일</dt><dd>{new Date(place.savedAt).toLocaleDateString('ko-KR')}</dd></div>
            {place.latitude !== null && place.longitude !== null && (
              <div><dt>좌표</dt><dd>{place.latitude.toFixed(6)}, {place.longitude.toFixed(6)}</dd></div>
            )}
          </dl>
          {place.originalUrl && (
            <a className="place-source-link" href={place.originalUrl} target="_blank" rel="noreferrer">
              원본 콘텐츠 열기 ↗
            </a>
          )}
        </div>
      </article>
    </main>
  )
}
