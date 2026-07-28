interface KakaoLatLng {
  getLat(): number
  getLng(): number
}

interface KakaoMapInstance {
  setBounds(bounds: KakaoLatLngBounds): void
  setCenter(position: KakaoLatLng): void
  setLevel(level: number): void
  relayout(): void
}

interface KakaoLatLngBounds {
  extend(position: KakaoLatLng): void
}

interface KakaoOverlay {
  setMap(map: KakaoMapInstance | null): void
}

interface KakaoMapsApi {
  load(callback: () => void): void
  Map: new (
    container: HTMLElement,
    options: { center: KakaoLatLng; level: number },
  ) => KakaoMapInstance
  LatLng: new (latitude: number, longitude: number) => KakaoLatLng
  LatLngBounds: new () => KakaoLatLngBounds
  Marker: new (options: {
    map: KakaoMapInstance
    position: KakaoLatLng
  }) => KakaoOverlay
  CustomOverlay: new (options: {
    map: KakaoMapInstance
    position: KakaoLatLng
    content: HTMLElement
    yAnchor?: number
  }) => KakaoOverlay
  Polyline: new (options: {
    map: KakaoMapInstance
    path: KakaoLatLng[]
    strokeWeight: number
    strokeColor: string
    strokeOpacity: number
    strokeStyle: string
  }) => KakaoOverlay
}

interface Window {
  kakao?: {
    maps: KakaoMapsApi
  }
}
