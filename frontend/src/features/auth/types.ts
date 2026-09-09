export type AuthUser = {
  id: number
  username: string
  email: string
  nickname: string
}

export type TokenResponse = {
  tokenType: 'Bearer'
  accessToken: string
  refreshToken: string
  expiresInSeconds: number
  user: AuthUser
}

export type LoginRequest = {
  username: string
  password: string
}

export type SignUpRequest = {
  username: string
  email: string
  emailOtp: string
  password: string
  nickname: string
}

export type ApiError = {
  code?: string
  message?: string
  fieldErrors?: Record<string, string>
}
