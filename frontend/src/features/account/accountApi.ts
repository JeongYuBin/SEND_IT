import { http } from '../../api/http'
import type { AuthUser } from '../auth/types'

export async function getProfile() {
  return (await http.get<AuthUser>('/users/me')).data
}

export async function updateProfile(nickname: string) {
  return (await http.patch<AuthUser>('/users/me', { nickname })).data
}

export async function deleteAccount(password: string) {
  await http.delete('/users/me', { data: { password } })
}

export async function updatePassword(currentPassword: string, newPassword: string) {
  await http.patch('/users/me/password', { currentPassword, newPassword })
}
