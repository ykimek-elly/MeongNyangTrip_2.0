package com.team.meongnyang.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.place.repository.PlaceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Gemini Vision API를 이용한 장소 이미지 적합성 검증 배치 서비스.
 *
 * imageUrl이 존재하는 장소를 대상으로 Gemini 2.0 Flash에 이미지를 전달하여
 * 해당 이미지가 장소명/카테고리와 관련 있는지 YES/NO로 판별한다.
 * 관련 없음(NO) 또는 오류 시 imageUrl을 NULL로 초기화한다.
 *
 * 키 로테이션: 429(일일 한도 초과) 발생 시 다음 키로 자동 전환 후 즉시 재시도.
 * 모든 키 소진 시 배치 중단.
 *
 * 수동 실행: POST /api/v1/admin/batch/validate-images
 *
 * 사전 준비:
 *   1. Google AI Studio(https://aistudio.google.com) API Key 발급
 *   2. backend/.env 또는 환경변수에 GEMINI_API_KEY, GEMINI_API_KEY_2 설정
 */
@Slf4j
@Service
public class GeminiImageValidateBatchService {

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash:generateContent?key=";

    private final PlaceRepository placeRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.api.key2:}")
    private String geminiApiKey2;

    @Autowired
    public GeminiImageValidateBatchService(PlaceRepository placeRepository) {
        this.placeRepository = placeRepository;
    }

    /**
     * 이미지 보유 장소 전체 대상으로 Gemini Vision 적합성 검증 실행.
     * 부적합 판정 시 imageUrl = NULL 초기화 → 이후 naver 이미지 보강 배치로 재보강.
     *
     * @return stats: validated(적합), invalidated(부적합→null화), error(오류), total
     */
    @Transactional
    @CacheEvict(value = {"places", "places:detail"}, allEntries = true)
    public Map<String, Integer> runValidateBatch() {
        // 유효한 키만 수집 (빈 값 제외)
        List<String> keys = buildKeyList();
        if (keys.isEmpty()) {
            log.warn("===== Gemini API Key가 설정되지 않아 이미지 검증을 건너뜁니다. =====");
            return Map.of("validated", 0, "invalidated", 0, "error", 0, "total", 0);
        }

        log.info("===== Gemini 이미지 검증 시작 — 사용 가능 키: {}개 =====", keys.size());

        List<Place> targets = placeRepository.findByImageUrlIsNotNullAndNotEmpty();
        int total = targets.size();
        log.info("===== 검증 대상: {}건 =====", total);

        int validated = 0;
        int invalidated = 0;
        int error = 0;
        int currentKeyIndex = 0;   // 현재 사용 중인 키 인덱스

        for (int i = 0; i < targets.size(); i++) {
            Place place = targets.get(i);
            boolean retried = false;

            while (true) {
                try {
                    String activeKey = keys.get(currentKeyIndex);
                    boolean relevant = isImageRelevant(activeKey, place.getImageUrl(), place.getTitle(), place.getCategory());
                    if (relevant) {
                        validated++;
                    } else {
                        place.enrichImageFromNaver(null);
                        invalidated++;
                        log.debug("[이미지무관] id={}, name={}", place.getId(), place.getTitle());
                    }
                    // Gemini free tier: 15 RPM 제한 → 요청 간 4초 간격 유지
                    Thread.sleep(4000);
                    break; // 성공 → 다음 장소로

                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("[Gemini검증] 배치 인터럽트 발생 — 중단");
                    return Map.of("validated", validated, "invalidated", invalidated, "error", error, "total", total);

                } catch (Exception e) {
                    boolean is429 = e.getMessage() != null && e.getMessage().contains("429");

                    if (is429 && !retried) {
                        // 429 발생 — 다음 키로 전환 후 즉시 재시도
                        currentKeyIndex++;
                        if (currentKeyIndex < keys.size()) {
                            log.warn("[Gemini키전환] 키 #{} 한도 초과 → 키 #{} 로 전환 후 재시도...",
                                    currentKeyIndex - 1, currentKeyIndex);
                            retried = true;
                            continue; // 같은 장소 재시도
                        } else {
                            // 모든 키 소진
                            log.error("===== [Gemini] 모든 API 키 일일 한도 초과 — 배치 중단 ({}/{}건 처리 완료) =====",
                                    i, total);
                            return Map.of("validated", validated, "invalidated", invalidated, "error", error, "total", total);
                        }
                    } else {
                        // 429 재시도 실패 또는 다른 에러
                        log.error("[이미지검증오류] id={}, name={} — {}", place.getId(), place.getTitle(), e.getMessage());
                        error++;
                        break; // 이 장소 건너뜀
                    }
                }
            }

            if ((i + 1) % 50 == 0) {
                log.info("[Gemini검증 진행] {}/{} — 적합: {}, 부적합: {}, 오류: {}, 현재키: #{}",
                        i + 1, total, validated, invalidated, error, currentKeyIndex);
            }
        }

        log.info("===== Gemini 이미지 검증 완료: 적합 {}건 / 부적합(null화) {}건 / 오류 {}건 / 전체 {}건 =====",
                validated, invalidated, error, total);
        return Map.of("validated", validated, "invalidated", invalidated, "error", error, "total", total);
    }

    /**
     * 유효한 API 키 목록 구성 (빈 값·공백 제외).
     */
    private List<String> buildKeyList() {
        List<String> keys = new ArrayList<>();
        if (geminiApiKey != null && !geminiApiKey.isBlank()) keys.add(geminiApiKey);
        if (geminiApiKey2 != null && !geminiApiKey2.isBlank()) keys.add(geminiApiKey2);
        return keys;
    }

    /**
     * Gemini 2.0 Flash Vision API 호출.
     * 이미지를 base64로 인코딩하여 inlineData로 전달 → YES/NO 응답 파싱.
     *
     * @param apiKey    사용할 API 키
     * @param imageUrl  검증할 이미지 URL
     * @param placeName 장소명 (프롬프트에 사용)
     * @param category  카테고리 (프롬프트에 사용)
     * @return true = 관련 있음(적합), false = 관련 없음(부적합)
     */
    private boolean isImageRelevant(String apiKey, String imageUrl, String placeName, String category) throws Exception {
        // 1. 이미지 바이트 다운로드 + base64 인코딩
        byte[] imageBytes = downloadImageBytes(imageUrl);
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        String mimeType = detectMimeType(imageUrl);

        // 2. Gemini API 요청 JSON 구성
        String prompt = String.format(
                "이 이미지가 '%s' (%s) 장소와 관련이 있습니까? 반려동물 동반 여행 장소 사진으로 적합한지 판단해주세요. YES 또는 NO로만 답해주세요.",
                placeName, category != null ? category : "여행지"
        );

        Map<String, Object> inlineData = Map.of(
                "mimeType", mimeType,
                "data", base64Image
        );
        Map<String, Object> imagePart = Map.of("inlineData", inlineData);
        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(imagePart, textPart));
        Map<String, Object> requestBody = Map.of("contents", List.of(content));

        // 3. API 호출
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                GEMINI_API_URL + apiKey, entity, String.class
        );

        // 4. 응답 파싱
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Gemini API 오류: " + response.getStatusCode());
        }

        JsonNode root = objectMapper.readTree(response.getBody());
        String answer = root
                .path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText("")
                .trim().toUpperCase();

        return answer.startsWith("YES");
    }

    private byte[] downloadImageBytes(String imageUrl) throws Exception {
        try (var stream = URI.create(imageUrl).toURL().openStream()) {
            return stream.readAllBytes();
        }
    }

    private String detectMimeType(String imageUrl) {
        String lower = imageUrl.toLowerCase();
        if (lower.contains(".png")) return "image/png";
        if (lower.contains(".gif")) return "image/gif";
        if (lower.contains(".webp")) return "image/webp";
        return "image/jpeg"; // default
    }
}
