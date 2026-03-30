# 멍냥트립 2.0 — Claude 작업 가이드

> 마감: **2026-03-27** | 배포: `http://54.180.22.22:8080` | 브랜치: `feat/frontend` → `main`

---

## 현재 활동 담당자: 팀원D

다른 팀원 관점이 필요하면 아래처럼 요청하면 돼:

```
"팀원A 입장에서 배포 파이프라인을 검토해줘"
"팀원B 역할로 Feed API 설계를 도와줘"
"팀원C 관점에서 Spring AI 연동 방향 알려줘"
"UI/UX 디자이너로서 이 페이지 개선점을 제안해줘"
```

---

## 팀원별 역할 정의

---

### 팀원A — 인프라 & 배포

**책임 범위:**
- AWS EC2 배포 및 운영 (`54.180.22.22:8080`)
- Docker 컨테이너 관리 (backend, frontend, PostgreSQL, Redis)
- GitHub Actions CI/CD 파이프라인
- SSL 인증서 (Let's Encrypt, `meongnyangtrip.duckdns.org`)
- DB 백업 및 복원 (S3 연동)

**주요 파일:**
- `Dockerfile.frontend` — FE 멀티스테이지 빌드 (node:20-alpine → nginx:alpine)
- `nginx.conf` — HTTPS 리다이렉트, API 프록시(`/api/`, `/oauth2/`), SPA fallback
- `backend/src/main/resources/application-prod.yml` — 운영 프로파일 설정
- `.github/workflows/deploy.yml` — SSM 기반 EC2 자동 배포

**환경 변수 (GitHub Secrets 등록 필요):**
```
AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY
DB_PASSWORD / JWT_SECRET
VITE_KAKAO_MAP_API_KEY / VITE_KAKAO_REST_API_KEY
PET_TOUR_SERVICE_KEY
NAVER_LOCAL_CLIENT_ID / NAVER_LOCAL_CLIENT_SECRET
KAKAO_OAUTH_CLIENT_ID / KAKAO_OAUTH_CLIENT_SECRET   ← 미등록
GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET             ← 미등록
GEMINI_API_KEY / GEMINI_API_KEY_2                   ← 미등록
MAIL_USERNAME / MAIL_PASSWORD                       ← 미등록
GOOGLE_PLACES_API_KEY                               ← 미등록
```

**현재 미완료:**
- `deploy.yml` `docker run`에 누락 환경변수 9개 추가
- `.dockerignore` 생성 (현재 없음 → `backend/.env` 이미지 포함 위험)
- `nginx.conf` 배치 API 타임아웃 추가 (`proxy_read_timeout 300s`)
- S3 DB 백업(`meongnyangtrip-bucket/db-backup/`) → EC2 DB 복원

---

### 팀원B — 피드 & 소셜

**책임 범위:**
- Feed / 라운지 BE 전체 (Entity, Repository, Service, Controller)
- 댓글(Comment) 시스템
- 팔로우/언팔로우 (Talk)
- FE Feed 페이지 실 API 연동

**주요 파일:**
- `src/app/pages/Feed.tsx` — FE 피드 페이지 (현재 mock 상태)
- `src/app/pages/Lounge.tsx` — FE 라운지 (더미 데이터)
- `src/app/store/useFeedStore.ts` — Zustand 피드 상태관리
- `src/app/store/useFriendStore.ts` — 팔로우 상태관리
- `backend/src/main/java/com/team/meongnyang/feed/` — BE 미착수

**API 연동 현황:**
| API | 상태 |
|---|---|
| `GET /api/v1/feeds` | ❌ BE 미착수 |
| `POST /api/v1/feeds` | ❌ BE 미착수 |
| `GET /api/v1/feeds/{id}/comments` | ❌ BE 미착수 |
| `POST /api/v1/feeds/{id}/comments` | ❌ BE 미착수 |
| `POST /api/v1/users/{id}/follow` | ❌ BE 미착수 |

**현재 미완료 (전체):**
- `FeedController` + `FeedService` (페이지네이션)
- `Feed` / `Comment` / `Talk` Entity + API
- FE `Lounge.tsx` 더미 → 실 API 교체

---

### 팀원C — AI & Spring AI 알림 서비스

**책임 범위:**
- Spring AI 기반 산책 가이드 코멘트 생성 API
- AI 장소 알림 서비스 (`places_for_ai_service.json` 244건 기반)
- 날씨 API 연동
- pgvector 도입 (PostgreSQL 벡터 확장)

**주요 파일:**
- `src/app/pages/AIWalkGuide.tsx` — FE AI 산책 가이드 (현재 하드코딩)
- `src/app/api/walkGuideApi.ts` — FE API 레이어 (신규)
- `exports/places_for_ai_service.json` — AI 서비스용 장소 데이터 244건 ← 전달 완료
- `backend/src/main/java/com/team/meongnyang/batch/` — 배치 서비스 참고용

**places_for_ai_service.json 필드:**
```
id, title, address, category, overview, tags,
ai_rating, operating_hours, operation_policy,
pet_facility, pet_policy
```

**현재 미완료 (전체):**
- `build.gradle` Spring AI BOM 의존성 추가
- Docker 이미지 → `pgvector/pgvector:pg16` 변경 (PostGIS 충돌 합의 필요)
- `GEMINI_API_KEY`, `WEATHER_API_KEY` 환경변수 등록
- Spring AI Gemini 산책 코멘트 생성 API 구현
- 날씨 API 연동 (`AIWalkGuide.tsx` 하드코딩 대체)

---

### 팀원D — FE & UI/UX & 인터페이스 (현재 담당자)

**책임 범위:**
- React/TypeScript FE 전체 21개 페이지
- UI/UX 설계 및 디자인 구현 (모바일 퍼스트, 디자인 시스템 총괄)
- 페이지 레이아웃, 컴포넌트 디자인, 인터랙션 (애니메이션, 트랜지션)
- API 명세 총괄 및 노션 문서화
- Mock API 인터셉터 관리
- 디자인 시스템 (Tailwind CSS 변수 기반)

**주요 파일:**
- `src/imports/work-progress/master-guide.md` — 유일한 기준 문서 (매 세션 필독)
- `src/app/mocks/mockApi.ts` — axios 인터셉터 Mock 핸들러
- `src/app/api/` — 실 API 레이어 (axios 기반)
- `src/app/store/useAppStore.ts` — 전역 상태 (places, wishlist, auth)
- `guidelines/Guidelines.md` — 프로젝트 규칙
- `src/imports/fe-dev-guidelines.md` — 디자인 시스템

**환경 변수:**
```
VITE_KAKAO_MAP_API_KEY=ebdedf4d5009cd05a32ac3cb5b1a834b
VITE_USE_MOCK=false  (운영), true (개발 단독 테스트)
VITE_API_BASE_URL=/api/v1
```

**코딩 규칙:**
- 색상: CSS 변수 기반 Tailwind (`text-primary`, `bg-primary`) — 하드코딩 금지
- 아이콘: `lucide-react`만 사용
- 모바일 퍼스트 (`max-width: 600px`)
- `index.html` 환경변수: `<{ VITE_KAKAO_MAP_API_KEY }>` 문법 (vite-plugin-html-env)

**Git 규칙:**
- 커밋/푸시 순서: `feat/frontend` 커밋 → `feat/frontend` push → `main` no-ff 머지 → `main` push
- `main`에 직접 커밋/push 금지
- `src/imports/work-progress/` 폴더는 `.gitignore` 적용 — git add 금지

---

### UI/UX 디자이너 — 전문 디자인 역할

> 팀원 역할과 별개로 언제든 디자인 전문가 관점으로 전환 가능.
> `"UI/UX 디자이너로서..."` 라고 요청하면 이 역할로 답변.

**역할 정의:**
- 사용자 경험(UX) 흐름 분석 및 개선 제안
- 모바일 앱 수준의 UI 디자인 (반려동물 앱 타깃: 20~40대)
- 컴포넌트 시각 계층 구조, 여백, 타이포그래피 검토
- 인터랙션 디자인 (터치 피드백, 트랜지션, 애니메이션)
- 접근성 (터치 타깃 최소 44px, 명도 대비)

**디자인 시스템 기준:**
- 컬러: CSS 변수 기반 (`--primary`, `--brand-point` 등) — 하드코딩 절대 금지
- 아이콘: `lucide-react`만 사용 (이모지는 의미 전달용으로만 제한적 사용)
- 레이아웃: `max-width: 600px` 모바일 퍼스트, flexbox/grid 기반
- 카드 반경: `rounded-xl`(12px) ~ `rounded-2xl`(16px) 일관 적용
- 폰트 사이즈: 본문 `text-xs`(12px) ~ `text-sm`(14px), 제목 `text-[15px]` ~ `text-[20px]`
- 그림자: `shadow-sm` 위주, 레이어 구분 시 `shadow-md`
- 상태 색상: primary(메인), brand-point(별점/강조), destructive(경고/삭제)

**참고 페이지 (디자인 기준점):**
- `src/app/pages/Detail.tsx` — 히어로 이미지 + 탭 네비 + 카드 레이아웃
- `src/app/pages/Home.tsx` — 배너 슬라이더 + 카테고리 탭 + 랭킹 카드
- `src/app/pages/MyPage.tsx` — 프로필 + 섹션 리스트

**디자인 개선 시 체크리스트:**
- [ ] 터치 영역이 충분한가 (버튼 최소 44px)
- [ ] 정보 계층이 명확한가 (제목 → 부제 → 본문 순서)
- [ ] 빈 상태(empty state)가 처리되었는가
- [ ] 로딩 상태가 표현되었는가
- [ ] 에러 피드백이 사용자에게 전달되는가
- [ ] 모바일에서 스크롤 없이 핵심 정보가 보이는가

---

## 기술 스택 요약

| 영역 | 스택 |
|---|---|
| FE | React 18 + TypeScript, Vite, Tailwind CSS |
| 라우터 | React Router v7 Data Mode |
| 상태관리 | Zustand (`useAppStore`, `useFeedStore`, `useFriendStore`) |
| 지도 | react-kakao-maps-sdk v1.2.1 |
| BE | Spring Boot 3, Java 21 (Virtual Threads) |
| DB | PostgreSQL 16 + PostGIS, Redis 7 |
| 인증 | JWT + OAuth2 (Google, Kakao) |
| AI | Google Gemini Vision API (배치), Spring AI (팀원C) |
| 배포 | AWS EC2 + S3, Docker, GitHub Actions + SSM |

---

## 통합 일정 (3/23~27)

| 날짜 | 주요 활동 |
|---|---|
| 3/23 ✅ | 팀 전체 현황 점검, Gmail SMTP 구현, Detail UI 개선, DB S3 백업 |
| 3/24 | 팀원C AI API 연동 지원, API 명세 노션 문서화 |
| 3/25 | 통합 테스트 + 배치 재실행 (EC2 기준), Swagger 전체 확인 |
| 3/26 | 전체 버그 수정 + AWS 운영 배포 검증 |
| 3/27 | **마감** — 최종 점검, 데모 준비, 제출 |
