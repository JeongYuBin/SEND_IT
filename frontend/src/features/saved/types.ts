export type VisitStatus = 'WANT_TO_VISIT' | 'PLANNED' | 'VISITED'

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
  collectionId: number | null
  collectionName: string | null
  memo: string | null
  visitStatus: VisitStatus
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
