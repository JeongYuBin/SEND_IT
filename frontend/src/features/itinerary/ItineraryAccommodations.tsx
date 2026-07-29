import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createSavedPlace,
  deleteSavedPlace,
  getSavedPlaces,
  getTourismAccommodations,
  getTourismPlaceDetail,
} from '../saved/savedApi'
import type { TourismAccommodation } from '../saved/types'
import type { ItineraryDay } from './types'

type Props = {
  day: ItineraryDay
}

export function ItineraryAccommodations({ day }: Props) {
  const queryClient = useQueryClient()
  const [selectedStay, setSelectedStay] = useState<TourismAccommodation | null>(null)
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
  const saveMutation = useMutation({
    mutationFn: (stay: TourismAccommodation) => createSavedPlace({
      name: stay.name,
      category: '숙박',
      address: stay.address ?? undefined,
      latitude: stay.latitude,
      longitude: stay.longitude,
      imageUrl: stay.imageUrl ?? undefined,
      tourismContentId: stay.contentId,
      tourismContentTypeId: stay.contentTypeId,
    }),
    onSuccess: (savedPlace) => {
      queryClient.setQueryData(
        ['saved-places'],
        (current: Awaited<ReturnType<typeof getSavedPlaces>> | undefined) => {
          if (!current) return [savedPlace]
          return current.some((place) => place.savedPlaceId === savedPlace.savedPlaceId)
            ? current
            : [savedPlace, ...current]
        },
      )
      queryClient.invalidateQueries({ queryKey: ['saved-places'] })
    },
  })
  const deleteMutation = useMutation({
    mutationFn: deleteSavedPlace,
    onSuccess: (_data, savedPlaceId) => {
      queryClient.setQueryData(
        ['saved-places'],
        (current: Awaited<ReturnType<typeof getSavedPlaces>> | undefined) =>
          current?.filter((place) => place.savedPlaceId !== savedPlaceId) ?? [],
      )
      queryClient.invalidateQueries({ queryKey: ['saved-places'] })
    },
  })

  if (!lastPlace || !coordinateAvailable) return null

  const savedByContentId = new Map(
    savedPlacesQuery.data
      ?.filter((place) => place.tourismContentId)
      .map((place) => [place.tourismContentId, place]),
  )
  const selectedSavedPlace = selectedStay
    ? savedByContentId.get(selectedStay.contentId)
    : undefined
  const mutationPending = saveMutation.isPending || deleteMutation.isPending

  const toggleSaved = (stay: TourismAccommodation) => {
    const savedPlace = savedByContentId.get(stay.contentId)
    if (savedPlace) {
      deleteMutation.mutate(savedPlace.savedPlaceId)
    } else {
      saveMutation.mutate(stay)
    }
  }

  return (
    <section className="itinerary-accommodations day-recommendation">
      <header>
        <div>
          <span className="eyebrow">DAY {day.dayNumber} STAY</span>
          <h2>이날 숙소 추천</h2>
        </div>
        <p>{lastPlace.name} 반경 10km · {accommodationQuery.data?.length ?? 0}곳</p>
      </header>
      {(saveMutation.isError || deleteMutation.isError) && (
        <div className="form-error">숙소의 저장 상태를 변경하지 못했습니다. 다시 시도해 주세요.</div>
      )}
      <article className="overnight-stay">
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
        {!!accommodationQuery.data?.length && (
          <div className="stay-carousel" aria-label={`DAY ${day.dayNumber} 주변 숙소`}>
            {accommodationQuery.data.map((stay) => {
                    const saved = savedByContentId.has(stay.contentId)
                    const changing = (saveMutation.isPending
                      && saveMutation.variables?.contentId === stay.contentId)
                      || (deleteMutation.isPending
                        && savedByContentId.get(stay.contentId)?.savedPlaceId
                          === deleteMutation.variables)
                    return (
                      <article
                        className="stay-card"
                        key={stay.contentId}
                        onClick={() => setSelectedStay(stay)}
                      >
                        {stay.imageUrl
                          ? <img src={stay.imageUrl} alt="" />
                          : <div className="stay-placeholder">숙박</div>}
                        <div>
                          <span>약 {(stay.distanceMeters / 1000).toFixed(1)}km</span>
                          <h3>
                            <button type="button" onClick={() => setSelectedStay(stay)}>
                              {stay.name}
                            </button>
                          </h3>
                          <p>{stay.address ?? '주소 정보 없음'}</p>
                          <button
                            className={`stay-save-button ${saved ? 'saved' : ''}`}
                            type="button"
                            disabled={mutationPending}
                            onClick={(event) => {
                              event.stopPropagation()
                              toggleSaved(stay)
                            }}
                          >
                            {changing ? '처리 중...' : saved ? '저장 취소' : '+ 내 장소에 저장'}
                          </button>
                        </div>
                      </article>
                    )
            })}
          </div>
        )}
      </article>
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
                  ? <img src={detailQuery.data?.imageUrl ?? selectedStay.imageUrl!} alt="" />
                  : <div className="nearby-placeholder"><strong>숙박</strong><small>제공 이미지 없음</small></div>}
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
                      className={`stay-save-button ${selectedSavedPlace ? 'saved' : ''}`}
                      type="button"
                      disabled={mutationPending}
                      onClick={() => toggleSaved(selectedStay)}
                    >
                      {mutationPending
                        ? '처리 중...'
                        : selectedSavedPlace ? '내 장소에서 삭제' : '+ 내 장소에 저장'}
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
