import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  createSavedPlace,
  getCollections,
  getNearbyTourismPlaces,
  getSavedPlace,
  getSavedPlaces,
  getTourismOperatingInfo,
  getTourismPlaceDetail,
  updateSavedPlace,
} from './savedApi'
import type { NearbyTourismPlace, VisitStatus } from './types'
import { KakaoMap } from '../../components/KakaoMap'

const statusLabels: Record<VisitStatus, string> = {
  WANT_TO_VISIT: '가고 싶어요',
  PLANNED: '방문 예정',
  VISITED: '방문 완료',
}

export function SavedPlaceDetailPage() {
  const { savedPlaceId: savedPlaceIdParam } = useParams()
  const savedPlaceId = Number(savedPlaceIdParam)
  const queryClient = useQueryClient()
  const [selectedNearby, setSelectedNearby] = useState<NearbyTourismPlace | null>(null)
  const placeQuery = useQuery({
    queryKey: ['saved-place', savedPlaceId],
    queryFn: () => getSavedPlace(savedPlaceId),
    enabled: Number.isFinite(savedPlaceId),
  })
  const collectionsQuery = useQuery({ queryKey: ['collections'], queryFn: getCollections })
  const savedPlacesQuery = useQuery({ queryKey: ['saved-places'], queryFn: getSavedPlaces })
  const operatingInfoQuery = useQuery({
    queryKey: [
      'tourism-operating-info',
      placeQuery.data?.name,
      placeQuery.data?.roadAddress ?? placeQuery.data?.address,
    ],
    queryFn: () => getTourismOperatingInfo(
      placeQuery.data!.name,
      placeQuery.data!.roadAddress ?? placeQuery.data!.address,
    ),
    enabled: Boolean(placeQuery.data?.name)
      && placeQuery.data?.tourismContentId === null,
  })
  const nearbyQuery = useQuery({
    queryKey: ['tourism-nearby', placeQuery.data?.latitude, placeQuery.data?.longitude, 10000],
    queryFn: () => getNearbyTourismPlaces(
      placeQuery.data!.latitude!,
      placeQuery.data!.longitude!,
      10000,
    ),
    enabled: typeof placeQuery.data?.latitude === 'number'
      && typeof placeQuery.data?.longitude === 'number',
  })
  const nearbyDetailQuery = useQuery({
    queryKey: [
      'tourism-place-detail',
      selectedNearby?.contentId,
      selectedNearby?.contentTypeId,
    ],
    queryFn: () => getTourismPlaceDetail(
      selectedNearby!.contentId,
      selectedNearby!.contentTypeId,
    ),
    enabled: selectedNearby !== null,
  })
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
  const saveNearbyMutation = useMutation({
    mutationFn: (nearby: NearbyTourismPlace) => createSavedPlace({
      name: nearby.name,
      category: nearby.category ?? undefined,
      address: nearby.address ?? undefined,
      latitude: nearby.latitude,
      longitude: nearby.longitude,
      imageUrl: nearby.imageUrl ?? undefined,
      tourismContentId: nearby.contentId,
      tourismContentTypeId: nearby.contentTypeId,
      collectionId: placeQuery.data?.collectionId ?? undefined,
    }),
    onSuccess: (savedPlace) => {
      queryClient.setQueryData(
        ['saved-places'],
        (current: Awaited<ReturnType<typeof getSavedPlaces>> | undefined) => {
          if (!current) return [savedPlace]
          return current.some((place) => place.savedPlaceId === savedPlace.savedPlaceId)
            ? current
            : [...current, savedPlace]
        },
      )
      queryClient.invalidateQueries({ queryKey: ['saved-places'] })
      setSelectedNearby(null)
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
  const storedOperatingInfo = place.operatingHours !== null || place.restDays !== null
  const savedPlaceNames = new Set(
    savedPlacesQuery.data?.map((saved) => saved.name.replaceAll(/\s/g, '').toLowerCase()),
  )
  const visibleNearbyPlaces = nearbyQuery.data
    ?.filter((nearby) => nearby.distanceMeters >= 10)
    .filter((nearby) => !savedPlaceNames.has(nearby.name.replaceAll(/\s/g, '').toLowerCase()))
    .slice(0, 6)

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
          {place.latitude !== null && place.longitude !== null && (
            <section className="place-detail-location">
              <div>
                <span className="eyebrow">LOCATION</span>
                <h2>위치 확인</h2>
              </div>
              <KakaoMap
                ariaLabel={`${place.name} 위치 지도`}
                points={[{
                  id: place.placeId,
                  name: place.name,
                  latitude: place.latitude,
                  longitude: place.longitude,
                }]}
              />
            </section>
          )}
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
          <section className="place-operating-info">
            <h2>방문 정보</h2>
            {operatingInfoQuery.isLoading && !storedOperatingInfo && (
              <p>관광공사 방문 정보를 확인하고 있습니다.</p>
            )}
            {operatingInfoQuery.isError && !storedOperatingInfo && <p>방문 정보를 불러오지 못했습니다.</p>}
            {(storedOperatingInfo || operatingInfoQuery.data?.available) ? (
              <dl>
                <div>
                  <dt>이용시간</dt>
                  <dd>{place.operatingHours ?? operatingInfoQuery.data?.hours ?? '등록된 정보 없음'}</dd>
                </div>
                <div>
                  <dt>휴무일</dt>
                  <dd>{place.restDays ?? operatingInfoQuery.data?.restDays ?? '등록된 정보 없음'}</dd>
                </div>
                {place.parkingInfo && (
                  <div>
                    <dt>주차 안내</dt>
                    <dd>{place.parkingInfo}</dd>
                  </div>
                )}
              </dl>
            ) : (
              !operatingInfoQuery.isLoading
              && !operatingInfoQuery.isError
              && <p>관광공사에 등록된 이용시간과 휴무일 정보가 없습니다.</p>
            )}
          </section>
          {place.memo && (
            <section>
              <h2>내 메모</h2>
              <p className="place-memo">{place.memo}</p>
            </section>
          )}
          <dl className="place-detail-facts">
            {place.phone && <div><dt>문의</dt><dd>{place.phone}</dd></div>}
            {place.homepageUrl && (
              <div>
                <dt>홈페이지</dt>
                <dd>
                  <a href={place.homepageUrl} target="_blank" rel="noreferrer">공식 홈페이지 ↗</a>
                </dd>
              </div>
            )}
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
      {place.latitude !== null && place.longitude !== null && (
        <section className="nearby-tourism">
          <div className="nearby-tourism-heading">
            <div>
              <span className="eyebrow">NEARBY</span>
              <h2>주변 관광지 추천</h2>
            </div>
            <p>관광공사 데이터 기준 최대 10km · 가까운 6곳</p>
          </div>
          {nearbyQuery.isLoading && <div className="empty-state">주변 장소를 찾고 있습니다.</div>}
          {nearbyQuery.isError && (
            <div className="form-error">주변 관광지를 불러오지 못했습니다.</div>
          )}
          <div className="nearby-tourism-grid">
            {visibleNearbyPlaces?.map((nearby) => {
                const isSaving = saveNearbyMutation.isPending
                  && saveNearbyMutation.variables?.contentId === nearby.contentId
                return (
                  <article
                    key={nearby.contentId}
                    onClick={() => setSelectedNearby(nearby)}
                  >
                    {nearby.imageUrl
                      ? <img src={nearby.imageUrl} alt="" />
                      : (
                        <div className="nearby-placeholder" aria-label="관광공사 제공 이미지 없음">
                          <strong>{nearby.category ?? '관광지'}</strong>
                          <small>제공 이미지 없음</small>
                        </div>
                      )}
                    <div>
                      <span>{nearby.category ?? '관광지'} · {nearby.distanceMeters.toLocaleString()}m</span>
                      <h3>
                        <button
                          className="nearby-detail-trigger"
                          type="button"
                          onClick={() => setSelectedNearby(nearby)}
                        >
                          {nearby.name}
                        </button>
                      </h3>
                      <p>{nearby.address ?? '주소 정보 없음'}</p>
                      <button
                        className="nearby-save-button"
                        type="button"
                        disabled={saveNearbyMutation.isPending}
                        onClick={(event) => {
                          event.stopPropagation()
                          saveNearbyMutation.mutate(nearby)
                        }}
                      >
                        <span aria-hidden="true">＋</span>
                        {isSaving ? '저장 중...' : '내 장소에 담기'}
                      </button>
                    </div>
                  </article>
                )
              })}
          </div>
          {selectedNearby && (
            <div
              className="nearby-detail-backdrop"
              role="presentation"
              onMouseDown={(event) => {
                if (event.target === event.currentTarget) setSelectedNearby(null)
              }}
            >
              <section
                className="nearby-detail-dialog"
                role="dialog"
                aria-modal="true"
                aria-labelledby="nearby-detail-title"
              >
                <button
                  className="nearby-detail-close"
                  type="button"
                  onClick={() => setSelectedNearby(null)}
                  aria-label="상세 미리보기 닫기"
                >
                  ×
                </button>
                {nearbyDetailQuery.isLoading && (
                  <div className="analysis-state"><span className="spinner" />상세정보를 불러오고 있습니다.</div>
                )}
                {nearbyDetailQuery.isError && (
                  <>
                    {selectedNearby.imageUrl
                      ? <img src={selectedNearby.imageUrl} alt="" />
                      : (
                        <div className="nearby-placeholder">
                          <strong>{selectedNearby.category ?? '관광지'}</strong>
                          <small>제공 이미지 없음</small>
                        </div>
                      )}
                    <div className="nearby-detail-content">
                      <span className="eyebrow">{selectedNearby.category ?? 'TOURISM'}</span>
                      <h2 id="nearby-detail-title">{selectedNearby.name}</h2>
                      <p className="nearby-detail-address">
                        {selectedNearby.address ?? '주소 정보 없음'}
                      </p>
                      <div className="nearby-detail-description">등록된 상세 설명이 없습니다.</div>
                      <div className="nearby-detail-actions">
                        <button
                          className="nearby-save-button"
                          type="button"
                          disabled={saveNearbyMutation.isPending}
                          onClick={() => saveNearbyMutation.mutate(selectedNearby)}
                        >
                          <span aria-hidden="true">＋</span>
                          {saveNearbyMutation.isPending ? '저장 중...' : '내 장소에 담기'}
                        </button>
                      </div>
                    </div>
                  </>
                )}
                {nearbyDetailQuery.data && (
                  <>
                    {nearbyDetailQuery.data.imageUrl
                      ? <img src={nearbyDetailQuery.data.imageUrl} alt="" />
                      : (
                        <div className="nearby-placeholder">
                          <strong>{nearbyDetailQuery.data.category ?? '관광지'}</strong>
                          <small>제공 이미지 없음</small>
                        </div>
                      )}
                    <div className="nearby-detail-content">
                      <span className="eyebrow">{nearbyDetailQuery.data.category ?? 'TOURISM'}</span>
                      <h2 id="nearby-detail-title">{nearbyDetailQuery.data.name}</h2>
                      <p className="nearby-detail-address">
                        {nearbyDetailQuery.data.address ?? '주소 정보 없음'}
                      </p>
                      <div className="nearby-detail-description">
                        {nearbyDetailQuery.data.description ?? '등록된 상세 설명이 없습니다.'}
                      </div>
                      <dl>
                        {nearbyDetailQuery.data.operatingHours && (
                          <div><dt>이용시간</dt><dd>{nearbyDetailQuery.data.operatingHours}</dd></div>
                        )}
                        {nearbyDetailQuery.data.restDays && (
                          <div><dt>휴무일</dt><dd>{nearbyDetailQuery.data.restDays}</dd></div>
                        )}
                        {nearbyDetailQuery.data.parkingInfo && (
                          <div><dt>주차 안내</dt><dd>{nearbyDetailQuery.data.parkingInfo}</dd></div>
                        )}
                        {nearbyDetailQuery.data.phone && (
                          <div><dt>문의</dt><dd>{nearbyDetailQuery.data.phone}</dd></div>
                        )}
                      </dl>
                      <div className="nearby-detail-actions">
                        {nearbyDetailQuery.data.homepageUrl && (
                          <a href={nearbyDetailQuery.data.homepageUrl} target="_blank" rel="noreferrer">
                            공식 홈페이지 ↗
                          </a>
                        )}
                        <button
                          className="nearby-save-button"
                          type="button"
                          disabled={saveNearbyMutation.isPending}
                          onClick={() => saveNearbyMutation.mutate(selectedNearby)}
                        >
                          <span aria-hidden="true">＋</span>
                          {saveNearbyMutation.isPending ? '저장 중...' : '내 장소에 담기'}
                        </button>
                      </div>
                    </div>
                  </>
                )}
              </section>
            </div>
          )}
        </section>
      )}
    </main>
  )
}
