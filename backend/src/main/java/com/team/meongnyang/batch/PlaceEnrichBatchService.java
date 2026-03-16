package com.team.meongnyang.batch;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.place.repository.PlaceRepository;
import com.team.meongnyang.place.service.GooglePlacesVerifyService;
import com.team.meongnyang.place.service.NaverLocalImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Google + 네이버 복합 속성 보강 배치 서비스.
 *
 * isVerified=false 레코드(한국문화정보원 수집분)를 대상으로
 *
 * [네이버 Local Search — 1단계]
 *   - 운영 교차검증: 검색결과 없으면 폐업 의심 표시
 *   - 이미지 취득: thumbnail → 이미지 검색 fallback 순서로 시도
 *
 * [Google Places — 2단계]
 *   - allowsDogs 취득 → tags에 "반려동물 동반 가능" 추가
 *   - businessStatus CLOSED_PERMANENTLY → 최종 폐업 판단 기준
 *   - 폐업 확정: Google 폐업 OR (네이버 미검색 + Google 정보없음) 동시 충족 시
 *
 * 수동 실행: POST /api/v1/admin/batch/enrich
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceEnrichBatchService {

    private final PlaceRepository placeRepository;
    private final GooglePlacesVerifyService googlePlacesVerifyService;
    private final NaverLocalImageService naverLocalImageService;

    @Transactional
    @CacheEvict(value = {"places", "places:detail"}, allEntries = true)
    public void runEnrichBatch() {
        List<Place> targets = placeRepository.findByIsVerified(false);
        log.info("===== Google+네이버 복합 보강 시작: {}건 =====", targets.size());

        int enriched = 0;
        int closed = 0;
        int noInfo = 0;
        int imageUpdated = 0;

        for (int i = 0; i < targets.size(); i++) {
            Place place = targets.get(i);
            try {
                // 1단계: 네이버 — 운영 교차검증 + 이미지 취득
                NaverLocalImageService.VerifyResult naverResult =
                        naverLocalImageService.verifyAndFetchImage(place.getTitle());

                if (naverResult.imageUrl() != null && place.getImageUrl() == null) {
                    place.enrichImageFromNaver(naverResult.imageUrl());
                    imageUpdated++;
                }

                // 2단계: Google — allowsDogs + 최종 폐업 판단
                GooglePlacesVerifyService.EnrichResult googleResult =
                        googlePlacesVerifyService.enrich(place.getTitle(), place.getAddress());

                // 폐업 확정: Google 명시적 폐업 OR 네이버+Google 동시 미검색
                boolean isClosed = googleResult.isClosed()
                        || (!naverResult.isActive() && !googleResult.allowsDogs());

                place.enrichFromGoogle(googleResult.allowsDogs(), isClosed,
                        googleResult.googleRating(), googleResult.googleReviewCount());

                if (isClosed) closed++;
                else if (googleResult.allowsDogs()) enriched++;
                else noInfo++;

                if ((i + 1) % 100 == 0) {
                    log.info("[보강 진행] {}/{} — 반려OK: {}, 폐업: {}, 정보없음: {}, 이미지: {}",
                            i + 1, targets.size(), enriched, closed, noInfo, imageUpdated);
                }

                // Naver API 속도 제한(10 req/s) 준수 — 1 iter당 최대 Naver 2회 호출
                try { Thread.sleep(250); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); break;
                }

            } catch (Exception e) {
                log.error("[보강오류] id={}, name={} — {}", place.getId(), place.getTitle(), e.getMessage());
                place.enrichFromGoogle(false, false, null, null);
                noInfo++;
            }
        }

        log.info("===== 보강 완료: 반려동물OK {}건 / 폐업 {}건 / 정보없음 {}건 / 이미지취득 {}건 =====",
                enriched, closed, noInfo, imageUpdated);
    }
}
