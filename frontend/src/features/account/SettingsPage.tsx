import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { logout } from '../auth/authApi'
import { useAuthStore } from '../../stores/authStore'
import { deleteAccount, exportAccountData, updatePassword } from './accountApi'

export function SettingsPage() {
  const navigate = useNavigate()
  const { refreshToken, clearSession } = useAuthStore()
  const [loggingOut, setLoggingOut] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [exportError, setExportError] = useState('')
  const [showDelete, setShowDelete] = useState(false)
  const [deletePassword, setDeletePassword] = useState('')
  const [deleting, setDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [passwordConfirmation, setPasswordConfirmation] = useState('')
  const [passwordError, setPasswordError] = useState('')
  const [changingPassword, setChangingPassword] = useState(false)

  const handleLogout = async () => {
    setLoggingOut(true)
    try {
      if (refreshToken) await logout(refreshToken)
    } finally {
      clearSession()
      navigate('/', { replace: true })
    }
  }

  const handlePasswordChange = async (event: FormEvent) => {
    event.preventDefault()
    if (newPassword !== passwordConfirmation) {
      setPasswordError('새 비밀번호 확인이 일치하지 않습니다.')
      return
    }
    setChangingPassword(true)
    setPasswordError('')
    try {
      await updatePassword(currentPassword, newPassword)
      clearSession()
      navigate('/login', { replace: true })
    } catch {
      setPasswordError('비밀번호를 변경하지 못했습니다. 현재 비밀번호와 입력 조건을 확인해 주세요.')
    } finally {
      setChangingPassword(false)
    }
  }

  const handleDeleteAccount = async (event: FormEvent) => {
    event.preventDefault()
    setDeleting(true)
    setDeleteError('')
    try {
      await deleteAccount(deletePassword)
      clearSession()
      navigate('/', { replace: true })
    } catch {
      setDeleteError('계정을 삭제하지 못했습니다. 현재 비밀번호를 확인해 주세요.')
    } finally {
      setDeleting(false)
    }
  }

  const handleExport = async () => {
    setExporting(true)
    setExportError('')
    try {
      const blob = await exportAccountData()
      const url = URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = `send-it-backup-${new Date().toISOString().slice(0, 10)}.json`
      anchor.click()
      URL.revokeObjectURL(url)
    } catch {
      setExportError('데이터 백업 파일을 만들지 못했습니다. 다시 시도해 주세요.')
    } finally {
      setExporting(false)
    }
  }

  return (
    <main className="account-shell">
      <nav className="top-nav">
        <Link className="brand-link" to="/">SEND IT</Link>
        <div>
          <Link to="/itineraries">여행 계획</Link>
          <Link to="/saved">저장한 장소</Link>
          <Link to="/profile">내 정보</Link>
        </div>
      </nav>
      <header className="account-header">
        <span className="eyebrow">SETTINGS</span>
        <h1>설정</h1>
        <p>앱과 계정 사용 설정을 관리합니다.</p>
      </header>
      <section className="settings-list">
        <div>
          <span>앱 버전</span>
          <strong>0.1.0</strong>
        </div>
        <div>
          <span>데이터 저장</span>
          <strong>내 계정에 저장</strong>
        </div>
        <button className="settings-action" type="button" onClick={handleExport} disabled={exporting}>
          <span>내 데이터 내려받기</span>
          <small>{exporting ? '백업 파일 생성 중...' : '장소·계획·원본 콘텐츠를 JSON으로 백업'}</small>
        </button>
        {exportError && <p className="settings-error">{exportError}</p>}
        <button className="settings-action" type="button" onClick={() => setShowPassword((value) => !value)}>
          <span>비밀번호 변경</span>
          <small>변경 후 모든 기기에서 다시 로그인합니다.</small>
        </button>
        {showPassword && (
          <form className="account-security-form" onSubmit={handlePasswordChange}>
            <label><span>현재 비밀번호</span><input type="password" autoComplete="current-password" value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} required /></label>
            <label><span>새 비밀번호</span><input type="password" autoComplete="new-password" minLength={8} maxLength={72} value={newPassword} onChange={(event) => setNewPassword(event.target.value)} required /></label>
            <label><span>새 비밀번호 확인</span><input type="password" autoComplete="new-password" minLength={8} maxLength={72} value={passwordConfirmation} onChange={(event) => setPasswordConfirmation(event.target.value)} required /></label>
            {passwordError && <p className="settings-error">{passwordError}</p>}
            <div>
              <button type="button" onClick={() => setShowPassword(false)}>취소</button>
              <button type="submit" disabled={changingPassword}>{changingPassword ? '변경 중...' : '비밀번호 변경'}</button>
            </div>
          </form>
        )}
        <button type="button" onClick={handleLogout} disabled={loggingOut}>
          {loggingOut ? '로그아웃 중...' : '로그아웃'}
        </button>
        <button className="account-delete-toggle" type="button" onClick={() => setShowDelete((value) => !value)}>
          계정 삭제
        </button>
        {showDelete && (
          <form className="account-delete-form" onSubmit={handleDeleteAccount}>
            <strong>계정과 모든 데이터를 영구 삭제합니다.</strong>
            <p>저장 장소, 여행 계획, 원본 콘텐츠와 분석 파일은 복구할 수 없습니다.</p>
            <label>
              <span>현재 비밀번호</span>
              <input
                type="password"
                value={deletePassword}
                onChange={(event) => setDeletePassword(event.target.value)}
                autoComplete="current-password"
                required
              />
            </label>
            {deleteError && <p className="settings-error">{deleteError}</p>}
            <div>
              <button type="button" onClick={() => setShowDelete(false)}>취소</button>
              <button type="submit" disabled={deleting || !deletePassword}>
                {deleting ? '삭제 중...' : '계정 영구 삭제'}
              </button>
            </div>
          </form>
        )}
      </section>
    </main>
  )
}
