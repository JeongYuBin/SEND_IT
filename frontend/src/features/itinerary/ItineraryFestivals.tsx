import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createSavedPlace,
  getSavedPlaces,
  getTourismFestivals,
  getTourismPlaceDetail,
} from '../saved/savedApi'
import { addItineraryItem, removeItineraryItem } from './itineraryApi'
import type { TourismFestival } from '../saved/types'
import type { Itinerary } from './types'
import { PlaceImage } from '../../components/PlaceImage'

type Props = {
  itinerary: Itinerary
}

function dateRange(startDate: string | null, endDate: string | null) {
  if (!startDate && !endDate) return '행사 기간 정보 없음'
  if (startDate === endDate || !endDate) return startDate
  return `${startDate ?? endDate} – ${endDate}`
}

export function ItineraryFestivals({ itinerary }: Props) {
  const queryClient = useQueryClient()
  const [selectedFestival, setSelectedFestival] = useState<TourismFestival | null>(null)
  const coordinates = itinerary.items
    .filter((item) => item.latitude !== null && item.longitude !== null)
    .map((item) => ({
      latitude: item.latitude!,
      longitude: item.longitude!,
    }))

  const festivalsQuery = useQuery({
    queryKey: ['tourism-events', itinerary.id, itinerary.startDate, itinerary.endDate],
    queryFn: async () => {
      const responses = await Promise.all(coordinates.map((coordinate) =>
        getTourismFestivals(
          itinerary.startDate,
          itinerary.endDate,
          coordinate.latitude,
          coordinate.longitude,
        )))
      const unique = new Map(responses.flat().map((festival) => [festival.contentId, festival]))
      return [...unique.values()]
        .sort((left, right) => left.distanceMeters - right.distanceMeters)
        .slice(0, 6)
    },
    enabled: coordinates.length > 0,
    staleTime: 1000 * 60 * 30,
  })
  const savedPlacesQuery = useQuery({
    queryKey: ['saved-places'],
    queryFn: getSavedPlaces,
  })
  const detailQuery = useQuery({
    queryKey: ['tourism-place-detail', selectedFestival?.contentId, '15'],
    queryFn: () => getTourismPlaceDetail(selectedFestival!.contentId, '15'),
    enabled: selectedFestival !== null,
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
    mutationFn: async ({
      festival,
      visitDate,
    }: {
      festival: TourismFestival
      visitDate: string
    }) => {
      let savedPlace = savedByContentId.get(festival.contentId)
      if (savedPlace && routeSavedPlaceIds.has(savedPlace.savedPlaceId)) {
        return removeItineraryItem(itinerary.id, savedPlace.savedPlaceId)
      }
      const isNewSavedPlace = !savedPlace
      const needsEventPeriod = savedPlace
        && ((!savedPlace.eventStartDate && festival.startDate)
          || (!savedPlace.eventEndDate && festival.endDate))
      if (!savedPlace || needsEventPeriod) {
        savedPlace = await createSavedPlace({
          name: festival.name,
          category: '행사',
          address: festival.address ?? undefined,
          latitude: festival.latitude,
          longitude: festival.longitude,
          imageUrl: festival.imageUrl ?? undefined,
          tourismContentId: festival.contentId,
          tourismContentTypeId: '15',
          eventStartDate: festival.startDate ?? undefined,
          eventEndDate: festival.endDate ?? undefined,
        })
        if (isNewSavedPlace) {
          queryClient.setQueryData(
            ['saved-places'],
            (current: Awaited<ReturnType<typeof getSavedPlaces>> | undefined) =>
              current ? [savedPlace!, ...current] : [savedPlace!],
          )
        }
      }
      return addItineraryItem(itinerary.id, savedPlace.savedPlaceId, visitDate)
    },
    onSuccess: (updatedItinerary) => {
      queryClient.setQueryData(['itineraries', itinerary.id], updatedItinerary)
      queryClient.invalidateQueries({ queryKey: ['itineraries'] })
      queryClient.invalidateQueries({ queryKey: ['saved-places'] })
    },
  })

  if (coordinates.length === 0) return null

  return (
    <section className="itinerary-festivals">
      <header>
        <div>
          <span className="eyebrow">TRIP EVENTS</span>
          <h2>여행 기간 주변 행사</h2>
        </div>
        <p>{itinerary.startDate} – {itinerary.endDate} · 전체 일정 장소 반경 30km</p>
      </header>
      {festivalsQuery.isLoading && <div className="empty-state">여행 기간의 행사를 찾고 있습니다.</div>}
      {festivalsQuery.isError && (
        <div className="empty-state">행사 정보를 불러오지 못했습니다. 잠시 후 다시 확인해 주세요.</div>
      )}
      {festivalsQuery.data?.length === 0 && (
        <div className="empty-state">여행 기간과 동선 주변에 등록된 행사가 없습니다.</div>
      )}
      {!!festivalsQuery.data?.length && (
        <div className="itinerary-festival-grid">
          {festivalsQuery.data.map((festival) => (
            <article
              key={festival.contentId}
              role="button"
              tabIndex={0}
              onClick={() => setSelectedFestival(festival)}
              onKeyDown={(event) => {
                if (event.key === 'Enter' || event.key === ' ') {
                  event.preventDefault()
                  setSelectedFestival(festival)
                }
              }}
            >
              {festival.imageUrl
                ? (
                  <PlaceImage
                    src={festival.imageUrl}
                    alt={`${festival.name} 대표 이미지`}
                    className="festival-image-skeleton"
                  />
                )
                : (
                  <div
                    className="place-image-skeleton festival-image-skeleton"
                    role="img"
                    aria-label="행사 이미지 준비 중"
                  >
                    <i className="place-image-skeleton-sun" />
                    <i className="place-image-skeleton-mountain" />
                    <i className="place-image-skeleton-ground" />
                  </div>
                )}
              <div>
                <span>{dateRange(festival.startDate, festival.endDate)}</span>
                <h3>{festival.name}</h3>
                <p>{festival.address ?? '장소 정보 없음'}</p>
                <small>일정 장소에서 약 {(festival.distanceMeters / 1000).toFixed(1)}km</small>
                <button
                  className="festival-detail-button"
                  type="button"
                  onClick={(event) => {
                    event.stopPropagation()
                    setSelectedFestival(festival)
                  }}
                >
                  상세 보기
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
      {routeMutation.isError && (
        <div className="form-error">행사의 경로 상태를 변경하지 못했습니다. 다시 시도해 주세요.</div>
      )}
      {selectedFestival && (
        <div
          className="nearby-detail-backdrop"
          role="presentation"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) setSelectedFestival(null)
          }}
        >
          <section
            className="nearby-detail-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="festival-detail-title"
          >
            <button
              className="nearby-detail-close"
              type="button"
              aria-label="행사 상세정보 닫기"
              onClick={() => setSelectedFestival(null)}
            >
              ×
            </button>
            {detailQuery.isLoading && (
              <div className="analysis-state">
                <span className="spinner" />행사 상세정보를 불러오고 있습니다.
              </div>
            )}
            {!detailQuery.isLoading && (
              <>
                {(detailQuery.data?.imageUrl ?? selectedFestival.imageUrl)
                  ? (
                    <PlaceImage
                      src={detailQuery.data?.imageUrl ?? selectedFestival.imageUrl}
                      alt={`${selectedFestival.name} 대표 이미지`}
                      className="festival-detail-skeleton"
                    />
                  )
                  : (
                    <div
                      className="place-image-skeleton festival-detail-skeleton"
                      role="img"
                      aria-label="행사 이미지 준비 중"
                    >
                      <i className="place-image-skeleton-sun" />
                      <i className="place-image-skeleton-mountain" />
                      <i className="place-image-skeleton-ground" />
                    </div>
                  )}
                <div className="nearby-detail-content">
                  <span className="eyebrow">FESTIVAL</span>
                  <h2 id="festival-detail-title">
                    {detailQuery.data?.name ?? selectedFestival.name}
                  </h2>
                  <p className="nearby-detail-address">
                    {detailQuery.data?.address ?? selectedFestival.address ?? '주소 정보 없음'}
                  </p>
                  <div className="festival-period">
                    {dateRange(selectedFestival.startDate, selectedFestival.endDate)}
                  </div>
                  <div className="nearby-detail-description">
                    {detailQuery.data?.description ?? '등록된 행사 상세 설명이 없습니다.'}
                  </div>
                  {detailQuery.data && (
                    <dl>
                      {detailQuery.data.operatingHours && (
                        <div><dt>행사·이용시간</dt><dd>{detailQuery.data.operatingHours}</dd></div>
                      )}
                      {detailQuery.data.phone && (
                        <div><dt>문의</dt><dd>{detailQuery.data.phone}</dd></div>
                      )}
                      {detailQuery.data.parkingInfo && (
                        <div><dt>주차 안내</dt><dd>{detailQuery.data.parkingInfo}</dd></div>
                      )}
                    </dl>
                  )}
                  {detailQuery.isError && (
                    <p className="field-error">일부 상세정보를 불러오지 못해 기본정보만 표시합니다.</p>
                  )}
                  {(() => {
                    const savedPlace = savedByContentId.get(selectedFestival.contentId)
                    const included = savedPlace
                      ? routeSavedPlaceIds.has(savedPlace.savedPlaceId)
                      : false
                    if (included) {
                      return (
                        <button
                          className="festival-route-remove"
                          type="button"
                          disabled={routeMutation.isPending}
                          onClick={() => routeMutation.mutate({
                            festival: selectedFestival,
                            visitDate: itinerary.startDate,
                          })}
                        >
                          {routeMutation.isPending ? '처리 중...' : '경로에서 제거'}
                        </button>
                      )
                    }
                    return (
                      <div className="festival-day-actions">
                        <strong>추가할 날짜 선택</strong>
                        <div>
                          {itinerary.days
                            .filter((day) =>
                              (!selectedFestival.startDate
                                || day.date >= selectedFestival.startDate)
                              && (!selectedFestival.endDate
                                || day.date <= selectedFestival.endDate))
                            .map((day) => (
                            <button
                              type="button"
                              key={day.date}
                              disabled={routeMutation.isPending}
                              onClick={() => routeMutation.mutate({
                                festival: selectedFestival,
                                visitDate: day.date,
                              })}
                            >
                              DAY {day.dayNumber}
                              <small>{day.date}</small>
                            </button>
                            ))}
                        </div>
                      </div>
                    )
                  })()}
                </div>
              </>
            )}
          </section>
        </div>
      )}
    </section>
  )
}
