import { http } from '../../api/http'
import type { CreateItinerary, Itinerary } from './types'

export async function getItineraries() {
  return (await http.get<Itinerary[]>('/itineraries')).data
}

export async function getItinerary(id: number) {
  return (await http.get<Itinerary>(`/itineraries/${id}`)).data
}

export async function createItinerary(request: CreateItinerary) {
  return (await http.post<Itinerary>('/itineraries', request)).data
}
