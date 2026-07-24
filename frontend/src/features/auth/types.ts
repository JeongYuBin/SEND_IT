export type AuthUser = {
  id: number
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
  email: string
  password: string
}

export type SignUpRequest = LoginRequest & {
  nickname: string
}

export type ApiError = {
  code?: string
  message?: string
  fieldErrors?: Record<string, string>
}

