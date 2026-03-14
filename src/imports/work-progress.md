# 멍냥트립 2.0 — 작업 진행 현황

> 최초 작성: 2026-03-14 | 담당: 팀원D (FE & Interface + 백엔드 통합)
> 매 작업 세션마다 누적 업데이트 — 최종 수정: 2026-03-14 세션 2

---

## 1. 전체 작업 현황 요약

| 영역 | 완료 | 진행중 | 대기 |
|---|:---:|:---:|:---:|
| 프론트엔드 UI/UX | ✅ | — | — |
| ERD / DB 설계 | ✅ | — | — |
| 백엔드 Entity/Repository | ✅ | — | — |
| 공공데이터 배치 + 교차검증 | ✅ | — | — |
| PostGIS 위치 검색 API | ✅ | — | — |
| Redis 캐싱 | ✅ | — | — |
| 프론트↔백엔드 연동 | — | 🔄 | — |
| 인증(JWT) | — | — | ⏳ 팀원A |
| PET CRUD API | — | — | ⏳ |
| Wishlist / Review API | — | — | ⏳ |
| Feed / Talk API | — | — | ⏳ |
| AI 연동 (Gemini) | — | — | ⏳ 팀원C |

---

## 2. 세션별 완료 작업

### 2026-03-13 (이전 세션)

| # | 구분 | 작업 내용 | 파일 |
|---|---|---|---|
| 01 | FE | Lounge FAB — 오른쪽 컨테이너 외부 고정 위치 변경 | `Lounge.tsx` |
| 02 | FE | Lounge FAB — Navigation 버튼 삭제, Bot 버튼 삭제 | `Lounge.tsx` |
| 03 | FE | AIChat — 전역 Bot 버튼 고정 (모든 페이지 우측 외부) | `AIChat.tsx` |
| 04 | FE | routes.tsx — AIChat `onNavigate` prop 제거 | `routes.tsx` |
| 05 | FE | MapSearch — 위치 버튼 `bottom-[82px]` (하단 고정바 위로) | `MapSearch.tsx` |
| 06 | FE | Home — 프로모 배너 `grid-cols-3` → `grid-cols-2` | `Home.tsx` |
| 07 | FE | VisitCheckIn — 리워드 전체 삭제, QR 삭제 → 사진+GPS 인증 방식으로 재작성 | `VisitCheckIn.tsx` |
| 08 | FE | MapSearch — `#동물병원` 필터 추가 | `MapSearch.tsx` |
| 09 | FE | MapSearch — Kakao Local API 동물병원 검색 연동 (파란 마커, 팝업, 카카오/네이버 외부링크) | `MapSearch.tsx` |
| 10 | FE | MapSearch — `h-screen` → `h-full` (자동 스크롤 제거) | `MapSearch.tsx` |
| 11 | FE | MapSearch — 팝콘배너 `bottom-[60px]` (하단 고정바 위로) | `MapSearch.tsx` |
| 12 | FE | .env — `VITE_KAKAO_REST_API_KEY` 추가 | `.env` |
| 13 | GIT | feat/frontend 브랜치 커밋 + main 머지 | — |

---

### 2026-03-14 (현재 세션)

#### DB / ERD

| # | 작업 내용 | 파일 | 결과 |
|---|---|---|---|
| 14 | ERD — DOG 테이블 → PET 통합 테이블로 교체 | `docs/specs/erd.md` | ✅ |
| 15 | ERD — Mermaid 다이어그램 `USER ||--o{ DOG` → `PET` 수정 | `docs/specs/erd.md` | ✅ |

#### 백엔드 — Entity / Repository

| # | 작업 내용 | 파일 | 결과 |
|---|---|---|---|
| 16 | `Dog.java` 삭제, `DogRepository.java` 삭제 | (삭제) | ✅ |
| 17 | `Pet.java` 신규 생성 — pets 테이블 매핑, PetType(강아지/고양이), PetGender(남아/여아), PetSize, PetActivity 열거형, is_representative 포함 | `user/entity/Pet.java` | ✅ |
| 18 | `PetRepository.java` 신규 생성 — `findByUserUserId`, `findByUserUserIdAndIsRepresentativeTrue` | `user/repository/PetRepository.java` | ✅ |
| 19 | `Place.java` 보강 — `contentId`(upsert 기준), `geom`(PostGIS Point/SRID:4326), `version`(@Version Optimistic Lock), `isVerified`, `upsertFromBatch()` 메서드 추가 | `place/entity/Place.java` | ✅ |
| 20 | `PlaceRepository.java` 보강 — `findByContentId`, `findNearby`(ST_DWithin), `findNearbyByCategory` 네이티브 쿼리 추가 | `place/repository/PlaceRepository.java` | ✅ |

#### 백엔드 — 서비스 / 배치 / 캐시

| # | 작업 내용 | 파일 | 결과 |
|---|---|---|---|
| 21 | `KakaoLocalVerifyService.java` 신규 — 1단계(좌표 정합성), 2단계(영업 여부), Haversine 100m 임계값 | `place/service/KakaoLocalVerifyService.java` | ✅ |
| 22 | `PlaceDataBatchService.java` 신규 — `@Scheduled` 매일 02:00, 서울(areaCode=1)+경기(31) 전체 페이지 수집, 교차검증, content_id 기준 Upsert, Redis 캐시 무효화 | `batch/PlaceDataBatchService.java` | ✅ |
| 23 | `CacheConfig.java` 신규 — `@EnableCaching`, places 1시간 / places:detail 6시간 TTL | `config/CacheConfig.java` | ✅ |
| 24 | `PlaceService.java` — `getPlacesNearby()` 추가 + `@Cacheable`/`@CacheEvict` 적용 | `place/service/PlaceService.java` | ✅ |
| 25 | `PlaceController.java` — `lat`, `lng`, `radius` 파라미터 추가 (위치 있으면 PostGIS, 없으면 키워드 fallback) | `place/controller/PlaceController.java` | ✅ |

#### 백엔드 — 환경 설정

| # | 작업 내용 | 파일 | 결과 |
|---|---|---|---|
| 26 | `backend/.env` 생성 — `PET_TOUR_SERVICE_KEY`, `KAKAO_REST_API_KEY` 등록 | `backend/.env` | ✅ |
| 27 | `application.yml` — `kakao.rest-api-key` 설정 추가 | `application.yml` | ✅ |
| 28 | `MeongNyangBackendApplication.java` — `@EnableScheduling` 추가, TODO 주석 갱신 | `MeongNyangBackendApplication.java` | ✅ |

#### Git — 버전 관리

| # | 작업 내용 | 결과 |
|---|---|---|
| 29 | `feat/frontend` 브랜치 커밋 (20개 파일, 999 insertions) — `26cdd09` | ✅ |
| 30 | `feat/frontend` → `origin` 푸시 | ✅ |
| 31 | `main` 브랜치로 no-ff 머지 (`721e727`) | ✅ |
| 32 | `main` → `origin` 푸시 → GitHub Actions CI/CD 트리거 → AWS EC2 자동 배포 | ✅ |

> 배포 서버: `http://54.180.22.22:8080` | Swagger: `http://54.180.22.22:8080/swagger-ui/index.html`

---

## 3. 데이터 파이프라인 구조 (완성)

```
[한국관광공사 API]
    areaCode=1 (서울) + areaCode=31 (경기)
    매일 새벽 02:00 @Scheduled
           ↓
[PlaceDataBatchService]
    전체 페이지 수집 (numOfRows=100)
           ↓
[KakaoLocalVerifyService]
    1단계: 좌표 정합성 검증 (오차 100m 이내)
    2단계: 영업 여부 확인 (Kakao 검색 결과 존재 여부)
    → 검증 실패: 저장 제외 (폐업/오류 장소)
    → 오차 초과: Kakao 좌표로 자동 보정
           ↓
[PostgreSQL + PostGIS]
    content_id 기준 Upsert
    geom = ST_SetSRID(ST_MakePoint(lng, lat), 4326)
           ↓
[Redis Cache 무효화]
    "places" 캐시 전체 삭제
           ↓
[GET /api/v1/places?lat=&lng=&radius=]
    Cache HIT  → Redis 즉시 반환 (TTL 1시간)
    Cache MISS → ST_DWithin 쿼리 → Redis 저장 후 반환
```

---

## 4. 현재 프론트↔백엔드 연동 상태

| API | 프론트 함수 | 백엔드 | 연동 상태 |
|---|---|---|---|
| 공공API 장소 목록 | `placeApi.getPublicPlaces()` | `GET /api/v1/public-places` | 🟡 Mock 중 (`VITE_USE_MOCK=true`) |
| DB 장소 목록(위치) | `placeApi.getPlaces()` | `GET /api/v1/places?lat=&lng=&radius=` | 🟡 Mock 중 |
| 장소 상세 | `placeApi.getPlace(id)` | `GET /api/v1/places/{id}` | 🟡 Mock 중 |
| 로그인/회원가입 | `useAppStore.login()` | — | ❌ 백엔드 미구현 (팀원A) |
| 반려동물 CRUD | `useAppStore.addPet()` 등 | — | ❌ PetController 미구현 |
| 찜하기 | `useAppStore.toggleWishlist()` | — | ❌ WishlistController 미구현 |
| 피드 | `useFeedStore` | — | ❌ FeedController 미구현 |

> 🟡 실연동 전환 방법: `.env`에서 `VITE_USE_MOCK=false` + 백엔드 서버 실행

---

## 5. 다음 작업 우선순위

### 즉시 진행 가능 (BE — 팀원D 담당)

| 우선순위 | 작업 | 선행 조건 | 상태 |
|:---:|---|---|:---:|
| ★★★ | PetController + PetService 구현 (`/api/v1/pets` CRUD) | Pet Entity/Repository ✅ | ⏳ |
| ★★★ | DB Entity 생성 — REVIEW, WISHLIST, FEED, FEED_LIKE, COMMENT, TALK, TALK_COMMENT | ERD ✅ | ⏳ |
| ★★★ | WishlistController + WishlistService 구현 | WISHLIST Entity | ⏳ |
| ★★☆ | ReviewController + ReviewService 구현 | REVIEW Entity | ⏳ |
| ★★☆ | FeedController + FeedService 구현 (페이지네이션) | FEED Entity | ⏳ |
| ★★☆ | VisitController + VisitService 구현 (GPS 인증) | — | ⏳ |

### 즉시 진행 가능 (FE — 팀원D 담당)

| 우선순위 | 작업 | 파일 | 상태 |
|:---:|---|---|:---:|
| ★★★ | `VITE_USE_MOCK=false` 전환 → 장소 API 실연동 테스트 | `.env`, `mockApi.ts` | ⏳ |
| ★★☆ | List 정렬 기능 (추천순/인기순/최신순) UI + 로직 | `List.tsx` | ⏳ |
| ★★☆ | 지도 실좌표 매핑 (임시 오프셋 → 실 DB lat/lng) | `MapSearch.tsx:90` | ⏳ |
| ★★☆ | 라운지 더미데이터 → 실 API 연동 + 무한스크롤 | `Lounge.tsx`, `useFeedStore.ts` | ⏳ |
| ★☆☆ | 방문기록 실 DB 연동 (`GET /api/v1/visits`) | `VisitCheckIn.tsx` | ⏳ |

### 팀원 작업 완료 후 연동

| 우선순위 | 작업 | 선행 조건 | 상태 |
|:---:|---|---|:---:|
| ★★☆ | JWT 인증 연동 (로그인/회원가입/소셜) | 팀원A SecurityConfig ✅ (main 머지 확인) | ⏳ |
| ★★☆ | AI 산책 가이드 연동 | 팀원C WalkGuideService 완료 후 | ⏳ |
| ★☆☆ | 4단계 교차검증 (Naver Blog + Gemini 의미론적 추출) | 팀원C 완료 후 | ⏳ |
| ★☆☆ | WebSocket(STOMP) 실시간 댓글/DM | FeedController 완료 후 | ⏳ |
| ★☆☆ | 이미지 업로드 → AWS S3 연동 | BE S3 설정 후 | ⏳ |

---

## 6. 파일 구조 변경 이력

```
backend/src/main/java/com/team/meongnyang/
├── MeongNyangBackendApplication.java   ← @EnableScheduling 추가
├── batch/
│   └── PlaceDataBatchService.java      ← 신규 (배치 수집+검증+upsert)
├── config/
│   ├── CacheConfig.java                ← 신규 (Redis TTL 설정)
│   ├── CorsConfig.java
│   └── RestClientConfig.java
├── place/
│   ├── controller/PlaceController.java ← lat/lng/radius 파라미터 추가
│   ├── entity/Place.java               ← contentId, geom, version, isVerified 추가
│   ├── repository/PlaceRepository.java ← ST_DWithin 네이티브 쿼리 추가
│   └── service/
│       ├── KakaoLocalVerifyService.java ← 신규 (교차검증)
│       └── PlaceService.java            ← getPlacesNearby + @Cacheable 추가
└── user/
    ├── entity/
    │   ├── Dog.java                    ← 삭제됨
    │   ├── Pet.java                    ← 신규 (강아지+고양이 통합)
    │   └── User.java
    └── repository/
        ├── DogRepository.java          ← 삭제됨
        ├── PetRepository.java          ← 신규
        └── UserRepository.java

backend/
└── .env                                ← 신규 (PET_TOUR_SERVICE_KEY, KAKAO_REST_API_KEY)

docs/specs/
└── erd.md                              ← DOG → PET 테이블 교체, Mermaid 수정

src/app/pages/
├── MapSearch.tsx   ← #동물병원 필터, Kakao Local API 동물병원, 스크롤 수정
├── VisitCheckIn.tsx ← 리워드/QR 삭제, 사진+GPS 인증으로 재작성
├── Home.tsx        ← 배너 grid-cols-2
└── Lounge.tsx      ← FAB 위치, Navigation 삭제

src/app/components/
└── AIChat.tsx      ← 전역 Bot 버튼 고정
```

---

## 7. 환경 변수 현황

### 프론트 (`.env`)
```
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_USE_MOCK=true          ← 실연동 시 false로 변경
VITE_KAKAO_MAP_API_KEY=ebdedf4d5009cd05a32ac3cb5b1a834b
VITE_KAKAO_REST_API_KEY=8e5bff56b87b37f9bd2aa10433ad5f5e
```

### 백엔드 (`backend/.env`)
```
PET_TOUR_SERVICE_KEY=dcdb89720c4eaa48b8c86bd53b5eb4fdda5aa8257f70d20b8f2116706ec50322
KAKAO_REST_API_KEY=8e5bff56b87b37f9bd2aa10433ad5f5e
```

### 미설정 (추후 필요)
```
GOOGLE_PLACES_API_KEY   ← 3단계 교차검증 (현재 제외)
NAVER_CLIENT_ID         ← 4단계 의미론적 추출 (팀원C 연동 후)
NAVER_CLIENT_SECRET     ← 위 동일
GEMINI_API_KEY          ← AI 연동 (팀원C 담당)
JWT_SECRET              ← 인증 (팀원A 담당)
```

---

## 8. 배포 현황

| 환경 | URL | 상태 |
|---|---|:---:|
| 로컬 FE | `http://localhost:5173` | ✅ |
| 로컬 BE | `http://localhost:8080` | ✅ |
| AWS EC2 | `http://54.180.22.22:8080` | ✅ (팀원A 배포, GitHub Actions CI/CD) |
| Swagger | `http://54.180.22.22:8080/swagger-ui/index.html` | ✅ |

> CI/CD: `main` 브랜치 push 시 GitHub Actions → Docker 빌드 → EC2 자동 배포

---

## 9. 세션 로그 (역순)

| 날짜 | 세션 | 주요 작업 | 커밋 |
|---|---|---|---|
| 2026-03-14 | 세션 2 | feat/frontend 커밋 · main 머지 · origin 푸시 · 배포 확인 | `721e727` (main) |
| 2026-03-14 | 세션 1 | DOG→PET 통합, 공공데이터 배치 파이프라인, PostGIS, Redis 캐싱, Kakao 교차검증 | `26cdd09` |
| 2026-03-13 | 세션 1 | Lounge FAB, AIChat 전역, MapSearch 동물병원, VisitCheckIn GPS 인증 | `b356cce` |
