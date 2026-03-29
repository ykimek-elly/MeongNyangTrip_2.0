# MeongNyangTrip 2.0

반려동물과 함께 갈 수 있는 장소를 찾고, 현재 상황에 맞는 추천까지 제공하려는 주니어 팀 프로젝트입니다.

## 1. 프로젝트 소개
MeongNyangTrip 2.0은 반려동물 동반 장소 정보를 기반으로, 사용자 위치와 반려동물 정보, 날씨 조건을 함께 고려해 외출 장소를 추천하는 서비스입니다.  
단순 장소 검색에서 끝나지 않고, "오늘 이 사용자에게 어떤 장소가 더 적합한가"를 백엔드에서 판단하도록 구현했습니다.

## 2. 문제 정의
반려동물과 함께 갈 수 있는 장소는 검색할 수 있어도, 실제 선택은 쉽지 않습니다.

- 현재 날씨에 나가도 되는지
- 우리 반려동물 성향과 맞는지
- 너무 멀지 않은지
- 실내/실외 조건이 괜찮은지

이 프로젝트는 이런 조건을 한 번에 묶어, 사용자가 직접 여러 정보를 비교하지 않아도 추천 결과를 받을 수 있게 만드는 것을 목표로 했습니다.

## 3. 해결 방식 (추천 시스템)
추천 기능은 AI에게 전부 맡기지 않고, 서버에서 먼저 후보를 추리고 점수화한 뒤 AI는 설명 생성에만 사용합니다.

- 사용자, 반려동물, 위치 정보 조회
- 기상청 날씨 조회 후 산책 가능 등급 계산
- 주변 장소를 거리, 운영 여부, 카테고리, 날씨 조건으로 1차 필터링
- 반려동물 적합도, 날씨 적합도, 장소 환경, 거리, 최근 추천 이력 페널티를 반영해 점수화
- 상위 결과를 바탕으로 Gemini가 추천 설명 문장 생성
- Redis 캐시로 날씨, AI 응답, 최종 추천 결과 재사용

이 방식으로 추천 품질과 응답 안정성을 같이 확보하려고 했습니다.

## 4. 전체 흐름 요약
`사용자 요청`  
-> `인증 사용자 확인`  
-> `위치 기반 날씨 조회`  
-> `주변 장소 후보 추출`  
-> `점수 계산 및 순위화`  
-> `Gemini 설명 생성`  
-> `추천 결과 반환 또는 알림 발송`

배치에서는 같은 추천 파이프라인을 사용해 추천 생성 후 카카오 알림까지 연결합니다.

## 5. 기술 스택
| 영역 | 기술 |
|------|------|
| Frontend | React, Vite, Tailwind CSS, Zustand, Axios |
| Backend | Spring Boot 3.5, Java 21, Spring Security, JPA/Hibernate |
| Database | PostgreSQL, PostGIS, Redis |
| AI | Spring AI, Google Gemini |
| External API | 기상청 API, Ncloud SENS |
| Infra | Docker, GitHub Actions, AWS |

## 6. 주요 기능
- 반려동물 동반 장소 조회
- 사용자 위치 기반 추천
- 날씨 기반 산책 가능 여부 반영
- 반려동물 성향/조건 기반 장소 점수화
- Gemini 기반 추천 설명 생성
- Redis 캐시를 활용한 추천 결과 재사용
- 당일 추천 결과 캐시 및 중복 요청 방지
- 배치 기반 추천 알림 발송

## 7. 아키텍처 요약
구조는 크게 `Frontend`, `Backend`, `DB/Cache`, `외부 API`로 나뉩니다.

- Frontend는 사용자 입력과 추천 결과 화면을 담당합니다.
- Backend는 인증, 장소 조회, 추천 파이프라인, 배치 알림을 담당합니다.
- PostgreSQL/PostGIS는 장소와 사용자 데이터를 저장하고, Redis는 캐시와 중복 제어에 사용합니다.
- 외부 연동은 기상청 API로 날씨를 받고, Gemini로 설명 문장을 만들고, Ncloud SENS로 알림을 보냅니다.

추천 기능 기준으로 보면 `Controller -> Query/Pipeline Service -> Weather/Candidate/Scoring -> Gemini -> Cache/Log -> Notification` 흐름으로 구성되어 있습니다.

## 8. 문서 링크
- [팀 작업 가이드](docs/team-guide.md)
- [추천 기능 면접용 요약](docs/recommendation-portfolio-review.md)
- [추천 내부 흐름 정리](backend/docs/recommendation-internal-flow-mobile-guide.md)
- [추천 점수화 정리](backend/docs/scoring-guide.md)
- [스케줄러/알림 아키텍처](backend/docs/scheduler-to-notification-architecture.md)
- [코어 설정 문서](docs/specs/core-setup.md)
- [API 명세](docs/specs/api-spec.md)
- [ERD 문서](docs/specs/erd.md)

## 프로젝트 구조
```text
MeongNyangTrip_2.0/
├─ src/                       # frontend
├─ backend/                   # spring boot backend
│  ├─ src/main/java/
│  ├─ src/test/java/
│  └─ docs/
└─ docs/                      # project documents
```

## 실행 요약
### Backend
```bash
cd backend
docker compose up -d
./gradlew bootRun
```

### Frontend
```bash
npm install
npm run dev
```

상세 실행 환경은 `backend/.env`와 프로젝트 문서를 기준으로 맞춰야 합니다.
