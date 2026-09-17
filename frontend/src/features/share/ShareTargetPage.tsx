import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuthStore } from '../../stores/authStore'
import { CollectionPicker } from '../saved/CollectionPicker'
import { createShare, getShare, selectShareCollection, type CreateShareInput } from './shareApi'
import { closeShareWindow, resizeShareWindow } from '../../nativeBridge'
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
  const [done, setDone] = useState(!!existingId)
  const [changingCollection, setChangingCollection] = useState(false)
  useEffect(() => { resizeShareWindow(changingCollection) }, [changingCollection])
  const mutation = useMutation({ mutationFn: createShare, onSuccess: () => {
    sessionStorage.removeItem(PENDING_SHARE_KEY)
    setDone(true)
    void cache.invalidateQueries({ queryKey: ['saved-shares'] })
  } })
  const { mutate } = mutation
  const shareId = mutation.data?.shareId ?? existingId
  const detail = useQuery({
    queryKey: ['share', shareId], enabled: !!accessToken && !!shareId,
    queryFn: () => getShare(shareId!),
    refetchInterval: (state) => ['PENDING', 'ANALYZING'].includes(state.state.data?.status ?? '') ? 1500 : false,
  })
  useEffect(() => {
    if (!done || changingCollection || !params.has('native')) return
    const timer = window.setTimeout(closeShareWindow, 4000)
    return () => window.clearTimeout(timer)
  }, [done, changingCollection, params])
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
    // Start after effect setup: StrictMode's cleanup must not detach the mutation observer.
    let cancelled = false
    queueMicrotask(() => {
      if (cancelled || started.current) return
      started.current = true
      mutate(request!, { onSuccess: () => sessionStorage.removeItem(PENDING_SHARE_KEY) })
    })
    return () => { cancelled = true }
  }, [accessToken, existingId, mutate, navigate, params, request])
  return <main className={`share-save-shell${params.has('native') ? ' native-share' : ''}`}>
    <section className="share-save-sheet" aria-labelledby="share-save-title">
      <header><h1 id="share-save-title">{changingCollection ? '컬렉션 변경' : done ? '게시물을 저장했어요' : '저장 중…'}</h1>
        <button type="button" aria-label="공유 창 닫기" onClick={closeShareWindow}>×</button></header>
      {!accessToken ? <p>SendIT 앱에서 먼저 로그인한 뒤 다시 공유해 주세요.</p>
        : !request && !existingId ? <p>공유할 링크를 찾지 못했습니다.</p>
        : mutation.isError ? <><p role="alert">게시물을 받지 못했습니다.</p><button onClick={() => mutation.mutate(request!)}>다시 시도</button></>
        : !shareId ? <p role="status">게시물을 받고 있어요…</p>
        : done && !changingCollection ? <>
          <div className="share-save-actions"><button onClick={() => setChangingCollection(true)}>컬렉션 변경</button><button onClick={closeShareWindow}>닫기</button>{!params.has('native') && <Link to="/saved">게시물 보기</Link>}</div>
        </> : <>
          <CollectionPicker compact category={detail.data?.extractedCategory} collectionId={detail.data?.collectionId ?? undefined}
            onConfirm={async (id) => {
              await selectShareCollection(shareId, id)
              void cache.invalidateQueries({ queryKey: ['share', shareId] })
              void cache.invalidateQueries({ queryKey: ['saved-shares'] })
              setDone(true)
              setChangingCollection(false)
              closeShareWindow()
            }} />
        </>}
    </section>
  </main>
}
