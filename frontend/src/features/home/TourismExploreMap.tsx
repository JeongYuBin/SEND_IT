import { useCallback, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { http } from '../../api/http'
import { KakaoMap, type KakaoMapPoint, type MapViewport } from '../../components/KakaoMap'
import { PlaceImage } from '../../components/PlaceImage'
import { createSavedPlace } from '../saved/savedApi'
import type { CreateSavedPlace } from '../saved/types'

type MapPlace = {
  contentId: string; contentTypeId: string; name: string; category: string
  address: string | null; latitude: number; longitude: number; imageUrl: string | null
  eventStartDate: string | null; eventEndDate: string | null
}
type Snapshot = { points: KakaoMapPoint[]; total: number; updatedAt: string | null; stale: boolean; failed: boolean }
const NATIONAL_CENTER = { latitude: 36.3, longitude: 127.8 }
const EMPTY_POINTS: KakaoMapPoint[] = []

export function TourismExploreMap({ mode }: { mode: 'nearby' | 'festival' }) {
  const navigate = useNavigate()
  const cache = useQueryClient()
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [center, setCenter] = useState(NATIONAL_CENTER)
  const [viewport, setViewport] = useState<MapViewport | null>(null)
  const [locationError, setLocationError] = useState('')
  const month = new Date().toLocaleDateString('sv-SE', { timeZone: 'Asia/Seoul' }).slice(0, 7)
  const onViewportChange = useCallback((next: MapViewport) => {
    const rounded = Object.fromEntries(Object.entries(next).map(([key, value]) => [key, Number(value.toFixed(5))])) as MapViewport
    setViewport((previous) => JSON.stringify(previous) === JSON.stringify(rounded) ? previous : rounded)
  }, [])
  const query = useQuery({
    queryKey: ['tourism-viewport', mode, month, viewport], enabled: viewport !== null,
    queryFn: async ({ signal }) => (await http.get<Snapshot>('/tourism/discover', { params: { mode, ...viewport }, signal })).data,
    staleTime: 60_000, gcTime: 120_000, retry: 1,
    refetchInterval: (state) => state.state.data && !state.state.data.updatedAt && !state.state.data.failed ? 10_000 : false,
  })
  const detail = useQuery({
    queryKey: ['tourism-catalog-detail', mode, selectedId], enabled: selectedId !== null,
    queryFn: async ({ signal }) => (await http.get<MapPlace>(`/tourism/discover/${encodeURIComponent(selectedId!)}`, { params: { mode }, signal })).data,
    staleTime: 600_000,
  })
  const selected = detail.data
  const save = useMutation({ mutationFn: (place: MapPlace) => {
    const request: CreateSavedPlace = {
      name: place.name, category: place.category, address: place.address ?? undefined,
      latitude: place.latitude, longitude: place.longitude, imageUrl: place.imageUrl ?? undefined,
      tourismContentId: place.contentId, tourismContentTypeId: place.contentTypeId,
      eventStartDate: place.eventStartDate ?? undefined, eventEndDate: place.eventEndDate ?? undefined,
    }
    return createSavedPlace(request)
  }, onSuccess: async (place) => {
    await cache.invalidateQueries({ queryKey: ['saved-places'] })
    navigate(`/?place=${place.savedPlaceId}`)
  } })
  const onSelect = useCallback((point: KakaoMapPoint) => { setSelectedId(String(point.id)) }, [])
  const locate = () => {
    if (!navigator.geolocation) { setLocationError('현재 위치를 지원하지 않는 브라우저입니다.'); return }
    navigator.geolocation.getCurrentPosition(({ coords }) => {
      setCenter({ latitude: coords.latitude, longitude: coords.longitude }); setLocationError('')
    }, () => setLocationError('위치 권한을 허용하거나 지도를 직접 이동해 주세요.'), { timeout: 8000 })
  }
  const points = query.data?.points ?? EMPTY_POINTS
  return <section className="home-map tourism-explore" aria-label="관광공사 여행 지도">
    <KakaoMap points={points} fitPoints={false} initialCenter={center} initialLevel={13}
      onViewportChange={onViewportChange}
      ariaLabel={`${mode === 'festival' ? '축제' : '여행지'} ${query.data?.total ?? 0}곳 지도`} onSelect={onSelect} />
    <div className="tourism-map-toolbar">
      <button onClick={() => navigate('/saved')}>‹ 게시물</button>
      <button aria-pressed={mode === 'nearby'} onClick={() => navigate('/?explore=nearby')}>여행지</button>
      <button aria-pressed={mode === 'festival'} onClick={() => navigate('/?explore=festival')}>이번 달 축제</button>
      <button onClick={locate} aria-label="내 위치로 이동">◎</button>
    </div>
    <div className="tourism-map-status" role="status">
      한국관광공사 · {mode === 'festival' ? `${month} 축제` : '여행지'} · 화면 내 {query.data?.total ?? 0}곳
      <small>{query.isFetching ? '현재 지도 범위를 확인하는 중…' : '숫자를 누르면 확대됩니다. 장소를 눌러 자세히 확인하세요.'}</small>
      {query.data && !query.data.updatedAt && <small>{query.data.failed ? '최초 데이터 수집에 실패했습니다. 잠시 후 다시 확인해 주세요.' : '첫 관광 데이터를 준비 중입니다. 완료되면 자동 표시됩니다.'}</small>}
      {query.data?.updatedAt && (query.data.stale || query.data.failed) && <small>최근 갱신이 지연되어 이전 데이터를 표시합니다.</small>}
      {query.data?.updatedAt && !query.isFetching && query.data.total === 0 && <small>이 범위에는 표시할 장소가 없습니다. 지도를 이동해 보세요.</small>}
      {(query.isError || query.data?.failed) && <button onClick={() => query.refetch()}>새로 확인</button>}
      {locationError && <small>{locationError}</small>}
    </div>
    {selectedId && <div className="tourism-map-detail">
      <button className="tourism-detail-close" aria-label="장소 정보 닫기" onClick={() => { setSelectedId(null); save.reset() }}>×</button>
      {detail.isPending && <p role="status">장소 정보를 불러오는 중…</p>}
      {detail.isError && <button onClick={() => detail.refetch()}>장소 정보를 불러오지 못했습니다 · 다시 시도</button>}
      {selected && <>
        <PlaceImage src={selected.imageUrl} alt={selected.name} category={selected.category} />
        <div className="tourism-detail-body"><small>{selected.category}</small><strong>{selected.name}</strong><p>{selected.address}</p>
          {selected.eventStartDate && <small>{selected.eventStartDate} ~ {selected.eventEndDate}</small>}
          <button className="tourism-save-button" disabled={save.isPending} aria-busy={save.isPending} onClick={() => save.mutate(selected)}>
            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M7 4h10a1 1 0 0 1 1 1v16l-6-4-6 4V5a1 1 0 0 1 1-1Z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" /><path d="M9 10h6m-3-3v6" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" /></svg>
            {save.isPending ? '저장하는 중' : '내 장소에 저장'}
          </button>
          {save.isError && <small role="alert">저장하지 못했습니다. 다시 시도해 주세요.</small>}
        </div>
      </>}
    </div>}
  </section>
}
