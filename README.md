# SEND IT

SNS, 블로그, 지도 등에서 발견한 관광 콘텐츠를 장소 데이터로 저장하고,
한국관광공사 관광 데이터와 결합해 방문 가능한 여행 일정을 만드는 서비스입니다.

## 프로젝트 구성

```text
SEND_IT/
├─ frontend/              React + TypeScript + Vite
├─ backend/               Java 21 + Spring Boot
├─ docs/                  아키텍처 및 개발 문서
├─ compose.yaml           PostGIS + Redis + 애플리케이션
└─ .env.example           로컬 환경변수 예시
```

백엔드는 기능별 패키지를 가진 모듈형 모놀리스로 시작합니다. 서비스 경계가
검증되기 전까지 분산 시스템의 운영 복잡도를 만들지 않는 것이 원칙입니다.

## 시작하기

### 전체 환경을 Docker로 실행

```bash
cp .env.example .env
docker compose up --build
```

- 프런트엔드: http://localhost:5173
- 백엔드 상태 확인: http://localhost:8080/actuator/health
- API 문서: http://localhost:8080/swagger-ui.html

### 프런트엔드만 실행

```bash
cd frontend
npm install
npm run dev
```

### 백엔드만 실행

Java 21과 Maven 3.9 이상이 필요합니다.

```bash
cd backend
mvn spring-boot:run
```

PostgreSQL과 Redis 접속 정보는 `backend/src/main/resources/application.yml`의
환경변수로 변경할 수 있습니다.

## 초기 개발 원칙

- 외부 API 키와 비밀번호는 저장소에 커밋하지 않습니다.
- API는 `/api/v1` 아래에서 버전을 관리합니다.
- DB 스키마 변경은 Flyway 마이그레이션으로만 수행합니다.
- 관광공사 API 응답은 백엔드에서 정규화하고 프런트엔드가 직접 호출하지 않습니다.
- 기능 작업은 검증 가능한 단위로 나누고 각 단위마다 Git 커밋을 남깁니다.

