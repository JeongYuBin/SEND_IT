import { useNavigate } from 'react-router-dom'

export function FeedDiscovery() {
  const navigate = useNavigate()
  return <section className="feed-discovery"><div className="discovery-shortcuts">
    <button onClick={() => navigate('/?explore=nearby')}><span>주변 여행지<br />찾기</span><span aria-hidden="true">✦</span></button>
    <button onClick={() => navigate('/?explore=festival')}><span>이번 달 축제<br />찾기</span><span aria-hidden="true">♫</span></button>
  </div></section>
}
