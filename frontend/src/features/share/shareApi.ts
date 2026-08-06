import { http } from '../../api/http'
import type { ShareAcceptedResponse, ShareDetail } from './types'

export type CreateShareInput = {
  url: string
  sharedText?: string
}

export async function createShare(input: string | CreateShareInput) {
  const request = typeof input === 'string' ? { url: input } : input
  const response = await http.post<ShareAcceptedResponse>('/shares', request)
  return response.data
}

export async function getShare(shareId: number) {
  const response = await http.get<ShareDetail>(`/shares/${shareId}`)
  return response.data
}

export async function getShares() {
  return (await http.get<ShareDetail[]>('/shares')).data
}

export async function reanalyzeShare(shareId: number) {
  const response = await http.post<ShareAcceptedResponse>(`/shares/${shareId}/reanalyze`)
  return response.data
}

export async function uploadSharedVideo(file: File, sharedText?: string) {
  const form = new FormData()
  form.append('file', file)
  if (sharedText?.trim()) form.append('sharedText', sharedText.trim())
  const response = await http.post<ShareAcceptedResponse>('/shares/media', form)
  return response.data
}
