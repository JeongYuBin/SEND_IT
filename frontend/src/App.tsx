import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthPage } from './features/auth/AuthPage'
import { HomePage } from './features/home/HomePage'
import { useAuthStore } from './stores/authStore'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useAuthStore((state) => Boolean(state.accessToken))
  return isAuthenticated ? children : <Navigate to="/login" replace />
}

export function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/login" element={<AuthPage mode="login" />} />
      <Route path="/signup" element={<AuthPage mode="signup" />} />
      <Route
        path="/saved"
        element={
          <ProtectedRoute>
            <div className="placeholder-page">저장한 장소 화면을 준비하고 있습니다.</div>
          </ProtectedRoute>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

