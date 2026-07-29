import { useState } from 'react'
import axios from 'axios'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  deleteItinerary,
  getItinerary,
  reorderItineraryItems,
  updateItinerary,
  updateItineraryItemSchedule,
  updateItineraryItemTransport,
} from './itineraryApi'
import { ItineraryEditPanel } from './ItineraryEditPanel'
import { ItineraryRouteMap } from './ItineraryRouteMap'
import { PlaceScheduleEditor } from './PlaceScheduleEditor'
import { TransitRouteGuide } from './TransitRouteGuide'
import type {
  ItineraryStatus,
  TransportType,
  UpdateItinerary,
  UpdateItineraryItemSchedule,
  ItineraryDay,
  ReorderItineraryItem,
} from './types'

const transportLabels: Record<TransportType, string> = {
  WALKING: '도보',
  PUBLIC_TRANSIT: '대중교통',
  CAR: '자동차',
}

const statusLabels: Record<ItineraryStatus, string> = {
  DRAFT: '초안',
  GENERATED: '경로 생성 완료',
  COMPLETED: '여행 완료',
}

function time(value: string) {
  return value.slice(0, 5)
}

function requestErrorMessage(error: unknown) {
  if (!error) return null
  if (!axios.isAxiosError<{ message?: string; error?: string }>(error)) {
    return '변경 요청 중 알 수 없는 오류가 발생했습니다.'
  }
  if (!error.response) {
    return `백엔드 서버에 연결하지 못했습니다. 현재 접속 주소: ${window.location.origin}`
  }
  return error.response.data?.message
    ?? error.response.data?.error
    ?? `서버 요청 실패 (HTTP ${error.response.status})`
}

export function ItineraryDetailPage() {
  const { itineraryId } = useParams()
  const id = Number(itineraryId)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [editingPlan, setEditingPlan] = useState(false)
  const [editingOrder, setEditingOrder] = useState(false)
  const [orderDraft, setOrderDraft] = useState<ItineraryDay[] | null>(null)
  const [draggingPlaceId, setDraggingPlaceId] = useState<number | null>(null)
  const [editingPlaceId, setEditingPlaceId] = useState<number | null>(null)
  const [saveMessage, setSaveMessage] = useState<string | null>(null)
  const itineraryQuery = useQuery({
    queryKey: ['itineraries', id],
    queryFn: () => getItinerary(id),
    enabled: Number.isInteger(id) && id > 0,
  })
  const refresh = (data: Awaited<ReturnType<typeof getItinerary>>) => {
    queryClient.setQueryData(['itineraries', id], data)
    queryClient.invalidateQueries({ queryKey: ['itineraries'] })
  }
  const updateMutation = useMutation({
    mutationFn: (request: UpdateItinerary) => updateItinerary(id, request),
    onSuccess: (data) => {
      refresh(data)
      setEditingPlan(false)
      setSaveMessage('여행 계획 변경사항을 저장했습니다.')
    },
    onMutate: () => setSaveMessage(null),
  })
  const scheduleMutation = useMutation({
    mutationFn: ({
      savedPlaceId,
      request,
    }: {
      savedPlaceId: number
      request: UpdateItineraryItemSchedule
    }) => updateItineraryItemSchedule(id, savedPlaceId, request),
    onSuccess: (data) => {
      refresh(data)
      setEditingPlaceId(null)
      setSaveMessage('방문 일정과 체류 시간을 저장했습니다.')
    },
    onMutate: () => setSaveMessage(null),
  })
  const deleteMutation = useMutation({
    mutationFn: () => deleteItinerary(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['itineraries'] })
      navigate('/itineraries', { replace: true })
    },
  })
  const reorderMutation = useMutation({
    mutationFn: (items: ReorderItineraryItem[]) => reorderItineraryItems(id, items),
    onSuccess: (data) => {
      refresh(data)
      setEditingOrder(false)
      setOrderDraft(null)
      setSaveMessage('날짜와 방문 순서를 저장했습니다.')
    },
    onMutate: () => {
      setSaveMessage(null)
    },
  })
  const transportMutation = useMutation({
    mutationFn: ({
      savedPlaceId,
      transportType,
    }: {
      savedPlaceId: number
      transportType: TransportType
    }) => updateItineraryItemTransport(id, savedPlaceId, transportType),
    onSuccess: (data) => {
      refresh(data)
      setSaveMessage('이동수단을 변경했습니다.')
    },
    onMutate: async ({ savedPlaceId, transportType }) => {
      setSaveMessage(null)
      await queryClient.cancelQueries({ queryKey: ['itineraries', id] })
      const previous = queryClient.getQueryData<Awaited<ReturnType<typeof getItinerary>>>(
        ['itineraries', id],
      )
      if (previous) {
        const updateItem = (item: typeof previous.items[number]) =>
          item.savedPlaceId === savedPlaceId
            ? { ...item, transportTypeFromPrevious: transportType }
            : item
        queryClient.setQueryData(['itineraries', id], {
          ...previous,
          items: previous.items.map(updateItem),
          days: previous.days.map((day) => ({
            ...day,
            items: day.items.map(updateItem),
          })),
        })
      }
      return { previous }
    },
    onError: (_error, _variables, context) => {
      if (context?.previous) {
        queryClient.setQueryData(['itineraries', id], context.previous)
      }
    },
  })

  const handleDelete = () => {
    if (window.confirm('이 여행 계획을 삭제할까요? 삭제한 계획은 복구할 수 없습니다.')) {
      deleteMutation.mutate()
    }
  }

  const mutationError = updateMutation.error ?? scheduleMutation.error
  const mutationErrorMessage = requestErrorMessage(mutationError)
  const reorderErrorMessage = requestErrorMessage(reorderMutation.error)

  const startOrderEditing = () => {
    const days = itineraryQuery.data?.days
    if (!days) return
    reorderMutation.reset()
    setOrderDraft(days.map((day) => ({ ...day, items: [...day.items] })))
    setEditingOrder(true)
  }

  const cancelOrderEditing = () => {
    setEditingOrder(false)
    setOrderDraft(null)
    reorderMutation.reset()
  }

  const moveCard = (targetDate: string, targetIndex: number) => {
    if (draggingPlaceId === null) return
    setOrderDraft((current) => {
      if (!current) return current
      const dragged = current.flatMap((day) => day.items)
        .find((item) => item.savedPlaceId === draggingPlaceId)
      if (!dragged) return current
      return current.map((day) => {
        const items = day.items.filter((item) => item.savedPlaceId !== draggingPlaceId)
        if (day.date === targetDate) {
          items.splice(Math.min(targetIndex, items.length), 0, dragged)
        }
        return { ...day, items }
      })
    })
  }

  const saveOrder = () => {
    if (!orderDraft) return
    reorderMutation.reset()
    let sequence = 1
    const transportSlots = itineraryQuery.data?.days
      .flatMap((day) => day.items)
      .map((item) => item.transportTypeFromPrevious) ?? []
    let slotIndex = 0
    reorderMutation.mutate(orderDraft.flatMap((day) => day.items.map((item) => ({
      savedPlaceId: item.savedPlaceId,
      visitDate: day.date,
      sequence: sequence++,
      transportTypeFromPrevious: transportSlots[slotIndex++] ?? itineraryQuery.data!.transportType,
    }))))
  }

  if (!Number.isInteger(id) || id <= 0) {
    return <main className="itinerary-shell"><div className="form-error">올바르지 않은 여행 계획 주소입니다.</div></main>
  }

  return (
    <main className="itinerary-shell">
      <nav className="top-nav">
        <Link className="brand-link" to="/">SEND IT</Link>
        <div>
          <Link to="/itineraries">여행 계획</Link>
          <Link to="/saved">저장한 장소</Link>
        </div>
      </nav>
      {itineraryQuery.isLoading && <div className="empty-state detail-loading">여행 계획을 불러오고 있습니다.</div>}
      {itineraryQuery.isError && <div className="form-error detail-loading">여행 계획을 찾을 수 없습니다.</div>}
      {itineraryQuery.data && (
        <article className="itinerary-detail">
          <header>
            <span className="eyebrow">{statusLabels[itineraryQuery.data.status]}</span>
            <h1>{itineraryQuery.data.title}</h1>
            <p>
              {itineraryQuery.data.startDate} — {itineraryQuery.data.endDate}
              <span>
                {itineraryQuery.data.startDate} {time(itineraryQuery.data.dailyStartTime)}
                {' – '}
                {itineraryQuery.data.endDate} {time(itineraryQuery.data.dailyEndTime)}
              </span>
              <span>{transportLabels[itineraryQuery.data.transportType]}</span>
            </p>
            <div className="itinerary-header-actions">
              <button type="button" onClick={() => setEditingPlan((value) => !value)}>
                {editingPlan ? '편집 닫기' : '계획 수정'}
              </button>
              {editingOrder ? (
                <>
                  <button type="button" onClick={cancelOrderEditing}>순서 편집 취소</button>
                  <button
                    type="button"
                    className="primary-button"
                    disabled={reorderMutation.isPending}
                    onClick={saveOrder}
                  >
                    {reorderMutation.isPending ? '순서 저장 중…' : '순서 저장'}
                  </button>
                </>
              ) : (
                <button type="button" onClick={startOrderEditing}>날짜·순서 편집</button>
              )}
              <button className="danger-button" type="button" disabled={deleteMutation.isPending} onClick={handleDelete}>
                {deleteMutation.isPending ? '삭제 중…' : '계획 삭제'}
              </button>
            </div>
          </header>
          {editingPlan && (
            <ItineraryEditPanel
              itinerary={itineraryQuery.data}
              pending={updateMutation.isPending}
              errorMessage={updateMutation.isError ? mutationErrorMessage : null}
              onCancel={() => setEditingPlan(false)}
              onSave={(request) => updateMutation.mutate(request)}
            />
          )}
          {editingOrder && (
            <section className="inline-order-notice">
              <strong>장소 카드를 직접 끌어서 순서나 날짜를 변경하세요.</strong>
              <span>변경을 마치면 위의 ‘순서 저장’을 눌러 주세요.</span>
              {reorderMutation.isError && <div className="form-error">{reorderErrorMessage}</div>}
            </section>
          )}
          {saveMessage && <div className="form-success">{saveMessage}</div>}
          {(mutationErrorMessage || deleteMutation.isError) && (
            <div className="form-error">
              {mutationErrorMessage ?? '여행 계획을 삭제하지 못했습니다.'}
            </div>
          )}
          <section className="itinerary-notice">
            장소 좌표의 직선거리와 이동 수단별 평균 속도로 계산한 예상 동선입니다.
            실제 도로 상황과 대중교통 시간은 다를 수 있습니다.
          </section>
          <ItineraryRouteMap days={itineraryQuery.data.days} />
          <div className="itinerary-days">
            {(orderDraft ?? itineraryQuery.data.days).map((day) => (
              <section
                className={`itinerary-day ${editingOrder ? 'itinerary-day-dropzone' : ''}`}
                key={day.date}
                onDragOver={(event) => editingOrder && event.preventDefault()}
                onDrop={() => moveCard(day.date, day.items.length)}
              >
                <header className="itinerary-day-header">
                  <div>
                    <span>DAY {day.dayNumber}</span>
                    <h2>{day.date}</h2>
                  </div>
                  <small>{day.items.length}개 장소</small>
                </header>
                {day.exceedsDailyWindow && (
                  <div className="schedule-warning">
                    마지막 장소의 종료 시각이 여행 종료 일시를 넘습니다. 장소 수나 체류 시간을 조정해 주세요.
                  </div>
                )}
                {day.items.length === 0 ? (
                  <div className="day-empty">이 날짜에 배정된 장소가 없습니다.</div>
                ) : (
                  <ol className="itinerary-timeline">
                    {day.items.map((item, itemIndex) => (
                      <li
                        key={item.savedPlaceId}
                        draggable={editingOrder}
                        className={`${editingOrder ? 'timeline-draggable' : ''} ${draggingPlaceId === item.savedPlaceId ? 'dragging' : ''}`}
                        onDragStart={() => setDraggingPlaceId(item.savedPlaceId)}
                        onDragEnd={() => setDraggingPlaceId(null)}
                        onDragOver={(event) => editingOrder && event.preventDefault()}
                        onDrop={(event) => {
                          event.stopPropagation()
                          moveCard(day.date, itemIndex)
                        }}
                      >
                        <span className="timeline-number">{editingOrder ? itemIndex + 1 : item.daySequence}</span>
                        <div className="timeline-stop">
                          {editingOrder && <div className="card-drag-handle">⠿ 카드 이동</div>}
                          {!editingOrder && (
                            <>
                              {item.crossDayTransfer && (
                                <div className="cross-day-transfer-label">
                                  전날 마지막 장소에서 오늘 첫 장소로 이동
                                </div>
                              )}
                              {item.travelMinutesFromPrevious > 0 && (
                                <div className="segment-transport-control">
                                  <label className="segment-transport-select">
                                    이 구간 이동수단
                                    <select
                                      value={item.transportTypeFromPrevious}
                                      disabled={transportMutation.isPending}
                                      onChange={(event) => {
                                        transportMutation.reset()
                                        transportMutation.mutate({
                                          savedPlaceId: item.savedPlaceId,
                                          transportType: event.target.value as TransportType,
                                        })
                                      }}
                                    >
                                      <option value="PUBLIC_TRANSIT">대중교통</option>
                                      <option value="CAR">자동차</option>
                                      <option value="WALKING">도보</option>
                                    </select>
                                  </label>
                                  {transportMutation.isPending && <small>이동수단 저장 중…</small>}
                                  {transportMutation.isError && (
                                    <small className="field-error">
                                      {requestErrorMessage(transportMutation.error)}
                                    </small>
                                  )}
                                </div>
                              )}
                              {item.transit ? (
                                <TransitRouteGuide route={item.transit} />
                              ) : item.transportTypeFromPrevious === 'PUBLIC_TRANSIT'
                                && item.travelMinutesFromPrevious > 0 ? (
                                <div className="transit-unavailable">
                                  <strong>카카오맵 대중교통 경로 없음</strong>
                                  <span>
                                    이 구간은 카카오가 이용 가능한 버스·지하철 경로를 반환하지 않았습니다.
                                  </span>
                                  <small>
                                    가까운 거리는 도보 이동이 더 적합할 수 있으며, 직선거리 기반 시간은 표시하지 않습니다.
                                  </small>
                                </div>
                              ) : item.travelMinutesFromPrevious > 0 && (
                                <div className="travel-estimate">
                                  예상 이동 {item.travelMinutesFromPrevious}분
                                  {item.distanceKmFromPrevious !== null && ` · 약 ${item.distanceKmFromPrevious}km`}
                                  {!item.coordinateAvailable && ' · 좌표 없음'}
                                </div>
                              )}
                            </>
                          )}
                          <Link to={`/saved/places/${item.savedPlaceId}`}>
                            {item.imageUrl
                              ? <img src={item.imageUrl} alt="" />
                              : <div className="timeline-placeholder">{item.name.slice(0, 1)}</div>}
                            <span>
                              <small>{time(item.arrivalTime)}–{time(item.departureTime)} · 체류 {item.stayMinutes}분</small>
                              <strong>{item.name}</strong>
                              <span>{item.address ?? '주소 정보 없음'}</span>
                            </span>
                          </Link>
                          {!editingOrder && (editingPlaceId === item.savedPlaceId ? (
                            <PlaceScheduleEditor
                              item={item}
                              startDate={itineraryQuery.data.startDate}
                              endDate={itineraryQuery.data.endDate}
                              pending={scheduleMutation.isPending}
                              errorMessage={scheduleMutation.isError ? mutationErrorMessage : null}
                              onCancel={() => setEditingPlaceId(null)}
                              onSave={(request) => scheduleMutation.mutate({
                                savedPlaceId: item.savedPlaceId,
                                request,
                              })}
                            />
                          ) : (
                            <button
                              className="schedule-edit-button"
                              type="button"
                              onClick={() => setEditingPlaceId(item.savedPlaceId)}
                            >
                              방문 시간 설정
                            </button>
                          ))}
                        </div>
                      </li>
                    ))}
                  </ol>
                )}
              </section>
            ))}
          </div>
        </article>
      )}
    </main>
  )
}
