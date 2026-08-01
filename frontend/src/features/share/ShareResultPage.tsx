import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getCollections, createSavedPlace } from '../saved/savedApi'
import { getShare, reanalyzeShare } from './shareApi'

const processingStatuses = new Set(['PENDING', 'ANALYZING'])

export function ShareResultPage() {
  const { shareId: shareIdParam } = useParams()
  const shareId = Number(shareIdParam)
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [category, setCategory] = useState('')
  const [address, setAddress] = useState('')
  const [memo, setMemo] = useState('')
  const [collectionId, setCollectionId] = useState<number | undefined>()
  const initialized = useRef(false)

  const shareQuery = useQuery({
    queryKey: ['share', shareId],
    queryFn: () => getShare(shareId),
    enabled: Number.isFinite(shareId),
    refetchInterval: (query) =>
      processingStatuses.has(query.state.data?.status ?? '') ? 1500 : false,
  })
  const collectionsQuery = useQuery({ queryKey: ['collections'], queryFn: getCollections })

  useEffect(() => {
    const share = shareQuery.data
    if (!share || initialized.current || share.status !== 'COMPLETED') return
    setName(share.extractedPlaceName ?? share.title ?? '')
    setCategory(share.extractedCategory ?? '')
    setAddress(share.extractedAddress ?? '')
    initialized.current = true
  }, [shareQuery.data])

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
      collectionId,
      sharedContentId: shareQuery.data.shareId,
      description: shareQuery.data.description ?? undefined,
      imageUrl: shareQuery.data.thumbnailUrl ?? undefined,
      latitude: shareQuery.data.extractedLatitude ?? undefined,
      longitude: shareQuery.data.extractedLongitude ?? undefined,
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
  const canSave = share.status === 'COMPLETED' || share.status === 'FAILED'

  return (
    <main className="result-shell">
      <nav className="top-nav">
        <Link className="brand-link" to="/">SEND IT</Link>
        <div>
          <Link to="/saved">저장한 장소</Link>
          <Link to="/profile">내 정보</Link>
          <Link to="/settings">설정</Link>
        </div>
      </nav>
      <section className="result-layout">
        <div className="result-preview">
          <span className="eyebrow">ANALYSIS RESULT</span>
          {share.thumbnailUrl ? <img src={share.thumbnailUrl} alt="" /> : <div className="preview-placeholder">SEND IT</div>}
          <div className="source-badge">{share.sourceType}</div>
          <h1>{share.title ?? '장소 정보를 찾는 중이에요.'}</h1>
          <p>{share.description ?? '원본 콘텐츠에서 설명을 가져오지 못했습니다.'}</p>
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
          {canSave && (
            <form className="result-form" onSubmit={handleSave}>
              <h2>장소 정보 확인</h2>
              {(share.extractedPlaceName || share.extractedAddress) && (
                <div className="auto-fill-notice">구조화된 장소 정보를 자동으로 채웠습니다. 저장 전에 내용을 확인해 주세요.</div>
              )}
              <label>장소명 *<input required value={name} onChange={(e) => setName(e.target.value)} placeholder="실제 장소명을 입력하세요" /></label>
              <label>카테고리<input value={category} onChange={(e) => setCategory(e.target.value)} placeholder="관광지, 음식점, 카페..." /></label>
              <label>주소<input value={address} onChange={(e) => setAddress(e.target.value)} placeholder="방문할 주소를 입력하세요" /></label>
              <label>컬렉션<select value={collectionId ?? ''} onChange={(e) => setCollectionId(e.target.value ? Number(e.target.value) : undefined)}>
                <option value="">컬렉션 없음</option>
                {collectionsQuery.data?.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
              </select></label>
              <label>메모<textarea value={memo} onChange={(e) => setMemo(e.target.value)} placeholder="이 장소에 대한 메모" /></label>
              {saveMutation.isError && <div className="form-error">장소를 저장하지 못했습니다. 입력 내용을 확인해 주세요.</div>}
              <button disabled={saveMutation.isPending}>{saveMutation.isPending ? '저장 중...' : '내 장소에 저장'}</button>
            </form>
          )}
        </aside>
      </section>
    </main>
  )
}
