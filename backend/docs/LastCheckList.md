# LastCheckList

## 1. 전체 총평
- 현재 프로젝트의 강점
  - 배치 시작점(`NotificationScheduler`)부터 추천 파이프라인(`RecommendationPipelineService`), 캐시(`WeatherCacheService`, `DailyRecommendationCacheService`, `GeminiCacheService`), AI 로그(`AiLogService`), 알림 전송(`NotificationService`, `NcloudClient`)까지 레이어가 분리되어 있어 "추천/AI/배치/캐시/알림" 전체 설계를 보여주기 좋다.
  - 추천 근거를 `ScoredPlace`, `ScoreBreakdown`, `RecommendationEvidenceContext`로 남기고, AI 프롬프트에도 그대로 반영하는 구조는 CRUD 프로젝트와 확실히 구분되는 포인트다.
  - 최근 추천 이력 기반 다양성 패널티(`AiLogService.getRecentRecommendedPlacePenalties`, `PlaceScoringService.resolveDiversityPenalty`)가 들어가 있어 추천 시스템 설계 의도를 설명하기 좋다.
  - 알림 전송 이후 최종 전달 상태까지 추적하려는 시도(`NotificationDeliveryTracker`)가 있어 운영 관점 고민이 코드에 남아 있다.
- 현재 프로젝트의 아쉬운 점
  - 실제 사용자 위치 대신 수원 고정 좌표를 사용하고 있어 추천 정확도와 설계 설득력이 크게 떨어진다. `RecommendationPipelineService.SUWON_LAT/SUWON_LNG`, `PlaceScoringService.DEFAULT_USER_LAT/DEFAULT_USER_LNG`가 대표적이다.
  - 배치 성공 처리의 핵심 상태값을 reflection으로 변경한다. `NotificationBatchService.markNotificationSent()`는 발표/실무 모두에서 바로 질문받을 포인트다.
  - 알림 전송 로그에 전화번호와 템플릿 파라미터가 그대로 남는다. 운영/보안 관점에서 민감하다.
  - `NcloudClient` 주석은 "mock 형태"라고 적혀 있는데 실제로는 `RestTemplate.exchange()`로 실전 호출 구조다. 문서와 코드가 어긋난다.
  - 테스트는 일부 핵심 happy path 위주로만 있고, 날씨 장애/Redis miss-hit/대표 펫 없음/알림 전달 실패/다양성 패널티 같은 실패 시나리오 검증이 얕다.
- 취업용 포트폴리오로서 인상적인 부분
  - 추천 점수를 설명 가능한 구조로 분해한 점.
  - AI 호출 실패 시 fallback, 캐시 hit/miss, 알림 전달 상태 추적까지 포함한 점.
  - 추천 결과를 배치와 실시간 조회에서 재사용하는 daily cache 전략.
- 마지막으로 꼭 보완해야 할 핵심 포인트
  - 1순위는 "실제 사용자 위치 반영".
  - 2순위는 "배치 상태 변경과 재시도 정책 정리".
  - 3순위는 "민감 정보 로그 정리".
  - 4순위는 "추천 실패/알림 실패/캐시 누락 시나리오 테스트 보강".

## 2. 흐름별 상세 점검
### 2-1. 배치 시작
- 관련 파일
  - `src/main/java/com/team/meongnyang/recommendation/batch/NotificationScheduler.java`
  - `src/main/java/com/team/meongnyang/recommendation/batch/NotificationBatchService.java`
  - `src/main/resources/application.yml`
- 현재 구조 설명
  - `NotificationScheduler.runDailyNotificationBatch()`가 `batch.notification-cron`으로 배치를 기동한다.
  - 실제 실행은 `NotificationBatchService.runDailyNotificationBatch()`에서 수행하고, 사용자별 작업은 virtual thread + `Semaphore`로 병렬 제한한다.
  - 날씨 preload는 별도 스케줄(`batch.weather-preload-cron`)로 먼저 수행한다.
- 문제점
  - `NotificationBatchService.runDailyNotificationBatch()`는 배치 중복 실행 방지 장치가 없다. 스케줄 중복, 수동 실행, 장애 재기동 시 같은 날 이중 발송 가능성이 있다.
  - 배치 단위 결과는 `success/failure/skip`만 남고, 실패 원인 분류가 없다. 운영 기준으로는 `NO_PET`, `NO_CANDIDATE`, `AI_FAIL`, `INVALID_PHONE`, `SENS_FAIL` 정도는 분리돼야 한다.
  - `NotificationBatchService.markNotificationSent()`가 reflection으로 `User.lastNotificationSentAt`를 바꾼다. 리팩토링 내성이 약하고, 엔티티 불변성 설계도 흐려진다.
  - `NotificationBatchService`는 전송 실패 사용자를 별도로 저장하지 않아 재시도 배치 근거가 없다.
- 개선 제안
  [x] 빠르게 수정 가능: `User` 엔티티에 `markNotificationSent(LocalDateTime)` 같은 도메인 메서드를 추가하고 reflection 제거.
  [x] 발표 전에만 정리해도 되는 것: 배치 결과 로그에 실패 사유 카운터를 추가하고, 문서에 "중복 실행 방지 전략 미구현"을 명시.
  [x] 운영 단계에서 개선할 것: Redis 락 또는 ShedLock 계열로 단일 실행 보장, 실패 대상 재시도 큐 분리.
  -   -> 단일 서버이기 때문에 아직 상관없어보임
- 우선순위
  - 높음

### 2-2. 사용자 조회
- 관련 파일
  - `src/main/java/com/team/meongnyang/recommendation/batch/NotificationBatchService.java`
  - `src/main/java/com/team/meongnyang/user/repository/UserRepository.java`
  - `src/main/java/com/team/meongnyang/recommendation/service/RecommendationUserReader.java`
  - `src/main/java/com/team/meongnyang/recommendation/service/RecommnedationPetReader.java`
  - `src/main/java/com/team/meongnyang/user/repository/PetRepository.java`
- 현재 구조 설명
  - 배치에서는 `UserRepository.findAllByNotificationEnabledTrueAndStatus(User.Status.ACTIVE)`로 전체 대상자를 가져온다.
  - 대표 반려동물은 `PetRepository.findAllByUserUserIdInAndIsRepresentativeTrueAndUserStatus(...)`로 일괄 조회한다.
  - 실시간 추천에서는 `RecommendationUserReader.getCurrentUserByEmail()` 후 `RecommnedationPetReader.getPrimaryPet()`로 단건 조회한다.
- 문제점
  - `RecommnedationPetReader` 클래스명 자체가 오타다. 면접에서 바로 눈에 띈다.
  - `RecommnedationPetReader.getPrimaryPet()` 주석에 `todo : 대표 펫이 없다면 FirstPet 조회 ...`가 남아 있는데 실제 구현이 없다.
  - 배치 대상 조회는 paging 없이 전체 로딩이다. 사용자가 많아지면 메모리와 DB 부하가 커진다.
  - 사용자 조회 시 전화번호 유효성, 최근 발송 여부, 대표 반려동물 존재 여부를 DB 레벨에서 미리 걸러내지 못하고 애플리케이션에서 skip한다.
- 개선 제안
  [x] 빠르게 수정 가능: 클래스명 `RecommendationPetReader`로 교정, TODO 제거 또는 실제 fallback 구현.
  [x] 발표 전에만 정리해도 되는 것: "현재는 ACTIVE + notificationEnabled만 선별하고, 펫/전화번호 검증은 후단 skip 처리"라고 한계 명시.
  [ ] 운영 단계에서 개선할 것: 발송 대상 전용 projection/paging 쿼리로 `userId`, `phoneNumber`, `representativePetId`, `lastNotificationSentAt`를 한 번에 가져오도록 최적화.
- 우선순위
  - 중간

### 2-3. 날씨 조회 / 캐시
- 관련 파일
  - `src/main/java/com/team/meongnyang/recommendation/batch/WeatherBatchService.java`
  - `src/main/java/com/team/meongnyang/recommendation/cache/WeatherCacheService.java`
  - `src/main/java/com/team/meongnyang/recommendation/weather/service/WeatherService.java`
  - `src/main/java/com/team/meongnyang/recommendation/weather/service/WeatherRuleService.java`
  - `src/main/java/com/team/meongnyang/recommendation/weather/service/WeatherGridConverter.java`
- 현재 구조 설명
  - 스케줄러가 서울/경기 격자만 미리 preload한다.
  - 추천 시 `WeatherGridConverter.convertToGrid()` 후 `WeatherCacheService.getOrLoadWeather(nx, ny)`를 호출한다.
  - miss면 `WeatherService.getWeather()`가 외부 API를 호출하고, 성공한 문맥만 Redis에 저장한다.
- 문제점
  - `WeatherRuleService.evaluateWalkLevel()`은 `raining || hot || windy || cold`면 전부 `CAUTION`이고 `DANGEROUS`를 전혀 만들지 않는다. 그런데 `CandidatePlaceService`는 `dangerous` 분기를 가지고 있다. 설계와 구현이 어긋난다.
  - `RecommendationPipelineService.resolveWeatherType()`는 `"NONE"`을 기준으로 흐림 여부를 판단하지만, `WeatherService.convertPty()`는 `"없음"`을 반환한다. 결과적으로 비가 아니고 폭염/한파가 아니면 거의 항상 `CLOUDY`가 될 가능성이 높다.
  - `WeatherBatchService` preload 대상이 서울/경기로 고정이라 사용자 확장성 설명이 약하다.
  - `WeatherCacheService`는 key lock을 잘 두었지만 hit/miss 외에 외부 API latency, fallback 발생률을 남기지 않는다.
  - `WeatherService.getBaseTime()`은 무조건 현재 시각 기준 1시간 전 `HH00`을 사용한다. 기상청 초단기 실황 발표 시각과 어긋날 가능성은 확인 필요다.
- 개선 제안
  [x] 빠르게 수정 가능: `resolveWeatherType()`에서 `"NONE"` 대신 `weatherContext.isRaining()`와 `walkLevel` 중심으로 분기 재정의.
  [x] 빠르게 수정 가능: `WeatherRuleService`에 `DANGEROUS` 기준을 실제로 만들거나, `CandidatePlaceService`에서 `dangerous` 분기를 제거해 정책을 일치.
  [x] 발표 전에만 정리해도 되는 것: preload가 MVP 범위라 서울/경기만 지원한다고 명시.
  [ ] 운영 단계에서 개선할 것: weather API latency, timeout, fallback rate 지표 로그 추가.
- 우선순위
  - 높음

### 2-4. 후보 장소 조회
- 관련 파일
  - `src/main/java/com/team/meongnyang/recommendation/service/CandidatePlaceService.java`
  - `src/main/java/com/team/meongnyang/place/repository/PlaceRepository.java`
  - `src/main/java/com/team/meongnyang/place/entity/Place.java`
- 현재 구조 설명
  - `PlaceRepository.findNearby()`로 반경 내 최대 120개를 가져온 뒤, 운영 여부/좌표/카테고리/거리/날씨 조건을 순차 필터링한다.
  - 엄격 필터 → 날씨 완화 필터 → core fallback 필터 순으로 후보를 만든다.
  - 반려동물 선호 장소 키워드가 있으면 약한 우선순위를 준다.
- 문제점
  - `isOperationalPlace()`는 `isVerified`만 보고 `Place.status`는 보지 않는다. `REJECTED`, `PENDING` 데이터가 `isVerified=true`라면 섞일 여지가 있다.
  - 날씨 정책과 실내/실외 판정이 태그 문자열 기반 substring에 강하게 의존한다. 데이터 품질이 흔들리면 필터 품질도 바로 흔들린다.
  - 거리 제한은 DB에서 한 번 걸고, 애플리케이션에서 다시 하버사인 계산으로 거른다. 정합성 확인용이면 괜찮지만 이유가 로그/주석으로 충분히 설명되지 않는다.
  - `BASE_FETCH_LIMIT=120`, `RESULT_LIMIT=20`, 거리 임계치가 모두 하드코딩이다.
- 개선 제안
  - 빠르게 수정 가능: `status == ACTIVE`도 함께 체크.
  - 발표 전에만 정리해도 되는 것: 태그 기반 실내/실외 판정은 규칙 기반 MVP이며 추후 구조화 예정이라고 문서화.
  - 운영 단계에서 개선할 것: 실내/실외, 반려동물 허용, 주차 여부 등을 구조화 컬럼으로 승격.
- 우선순위
  - 높음

### 2-5. 추천 필터링
- 관련 파일
  - `src/main/java/com/team/meongnyang/recommendation/service/CandidatePlaceService.java`
  - `src/main/java/com/team/meongnyang/recommendation/weather/service/WeatherRuleService.java`
- 현재 구조 설명
  - 필터링은 `matchesWeatherPolicy()`, `isIndoorPlace()`, `isOutdoorPlace()` 중심이다.
  - `walkLevel`에 따라 허용 후보를 좁힌다.
- 문제점
  - 앞단 `WeatherRuleService`가 `DANGEROUS`를 만들지 않으므로 `matchesWeatherPolicy()`의 `dangerous` 정책은 죽은 코드에 가깝다.
  - `relaxWeather=true`가 되면 날씨 조건을 완전히 무시한다. "비 와도 야외 대형 공원" 같은 추천이 튈 가능성이 있다.
  - 추천 실패 시 사용자 메시지가 `"인기 있는 장소를 추천합니다."`인데 실제 fallback 추천은 수행하지 않는다. 메시지와 실제 동작이 다르다.
- 개선 제안
  - 빠르게 수정 가능: relax 모드에서도 "실외 완전 개방형 제외" 같은 최소 안전 규칙 유지.
  - 빠르게 수정 가능: 실패 메시지를 실제 동작과 맞추기.
  - 운영 단계에서 개선할 것: 필터 drop 이유를 샘플링 로그로 남겨 데이터 품질 개선에 활용.
- 우선순위
  - 높음

### 2-6. 추천 스코어링
- 관련 파일
  - `src/main/java/com/team/meongnyang/recommendation/service/PlaceScoringService.java`
  - `src/test/java/com/team/meongnyang/recommendation/service/PlaceScoringServiceTest.java`
- 현재 구조 설명
  - 반려동물 적합도, 날씨 적합도, 장소 환경, 이동 편의성, 보너스를 합산하고 penalty를 뺀다.
  - `ScoredPlace`에 summary, reason, scoreDetails까지 남겨 설명 가능성을 확보했다.
- 문제점
  - `PlaceScoringService` 안에 하드코딩 상수와 규칙이 매우 많고, 대부분 설정이나 정책 객체가 아니라 코드에 박혀 있다.
  - `searchablePlaceText(place)`가 한 장소당 여러 메서드에서 반복 호출된다. 후보 수가 많아질수록 문자열 정규화 비용이 중복된다.
  - `user` 파라미터는 여러 메서드에서 거의 사용되지 않는데 시그니처에 계속 남아 있다. 설계 의도와 실제 사용이 어긋난다.
  - `scorePlace()`와 `scorePlaces()` 오버로드가 많고, 기본 좌표가 다시 수원 고정값이다.
- 개선 제안
  - 빠르게 수정 가능: `searchablePlaceText` 결과를 한 번만 만들어 재사용.
  - 발표 전에만 정리해도 되는 것: 점수 가중치 표를 문서로 정리해 면접에서 설명 가능하게 만들기.
  - 운영 단계에서 개선할 것: 정책값을 yml 또는 전략 객체로 외부화하고, 오프라인 튜닝 로그를 남기기.
- 우선순위
  - 중간

### 2-7. 다양성/최근 추천 반영
- 관련 파일
  - `src/main/java/com/team/meongnyang/recommendation/log/service/AiLogService.java`
  - `src/main/java/com/team/meongnyang/recommendation/log/entity/AiResponseLog.java`
  - `src/main/java/com/team/meongnyang/recommendation/log/repository/AiResponseLogRepository.java`
  - `src/main/java/com/team/meongnyang/recommendation/service/PlaceScoringService.java`
- 현재 구조 설명
  - 최근 7일 AI 로그를 조회해 장소별 패널티를 만들고, 점수 계산 시 감점한다.
  - 1일 이내 25점, 7일 이내 12점 패널티를 준다.
- 문제점
  - `AiLogService.getRecentRecommendedPlacePenalties()`는 추천이 실제 알림 발송까지 성공했는지 구분하지 않고 `AiResponseLog`만 본다. 실시간 조회나 실패한 추천도 다양성 패널티에 섞일 수 있다.
  - `AiResponseLog`에 `deliverySuccess`, `servedFromBatch`, `sourceChannel(batch/web)` 같은 필드가 없다.
  - 패널티 폭이 큰데 근거가 코드 밖에 없다. 포트폴리오 발표에서는 정책 근거 설명 자료가 필요하다.
- 개선 제안
  - 빠르게 수정 가능: 로그 엔티티에 source/delivery outcome 컬럼 추가.
  - 발표 전에만 정리해도 되는 것: "다양성 패널티는 최근 추천 중복 노출 방지용"이라는 설계 문서 추가.
  - 운영 단계에서 개선할 것: 일자별 추천 노출 로그와 클릭/저장 행동까지 분리해 패널티 근거 정교화.
- 우선순위
  - 중간

### 2-8. RAG/AI 코멘트 생성
- 관련 파일
  - `src/main/java/com/team/meongnyang/recommendation/context/service/RecommendationEvidenceContextService.java`
  - `src/main/java/com/team/meongnyang/recommendation/service/RecommendationPromptService.java`
  - `src/main/java/com/team/meongnyang/recommendation/service/GeminiRecommendationService.java`
  - `src/main/java/com/team/meongnyang/recommendation/service/RecommendationPipelineService.java`
- 현재 구조 설명
  - 스코어링 결과를 `RecommendationEvidenceContext`로 압축한 뒤, 프롬프트를 만들고 Gemini 호출 또는 캐시 재사용을 수행한다.
  - AI 응답 원문은 `AiResponseLog`와 daily cache에 저장된다.
- 문제점
  - 엄밀히 말하면 현재는 외부 문서 검색형 RAG보다 "서버 계산 근거를 프롬프트로 주입하는 evidence injection"에 가깝다. 발표에서 RAG라고 부르면 질문을 받을 수 있다.
  - `RecommendationPipelineService.extractNotificationSummary()`는 `[알림요약]` 섹션이 없으면 기본 문구로 떨어진다. 모델 출력 형식이 조금만 흔들려도 동일한 기본 문구가 반복될 수 있다.
  - `GeminiCacheService.generateKey()`는 모델명을 반영하지 않고 프롬프트 해시만 쓴다. 주석은 "모델 버전과 프롬프트 내용을 함께 반영"이라고 되어 있어 코드와 불일치한다.
  - `AiLogService.save()`는 modelName을 `"gemini-2.5-flash-lite"`로 하드코딩한다. 실제 설정 변경 시 로그와 운영 환경이 어긋난다.
- 개선 제안
  - 빠르게 수정 가능: 문서/발표에서는 "RAG" 대신 "추천 근거 컨텍스트 주입"으로 표현하거나, 실제 외부 지식 검색 계층을 추가.
  - 빠르게 수정 가능: Gemini 캐시 키에 모델명 포함.
  - 발표 전에만 정리해도 되는 것: 프롬프트 입력 섹션과 출력 형식을 표로 정리.
  - 운영 단계에서 개선할 것: AI 응답 형식을 JSON 스키마로 강제하고 parsing 안정성 확보.
- 우선순위
  - 높음

### 2-9. 캐시 저장
- 관련 파일
  - `src/main/java/com/team/meongnyang/recommendation/cache/DailyRecommendationCacheService.java`
  - `src/main/java/com/team/meongnyang/recommendation/dto/DailyRecommendationCachePayload.java`
  - `src/main/java/com/team/meongnyang/recommendation/cache/GeminiCacheService.java`
  - `src/main/java/com/team/meongnyang/recommendation/cache/WeatherCacheService.java`
  - `src/main/java/com/team/meongnyang/recommendation/service/RecommendationQueryService.java`
- 현재 구조 설명
  - 날씨/AI 응답은 요청 중간 캐시, daily recommendation은 당일 배치 결과 재사용 캐시다.
  - 사용자가 당일 알림을 받았으면 `RecommendationQueryService`가 daily cache를 우선 사용한다.
- 문제점
  - `RecommendationQueryService`는 `lastNotificationSentAt`이 오늘이면 daily cache를 조회한다. 그런데 배치 중 `userRepository.save()` 성공 후 `dailyRecommendationCacheService.saveToday()`가 실패하면, 사용자는 "오늘 발송됨" 상태인데 cache miss로 다시 실시간 추천을 타게 된다.
  - `DailyRecommendationCachePayload`에 `Place` 엔티티를 통째로 넣는다. Redis payload가 커지고, 엔티티 구조 변경에 민감하다.
  - weather/gemini cache hit/miss는 로그만 있고 배치 총괄 지표로 합산되지 않는다.
- 개선 제안
  - 빠르게 수정 가능: daily cache 저장 실패를 별도 warn/error로 분리하고, 저장 실패 시 `lastNotificationSentAt` 갱신 순서를 재검토.
  - 빠르게 수정 가능: `Place` 엔티티 전체 대신 placeId, title, category, imageUrl 정도의 캐시 전용 DTO로 축소.
  - 운영 단계에서 개선할 것: 캐시 hit ratio, payload size, ttl 만료 후 재생성 빈도 메트릭 추가.
- 우선순위
  - 높음

### 2-10. 알림 메시지 생성
- 관련 파일
  - `src/main/java/com/team/meongnyang/recommendation/notification/service/NotificationMessageBuilder.java`
  - `src/main/java/com/team/meongnyang/recommendation/notification/template/KakaoTemplateManager.java`
  - `src/main/java/com/team/meongnyang/recommendation/notification/template/KakaoWeatherTemplateType.java`
  - `src/test/java/com/team/meongnyang/recommendation/notification/service/NotificationMessageBuilderTest.java`
- 현재 구조 설명
  - `weatherType`를 내부 enum으로 정규화하고, 템플릿 코드/본문/치환 파라미터를 한 곳에서 만든다.
  - 파라미터는 `petName`, `placeName`, `comment` 세 개만 사용한다.
- 문제점
  - 템플릿 매핑 안정성은 괜찮지만, 실제 place detail link/button이 전혀 붙지 않는다. `NcloudSensProperties.buildDetailLink()`와 `hasUsableDetailLink()`가 있는데 사용되지 않는다.
  - 길이 제한이 없다. 장소명/코멘트가 길면 알림톡 심사/전송에서 잘릴 수 있다.
  - `weatherType` 매핑이 앞단 버그의 영향을 받으면 잘못된 templateCode가 선택된다.
- 개선 제안
  - 빠르게 수정 가능: `comment`, `placeName` 길이 제한과 trimming 정책 추가.
  - 발표 전에만 정리해도 되는 것: 템플릿별 코드 매핑표와 예시 payload 문서화.
  - 운영 단계에서 개선할 것: detail link button 실제 연결, 템플릿 심사 정책 기준 길이 검증 추가.
- 우선순위
  - 중간

### 2-11. 알림 전송
- 관련 파일
  - `src/main/java/com/team/meongnyang/recommendation/notification/service/NotificationService.java`
  - `src/main/java/com/team/meongnyang/recommendation/notification/client/NcloudClient.java`
  - `src/main/java/com/team/meongnyang/recommendation/notification/service/NotificationDeliveryTracker.java`
  - `src/main/java/com/team/meongnyang/recommendation/notification/config/NcloudSensProperties.java`
  - `src/test/java/com/team/meongnyang/recommendation/notification/client/NcloudClientTest.java`
  - `src/test/java/com/team/meongnyang/recommendation/notification/service/NotificationDeliveryTrackerTest.java`
- 현재 구조 설명
  - `NotificationService.send()`가 요청 생성 후 `NcloudClient.send()`를 호출하고, requestId가 있으면 전달 상태를 재조회한다.
  - `NcloudClient`는 SENS 서명 헤더를 만들고 `RestTemplate`로 실제 HTTP 호출을 수행한다.
- 문제점
  - `NotificationService.send()` 로그에 전화번호, `templateParameter`, placeId가 그대로 찍힌다. 개인정보/운영 로그 최소화 원칙에 어긋난다.
  - `NotificationDeliveryTracker.trackByRequestId()`는 발송 1건마다 최대 4번 HTTP를 칠 수 있다. 대량 사용자 배치에서는 전송보다 상태 조회가 병목이 될 가능성이 높다.
  - `NcloudClient` 클래스 주석은 "mock 형태"라고 적혀 있는데 구현은 실전 호출이다. 포트폴리오 문서 신뢰도를 깎는다.
  - mock 전환용 인터페이스 분리가 없다. 실전/테스트 전환은 현재 `RestTemplate` mocking에 의존한다.
  - `isConfigurationReady()`는 baseUrl/serviceId/access/secret만 보고 plusFriendId/templateCode 누락은 뒤 단계에서 예외로 터진다. 검증 시점이 일관되지 않다.
- 개선 제안
  - 빠르게 수정 가능: 로그 마스킹 적용. 전화번호는 뒤 4자리만, 템플릿 파라미터는 길이/키만 남기기.
  - 빠르게 수정 가능: 설정 유효성 검사를 한 곳으로 모아 시작 시 검증.
  - 발표 전에만 정리해도 되는 것: "현재는 동기 전달 추적, 운영에서는 비동기 delivery reconciliation로 전환 예정"이라고 설명.
  - 운영 단계에서 개선할 것: `NotificationGateway` 인터페이스 도입, 비동기 결과 수집 배치 분리.
- 우선순위
  - 매우 높음

### 2-12. 로그 / 예외 처리 / 운영 관점
- 관련 파일
  - `src/main/java/com/team/meongnyang/common/GlobalExceptionHandler.java`
  - `src/main/java/com/team/meongnyang/recommendation/log/RecommendationBatchTraceContext.java`
  - `src/main/java/com/team/meongnyang/recommendation/log/service/AiLogService.java`
  - `src/main/java/com/team/meongnyang/recommendation/batch/NotificationBatchService.java`
- 현재 구조 설명
  - 배치에서는 MDC에 `batchExecutionId`, `userId`, `petId`를 넣어 로그를 묶는다.
  - 예외는 대부분 로깅 후 fallback 또는 failure response로 흡수한다.
- 문제점
  - `GlobalExceptionHandler.handleException()`이 예외 클래스명과 메시지를 그대로 API 응답에 노출한다.
  - 배치 성공/실패는 집계하지만 핵심 KPI가 없다. 예: weather cache hit rate, gemini cache hit rate, AI fallback rate, invalid phone rate, delivery accepted rate.
  - `AiLogService.save()` 자체 실패 시 상위 파이프라인에 영향이 갈 수 있는데 별도 보호가 없다.
  - 로그는 많지만 운영 관점 핵심 숫자는 부족하고, 반대로 민감 정보는 많다.
- 개선 제안
  - 빠르게 수정 가능: 공통 예외 응답에서 내부 예외명 제거.
  - 빠르게 수정 가능: 배치 마지막 summary 로그에 `aiFallbackCount`, `cacheHitCount`, `invalidPhoneCount` 추가.
  - 운영 단계에서 개선할 것: 알림 실패 레코드 테이블, 재시도 상태 테이블, 모니터링 대시보드 지표 정의.
- 우선순위
  - 높음

## 3. 성능 관점 Last Check
- 병목 가능 지점
  - `NotificationService.send()` 이후 `NotificationDeliveryTracker.trackByRequestId()`가 동기 재조회까지 수행한다. 사용자 수가 늘면 SENS 재조회 호출이 배치 전체 시간을 잡아먹는다.
  - `RecommendationPipelineService`는 사용자별로 AI 호출까지 직렬 체인으로 간다. weather는 캐시되더라도 Gemini miss가 많으면 배치 시간이 AI 응답 시간에 종속된다.
  - `CandidatePlaceService`와 `PlaceScoringService`는 문자열 정규화/substring 연산을 후보마다 반복한다.
  - `NotificationBatchService`는 전체 대상자와 대표 펫을 한 번에 로딩한다.
- 우선 개선할 성능 포인트
  - 1순위: 알림 전달 상태 조회를 동기 분리.
  - 2순위: 사용자/대표펫 조회를 paging 또는 projection 기반으로 전환.
  - 3순위: 장소 텍스트 전처리 재사용.
  - 4순위: AI cache key와 hit ratio를 개선해 Gemini miss를 줄이기.
- 대량 사용자 기준 위험 요소
  - 전송 성공 후 결과 조회 polling이 외부 API rate limit에 걸릴 수 있다.
  - daily cache에 `Place` 엔티티를 통째로 저장하면 Redis 메모리 사용량이 커진다.
  - 대상자 전체 로딩 구조는 수만 명 이상에서 메모리 피크가 생길 수 있다.
- 캐시 전략 보완점
  - weather cache는 괜찮지만 preload 지역 범위가 좁다.
  - gemini cache key에 모델 버전이 빠져 있다.
  - daily cache 저장 실패와 `lastNotificationSentAt` 갱신 순서가 엇갈릴 수 있다.
- 병렬 처리 또는 비동기 처리 제안
  - 빠르게 수정 가능: delivery tracking을 옵션화해서 배치 기본 경로에서는 끄기.
  - 운영 단계에서 개선할 것: 알림 전송과 전달 상태 추적을 분리한 2단계 배치.
  - 운영 단계에서 개선할 것: 대상 사용자 chunk 단위 처리와 결과 집계.

## 4. 취업용 차별점 강화 포인트
- 지금도 좋은 점
  - 추천 점수 근거가 실제 DTO와 로그로 남아 있다.
  - AI 호출 전에 서버가 먼저 후보를 좁히고 근거를 정리하는 구조가 명확하다.
  - 날씨, 다양성, 캐시, 알림까지 end-to-end 흐름이 있다.
- 더 강조하면 좋은 점
  - `RecommendationEvidenceContextService`와 `RecommendationPromptService`를 묶어 "설명 가능한 추천 근거를 AI 요약으로 압축"하는 구조를 더 전면에 내세우기.
  - `AiLogService`와 `NotificationDeliveryTracker`를 묶어 "운영 추적 가능한 AI 추천 배치"라는 메시지로 설명하기.
- 코드/문서/로그에서 보완할 점
  - 추천 정책표: 점수 항목, 가중치, 패널티 기준, fallback 기준.
  - 배치 시퀀스 다이어그램: scheduler → user query → weather cache → candidate → score → context → gemini → cache → notification.
  - 운영 로그 샘플: 성공 1건, AI fallback 1건, invalid phone 1건, no candidate 1건.
  - 현재는 수원 고정 좌표라는 한계를 반드시 명시하고, 실제 사용자 위치 반영 예정 여부를 적어야 한다.
- 발표 때 어필 가능한 기술 포인트
  - "추천 결과를 점수로 설명 가능하게 만든 뒤 AI는 설명 생성만 담당하도록 역할을 분리했다."
  - "날씨/AI/일일 추천을 각각 다른 TTL과 목적의 Redis 캐시로 나눴다."
  - "최근 추천 이력으로 다양성을 보정해 같은 장소 반복 추천을 줄이려 했다."
  - "알림 발송 후 requestId 기반 전달 상태까지 추적하도록 설계했다."

## 5. 꼭 수정하면 좋은 항목 TOP 10
- [ ] 실제 사용자 위치 반영으로 수원 고정 좌표 제거
  - 이유: 추천 품질과 면접 설득력에 가장 큰 영향.
  - 관련 파일: `RecommendationPipelineService`, `PlaceScoringService`
  - 수정 난이도: 중간
  - 기대 효과: "개인화 추천" 주장 근거 확보
- [ ] `resolveWeatherType()`와 `WeatherService.convertPty()` 불일치 수정
  - 이유: 잘못된 templateCode 선택 가능성.
  - 관련 파일: `RecommendationPipelineService`, `WeatherService`
  - 수정 난이도: 낮음
  - 기대 효과: 알림 템플릿 정확도 상승
- [ ] `WeatherRuleService`의 `DANGEROUS` 정책 정합성 맞추기
  - 이유: 필터 설계와 실제 날씨 레벨 생성 로직 불일치.
  - 관련 파일: `WeatherRuleService`, `CandidatePlaceService`
  - 수정 난이도: 낮음
  - 기대 효과: 날씨 기반 필터 설명 가능성 개선
- [ ] `lastNotificationSentAt` reflection 제거
  - 이유: 유지보수성과 도메인 모델 신뢰도 저하.
  - 관련 파일: `NotificationBatchService`, `User`
  - 수정 난이도: 낮음
  - 기대 효과: 발표/면접 방어 쉬워짐
- [ ] 알림 로그 마스킹 적용
  - 이유: 전화번호와 templateParameter가 그대로 노출됨.
  - 관련 파일: `NotificationService`, `NcloudClient`
  - 수정 난이도: 낮음
  - 기대 효과: 운영/보안 관점 완성도 상승
- [ ] 알림 결과 polling을 배치 기본 경로에서 분리 또는 옵션화
  - 이유: 외부 API 병목 가능성이 큼.
  - 관련 파일: `NotificationService`, `NotificationDeliveryTracker`
  - 수정 난이도: 중간
  - 기대 효과: 대량 사용자 확장성 개선
- [ ] daily cache 저장 실패 시나리오 정리
  - 이유: 발송 시각 갱신과 캐시 저장이 원자적이지 않음.
  - 관련 파일: `NotificationBatchService`, `DailyRecommendationCacheService`, `RecommendationQueryService`
  - 수정 난이도: 중간
  - 기대 효과: 사용자 경험 일관성 개선
- [ ] `AiLogService` 로그 스키마 확장
  - 이유: 추천 생성 로그와 실제 발송 성공 로그가 구분되지 않음.
  - 관련 파일: `AiResponseLog`, `AiLogService`
  - 수정 난이도: 중간
  - 기대 효과: 다양성 패널티 근거 정교화
- [ ] `GeminiCacheService.generateKey()`에 모델명 포함
  - 이유: 주석과 구현 불일치, 모델 변경 시 캐시 오염 가능성.
  - 관련 파일: `GeminiCacheService`, `GeminiRecommendationService`
  - 수정 난이도: 낮음
  - 기대 효과: AI 캐시 신뢰도 상승
- [ ] `RecommnedationPetReader` 오타와 TODO 정리
  - 이유: 포트폴리오 완성도 저하 요소.
  - 관련 파일: `RecommnedationPetReader`
  - 수정 난이도: 낮음
  - 기대 효과: 코드 품질 인상 개선

## 6. 있으면 완성도가 올라가는 항목
- [ ] 추천 정책표 문서
  - 이유: 점수/패널티/필터 기준을 면접에서 빠르게 설명 가능.
  - 관련 파일: 신규 문서, `PlaceScoringService`, `CandidatePlaceService`
  - 기대 효과: 설명 가능 추천 설계가 더 또렷해짐
- [ ] 배치 운영 지표 로그 샘플
  - 이유: 단순 기능 구현이 아니라 운영 가능성을 보여줌.
  - 관련 파일: `NotificationBatchService`, `WeatherCacheService`, `GeminiCacheService`
  - 기대 효과: 포트폴리오 차별화
- [ ] 추천 흐름 시퀀스 다이어그램
  - 이유: 배치→추천→AI→알림 흐름이 한 장으로 정리됨.
  - 관련 파일: 신규 문서
  - 기대 효과: 발표 전달력 상승
- [ ] 실패 유형별 재시도 정책 표
  - 이유: 운영 질문 대비에 좋음.
  - 관련 파일: 신규 문서, `NotificationService`, `WeatherService`, `GeminiRecommendationService`
  - 기대 효과: 실무형 설계 어필
- [ ] 알림 payload 예시와 템플릿 코드 매핑표
  - 이유: templateCode, weatherType, phone number 정합성 설명에 유용.
  - 관련 파일: 신규 문서, `NotificationMessageBuilder`, `KakaoTemplateManager`
  - 기대 효과: 외부 연동 설계 신뢰도 향상
- [ ] 테스트 매트릭스
  - 이유: 현재 없는 실패 시나리오를 한눈에 보여줄 수 있음.
  - 관련 파일: 신규 문서, `src/test/java/...`
  - 기대 효과: 품질 관리 관점 강화

## 7. 최종 한줄 정리
- 지금 당장 무엇부터 손대야 하는지 우선순위 순으로 정리
  - 1. 사용자 위치 하드코딩 제거
  - 2. 날씨 타입/날씨 레벨 정책 불일치 수정
  - 3. 알림 로그 마스킹과 동기 polling 부담 축소
  - 4. reflection 제거와 daily cache 저장 순서 정리
  - 5. 발표용 정책표, 시퀀스 다이어그램, 운영 지표 로그 샘플 추가

## 면접관이 물을 가능성이 높은 질문 포인트
- 왜 실제 사용자 위치 대신 수원 좌표를 사용했는지, 그리고 이를 어떻게 바꿀 계획인지?
- 추천 점수는 서버가 계산하고 AI는 설명만 하게 분리한 이유가 무엇인지?
- Gemini 응답이 실패하거나 형식이 깨질 때 어떤 fallback을 두었는지?
- 같은 사용자에게 동일 장소가 반복 추천되지 않도록 어떤 장치를 넣었는지?
- 알림톡은 "요청 성공"과 "최종 전달 성공"이 다른데, 현재 시스템은 어디까지 보장하는지?

## 참고 메모
- 실제 확인한 테스트 결과
  - `backend`에서 `.\gradlew test` 실행 결과 2026-03-24 기준 `BUILD SUCCESSFUL`.
- 추가로 확인이 필요한 부분
  - 기상청 `base_time` 계산이 실제 API 발표 시각 정책과 완전히 맞는지는 별도 운영 검증 필요.
  - 사용자 실좌표 저장 필드 또는 위치 수집 흐름은 본 점검 범위에서 확인되지 않음.
