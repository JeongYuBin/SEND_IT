import { Link } from 'react-router-dom'
import './privacy.css'

const requestEmail = `mailto:sendit8969@gmail.com?subject=${encodeURIComponent('SendIT 계정 삭제 요청')}&body=${encodeURIComponent('SendIT 계정 및 관련 데이터 삭제를 요청합니다.\r\n\r\nSendIT 아이디: \r\n가입한 이메일 주소: ')}`

export function DeleteAccountPage() {
  return (
    <main className="privacy-page">
      <title>SendIT 계정 삭제</title>
      <Link className="privacy-home" to="/">← SendIT 홈</Link>
      <article className="privacy-document">
        <header>
          <h1>SendIT 계정 삭제</h1>
          <p>SendIT은 사용자가 언제든지 계정 및 관련 데이터 삭제를 요청할 수 있도록 지원합니다.</p>
        </header>
        <section>
          <h2>1. 앱에서 계정 삭제</h2>
          <p>SendIT 앱에서 다음 경로로 계정을 삭제할 수 있습니다.</p>
          <p><strong>프로필 → 설정 → 계정 삭제</strong></p>
        </section>
        <section>
          <h2>2. 웹에서 계정 삭제 요청</h2>
          <p>앱을 사용할 수 없는 경우 아래 이메일로 계정 삭제를 요청해 주세요.</p>
          <p><strong>이메일: <a href={requestEmail}>sendit8969@gmail.com</a></strong></p>
          <p>삭제 요청 시 아래 정보를 함께 보내 주세요.</p>
          <ul>
            <li>SendIT 아이디</li>
            <li>가입한 이메일 주소</li>
          </ul>
          <a className="privacy-request-button" href={requestEmail}>이메일로 삭제 요청하기</a>
          <p className="privacy-request-help">메일 작성 화면이 열립니다. 위 정보를 입력한 후 이메일을 보내 주세요. 메일 앱이 없다면 위 주소로 직접 보내실 수 있습니다.</p>
        </section>
        <section>
          <h2>3. 삭제되는 데이터</h2>
          <p>계정 삭제 시 다음 데이터가 함께 삭제됩니다.</p>
          <ul>
            <li>계정 정보</li>
            <li>저장한 장소</li>
            <li>컬렉션</li>
            <li>여행 계획 및 일정</li>
            <li>저장한 콘텐츠 및 관련 서비스 데이터</li>
          </ul>
          <p>삭제 요청 처리 과정에서 본인 확인을 위해 추가 정보를 요청할 수 있습니다.</p>
          <p><strong>서비스명: SendIT</strong><br />문의: <a href="mailto:sendit8969@gmail.com">sendit8969@gmail.com</a></p>
          <p><Link to="/privacy">개인정보처리방침 보기</Link></p>
        </section>
      </article>
    </main>
  )
}
