import { Component, type ErrorInfo, type ReactNode } from 'react'

type State = { failed: boolean }

export class AppErrorBoundary extends Component<{ children: ReactNode }, State> {
  state: State = { failed: false }

  static getDerivedStateFromError(): State {
    return { failed: true }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    if (import.meta.env.DEV) console.error('화면 렌더링 오류', error, info)
  }

  render() {
    if (!this.state.failed) return this.props.children
    return (
      <main className="fatal-error" role="alert">
        <span className="eyebrow">SEND IT</span>
        <h1>화면을 불러오지 못했습니다.</h1>
        <p>저장된 데이터는 그대로 유지됩니다. 페이지를 새로 불러와 다시 시도해 주세요.</p>
        <button type="button" onClick={() => window.location.reload()}>다시 불러오기</button>
      </main>
    )
  }
}
