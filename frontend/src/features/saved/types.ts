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
}

export type NearbyTourismPlace = {
  contentId: string
  name: string
  category: string | null
  address: string | null
  latitude: number
  longitude: number
  imageUrl: string | null
  distanceMeters: number
}
