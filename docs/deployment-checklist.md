# SEND IT 운영 배포 체크리스트

## 필수 환경변수

- `POSTGRES_PASSWORD`: 개발 기본값과 다른 긴 비밀번호
- `JWT_SECRET`: 32바이트 이상의 무작위 문자열
- `CORS_ALLOWED_ORIGINS`: 실제 HTTPS 프런트엔드 도메인만 쉼표로 지정
- `TOUR_API_SERVICE_KEY`, `KAKAO_REST_API_KEY`, `YOUTUBE_API_KEY`
- `VITE_API_BASE_URL`, `VITE_KAKAO_MAP_APP_KEY`

운영 실행은 개발 Compose에 운영 오버레이를 함께 사용합니다.

```bash
docker compose -f compose.yaml -f compose.prod.yaml up -d --build
```

## 배포 전 확인

1. `.env`가 Git 추적 대상이 아닌지 확인합니다.
2. `DOCS_PUBLIC=false`와 `SPRING_PROFILES_ACTIVE=prod`를 확인합니다.
3. DB·Redis 포트가 인터넷에 노출되지 않았는지 확인합니다.
4. `/actuator/health`가 `UP`인지 확인합니다.
5. 회원가입, 로그인, URL 분석, 장소 저장, 일정 생성 스모크 테스트를 실행합니다.
6. HTTPS 인증서와 HTTP→HTTPS 리다이렉트를 확인합니다.
7. PostgreSQL 백업을 생성한 뒤 별도 저장소에서 복원 테스트합니다.

## 백업과 복구

백업:

```bash
docker compose exec -T db pg_dump -U sendit -Fc sendit > sendit.dump
```

새 DB에 복구:

```bash
docker compose exec -T db pg_restore -U sendit -d sendit --clean --if-exists < sendit.dump
```

미디어는 `media_uploads` 볼륨 또는 운영 S3 버킷을 DB 백업과 같은 시점에 보관합니다.
복구 연습 없이 생성만 한 백업은 운영 백업으로 간주하지 않습니다.

## 운영 관측

- 백엔드 상태와 컨테이너 재시작 횟수 알림
- 5xx 응답률과 분석 실패율
- DB 디스크 사용량과 연결 수
- 미디어 저장소 사용량
- 외부 API 401·403·429 응답 증가
- 분석 작업이 `PROCESSING` 상태로 30분 이상 머무는지 확인
