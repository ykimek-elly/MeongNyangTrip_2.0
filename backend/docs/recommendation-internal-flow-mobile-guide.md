# 추천 내부 흐름 가이드

## 한눈에 보기

현재 추천 조회 진입점은 아래 흐름이다.

`RecommendationController`
-> `RecommendationQueryService`
-> `RecommendationPipelineService`

핵심 원칙은 2가지다.

- 추천 순위는 코드가 계산한다.
- AI는 계산된 결과를 설명하는 역할만 한다.

## 1. 조회 시작

사용자가 추천 조회 API를 호출하면 `RecommendationQueryService`가 먼저 실행된다.

처리 순서는 아래와 같다.

1. 현재 사용자를 조회한다.
2. 오늘 이미 발송된 추천이 있는지 확인한다.
3. 있으면 `DailyRecommendationCacheService`에서 오늘 결과를 바로 반환한다.
4. 없으면 `RecommendationPipelineService`로 들어간다.

즉, 조회 API도 배치가 만든 오늘의 추천 결과를 그대로 재사용할 수 있다.

## 2. 추천 파이프라인

`RecommendationPipelineService`가 실제 추천 생성의 중심이다.

처리 순서는 아래와 같다.

1. 사용자 좌표를 검증한다.
2. 좌표를 기상청 격자로 변환한다.
3. `WeatherCacheService`로 날씨를 조회한다.
4. 날씨가 `ERROR`면 추천을 중단하고 에러 응답을 반환한다.
5. `CandidatePlaceService`로 후보 장소를 수집한다.
6. `RecommendationResultCacheService`에서 동일 조건의 추천 결과 캐시를 확인한다.
7. 캐시가 없으면 `PlaceScoringService`로 후보를 점수화한다.
8. `RecommendationEvidenceContextService`가 설명 근거를 만든다.
9. `RecommendationPromptService`가 Gemini 프롬프트를 만든다.
10. Gemini context cache, prompt cache를 순서대로 확인한다.
11. 캐시가 없으면 `GeminiRecommendationService`가 설명 문구를 생성한다.
12. 결과 캐시와 최근 추천 이력을 저장한다.

## 3. 후보 장소 수집

후보 수집은 `CandidatePlaceService`가 담당한다.

### 현재 반영 요소

- 사용자 좌표 기준 반경 검색
- 날씨 walk level
- 반려동물 activity radius
- 운영 가능 여부
- 좌표 유효성
- 추천 대상 카테고리
- 실내/실외와 날씨 정책
- 선호 장소 우선순위

### 거리 기준

- `GOOD`: 최대 12km
- `CAUTION`: 최대 8km
- `DANGEROUS`: 최대 5km
- 반려동물 activity radius가 더 작으면 그 값을 우선한다.

### 필터 단계

후보는 아래 3단계로 찾는다.

1. strict: 날씨 조건까지 그대로 반영
2. relaxed: 날씨 조건만 완화
3. fallback: 운영 가능 여부, 좌표, 카테고리, 거리만 확인

## 4. 점수 계산

점수 계산은 `PlaceScoringService`가 담당한다.

현재 점수는 아래 5개 섹션으로 구성된다.

- 반려동물 적합도
- 날씨 적합도
- 장소 환경 적합도
- 거리/이동 편의
- 보너스

그리고 아래 2개가 함께 붙는다.

- 정책/상황 페널티
- 최근 추천 중복 방지용 diversity penalty

### 설명 가능성용 데이터

현재 스코어링 결과에는 아래 값도 함께 남는다.

- `sectionScores`
- `breakdowns`
- `scoreDetails`
- `appliedBoosts`
- `appliedPenalties`
- `summary`
- `reason`

## 5. 설명 근거 생성

`RecommendationEvidenceContextService`는 점수 결과를 바로 AI에 넘기지 않고, 설명용 근거 문맥으로 다시 정리한다.

현재 포함되는 영역은 아래와 같다.

- 사용자 정보
- 반려동물 정보
- 날씨 정보
- 추천 판단 요약
- 설명 필수 근거
- 상위 장소 근거
- 추가 지침

특히 `explanationFocusSection`은 1위 장소에 대해 아래를 우선 뽑는다.

- 날씨 관련 근거
- 반려동물 관련 근거
- 일반 강점 근거
- 필요 시 주의점

## 6. AI 설명 생성

`RecommendationPromptService`는 출력 형식과 제약을 강하게 고정한다.

현재 프롬프트 규칙 핵심은 아래와 같다.

- 4~6문장으로 작성
- 1위 장소 근거를 최소 2개 포함
- 날씨 근거를 최소 1개 포함
- 반려동물 관련 근거가 있으면 반드시 포함
- 2위, 3위보다 왜 앞서는지 마지막에 드러나야 함
- `[추천설명]`, `[알림요약]` 형식을 지켜야 함

즉, AI가 순위를 정하는 구조가 아니라 이미 정해진 1위를 설명하는 구조다.

## 7. 캐시 구조

현재 추천 조회에 직접 연결되는 캐시는 4개다.

### 날씨 캐시

- 서비스: `WeatherCacheService`
- 기준: 격자 좌표

### 추천 결과 캐시

- 서비스: `RecommendationResultCacheService`
- 목적: 같은 입력 조건의 추천 재계산 방지

### Gemini 캐시

- 서비스: `GeminiCacheService`
- 종류: context cache, prompt cache

### 일일 추천 캐시

- 서비스: `DailyRecommendationCacheService`
- 목적: 배치가 보낸 오늘의 추천을 조회 API에서도 재사용

## 8. 실패 처리

### 후보가 없는 경우

- `place = null`
- 빈 추천 메시지 반환
- 시스템 오류로 보지 않는다

### 날씨 조회 실패

- `WeatherService`는 fallback weather를 만들 수 있다.
- 현재 파이프라인은 `walkLevel=ERROR` 또는 `precipitationType=ERROR`면 추천을 중단한다.

### AI 실패

- Gemini 서비스가 fallback 문구를 반환할 수 있다.
- fallback 응답은 캐시에 저장하지 않는다.

### 중복 요청

- `RecommendationDedupService`로 사용자 단위 lock을 건다.
- 이미 진행 중이면 `RECOMMENDATION_IN_PROGRESS`를 반환한다.

## 9. 운영에서 기억할 점

- 추천 품질의 핵심은 `CandidatePlaceService`와 `PlaceScoringService`다.
- AI 품질보다 먼저 후보와 점수 로직을 봐야 한다.
- 오늘 발송된 결과는 조회 API에서 그대로 재사용될 수 있다.
- 날씨 실패는 이제 추천 진행이 아니라 명시적 에러로 처리된다.
- context cache hit도 이제 AI 로그에 `cacheHit=true`로 남는다.
