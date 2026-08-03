import { useEffect, useRef, useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { AxiosError } from 'axios'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuthStore } from '../../stores/authStore'
import type { ApiError } from '../auth/types'
import { createShare, type CreateShareInput } from './shareApi'

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
  const [searchParams] = useSearchParams()
  const accessToken = useAuthStore((state) => state.accessToken)
  const started = useRef(false)
  const [request] = useState(() => readPendingShare(searchParams))
  const mutation = useMutation({ mutationFn: createShare })

  useEffect(() => {
    if (started.current || !request) return
    if (!accessToken) {
      sessionStorage.setItem(PENDING_SHARE_KEY, JSON.stringify(request))
      navigate('/login', {
        replace: true,
        state: {
          returnTo: '/share-target?pending=1',
          message: '공유한 게시물을 저장하려면 로그인해 주세요. URL은 안전하게 보관했습니다.',
        },
      })
      return
    }

    started.current = true
    mutation.mutate(request, {
      onSuccess: () => sessionStorage.removeItem(PENDING_SHARE_KEY),
    })
  }, [accessToken, mutation, navigate, request])

  const apiError = (mutation.error as AxiosError<ApiError> | null)?.response?.data

  return (
    <main className="share-target-shell">
      <Link className="brand-link" to="/">SEND IT</Link>
      <section className="share-target-card">
        {!request ? (
          <>
            <span className="share-target-mark error" aria-hidden="true">!</span>
            <h1>공유할 URL을 찾지 못했습니다.</h1>
            <p>SNS 게시물에서 링크 공유를 선택한 뒤 다시 SEND IT으로 보내 주세요.</p>
            <Link className="share-target-primary" to="/">홈으로 이동</Link>
          </>
        ) : mutation.isPending || mutation.isIdle ? (
          <>
            <span className="share-target-loader" aria-hidden="true" />
            <h1>게시물을 받고 있습니다.</h1>
            <p>URL을 저장하고 장소 분석을 요청하는 중입니다.</p>
          </>
        ) : mutation.isSuccess ? (
          <>
            <span className="share-target-mark" aria-hidden="true">✓</span>
            <h1>{mutation.data.duplicate ? '이미 보관한 게시물입니다.' : '게시물을 저장했습니다.'}</h1>
            <p>장소 분석은 백그라운드에서 계속됩니다. 이제 SNS로 돌아가도 괜찮습니다.</p>
            <div className="share-target-actions">
              <Link className="share-target-primary" to={`/shares/${mutation.data.shareId}`}>분석 내용 보기</Link>
              <button type="button" onClick={() => navigate('/', { replace: true })}>나중에 보기</button>
            </div>
          </>
        ) : (
          <>
            <span className="share-target-mark error" aria-hidden="true">!</span>
            <h1>게시물을 저장하지 못했습니다.</h1>
            <p>{apiError?.message ?? '잠시 후 다시 시도해 주세요.'}</p>
            <button className="share-target-primary" type="button" onClick={() => mutation.mutate(request)}>
              다시 시도
            </button>
          </>
        )}
      </section>
    </main>
  )
}
