import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthPage } from './features/auth/AuthPage'
import { HomePage } from './features/home/HomePage'
import { ItinerariesPage } from './features/itinerary/ItinerariesPage'
import { ItineraryDetailPage } from './features/itinerary/ItineraryDetailPage'
import { ItineraryListPage } from './features/itinerary/ItineraryListPage'
import { SavedPlacesPage } from './features/saved/SavedPlacesPage'
import { SavedPlaceDetailPage } from './features/saved/SavedPlaceDetailPage'
import { ShareResultPage } from './features/share/ShareResultPage'
import { useAuthStore } from './stores/authStore'
import { MobileBottomNavigation } from './components/MobileBottomNavigation'
import { ProfilePage } from './features/account/ProfilePage'
import { SettingsPage } from './features/account/SettingsPage'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useAuthStore((state) => Boolean(state.accessToken))
  return isAuthenticated ? children : <Navigate to="/login" replace />
}

export function App() {
  return (
    <>
      <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/login" element={<AuthPage mode="login" />} />
      <Route path="/signup" element={<AuthPage mode="signup" />} />
      <Route path="/shares/:shareId" element={<ProtectedRoute><ShareResultPage /></ProtectedRoute>} />
      <Route
        path="/itineraries/:itineraryId"
        element={<ProtectedRoute><ItineraryDetailPage /></ProtectedRoute>}
      />
      <Route
        path="/itineraries"
        element={<ProtectedRoute><ItineraryListPage /></ProtectedRoute>}
      />
      <Route
        path="/itineraries/new"
        element={<ProtectedRoute><ItinerariesPage /></ProtectedRoute>}
      />
      <Route
        path="/saved/places/:savedPlaceId"
        element={
          <ProtectedRoute>
            <SavedPlaceDetailPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/saved/collections/:collectionId"
        element={
          <ProtectedRoute>
            <SavedPlacesPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/saved"
        element={
          <ProtectedRoute>
            <SavedPlacesPage />
          </ProtectedRoute>
        }
      />
      <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
      <Route path="/settings" element={<ProtectedRoute><SettingsPage /></ProtectedRoute>} />
      <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
      <MobileBottomNavigation />
    </>
  )
}
