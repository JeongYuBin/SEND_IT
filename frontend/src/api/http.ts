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
        const response = await axios.post<TokenResponse>(`${baseURL}/auth/refresh`, {
          refreshToken: auth.refreshToken,
        })
        auth.setSession(response.data)
        request.headers.Authorization = `Bearer ${response.data.accessToken}`
        return http(request)
      } catch {
        auth.clearSession()
      }
    }

    if (
      !isAuthRequest &&
      (error.response?.status === 401 || error.response?.status === 403)
    ) {
      auth.clearSession()
    }

    return Promise.reject(error)
  },
)
