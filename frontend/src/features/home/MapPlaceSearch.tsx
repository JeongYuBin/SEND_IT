import { useState, type FormEvent } from 'react'
import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { http } from '../../api/http'
import { createSavedPlace, searchKakaoPlaces } from '../saved/savedApi'
import type { CreateSavedPlace, SavedPlace } from '../saved/types'
import type { KakaoMapPoint } from '../../components/KakaoMap'
import './map-place-search.css'

type TourismResult = {
  contentId: string; contentTypeId: string; name: string; category: string
  address: string | null; latitude: number; longitude: number; imageUrl: string | null
  eventStartDate: string | null; eventEndDate: string | null
}
type Result = {
  id: string; name: string; address?: string | null; category?: string | null
  latitude?: number | null; longitude?: number | null; savedId?: number
  request?: CreateSavedPlace; url?: string | null
}

export function MapPlaceSearch({ places, onSelect }: {
  places: SavedPlace[]; onSelect: (point: KakaoMapPoint | null) => void
}) {
  const navigate = useNavigate()
  const cache = useQueryClient()
  const [input, setInput] = useState('')
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(1)
  const [open, setOpen] = useState(false)
  const [selected, setSelected] = useState<Result | null>(null)
  const tourism = useInfiniteQuery({
    queryKey: ['map-search-tourism-unified', query], enabled: query.length >= 2,
    initialPageParam: 1,
    queryFn: async ({ signal, pageParam }) => (await http.get<TourismResult[]>('/tourism/search', { params: { query, page: pageParam }, signal })).data,
    getNextPageParam: (last, pages) => last.length > 20 && pages.length < 100 ? pages.length + 1 : undefined,
    staleTime: 60_000,
  })
  const kakao = useInfiniteQuery({
    queryKey: ['map-search-kakao-unified', query], enabled: query.length >= 2,
    initialPageParam: 1,
    queryFn: ({ pageParam }) => searchKakaoPlaces(query, pageParam), staleTime: 60_000,
    getNextPageParam: (last) => !last.last && last.page < 45 ? last.page + 1 : undefined,
  })
  const save = useMutation({
    mutationFn: createSavedPlace,
    onSuccess: async (place) => {
      await cache.invalidateQueries({ queryKey: ['saved-places'] })
      setSelected((previous) => previous ? { ...previous, savedId: place.savedPlaceId } : null)
    },
  })
  const keyword = query.toLocaleLowerCase('ko')
  const savedMatches = places.filter((place) => [place.name, place.category, place.address, place.roadAddress, place.memo,
    ...place.sources.flatMap((source) => [source.title, source.description])].some((text) => text?.toLocaleLowerCase('ko').includes(keyword)))
  const candidates: Result[] = [...savedMatches.slice(0, page * 20).map((place) => ({
    ...place, id: `saved-${place.savedPlaceId}`, savedId: place.savedPlaceId, address: place.roadAddress ?? place.address,
  })), ...(tourism.data?.pages.flatMap((items) => items.slice(0, 20)) ?? []).map((place) => ({
    ...place, id: `tourism-${place.contentId}`,
    savedId: places.find((saved) => saved.tourismContentId === place.contentId)?.savedPlaceId,
    request: { name: place.name, category: place.category, address: place.address ?? undefined,
      latitude: place.latitude, longitude: place.longitude, imageUrl: place.imageUrl ?? undefined,
      tourismContentId: place.contentId, tourismContentTypeId: place.contentTypeId,
      eventStartDate: place.eventStartDate ?? undefined, eventEndDate: place.eventEndDate ?? undefined },
  })), ...(kakao.data?.pages.flatMap((items) => items.places) ?? []).map((place) => ({
    ...place, id: `kakao-${place.kakaoPlaceId}`, address: place.roadAddress ?? place.address, url: place.kakaoPlaceUrl,
    savedId: places.find((saved) => saved.kakaoPlaceId === place.kakaoPlaceId)?.savedPlaceId,
    request: { name: place.name, category: place.categoryGroup ?? place.category ?? undefined,
      address: place.address ?? undefined, roadAddress: place.roadAddress ?? undefined,
      latitude: place.latitude ?? undefined, longitude: place.longitude ?? undefined,
      kakaoPlaceId: place.kakaoPlaceId, kakaoPlaceUrl: place.kakaoPlaceUrl ?? undefined, phone: place.phone ?? undefined },
  }))]
  const normalize = (text?: string | null) => text?.toLocaleLowerCase('ko').replace(/\s+/g, '') ?? ''
  const results: Result[] = []
  for (const candidate of candidates) {
    const existing = results.find((item) => (item.savedId != null && item.savedId === candidate.savedId)
      || (normalize(item.name) === normalize(candidate.name) && (
        (!!item.address && normalize(item.address) === normalize(candidate.address))
        || (item.latitude != null && item.longitude != null && candidate.latitude != null && candidate.longitude != null
          && Math.abs(item.latitude - candidate.latitude) < 0.0003 && Math.abs(item.longitude - candidate.longitude) < 0.0003))))
    if (existing) {
      existing.savedId ??= candidate.savedId
      existing.url ??= candidate.url
    } else results.push({ ...candidate })
  }
  results.sort((a, b) => Number(!!b.savedId) - Number(!!a.savedId))
  const loading = tourism.isFetching || kakao.isFetching
  const error = tourism.isError || kakao.isError
  const hasNext = savedMatches.length > page * 20 || tourism.hasNextPage || kakao.hasNextPage
  const clearSelection = () => { setSelected(null); onSelect(null); save.reset() }
  const submit = (event: FormEvent) => {
    event.preventDefault()
    setQuery(input.trim()); setPage(1); setOpen(true); clearSelection()
  }
  return <div className="map-place-search">
    {open && <div className="map-search-panel">
      <div className="map-search-heading">
        <strong>검색 결과</strong>
        <button type="button" aria-label="검색 결과 접기" onClick={() => setOpen(false)}>⌄</button>
      </div>
      {selected ? <div className="map-search-selection">
        <button type="button" onClick={clearSelection}>‹ 검색 결과</button>
        <strong>{selected.name}</strong><small>{selected.category}</small><p>{selected.address ?? '주소 정보 없음'}</p>
        {selected.latitude == null && <small>등록된 위치 정보가 없습니다.</small>}
        <div className="map-search-actions">
          {selected.savedId ? <button type="button" onClick={() => navigate(`/saved/places/${selected.savedId}`)}>게시물·상세 보기</button>
            : <button type="button" disabled={save.isPending || !selected.request} onClick={() => selected.request && save.mutate(selected.request)}>{save.isPending ? '저장 중…' : '내 장소에 저장'}</button>}
          {selected.url && <a href={selected.url} target="_blank" rel="noreferrer">카카오맵 ↗</a>}
        </div>
        {save.isError && <p role="alert">저장하지 못했습니다. 다시 시도해 주세요.</p>}
      </div> : <div className="map-search-results" aria-live="polite">
        {query.length < 2 ? <p>장소명이나 지역을 두 글자 이상 입력해 주세요.</p>
          : <>
            {loading && <p>장소를 찾고 있어요…</p>}
            {error && <p>일부 검색 결과를 불러오지 못했습니다. <button onClick={() => {
              if (tourism.isError) void tourism.refetch()
              if (kakao.isError) void kakao.refetch()
            }}>다시 시도</button></p>}
            {!loading && !error && results.length === 0 && <p>검색 결과가 없습니다. 다른 장소명이나 지역을 입력해 주세요.</p>}
            {results.map((result) => <button className="map-search-result" type="button" key={result.id} onClick={() => {
              setSelected(result); save.reset()
              onSelect(result.latitude != null && result.longitude != null ? {
                id: result.id, name: result.name, label: result.name, category: result.category,
                latitude: result.latitude, longitude: result.longitude,
              } : null)
            }}><strong>{result.name}</strong><small>{result.savedId ? '저장됨' : result.category ?? '장소'}</small><span>{result.address ?? '주소 정보 없음'}</span></button>)}
            {hasNext && <div className="map-search-pagination">
              <button disabled={loading} onClick={() => {
                setPage(page + 1)
                if (tourism.hasNextPage) void tourism.fetchNextPage()
                if (kakao.hasNextPage) void kakao.fetchNextPage()
              }}>{loading ? '불러오는 중…' : '결과 더 보기'}</button>
            </div>}
          </>}
      </div>}
    </div>}
    <form className="map-search-form" onSubmit={submit}>
      <input type="search" aria-label="지도에서 장소 검색" placeholder="저장한 장소·여행지·카카오맵 검색" maxLength={100}
        value={input} onFocus={() => setOpen(true)} onChange={(event) => setInput(event.target.value)} enterKeyHint="search" />
      <button type="submit" disabled={input.trim().length < 2}>검색</button>
    </form>
  </div>
}
