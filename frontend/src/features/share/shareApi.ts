import { http } from '../../api/http'
import type { ShareAcceptedResponse } from './types'

export async function createShare(url: string) {
  const response = await http.post<ShareAcceptedResponse>('/shares', { url })
  return response.data
}

