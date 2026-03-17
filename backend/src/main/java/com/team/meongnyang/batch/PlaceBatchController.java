package com.team.meongnyang.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 장소 데이터 배치 수동 트리거 컨트롤러.
 * 개발/테스트 환경에서 DB 초기 데이터 투입 시 사용.
 *
 * POST /api/v1/admin/batch/places
 * POST /api/v1/admin/batch/culture
 * POST /api/v1/admin/batch/enrich
 * POST /api/v1/admin/batch/enrich-images
 */
@RestController
@RequestMapping("/api/v1/admin/batch")
@RequiredArgsConstructor
public class PlaceBatchController {

    private final PlaceDataBatchService placeDataBatchService;
    private final CultureFacilityBatchService cultureFacilityBatchService;
    private final PlaceEnrichBatchService placeEnrichBatchService;
    private final NaverImageEnrichBatchService naverImageEnrichBatchService;

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
        return ResponseEntity.ok("Google Places 속성 보강 완료");
    }

    @PostMapping("/enrich-images")
    public ResponseEntity<String> triggerImageEnrichBatch() {
        var result = naverImageEnrichBatchService.runImageEnrichBatch();
        return ResponseEntity.ok(String.format(
                "네이버 이미지 보강 완료 — 성공: %d건 / 이미지없음: %d건 / 전체: %d건",
                result.get("updated"), result.get("noImage"), result.get("total")));
    }
}
