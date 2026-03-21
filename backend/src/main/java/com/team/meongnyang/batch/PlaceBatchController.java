package com.team.meongnyang.batch;

import com.team.meongnyang.place.entity.PlaceStatus;
import com.team.meongnyang.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 장소 데이터 배치 수동 트리거 컨트롤러.
 *
 * [파이프라인 2.0 — 권장 실행 순서]
 *   1. POST /api/v1/admin/batch/places          — KTO 수집 + 네이버·카카오 이중검증 + 저장
 *   2. POST /api/v1/admin/batch/culture         — KCISA 수집 + 네이버·카카오 이중검증 + 저장 (kakaoId 중복제거)
 *   3. POST /api/v1/admin/batch/enrich-images   — 이미지 없는 장소 Naver 이미지 보강
 *   4. POST /api/v1/admin/batch/validate-images — Gemini Vision 이미지 적합성 검증 (부적합 → null화)
 *   5. POST /api/v1/admin/batch/recalculate-ai-rating — AI 별점 계산
 *
 * [레거시 — 기존 데이터 재처리용]
 *   POST /api/v1/admin/batch/enrich            — isVerified=false 레코드 재검증
 *   POST /api/v1/admin/batch/re-verify         — 전체 교차검증 재실행
 *   POST /api/v1/admin/batch/fix-broken-images — 깨진 이미지 교체
 */
@RestController
@RequestMapping("/api/v1/admin/batch")
@RequiredArgsConstructor
public class PlaceBatchController {

    private final PlaceDataBatchService placeDataBatchService;
    private final CultureFacilityBatchService cultureFacilityBatchService;
    private final PlaceEnrichBatchService placeEnrichBatchService;
    private final GeminiImageValidateBatchService geminiImageValidateBatchService;
    private final PlaceExportService placeExportService;
    private final PlaceRepository placeRepository;

    /** 장소 DB 상태별 현황 조회 — GET /api/v1/admin/batch/stats */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total",    placeRepository.count());
        stats.put("active",   placeRepository.countByStatus(PlaceStatus.ACTIVE));
        stats.put("pending",  placeRepository.countByStatus(PlaceStatus.PENDING));
        stats.put("rejected", placeRepository.countByStatus(PlaceStatus.REJECTED));
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/places")
    public ResponseEntity<String> triggerPlaceBatch() {
        placeDataBatchService.runDailyBatch();
        return ResponseEntity.ok("배치 실행 완료");
    }

    @PostMapping("/export-enrich")
    public ResponseEntity<String> exportEnrichJson() {
        try {
            String path = placeExportService.exportToEnrichJson();
            return ResponseEntity.ok("추출 완료: " + path);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("추출 실패: " + e.getMessage());
        }
    }

    @PostMapping("/import-enrich")
    public ResponseEntity<String> importEnrichJson() {
        try {
            int count = placeExportService.importFromEnrichedJson();
            return ResponseEntity.ok("가져오기 완료: " + count + "건");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("가져오기 실패: " + e.getMessage());
        }
    }

    @PostMapping("/culture")
    public ResponseEntity<String> triggerCultureBatch() {
        cultureFacilityBatchService.runCultureBatch();
        return ResponseEntity.ok("문화시설 배치 실행 완료");
    }

    @PostMapping("/enrich")
    public ResponseEntity<String> triggerEnrichBatch() {
        placeEnrichBatchService.runEnrichBatch();
        return ResponseEntity.ok("네이버+카카오 교차검증 + AI 별점 산정 완료");
    }

    @PostMapping("/re-verify")
    public ResponseEntity<String> triggerReVerifyBatch() {
        placeEnrichBatchService.runReVerifyBatch();
        return ResponseEntity.ok("전체 교차검증 재실행 완료 (이름 유사도 검증 적용)");
    }

    /**
     * ACTIVE 장소 10건 카카오 재검증 — dry-run (DB 변경 없음).
     * 결과를 JSON으로 반환해 폐업 의심 장소를 사전 확인한다.
     */
    @PostMapping("/re-verify-sample")
    public ResponseEntity<Map<String, Object>> triggerReVerifySample() {
        Map<String, Object> result = placeEnrichBatchService.reVerifySample();
        return ResponseEntity.ok(result);
    }

    /**
     * 전체 ACTIVE 장소 네이버 재검증 + DB 반영.
     * REJECT 판정 장소 → status = REJECTED (DB 변경).
     */
    @PostMapping("/re-verify-all")
    public ResponseEntity<Map<String, Object>> triggerReVerifyAll() {
        Map<String, Object> result = placeEnrichBatchService.reVerifyAllAndApply();
        return ResponseEntity.ok(result);
    }

    /**
     * 특정 장소 단건 카카오 재검증 — dry-run.
     * POST /api/v1/admin/batch/re-verify-one/{id}
     */
    @PostMapping("/re-verify-one/{id}")
    public ResponseEntity<Map<String, Object>> triggerReVerifyOne(@PathVariable Long id) {
        Map<String, Object> result = placeEnrichBatchService.reVerifyOne(id);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/recalculate-ai-rating")
    public ResponseEntity<String> triggerRecalculateAiRating() {
        int updated = placeEnrichBatchService.recalculateAiRating();
        return ResponseEntity.ok(String.format("AI 별점 재계산 완료 — %d건", updated));
    }

    /** 전체 ACTIVE 장소 AI 별점 + 블로그 키워드 강제 재계산 (blogCount 포함) */
    @PostMapping("/recalculate-ai-rating-all")
    public ResponseEntity<String> triggerRecalculateAllAiRating() {
        int updated = placeEnrichBatchService.recalculateAllAiRating();
        return ResponseEntity.ok(String.format("AI 별점 전체 재계산 완료 — %d건", updated));
    }

    /**
     * 이름 끝에 숫자가 붙은 ACTIVE 장소 Naver 재검증 — dry-run.
     * suspect=true 장소는 /api/v1/admin/places/{id}/reject 로 수동 처리.
     * POST /api/v1/admin/batch/scan-numeric-names
     */
    @PostMapping("/scan-numeric-names")
    public ResponseEntity<Map<String, Object>> triggerScanNumericNames() {
        Map<String, Object> result = placeEnrichBatchService.scanNumericNameSuspects();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/validate-images")
    public ResponseEntity<String> triggerValidateImagesBatch() {
        var result = geminiImageValidateBatchService.runValidateBatch();
        return ResponseEntity.ok(String.format(
                "Gemini 이미지 검증 완료 — 적합: %d건 / 부적합(null화): %d건 / 오류: %d건 / 전체: %d건",
                result.get("validated"), result.get("invalidated"), result.get("error"), result.get("total")));
    }

    @PostMapping("/validate-images/{id}")
    public ResponseEntity<Map<String, Object>> testValidateImage(@PathVariable Long id) {
        var result = geminiImageValidateBatchService.testValidatePlace(id);
        return ResponseEntity.ok(result);
    }
}
