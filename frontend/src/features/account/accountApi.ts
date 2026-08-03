import { http } from '../../api/http'
import type { AuthUser } from '../auth/types'
import type { TransportType } from '../itinerary/types'

export type UserProfile = AuthUser & {
  preferredTransport: TransportType
  travelWithPet: boolean
}

export async function getProfile() {
  return (await http.get<UserProfile>('/users/me')).data
}

export async function updateProfile(nickname: string) {
  return (await http.patch<UserProfile>('/users/me', { nickname })).data
}

export async function updatePreferences(preferences: {
  preferredTransport: TransportType
  travelWithPet: boolean
}) {
  return (await http.patch<UserProfile>('/users/me/preferences', preferences)).data
}
