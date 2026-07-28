import { http } from '../../api/http'
import type { ShareAcceptedResponse, ShareDetail } from './types'

export async function createShare(url: string) {
  const response = await http.post<ShareAcceptedResponse>('/shares', { url })
  return response.data
}

export async function getShare(shareId: number) {
  const response = await http.get<ShareDetail>(`/shares/${shareId}`)
  return response.data
}
