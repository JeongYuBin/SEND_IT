import { useEffect } from 'react'
import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { PlaceImage } from '../../components/PlaceImage'
import { getSavedSharesPage, reanalyzeShare } from '../share/shareApi'
import type { AnalysisStatus } from '../share/types'
import './saved-shared-posts.css'

const statusLabels: Record<AnalysisStatus, string> = {
  PENDING: '저장됨 · 분석 대기', ANALYZING: '저장됨 · 장소 찾는 중', COMPLETED: '저장됨',
  NEEDS_CONFIRMATION: '저장됨 · 장소 확인 필요', FAILED: '저장됨 · 분석 재시도 필요',
}

export function SavedSharedPosts({ collectionId }: { collectionId: number | null }) {
  const cache = useQueryClient()
  const query = useInfiniteQuery({
    queryKey: ['saved-shares', collectionId],
    queryFn: ({ pageParam }) => getSavedSharesPage(pageParam, collectionId),
    initialPageParam: 0,
    getNextPageParam: (last) => last.last ? undefined : last.page + 1,
    staleTime: 0, refetchOnWindowFocus: true,
    refetchInterval: (state) => state.state.data?.pages.some((page) => page.content.some(
      (post) => post.status === 'PENDING' || post.status === 'ANALYZING')) ? 3000 : 15000,
  })
  useEffect(() => {
    if (query.dataUpdatedAt) void cache.invalidateQueries({ queryKey: ['saved-places'] })
  }, [cache, query.dataUpdatedAt])
  const retry = useMutation({ mutationFn: reanalyzeShare,
    onSuccess: () => cache.invalidateQueries({ queryKey: ['saved-shares'] }),
  })
  const posts = query.data?.pages.flatMap((page) => page.content) ?? []
  if (query.isSuccess && posts.length === 0) return null
  return <section className="saved-shared-posts" aria-label="공유한 게시물">
    <header><h2>공유한 게시물</h2><span>{query.data?.pages[0].totalElements ?? '…'}개</span></header>
    {query.isLoading && <p role="status">저장한 게시물을 불러오고 있어요.</p>}
    {query.isError && <p role="alert">게시물을 불러오지 못했습니다. <button onClick={() => void query.refetch()}>다시 시도</button></p>}
    <div className="saved-shared-post-grid">
      {posts.map((post) => <article key={post.shareId}>
        <Link to={`/shares/${post.shareId}`}>
          <PlaceImage src={post.thumbnailUrl} category={post.collectionName} alt="" className="saved-shared-post-image" />
          <span className="saved-shared-post-copy">
            <small>{post.collectionName} · {post.sourceType}</small>
            <strong>{post.extractedPlaceName ?? post.title ?? '공유한 게시물'}</strong>
            <span className={`saved-shared-post-status ${post.status.toLowerCase()}`}>{statusLabels[post.status]}</span>
          </span>
        </Link>
        <footer><a href={post.originalUrl} target="_blank" rel="noreferrer">원본 보기 ↗</a>
          {(post.status === 'FAILED' || post.status === 'NEEDS_CONFIRMATION') && <button disabled={retry.isPending} onClick={() => retry.mutate(post.shareId)}>다시 분석</button>}
        </footer>
      </article>)}
    </div>
    {retry.isError && <p role="alert">재분석을 요청하지 못했습니다. 다시 시도해 주세요.</p>}
    {query.hasNextPage && <button className="saved-shared-post-more" disabled={query.isFetchingNextPage} onClick={() => void query.fetchNextPage()}>{query.isFetchingNextPage ? '불러오는 중…' : '게시물 더 보기'}</button>}
  </section>
}
