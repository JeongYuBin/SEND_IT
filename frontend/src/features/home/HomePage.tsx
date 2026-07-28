import { useState, type FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { AxiosError } from 'axios'
import { Link, useNavigate } from 'react-router-dom'
import { logout } from '../auth/authApi'
import type { ApiError } from '../auth/types'
import { createShare } from '../share/shareApi'
import { useAuthStore } from '../../stores/authStore'

export function HomePage() {
  const { accessToken, refreshToken, user, clearSession } = useAuthStore()
  const navigate = useNavigate()
  const [url, setUrl] = useState('')
  const shareMutation = useMutation({
    mutationFn: createShare,
    onError: (error) => {
      const status = (error as AxiosError).response?.status
      if (status === 401 || status === 403) {
        navigate('/login', {
          replace: true,
          state: { message: '로그인이 만료되었습니다. 다시 로그인해 주세요.' },
        })
      }
    },
  })

  const handleLogout = async () => {
    try {
      if (refreshToken) {
        await logout(refreshToken)
      }
    } finally {
      clearSession()
    }
  }

  const handleShare = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!accessToken) {
      navigate('/login', { state: { returnTo: '/' } })
      return
    }
    shareMutation.mutate(url, {
      onSuccess: (result) => {
        setUrl('')
        navigate(`/shares/${result.shareId}`)
      },
    })
  }

  const shareError = (shareMutation.error as AxiosError<ApiError> | null)?.response?.data

  return (
    <main className="shell">
      <nav className="top-nav">
        <Link className="brand-link" to="/">SEND IT</Link>
        <div>
          {accessToken ? (
            <>
              <span>{user?.nickname}님</span>
              <Link to="/saved">저장한 장소</Link>
              <button className="text-button" type="button" onClick={handleLogout}>로그아웃</button>
            </>
          ) : (
            <>
              <Link to="/login">로그인</Link>
              <Link className="nav-cta" to="/signup">시작하기</Link>
            </>
          )}
        </div>
      </nav>

      <section className="hero">
        <span className="eyebrow">SEND IT</span>
        <h1>발견한 여행지를<br />진짜 여행으로.</h1>
        <p>
          SNS와 블로그에서 찾은 장소를 저장하고,
          방문 가능한 여행 동선으로 만들어 보세요.
        </p>
        <form className="share-form" onSubmit={handleShare}>
          <label htmlFor="content-url">저장할 콘텐츠 URL</label>
          <div>
            <input
              id="content-url"
              required
              type="url"
              placeholder="https://..."
              aria-describedby="url-help"
              value={url}
              onChange={(event) => setUrl(event.target.value)}
            />
            <button type="submit" disabled={shareMutation.isPending}>
              {shareMutation.isPending ? '접수 중...' : '장소 찾기'}
            </button>
          </div>
          <small id="url-help">Instagram, YouTube, 네이버 블로그와 일반 웹페이지를 지원할 예정입니다.</small>
          {shareMutation.isSuccess && (
            <div className="share-feedback success" role="status">
              {shareMutation.data.duplicate ? '이미 저장한 콘텐츠예요.' : '저장했어요! 장소를 분석하고 있습니다.'}
              <span>접수 번호 #{shareMutation.data.shareId}</span>
            </div>
          )}
          {shareMutation.isError && (
            <div className="share-feedback error" role="alert">
              {shareError?.message ?? 'URL을 접수하지 못했습니다. 다시 시도해 주세요.'}
            </div>
          )}
        </form>
      </section>
    </main>
  )
}
