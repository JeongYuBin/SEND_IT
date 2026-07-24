import { http } from '../../api/http'
import type { LoginRequest, SignUpRequest, TokenResponse } from './types'

export async function login(request: LoginRequest) {
  const response = await http.post<TokenResponse>('/auth/login', request)
  return response.data
}

export async function signUp(request: SignUpRequest) {
  const response = await http.post<TokenResponse>('/auth/signup', request)
  return response.data
}

export async function logout(refreshToken: string) {
  await http.post('/auth/logout', { refreshToken })
}

