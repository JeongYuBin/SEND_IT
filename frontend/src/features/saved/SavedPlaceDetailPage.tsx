import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AxiosError } from 'axios'
import { useState } from 'react'
import { PlaceImage } from '../../components/PlaceImage'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import {
  createSavedPlace,
  deleteSavedPlace,
  getCollections,
  getNearbyTourismPlaces,
  getSavedPlace,
  getSavedPlaces,
  getTourismOperatingInfo,
  getTourismPetInfo,
  getTourismPlaceDetail,
  updateSavedPlace,
} from './savedApi'
import type { NearbyTourismPlace } from './types'
import { KakaoMap } from '../../components/KakaoMap'
import { eventPeriodState } from './eventPeriod'
import { sourceExcerpt } from './sourceExcerpt'

const sourceLabels = {
  INSTAGRAM: 'Instagram',
  TIKTOK: 'TikTok',
  YOUTUBE: 'YouTube',
  NAVER_BLOG: 'Naver Blog',
  MAP: 'Map',
  WEB: 'Web',
} as const

function kakaoMapUrl(place: {
  name: string
  address: string | null
  roadAddress: string | null
  latitude: number | null
  longitude: number | null
}) {
  if (place.latitude !== null && place.longitude !== null) {
    return `https://map.kakao.com/link/map/${encodeURIComponent(place.name)},${place.latitude},${place.longitude}`
  }
  const query = [place.name, place.roadAddress ?? place.address].filter(Boolean).join(' ')
  return `https://map.kakao.com/link/search/${encodeURIComponent(query)}`
}

export function SavedPlaceDetailPage({ placeId, embedded = false, onDeleted }: { placeId?: number; embedded?: boolean; onDeleted?: () => void } = {}) {
  const { savedPlaceId: savedPlaceIdParam } = useParams()
  const savedPlaceId = placeId ?? Number(savedPlaceIdParam)
  const navigate = useNavigate()
  const [confirmDelete, setConfirmDelete] = useState(false)
  const queryClient = useQueryClient()
  const [selectedNearby, setSelectedNearby] = useState<NearbyTourismPlace | null>(null)
  const [editingDetails, setEditingDetails] = useState(false)
  const [editName, setEditName] = useState('')
  const [editAddress, setEditAddress] = useState('')
  const [nearbyPage, setNearbyPage] = useState(0)
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
  const petInfoQuery = useQuery({
    queryKey: ['tourism-pet-info', placeQuery.data?.tourismContentId],
    queryFn: () => getTourismPetInfo(placeQuery.data!.tourismContentId!),
    enabled: Boolean(placeQuery.data?.tourismContentId),
    retry: false,
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
      collectionId?: number
      clearCollection?: boolean
      name?: string
      category?: string
      address?: string
      roadAddress?: string
      imageUrl?: string
    }) => updateSavedPlace(savedPlaceId, request),
    onSuccess: (place) => {
      queryClient.setQueryData(['saved-place', savedPlaceId], place)
      queryClient.invalidateQueries({ queryKey: ['saved-places'] })
      setEditingDetails(false)
    },
  })
  const deleteMutation = useMutation({
    mutationFn: () => deleteSavedPlace(savedPlaceId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['saved-places'] })
      await queryClient.invalidateQueries({ queryKey: ['itineraries'] })
      queryClient.removeQueries({ queryKey: ['saved-place', savedPlaceId], exact: true })
      if (onDeleted) onDeleted()
      else navigate('/saved', { replace: true })
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
  const eventState = eventPeriodState(place)
  const backUrl = place.collectionId ? `/saved/collections/${place.collectionId}` : '/saved'
  const storedOperatingInfo = place.operatingHours !== null || place.restDays !== null
  const savedPlaceNames = new Set(
    savedPlacesQuery.data?.map((saved) => saved.name.replaceAll(/\s/g, '').toLowerCase()),
  )
  const visibleNearbyPlaces = nearbyQuery.data
    ?.filter((nearby) => nearby.distanceMeters >= 10)
    .filter((nearby) => !savedPlaceNames.has(nearby.name.replaceAll(/\s/g, '').toLowerCase()))
    .slice(0, 6)
  const nearbyPageCount = Math.ceil((visibleNearbyPlaces?.length ?? 0) / 4)
  const currentNearbyPage = Math.min(nearbyPage, Math.max(0, nearbyPageCount - 1))

  return (
    <main className={`place-detail-shell compact-saved-detail${embedded ? ' embedded-place-detail' : ''}`}>
      {!embedded && <nav className="top-nav">
        <Link className="brand-link" to="/">SEND IT</Link>
        <div>
          <Link to={backUrl}>← 저장한 장소</Link>
          <Link to="/profile">내 정보</Link>
          <Link to="/settings">설정</Link>
          <Link to="/notifications">알림</Link>
        </div>
      </nav>}
      <article className="place-detail">
        <div className="place-detail-visual">
          <PlaceImage src={place.imageUrl} alt={place.name} category={place.collectionName}
            fallbackSources={place.sources.map((source) => source.thumbnailUrl)} className="place-detail-image-skeleton" />
          {!embedded && place.latitude !== null && place.longitude !== null && (
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
            <span>{place.collectionName ?? '기타'}</span>
          </div>
          <h1>{place.name}</h1>
          <p className="place-detail-address">{place.roadAddress ?? place.address ?? '주소 정보 없음'}</p>
          <div className="saved-place-actions">
          {!editingDetails && (
            <button
              type="button"
              className="secondary-button place-detail-edit-toggle"
              onClick={() => {
                setEditName(place.name)
                setEditAddress(place.roadAddress ?? place.address ?? '')
                setEditingDetails(true)
              }}
            >
              장소 정보 수정
            </button>
          )}
          <a className="kakao-map-link" href={kakaoMapUrl(place)} target="_blank" rel="noreferrer">카카오맵에서 보기 ↗</a>
          <button type="button" className="saved-place-delete" onClick={() => setConfirmDelete(true)} disabled={deleteMutation.isPending}>삭제</button>
          </div>
          {deleteMutation.isError && <p className="form-error" role="alert">장소를 삭제하지 못했습니다. 다시 시도해 주세요.</p>}
          <ConfirmDialog open={confirmDelete} title="저장한 장소를 삭제할까요?" description="내 저장 목록에서 이 장소를 삭제합니다. 연결된 여행 일정에서도 제외됩니다." pending={deleteMutation.isPending} onCancel={() => setConfirmDelete(false)} onConfirm={() => deleteMutation.mutate()} />
          {editingDetails && (
            <form
              className="place-detail-edit-form"
              onSubmit={(event) => {
                event.preventDefault()
                updateMutation.mutate({
                  name: editName,
                  roadAddress: editAddress,
                })
              }}
            >
              <div className="place-detail-edit-heading">
                <div>
                  <span className="eyebrow">EDIT PLACE</span>
                  <h2>장소 정보 수정</h2>
                </div>
                <button type="button" className="edit-close-button" onClick={() => setEditingDetails(false)} aria-label="장소 정보 수정 닫기">×</button>
              </div>
              <div className="place-detail-edit-fields">
                <label><span>장소명 <em>필수</em></span><input required maxLength={200} value={editName} onChange={(event) => setEditName(event.target.value)} /></label>
                <label className="wide-field"><span>주소</span><input maxLength={500} value={editAddress} onChange={(event) => setEditAddress(event.target.value)} /></label>
              </div>
              {updateMutation.isError && (
                <p className="form-error" role="alert">
                  {(updateMutation.error as AxiosError<{ message?: string }>).response?.data?.message
                    ?? '장소 정보를 저장하지 못했습니다. 입력 내용을 확인해 주세요.'}
                </p>
              )}
              <div className="place-detail-edit-actions">
                <button type="button" className="secondary-button" onClick={() => setEditingDetails(false)}>취소</button>
                <button type="submit" className="primary-button" disabled={updateMutation.isPending}>{updateMutation.isPending ? '저장 중...' : '변경 저장'}</button>
              </div>
            </form>
          )}
          {eventState && (
            <section className={`place-event-period ${eventState.tone}`}>
              <div>
                <span className="event-status-badge">{eventState.label}</span>
                <h2>행사 기간</h2>
              </div>
              <strong>{eventState.period}</strong>
              {eventState.tone === 'ended' && (
                <p>행사가 종료되었습니다. 방문 전에 상시 운영 장소인지 확인해 주세요.</p>
              )}
            </section>
          )}

          <details className="saved-detail-disclosure">
            <summary>장소 설명</summary>
            <p>{place.description ?? '저장된 설명이 없습니다.'}</p>
          </details>
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
          {petInfoQuery.data && (
            <section className="pet-travel-info">
              <div className="pet-travel-heading">
                <span className="pet-paw-icon" aria-hidden="true">
                  <svg viewBox="0 0 48 48">
                    <ellipse cx="13" cy="15" rx="5" ry="7" />
                    <ellipse cx="24" cy="10" rx="5" ry="7" />
                    <ellipse cx="35" cy="15" rx="5" ry="7" />
                    <ellipse cx="39" cy="26" rx="4.5" ry="6" />
                    <path d="M24 19c-8 0-14 7-14 14 0 5 4 8 9 6 3-1 7-1 10 0 5 2 9-1 9-6 0-7-6-14-14-14Z" />
                  </svg>
                </span>
                <div>
                  <h2>반려동물 동반 정보</h2>
                  <p>한국관광공사 반려동물 동반여행 데이터</p>
                </div>
              </div>
              <div className="pet-companion-status">
                {petInfoQuery.data.companionType ?? '동반 조건 확인 필요'}
              </div>
              <dl>
                {petInfoQuery.data.allowedPets && (
                  <div><dt>동반 가능 동물</dt><dd>{petInfoQuery.data.allowedPets}</dd></div>
                )}
                {petInfoQuery.data.requiredItems && (
                  <div><dt>필수 준비물</dt><dd>{petInfoQuery.data.requiredItems}</dd></div>
                )}
                {petInfoQuery.data.additionalRules && (
                  <div><dt>이용 규칙</dt><dd>{petInfoQuery.data.additionalRules}</dd></div>
                )}
                {petInfoQuery.data.facilities && (
                  <div><dt>이용 가능 시설</dt><dd>{petInfoQuery.data.facilities}</dd></div>
                )}
                {petInfoQuery.data.providedItems && (
                  <div><dt>비치 품목</dt><dd>{petInfoQuery.data.providedItems}</dd></div>
                )}
                {petInfoQuery.data.rentalItems && (
                  <div><dt>대여 품목</dt><dd>{petInfoQuery.data.rentalItems}</dd></div>
                )}
                {petInfoQuery.data.purchasableItems && (
                  <div><dt>구매 가능 품목</dt><dd>{petInfoQuery.data.purchasableItems}</dd></div>
                )}
                {petInfoQuery.data.safetyInformation && (
                  <div><dt>안전 안내</dt><dd>{petInfoQuery.data.safetyInformation}</dd></div>
                )}
              </dl>
            </section>
          )}
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
            <div><dt>컬렉션</dt><dd>
              <select
                value={place.collectionId ?? 'none'}
                disabled={updateMutation.isPending}
                onChange={(event) => {
                  const value = event.target.value
                  updateMutation.mutate({ collectionId: Number(value) })
                }}
              >
                
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
          {(place.sources.length > 0 || place.originalUrl) && (
            <section className="place-source-section">
              <div>
                <h2>저장한 원본 콘텐츠</h2>
                <span>{place.sources.length || 1}개</span>
              </div>
              {place.sources.length > 0 ? (
                <ul>
                  {place.sources.map((source) => {
                    const excerpt = sourceExcerpt(source.description)
                    return (
                    <li key={source.sharedContentId}>
                      <a href={source.originalUrl} target="_blank" rel="noreferrer">
                        <PlaceImage src={source.thumbnailUrl} className="source-image" />
                        <span>
                          <small>{sourceLabels[source.sourceType]}</small>
                          <strong>{source.title ?? `${sourceLabels[source.sourceType]} 원본 콘텐츠`}</strong>
                          {excerpt && <p>{excerpt}</p>}
                          <time>{new Date(source.linkedAt).toLocaleDateString('ko-KR')}</time>
                        </span>
                        <b aria-hidden="true">↗</b>
                      </a>
                    </li>
                    )
                  })}
                </ul>
              ) : (
                <a className="place-source-link" href={place.originalUrl!} target="_blank" rel="noreferrer">
                  원본 콘텐츠 열기 ↗
                </a>
              )}
            </section>
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
            {visibleNearbyPlaces?.slice(currentNearbyPage * 4, (currentNearbyPage + 1) * 4).map((nearby) => {
                const isSaving = saveNearbyMutation.isPending
                  && saveNearbyMutation.variables?.contentId === nearby.contentId
                return (
                  <article
                    key={nearby.contentId}
                    onClick={() => setSelectedNearby(nearby)}
                  >
                    <PlaceImage src={nearby.imageUrl} category={nearby.category} alt={nearby.name} className="nearby-image" />
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
          {!!visibleNearbyPlaces?.length && visibleNearbyPlaces.length > 4 && (
            <div className="saved-nearby-pagination">
              <button type="button" disabled={currentNearbyPage === 0} onClick={() => setNearbyPage(currentNearbyPage - 1)}>이전</button>
              <span>{currentNearbyPage + 1} / {nearbyPageCount}</span>
              <button type="button" disabled={currentNearbyPage >= nearbyPageCount - 1} onClick={() => setNearbyPage(currentNearbyPage + 1)}>다음</button>
            </div>
          )}
          {nearbyQuery.isSuccess && visibleNearbyPlaces?.length === 0 && <p className="saved-nearby-empty">새롭게 추천할 주변 장소가 없습니다.</p>}
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
                    <PlaceImage src={selectedNearby.imageUrl} category={selectedNearby.category} alt={selectedNearby.name} className="nearby-image" />
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
                    <PlaceImage src={nearbyDetailQuery.data.imageUrl} fallbackSources={[selectedNearby.imageUrl]} category={nearbyDetailQuery.data.category} alt={nearbyDetailQuery.data.name} className="nearby-image" />
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
