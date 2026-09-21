import { useEffect, useState, type FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { AxiosError } from 'axios'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { checkUsername, login, sendEmailOtp, signUp, verifyEmailOtp } from './authApi'
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
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [emailOtp, setEmailOtp] = useState('')
  const [otpExpiresIn, setOtpExpiresIn] = useState(0)
  const [usernameAvailable, setUsernameAvailable] = useState(false)
  const [emailVerified, setEmailVerified] = useState(false)
  const [password, setPassword] = useState('')
  const [nickname, setNickname] = useState('')
  const isSignUp = mode === 'signup'
  const usernameValid = /^(?=.*[a-z])(?=.*\d)[a-z\d]{8,15}$/.test(username)
  const passwordValid = /^(?=.*[A-Za-z])(?=.*\d)[\x21-\x7E]{8,15}$/.test(password)

  useEffect(() => {
    if (otpExpiresIn <= 0) return
    const timer = window.setInterval(() => {
      setOtpExpiresIn((seconds) => {
        if (seconds <= 1) {
          setEmailVerified(false)
          return 0
        }
        return seconds - 1
      })
    }, 1000)
    return () => window.clearInterval(timer)
  }, [otpExpiresIn])

  const mutation = useMutation({
    mutationFn: (request: LoginRequest | SignUpRequest) =>
      isSignUp ? signUp(request as SignUpRequest) : login(request),
    onSuccess: (session) => {
      setSession(session)
      navigate(location.state?.returnTo ?? '/', { replace: true })
    },
  })
  const usernameMutation = useMutation({
    mutationFn: checkUsername,
    onSuccess: setUsernameAvailable,
  })
  const otpSendMutation = useMutation({
    mutationFn: sendEmailOtp,
    onSuccess: () => {
      setEmailOtp('')
      setEmailVerified(false)
      setOtpExpiresIn(180)
    },
  })
  const otpVerifyMutation = useMutation({
    mutationFn: () => verifyEmailOtp(email, emailOtp),
    onSuccess: () => setEmailVerified(true),
  })

  if (isAuthenticated) {
    return <Navigate to={location.state?.returnTo ?? '/'} replace />
  }

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (isSignUp) {
      if (!usernameAvailable || !emailVerified || !passwordValid) return
      mutation.mutate({ username, email, emailOtp, password, nickname })
    } else {
      mutation.mutate({ username, password })
    }
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
          <label>
            아이디
            <span className="auth-inline-field">
              <input
                required
                minLength={isSignUp ? 8 : undefined}
                maxLength={isSignUp ? 15 : undefined}
                pattern={isSignUp ? '(?=.*[a-z])(?=.*\\d)[a-z\\d]{8,15}' : undefined}
                autoComplete="username"
                value={username}
                onChange={(event) => {
                  setUsername(isSignUp
                    ? event.target.value.toLowerCase().replace(/[^a-z0-9]/g, '')
                    : event.target.value)
                  setUsernameAvailable(false)
                  usernameMutation.reset()
                }}
                placeholder={isSignUp ? '영문 소문자와 숫자 8~15자' : '아이디'}
              />
              {isSignUp && (
                <button
                  type="button"
                  disabled={!usernameValid || usernameMutation.isPending}
                  onClick={() => usernameMutation.mutate(username)}
                >중복 확인</button>
              )}
            </span>
            {isSignUp && usernameMutation.isSuccess && (
              <small className={usernameAvailable ? 'field-success' : ''}>
                {usernameAvailable ? '사용할 수 있는 아이디입니다.' : '이미 사용 중인 아이디입니다.'}
              </small>
            )}
            {apiError?.fieldErrors?.username && <small>{apiError.fieldErrors.username}</small>}
          </label>
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
          {isSignUp && <label>
            이메일 인증
            <span className="auth-inline-field">
            <input
              required
              type="email"
              autoComplete="email"
              value={email}
              onChange={(event) => {
                setEmail(event.target.value)
                setEmailVerified(false)
                setOtpExpiresIn(0)
                otpSendMutation.reset()
                otpVerifyMutation.reset()
              }}
              placeholder="travel@example.com"
            />
              <button
                type="button"
                disabled={!email || otpSendMutation.isPending}
                onClick={() => otpSendMutation.mutate(email)}
              >{otpExpiresIn > 120 ? '전송 완료' : '인증번호 전송'}</button>
            </span>
            {apiError?.fieldErrors?.email && <small>{apiError.fieldErrors.email}</small>}
            {otpSendMutation.isError && <small>인증번호를 보내지 못했습니다. 이메일과 메일 설정을 확인해 주세요.</small>}
          </label>}
          {isSignUp && otpExpiresIn > 0 && (
            <label>
              인증번호
              <span className="auth-inline-field">
                <input
                  required
                  inputMode="numeric"
                  maxLength={6}
                  pattern="\d{6}"
                  value={emailOtp}
                  onChange={(event) => {
                    setEmailOtp(event.target.value.replace(/\D/g, '').slice(0, 6))
                    setEmailVerified(false)
                    otpVerifyMutation.reset()
                  }}
                  placeholder="6자리 숫자"
                />
                <button
                  type="button"
                  disabled={emailOtp.length !== 6 || otpVerifyMutation.isPending || emailVerified}
                  onClick={() => otpVerifyMutation.mutate()}
                >{emailVerified ? '인증 완료' : '인증 확인'}</button>
              </span>
              <small className={emailVerified ? 'field-success' : 'otp-timer'}>
                {emailVerified
                  ? '이메일 인증이 완료되었습니다.'
                  : `남은 시간 ${Math.floor(otpExpiresIn / 60)}:${String(otpExpiresIn % 60).padStart(2, '0')}`}
              </small>
              {otpVerifyMutation.isError && <small>인증번호가 올바르지 않거나 만료되었습니다.</small>}
            </label>
          )}
          <label>
            비밀번호
            <input
              required
              minLength={isSignUp ? 8 : undefined}
              maxLength={isSignUp ? 15 : undefined}
              pattern={isSignUp ? '(?=.*[A-Za-z])(?=.*\\d)[\\x21-\\x7E]{8,15}' : undefined}
              type="password"
              autoComplete={isSignUp ? 'new-password' : 'current-password'}
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder={isSignUp ? '영문과 숫자 포함 8~15자' : '비밀번호'}
            />
            {apiError?.fieldErrors?.password && <small>{apiError.fieldErrors.password}</small>}
          </label>

          {!isSignUp && (
            <div className="auth-recovery-links">
              <Link to="/find-id">아이디 찾기</Link>
              <span aria-hidden="true">·</span>
              <Link to="/reset-password">비밀번호 찾기</Link>
            </div>
          )}

          {mutation.isError && (
            <div className="form-error" role="alert">
              {apiError?.message ?? '요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.'}
            </div>
          )}

          <button type="submit" disabled={mutation.isPending || (isSignUp && (!usernameAvailable || !emailVerified || !passwordValid))}>
            {mutation.isPending ? '처리 중...' : isSignUp ? '시작하기' : '로그인'}
          </button>
        </form>

        <div className="auth-switch">
          {isSignUp ? '이미 계정이 있나요?' : '아직 계정이 없나요?'}
          <Link
            to={isSignUp ? '/login' : '/signup'}
            state={location.state}
          >
            {isSignUp ? '로그인' : '회원가입'}
          </Link>
        </div>
        <div className="auth-switch"><Link to="/privacy">개인정보처리방침</Link></div>
      </section>
    </main>
  )
}
