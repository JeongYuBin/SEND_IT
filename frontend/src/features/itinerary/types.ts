export type TransportType = 'WALKING' | 'PUBLIC_TRANSIT' | 'CAR'
export type ItineraryStatus = 'DRAFT' | 'GENERATED' | 'COMPLETED'

export type ItineraryItem = {
  savedPlaceId: number
  sequence: number
  daySequence: number
  visitDate: string
  arrivalTime: string
  departureTime: string
  travelMinutesFromPrevious: number
  distanceKmFromPrevious: number | null
  coordinateAvailable: boolean
  name: string
  category: string | null
  address: string | null
  latitude: number | null
  longitude: number | null
  imageUrl: string | null
  stayMinutes: number
}

export type ItineraryDay = {
  date: string
  dayNumber: number
  exceedsDailyWindow: boolean
  items: ItineraryItem[]
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
  days: ItineraryDay[]
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
