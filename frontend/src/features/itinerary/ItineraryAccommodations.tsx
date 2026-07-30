import { useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createSavedPlace,
  getSavedPlaces,
  getTourismAccommodations,
  getTourismPlaceDetail,
} from '../saved/savedApi'
import { addItineraryItem, removeItineraryItem } from './itineraryApi'
import type { TourismAccommodation } from '../saved/types'
import type { Itinerary, ItineraryDay } from './types'
import { PlaceImage } from '../../components/PlaceImage'

type Props = {
  itinerary: Itinerary
  day: ItineraryDay
}

export function ItineraryAccommodations({ itinerary, day }: Props) {
  const queryClient = useQueryClient()
  const [selectedStay, setSelectedStay] = useState<TourismAccommodation | null>(null)
  const [stayIndex, setStayIndex] = useState(0)
  const [expanded, setExpanded] = useState(true)
  const touchStartX = useRef<number | null>(null)
  const lastPlace = day.items.at(-1)
  const coordinateAvailable = lastPlace?.latitude !== null
    && lastPlace?.latitude !== undefined
    && lastPlace?.longitude !== null
    && lastPlace?.longitude !== undefined
  const accommodationQuery = useQuery({
    queryKey: ['tourism-accommodations', day.date, lastPlace?.latitude, lastPlace?.longitude],
    queryFn: () => getTourismAccommodations(lastPlace!.latitude!, lastPlace!.longitude!),
    enabled: coordinateAvailable,
    staleTime: 1000 * 60 * 30,
  })
  const savedPlacesQuery = useQuery({
    queryKey: ['saved-places'],
    queryFn: getSavedPlaces,
  })
  const detailQuery = useQuery({
    queryKey: [
      'tourism-place-detail',
      selectedStay?.contentId,
      selectedStay?.contentTypeId,
    ],
    queryFn: () => getTourismPlaceDetail(
      selectedStay!.contentId,
      selectedStay!.contentTypeId,
    ),
    enabled: selectedStay !== null,
  })
  const savedByContentId = new Map(
    savedPlacesQuery.data
      ?.filter((place) => place.tourismContentId)
      .map((place) => [place.tourismContentId, place]),
  )
  const routeSavedPlaceIds = new Set(
    itinerary.items.map((item) => item.savedPlaceId),
  )
  const routeMutation = useMutation({
    mutationFn: async (stay: TourismAccommodation) => {
      let savedPlace = savedByContentId.get(stay.contentId)
      if (savedPlace && routeSavedPlaceIds.has(savedPlace.savedPlaceId)) {
        return removeItineraryItem(itinerary.id, savedPlace.savedPlaceId)
      }
      if (!savedPlace) {
        savedPlace = await createSavedPlace({
          name: stay.name,
          category: '숙박',
          address: stay.address ?? undefined,
          latitude: stay.latitude,
          longitude: stay.longitude,
          imageUrl: stay.imageUrl ?? undefined,
          tourismContentId: stay.contentId,
          tourismContentTypeId: stay.contentTypeId,
        })
        queryClient.setQueryData(
          ['saved-places'],
          (current: Awaited<ReturnType<typeof getSavedPlaces>> | undefined) =>
            current ? [savedPlace!, ...current] : [savedPlace!],
        )
      }
      return addItineraryItem(itinerary.id, savedPlace.savedPlaceId, day.date)
    },
    onSuccess: (updatedItinerary) => {
      queryClient.setQueryData(['itineraries', itinerary.id], updatedItinerary)
      queryClient.invalidateQueries({ queryKey: ['itineraries'] })
      queryClient.invalidateQueries({ queryKey: ['saved-places'] })
    },
  })
  const selectedSavedPlace = selectedStay
    ? savedByContentId.get(selectedStay.contentId)
    : undefined
  const selectedInRoute = selectedSavedPlace
    ? routeSavedPlaceIds.has(selectedSavedPlace.savedPlaceId)
    : false
  const stays = accommodationQuery.data ?? []
  const visibleIndex = stays.length === 0 ? 0 : Math.min(stayIndex, stays.length - 1)
  const visibleStay = stays[visibleIndex]
  const showPrevious = () => setStayIndex((current) =>
    current === 0 ? stays.length - 1 : current - 1)
  const showNext = () => setStayIndex((current) =>
    current >= stays.length - 1 ? 0 : current + 1)

  if (!lastPlace || !coordinateAvailable) return null

  return (
    <section className="itinerary-accommodations day-recommendation">
      <header>
        <div>
          <span className="eyebrow">DAY {day.dayNumber} STAY</span>
          <h2>이날 숙소 추천</h2>
        </div>
        <div className="stay-recommendation-controls">
          <p>{lastPlace.name} 반경 10km · {accommodationQuery.data?.length ?? 0}곳</p>
          <button
            className="stay-toggle-button"
            type="button"
            aria-expanded={expanded}
            onClick={() => setExpanded((current) => !current)}
          >
            {expanded ? '숙소 추천 접기' : '숙소 추천 보기'}
            <span aria-hidden="true">{expanded ? '⌃' : '⌄'}</span>
          </button>
        </div>
      </header>
      {routeMutation.isError && (
        <div className="form-error">숙소의 경로 상태를 변경하지 못했습니다. 다시 시도해 주세요.</div>
      )}
      {expanded && <article className="overnight-stay">
        <div className="overnight-stay-heading">
          <span>DAY {day.dayNumber} 숙박</span>
          <strong>{lastPlace.name} 주변</strong>
          <small>{day.date} 일정 종료 후</small>
        </div>
        {accommodationQuery.isLoading && <div className="empty-state">주변 숙소를 찾고 있습니다.</div>}
        {accommodationQuery.isError && <div className="empty-state">숙소 정보를 불러오지 못했습니다.</div>}
        {accommodationQuery.data?.length === 0 && (
          <div className="empty-state">반경 10km 안에 관광공사 등록 숙소가 없습니다.</div>
        )}
        {visibleStay && (
          <div
            className="stay-slideshow"
            aria-label={`DAY ${day.dayNumber} 주변 숙소`}
            onTouchStart={(event) => {
              touchStartX.current = event.touches[0]?.clientX ?? null
            }}
            onTouchEnd={(event) => {
              if (touchStartX.current === null) return
              const endX = event.changedTouches[0]?.clientX ?? touchStartX.current
              const distance = endX - touchStartX.current
              touchStartX.current = null
              if (Math.abs(distance) < 45) return
              if (distance < 0) showNext()
              else showPrevious()
            }}
          >
            <button
              className="stay-slide-arrow previous"
              type="button"
              aria-label="이전 숙소"
              onClick={showPrevious}
            >
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M15 5 8 12l7 7" />
              </svg>
            </button>
            {(() => {
                    const stay = visibleStay
                    const savedPlace = savedByContentId.get(stay.contentId)
                    const included = savedPlace
                      ? routeSavedPlaceIds.has(savedPlace.savedPlaceId)
                      : false
                    const changing = routeMutation.isPending
                      && routeMutation.variables?.contentId === stay.contentId
                    return (
                      <article
                        className="stay-card"
                        key={stay.contentId}
                        onClick={() => setSelectedStay(stay)}
                      >
                        {stay.imageUrl
                          ? (
                            <PlaceImage
                              src={stay.imageUrl}
                              alt={`${stay.name} 대표 이미지`}
                              className="stay-placeholder"
                            />
                          )
                          : (
                            <div className="stay-skeleton" role="img" aria-label="숙소 이미지 준비 중">
                              <span className="stay-skeleton-building" />
                              <span className="stay-skeleton-ground" />
                            </div>
                          )}
                        <div>
                          <span>약 {(stay.distanceMeters / 1000).toFixed(1)}km</span>
                          <h3>
                            <button type="button" onClick={() => setSelectedStay(stay)}>
                              {stay.name}
                            </button>
                          </h3>
                          <p>{stay.address ?? '주소 정보 없음'}</p>
                          <button
                            className={`stay-save-button ${included ? 'saved' : ''}`}
                            type="button"
                            disabled={routeMutation.isPending}
                            onClick={(event) => {
                              event.stopPropagation()
                              routeMutation.mutate(stay)
                            }}
                          >
                            {changing ? '처리 중...' : included ? '경로에서 제거' : '+ 경로에 추가'}
                          </button>
                        </div>
                      </article>
                    )
            })()}
            <button
              className="stay-slide-arrow next"
              type="button"
              aria-label="다음 숙소"
              onClick={showNext}
            >
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="m9 5 7 7-7 7" />
              </svg>
            </button>
            <div className="stay-slide-dots" aria-label={`${stays.length}개 숙소 중 ${visibleIndex + 1}번째`}>
              {stays.map((stay, index) => (
                <button
                  className={index === visibleIndex ? 'active' : ''}
                  type="button"
                  key={stay.contentId}
                  aria-label={`${index + 1}번째 숙소 보기`}
                  onClick={() => setStayIndex(index)}
                />
              ))}
            </div>
          </div>
        )}
      </article>}
      {selectedStay && (
        <div
          className="nearby-detail-backdrop"
          role="presentation"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) setSelectedStay(null)
          }}
        >
          <section
            className="nearby-detail-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="stay-detail-title"
          >
            <button
              className="nearby-detail-close"
              type="button"
              onClick={() => setSelectedStay(null)}
              aria-label="숙소 상세정보 닫기"
            >
              ×
            </button>
            {detailQuery.isLoading && (
              <div className="analysis-state"><span className="spinner" />숙소 상세정보를 불러오고 있습니다.</div>
            )}
            {!detailQuery.isLoading && (
              <>
                {(detailQuery.data?.imageUrl ?? selectedStay.imageUrl)
                  ? (
                    <PlaceImage
                      src={detailQuery.data?.imageUrl ?? selectedStay.imageUrl}
                      alt={`${selectedStay.name} 대표 이미지`}
                      className="stay-placeholder"
                    />
                  )
                  : (
                    <div className="stay-skeleton detail" role="img" aria-label="숙소 이미지 준비 중">
                      <span className="stay-skeleton-building" />
                      <span className="stay-skeleton-ground" />
                    </div>
                  )}
                <div className="nearby-detail-content">
                  <span className="eyebrow">ACCOMMODATION</span>
                  <h2 id="stay-detail-title">{detailQuery.data?.name ?? selectedStay.name}</h2>
                  <p className="nearby-detail-address">
                    {detailQuery.data?.address ?? selectedStay.address ?? '주소 정보 없음'}
                  </p>
                  <div className="nearby-detail-description">
                    {detailQuery.data?.description ?? '등록된 상세 설명이 없습니다.'}
                  </div>
                  {detailQuery.data && (
                    <dl>
                      {detailQuery.data.operatingHours && (
                        <div><dt>체크인·이용시간</dt><dd>{detailQuery.data.operatingHours}</dd></div>
                      )}
                      {detailQuery.data.parkingInfo && (
                        <div><dt>주차 안내</dt><dd>{detailQuery.data.parkingInfo}</dd></div>
                      )}
                      {detailQuery.data.phone && (
                        <div><dt>문의</dt><dd>{detailQuery.data.phone}</dd></div>
                      )}
                    </dl>
                  )}
                  {detailQuery.isError && (
                    <p className="field-error">일부 상세정보를 불러오지 못해 기본정보만 표시합니다.</p>
                  )}
                  <div className="nearby-detail-actions">
                    {detailQuery.data?.homepageUrl && (
                      <a href={detailQuery.data.homepageUrl} target="_blank" rel="noreferrer">
                        공식 홈페이지 열기
                      </a>
                    )}
                    <button
                      className={`stay-save-button ${selectedInRoute ? 'saved' : ''}`}
                      type="button"
                      disabled={routeMutation.isPending}
                      onClick={() => routeMutation.mutate(selectedStay)}
                    >
                      {routeMutation.isPending
                        ? '처리 중...'
                        : selectedInRoute ? '경로에서 제거' : '+ 이 DAY 경로에 추가'}
                    </button>
                  </div>
                </div>
              </>
            )}
          </section>
        </div>
      )}
    </section>
  )
}
