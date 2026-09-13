export type AnalysisStatus =
  | 'PENDING'
  | 'ANALYZING'
  | 'COMPLETED'
  | 'NEEDS_CONFIRMATION'
  | 'FAILED'

export type ShareAcceptedResponse = {
  shareId: number
  status: AnalysisStatus
  message: string
  duplicate: boolean
}

export type ShareDetail = {
  collectionId: number | null
  collectionName: string | null
  shareId: number
  originalUrl: string
  sourceType: 'INSTAGRAM' | 'TIKTOK' | 'YOUTUBE' | 'NAVER_BLOG' | 'MAP' | 'WEB'
  sharedText: string | null
  title: string | null
  description: string | null
  thumbnailUrl: string | null
  status: AnalysisStatus
  analysisError: string | null
  extractedPlaceName: string | null
  extractedCategory: string | null
  extractedAddress: string | null
  extractedLatitude: number | null
  extractedLongitude: number | null
  mediaOriginalFilename: string | null
  mediaContentType: string | null
  mediaSizeBytes: number | null
  mediaDurationSeconds: number | null
  mediaFrameCount: number
  mediaAudioAvailable: boolean
  mediaOcrText: string | null
  mediaTranscript: string | null
  extractedPlaces: ExtractedPlace[]
  createdAt: string
}

export type ExtractedPlace = {
  id: number
  order: number
  name: string
  category: string | null
  address: string | null
  latitude: number | null
  longitude: number | null
  imageUrl: string | null
  savedPlaceId: number | null
}

export type SharePage = {
  content: ShareDetail[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}
