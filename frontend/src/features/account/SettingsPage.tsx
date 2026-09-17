import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { logout } from '../auth/authApi'
import { useAuthStore } from '../../stores/authStore'
import { deleteAccount, updatePassword } from './accountApi'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { AccountIcon, AccountPageHeader } from './AccountPageHeader'

export function SettingsPage() {
  const navigate = useNavigate()
  const { refreshToken, clearSession } = useAuthStore()
  const [loggingOut, setLoggingOut] = useState(false)
  const [showDelete, setShowDelete] = useState(false)
  const [deletePassword, setDeletePassword] = useState('')
  const [deleting, setDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState('')
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
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

  const handleDeleteAccount = (event: FormEvent) => {
    event.preventDefault()
    if (deletePassword) setShowDeleteConfirm(true)
  }

  const confirmDeleteAccount = async () => {
    setDeleting(true)
    setDeleteError('')
    try {
      await deleteAccount(deletePassword)
      setShowDeleteConfirm(false)
      clearSession()
      navigate('/', { replace: true })
    } catch {
      setShowDeleteConfirm(false)
      setDeleteError('계정을 삭제하지 못했습니다. 현재 비밀번호를 확인해 주세요.')
    } finally {
      setDeleting(false)
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
          <Link to="/notifications">알림</Link>
        </div>
      </nav>
      <AccountPageHeader eyebrow="SETTINGS" title="설정" description="앱과 계정 사용 설정을 관리합니다." />
      <h2 className="account-section-title">앱 정보</h2>
      <section className="settings-list" aria-label="앱 정보">
        <div>
          <span>앱 버전</span>
          <strong>0.1.0</strong>
        </div>
        <div>
          <span>데이터 저장</span>
          <strong>내 계정에 저장</strong>
        </div>
      </section>
      <h2 className="account-section-title">계정 및 보안</h2>
      <section className="settings-list" aria-label="계정 및 보안">
        <button className="settings-action account-menu-row" type="button" aria-expanded={showPassword} aria-controls="account-password-form" onClick={() => setShowPassword((value) => !value)}>
          <AccountIcon name="lock" />
          <span>비밀번호 변경<small>변경 후 모든 기기에서 다시 로그인합니다.</small></span>
          <span className="account-chevron" aria-hidden="true">{showPassword ? '⌄' : '›'}</span>
        </button>
        {showPassword && (
          <form id="account-password-form" className="account-security-form" onSubmit={handlePasswordChange}>
            <label><span>현재 비밀번호</span><input type="password" autoComplete="current-password" value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} required /></label>
            <label><span>새 비밀번호</span><input type="password" autoComplete="new-password" minLength={8} maxLength={15} pattern="(?=.*[A-Za-z])(?=.*\d)[\x21-\x7E]{8,15}" placeholder="영문과 숫자 포함 8~15자" title="영문과 숫자를 포함한 8~15자로 입력해 주세요." value={newPassword} onChange={(event) => setNewPassword(event.target.value)} required /></label>
            <label><span>새 비밀번호 확인</span><input type="password" autoComplete="new-password" minLength={8} maxLength={15} placeholder="새 비밀번호를 다시 입력해 주세요" value={passwordConfirmation} onChange={(event) => setPasswordConfirmation(event.target.value)} required /></label>
            {passwordError && <p className="settings-error" role="alert">{passwordError}</p>}
            <div>
              <button type="button" disabled={changingPassword} onClick={() => setShowPassword(false)}>취소</button>
              <button type="submit" disabled={changingPassword}>{changingPassword ? '변경 중...' : '비밀번호 변경'}</button>
            </div>
          </form>
        )}
        <button className="account-menu-row account-logout" type="button" onClick={handleLogout} disabled={loggingOut}>
          <AccountIcon name="logout" /><span>{loggingOut ? '로그아웃 중...' : '로그아웃'}</span><span className="account-chevron" aria-hidden="true">›</span>
        </button>
        <button className="account-delete-toggle account-menu-row" type="button" aria-expanded={showDelete} aria-controls="account-delete-form" onClick={() => setShowDelete((value) => !value)}>
          <AccountIcon name="delete" /><span>계정 삭제</span><span className="account-chevron" aria-hidden="true">{showDelete ? '⌄' : '›'}</span>
        </button>
        {showDelete && (
          <form id="account-delete-form" className="account-delete-form" onSubmit={handleDeleteAccount}>
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
            {deleteError && <p className="settings-error" role="alert">{deleteError}</p>}
            <div>
              <button type="button" onClick={() => setShowDelete(false)}>취소</button>
              <button type="submit" disabled={deleting || !deletePassword}>
                {deleting ? '삭제 중...' : '계정 영구 삭제'}
              </button>
            </div>
          </form>
        )}
      </section>
      <ConfirmDialog
        open={showDeleteConfirm}
        title="계정을 영구 삭제할까요?"
        description="저장한 장소, 여행 계획, 받은 콘텐츠와 계정 데이터가 모두 삭제되며 복구할 수 없습니다."
        confirmLabel="계정 영구 삭제"
        pending={deleting}
        onCancel={() => setShowDeleteConfirm(false)}
        onConfirm={confirmDeleteAccount}
      />
    </main>
  )
}
