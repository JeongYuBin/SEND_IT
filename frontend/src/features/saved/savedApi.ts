import { http } from '../../api/http'
import type {
  Collection,
  CreateSavedPlace,
  NearbyTourismPlace,
  SavedPlace,
  TourismOperatingInfo,
  VisitStatus,
} from './types'

export async function getSavedPlaces() {
  return (await http.get<SavedPlace[]>('/saved-places')).data
}

export async function getSavedPlace(id: number) {
  return (await http.get<SavedPlace>(`/saved-places/${id}`)).data
}

export async function createSavedPlace(request: CreateSavedPlace) {
  return (await http.post<SavedPlace>('/saved-places', request)).data
}

export async function updateSavedPlace(
  id: number,
  request: {
    memo?: string
    visitStatus?: VisitStatus
    priority?: number
    collectionId?: number
    clearCollection?: boolean
  },
) {
  return (await http.patch<SavedPlace>(`/saved-places/${id}`, request)).data
}

export async function syncSavedPlaceTourism(id: number) {
  return (await http.post<SavedPlace>(`/saved-places/${id}/sync-tourism`)).data
}

export async function deleteSavedPlace(id: number) {
  await http.delete(`/saved-places/${id}`)
}

export async function getCollections() {
  return (await http.get<Collection[]>('/collections')).data
}

export async function createCollection(name: string) {
  return (await http.post<Collection>('/collections', { name })).data
}

export async function getNearbyTourismPlaces(
  latitude: number,
  longitude: number,
  radius = 10000,
) {
  return (await http.get<NearbyTourismPlace[]>('/tourism/nearby', {
    params: { latitude, longitude, radius },
  })).data
}

export async function getTourismOperatingInfo(name: string, address?: string | null) {
  return (await http.get<TourismOperatingInfo>('/tourism/operating-info', {
    params: { name, address: address || undefined },
  })).data
}
