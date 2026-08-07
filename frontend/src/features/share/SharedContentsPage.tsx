import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { PlaceImage } from '../../components/PlaceImage'
import { getShares } from './shareApi'
import type { AnalysisStatus } from './types'

const sourceLabels = {
  INSTAGRAM: 'Instagram',
  TIKTOK: 'TikTok',
  YOUTUBE: 'YouTube',
  NAVER_BLOG: '네이버 블로그',
  MAP: '지도',
  WEB: '웹페이지',
}

const statusLabels: Record<AnalysisStatus, string> = {
  PENDING: '분석 대기',
  ANALYZING: '분석 중',
  COMPLETED: '분석 완료',
  NEEDS_CONFIRMATION: '확인 필요',
  FAILED: '분석 실패',
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

export function SharedContentsPage() {
  const sharesQuery = useQuery({ queryKey: ['shares'], queryFn: getShares, refetchInterval: 5000 })

  return (
    <main className="shared-contents-shell">
      <nav className="top-nav">
        <Link className="brand-link" to="/">SEND IT</Link>
        <div>
          <Link to="/saved">저장한 장소</Link>
          <Link to="/profile">내 정보</Link>
          <Link to="/settings">설정</Link>
        </div>
      </nav>

      <header className="shared-contents-header">
        <span className="eyebrow">SHARED CONTENTS</span>
        <h1>받은 콘텐츠</h1>
        <p>SNS에서 SEND IT으로 보낸 게시물과 장소 분석 상태를 확인해 보세요.</p>
      </header>

      {sharesQuery.isLoading ? (
        <div className="shared-contents-state">받은 콘텐츠를 불러오고 있습니다.</div>
      ) : sharesQuery.isError ? (
        <div className="shared-contents-state error">받은 콘텐츠를 불러오지 못했습니다.</div>
      ) : sharesQuery.data?.length ? (
        <section className="shared-content-list" aria-label="받은 콘텐츠 목록">
          {sharesQuery.data.map((share) => (
            <Link to={`/shares/${share.shareId}`} className="shared-content-card" key={share.shareId}>
              {share.thumbnailUrl ? (
                <PlaceImage src={share.thumbnailUrl} alt="" />
              ) : (
                <span className="shared-content-placeholder" aria-hidden="true">
                  {sourceLabels[share.sourceType].slice(0, 1)}
                </span>
              )}
              <span className="shared-content-copy">
                <span className="shared-content-meta">
                  <b>{sourceLabels[share.sourceType]}</b>
                  <small>{formatDate(share.createdAt)}</small>
                </span>
                <strong>{share.extractedPlaceName ?? share.title ?? '장소 정보를 분석하고 있습니다.'}</strong>
                <span>{share.description ?? share.sharedText ?? '게시물 설명이 없습니다.'}</span>
                <em className={`analysis-badge ${share.status.toLowerCase()}`}>
                  {statusLabels[share.status]}
                </em>
              </span>
            </Link>
          ))}
        </section>
      ) : (
        <div className="shared-contents-state">
          <strong>아직 받은 콘텐츠가 없습니다.</strong>
          <span>SNS 게시물의 공유하기에서 SEND IT을 선택해 보세요.</span>
          <Link to="/">URL 직접 저장하기</Link>
        </div>
      )}
    </main>
  )
}
