package com.team.meongnyang.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    private final NaverImageEnrichBatchService naverImageEnrichBatchService;
    private final GeminiImageValidateBatchService geminiImageValidateBatchService;

    @PostMapping("/places")
    public ResponseEntity<String> triggerPlaceBatch() {
        placeDataBatchService.runDailyBatch();
        return ResponseEntity.ok("배치 실행 완료");
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

    @PostMapping("/recalculate-ai-rating")
    public ResponseEntity<String> triggerRecalculateAiRating() {
        int updated = placeEnrichBatchService.recalculateAiRating();
        return ResponseEntity.ok(String.format("AI 별점 재계산 완료 — %d건", updated));
    }

    @PostMapping("/enrich-images")
    public ResponseEntity<String> triggerImageEnrichBatch() {
        var result = naverImageEnrichBatchService.runImageEnrichBatch();
        return ResponseEntity.ok(String.format(
                "네이버 이미지 보강 완료 — 성공: %d건 / 이미지없음: %d건 / 전체: %d건",
                result.get("updated"), result.get("noImage"), result.get("total")));
    }

    @PostMapping("/fix-broken-images")
    public ResponseEntity<String> triggerFixBrokenImagesBatch() {
        var result = naverImageEnrichBatchService.runFixBrokenImagesBatch();
        return ResponseEntity.ok(String.format(
                "깨진 이미지 교체 완료 — 초기화: %d건 / 재보강 성공: %d건 / 이미지없음: %d건 / 전체: %d건",
                result.get("reset"), result.get("updated"), result.get("noImage"), result.get("total")));
    }

    @PostMapping("/validate-images")
    public ResponseEntity<String> triggerValidateImagesBatch() {
        var result = geminiImageValidateBatchService.runValidateBatch();
        return ResponseEntity.ok(String.format(
                "Gemini 이미지 검증 완료 — 적합: %d건 / 부적합(null화): %d건 / 오류: %d건 / 전체: %d건",
                result.get("validated"), result.get("invalidated"), result.get("error"), result.get("total")));
    }
}
