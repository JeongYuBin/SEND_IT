export type TransportType = 'WALKING' | 'PUBLIC_TRANSIT' | 'CAR'
export type ItineraryStatus = 'DRAFT' | 'GENERATED' | 'COMPLETED'

export type ItineraryItem = {
  savedPlaceId: number
  sequence: number
  name: string
  category: string | null
  address: string | null
  latitude: number | null
  longitude: number | null
  imageUrl: string | null
  stayMinutes: number
}

export type Itinerary = {
  id: number
  title: string
  startDate: string
  endDate: string
  dailyStartTime: string
  dailyEndTime: string
  transportType: TransportType
  status: ItineraryStatus
  items: ItineraryItem[]
  createdAt: string
}

export type CreateItinerary = {
  title: string
  startDate: string
  endDate: string
  dailyStartTime: string
  dailyEndTime: string
  transportType: TransportType
  savedPlaceIds: number[]
}
