import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import { App } from './App'
import { AppErrorBoundary } from './components/AppErrorBoundary'
import './styles.css'
import './design-system.css'
import './soft-theme.css'
import './feed.css'
import '@fontsource-variable/noto-sans-kr'
import './trip-create.css'
import './trip-detail.css'
import './saved-detail.css'
import './place-image.css'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: (failureCount, error: unknown) => {
        const status = (error as { response?: { status?: number } })?.response?.status
        return status !== 401 && status !== 403 && status !== 404 && failureCount < 2
      },
      refetchOnWindowFocus: false,
    },
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AppErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </QueryClientProvider>
    </AppErrorBoundary>
  </StrictMode>,
)

if (import.meta.env.PROD && 'serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/service-worker.js').catch(() => {
      // 네트워크 이용이 가능한 웹 환경에서는 서비스 워커 없이도 앱을 계속 사용합니다.
    })
  })
}
