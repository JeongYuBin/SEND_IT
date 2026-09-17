import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { createSavedPlace, searchKakaoPlaces } from '../saved/savedApi'
import { getShare, reanalyzeShare, selectShareCollection } from './shareApi'

import { CollectionPicker } from '../saved/CollectionPicker'
import { CollectionChoiceCancelled } from '../saved/collectionChoice'
import { PlaceImage } from '../../components/PlaceImage'

const processingStatuses = new Set(['PENDING', 'ANALYZING'])
const editableStatuses = new Set(['COMPLETED', 'NEEDS_CONFIRMATION', 'FAILED'])

function normalizeAddress(value: string | null | undefined) {
  return value?.replace(/[^0-9a-z가-힣]/gi, '').toLocaleLowerCase() ?? ''
}

function conciseTitle(value: string | null) {
  if (!value) return '장소 정보를 확인해 주세요'
  const withoutInstagram = value
    .replace(/^.+? on Instagram:\s*[“"']?/i, '')
    .replace(/[”"']?\s*•\s*Instagram.*$/i, '')
    .trim()
  return withoutInstagram || '장소 정보를 확인해 주세요'
}

export function ShareResultPage() {
  const { shareId: shareIdParam } = useParams()
  const shareId = Number(shareIdParam)
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [category, setCategory] = useState('')
  const [address, setAddress] = useState('')
  const [memo, setMemo] = useState('')
  const initialized = useRef(false)

  const shareQuery = useQuery({
    queryKey: ['share', shareId],
    queryFn: () => getShare(shareId),
    enabled: Number.isFinite(shareId),
    refetchInterval: (query) =>
      processingStatuses.has(query.state.data?.status ?? '') ? 1500 : false,
  })
  const extractedAddress = shareQuery.data?.extractedAddress?.trim() ?? ''
  const addressLookup = useQuery({
    queryKey: ['share-address-place', extractedAddress],
    queryFn: () => searchKakaoPlaces(extractedAddress),
    enabled: Boolean(extractedAddress && !shareQuery.data?.extractedPlaceName),
    staleTime: 5 * 60_000,
  })
  const resolvedAddressPlace = addressLookup.data?.places.find((place) => {
    const expected = normalizeAddress(extractedAddress)
    return [place.roadAddress, place.address].some((candidate) => {
      const normalized = normalizeAddress(candidate)
      return Boolean(normalized) && (normalized === expected || normalized.includes(expected) || expected.includes(normalized))
    })
  })

  useEffect(() => {
    const share = shareQuery.data
    if (!share || initialized.current || !editableStatuses.has(share.status)) return
    setName(share.extractedPlaceName ?? '')
    setCategory(share.extractedCategory ?? '')
    setAddress(share.extractedAddress ?? '')
    initialized.current = true
  }, [shareQuery.data])

  useEffect(() => {
    if (!resolvedAddressPlace || name.trim()) return
    setName(resolvedAddressPlace.name)
    setCategory(resolvedAddressPlace.categoryGroup ?? resolvedAddressPlace.category ?? '')
    setAddress(resolvedAddressPlace.roadAddress ?? resolvedAddressPlace.address ?? extractedAddress)
  }, [extractedAddress, name, resolvedAddressPlace])

  const saveMutation = useMutation({
    mutationFn: createSavedPlace,
    onSuccess: () => navigate('/saved', { replace: true }),
  })
  const reanalyzeMutation = useMutation({
    mutationFn: () => reanalyzeShare(shareId),
    onSuccess: () => {
      initialized.current = false
      setName('')
      setCategory('')
      setAddress('')
      shareQuery.refetch()
    },
  })

  const handleSave = (event: FormEvent) => {
    event.preventDefault()
    if (!shareQuery.data) return
    saveMutation.mutate({
      name,
      category: category || undefined,
      address: address || undefined,
      memo: memo || undefined,
      sharedContentId: shareQuery.data.shareId,
      imageUrl: shareQuery.data.thumbnailUrl ?? undefined,
      latitude: shareQuery.data.extractedLatitude ?? resolvedAddressPlace?.latitude ?? undefined,
      longitude: shareQuery.data.extractedLongitude ?? resolvedAddressPlace?.longitude ?? undefined,
    })
  }

  if (shareQuery.isLoading) {
    return <main className="result-shell"><div className="analysis-state"><span className="spinner" />분석 결과를 불러오고 있습니다.</div></main>
  }
  if (shareQuery.isError || !shareQuery.data) {
    return <main className="result-shell"><div className="analysis-state error">분석 결과를 찾을 수 없습니다.<Link to="/">홈으로 돌아가기</Link></div></main>
  }

  const share = shareQuery.data
  const isProcessing = processingStatuses.has(share.status)
  const canSave = editableStatuses.has(share.status)
  const previewTitle = share.extractedPlaceName
    ?? resolvedAddressPlace?.name
    ?? (canSave ? '장소 정보를 확인해 주세요' : conciseTitle(share.title))

  return (
    <main className="result-shell">
      <nav className="top-nav">
        <Link className="brand-link" to="/">SEND IT</Link>
        <div>
          <Link to="/saved">저장한 장소</Link>
          <Link to="/profile">내 정보</Link>
          <Link to="/settings">설정</Link>
          <Link to="/notifications">알림</Link>
        </div>
      </nav>
      <section className="result-layout">
        <div className="result-preview">
          <span className="eyebrow">분석 결과</span>
          <PlaceImage src={share.thumbnailUrl} category={share.extractedCategory} className="preview-placeholder" />
          <div className="source-badge">{share.sourceType}</div>
          <h1>{previewTitle}</h1>
          <p>{canSave && share.extractedAddress
            ? `${share.extractedAddress} 주소를 기준으로 장소 정보를 확인하고 있습니다.`
            : (share.description ?? conciseTitle(share.title))}</p>
          <a href={share.originalUrl} target="_blank" rel="noreferrer">원본 콘텐츠 열기 ↗</a>
          {!isProcessing && (
            <button
              className="reanalyze-button"
              type="button"
              disabled={reanalyzeMutation.isPending}
              onClick={() => reanalyzeMutation.mutate()}
            >
              {reanalyzeMutation.isPending ? '재분석 요청 중...' : '다시 분석하기'}
            </button>
          )}
          {reanalyzeMutation.isError && <div className="form-error">재분석을 요청하지 못했습니다.</div>}
        </div>

        <aside className="save-panel">
          {isProcessing && (
            <div className="analysis-state"><span className="spinner" /><strong>콘텐츠를 분석하고 있어요.</strong><small>잠시만 기다리면 결과가 자동으로 표시됩니다.</small></div>
          )}
          {share.status === 'FAILED' && (
            <div className="form-error">자동 분석을 완료하지 못했습니다. 장소 정보를 직접 입력해 저장할 수 있습니다.</div>
          )}
          {share.status === 'NEEDS_CONFIRMATION' && (
            <div className="auto-fill-notice">
              장소 정보를 자동으로 확정하지 못했습니다. 추출된 후보를 확인하거나 실제 장소명을 입력해 주세요.
            </div>
          )}
          {share.extractedPlaces.length > 0 && (
            <section className="extracted-place-list" aria-labelledby="extracted-place-title">
              <div className="extracted-place-heading">
                <div>
                  <span className="eyebrow">EXTRACTED PLACES</span>
                  <h2 id="extracted-place-title">찾은 장소 {share.extractedPlaces.length}곳</h2>
                </div>
                <span className="saved-count">
                  {share.extractedPlaces.filter((place) => place.savedPlaceId !== null).length}곳 저장됨
                </span>
              </div>
              <ol>
                {share.extractedPlaces.map((place) => (
                  <li key={place.id}>
                    <span className="place-order">{place.order}</span>
                    <div>
                      <strong>{place.name}</strong>
                      <small>{[place.category, place.address].filter(Boolean).join(' · ')}</small>
                    </div>
                    {place.savedPlaceId !== null
                      ? <Link to={`/saved/places/${place.savedPlaceId}`}>저장됨</Link>
                      : <span>저장 대기</span>}
                  </li>
                ))}
              </ol>
              <CollectionPicker category={share.extractedCategory} collectionId={share.collectionId ?? undefined} onConfirm={async (id) => {
                await selectShareCollection(shareId, id); await shareQuery.refetch()
              }} />
            </section>
          )}
          {canSave && share.extractedPlaces.length === 0 && (
            <form className="result-form" onSubmit={handleSave}>
              <h2>장소 정보 확인</h2>
              {(share.extractedPlaceName || share.extractedAddress) && (
                <div className="auto-fill-notice">구조화된 장소 정보를 자동으로 채웠습니다. 저장 전에 내용을 확인해 주세요.</div>
              )}
              <label>장소명 *<input required value={name} onChange={(e) => setName(e.target.value)} placeholder="실제 장소명을 입력하세요" /></label>
              <label>주소<input value={address} onChange={(e) => setAddress(e.target.value)} placeholder="방문할 주소를 입력하세요" /></label>
              <label>메모<textarea value={memo} onChange={(e) => setMemo(e.target.value)} placeholder="이 장소에 대한 메모" /></label>
              {saveMutation.isError && !(saveMutation.error instanceof CollectionChoiceCancelled) && <div className="form-error">장소를 저장하지 못했습니다. 입력 내용을 확인해 주세요.</div>}
              <button disabled={saveMutation.isPending}>{saveMutation.isPending ? '저장 중...' : '내 장소에 저장'}</button>
            </form>
          )}
        </aside>
      </section>
    </main>
  )
}
