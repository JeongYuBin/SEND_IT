# AWS 저비용 스테이징 운영안

## 기본 구성

- EC2 한 대에서 frontend, backend, PostgreSQL 실행
- 분석 원본 영상과 WAV는 분석 완료 즉시 삭제
- 비정상 종료로 남은 원본과 WAV는 24시간 후 자동 삭제
- OCR/게시물 프레임은 장소 이미지 출처로 사용될 수 있어 자동 삭제 대상에서 제외
- Docker 로그는 서비스별 10~30MB 범위로 순환

권장 시작 사양은 `t3.medium`, gp3 30GB입니다. 영상 분석은 한 번에 한 건만 처리하고,
메모리 부족이나 분석 대기가 확인될 때만 상향합니다.

## 실행

```bash
docker compose \
  -f compose.yaml \
  -f compose.prod.yaml \
  -f compose.staging.yaml \
  up -d --build
```

## 용량 정책

| 대상 | 정책 |
|---|---|
| 원본 영상 | 분석 성공 직후 삭제, 누락 시 24시간 후 삭제 |
| 추출 WAV | STT 처리 직후 삭제, 누락 시 24시간 후 삭제 |
| OCR 프레임 | 장소 대표 이미지로 연결된 경우 유지 |
| PostgreSQL | 매일 압축 백업, 기본 7일 보관 |
| Docker 로그 | backend/db 각 최대 30MB, frontend 최대 10MB |

S3에는 `place-images/`의 대표 이미지와 `database-backups/`의 DB 백업만 저장합니다.
SNS 원본 영상, WAV와 OCR 작업용 프레임 전체를 S3에 영구 보관하지 않습니다.

## 비용 경보

AWS Budgets에서 월 5달러, 10달러, 20달러 경보를 만듭니다. EBS 70%, 85%,
EC2 메모리 80%, CPU 70% 지속 사용도 알림 대상으로 둡니다.

## 확장 기준

- 영상 분석 중 API 응답이 느려지면 분석 worker 분리
- 테스트 사용자 데이터가 복구 불가능하면 안 되는 시점부터 RDS 사용
- EBS 사용량이 60%를 넘으면 오래된 미디어 정리 후 S3 이전 검토
- CPU 70% 또는 메모리 80%가 15분 이상 지속되면 인스턴스 조정
