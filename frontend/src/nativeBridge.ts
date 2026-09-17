import { useAuthStore } from './stores/authStore'

declare global {
  interface Window {
    SendITNative?: { postMessage: (message: string) => void }
    webkit?: { messageHandlers?: { sendit?: { postMessage: (message: string) => void } } }
  }
}
function send(message: object) {
  const bridge = window.SendITNative ?? window.webkit?.messageHandlers?.sendit
  if (!bridge) return false
  bridge.postMessage(JSON.stringify(message))
  return true
}
export function closeShareWindow() {
  if (!send({ type: 'close' })) window.close()
}
export function resizeShareWindow(expanded: boolean) {
  send({ type: 'share-layout', expanded })
}
export function startNativeSessionSync() {
  const sync = () => {
    const { accessToken, refreshToken, user } = useAuthStore.getState()
    send({ type: 'session', value: JSON.stringify({ state: { accessToken, refreshToken, user }, version: 0 }) })
  }
  sync()
  useAuthStore.subscribe(sync)
}
