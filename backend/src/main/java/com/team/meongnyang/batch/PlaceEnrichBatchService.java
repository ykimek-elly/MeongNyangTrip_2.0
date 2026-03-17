package com.team.meongnyang.batch;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.place.repository.PlaceRepository;
import com.team.meongnyang.place.service.KakaoLocalVerifyService;
import com.team.meongnyang.place.service.NaverLocalImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 네이버 + 카카오 복합 교차검증 배치 서비스.
 *
 * isVerified=false 레코드를 대상으로:
 *
 * [1단계 — 네이버 Local Search]
 *   - 운영 교차검증: 검색결과 없으면 폐업 의심
 *   - 이미지 취득: thumbnail (imageUrl이 null/blank인 경우)
 *
 * [2단계 — 카카오 Local API (네이버 미검색 시에만)]
 *   - 카카오에서도 미검색 → 폐업 확정
 *   - KakaoLocalVerifyService 재활용
 *
 * 폐업 확정: 네이버 X + 카카오 X 동시 미검색
 *
 * 수동 실행: POST /api/v1/admin/batch/enrich
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceEnrichBatchService {

    private final PlaceRepository placeRepository;
    private final NaverLocalImageService naverLocalImageService;
    private final KakaoLocalVerifyService kakaoLocalVerifyService;

    @Transactional
    @CacheEvict(value = {"places", "places:detail"}, allEntries = true)
    public void runEnrichBatch() {
        List<Place> targets = placeRepository.findByIsVerified(false);
        log.info("===== 네이버+카카오 교차검증 보강 시작: {}건 =====", targets.size());

        int closed = 0;
        int active = 0;
        int imageUpdated = 0;

        for (int i = 0; i < targets.size(); i++) {
            Place place = targets.get(i);
            try {
                // 1단계: 네이버 — 운영 교차검증 + 이미지 취득
                NaverLocalImageService.VerifyResult naverResult =
                        naverLocalImageService.verifyAndFetchImage(place.getTitle(), place.getAddress());

                if (naverResult.imageUrl() != null
                        && (place.getImageUrl() == null || place.getImageUrl().isBlank())) {
                    place.enrichImageFromNaver(naverResult.imageUrl());
                    imageUpdated++;
                }

                boolean isClosed;
                if (naverResult.isActive()) {
                    // 네이버 검색 성공 → 운영 중
                    isClosed = false;
                } else {
                    // 2단계: 카카오 교차검증 (네이버 미검색 시에만)
                    KakaoLocalVerifyService.VerifyResult kakaoResult =
                            kakaoLocalVerifyService.verify(place.getTitle(), place.getLatitude(), place.getLongitude());
                    isClosed = !kakaoResult.isActive();
                    log.debug("[교차검증] name='{}' 네이버X / 카카오{} → {}",
                            place.getTitle(), kakaoResult.isActive() ? "O" : "X",
                            isClosed ? "폐업" : "운영중");
                }

                place.markVerified(isClosed);

                // AI 추천 별점 계산 — 운영 중/폐업 모두 계산 (폐업이면 0점 반환)
                NaverLocalImageService.BlogResult blog =
                        isClosed ? NaverLocalImageService.BlogResult.empty()
                                 : naverLocalImageService.fetchBlogData(place.getTitle());
                place.computeAiRating(blog.total(), blog.latestPostDate(), blog.descriptions());

                if (isClosed) closed++; else active++;

                if ((i + 1) % 100 == 0) {
                    log.info("[보강 진행] {}/{} — 운영: {}, 폐업: {}, 이미지취득: {}",
                            i + 1, targets.size(), active, closed, imageUpdated);
                }

                // Naver API 속도 제한(10 req/s) 준수
                try { Thread.sleep(200); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); break;
                }

            } catch (Exception e) {
                log.error("[보강오류] id={}, name={} — {}", place.getId(), place.getTitle(), e.getMessage());
                place.markVerified(false); // 오류 시 운영중으로 가정
                active++;
            }
        }

        log.info("===== 보강 완료: 운영 {}건 / 폐업 {}건 / 이미지취득 {}건 =====",
                active, closed, imageUpdated);
    }

    /**
     * 교차검증 전체 재실행 — isVerified 리셋 후 enrich 배치 재실행.
     * 이름 유사도 검증 로직 변경 후 전체 데이터 재검증 시 사용.
     *
     * 수동 실행: POST /api/v1/admin/batch/re-verify
     */
    @Transactional
    @CacheEvict(value = {"places", "places:detail"}, allEntries = true)
    public void runReVerifyBatch() {
        int reset = placeRepository.resetAllVerified();
        log.info("===== 교차검증 플래그 리셋: {}건 → enrich 배치 재실행 =====", reset);
        runEnrichBatch();
    }

    /**
     * AI 별점 단독 재계산 — aiRating IS NULL 장소 대상.
     * enrich 배치에서 오류로 누락된 장소를 보완할 때 사용.
     * 이미 isVerified된 장소도 대상 포함 (교차검증 생략).
     *
     * 수동 실행: POST /api/v1/admin/batch/recalculate-ai-rating
     */
    @Transactional
    @CacheEvict(value = {"places", "places:detail"}, allEntries = true)
    public int recalculateAiRating() {
        List<Place> targets = placeRepository.findByAiRatingIsNull();
        log.info("===== AI 별점 재계산 시작: {}건 =====", targets.size());

        int updated = 0;
        for (Place place : targets) {
            try {
                NaverLocalImageService.BlogResult blog =
                        naverLocalImageService.fetchBlogData(place.getTitle());
                place.computeAiRating(blog.total(), blog.latestPostDate(), blog.descriptions());
                updated++;
                try { Thread.sleep(200); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); break;
                }
            } catch (Exception e) {
                log.warn("[AI점수오류] id={}, name={} — {}", place.getId(), place.getTitle(), e.getMessage());
            }
        }

        log.info("===== AI 별점 재계산 완료: {}건 =====", updated);
        return updated;
    }
}
