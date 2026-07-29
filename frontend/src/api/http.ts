import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '../stores/authStore'
import type { TokenResponse } from '../features/auth/types'

const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'

export const http = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
})

http.interceptors.request.use((config) => {
  const accessToken = useAuthStore.getState().accessToken
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

type RetryableRequest = InternalAxiosRequestConfig & { _retry?: boolean }

let refreshSessionPromise: Promise<TokenResponse> | null = null

function refreshSession(refreshToken: string) {
  if (!refreshSessionPromise) {
    refreshSessionPromise = axios
      .post<TokenResponse>(`${baseURL}/auth/refresh`, { refreshToken })
      .then((response) => {
        useAuthStore.getState().setSession(response.data)
        return response.data
      })
      .finally(() => {
        refreshSessionPromise = null
      })
  }
  return refreshSessionPromise
}

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const request = error.config as RetryableRequest | undefined
    const auth = useAuthStore.getState()
    const isAuthRequest = request?.url?.includes('/auth/')

    if (
      error.response?.status === 401 &&
      request &&
      !request._retry &&
      auth.refreshToken &&
      !isAuthRequest
    ) {
      request._retry = true
      try {
        const session = await refreshSession(auth.refreshToken)
        request.headers.Authorization = `Bearer ${session.accessToken}`
        return http(request)
      } catch {
        useAuthStore.getState().clearSession()
        return Promise.reject(error)
      }
    }

    if (
      !isAuthRequest &&
      error.response?.status === 401
    ) {
      useAuthStore.getState().clearSession()
    }

    return Promise.reject(error)
  },
)
