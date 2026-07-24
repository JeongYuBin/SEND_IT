import { useState, type FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { AxiosError } from 'axios'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { login, signUp } from './authApi'
import type { ApiError, LoginRequest, SignUpRequest } from './types'
import { useAuthStore } from '../../stores/authStore'

type AuthPageProps = {
  mode: 'login' | 'signup'
}

export function AuthPage({ mode }: AuthPageProps) {
  const navigate = useNavigate()
  const location = useLocation()
  const setSession = useAuthStore((state) => state.setSession)
  const isAuthenticated = useAuthStore((state) => Boolean(state.accessToken))
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [nickname, setNickname] = useState('')
  const isSignUp = mode === 'signup'

  const mutation = useMutation({
    mutationFn: (request: LoginRequest | SignUpRequest) =>
      isSignUp ? signUp(request as SignUpRequest) : login(request),
    onSuccess: (session) => {
      setSession(session)
      navigate('/', { replace: true })
    },
  })

  if (isAuthenticated) {
    return <Navigate to="/" replace />
  }

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    mutation.mutate({
      email,
      password,
      ...(isSignUp ? { nickname } : {}),
    })
  }

  const apiError = (mutation.error as AxiosError<ApiError> | null)?.response?.data

  return (
    <main className="auth-shell">
      <Link className="brand-link" to="/">SEND IT</Link>
      <section className="auth-card">
        {location.state?.message && (
          <div className="form-error session-message" role="alert">
            {location.state.message}
          </div>
        )}
        <span className="eyebrow">{isSignUp ? 'JOIN US' : 'WELCOME BACK'}</span>
        <h1>{isSignUp ? '여행을 모으기 시작해요.' : '다시 여행을 이어가요.'}</h1>
        <p>
          {isSignUp
            ? '흩어진 여행 콘텐츠를 나만의 장소로 정리해 드릴게요.'
            : '저장한 장소와 여행 계획이 기다리고 있어요.'}
        </p>

        <form className="auth-form" onSubmit={handleSubmit}>
          {isSignUp && (
            <label>
              닉네임
              <input
                required
                maxLength={50}
                value={nickname}
                onChange={(event) => setNickname(event.target.value)}
                placeholder="어떻게 불러드릴까요?"
              />
              {apiError?.fieldErrors?.nickname && <small>{apiError.fieldErrors.nickname}</small>}
            </label>
          )}
          <label>
            이메일
            <input
              required
              type="email"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="travel@example.com"
            />
            {apiError?.fieldErrors?.email && <small>{apiError.fieldErrors.email}</small>}
          </label>
          <label>
            비밀번호
            <input
              required
              minLength={8}
              maxLength={72}
              type="password"
              autoComplete={isSignUp ? 'new-password' : 'current-password'}
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="8자 이상 입력"
            />
            {apiError?.fieldErrors?.password && <small>{apiError.fieldErrors.password}</small>}
          </label>

          {mutation.isError && (
            <div className="form-error" role="alert">
              {apiError?.message ?? '요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.'}
            </div>
          )}

          <button type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? '처리 중...' : isSignUp ? '시작하기' : '로그인'}
          </button>
        </form>

        <div className="auth-switch">
          {isSignUp ? '이미 계정이 있나요?' : '아직 계정이 없나요?'}
          <Link to={isSignUp ? '/login' : '/signup'}>
            {isSignUp ? '로그인' : '회원가입'}
          </Link>
        </div>
      </section>
    </main>
  )
}
