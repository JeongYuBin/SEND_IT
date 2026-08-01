export type Collection = {
  id: number
  name: string
  description: string | null
  coverImageUrl: string | null
  createdAt: string
}

export type SavedPlace = {
  savedPlaceId: number
  placeId: number
  name: string
  category: string | null
  address: string | null
  roadAddress: string | null
  latitude: number | null
  longitude: number | null
  description: string | null
  imageUrl: string | null
  phone: string | null
  homepageUrl: string | null
  tourismContentId: string | null
  tourismContentTypeId: string | null
  operatingHours: string | null
  restDays: string | null
  parkingInfo: string | null
  eventStartDate: string | null
  eventEndDate: string | null
  collectionId: number | null
  collectionName: string | null
  memo: string | null
  priority: number
  savedAt: string
  originalUrl: string | null
}

export type CreateSavedPlace = {
  name: string
  category?: string
  address?: string
  roadAddress?: string
  collectionId?: number
  memo?: string
  priority?: number
  sharedContentId?: number
  description?: string
  imageUrl?: string
  latitude?: number
  longitude?: number
  tourismContentId?: string
  tourismContentTypeId?: string
  eventStartDate?: string
  eventEndDate?: string
}

export type NearbyTourismPlace = {
  contentId: string
  contentTypeId: string
  name: string
  category: string | null
  address: string | null
  latitude: number
  longitude: number
  imageUrl: string | null
  distanceMeters: number
}

export type TourismOperatingInfo = {
  hours: string | null
  restDays: string | null
  available: boolean
}

export type TourismPlaceDetail = {
  contentId: string
  contentTypeId: string
  name: string
  category: string | null
  address: string | null
  latitude: number | null
  longitude: number | null
  imageUrl: string | null
  description: string | null
  phone: string | null
  homepageUrl: string | null
  operatingHours: string | null
  restDays: string | null
  parkingInfo: string | null
}

export type PetTravelInfo = {
  contentId: string
  companionType: string | null
  allowedPets: string | null
  requiredItems: string | null
  additionalRules: string | null
  facilities: string | null
  providedItems: string | null
  rentalItems: string | null
  purchasableItems: string | null
  safetyInformation: string | null
}

export type TourismFestival = {
  contentId: string
  name: string
  address: string | null
  latitude: number
  longitude: number
  imageUrl: string | null
  startDate: string | null
  endDate: string | null
  distanceMeters: number
}

export type TourismAccommodation = {
  contentId: string
  contentTypeId: string
  name: string
  address: string | null
  latitude: number
  longitude: number
  imageUrl: string | null
  distanceMeters: number
}
