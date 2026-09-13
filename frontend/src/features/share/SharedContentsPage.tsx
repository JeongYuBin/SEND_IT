import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { PlaceImage } from '../../components/PlaceImage'
import { deleteAllShares, deleteShare, getSharesPage } from './shareApi'
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
  const queryClient = useQueryClient()
  const [deleteTarget, setDeleteTarget] = useState<number | 'all' | null>(null)
  const sharesQuery = useInfiniteQuery({
    queryKey: ['shares', 'page'],
    queryFn: ({ pageParam }) => getSharesPage(pageParam),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => lastPage.last ? undefined : lastPage.page + 1,
    refetchInterval: 5000,
  })
  const shares = sharesQuery.data?.pages.flatMap((page) => page.content) ?? []
  const totalElements = sharesQuery.data?.pages[0]?.totalElements ?? 0
  const deleteMutation = useMutation({
    mutationFn: deleteShare,
    onSuccess: () => {
      setDeleteTarget(null)
      queryClient.invalidateQueries({ queryKey: ['shares'] })
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
  })
  const deleteAllMutation = useMutation({
    mutationFn: deleteAllShares,
    onSuccess: () => {
      setDeleteTarget(null)
      queryClient.invalidateQueries({ queryKey: ['shares'] })
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
  })

  return (
    <main className="shared-contents-shell">
      <nav className="top-nav">
        <Link className="brand-link" to="/">SEND IT</Link>
        <div>
          <Link to="/saved">저장한 장소</Link>
          <Link to="/profile">내 정보</Link>
          <Link to="/settings">설정</Link>
          <Link to="/notifications">알림</Link>
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
      ) : shares.length ? (
        <>
        <div className="shared-content-toolbar">
          <button type="button" onClick={() => setDeleteTarget('all')}>전체 삭제</button>
          <span>전체 {totalElements}개</span>
        </div>
        <section className="shared-content-list" aria-label="받은 콘텐츠 목록">
          {shares.map((share) => (
            <article className="shared-content-card-wrap" key={share.shareId}>
              <Link to={`/shares/${share.shareId}`} className="shared-content-card">
              <PlaceImage src={share.thumbnailUrl} category={share.collectionName} alt="" />
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
              <button
                className="shared-content-delete"
                type="button"
                disabled={deleteMutation.isPending || ['PENDING', 'ANALYZING'].includes(share.status)}
                onClick={() => setDeleteTarget(share.shareId)}
                aria-label="받은 콘텐츠 삭제"
              >삭제</button>
            </article>
          ))}
        </section>
        {sharesQuery.hasNextPage && (
          <button
            className="shared-content-more"
            type="button"
            disabled={sharesQuery.isFetchingNextPage}
            onClick={() => sharesQuery.fetchNextPage()}
          >
            {sharesQuery.isFetchingNextPage ? '불러오는 중...' : '더 보기'}
          </button>
        )}
        </>
      ) : (
        <div className="shared-contents-state">
          <strong>아직 받은 콘텐츠가 없습니다.</strong>
          <span>SNS 게시물의 공유하기에서 SEND IT을 선택해 보세요.</span>
          <Link to="/">URL 직접 저장하기</Link>
        </div>
      )}
      <ConfirmDialog
        open={deleteTarget !== null}
        title={deleteTarget === 'all' ? '받은 콘텐츠를 모두 삭제할까요?' : '받은 콘텐츠를 삭제할까요?'}
        description={deleteTarget === 'all'
          ? `받은 콘텐츠 ${totalElements}개와 분석용 파일을 삭제합니다. 이미 저장한 장소는 그대로 유지됩니다.`
          : '콘텐츠와 분석용 영상 파일만 삭제됩니다. 이미 저장한 장소는 그대로 유지됩니다.'}
        confirmLabel={deleteTarget === 'all' ? '전체 삭제' : '삭제'}
        pending={deleteMutation.isPending || deleteAllMutation.isPending}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => {
          if (deleteTarget === 'all') deleteAllMutation.mutate()
          else if (deleteTarget !== null) deleteMutation.mutate(deleteTarget)
        }}
      />
    </main>
  )
}
