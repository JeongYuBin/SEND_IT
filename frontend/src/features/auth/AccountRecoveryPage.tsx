import { useEffect, useState, type FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { requestPasswordResetCode, requestUsername, resetPassword } from './authApi'

type Props = { mode: 'username' | 'password' }

export function AccountRecoveryPage({ mode }: Props) {
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [code, setCode] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [expiresIn, setExpiresIn] = useState(0)
  const isPassword = mode === 'password'
  const passwordValid = /^(?=.*[A-Za-z])(?=.*\d)[\x21-\x7E]{8,15}$/.test(newPassword)

  useEffect(() => {
    if (expiresIn <= 0) return
    const timer = window.setInterval(() => setExpiresIn((value) => Math.max(0, value - 1)), 1000)
    return () => window.clearInterval(timer)
  }, [expiresIn])

  const usernameMutation = useMutation({ mutationFn: () => requestUsername(email) })
  const codeMutation = useMutation({
    mutationFn: () => requestPasswordResetCode(username, email),
    onSuccess: () => {
      setCode('')
      setExpiresIn(180)
    },
  })
  const resetMutation = useMutation({
    mutationFn: () => resetPassword(username, email, code, newPassword),
  })

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!isPassword) usernameMutation.mutate()
    else if (expiresIn > 0 && code.length === 6 && passwordValid) resetMutation.mutate()
    else codeMutation.mutate()
  }

  const sent = usernameMutation.isSuccess || codeMutation.isSuccess
  const pending = usernameMutation.isPending || codeMutation.isPending || resetMutation.isPending
  const failed = usernameMutation.isError || codeMutation.isError || resetMutation.isError

  return (
    <main className="auth-shell">
      <Link className="brand-link" to="/">SEND IT</Link>
      <section className="auth-card recovery-card">
        <span className="eyebrow">ACCOUNT RECOVERY</span>
        <h1>{isPassword ? '비밀번호를 다시 설정해요.' : '아이디를 찾아드릴게요.'}</h1>
        <p>{isPassword
          ? '아이디와 가입 이메일을 확인한 뒤 인증번호를 보내드려요.'
          : '가입할 때 인증한 이메일로 아이디를 보내드려요.'}</p>

        <form className="auth-form" onSubmit={submit}>
          {isPassword && <label>
            아이디
            <input required value={username} onChange={(event) => setUsername(event.target.value)} placeholder="아이디" />
          </label>}
          <label>
            가입 이메일
            <input required type="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="travel@example.com" />
          </label>

          {isPassword && expiresIn > 0 && <>
            <label>
              인증번호
              <input required inputMode="numeric" maxLength={6} pattern="\d{6}" value={code}
                onChange={(event) => setCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
                placeholder="6자리 숫자" />
              <small className="otp-timer">남은 시간 {Math.floor(expiresIn / 60)}:{String(expiresIn % 60).padStart(2, '0')}</small>
            </label>
            <label>
              새 비밀번호
              <input required type="password" minLength={8} maxLength={15}
                pattern="(?=.*[A-Za-z])(?=.*\d)[\x21-\x7E]{8,15}"
                value={newPassword} onChange={(event) => setNewPassword(event.target.value)}
                placeholder="영문과 숫자 포함 8~15자" />
            </label>
          </>}

          {sent && !resetMutation.isSuccess && <div className="form-success" role="status">
            입력한 정보와 일치하는 계정이 있다면 이메일을 전송했습니다.
          </div>}
          {resetMutation.isSuccess && <div className="form-success" role="status">
            비밀번호를 변경했습니다. 새 비밀번호로 로그인해 주세요.
          </div>}
          {failed && <div className="form-error" role="alert">요청을 처리하지 못했습니다. 입력 정보와 인증번호를 확인해 주세요.</div>}

          {!resetMutation.isSuccess && <button type="submit" disabled={pending || (isPassword && expiresIn > 0 && (!passwordValid || code.length !== 6))}>
            {pending ? '처리 중...' : isPassword && expiresIn > 0 ? '비밀번호 변경' : isPassword ? '인증번호 받기' : '아이디 이메일로 받기'}
          </button>}
        </form>

        <div className="auth-switch"><Link to="/login">로그인으로 돌아가기</Link></div>
      </section>
    </main>
  )
}
