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

export async function checkUsername(username: string) {
  return (await http.get<{ available: boolean }>(
    `/auth/usernames/${encodeURIComponent(username)}/availability`,
  )).data.available
}

export async function sendEmailOtp(email: string) {
  await http.post('/auth/email-otp', { email })
}

export async function verifyEmailOtp(email: string, code: string) {
  await http.post('/auth/email-otp/verify', { email, code })
}

export async function requestUsername(email: string) {
  await http.post('/auth/recovery/username', { email })
}

export async function requestPasswordResetCode(username: string, email: string) {
  await http.post('/auth/recovery/password/code', { username, email })
}

export async function resetPassword(
  username: string,
  email: string,
  code: string,
  newPassword: string,
) {
  await http.post('/auth/recovery/password/reset', { username, email, code, newPassword })
}

export async function logout(refreshToken: string) {
  await http.post('/auth/logout', { refreshToken })
}
