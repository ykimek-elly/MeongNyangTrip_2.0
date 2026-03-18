# NEXT STEPS CHECKLIST

## 1. 발표 전 필수 작업
- [ ] `RecommendationPipelineService`의 하드코딩 사용자/위치 의존성을 발표 시나리오 기준으로 정리하기
  - 이유: 현재 추천 흐름은 사용자 이메일과 좌표가 고정값에 기대고 있어, 발표 중 "개인화 추천" 설명과 실제 동작이 어긋날 수 있습니다. 실제 연동 전이라도 데모 기준 입력값 정책과 fallback 시나리오를 문서/화면 기준으로 명확히 정리해야 합니다.
  - 관련 클래스: `RecommendationPipelineService`, `RecommendationController`, `RecommendationUserReader`, `RecommnedationPetReader`

- [ ] `RecommendationPipelineService`의 단계별 결과를 발표용으로 확인할 수 있게 점검 항목 정리하기
  - 이유: 현재 파이프라인은 후보 추출, 점수화, RAG, 캐시, AI 응답, 로그 저장까지 단계가 많습니다. 발표 전에 각 단계에서 무엇이 나오는지 확인 포인트를 정리해 두면 장애가 나도 빠르게 설명하고 대응할 수 있습니다.
  - 관련 클래스: `RecommendationPipelineService`, `CandidatePlaceService`, `PlaceScoringService`, `RagService`, `GeminiCacheService`, `AiLogService`

- [ ] `CandidatePlaceService`의 후보 추출 기준을 발표 자료 기준으로 고정 정리하기
  - 이유: 현재 추천 결과는 거리, 카테고리, 검증 여부, 날씨 정책, 선호 장소 우선순위에 의해 갈립니다. 이 기준을 명확히 정리하지 않으면 "왜 이 장소가 추천됐는지" 질문에 답하기 어렵습니다.
  - 관련 클래스: `CandidatePlaceService`, `PlaceRepository`, `DistanceCalculator`

- [ ] `PlaceScoringService`의 점수 항목과 가중치를 발표용 표로 정리하기
  - 이유: 이 프로젝트의 강점은 설명 가능한 추천입니다. 반려동물 적합도, 날씨 적합도, 환경, 이동 편의성, 보너스 점수를 어떤 기준으로 계산하는지 정리해 두면 AI 추천보다 시스템 설계 역량이 더 잘 드러납니다.
  - 관련 클래스: `PlaceScoringService`, `ScoredPlace`, `ScoreBreakdown`, `ScoreDetail`

- [ ] `RagService`의 문서 사용 목적과 한계를 발표 전에 명확히 정의하기
  - 이유: 현재 RAG는 추천 결과를 직접 결정한다기보다 설명을 보강하는 역할에 가깝습니다. 이 점을 명확히 구분해야 "RAG가 왜 필요한가"에 대한 질문에 정확히 답할 수 있습니다.
  - 관련 클래스: `RagService`, `PDFLoaderRunner`, `PDFLoaderService`

- [ ] 카카오 알림 연동 전까지 `NotificationBatchService`의 Mock 전송 정책을 체크리스트로 고정하기
  - 이유: 실제 계정 준비 전에는 "전송 요청 성공 처리" 기준이 필요합니다. 발표 직전 흐름이 흔들리지 않도록 어떤 응답을 성공으로 볼지, 어떤 로그를 남길지 정리해 두는 것이 안전합니다.
  - 관련 클래스: `NotificationBatchService`, `KakaoNotificationService`, `NotificationMessageBuilder`, `NotificationScheduler`

- [ ] 현재 테스트 파일과 실제 패키지 구조의 불일치 정리 계획 세우기
  - 이유: 테스트가 남아 있더라도 패키지명과 시그니처가 현재 구조와 어긋나면 발표 중 신뢰도가 떨어집니다. 최소한 어떤 테스트가 유효하고 어떤 테스트가 정리 대상인지 구분이 필요합니다.
  - 관련 클래스: `RecommendationPipelineService`, `RecommendationPipeilineServiceTest`, `MeongNyangBackendApplicationTests`

- [ ] `AiLogService` 기준으로 추천 이력 확인 항목을 발표 전에 정리하기
  - 이유: 추천 결과만 보여주는 것보다 prompt, 추천 장소 요약, cache hit 여부, fallback 여부, latency를 함께 보여주면 시스템 완성도가 크게 올라갑니다.
  - 관련 클래스: `AiLogService`, `AiResponseLog`, `AiResponseLogRepository`, `RecommendationPipelineService`

## 2. 있으면 완성도가 올라가는 작업
- [ ] `RecommendationPipelineService`를 기준으로 추천 실패 케이스별 메시지 기준 정리하기
  - 이유: 현재는 후보 없음, 점수 결과 없음, Gemini 예외 등 여러 실패 경로가 있습니다. 각 경우의 사용자 메시지와 로그 메시지를 정리하면 UX와 발표 퀄리티가 동시에 올라갑니다.
  - 관련 클래스: `RecommendationPipelineService`, `GeminiRecommendationService`, `AiLogService`

- [ ] `CandidatePlaceService`의 indoor/outdoor 판별 규칙을 데이터 기준으로 검토하기
  - 이유: 현재 태그와 텍스트 키워드 기반 판단 비중이 높아 데이터 품질에 따라 오분류가 생길 수 있습니다. 자주 쓰는 태그 패턴을 정리해 두면 추천 품질 설명이 더 탄탄해집니다.
  - 관련 클래스: `CandidatePlaceService`, `Place`

- [ ] `PlaceScoringService`의 점수 breakdown을 응답/시연 화면에서 보여줄 기준 정리하기
  - 이유: 이미 내부적으로 설명 데이터가 만들어지고 있어서, 이것을 시연에서 보여주면 "추천 근거가 보이는 서비스"로 인식됩니다.
  - 관련 클래스: `PlaceScoringService`, `ScoredPlace`, `ScoreBreakdown`, `ScoreDetail`

- [ ] `RagService`의 검색 질의와 필터링 결과를 검증할 샘플 시나리오 만들기
  - 이유: 품종, 날씨, 활동 수준에 따라 어떤 질의가 만들어지고 어떤 문맥이 붙는지 확인 시나리오가 있으면 RAG 품질을 빠르게 검토할 수 있습니다.
  - 관련 클래스: `RagService`, `WeatherContext`, `Pet`

- [ ] `GeminiCacheService`와 `WeatherCacheService`의 cache hit/miss 확인 절차 정리하기
  - 이유: 캐시가 실제로 동작하는지 발표 중 한 번만 확인돼도 설계 신뢰도가 올라갑니다. TTL, 키 기준, miss 시 동작을 체크리스트로 정리해 두는 것이 좋습니다.
  - 관련 클래스: `GeminiCacheService`, `WeatherCacheService`, `CacheConfig`

- [ ] `NotificationMessageBuilder` 기준으로 알림 메시지 템플릿 문구 정리하기
  - 이유: 알림 흐름은 추천 파이프라인의 최종 소비 채널입니다. 실제 외부 API 연동 전이라도 메시지 형태가 안정되어 있어야 나중에 연결만 바꾸면 됩니다.
  - 관련 클래스: `NotificationMessageBuilder`, `KakaoNotificationService`, `NotificationBatchService`

- [ ] `AiLogService`에 저장되는 필드 중심으로 운영/시연용 로그 조회 기준 만들기
  - 이유: 추천 정확도보다도 "우리가 어떤 추천을 언제 어떻게 만들었는지 추적 가능하다"는 점이 프로젝트 완성도를 높여 줍니다.
  - 관련 클래스: `AiLogService`, `AiResponseLog`

- [ ] recommendation 관련 클래스명/패키지명의 오탈자와 이전 흔적 정리 목록 만들기
  - 이유: `RecommnedationPetReader`, `RecommendationPipeilineServiceTest`, `orchestrator` 패키지 흔적은 구현보다 정리도가 낮아 보이는 지점입니다. 바로 수정하지 않더라도 정리 계획이 있어야 합니다.
  - 관련 클래스: `RecommnedationPetReader`, `RecommendationPipeilineServiceTest`, `RecommendationPipelineService`

## 3. 운영 단계에서 확장할 작업
- [ ] `RecommendationPipelineService`에 사용자 실제 위치 입력 흐름 연결하기
  - 이유: 지금 구조는 위치 기반 추천을 전제로 잘 설계돼 있지만, 실제 좌표가 연결되어야 추천 품질이 완성됩니다. 운영 단계에서는 이 연결이 핵심 고도화 포인트입니다.
  - 관련 클래스: `RecommendationPipelineService`, `WeatherGridConverter`, `CandidatePlaceService`

- [ ] `CandidatePlaceService`의 후보 추출 정책을 사용자 행동 데이터와 연결하기
  - 이유: 현재는 선호 장소 문자열 기반 약한 우선순위 정도만 있습니다. 운영 단계에서는 체크인, 클릭, 재방문 데이터를 반영하면 개인화 강도가 올라갑니다.
  - 관련 클래스: `CandidatePlaceService`, `CheckInService`, `PlaceRepository`

- [ ] `PlaceScoringService`에 점수 버전 관리 기준 추가하기
  - 이유: 운영 단계에서는 점수 규칙이 바뀔 가능성이 높습니다. 어떤 버전의 점수 규칙으로 추천했는지 추적 가능해야 로그와 실험 결과를 해석할 수 있습니다.
  - 관련 클래스: `PlaceScoringService`, `AiLogService`

- [ ] `RagService`에 문서 출처/청크 출처 추적 구조 추가 검토하기
  - 이유: 운영 단계에서는 추천 설명의 신뢰성을 위해 어떤 문서가 사용되었는지 확인 가능해야 합니다. 현재는 context 문자열만 남아 있어 출처 추적성이 약합니다.
  - 관련 클래스: `RagService`, `AiLogService`, `VectorStore`

- [ ] `GeminiCacheService`의 캐시 정책을 요청 조건별로 세분화하기
  - 이유: 현재는 프롬프트 전체 해시 기준 1시간 TTL입니다. 운영 단계에서는 날씨, 시간대, 위치 변화 민감도를 고려한 TTL 조정이 필요할 수 있습니다.
  - 관련 클래스: `GeminiCacheService`, `RecommendationPromptService`

- [ ] `WeatherCacheService`의 지역/시간대별 캐시 정책 검토하기
  - 이유: 날씨는 지역성과 시간 민감도가 큽니다. 운영 단계에서는 격자 기반 캐시가 적절한지, 갱신 주기가 충분한지 점검이 필요합니다.
  - 관련 클래스: `WeatherCacheService`, `WeatherService`, `WeatherGridConverter`

- [ ] `NotificationBatchService`의 발송 결과 집계와 재시도 정책 검토하기
  - 이유: 운영 단계에서는 단순 성공/실패 로그만으로 부족합니다. 발송 실패 사유, 재시도 여부, 사용자별 마지막 발송 시점 관리가 필요합니다.
  - 관련 클래스: `NotificationBatchService`, `NotificationScheduler`, `KakaoNotificationService`, `User`

- [ ] recommendation 전반의 관측 지표를 정리하기
  - 이유: 운영 단계에서는 추천 성공률보다 cache hit rate, fallback rate, notification success rate, 평균 latency 같은 지표가 중요합니다.
  - 관련 클래스: `RecommendationPipelineService`, `GeminiCacheService`, `WeatherCacheService`, `AiLogService`, `NotificationBatchService`

## 4. 현재 구조에서 주의할 부분
- [ ] 추천 파이프라인이 한 클래스에 많이 모여 있음
  - 왜 주의해야 하는지: `RecommendationPipelineService`가 오케스트레이션, fallback, 응답 생성, 캐시, 로그까지 함께 담당하고 있어 수정 시 영향 범위가 큽니다.

- [ ] 후보 필터와 점수화 모두 텍스트/태그 품질에 크게 의존함
  - 왜 주의해야 하는지: `CandidatePlaceService`, `PlaceScoringService` 모두 `Place.tags`, `description`, `title` 기반 규칙이 많아서 배치 데이터 품질이 흔들리면 추천 품질도 같이 흔들립니다.

- [ ] RAG가 설명 보강용인지 추천 결정용인지 경계가 혼동될 수 있음
  - 왜 주의해야 하는지: 현재 구조상 `RagService`는 최종 추천 순위를 정하는 컴포넌트가 아니라 설명 보강에 가깝습니다. 발표에서 역할을 과장하면 질문 대응이 어려워집니다.

- [ ] 알림 발송 흐름이 아직 Mock 전제에 가까움
  - 왜 주의해야 하는지: `KakaoNotificationService`는 실제 메시지 본문 활용과 외부 채널 제어가 아직 단순합니다. 지금은 "전송 요청 완료" 수준으로 설명하는 편이 안전합니다.

- [ ] 테스트 구조가 현재 패키지 구조와 일부 어긋나 있음
  - 왜 주의해야 하는지: `RecommendationPipeilineServiceTest`가 `orchestrator` 패키지에 남아 있어 리팩토링 정합성이 낮아 보일 수 있습니다.

- [ ] 로그는 많지만 운영 관점의 핵심 지표로 정리돼 있지 않음
  - 왜 주의해야 하는지: 지금도 로그는 충분하지만, 발표나 운영에서는 "무엇을 측정하고 있는가"가 더 중요합니다. `AiLogService` 중심 지표 정의가 필요합니다.

## 5. 추천 우선순위 TOP5
1. `RecommendationPipelineService` 기준으로 하드코딩 입력과 단계별 확인 포인트를 먼저 정리하기
2. `CandidatePlaceService`와 `PlaceScoringService`의 추천 기준을 발표용 문서/표로 정리하기
3. `NotificationBatchService`와 `KakaoNotificationService`의 Mock 전송 기준을 고정하기
4. `AiLogService` 기준으로 추천 이력, fallback, cache hit, latency 확인 흐름을 정리하기
5. `RecommendationPipeilineServiceTest`를 포함한 recommendation 테스트 구조의 유효 범위를 재정리하기
