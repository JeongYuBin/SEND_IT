import { http } from '../../api/http'
import type {
  CreateItinerary,
  Itinerary,
  UpdateItinerary,
  UpdateItineraryItemSchedule,
  ReorderItineraryItem,
  TransportType,
} from './types'

export async function getItineraries() {
  return (await http.get<Itinerary[]>('/itineraries')).data
}

export async function getItinerary(id: number) {
  return (await http.get<Itinerary>(`/itineraries/${id}`)).data
}

export async function createItinerary(request: CreateItinerary) {
  return (await http.post<Itinerary>('/itineraries', request)).data
}

export async function updateItinerary(id: number, request: UpdateItinerary) {
  return (await http.put<Itinerary>(`/itineraries/${id}`, request)).data
}

export async function updateItineraryItemSchedule(
  itineraryId: number,
  savedPlaceId: number,
  request: UpdateItineraryItemSchedule,
) {
  return (await http.put<Itinerary>(
    `/itineraries/${itineraryId}/items/${savedPlaceId}/schedule`,
    request,
  )).data
}

export async function deleteItinerary(id: number) {
  await http.delete(`/itineraries/${id}`)
}

export async function reorderItineraryItems(
  itineraryId: number,
  items: ReorderItineraryItem[],
) {
  return (await http.put<Itinerary>(
    `/itineraries/${itineraryId}/items/order`,
    { items },
  )).data
}

export async function updateItineraryItemTransport(
  itineraryId: number,
  savedPlaceId: number,
  transportType: TransportType,
) {
  return (await http.put<Itinerary>(
    `/itineraries/${itineraryId}/items/${savedPlaceId}/transport`,
    { transportType },
  )).data
}
