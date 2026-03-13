# 팀원 B+D 작업 현황 및 리스트

> 최종 업데이트: 2026-03-13
> 2026-03-13 회의 결정 — 팀원B(코어 데이터/BE) + 팀원D(FE & 인터페이스) 업무 통합 확정
> B = 백엔드 API 구현 / D = 프론트엔드 구현 및 연동

---

## 완료된 작업

| 구분 | 항목 | 담당 | 비고 |
|------|------|------|------|
| UI/UX | 전체 17개 페이지 구현 완료 | D | Home, Login, Signup, FindId, FindPassword, Onboarding, MapSearch, Detail, List, Lounge, MyPage, AIWalkGuide, SeniorPetDashboard, VisitCheckIn, EditProfile, AdminDashboard, NotFound |
| 디자인 시스템 | 컬러, 타이포그래피, 컴포넌트 패턴 확정 | D | CSS 변수 기반 Tailwind |
| 지도 | 카카오맵 SDK 연동 완료 | D | vite-plugin-html-env 문법 수정, 포트 5173 고정 |
| Mock API | axios 인터셉터 연동 | D | `/places`, `/public-places` 가로채기 |
| 라우팅 | React Router v7 전체 구조 완성 | D | Data Mode |
| 상태관리 | Zustand 스토어 3개 | D | useAppStore, useFeedStore, useFriendStore |
| JWT 인터셉터 구조 | axios.ts — Bearer 토큰 자동 첨부 / 401 → 로그인 리다이렉트 | D | 토큰 실발급은 백엔드 연동 후 |
| 커스텀 훅 | useGeolocation.ts — 현재 위치 + 카카오 역지오코딩 | D | |
| 컴포넌트 | ShareSheet, PetProfileForm, AIChat, BottomNav 등 | D | |
| 백엔드 코어 세팅 | ERD 설계, AOP, 공통 예외처리, PostGIS 연동 | B | |

---

## 1단계 — 즉시 처리 (프론트 코드 변경) `담당: D`

> 백엔드 대기 없이 즉시 진행 가능한 항목

| 순서 | 업무 | 파일 | 상태 |
|------|------|------|------|
| 1 | **펫케어 전체 보류** — `senior-pet` 라우트 비활성 처리 (컴포넌트 삭제 X, 진입만 차단) | `routes.tsx` | - [ ] |
| 2 | **VisitCheckIn QR 제거** — `QrCode` 버튼·스캔 모달·`showScanModal` 로직 삭제 | `VisitCheckIn.tsx` | - [ ] |
| 3 | **VisitCheckIn 포인트 기능 제거** — `REWARDS`·`totalPoints`·포인트 탭/UI 전체 삭제, 리워드 탭 제거 | `VisitCheckIn.tsx` | - [ ] |
| 4 | **List 정렬 기능 추가** — 추천순/인기순/최신순 정렬 드롭다운 UI + 로직 구현 (`rating`, `reviewCount`, `createdAt` 기준) | `List.tsx` | - [ ] |

---

## 2단계 — 백엔드 API 구현 `담당: B`

### 장소

| 순서 | API | 설명 | 상태 |
|------|-----|------|------|
| 1 | `GET /api/places?sort=recommended\|popular\|latest&page=&size=` | 장소 목록 조회 + 정렬 파라미터 지원 | - [ ] |
| 2 | `GET /api/places?lat=&lng=&radius=` | 위치기반 근처 장소 조회 (VisitCheckIn 방문인증용) | - [ ] |
| 3 | `GET /api/places/{id}` | 장소 상세 조회 | - [ ] |
| 4 | `POST /api/wishlists/{id}` | 찜 등록 | - [ ] |
| 5 | `DELETE /api/wishlists/{id}` | 찜 삭제 | - [ ] |
| 6 | `POST /api/saved-routes` | AI 추천 경로 저장 | - [ ] |
| 7 | `DELETE /api/saved-routes/{id}` | 저장 경로 삭제 | - [ ] |

### 커뮤니티 (Lounge)

| 순서 | API | 설명 | 상태 |
|------|-----|------|------|
| 1 | `GET /api/posts?page=0&size=10` | 피드 목록 조회 (페이지네이션) | - [ ] |
| 2 | `POST /api/posts` | 게시글 작성 (이미지 URL 포함) | - [ ] |
| 3 | `PUT /api/posts/{id}` | 게시글 수정 | - [ ] |
| 4 | `DELETE /api/posts/{id}` | 게시글 삭제 | - [ ] |
| 5 | `POST /api/posts/{id}/likes` | 좋아요 토글 | - [ ] |
| 6 | `POST /api/posts/{id}/comments` | 댓글 작성 | - [ ] |
| 7 | `POST /api/dms` | DM 전송 | - [ ] |
| 8 | `PATCH /api/dms/{id}/read` | DM 읽음 처리 | - [ ] |
| 9 | `POST /api/posts/{id}/report` | 게시글 신고 | - [ ] |
| 10 | `PATCH /api/admin/posts/{id}/hide` | 관리자 게시글 숨김 | - [ ] |

### 방문인증

| 순서 | API | 설명 | 상태 |
|------|-----|------|------|
| 1 | `POST /api/visits` | 방문 체크인 (위치 좌표 기반 인증) | - [ ] |
| 2 | `GET /api/visits` | 방문기록 목록 조회 | - [ ] |

### 반려동물

| 순서 | API | 설명 | 상태 |
|------|-----|------|------|
| 1 | `POST /api/pets` | 반려동물 등록 | - [ ] |
| 2 | `PUT /api/pets/{id}` | 반려동물 수정 | - [ ] |
| 3 | `DELETE /api/pets/{id}` | 반려동물 삭제 | - [ ] |

---

## 3단계 — 프론트 DB 연동 `담당: D` (백엔드 완성 후 순서대로 진행)

### 우선순위 높음 (회의 결정 반영)

| 순서 | 업무 | 연동 위치 | 상태 |
|------|------|-----------|------|
| 1 | **라운지 더미데이터 제거 + 실 API 연동** — `INITIAL_POSTS` 완전 삭제, `GET /api/posts` 연결 | `useFeedStore.ts:42`, `useFeedStore.ts:132` | - [ ] |
| 2 | **무한 스크롤 구현** — `IntersectionObserver` + 페이지네이션 (`?page&size`) | `Lounge.tsx` | - [ ] |
| 3 | **List 정렬 API 연동** — 정렬 파라미터를 실 API에 전달 | `List.tsx`, `useAppStore.ts` | - [ ] |
| 4 | **방문기록 실 DB 연동** — `VISIT_HISTORY` Mock 제거, `GET /api/visits` 연결 | `VisitCheckIn.tsx` | - [ ] |
| 5 | **지도 실좌표 매핑** — id 기반 임시 오프셋 좌표 → 실 DB `lat/lng` | `MapSearch.tsx:90` | - [ ] |

### 인증/회원

| 순서 | 업무 | 연동 위치 | 상태 |
|------|------|-----------|------|
| 1 | `POST /api/auth/login` — JWT 토큰 저장 | `useAppStore.ts:75` | - [ ] |
| 2 | `POST /api/auth/signup` — 회원가입 | `Signup.tsx:23` | - [ ] |
| 3 | `GET /api/oauth2/{provider}` — 소셜 로그인 | `Signup.tsx:30` | - [ ] |
| 4 | `PUT /api/users/profile` — 회원정보 수정 | `EditProfile.tsx:39` | - [ ] |
| 5 | `PUT /api/auth/password` — 비밀번호 변경 | `EditProfile.tsx:51` | - [ ] |
| 6 | `DELETE /api/auth/account` — 계정 삭제 | `EditProfile.tsx:274` | - [ ] |

### 장소 / 찜 / 경로

| 순서 | 업무 | 연동 위치 | 상태 |
|------|------|-----------|------|
| 1 | `GET /api/places` — Mock 인터셉터 제거 후 실 DB 연동 | `mockApi.ts`, `useAppStore.ts` | - [ ] |
| 2 | `POST/DELETE /api/wishlists/{id}` — 찜 서버 저장 | `useAppStore.ts:105` | - [ ] |
| 3 | `POST /api/saved-routes` — 경로 저장 | `useAppStore.ts:118` | - [ ] |

### 커뮤니티 / 피드

| 순서 | 업무 | 연동 위치 | 상태 |
|------|------|-----------|------|
| 1 | 게시글 좋아요/댓글/신고 API 연동 | `useFeedStore.ts` | - [ ] |
| 2 | DM 전송 / 읽음 처리 연동 | `useFeedStore.ts:173` | - [ ] |
| 3 | 관리자 게시글 숨김/삭제 연동 | `useFeedStore.ts:219` | - [ ] |
| 4 | 이미지 업로드 → AWS S3 연동 (샘플 이미지 대체) | `Lounge.tsx` | - [ ] |
| 5 | WebSocket(STOMP) 실시간 동기화 — 댓글/DM | `useFeedStore.ts`, `useFriendStore.ts` | - [ ] |

### 기타

| 순서 | 업무 | 연동 위치 | 상태 |
|------|------|-----------|------|
| 1 | 반려동물 등록/수정/삭제 API 연동 | `useAppStore.ts:94` | - [ ] |
| 2 | 온보딩 완료 플래그 서버 저장 | `useAppStore.ts:91` | - [ ] |
| 3 | 친구 목록 `GET /api/friends` 연동 (정적 Mock 제거) | `useFriendStore.ts` | - [ ] |
| 4 | 리뷰 실데이터 연동 (Mock 리뷰 제거) | `Detail.tsx` | - [ ] |

---

## API 명세 확정 작업 `담당: B+D 공동`

> 팀원A, C 개발 기준이 되는 Request/Response 규격 노션 문서화

- [ ] 장소 검색 API 명세 (PostGIS 좌표, 정렬 파라미터 포함)
- [ ] 반려동물 등록 API 명세
- [ ] 피드/댓글/DM API 명세
- [ ] 방문인증 API 명세 (위치 좌표 기반)
- [ ] AI 산책 가이드 API 명세 (팀원C Spring AI 연동 기준)

---

## 전체 진행률 (2026-03-13 기준)

| 영역 | 진행률 | 담당 | 비고 |
|------|--------|------|------|
| UI/UX 구현 (17개 페이지) | 100% | D | 완료 |
| 라우팅 / 상태관리 / Mock API | 100% | D | 완료 |
| JWT 인터셉터 구조 | 100% | D | 토큰 실발급은 A 대기 |
| **펫케어 시스템** | **보류** | — | 2026-03-13 회의 결정 |
| 1단계: 프론트 즉시 처리 (VisitCheckIn 정리, List 정렬) | 0% | D | 진행 예정 |
| 2단계: 백엔드 API 구현 | 0% | B | 진행 예정 |
| 3단계: 프론트 DB 연동 | 0% | D | 2단계 완성 후 |
| **라운지 실 DB 연동** | 0% | B+D | 우선순위 최상 |
| WebSocket / S3 | 0% | B+D | 후순위 |
| **전체** | **약 45%** | — | 보류 항목 반영 |

> 각 스토어 함수에 `TODO: [DB 연동]` 주석으로 연동 위치 명시 — API 완성 즉시 연동 가능한 구조
