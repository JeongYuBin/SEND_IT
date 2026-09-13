export function recommendedCollection(category?: string | null) {
  const value = category?.trim() ?? ''
  if (/카페|커피|디저트|베이커리|빵집/.test(value)) return '카페'
  if (/숙박|숙소|호텔|펜션|리조트|게스트/.test(value)) return '숙소'
  if (/음식|식당|구이|요리|한식|중식|일식|양식|갈비|치킨|고기|초밥|술집/.test(value)) return '음식점'
  if (/행사|축제|공연/.test(value)) return '행사'
  if (/관광|문화|레포츠|쇼핑|여행|Tourist/.test(value)) return '여행지'
  return value || '기타'
}

export class CollectionChoiceCancelled extends Error {
  constructor() { super('저장을 취소했습니다.'); this.name = 'CollectionChoiceCancelled' }
}
export type CollectionChoiceRequest = { name: string; category?: string | null; collectionId?: number }
type Chooser = (request: CollectionChoiceRequest) => Promise<number>
let chooser: Chooser | null = null
export function registerCollectionChooser(next: Chooser) {
  chooser = next
  return () => { if (chooser === next) chooser = null }
}
export function chooseCollection(request: CollectionChoiceRequest) {
  if (!chooser) return Promise.reject(new Error('컬렉션 선택창을 준비하지 못했습니다. 새로고침해 주세요.'))
  return chooser(request)
}
