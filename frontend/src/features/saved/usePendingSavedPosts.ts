import { useEffect } from 'react'
import { useInfiniteQuery, useQueryClient } from '@tanstack/react-query'
import { getSavedSharesPage } from '../share/shareApi'
import type { SavedPlace } from './types'

// Originals occupy the same grid until individual saved places arrive. No second save.
export function usePendingSavedPosts(collectionId: number | null, places: SavedPlace[]) {
  const cache = useQueryClient()
  const query = useInfiniteQuery({
    queryKey: ['saved-shares', collectionId],
    queryFn: ({ pageParam }) => getSavedSharesPage(pageParam, collectionId),
    initialPageParam: 0,
    getNextPageParam: last => last.last ? undefined : last.page + 1,
    staleTime: 0, refetchOnWindowFocus: true,
    refetchInterval: state => state.state.data?.pages.some(page => page.content.some(post => ['PENDING', 'ANALYZING'].includes(post.status))) ? 3000 : 15000,
  })
  useEffect(() => {
    if (query.dataUpdatedAt) void cache.invalidateQueries({ queryKey: ['saved-places'] })
  }, [cache, query.dataUpdatedAt])
  const linked = new Set(places.flatMap(place => place.sources.map(source => source.sharedContentId)))
  const posts = (query.data?.pages.flatMap(page => page.content) ?? [])
    .filter(post => post.status !== 'COMPLETED' && !linked.has(post.shareId))
  return { ...query, posts }
}
