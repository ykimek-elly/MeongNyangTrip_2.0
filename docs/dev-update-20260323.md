# 멍냥트립 2.0 — 개발 업데이트 노트 (2026-03-23)

> 작성: 팀원D | 브랜치: feat/frontend → main 머지

---

## 📋 요약

관리자 대시보드 **신규 장소 수동 등록** 기능 추가.
BE에 신규 엔드포인트 3개 추가, 기존 서비스 클래스 의존성 확장.

---

## 🔴 백엔드 변경사항 (팀원 확인 필수)

### 1. `PlaceAdminController` — 엔드포인트 2개 추가

```
POST /api/v1/admin/places/analyze   ← NEW
POST /api/v1/admin/places           ← NEW
```

#### `POST /api/v1/admin/places/analyze`
DB 저장 없이 Naver 블로그 분석 + Kakao 주소→좌표 변환 + aiRating 미리 계산.

**Request Body:**
```json
{
  "title": "멍냥카페 성수점",
  "address": "서울 성동구 성수동1가 13-19",
  "phone": "02-1234-5678",       // optional
  "homepage": "https://...",     // optional
  "imageUrl": "https://...",     // optional
  "description": "설명..."       // optional
}
```

**Response:**
```json
{
  "lat": 37.54423,
  "lng": 127.05578,
  "geocodeSuccess": true,
  "aiRating": 3.2,
  "blogCount": 47,
  "blogPositiveTags": "분위기좋음,반려동물환영",
  "blogNegativeTags": null,
  "naverVerified": true
}
```

---

#### `POST /api/v1/admin/places`
신규 장소를 ACTIVE 상태로 DB에 직접 등록.
`contentId`는 `ADMIN-{timestamp}` 형식으로 자동 부여.

**Request Body:**
```json
{
  "title": "멍냥카페 성수점",      // required
  "category": "DINING",           // required: PLACE | STAY | DINING
  "address": "서울 성동구 ...",    // required
  "lat": 37.54423,                // required
  "lng": 127.05578,               // required
  "phone": "02-1234-5678",        // optional
  "homepage": "https://...",      // optional
  "imageUrl": "https://...",      // optional
  "description": "설명...",       // optional
  "aiRating": 3.2                 // optional (analyze 결과값 전달 권장)
}
```

**Response:** `PendingPlaceDto` (기존 타입 그대로)

---

### 2. `PlaceAdminService` — 의존성 추가 및 메서드 2개 추가

**추가된 의존성 주입:**
```java
// @RequiredArgsConstructor 자동 생성자에 추가됨
private final NaverLocalVerifyService naverLocalVerifyService;
private final KakaoLocalVerifyService kakaoLocalVerifyService;
```

> ⚠️ **기존 생성자 주입 코드가 있다면 충돌 가능** — `@RequiredArgsConstructor` 방식이므로 별도 수동 생성자 없으면 문제없음.

**추가된 메서드:**
- `analyzePlacePreview(...)` — DB 저장 없이 분석만 수행, `Map<String, Object>` 반환
- `createPlace(..., Double aiRating)` — 신규 장소 ACTIVE 저장

**기존 `createPlace` 시그니처 변경:**
```java
// 변경 전 (없던 메서드)
// 변경 후
public PendingPlaceDto createPlace(
    String title, String category, String address,
    Double lat, Double lng,
    String phone, String homepage,
    String imageUrl, String description,
    Double aiRating   // ← 추가된 파라미터
)
```

---

### 3. `KakaoLocalVerifyService` — 메서드 1개 추가

```java
/** 주소 → 좌표 변환 (Kakao Address Search API) */
public double[] geocodeAddress(String address)
// 성공 시 [lat, lng], 실패 시 null 반환
```

사용 API: `https://dapi.kakao.com/v2/local/search/address.json`
기존 `kakao.rest-api-key` 환경변수 그대로 사용 (추가 설정 불필요).

---

### 4. `Place.java` — `description` 필드 Builder 지원 확인

`description` 필드가 Builder에서 사용됨.
기존 `@Builder` 설정에 포함되어 있으므로 별도 수정 없음.

---

## 🟡 프론트엔드 변경사항

### AdminDashboard — 장소검토 탭 "신규추가" 서브탭 추가

**3단계 플로우:**

```
① 기본정보 입력
   장소명(필수) · 카테고리(필수) · 주소(필수) · 전화 · 홈페이지 · 이미지 · 설명
        ↓ "AI 보강 분석 실행" 버튼
② AI 분석 결과 미리보기 (DB 저장 없음)
   Kakao 주소→좌표 자동변환 · Naver 블로그 분석 · aiRating 계산 결과 표시
   좌표 변환 실패 시 수동 입력 폼 노출
        ↓ "DB 등록 확정" 버튼
③ ACTIVE 상태로 DB 저장 완료
```

### adminApi.ts — 함수 2개 추가

```typescript
adminApi.analyzePlacePreview(data)  // POST /admin/places/analyze
adminApi.createPlace(data)          // POST /admin/places
```

### mockApi.ts — Mock 핸들러 2개 추가

`VITE_USE_MOCK=true` 환경에서도 신규추가 탭 동작 가능하도록 추가.

---

### AdminDashboard — 배치 탭 "실행 이력" 기능 추가

배치 수동 실행 시 결과를 **localStorage**에 자동 기록. 페이지를 닫아도 최대 50건 유지.

**저장 항목:**
| 필드 | 내용 |
|---|---|
| `jobLabel` | 실행한 배치 이름 |
| `startedAt` | 실행 시작 시각 (ISO) |
| `completedAt` | 완료 시각 (ISO) |
| `durationSec` | 소요 시간 (초) |
| `status` | `success` / `error` |
| `result` | 서버 메시지 또는 오류 메시지 |

**UI:**
- 배치 탭 하단 "실행 이력" 섹션 (토글, 건수 배지)
- 전체 삭제 버튼으로 이력 초기화 가능

> 별도 BE 연동 없음 — 순수 FE localStorage 저장.

---

## ⚙️ 환경변수 — 추가 필요 없음

이번 변경은 기존 환경변수를 그대로 사용:
- `kakao.rest-api-key` — Kakao Address API 추가 사용 (기존 키 그대로)
- `naver.local.client-id` / `naver.local.client-secret` — 기존 그대로

---

## 🔁 DB 변경사항

스키마 변경 없음.
신규 등록 장소는 `contentId = 'ADMIN-{timestamp}'` 형식으로 구분 가능.

---

## ✅ 테스트 방법

```bash
# 1. 분석 엔드포인트 테스트
curl -X POST http://localhost:8080/api/v1/admin/places/analyze \
  -H "Content-Type: application/json" \
  -d '{"title":"테스트카페","address":"서울 성동구 성수동"}'

# 2. 등록 엔드포인트 테스트
curl -X POST http://localhost:8080/api/v1/admin/places \
  -H "Content-Type: application/json" \
  -d '{"title":"테스트카페","category":"DINING","address":"서울 성동구 성수동","lat":37.544,"lng":127.055}'

# 3. Swagger UI
http://54.180.22.22:8080/swagger-ui/index.html
```

---

## 📌 참고

- 신규 등록 장소는 `recalculate-ai-rating-all` 배치 실행 시 aiRating 갱신됨
- `analyze` 엔드포인트는 Naver API 미설정 시에도 동작 (blogCount=0, naverVerified=false 반환)
- Kakao 주소 변환 실패 시 FE에서 수동 좌표 입력 폼 노출
