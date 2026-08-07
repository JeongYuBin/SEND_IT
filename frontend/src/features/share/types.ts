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
  shareId: number
  originalUrl: string
  sourceType: 'INSTAGRAM' | 'YOUTUBE' | 'NAVER_BLOG' | 'MAP' | 'WEB'
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
  createdAt: string
}
