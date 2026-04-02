# Scheduler to Notification Architecture

이 문서는 `backend` 내부의 recommendation 관련 패키지를 기준으로, 배치 스케줄러부터 추천 생성, 캐시, Gemini, 알림 전송까지의 흐름을 이해하기 쉽게 정리한 문서입니다.

범위는 다음 흐름에 직접 연결된 클래스만 봤습니다.

- `recommendation.batch`
- `recommendation.service`
- `recommendation.cache`
- `recommendation.notification`
- `recommendation.weather`

## 1. 시스템 개요

이 구조는 "정해진 시간에 사용자별 추천을 만들고, 그 결과를 알림으로 보내는 배치형 추천 시스템"입니다.

핵심 포인트는 3가지입니다.

1. 스케줄러가 배치를 시작한다.
2. 추천 파이프라인이 날씨와 장소 정보를 바탕으로 추천 결과를 만든다.
3. 만든 결과를 캐시하고, AI 문장을 붙여 알림으로 전송한다.

전체적으로 보면 `NotificationBatchService`가 흐름을 조율하고, 실제 추천 생성은 `RecommendationPipelineService`, 실제 발송은 `NotificationService`가 맡는 구조입니다.

## 2. 전체 흐름

### Step 1. 스케줄러 실행

- 시작 클래스: `NotificationScheduler`
- 역할:
  - `runDailyNotificationBatch()`로 추천 알림 배치 실행
  - `runWeatherPreloadBatch()`로 날씨 캐시 선적재 실행

즉, 스케줄러는 직접 추천을 만들지 않고 "언제 시작할지"만 담당합니다.

### Step 2. 알림 대상 사용자 조회

- 담당 서비스: `NotificationBatchService`
- 조회 기준:
  - 알림 수신이 켜져 있는 사용자
  - `ACTIVE` 상태 사용자
  - 일반 `USER` 권한 사용자

이후 대표 반려동물도 한 번에 조회해서 사용자별 처리 준비를 합니다.

### Step 3. 알림 가능 여부 판단

- 담당 서비스: `NotificationPolicyService`
- 확인 항목:
  - 사용자 상태 정상 여부
  - 알림 수신 동의 여부
  - 전화번호 존재 여부
  - 대표 반려동물 존재 여부
  - 오늘 이미 발송했는지 여부

여기서 조건을 통과한 사용자만 추천 생성 단계로 넘어갑니다.

### Step 4. 추천 생성

- 담당 서비스: `RecommendationPipelineService`
- 처리 순서:
  - 사용자 좌표를 날씨 격자 좌표로 변환
  - `WeatherCacheService`에서 날씨 조회
  - `CandidatePlaceService`에서 1차 후보 장소 수집
  - `RecommendationResultCacheService`에서 동일 조건 추천 결과 재사용 여부 확인
  - `PlaceScoringService`에서 후보 장소 점수 계산
  - 상위 장소 기준으로 추천 근거 구성
  - Gemini 문장 생성 또는 캐시 재사용
  - 최종 `RecommendationNotificationResult` 반환

### Step 5. 알림 전송

- 담당 서비스: `NotificationService`
- 처리 순서:
  - `NotificationMessageBuilder`로 알림 메시지 생성
  - `NcloudClient`로 SENS 알림톡 요청 전송
  - `NotificationDeliveryTracker`로 최종 전달 상태 조회

### Step 6. 성공 결과 저장

알림 전송 성공 시 `NotificationBatchService`가 아래 작업을 수행합니다.

- 사용자의 `lastNotificationSentAt` 갱신
- `DailyRecommendationCacheService`에 오늘 추천 결과 저장
- 오늘 발송 마커 저장

## 3. 주요 구성 요소

### `NotificationScheduler`

- 배치 진입점
- 추천 알림 배치와 날씨 선적재 배치를 스케줄링

### `NotificationBatchService`

- 전체 배치 오케스트레이션 담당
- 대상 사용자 조회, 대표 반려동물 조회, 병렬 처리, 성공/실패 집계 수행
- 실질적으로 "배치의 메인 서비스" 역할

### `NotificationPolicyService`

- 발송 가능 여부만 판단
- 추천 생성 로직과 발송 조건을 분리해 역할이 명확함

### `RecommendationPipelineService`

- 추천 생성의 중심
- 날씨 조회, 후보 수집, 점수 계산, 캐시 활용, AI 문장 생성까지 한 흐름으로 연결

### `CandidatePlaceService`

- 추천 가능한 장소 후보를 모으는 1차 필터
- 거리, 카테고리, 운영 여부, 날씨 조건을 기준으로 후보를 줄임

### `PlaceScoringService`

- 후보 장소를 점수화해서 순위를 매김
- 반려동물 적합성, 날씨 적합성, 환경, 이동 편의성, 보너스 요소 등을 반영

### `NotificationService`

- 추천 결과를 실제 외부 알림 전송 요청으로 바꾸는 서비스
- 메시지 생성과 전송 결과 추적까지 담당

## 4. 추천 파이프라인 흐름

추천은 단순히 "AI가 장소를 골라주는 구조"가 아닙니다.  
실제 코드는 먼저 규칙 기반으로 후보를 좁히고, 점수로 순위를 정한 뒤, 마지막에 AI가 설명 문장을 붙이는 구조입니다.

### 4-1. 날씨 조회

- `WeatherGridConverter`가 사용자 좌표를 격자 좌표로 변환
- `WeatherCacheService`가 Redis 캐시를 먼저 확인
- 캐시가 없으면 `WeatherService`를 통해 날씨 API 조회

### 4-2. 후보 장소 수집

- `CandidatePlaceService.getInitialCandidates()`
- 주요 기준:
  - 운영 가능한 장소인지
  - 좌표가 유효한지
  - 추천 가능한 카테고리인지
  - 현재 날씨에 맞는지
  - 사용자와 너무 멀지 않은지

후보가 없으면 완화 조건이나 fallback 필터로 한 번 더 시도합니다.

### 4-3. 추천 결과 캐시 확인

- `RecommendationContextKeyFactory`로 추천 결과 키 생성
- `RecommendationResultCacheService`에서 동일 컨텍스트 결과가 있으면 재사용

즉, 같은 사용자/반려동물/날씨/후보 조합이면 다시 계산하지 않습니다.

### 4-4. 장소 점수 계산

- `PlaceScoringService.scorePlaces()`
- 점수 반영 요소:
  - 반려동물 적합성
  - 날씨 적합성
  - 장소 환경
  - 거리와 이동 편의성
  - 추가 보너스
  - 최근 추천 이력 기반 diversity penalty

이 단계에서 "어떤 장소가 왜 1순위인지"가 결정됩니다.

### 4-5. AI 설명 생성

- `RecommendationEvidenceContextService`가 추천 근거를 정리
- `RecommendationPromptService`가 Gemini 프롬프트 생성
- `GeminiCacheService`를 먼저 확인
- 캐시가 없으면 `GeminiRecommendationService` 호출

최종적으로는 추천 장소와 함께 짧은 알림용 메시지가 만들어집니다.

## 5. 캐시 전략

이 구조에서 캐시는 성능보다도 "중복 계산 방지"와 "외부 API 호출 감소" 목적이 큽니다.

### 날씨 캐시

- 서비스: `WeatherCacheService`
- 키 기준: 날씨 격자 좌표
- 특징:
  - 같은 지역 날씨 재조회 방지
  - 선적재 배치(`WeatherBatchService`)와 공유
  - 악천후일수록 TTL을 더 짧게 사용

### 추천 결과 캐시

- 서비스: `RecommendationResultCacheService`
- 특징:
  - 동일 추천 조건이면 결과 재사용
  - 짧은 TTL로 순간적인 중복 계산 방지

### Gemini 캐시

- 서비스: `GeminiCacheService`
- 방식:
  - 프롬프트 기준 캐시
  - 문맥 fingerprint 기준 캐시

같은 설명을 다시 만들 필요가 없을 때 AI 호출을 줄입니다.

### 일일 추천 캐시

- 서비스: `DailyRecommendationCacheService`
- 특징:
  - 오늘 발송한 추천 결과 저장
  - 오늘 이미 보낸 사용자 여부를 빠르게 판단
  - 배치 후 조회 화면에서도 같은 결과 재사용 가능

### 중복 요청 방지

- 서비스: `RecommendationDedupService`
- 역할:
  - 사용자별 추천 요청 lock
  - 최근 추천 장소 이력 저장
  - 같은 장소 반복 추천 완화

## 6. AI(Gemini) 역할

Gemini는 추천의 "최종 문장 작성자" 역할입니다.

중요한 점은, Gemini가 추천 순위를 직접 계산하지 않는다는 것입니다.

- 추천 순위 결정: `PlaceScoringService`
- 추천 근거 정리: `RecommendationEvidenceContextService`
- 프롬프트 생성: `RecommendationPromptService`
- 문장 생성: `GeminiRecommendationService`

즉, 이 시스템은 "규칙과 점수로 장소를 고르고, AI는 그 결과를 자연스럽게 설명한다"는 구조입니다.

추가로 `GeminiRecommendationService`는 다음 안전장치를 가집니다.

- timeout 설정
- 재시도
- 실패 시 fallback 문장 반환

그래서 AI 호출이 불안정해도 전체 배치가 바로 멈추지 않게 설계되어 있습니다.

## 7. 알림 전송 흐름

알림 전송은 추천 결과를 그대로 보내는 것이 아니라, 알림 플랫폼 형식에 맞게 한 번 변환하는 단계가 있습니다.

### 전송 순서

1. `NotificationService.send()`
2. `NotificationMessageBuilder.buildRequest()`
3. 날씨 타입에 맞는 템플릿 선택
4. 전화번호 정규화
5. 템플릿 파라미터 채우기
6. `NcloudClient.send()` 호출
7. `NotificationDeliveryTracker.trackByRequestId()`로 최종 상태 조회

### 전송 구조의 장점

- 메시지 생성 책임과 외부 API 호출 책임이 분리됨
- 템플릿 기반이라 날씨별 메시지 관리가 쉬움
- 전송 요청 성공과 실제 전달 성공을 구분해서 추적 가능

## 8. 예외 처리 개요

예외 처리는 "한 명 실패가 전체 배치를 막지 않도록" 설계되어 있습니다.

### 배치 레벨 처리

- `NotificationBatchService`가 사용자별 작업을 개별 처리
- 한 사용자 처리 중 예외가 나도 다른 사용자는 계속 진행
- 실패 사유를 enum 기준으로 집계

주요 실패 분류 예시는 다음과 같습니다.

- `PET_NOT_FOUND`
- `NO_CANDIDATE`
- `WEATHER_API_ERROR`
- `AI_RESPONSE_ERROR`
- `NOTIFICATION_MESSAGE_BUILD_FAIL`
- `NOTIFICATION_SEND_FAIL`
- `UNKNOWN_ERROR`

### 추천 파이프라인 처리

- 캐시 hit 시 외부 호출 없이 복구 가능
- Gemini 실패 시 fallback 메시지로 대체 가능
- 전체 파이프라인 예외 시 error 형태의 추천 결과 반환

### 알림 전송 처리

- 전화번호 형식 오류, 템플릿 설정 오류는 요청 생성 단계에서 차단
- 전송 응답이 실패면 배치 실패로 집계
- requestId가 있으면 전달 결과까지 추가 추적

## 한눈에 보는 요약

- 시작은 `NotificationScheduler`
- 전체 조율은 `NotificationBatchService`
- 추천 생성 중심은 `RecommendationPipelineService`
- 후보 수집은 `CandidatePlaceService`
- 순위 계산은 `PlaceScoringService`
- AI는 추천 이유를 문장으로 정리하는 역할
- 캐시는 날씨, 추천 결과, Gemini 응답, 일일 발송 결과로 나뉨
- 최종 발송은 `NotificationService`가 담당
- 실패해도 사용자 단위로 격리되어 전체 배치는 계속 진행됨
