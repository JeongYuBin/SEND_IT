import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { AuthUser, TokenResponse } from '../features/auth/types'

type AuthState = {
  accessToken: string | null
  refreshToken: string | null
  user: AuthUser | null
  setSession: (session: TokenResponse) => void
  updateUser: (user: AuthUser) => void
  clearSession: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      setSession: (session) =>
        set({
          accessToken: session.accessToken,
          refreshToken: session.refreshToken,
          user: session.user,
        }),
      updateUser: (user) => set({ user }),
      clearSession: () =>
        set({
          accessToken: null,
          refreshToken: null,
          user: null,
        }),
    }),
    {
      name: 'sendit-auth',
    },
  ),
)
