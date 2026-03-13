# PET 테이블 명세 (B+D 협의 확정)

> 확정일: 2026-03-13
> 강아지/고양이 통합 → 단일 PET 테이블로 변경 확정
> 회원 1명당 다중 등록 가능 / 대표 동물 1마리 설정 기능 추가 확정

---

## PET 테이블 스키마

| 컬럼명 | 타입 | 설명 | 유효성 검사 |
| --- | --- | --- | --- |
| pet_id | BIGINT PK | 반려동물 고유 식별자 | PK, 자동 생성 |
| user_id | BIGINT FK | 소유 회원 ID | 필수, FK (삭제된 회원 등록 불가) |
| pet_name | VARCHAR(20) | 반려동물 이름 | 필수, 1~20자 |
| pet_type | VARCHAR | 종류 | 필수, `강아지` \| `고양이` |
| pet_breed | VARCHAR(50) | 품종 | 필수, 1~50자 |
| pet_gender | VARCHAR | 성별 | 필수, `남아` \| `여아` |
| pet_size | VARCHAR | 크기 | 필수, ENUM `SMALL` \| `MEDIUM` \| `LARGE` |
| pet_age | INT | 나이 (년) | 필수, 양수 |
| pet_weight | DECIMAL | 체중 (kg) | 선택, 양수 |
| pet_activity | VARCHAR | 활동량 | 필수, ENUM `LOW` \| `NORMAL` \| `HIGH` |
| personality | VARCHAR(100) | 성격 | 선택, 최대 100자 |
| preferred_place | VARCHAR(50) | 선호 장소 유형 | 선택, 최대 50자 |
| is_representative | BOOLEAN | 대표 동물 여부 (알림 수신 기준) | 기본값 false, 첫 등록 시 자동 true |
| reg_date | DATETIME | 등록일 | 자동 저장 |

---

## 프론트엔드 ↔ 백엔드 필드 매핑

| 백엔드 (snake_case) | 프론트엔드 (camelCase) | 타입 | 비고 |
| --- | --- | --- | --- |
| pet_id | id | `number?` | 등록 전 undefined, PUT/DELETE 시 필수 |
| user_id | — | — | 서버에서 JWT 토큰으로 자동 매핑 |
| pet_name | name | `string` | |
| pet_type | type | `'강아지' \| '고양이'` | |
| pet_breed | breed | `string` | |
| pet_gender | gender | `'남아' \| '여아'` | |
| pet_size | size | `'SMALL' \| 'MEDIUM' \| 'LARGE'` | |
| pet_age | age | `number` | |
| pet_weight | weight | `number?` | |
| pet_activity | activity | `'LOW' \| 'NORMAL' \| 'HIGH'` | |
| personality | personality | `string?` | |
| preferred_place | preferredPlace | `string?` | |
| is_representative | isRepresentative | `boolean?` | 대표 동물 (알림 수신 기준), 첫 등록 시 자동 true |
| reg_date | — | — | 서버 자동 저장, 프론트 불필요 |

---

## API Request / Response 규격

### POST /api/pets (등록)

**Request Body**
```json
{
  "pet_name": "초코",
  "pet_type": "강아지",
  "pet_breed": "말티즈",
  "pet_gender": "남아",
  "pet_size": "SMALL",
  "pet_age": 3,
  "pet_weight": 4.5,
  "pet_activity": "NORMAL",
  "personality": "활발하고 호기심 많음",
  "preferred_place": "공원"
}
```

**Response Body**
```json
{
  "status": 201,
  "message": "SUCCESS",
  "data": {
    "pet_id": 1,
    "user_id": 42,
    "pet_name": "초코",
    "pet_type": "강아지",
    "pet_breed": "말티즈",
    "pet_gender": "남아",
    "pet_size": "SMALL",
    "pet_age": 3,
    "pet_weight": 4.5,
    "pet_activity": "NORMAL",
    "personality": "활발하고 호기심 많음",
    "preferred_place": "공원",
    "reg_date": "2026-03-13T10:00:00"
  }
}
```

### PUT /api/pets/{pet_id} (수정)

**Request Body** — 변경할 필드만 전송
```json
{
  "pet_name": "초코",
  "pet_age": 4,
  "pet_weight": 5.0
}
```

**Response Body**
```json
{
  "status": 200,
  "message": "SUCCESS",
  "data": { "pet_id": 1, "..." : "..." }
}
```

### DELETE /api/pets/{pet_id} (삭제)

**Response Body**
```json
{
  "status": 200,
  "message": "SUCCESS"
}
```

---

## 검증 규칙 (DTO — 백엔드)

```java
@NotBlank                        // pet_name
@Size(min = 1, max = 20)

@NotBlank                        // pet_type
// "강아지" | "고양이" 검증

@NotBlank                        // pet_breed
@Size(min = 1, max = 50)

@NotBlank                        // pet_gender
// "남아" | "여아" 검증

@NotNull                         // pet_size
// ENUM: SMALL | MEDIUM | LARGE

@NotNull @Positive               // pet_age
@Positive                        // pet_weight (nullable)

@NotNull                         // pet_activity
// ENUM: LOW | NORMAL | HIGH

@Size(max = 100)                 // personality (nullable)
@Size(max = 50)                  // preferred_place (nullable)
```

---

## 비고

- 회원 1명당 반려동물 **다중 등록 가능** (2026-03-13 확정)
- `is_representative` → 동일 `user_id` 내 1마리만 `true` 유지 (DB 레벨 CHECK 또는 서비스 레이어에서 보장)
- 대표 동물 삭제 시 다음 등록 동물을 자동으로 대표 승격 처리
- `pet_size`, `pet_activity` → DB ENUM 또는 CHECK 제약 조건 적용
- 삭제된 회원의 반려동물 등록 불가 (`user_id` FK 제약)
