import { http } from '../../api/http'
import type { Collection, CreateSavedPlace, SavedPlace, VisitStatus } from './types'

export async function getSavedPlaces() {
  return (await http.get<SavedPlace[]>('/saved-places')).data
}

export async function createSavedPlace(request: CreateSavedPlace) {
  return (await http.post<SavedPlace>('/saved-places', request)).data
}

export async function updateSavedPlace(
  id: number,
  request: { memo?: string; visitStatus?: VisitStatus; priority?: number; collectionId?: number | null },
) {
  return (await http.patch<SavedPlace>(`/saved-places/${id}`, request)).data
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

