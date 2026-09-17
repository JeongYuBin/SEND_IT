import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { PlaceImage } from '../../components/PlaceImage'
import { reanalyzeShare } from '../share/shareApi'
import type { ShareDetail } from '../share/types'

export function PendingSavedPost({ post }: { post: ShareDetail }) {
  const navigate = useNavigate()
  const cache = useQueryClient()
  const retry = useMutation({ mutationFn: () => reanalyzeShare(post.shareId),
    onSuccess: () => cache.invalidateQueries({ queryKey: ['saved-shares'] }) })
  const failed = post.status === 'FAILED' || post.status === 'NEEDS_CONFIRMATION'
  return <article className="place-card pending-saved-post" role="link" tabIndex={0}
    onClick={() => navigate(`/shares/${post.shareId}`)}
    onKeyDown={event => { if (event.key === 'Enter' || event.key === ' ') navigate(`/shares/${post.shareId}`) }}>
    <div className="place-image"><PlaceImage src={post.thumbnailUrl} category={post.collectionName} alt="" /></div>
    <div className="place-content">
      <div className="place-meta"><span>{post.collectionName ?? '기타'}</span></div>
      <h2>{post.title ?? '저장한 게시물'}</h2>
      <p role="status">{failed ? '저장 완료 · 장소를 확인하지 못했어요.' : '저장 완료 · 장소를 자동으로 정리하고 있어요.'}</p>
      <div className="pending-post-actions" onClick={event => event.stopPropagation()} onKeyDown={event => event.stopPropagation()}>
        <a href={post.originalUrl} target="_blank" rel="noreferrer">원본 보기 ↗</a>
        {failed && <><button className="pending-post-edit" onClick={() => navigate(`/shares/${post.shareId}`)}>직접 수정</button>
          <button className="pending-post-retry" disabled={retry.isPending} onClick={() => retry.mutate()}>{retry.isPending ? '요청 중…' : '분석 재시도'}</button></>}
      </div>
      {retry.isError && <p role="alert">재분석 요청에 실패했습니다.</p>}
    </div>
  </article>
}
