export type TransportType = 'WALKING' | 'PUBLIC_TRANSIT' | 'CAR'
export type ItineraryStatus = 'DRAFT' | 'GENERATED' | 'COMPLETED'

export type TransitStep = {
  type: string
  guidance: string
  minutes: number
  distanceMeters: number
  startStop: string | null
  endStop: string | null
  vehicles: string[]
}

export type TransitRoute = {
  type: string
  totalMinutes: number
  totalDistanceMeters: number
  transfers: number
  fare: number
  landingUrl: string | null
  steps: TransitStep[]
}

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
  preferredVisitDate: string | null
  preferredStartTime: string | null
  name: string
  category: string | null
  address: string | null
  latitude: number | null
  longitude: number | null
  imageUrl: string | null
  stayMinutes: number
  transit: TransitRoute | null
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

export type UpdateItinerary = Omit<CreateItinerary, 'savedPlaceIds'>

export type UpdateItineraryItemSchedule = {
  visitDate: string | null
  startTime: string | null
  stayMinutes: number
}
