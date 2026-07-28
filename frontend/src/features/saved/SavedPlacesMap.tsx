import { useEffect, useMemo } from 'react'
import { divIcon, latLngBounds } from 'leaflet'
import { MapContainer, Marker, Popup, TileLayer, useMap } from 'react-leaflet'
import { Link } from 'react-router-dom'
import 'leaflet/dist/leaflet.css'
import type { SavedPlace } from './types'

const placeIcon = divIcon({
  className: 'saved-place-marker',
  html: '<span></span>',
  iconSize: [28, 36],
  iconAnchor: [14, 36],
  popupAnchor: [0, -32],
})

function FitSavedPlaces({ places }: { places: SavedPlace[] }) {
  const map = useMap()

  useEffect(() => {
    if (places.length === 0) return
    const bounds = latLngBounds(places.map((place) => [place.latitude!, place.longitude!]))
    if (places.length === 1) {
      map.setView(bounds.getCenter(), 14)
    } else {
      map.fitBounds(bounds, { padding: [48, 48], maxZoom: 15 })
    }
  }, [map, places])

  return null
}

export function SavedPlacesMap({ places }: { places: SavedPlace[] }) {
  const mappedPlaces = useMemo(
    () => places.filter((place) => place.latitude !== null && place.longitude !== null),
    [places],
  )

  if (mappedPlaces.length === 0) {
    return (
      <div className="map-empty-state">
        <strong>지도에 표시할 수 있는 장소가 없어요.</strong>
        <span>주소와 좌표가 분석된 장소를 저장하면 지도에서 확인할 수 있습니다.</span>
      </div>
    )
  }

  return (
    <section className="saved-map" aria-label="저장 장소 지도">
      <MapContainer center={[36.5, 127.8]} zoom={7} scrollWheelZoom>
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <FitSavedPlaces places={mappedPlaces} />
        {mappedPlaces.map((place) => (
          <Marker
            key={place.savedPlaceId}
            position={[place.latitude!, place.longitude!]}
            icon={placeIcon}
          >
            <Popup>
              <div className="map-place-popup">
                <span>{place.category ?? '미분류'}</span>
                <strong>{place.name}</strong>
                <small>{place.roadAddress ?? place.address ?? '주소 정보 없음'}</small>
                <Link to={`/saved/places/${place.savedPlaceId}`}>상세 보기</Link>
              </div>
            </Popup>
          </Marker>
        ))}
      </MapContainer>
    </section>
  )
}
