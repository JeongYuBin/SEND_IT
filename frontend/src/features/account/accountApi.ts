import { http } from '../../api/http'
import type { AuthUser } from '../auth/types'

export async function getProfile() {
  return (await http.get<AuthUser>('/users/me')).data
}

export async function updateProfile(nickname: string) {
  return (await http.patch<AuthUser>('/users/me', { nickname })).data
}

export async function exportAccountData() {
  return (await http.get<Blob>('/users/me/data/export', { responseType: 'blob' })).data
}

export async function deleteAccount(password: string) {
  await http.delete('/users/me', { data: { password } })
}
