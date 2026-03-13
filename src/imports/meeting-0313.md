# 2026-03-13 회의 결정사항 및 업무 분장

> 작성일: 2026-03-13

---

## 회의 결정사항 요약

1. 프론트 펫케어 시스템 전체 **보류**
2. 방문인증센터 — 위치기반 인증 + 방문기록 확인 기능만 유지, **포인트 기능 삭제 / QR코드 제외**
3. 목록보기 리스트 페이지 — **추천순 / 인기순 / 최신순 정렬 기능 추가**
4. 라운지 더미데이터 삭제 후 **실제 DB 연동 진행**
5. **팀원B + 팀원D 업무 통합**으로 진행 확정

---

## 팀원별 업무 분장

---

### 팀원 A — 인프라 & 보안

| 우선순위 | 업무 | 상태 |
|---------|------|------|
| 1 | AWS EC2 / RDS(PostgreSQL) / S3 인스턴스 세팅 | 대기 |
| 2 | GitHub Actions 배포 워크플로우 완성 (`.github/workflows/deploy.yml`) | 진행중 |
| 3 | `POST /api/auth/login`, `POST /api/auth/signup` Spring Security + JWT 구현 | 대기 |
| 4 | Google/Kakao OAuth2.0 소셜 로그인 구현 | 대기 |
| 5 | 운영 환경 `application-prod.yml` 분리 | 대기 |

---

### 팀원 B + 팀원 D — 코어 데이터 & FE (통합)

> B = 백엔드 API 구현 / D = 프론트엔드 구현 및 연동

#### 즉시 처리 — 프론트 코드 변경 (팀원 D)

| 우선순위 | 업무 | 파일 |
|---------|------|------|
| 1 | **펫케어 전체 보류** — `senior-pet` 라우트 비활성 처리 (페이지 삭제 X) | `routes.tsx` |
| 2 | **VisitCheckIn 포인트 기능 제거** — `REWARDS`, `totalPoints`, 포인트 탭/UI 전체 삭제 | `VisitCheckIn.tsx` |
| 3 | **VisitCheckIn QR 제거** — `QrCode` 버튼, 스캔 모달, `showScanModal` 로직 삭제 | `VisitCheckIn.tsx` |
| 4 | **List.tsx 정렬 기능 추가** — 추천순/인기순/최신순 정렬 UI + 로직 (`rating`, `reviewCount`, `createdAt` 기준) | `List.tsx` |

#### 백엔드 API 구현 (팀원 B)

| 우선순위 | 업무 |
|---------|------|
| 1 | `GET /api/places?sort=recommended\|popular\|latest` — 장소 목록 + 정렬 파라미터 지원 |
| 2 | `GET /api/posts?page=0&size=10` — 라운지 피드 목록 (페이지네이션) |
| 3 | `POST /api/posts`, `POST /api/posts/{id}/likes`, `POST /api/posts/{id}/comments` |
| 4 | `GET /api/places?lat=&lng=&radius=` — 위치기반 장소 조회 (VisitCheckIn 방문인증용) |
| 5 | `GET /api/visits` — 방문기록 조회 API |

#### 프론트 DB 연동 — 백엔드 완성 후 (팀원 D)

| 업무 | 파일 |
|------|------|
| **라운지 더미데이터 제거 + 실 API 연동** — `INITIAL_POSTS` 삭제, `GET /api/posts` 연결 | `useFeedStore.ts` |
| 장소 목록 정렬 API 연동 | `List.tsx`, `useAppStore.ts` |
| 방문기록 실 DB 연동 (`VISIT_HISTORY` Mock 제거) | `VisitCheckIn.tsx` |
| 무한 스크롤 구현 (`IntersectionObserver` + 페이지네이션) | `Lounge.tsx` |

---

### 팀원 C — AI & 연동

> 아래 1~5번은 현재 미적용 상태 (2026-03-12 코드 기준 확인)

| 우선순위 | 업무 | 파일 | 상태 |
|---------|------|------|------|
| 1 | **Gradle 의존성 추가** — `spring-dotenv`, BOM `spring-ai-bom:1.1.2` + AI 모듈 4개 | `build.gradle` | 미적용 |
| 2 | **Docker 이미지 변경** — `pgvector/pgvector:pg16` 교체 **→ 팀원B PostGIS 충돌 합의 필수** | `docker-compose.yml` | 미적용 |
| 3 | **환경변수 추가** — `GEMINI_API_KEY`, `WEATHER_API_KEY` | `.env.example` | 미적용 |
| 4 | **application.yml Gemini 설정** 추가 | `application.yml` | 미적용 |
| 5 | pgvector 수동 설치 SQL 실행 (`CREATE EXTENSION vector`, `vector_store` 테이블) | DB 직접 | 대기 |
| 6 | Spring AI Gemini 기반 산책 코멘트 생성 API | `AIWalkGuide.tsx` 연동 대상 | 대기 |
| 7 | 날씨 API 연동 (`WEATHER_API_KEY` 활용, `AIWalkGuide.tsx` 하드코딩 대체) | 백엔드 + 프론트 | 대기 |
| 8 | 카카오 알림톡 건강 알림 자동화 | — | 대기 |

---

## 전체 진행률 (2026-03-13 기준)

| 영역 | 진행률 | 비고 |
|------|--------|------|
| 프론트 UI/UX (17페이지) | 100% | — |
| 라우팅 / 상태관리 / Mock API | 100% | — |
| **펫케어 시스템** | **보류** | 회의 결정 |
| VisitCheckIn 정리 (포인트/QR 제거) | 0% | 신규 태스크 |
| List 정렬 기능 | 0% | 신규 태스크 |
| 라운지 실 DB 연동 | 0% | 우선순위 상향 |
| 백엔드 API 전체 | 0% | B+D 통합 진행 |
| Spring AI / pgvector | 0% | C 착수 필요 |
| 인프라 / 인증 | 진행중 | A |
| **전체** | **약 45%** | 보류 항목 반영 하향 |
