import { useMutation, useQueryClient } from '@tanstack/react-query'
import { PlaceImage } from '../../components/PlaceImage'
import { reanalyzeShare } from '../share/shareApi'
import type { ShareDetail } from '../share/types'

export function PendingSavedPost({ post }: { post: ShareDetail }) {
  const cache = useQueryClient()
  const retry = useMutation({ mutationFn: () => reanalyzeShare(post.shareId),
    onSuccess: () => cache.invalidateQueries({ queryKey: ['saved-shares'] }) })
  const failed = post.status === 'FAILED' || post.status === 'NEEDS_CONFIRMATION'
  return <article className="place-card pending-saved-post">
    <div className="place-image"><PlaceImage src={post.thumbnailUrl} category={post.collectionName} alt="" /></div>
    <div className="place-content">
      <div className="place-meta"><span>{post.collectionName ?? '기타'}</span></div>
      <h2>{post.title ?? '저장한 게시물'}</h2>
      <p role="status">{failed ? '저장 완료 · 장소를 확인하지 못했어요.' : '저장 완료 · 장소를 자동으로 정리하고 있어요.'}</p>
      <a href={post.originalUrl} target="_blank" rel="noreferrer">원본 보기 ↗</a>
      {failed && <button disabled={retry.isPending} onClick={() => retry.mutate()}>분석 재시도</button>}
      {retry.isError && <p role="alert">재분석 요청에 실패했습니다.</p>}
    </div>
  </article>
}
