import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { createSavedPlace, getNearbyTourismPlaces, getTourismFestivals } from './savedApi'
import type { CreateSavedPlace, SavedPlace } from './types'
import { PlaceImage } from '../../components/PlaceImage'

export function FeedDiscovery({ places }: { places: SavedPlace[] }) {
  const [mode, setMode] = useState<'nearby' | 'festival' | null>(null)
  const [anchorId, setAnchorId] = useState('')
  const anchors = places.filter((place) => place.latitude != null && place.longitude != null)
  const anchor = anchors.find((place) => String(place.savedPlaceId) === anchorId) ?? anchors[0]
  const navigate = useNavigate()
  const cache = useQueryClient()
  const now = new Date()
  const date = (value: Date) => `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`
  const start = date(now)
  const end = date(new Date(now.getFullYear(), now.getMonth() + 1, 0))
  const results = useQuery({
    queryKey: ['feed-discovery', mode, anchor?.savedPlaceId, start, end],
    enabled: mode !== null && Boolean(anchor),
    staleTime: 300_000,
    queryFn: async (): Promise<CreateSavedPlace[]> => {
      if (!anchor) return []
      if (mode === 'festival') return (await getTourismFestivals(start, end, anchor.latitude!, anchor.longitude!, 30000)).map((item) => ({
        name: item.name, category: '행사', address: item.address ?? undefined,
        latitude: item.latitude, longitude: item.longitude, imageUrl: item.imageUrl ?? undefined,
        tourismContentId: item.contentId, tourismContentTypeId: '15',
        eventStartDate: item.startDate ?? undefined, eventEndDate: item.endDate ?? undefined,
      }))
      return (await getNearbyTourismPlaces(anchor.latitude!, anchor.longitude!, 10000)).map((item) => ({
        name: item.name, category: item.category ?? undefined, address: item.address ?? undefined,
        latitude: item.latitude, longitude: item.longitude, imageUrl: item.imageUrl ?? undefined,
        tourismContentId: item.contentId, tourismContentTypeId: item.contentTypeId,
      }))
    },
  })
  const save = useMutation({ mutationFn: createSavedPlace, onSuccess: async (place) => {
    await cache.invalidateQueries({ queryKey: ['saved-places'] })
    navigate(`/?place=${place.savedPlaceId}`)
  } })
  return <section className="feed-discovery">
    <div className="discovery-shortcuts">
      <button aria-pressed={mode === 'nearby'} onClick={() => setMode(mode === 'nearby' ? null : 'nearby')}><span>주변 여행지<br />찾기</span><span aria-hidden="true">✦</span></button>
      <button aria-pressed={mode === 'festival'} onClick={() => setMode(mode === 'festival' ? null : 'festival')}><span>이번 달 축제<br />찾기</span><span aria-hidden="true">♫</span></button>
    </div>
    {mode && <div className="discovery-results">
      <header><strong>{mode === 'nearby' ? '함께 들를 여행지' : '여행에 더할 축제'}</strong><button onClick={() => setMode(null)}>닫기</button></header>
      <p>한국관광공사 제공 · {mode === 'nearby' ? '반경 10km' : `반경 30km · ${start} ~ ${end}`}</p>
      {anchor ? <label>이 장소 주변에서 찾기<select value={String(anchor.savedPlaceId)} onChange={(event) => setAnchorId(event.target.value)}>{anchors.map((place) => <option key={place.savedPlaceId} value={place.savedPlaceId}>{place.name}</option>)}</select></label>
        : <p>위치가 있는 장소를 먼저 저장해 주세요.</p>}
      {results.isFetching && <p role="status">여행 정보를 불러오는 중…</p>}
      {results.isError && <p role="alert">정보를 불러오지 못했습니다. <button onClick={() => results.refetch()}>다시 시도</button></p>}
      {results.isSuccess && results.data.length === 0 && <p>이 주변에서 제공되는 정보가 없습니다. 다른 기준 장소를 선택해 보세요.</p>}
      {save.isError && <p role="alert">저장하지 못했습니다. 다시 시도해 주세요.</p>}
      <div className="discovery-grid">{results.data?.map((item) => <article key={item.tourismContentId}>
        <PlaceImage src={item.imageUrl} alt={item.name} />
        <strong>{item.name}</strong><small>{item.address}</small>
        {item.eventStartDate && <small>{item.eventStartDate} ~ {item.eventEndDate}</small>}
        <button disabled={save.isPending} onClick={() => save.mutate(item)}>저장하고 지도 보기</button>
      </article>)}</div>
    </div>}
  </section>
}
