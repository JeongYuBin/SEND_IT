export function App() {
  return (
    <main className="shell">
      <section className="hero">
        <span className="eyebrow">SEND IT</span>
        <h1>발견한 여행지를<br />진짜 여행으로.</h1>
        <p>
          SNS와 블로그에서 찾은 장소를 저장하고,
          방문 가능한 여행 동선으로 만들어 보세요.
        </p>
        <form className="share-form" onSubmit={(event) => event.preventDefault()}>
          <label htmlFor="content-url">저장할 콘텐츠 URL</label>
          <div>
            <input
              id="content-url"
              type="url"
              placeholder="https://..."
              aria-describedby="url-help"
            />
            <button type="submit">장소 찾기</button>
          </div>
          <small id="url-help">Instagram, YouTube, 네이버 블로그와 일반 웹페이지를 지원할 예정입니다.</small>
        </form>
      </section>
    </main>
  )
}

