import { useEffect, useRef, useState } from 'react'
import { loadKakaoMaps } from '../lib/kakaoMapLoader'

export type KakaoMapPoint = {
  id: number | string
  name: string
  latitude: number
  longitude: number
  label?: string
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
}

const EMPTY_ROUTES: KakaoMapRoute[] = []

export function KakaoMap({ points, routes = EMPTY_ROUTES, ariaLabel, onSelect }: KakaoMapProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let disposed = false
    const overlays: KakaoOverlay[] = []
    const cleanupCallbacks: Array<() => void> = []

    loadKakaoMaps()
      .then((maps) => {
        if (disposed || !containerRef.current || points.length === 0) return
        setError(null)
        const center = new maps.LatLng(points[0].latitude, points[0].longitude)
        const map = new maps.Map(containerRef.current, { center, level: 7 })
        const bounds = new maps.LatLngBounds()

        points.forEach((point) => {
          const position = new maps.LatLng(point.latitude, point.longitude)
          bounds.extend(position)
          overlays.push(new maps.Marker({ map, position }))

          const label = document.createElement('button')
          label.type = 'button'
          label.className = 'kakao-map-label'
          label.textContent = point.label ?? point.name
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
            yAnchor: 2.25,
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

        if (points.length === 1) {
          map.setCenter(center)
          map.setLevel(4)
        } else {
          map.setBounds(bounds)
        }
        window.setTimeout(() => map.relayout(), 0)
      })
      .catch((reason: Error) => {
        if (!disposed) setError(reason.message)
      })

    return () => {
      disposed = true
      cleanupCallbacks.forEach((cleanup) => cleanup())
      overlays.forEach((overlay) => overlay.setMap(null))
    }
  }, [onSelect, points, routes])

  if (error) {
    return <div className="map-error" role="alert">{error}</div>
  }

  return <div className="kakao-map" ref={containerRef} role="img" aria-label={ariaLabel} />
}
