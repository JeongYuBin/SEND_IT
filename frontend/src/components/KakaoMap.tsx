import { useEffect, useRef, useState } from 'react'
import { loadKakaoMaps } from '../lib/kakaoMapLoader'

export type KakaoMapPoint = {
  id: number | string
  name: string
  latitude: number
  longitude: number
  label?: string
  category?: string | null
}

export type KakaoMapRoute = {
  id: number | string
  color: string
  points: KakaoMapPoint[]
}

type KakaoMapProps = {
  points: KakaoMapPoint[]
  routes?: KakaoMapRoute[]
  ariaLabel: string
  onSelect?: (point: KakaoMapPoint) => void
  initialCenter?: { latitude: number; longitude: number }
  fitPoints?: boolean
}

const EMPTY_ROUTES: KakaoMapRoute[] = []

type MarkerVisual = { color: string; paths: string[] }

function markerVisual(category?: string | null): MarkerVisual {
  const value = category?.toLowerCase() ?? ''
  if (/카페|커피|디저트|coffee|cafe/.test(value)) return {
    color: '#9b6b4a', paths: ['M5 8h11v5a5 5 0 0 1-5 5H9a4 4 0 0 1-4-4V8Z', 'M16 10h1.5a2.5 2.5 0 0 1 0 5H16', 'M7 4c0 1 1 1 1 2', 'M11 4c0 1 1 1 1 2'],
  }
  if (/숙소|호텔|펜션|게스트|리조트|hotel/.test(value)) return {
    color: '#0a84ff', paths: ['m3.5 11 8.5-7 8.5 7', 'M5.5 10v10h13V10', 'M9.5 20v-6h5v6'],
  }
  if (/음식|식당|맛집|한식|중식|일식|양식|restaurant/.test(value)) return {
    color: '#ff6b35', paths: ['M6 3v8', 'M3.5 3v5a2.5 2.5 0 0 0 5 0V3', 'M6 11v10', 'M16 3v18', 'M16 3c3 2 3 7 0 9'],
  }
  if (/쇼핑|시장|백화점|shopping/.test(value)) return {
    color: '#bf5af2', paths: ['M5 8h14l-1 13H6L5 8Z', 'M9 9V6a3 3 0 0 1 6 0v3'],
  }
  if (/술집|주점|바|bar/.test(value)) return {
    color: '#ff375f', paths: ['M5 4h14l-7 8-7-8Z', 'M12 12v7', 'M8 20h8'],
  }
  return { color: '#34c759', paths: ['m12 3 2.7 5.5 6.1.9-4.4 4.3 1 6.1-5.4-2.9-5.4 2.9 1-6.1-4.4-4.3 6.1-.9L12 3Z'] }
}

function markerIcon(category?: string | null) {
  const visual = markerVisual(category)
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
  svg.setAttribute('viewBox', '0 0 24 24')
  svg.setAttribute('aria-hidden', 'true')
  visual.paths.forEach((value) => {
    const path = document.createElementNS('http://www.w3.org/2000/svg', 'path')
    path.setAttribute('d', value)
    svg.append(path)
  })
  return { color: visual.color, svg }
}

export function KakaoMap({
  points,
  routes = EMPTY_ROUTES,
  ariaLabel,
  onSelect,
  initialCenter,
  fitPoints = true,
}: KakaoMapProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const mapRef = useRef<KakaoMapInstance | null>(null)
  const appliedCenter = useRef<typeof initialCenter>(undefined)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let disposed = false
    const overlays: KakaoOverlay[] = []
    const cleanupCallbacks: Array<() => void> = []

    loadKakaoMaps()
      .then((maps) => {
        if (disposed || !containerRef.current) return
        setError(null)
        const center = new maps.LatLng(
          initialCenter?.latitude ?? points[0]?.latitude ?? 37.5665,
          initialCenter?.longitude ?? points[0]?.longitude ?? 126.978,
        )
        const map = mapRef.current ?? new maps.Map(containerRef.current, { center, level: initialCenter ? 5 : 7 })
        mapRef.current = map
        const bounds = new maps.LatLngBounds()
        const markerElements: HTMLButtonElement[] = []

        points.forEach((point) => {
          const position = new maps.LatLng(point.latitude, point.longitude)
          bounds.extend(position)
          const label = document.createElement('button')
          label.type = 'button'
          label.className = 'kakao-map-category-marker'
          label.dataset.category = point.category ?? ''
          const icon = markerIcon(point.category)
          label.style.setProperty('--marker-color', icon.color)
          const name = document.createElement('b')
          name.textContent = point.label ?? point.name
          label.append(icon.svg, name)
          markerElements.push(label)
          label.title = point.name
          if (onSelect) {
            const handleClick = () => onSelect(point)
            label.addEventListener('click', handleClick)
            cleanupCallbacks.push(() => label.removeEventListener('click', handleClick))
          }
          overlays.push(new maps.CustomOverlay({
            map,
            position,
            content: label,
            yAnchor: 0.5,
          }))
        })

        routes.forEach((route) => {
          if (route.points.length < 2) return
          overlays.push(new maps.Polyline({
            map,
            path: route.points.map(
              (point) => new maps.LatLng(point.latitude, point.longitude),
            ),
            strokeWeight: 5,
            strokeColor: route.color,
            strokeOpacity: 0.8,
            strokeStyle: 'solid',
          }))
        })

        if (!fitPoints || points.length === 0) {
          if (initialCenter && appliedCenter.current !== initialCenter) {
            map.setCenter(center)
            appliedCenter.current = initialCenter
          }
        } else if (points.length === 1) {
          map.setCenter(center)
          map.setLevel(4)
        } else {
          map.setBounds(bounds)
        }
        const resizeMarkers = () => {
          const level = map.getLevel()
          const scale = Math.max(0.56, Math.min(1, 1.08 - (level - 1) * 0.07))
          markerElements.forEach((marker) => {
            marker.style.setProperty('--marker-scale', String(scale))
            marker.classList.toggle('hide-name', level >= 6)
          })
        }
        resizeMarkers()
        maps.event.addListener(map, 'zoom_changed', resizeMarkers)
        cleanupCallbacks.push(() => maps.event.removeListener(map, 'zoom_changed', resizeMarkers))
        const timer = window.setTimeout(() => map.relayout(), 0)
        cleanupCallbacks.push(() => window.clearTimeout(timer))
      })
      .catch((reason: Error) => {
        if (!disposed) setError(reason.message)
      })

    return () => {
      disposed = true
      cleanupCallbacks.forEach((cleanup) => cleanup())
      overlays.forEach((overlay) => overlay.setMap(null))
    }
  }, [fitPoints, initialCenter, onSelect, points, routes])

  if (error) {
    return <div className="map-error" role="alert">{error}</div>
  }

  return <div className="kakao-map" ref={containerRef} role="img" aria-label={ariaLabel} />
}
