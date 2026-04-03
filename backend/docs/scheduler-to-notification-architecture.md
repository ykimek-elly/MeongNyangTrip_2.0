# 스케줄러에서 알림까지

## 한 줄 설명

배치 알림은 `NotificationScheduler`가 시작하고, 실제 사용자별 추천 생성과 발송은 `NotificationBatchService`가 처리한다.

## 1. 시작 지점

스케줄러는 2개가 있다.

- `runDailyNotificationBatch()`
- `runWeatherPreloadBatch()`

추천 알림 흐름은 첫 번째가 담당한다.

## 2. 전체 흐름

현재 배치 흐름은 아래 순서다.

1. `NotificationScheduler`가 배치를 시작한다.
2. `NotificationBatchService`가 알림 대상 사용자를 조회한다.
3. 대표 반려동물을 한 번에 조회해 `userId -> pet` 맵으로 만든다.
4. 사용자별로 병렬 처리한다.
5. 각 사용자에 대해 좌표, 대표 반려동물, 오늘 발송 여부를 다시 확인한다.
6. 통과한 사용자만 `RecommendationPipelineService`로 추천을 만든다.
7. 추천 결과의 1위 장소가 있으면 `NotificationService`로 알림을 보낸다.
8. 전송 성공 시 `lastNotificationSentAt`과 일일 추천 캐시를 저장한다.
9. 실패는 이유별로 집계한다.

## 3. 대상 사용자 선정

대상 조회는 `getNotificationTargets()`가 담당한다.

현재 조건은 아래와 같다.

- `notificationEnabled = true`
- `status = ACTIVE`
- `role = USER`
- 오늘 이미 발송한 사용자는 제외

이 단계는 1차 필터다.

이후 `processTarget()`에서 다시 한 번 아래를 확인한다.

- 대표 반려동물 존재 여부
- 오늘 이미 발송했는지
- 좌표가 유효한지

## 4. 사용자별 처리

`NotificationBatchService`는 virtual thread executor와 semaphore를 같이 사용한다.

현재 목적은 아래 2가지다.

- 사용자별 처리를 서로 독립적으로 실행
- 병렬 수를 `batch.notification-parallelism`으로 제한

즉, 한 사용자 실패가 전체 배치를 멈추지 않도록 설계되어 있다.

## 5. 추천 생성 구간

사용자별 추천 생성은 별도 배치 전용 로직이 아니라 기존 추천 파이프라인을 그대로 사용한다.

호출은 아래 메서드로 들어간다.

`RecommendationPipelineService.recommendForNotification(user, pet, batchExecutionId)`

이 구조의 장점은 아래와 같다.

- 조회 API와 배치 알림이 같은 추천 기준을 사용한다.
- 추천 품질이 한쪽만 다르게 깨질 가능성이 줄어든다.
- 로그와 캐시 구조가 공통화된다.

## 6. 알림 발송 구간

알림 발송은 `NotificationService`가 담당한다.

내부 순서는 아래와 같다.

1. `NotificationMessageBuilder`가 템플릿 요청을 만든다.
2. `NcloudClient`가 외부 발송 API를 호출한다.
3. `NotificationDeliveryTracker`가 requestId 기준 최종 상태를 조회한다.
4. 최종 상태 코드가 성공이면 성공 응답으로 바꾼다.

즉, 요청 성공과 실제 전달 성공을 분리해서 본다.

## 7. 성공 후 저장

전송이 성공하면 `NotificationBatchService`가 아래를 저장한다.

- `target.markNotificationSent()`
- `userRepository.save(target)`
- `dailyRecommendationCacheService.saveToday(...)`

이렇게 저장된 결과는 같은 날 조회 API에서도 재사용된다.

## 8. 실패 분류

현재 배치는 실패를 enum으로 집계한다.

대표적인 실패 이유는 아래와 같다.

- `PET_NOT_FOUND`
- `ALREADY_SENT_TODAY`
- `COORDINATE_INVALID`
- `NO_CANDIDATE`
- `WEATHER_API_ERROR`
- `AI_RESPONSE_ERROR`
- `NOTIFICATION_MESSAGE_BUILD_FAIL`
- `NOTIFICATION_SEND_FAIL`
- `UNKNOWN_ERROR`

운영에서는 배치 마지막 요약 로그의 `failureReasons`를 보면 어느 구간에서 문제가 났는지 빠르게 알 수 있다.

## 9. 현재 운영 관점 포인트

- 배치와 조회가 같은 추천 파이프라인을 사용한다.
- 좌표 불량 사용자는 추천 전에 `COORDINATE_INVALID`로 빠진다.
- 날씨 조회 실패는 이제 추천을 계속 진행하지 않고 에러로 분리된다.
- 알림 성공 후에만 오늘 발송 처리와 일일 추천 캐시가 저장된다.

## 10. 파일 기준으로 보기

- 스케줄 시작: `recommendation.batch.NotificationScheduler`
- 배치 오케스트레이션: `recommendation.batch.NotificationBatchService`
- 추천 생성: `recommendation.service.RecommendationPipelineService`
- 알림 발송: `recommendation.notification.service.NotificationService`
- 일일 결과 캐시: `recommendation.cache.DailyRecommendationCacheService`
