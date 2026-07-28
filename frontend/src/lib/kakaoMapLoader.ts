let kakaoMapsPromise: Promise<KakaoMapsApi> | null = null

export function loadKakaoMaps(): Promise<KakaoMapsApi> {
  if (kakaoMapsPromise) return kakaoMapsPromise

  const appKey = import.meta.env.VITE_KAKAO_MAP_APP_KEY
  if (!appKey) {
    return Promise.reject(new Error('카카오맵 JavaScript 키가 설정되지 않았습니다.'))
  }

  const loadingPromise = new Promise<KakaoMapsApi>((resolve, reject) => {
    const start = () => {
      if (!window.kakao?.maps) {
        reject(new Error('카카오맵 SDK를 불러오지 못했습니다.'))
        return
      }
      window.kakao.maps.load(() => resolve(window.kakao!.maps))
    }

    const existingScript = document.querySelector<HTMLScriptElement>(
      'script[data-sendit-kakao-map]',
    )
    if (existingScript) {
      if (window.kakao?.maps) start()
      else {
        existingScript.addEventListener('load', start, { once: true })
        existingScript.addEventListener(
          'error',
          () => reject(new Error('카카오맵 SDK 요청에 실패했습니다.')),
          { once: true },
        )
      }
      return
    }

    const script = document.createElement('script')
    script.dataset.senditKakaoMap = 'true'
    script.async = true
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(appKey)}&autoload=false`
    script.addEventListener('load', start, { once: true })
    script.addEventListener(
      'error',
      () => reject(new Error('카카오맵 SDK 요청에 실패했습니다.')),
      { once: true },
    )
    document.head.appendChild(script)
  }).catch((error) => {
    kakaoMapsPromise = null
    throw error
  })

  kakaoMapsPromise = loadingPromise
  return loadingPromise
}
