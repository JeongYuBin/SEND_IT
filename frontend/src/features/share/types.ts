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

