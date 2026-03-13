# 팀원D 작업 현황 및 리스트

> 최종 업데이트: 2026-03-11

---

## 완료된 작업

| 구분 | 항목 |
|------|------|
| UI/UX | 전체 17개 페이지 구현 완료 |
| 디자인 시스템 | 컬러, 타이포그래피, 컴포넌트 패턴 확정 |
| 지도 | 카카오맵 SDK 연동 완료 (vite-plugin-html-env 문법 수정) |
| Mock API | axios-mock-adapter 인터셉터 연동 |
| 라우팅 | React Router v7 전체 구조 완성 |
| 상태관리 | Zustand 스토어 3개 (useAppStore, useFeedStore, useFriendStore) |

---

## 고도화 필수 작업 (백엔드 연동)

### 인증/회원
- [ ] `POST /api/auth/login` — JWT 토큰 연동
- [ ] `POST /api/auth/signup` — 회원가입
- [ ] `GET /api/oauth2/{provider}` — Google/Kakao 소셜 로그인
- [ ] `PUT /api/users/profile` — 회원정보 수정
- [ ] `PUT /api/auth/password` — 비밀번호 변경
- [ ] `POST /api/auth/password-reset` — 비밀번호 초기화
- [ ] `DELETE /api/auth/account` — 계정 삭제

### 반려동물
- [ ] `POST /api/pets` — 반려동물 등록
- [ ] `PUT /api/pets/{id}` — 반려동물 수정
- [ ] `DELETE /api/pets/{id}` — 반려동물 삭제

### 장소
- [ ] `GET /api/places` — 실제 백엔드 데이터 연동 (Mock 제거)
- [ ] `POST/DELETE /api/wishlists/{id}` — 찜 서버 저장
- [ ] `POST /api/saved-routes` — AI 경로 저장

### 커뮤니티 (Lounge)
- [ ] `GET /api/posts` — 피드 목록 조회
- [ ] `POST /api/posts` — 게시글 작성 (AWS S3 이미지 업로드 포함)
- [ ] `PUT /api/posts/{id}` — 게시글 수정
- [ ] `DELETE /api/admin/posts/{id}` — 게시글 삭제
- [ ] `POST /api/posts/{id}/comments` — 댓글 작성 (WebSocket 예정)
- [ ] `POST /api/posts/{id}/likes` — 좋아요 토글
- [ ] `POST /api/dms` — DM 전송 (WebSocket 예정)
- [ ] `PATCH /api/dms/{id}/read` — DM 읽음 처리
- [ ] `POST /api/posts/{id}/report` — 게시글 신고

### 관리자
- [ ] `PATCH /api/admin/posts/{id}/hide` — 게시글 숨김
- [ ] 관리자 권한 체크 (Spring Security 연동 후 @PreAuthorize 적용)

---

## 기능 고도화 작업

| 항목 | 파일 | 내용 |
|------|------|------|
| 지도 실좌표 매핑 | `MapSearch.tsx` | 임시 오프셋 좌표 → 실제 DB lat/lng 사용 |
| 날씨 API 연동 | `routes.tsx`, `AIWalkGuide.tsx` | 하드코딩 `23°C` → 실제 날씨 API |
| 이미지 업로드 | `Lounge.tsx` | Sample 이미지 → AWS S3 연동 |
| 리뷰 실데이터 | `Detail.tsx` | Mock 리뷰 → 실제 DB 리뷰 |
| WebSocket | `Lounge.tsx` | 댓글/DM 실시간 동기화 (STOMP) |
| 온보딩 서버 저장 | `Onboarding.tsx` | localStorage 플래그 → 서버 저장 |

---

## API 명세 확정 작업 (팀원D 총괄)

> 팀원B, C 개발 기준이 되는 Request/Response 규격 노션 문서화

- [ ] 장소 검색 API 명세 (PostGIS 좌표 포함)
- [ ] 반려동물 등록 API 명세
- [ ] 피드/댓글/DM API 명세
- [ ] AI 산책 가이드 API 명세 (Spring AI 연동)
- [ ] 건강 알림 API 명세 (카카오 알림톡)

---

## 전체 진행률

| 영역 | 진행률 |
|------|--------|
| UI/UX 구현 | 100% |
| 상태관리 (Zustand) | 100% |
| 라우팅 | 100% |
| Mock API | 100% |
| 백엔드 API 연동 | 0% |
| 실시간 기능 (WebSocket) | 0% |
| 파일 업로드 (S3) | 0% |
| **전체** | **약 50%** |
