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
 */
@RestController
@RequestMapping("/api/v1/admin/batch")
@RequiredArgsConstructor
public class PlaceBatchController {

    private final PlaceDataBatchService placeDataBatchService;

    @PostMapping("/places")
    public ResponseEntity<String> triggerPlaceBatch() {
        placeDataBatchService.runDailyBatch();
        return ResponseEntity.ok("배치 실행 완료");
    }
}
