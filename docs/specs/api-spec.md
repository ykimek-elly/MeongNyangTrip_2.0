# API 명세서

## 1. 문서 정보

| 항목 | 내용 |
|-----|-----|
| 프로젝트 | 멍냥트립 2.0 (MeongNyangTrip 2.0) |
| 문서 버전 | v1.0 |
| 작성일 | 2026-03-11 |
| 작성자 | 김은경  |

---

# 2. 외부 API 목록

## 2.1 지도 및 위치 코어

| API ID | API 이름 | 주요 용도 | 참고 링크 |
|--------|---------|----------|----------|
| MAP-001 | Kakao Map SDK (Web) | 장소 마커 표시 및 사용자 위치 기반 지도 이동 | [링크](https://apis.map.kakao.com/web/) |
| MAP-002 | Kakao Local API | 텍스트 주소의 위경도 좌표 변환 및 PostGIS 적재 | [링크](https://developers.kakao.com/docs/latest/ko/local/common) |

## 2.2 AI 및 실시간 날씨

| API ID | API 이름 | 주요 용도 | 참고 링크 |
|--------|---------|----------|----------|
| AI-001 | Google Gemini API (Spring AI 연동) | RAG 조합을 통한 맞춤형 산책 조언 생성 | [링크](https://aistudio.google.com/) |
| WTH-001 | 기상청 단기예보 | 실시간 날씨 정보 수집 | [링크](https://data.kma.go.kr/data/rmt/rmtList.do?code=420&pgmNo=572) |

## 2.3 인증

| API ID | API 이름 | 주요 용도 | 참고 링크 |
|--------|---------|----------|----------|
| AUTH-001 | Google Identity (OAuth 2.0) | 구글 계정 기반 간편 로그인 | [링크](https://developers.google.com/identity/protocols/oauth2) |
| AUTH-002 | Kakao Login API (OAuth 2.0) | 카카오 계정 기반 간편 로그인 | [링크](https://developers.kakao.com/docs/latest/ko/kakaologin/common) |
| MSG-001 | Kakao AlimTalk API (카카오 알림톡) | 카카오톡 메시지로 실시간 알림 전송 | [링크](https://developers.kakao.com/docs/latest/ko/message/common) |

## 2.4 공공데이터포털 & 로컬데이터

| API ID | API 이름 | 주요 용도 | 참고 링크 |
|--------|---------|----------|----------|
| DATA-001 | 행정안전부 동물병원 | 원천 데이터 (동물병원 목록) | [링크](https://www.data.go.kr/data/15045050/fileData.do) |
| DATA-002 | 한국관광공사 반려동물 동반여행 서비스 | 속성 데이터 보강 (동반 가능 시설) | [링크](https://www.data.go.kr/iim/api/selectAPIAcountView.do) |

## 2.5 속성 데이터 보강

| API ID | API 이름 | 주요 용도 | 참고 링크 |
|--------|---------|----------|----------|
| ENR-001 | Google Places API | 장소 상세 속성(allowsDogs 등) 조회 | [링크](https://console.cloud.google.com/google/maps-apis/overview) |
| ENR-002 | Naver Blog Search API | 블로그 리뷰 기반의 의미론적 특징 추출 | [링크](https://developers.naver.com/docs/serviceapi/search/blog/blog.md) |

---

# 3. 내부 API 목록 (REST API)

> 프론트엔드(React) ↔ 백엔드(Spring Boot 3.5.11) 간 통신 API

### 공통 응답 규격

```json
{
  "status": 200,
  "message": "요청 성공 메시지",
  "data": { ... }
}
```

| API ID | API 이름 | Method | Endpoint | 설명 |
|--------|---------|--------|----------|------|
| PET-001 | 반려동물 등록 | POST | `/api/v1/pets` | 온보딩 시 반려동물 정보 등록 |
| PET-002 | 반려동물 조회 | GET | `/api/v1/pets/{petId}` | 등록된 반려동물 정보 조회 |
| PET-003 | 반려동물 수정 | PUT | `/api/v1/pets/{petId}` | 반려동물 정보 수정 |
| PLACE-001 | 장소 목록 조회 | GET | `/api/v1/places` | PostGIS 기반 근거리 장소 검색 |
| PLACE-002 | 장소 상세 조회 | GET | `/api/v1/places/{placeId}` | 장소 상세 정보 + 리뷰 |
| PLACE-003 | 방문 인증 | POST | `/api/v1/places/{placeId}/check-in` | GPS 기반 방문 인증 (반경 50m) |
| PLACE-004 | 찜하기 토글 | POST | `/api/v1/places/{placeId}/wish` | 장소 찜 추가/삭제 |
| AUTH-010 | 로그인 | POST | `/api/v1/auth/login` | JWT 토큰 발급 |
| AUTH-011 | 회원가입 | POST | `/api/v1/auth/signup` | 이메일/소셜 회원가입 |
| AUTH-012 | 프로필 수정 | PUT | `/api/v1/auth/profile` | 닉네임/이메일/비밀번호 변경 |
| AI-010 | AI 산책 가이드 | POST | `/api/v1/ai/walk-guide` | 날씨+펫 정보 기반 맞춤 산책 추천 |
| AI-011 | AI 챗봇 | POST | `/api/v1/ai/chat` | AI 멍냥 플래너 대화 |
| FEED-001 | 피드 목록 | GET | `/api/v1/feeds` | 라운지 피드 목록 조회 |
| FEED-002 | 피드 작성 | POST | `/api/v1/feeds` | 라운지 피드 글쓰기 |
| FEED-003 | 좋아요 토글 | POST | `/api/v1/feeds/{feedId}/like` | 피드 좋아요 추가/삭제 |
| FEED-004 | 댓글 작성 | POST | `/api/v1/feeds/{feedId}/comments` | 피드 댓글 작성 |
| CARE-001 | 건강 데이터 조회 | GET | `/api/v1/pets/{petId}/health` | 시니어 펫 건강 지표 조회 |
| CARE-002 | 체크리스트 토글 | PUT | `/api/v1/pets/{petId}/checklist` | 일일 케어 체크리스트 업데이트 |

---

# 4. API 상세 명세

---

## PET-001 / 반려동물 등록

### 기본 정보

| 항목 | 내용 |
|-----|-----|
| Method | POST |
| Endpoint | `/api/v1/pets` |
| 인증 | Bearer Token (JWT) |
| 설명 | 온보딩 시 반려동물 정보를 등록한다. 나이, 품종, 활동량, 크기 등을 수집하여 AI 추천에 활용. |

### Request Parameter

| Name | Type | Required | Description |
|-----|-----|-----|-----|
| name | String | ✅ | 반려동물 이름 |
| type | String | ✅ | 종류 (DOG / CAT) |
| breed | String | ✅ | 품종 (예: 푸들, 코리안숏헤어) |
| gender | String | ✅ | 성별 (남아 / 여아) |
| age | Integer | ✅ | 나이 (세) |
| size | String | ✅ | 크기 (SMALL / MEDIUM / LARGE) |
| activity | String | ✅ | 활동량 (LOW / NORMAL / HIGH) |
| weight | Float | ❌ | 체중 (kg) — 시니어 펫 케어용 |

### Example Request

```json
{
  "name": "초코",
  "type": "DOG",
  "breed": "푸들",
  "gender": "남아",
  "age": 3,
  "size": "SMALL",
  "activity": "HIGH",
  "weight": 4.2
}
```

### Example Response (성공)

```json
{
  "status": 200,
  "message": "반려동물 등록 성공",
  "data": {
    "petId": 1,
    "name": "초코",
    "type": "DOG",
    "breed": "푸들",
    "gender": "남아",
    "age": 3,
    "size": "SMALL",
    "activity": "HIGH",
    "weight": 4.2
  }
}
```

---

## PLACE-001 / 장소 목록 조회

### 기본 정보

| 항목 | 내용 |
|-----|-----|
| Method | GET |
| Endpoint | `/api/v1/places` |
| 인증 | 선택 (비로그인 시에도 기본 목록 제공) |
| 설명 | 사용자 위치(위경도) 기반 반경 내 반려동물 동반 가능 장소를 조회한다. PostGIS `ST_DWithin` 활용. |

### Request Parameter (Query String)

| Name | Type | Required | Description |
|-----|-----|-----|-----|
| lat | Double | ✅ | 사용자 위도 (Latitude) |
| lng | Double | ✅ | 사용자 경도 (Longitude) |
| radius | Integer | ❌ | 검색 반경 (m), 기본값: 5000 |
| category | String | ❌ | 카테고리 필터 (PLACE / STAY / DINING / ALL) |
| sort | String | ❌ | 정렬 기준 (DISTANCE / RATING / REVIEW) |
| page | Integer | ❌ | 페이지 번호, 기본값: 0 |
| size | Integer | ❌ | 페이지 크기, 기본값: 20 |

### Example Request

```
GET /api/v1/places?lat=37.5665&lng=126.9780&radius=3000&category=STAY&sort=RATING
```

### Example Response (성공)

```json
{
  "status": 200,
  "message": "장소 목록 조회 성공",
  "data": {
    "content": [
      {
        "id": 1,
        "title": "서울숲 반려동물 구역",
        "category": "PLACE",
        "address": "서울특별시 성동구 뚝섬로 273",
        "lat": 37.5445,
        "lng": 127.0374,
        "rating": 4.9,
        "reviewCount": 128,
        "distance": 1200,
        "thumbnailUrl": "https://...",
        "tags": ["대형견", "주차가능", "야외"]
      }
    ],
    "totalPages": 5,
    "totalElements": 87
  }
}
```

---

## PLACE-003 / 방문 인증

### 기본 정보

| 항목 | 내용 |
|-----|-----|
| Method | POST |
| Endpoint | `/api/v1/places/{placeId}/check-in` |
| 인증 | Bearer Token (JWT) — 필수 |
| 설명 | 사용자의 현재 GPS 좌표와 장소 좌표를 대조하여 반경 50m 이내일 때 방문 인증을 처리한다. |

### Request Parameter

| Name | Type | Required | Description |
|-----|-----|-----|-----|
| placeId | Long (Path) | ✅ | 인증할 장소 ID |
| lat | Double | ✅ | 사용자 현재 위도 |
| lng | Double | ✅ | 사용자 현재 경도 |
| photoUrl | String | ❌ | 인증 사진 URL (S3 업로드 후) |

### Example Request

```json
{
  "lat": 37.5445,
  "lng": 127.0374,
  "photoUrl": "https://s3.amazonaws.com/meongnyang/checkin/photo123.jpg"
}
```

### Example Response (성공)

```json
{
  "status": 200,
  "message": "방문 인증 완료",
  "data": {
    "checkInId": 42,
    "placeId": 1,
    "verified": true,
    "badge": "VERIFIED_VISITOR",
    "checkedInAt": "2026-03-11T09:15:00"
  }
}
```

### Example Response (실패 — 거리 초과)

```json
{
  "status": 400,
  "message": "현재 위치가 장소와 50m 이상 떨어져 있습니다.",
  "data": null
}
```

---

## AI-010 / AI 산책 가이드

### 기본 정보

| 항목 | 내용 |
|-----|-----|
| Method | POST |
| Endpoint | `/api/v1/ai/walk-guide` |
| 인증 | 선택 (비로그인 시 수동 입력 데이터 사용) |
| 설명 | 반려동물 프로필 + 실시간 날씨 데이터를 Gemini API에 전달하여 맞춤형 산책 가이드를 생성한다. |

### Request Parameter

| Name | Type | Required | Description |
|-----|-----|-----|-----|
| petId | Long | ❌ | 반려동물 ID (로그인 시) |
| size | String | ❌ | 수동 입력 크기 (비로그인 시, SMALL/MEDIUM/LARGE) |
| activity | String | ❌ | 수동 입력 활동량 (비로그인 시, LOW/NORMAL/HIGH) |
| lat | Double | ✅ | 현재 위도 |
| lng | Double | ✅ | 현재 경도 |

### Example Request

```json
{
  "petId": 1,
  "lat": 37.5665,
  "lng": 126.9780
}
```

### Example Response (성공)

```json
{
  "status": 200,
  "message": "AI 산책 가이드 생성 완료",
  "data": {
    "weather": {
      "temperature": 23,
      "condition": "맑음",
      "humidity": 45,
      "walkIndex": 92
    },
    "aiComment": "오늘은 초코에게 딱 좋은 산책 날씨예요! 기온이 23°C로 적당하고 습도도 낮아서 30~40분 산책을 추천드려요.",
    "recommendedRoutes": [
      {
        "name": "서울숲 산책로",
        "distance": "2.3km",
        "estimatedTime": "35분",
        "difficulty": "EASY"
      }
    ],
    "tips": [
      "물을 충분히 챙겨주세요",
      "아스팔트보다 잔디밭 위주로 산책하세요"
    ]
  }
}
```

---

## AUTH-010 / 로그인

### 기본 정보

| 항목 | 내용 |
|-----|-----|
| Method | POST |
| Endpoint | `/api/v1/auth/login` |
| 인증 | 없음 |
| 설명 | 이메일/비밀번호 로그인 후 JWT 토큰을 발급한다. |

### Request Parameter

| Name | Type | Required | Description |
|-----|-----|-----|-----|
| email | String | ✅ | 사용자 이메일 |
| password | String | ✅ | 비밀번호 |

### Example Request

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

### Example Response (성공)

```json
{
  "status": 200,
  "message": "로그인 성공",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "username": "김은경",
      "email": "user@example.com"
    }
  }
}
```

### Example Response (실패)

```json
{
  "status": 401,
  "message": "이메일 또는 비밀번호가 올바르지 않습니다.",
  "data": null
}
```

---

# 5. 데이터 보강을 위한 교차 검증 전략

| 단계 | 검증 항목 | 활용 API | 수행 로직 | 기대 효과 |
|:---:|----------|---------|----------|----------|
| **1단계** | 주소 및 좌표 검증 | Kakao Local API | 공공데이터 주소를 좌표(X, Y)로 변환 후 PostGIS 적재 | 지번/도로명 주소 오류 제거, 고정밀 위치 확보 |
| **2단계** | 영업 상태 검증 | Kakao Local API | 장소명·전화번호 대조로 폐업 여부 확인 | 실제 영업 중인 장소만 필터링 |
| **3단계** | 속성 자동 보강 | Google Places API | allowsDogs 필드 등 반려동물 동반 가능 여부 자동 업데이트 | 수동 확인 없이 시설 데이터 구조화 |
| **4단계** | 의미론적 추출 | Naver Blog API + Gemini | 블로그 리뷰에서 '대형견 출입', '유모차 필수' 등 세부 특징 추출 | 초개인화 메타데이터(JSONB) 확보 |
| **5단계** | 실시간 위치 정정 | 사용자 GPS (방문 인증) | 체크인 좌표와 DB 좌표 대조 후 오차 시 자동 보정 | 실시간 지도 정확도 유지 |

---

# 6. 안정적 운영을 위한 데이터 전략

## 🔄 데이터 생애주기 및 스케줄링

| 데이터 유형 | 검증 주기 | 내용 |
|------------|----------|------|
| 동물병원/약국 (치명도 높음) | **주 1회** | 배치(Batch) 작업으로 폐업·이전 여부 검증 |
| 동반 카페/식당 | **월 1회** | 외부 API를 통해 속성 재검증 (노키즈존 등) |
| 사용자 제보 | **수시** | [정보 수정 제안] 기능으로 능동적 제보 접수 |
| 장소 썸네일 | **등록 시** | 이미지 누락 장소는 추천 랭킹 패널티 부여 |

## 🛡️ 외부 API 장애 대비 폴백(Fallback)

| 장애 상황 | 폴백 전략 |
|----------|----------|
| **날씨 API 장애** | 가중치 산식에서 날씨 비중 제외, 거리·적합도 위주 추천 |
| **Gemini API 지연/타임아웃** | 규칙 기반(Rule-based) 기본 추천 문구 노출 |
| **Kakao Map SDK 장애** | 정적 지도 이미지 + 주소 텍스트 노출 |
| **DB 장애** | `GlobalExceptionHandler`를 통한 `ApiResponse` 규격 에러 반환 |

---

# 7. API 통신 규격 (FE ↔ BE)

| 항목 | 값 |
|-----|-----|
| **인증 방식** | JWT (JSON Web Token) |
| **토큰 전달** | `Authorization: Bearer <token>` |
| **토큰 저장 (FE)** | `localStorage` (`accessToken` 키) |
| **Base URL (로컬)** | `http://localhost:8080/api/v1` |
| **Base URL (운영)** | `https://api.meongnyangtrip.com/api/v1` |
| **CORS 허용 Origin** | `http://localhost:5173`, `https://meongnyangtrip.com` |
| **허용 Methods** | GET, POST, PUT, DELETE, OPTIONS |
| **응답 규격** | `ApiResponse<T>` — `{ status, message, data }` |