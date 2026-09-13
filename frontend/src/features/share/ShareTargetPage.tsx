import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuthStore } from '../../stores/authStore'
import { CollectionPicker } from '../saved/CollectionPicker'
import { createShare, getShare, selectShareCollection, type CreateShareInput } from './shareApi'
import { closeShareWindow } from '../../nativeBridge'
import './share-save-sheet.css'

const PENDING_SHARE_KEY = 'sendit-pending-share'

function extractUrl(value: string) {
  const match = value.match(/https?:\/\/[^\s<>"']+/i)
  if (!match) return null
  const candidate = match[0].replace(/[),.;!?\]}]+$/, '')
  try {
    return new URL(candidate).toString()
  } catch {
    return null
  }
}

function readPendingShare(searchParams: URLSearchParams): CreateShareInput | null {
  const title = searchParams.get('title')?.trim() ?? ''
  const text = searchParams.get('text')?.trim() ?? ''
  const urlParam = searchParams.get('url')?.trim() ?? ''
  const url = extractUrl(urlParam) ?? extractUrl(text)
  if (url) {
    return {
      url,
      sharedText: [title, text].filter(Boolean).join('\n').slice(0, 10_000) || undefined,
    }
  }

  const stored = sessionStorage.getItem(PENDING_SHARE_KEY)
  if (!stored) return null
  try {
    return JSON.parse(stored) as CreateShareInput
  } catch {
    sessionStorage.removeItem(PENDING_SHARE_KEY)
    return null
  }
}


export function ShareTargetPage() {
  const navigate = useNavigate()
  const cache = useQueryClient()
  const [params] = useSearchParams()
  const accessToken = useAuthStore((state) => state.accessToken)
  const started = useRef(false)
  const [request] = useState(() => readPendingShare(params))
  const existingId = Number(params.get('share')) || null
  const [done, setDone] = useState(false)
  const mutation = useMutation({ mutationFn: createShare })
  const shareId = mutation.data?.shareId ?? existingId
  const detail = useQuery({
    queryKey: ['share', shareId], enabled: !!accessToken && !!shareId,
    queryFn: () => getShare(shareId!),
    refetchInterval: (state) => ['PENDING', 'ANALYZING'].includes(state.state.data?.status ?? '') ? 1500 : false,
  })
  useEffect(() => {
    if (started.current || (!request && !existingId)) return
    if (!accessToken) {
      if (request) sessionStorage.setItem(PENDING_SHARE_KEY, JSON.stringify(request))
      // A share extension must not turn into a full login flow.
      if (params.has('native')) return
      navigate('/login', { replace: true, state: {
        returnTo: existingId ? `/share-target?share=${existingId}` : '/share-target?pending=1',
        message: '공유한 게시물을 저장하려면 로그인해 주세요.',
      } })
      return
    }
    if (existingId) return
    started.current = true
    mutation.mutate(request!, { onSuccess: () => sessionStorage.removeItem(PENDING_SHARE_KEY) })
  }, [accessToken, existingId, mutation, navigate, params, request])
  const processing = !detail.data || ['PENDING', 'ANALYZING'].includes(detail.data.status)
  return <main className="share-save-shell">
    <section className="share-save-sheet" aria-labelledby="share-save-title">
      <header><div><small>SEND IT</small><h1 id="share-save-title">{done ? '담기 완료!' : '어디에 담을까요?'}</h1></div>
        <button type="button" aria-label="공유 창 닫기" onClick={closeShareWindow}>×</button></header>
      {!accessToken ? <p>SendIT 앱에서 먼저 로그인한 뒤 다시 공유해 주세요.</p>
        : !request && !existingId ? <p>공유할 링크를 찾지 못했습니다.</p>
        : mutation.isError ? <><p role="alert">게시물을 받지 못했습니다.</p><button onClick={() => mutation.mutate(request!)}>다시 시도</button></>
        : !shareId ? <p role="status">게시물을 받고 있어요…</p>
        : done ? <>
          <p>선택한 컬렉션에 담았습니다.{processing ? ' 장소 분석이 끝나면 자동으로 추가됩니다.' : ''}</p>
          <div className="share-save-actions"><button onClick={closeShareWindow}>완료</button><button onClick={() => setDone(false)}>컬렉션 변경</button></div>
          {!params.has('native') && <><small>창이 닫히지 않으면 이전 앱으로 돌아가도 됩니다.</small><Link to={`/shares/${shareId}`}>분석 보기</Link></>}
        </> : <>
          <p className="share-save-preview">{detail.data?.extractedPlaceName ?? detail.data?.title ?? request?.url}</p>
          {processing && <p role="status">자동 추천을 분석 중이에요. 먼저 컬렉션을 선택해도 됩니다.</p>}
          {detail.isError && <p role="alert">분석 상태를 불러오지 못했습니다. <button onClick={() => detail.refetch()}>다시 시도</button></p>}
          {detail.data?.status === 'FAILED' && <p>자동 분석에 실패했습니다. 컬렉션을 지정하고 나중에 장소를 확인할 수 있어요.</p>}
          <CollectionPicker category={detail.data?.extractedCategory} collectionId={detail.data?.collectionId ?? undefined}
            onConfirm={async (id) => {
              await selectShareCollection(shareId, id)
              await cache.invalidateQueries({ queryKey: ['share', shareId] })
              setDone(true)
            }} />
        </>}
    </section>
  </main>
}
