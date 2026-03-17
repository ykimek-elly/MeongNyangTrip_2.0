package com.team.meongnyang.batch;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.place.repository.PlaceRepository;
import com.team.meongnyang.place.service.NaverLocalImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 네이버 지역 검색 API 이미지 보강 배치 서비스.
 *
 * imageUrl이 NULL인 장소를 대상으로 네이버 Local Search API에서
 * 대표 이미지(thumbnail)를 가져와 저장한다.
 *
 * 수동 실행: POST /api/v1/admin/batch/enrich-images
 *
 * 사전 준비:
 *   1. 네이버 개발자 센터(https://developers.naver.com) 앱 등록
 *   2. "검색 > 지역" API 권한 추가
 *   3. application.yml 또는 환경변수에 키 설정:
 *      NAVER_LOCAL_CLIENT_ID / NAVER_LOCAL_CLIENT_SECRET
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NaverImageEnrichBatchService {

    private final PlaceRepository placeRepository;
    private final NaverLocalImageService naverLocalImageService;

    /**
     * 오배치 이미지 일괄 교체 배치.
     * - Instagram/Facebook/Pinterest CDN (403/핫링크 차단)
     * - imgnews.naver.net (뉴스 기사 사진 — 장소 사진 아님)
     * 위 URL을 NULL 초기화 후 cafeblog 필터로 재보강.
     *
     * POST /api/v1/admin/batch/fix-broken-images
     */
    @Transactional
    @CacheEvict(value = {"places", "places:detail"}, allEntries = true)
    public Map<String, Integer> runFixBrokenImagesBatch() {
        int resetBroken = placeRepository.resetBrokenImageUrls();
        int resetNews = placeRepository.resetNewsImageUrls();
        int totalReset = resetBroken + resetNews;
        log.info("===== 오배치 이미지 초기화: SNS차단={}건 / 뉴스CDN={}건 / 합계={}건 =====",
                resetBroken, resetNews, totalReset);

        var result = runImageEnrichBatch();
        return Map.of(
                "reset", totalReset,
                "updated", result.get("updated"),
                "noImage", result.get("noImage"),
                "total", result.get("total")
        );
    }

    @Transactional
    @CacheEvict(value = {"places", "places:detail"}, allEntries = true)
    public Map<String, Integer> runImageEnrichBatch() {
        List<Place> targets = placeRepository.findByImageUrlIsNullOrEmpty();
        int total = targets.size();
        log.info("===== 네이버 이미지 보강 시작: {}건 =====", total);

        int updated = 0;
        int noImage = 0;

        for (int i = 0; i < targets.size(); i++) {
            Place place = targets.get(i);
            try {
                String thumbnail = naverLocalImageService.fetchThumbnailUrl(place.getTitle());
                if (thumbnail != null) {
                    place.enrichImageFromNaver(thumbnail);
                    updated++;
                } else {
                    noImage++;
                }
            } catch (Exception e) {
                log.error("[이미지보강오류] id={}, name={} — {}", place.getId(), place.getTitle(), e.getMessage());
                noImage++;
            }

            if ((i + 1) % 100 == 0) {
                log.info("[이미지보강 진행] {}/{} — 성공: {}, 이미지없음: {}",
                        i + 1, total, updated, noImage);
            }
        }

        log.info("===== 이미지 보강 완료: 성공 {}건 / 이미지없음 {}건 / 전체 {}건 =====",
                updated, noImage, total);
        return Map.of("updated", updated, "noImage", noImage, "total", total);
    }
}
