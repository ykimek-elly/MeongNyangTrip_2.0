# 🗄️ 멍냥트립 2.0 DB 스키마 변경 및 추가 안내 (DB 담당자용)

최근 기능 고도화(DM, 온보딩, 친구 기능)에 따라 기존 테이블에 추가된 컬럼과 신규로 생성된 테이블 목록입니다. 운영 또는 스테이징 DB 반영 시 참고해 주세요.

---

## 1. 🆕 신규 생성 테이블 (Table Additions)

### ① `dm_messages` (DM/멍냥톡 내역)
- **설명**: 사용자 간의 다이렉트 메시지를 저장합니다.
- **주요 컬럼**:
  - `id` (BIGINT, PK, Identity)
  - `sender_id` (BIGINT, FK -> `users.user_id`)
  - `receiver_id` (BIGINT, FK -> `users.user_id`)
  - `content` (TEXT, NOT NULL)
  - `is_read` (BOOLEAN, DEFAULT FALSE)
  - `reg_date` (TIMESTAMP, BaseEntity)

### ② `friends` (친구 관계)
- **설명**: 사용자 간의 친구 맺기 정보를 저장합니다.
- **주요 컬럼**:
  - `id` (BIGINT, PK, Identity)
  - `user_id` (BIGINT, FK -> `users.user_id`)
  - `friend_user_id` (BIGINT, FK -> `users.user_id`)
  - `reg_date` (TIMESTAMP)

### ③ `share_records` (장소 공유 기록)
- **설명**: 친구에게 장소를 공유한 이력을 기록합니다.
- **주요 컬럼**:
  - `id` (BIGINT, PK, Identity)
  - `user_id` (BIGINT, FK -> `users.user_id`)
  - `place_id` (BIGINT, FK -> `places.id`)
  - `friend_user_id` (BIGINT, FK -> `users.user_id`)
  - `reg_date` (TIMESTAMP)

---

## 2. 🔼 기존 테이블 변경 사항 (Column Additions)

### ① `users` 테이블
기존 회원 정보 외에 **위치 기반 정밀 추천 및 온보딩**을 위한 컬럼이 추가되었습니다.
- `latitude` (DOUBLE PRECISION): 사용자 현재/설정 위도
- `longitude` (DOUBLE PRECISION): 사용자 현재/설정 경도
- `activity_radius` (INTEGER): 활동 반경 (기본값: 15)
- `region` (VARCHAR(50)): 활동 지역 명칭 (예: "서울 강남구")

### ② `pets` 테이블
- `is_representative` (BOOLEAN): 한 명의 사용자가 가진 여러 마리 중 **대표 반려동물** 여부 (기본값: false)
- `notify_enabled` (BOOLEAN): 개별 반려동물별 알림 수신 여부 (기본값: true)

---

## 3. 📝 DB 담당자 전달 사항 (Action Items)

1. **인덱스(Index) 권장**: 
   - `dm_messages` 테이블의 `sender_id`, `receiver_id` 복합 인덱스 생성을 권장합니다. (채팅방별 최신순 조회 성능 최적화용)
   - `friends` 테이블의 `user_id` 인덱스 생성을 권장합니다.
2. **PostGIS 확인**: 
   - 현재 `places` 테이블에서 PostGIS의 `Point` 타입(`geom` 컬럼)을 사용 중이므로, 해당 Extension이 활성화되어 있어야 합니다.
3. **DDL 자동 생성 주의**: 
   - 현재 로컬/개발 환경에서는 Hibernate `ddl-auto: update`를 사용 중이나, 운영 환경 반영 시에는 위 변경 사항을 포함한 SQL 스크립트 기반의 마이그레이션을 권장합니다.
