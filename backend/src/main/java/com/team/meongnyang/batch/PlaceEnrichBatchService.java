package com.team.meongnyang.batch;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.place.repository.PlaceRepository;
import com.team.meongnyang.place.service.KakaoLocalVerifyService;
import com.team.meongnyang.place.service.NaverLocalVerifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team.meongnyang.place.entity.PlaceStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 네이버 + 카카오 복합 교차검증 배치 서비스.
 *
 * isVerified=false 레코드를 대상으로:
 * [1단계 — 네이버 Local Search] 운영 교차검증
 * [2단계 — 카카오 Local API] 네이버 미검색 시 교차검증
 *
 * 수동 실행: POST /api/v1/admin/batch/enrich
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceEnrichBatchService {

    private final PlaceRepository placeRepository;
    private final NaverLocalVerifyService naverLocalVerifyService;
    private final KakaoLocalVerifyService kakaoLocalVerifyService;

    @Transactional
    @CacheEvict(value = {"places", "places:detail"}, allEntries = true)
    public void runEnrichBatch() {
        List<Place> targets = placeRepository.findByIsVerified(false);
        log.info("===== 네이버+카카오 교차검증 보강 시작: {}건 =====", targets.size());

        int closed = 0;
        int active = 0;

        for (int i = 0; i < targets.size(); i++) {
            Place place = targets.get(i);
            try {
                // 1단계: 네이버 — 운영 교차검증
                NaverLocalVerifyService.VerifyResult naverResult =
                        naverLocalVerifyService.verify(place.getTitle(), place.getAddress());

                boolean isClosed;
                if (naverResult.isActive()) {
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

                // AI 추천 별점 계산
                NaverLocalVerifyService.BlogResult blog =
                        isClosed ? NaverLocalVerifyService.BlogResult.empty()
                                 : naverLocalVerifyService.fetchBlogData(place.getTitle());
                place.computeAiRating(blog.total(), blog.latestPostDate(), blog.descriptions());

                if (isClosed) closed++; else active++;

                if ((i + 1) % 100 == 0) {
                    log.info("[보강 진행] {}/{} — 운영: {}, 폐업: {}",
                            i + 1, targets.size(), active, closed);
                }

                try { Thread.sleep(200); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); break;
                }

            } catch (Exception e) {
                log.error("[보강오류] id={}, name={} — {}", place.getId(), place.getTitle(), e.getMessage());
                place.markVerified(false);
                active++;
            }
        }

        log.info("===== 보강 완료: 운영 {}건 / 폐업 {}건 =====", active, closed);
    }

    @Transactional
    @CacheEvict(value = {"places", "places:detail"}, allEntries = true)
    public void runReVerifyBatch() {
        int reset = placeRepository.resetAllVerified();
        log.info("===== 교차검증 플래그 리셋: {}건 → enrich 배치 재실행 =====", reset);
        runEnrichBatch();
    }

    /**
     * 전체 ACTIVE 장소 AI 별점 + 블로그 키워드 강제 재계산 (blogCount 포함).
     * 기존 aiRating 값 유무와 관계없이 전체 재실행.
     */
    @Transactional
    @CacheEvict(value = {"places", "places:detail"}, allEntries = true)
    public int recalculateAllAiRating() {
        List<Place> targets = placeRepository.findByStatusOrderByCreatedAtDesc(PlaceStatus.ACTIVE);
        log.info("===== AI 별점 전체 재계산 시작: {}건 =====", targets.size());

        int updated = 0;
        for (Place place : targets) {
            try {
                NaverLocalVerifyService.BlogResult blog =
                        naverLocalVerifyService.fetchBlogData(place.getTitle());
                place.computeAiRating(blog.total(), blog.latestPostDate(), blog.descriptions());
                updated++;
                if (updated % 100 == 0) {
                    log.info("[전체재계산 진행] {}/{}", updated, targets.size());
                }
                try { Thread.sleep(200); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); break;
                }
            } catch (Exception e) {
                log.warn("[AI점수오류] id={}, name={} — {}", place.getId(), place.getTitle(), e.getMessage());
            }
        }

        log.info("===== AI 별점 전체 재계산 완료: {}건 =====", updated);
        return updated;
    }

    @Transactional
    @CacheEvict(value = {"places", "places:detail"}, allEntries = true)
    public int recalculateAiRating() {
        List<Place> targets = placeRepository.findByAiRatingIsNull();
        log.info("===== AI 별점 재계산 시작: {}건 =====", targets.size());

        int updated = 0;
        for (Place place : targets) {
            try {
                NaverLocalVerifyService.BlogResult blog =
                        naverLocalVerifyService.fetchBlogData(place.getTitle());
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

    /**
     * ACTIVE 장소 10건 카카오 재검증 — dry-run (DB 변경 없음).
     *
     * 각 장소에 대해 KakaoLocalVerifyService.verify() 를 실행하고
     * 결과를 JSON-compatible Map 리스트로 반환한다.
     * 실제 상태 변경은 하지 않으므로 안전하게 테스트 가능.
     *
     * @return 검증 결과 요약 Map (total / wouldReject / wouldKeep / results)
     */
    public Map<String, Object> reVerifySample() {
        List<Place> samples = placeRepository.findRandomActiveSample(10);
        log.info("===== 재검증 샘플 시작 (dry-run): {}건 =====", samples.size());

        List<Map<String, Object>> results = new ArrayList<>();
        int wouldReject = 0;
        int wouldKeep = 0;

        for (Place place : samples) {
            NaverLocalVerifyService.VerifyResult naver =
                    naverLocalVerifyService.verify(place.getTitle(), place.getAddress());
            KakaoLocalVerifyService.VerifyResult kakao =
                    kakaoLocalVerifyService.verify(place.getTitle(), place.getLatitude(), place.getLongitude());

            // 카카오 API 한도 초과(kakaoId=null이면 예외 반환) 시 네이버 결과만 사용
            boolean kakaoValid = kakao.isActive() && kakao.kakaoId() != null;
            boolean keep = naver.isActive() || kakaoValid;
            if (!keep) wouldReject++; else wouldKeep++;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", place.getId());
            row.put("title", place.getTitle());
            row.put("address", place.getAddress());
            row.put("verdict", !keep ? "REJECT" : "KEEP");
            row.put("naverFound", naver.isActive());
            row.put("kakaoFound", kakaoValid);
            row.put("kakaoId", kakao.kakaoId());
            results.add(row);

            log.info("[샘플검증] id={} '{}' → 네이버:{} 카카오:{} → {}", place.getId(), place.getTitle(),
                      naver.isActive() ? "✅" : "❌",
                    kakaoValid ? "✅" : "❌",
                    !keep ? "❌ REJECT" : "✅ KEEP");

            try { Thread.sleep(200); } catch (InterruptedException ie) {
                Thread.currentThread().interrupt(); break;
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", samples.size());
        summary.put("wouldKeep", wouldKeep);
        summary.put("wouldReject", wouldReject);
        summary.put("results", results);

        log.info("===== 재검증 샘플 완료 (dry-run): KEEP {}건 / REJECT {}건 =====", wouldKeep, wouldReject);
        return summary;
    }

    /**
     * 전체 ACTIVE 장소 네이버 재검증 + DB 반영.
     * 네이버에서 미검색(items 없음)인 장소 → status = REJECTED.
     * 100건마다 로그 출력. 완료 후 요약 반환.
     */
    @Transactional
    @CacheEvict(value = {"places", "places:detail"}, allEntries = true)
    public Map<String, Object> reVerifyAllAndApply() {
        List<Place> all = placeRepository.findByStatusOrderByCreatedAtDesc(PlaceStatus.ACTIVE);
        log.info("===== 전체 재검증 시작: {}건 =====", all.size());

        int kept = 0, rejected = 0;
        List<Map<String, Object>> rejectedList = new ArrayList<>();

        for (int i = 0; i < all.size(); i++) {
            Place place = all.get(i);
            try {
                NaverLocalVerifyService.VerifyResult naver =
                        naverLocalVerifyService.verify(place.getTitle(), place.getAddress());

                if (!naver.isActive()) {
                    place.markRejectedByBatch();
                    rejected++;
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", place.getId());
                    row.put("title", place.getTitle());
                    row.put("address", place.getAddress());
                    rejectedList.add(row);
                } else {
                    kept++;
                }

                if ((i + 1) % 100 == 0) {
                    log.info("[재검증 진행] {}/{} — KEEP: {}, REJECT: {}", i + 1, all.size(), kept, rejected);
                }

                Thread.sleep(100);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("[재검증오류] id={} '{}' — {}", place.getId(), place.getTitle(), e.getMessage());
                kept++; // 오류 시 안전하게 KEEP 처리
            }
        }

        log.info("===== 전체 재검증 완료 — KEEP: {}건 / REJECT: {}건 =====", kept, rejected);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", all.size());
        summary.put("kept", kept);
        summary.put("rejected", rejected);
        summary.put("rejectedPlaces", rejectedList);
        return summary;
    }

    /**
     * 이름 끝에 숫자가 붙은 ACTIVE 장소 Naver 재검증 — dry-run (DB 변경 없음).
     *
     * 예: "준하우스3" → Naver가 "준하우스"만 매칭 → 숫자 접미사가 실제 다른 업체임을 감지.
     * suspect=true 인 장소는 수동 검토 후 /admin/places/{id}/reject 로 처리.
     */
    public Map<String, Object> scanNumericNameSuspects() {
        List<Place> suspects = placeRepository.findActiveByTitleWithDigit();
        log.info("===== 숫자 접미사 장소 스캔 시작: {}건 =====", suspects.size());

        List<Map<String, Object>> results = new ArrayList<>();
        int flagged = 0;
        int clean = 0;

        for (Place place : suspects) {
            NaverLocalVerifyService.TitledVerifyResult result =
                    naverLocalVerifyService.verifyWithTitle(place.getTitle(), place.getAddress());
            String naverTitle = result.naverTitle();
            boolean isSuspect = false;
            String reason = null;

            if (!result.isActive()) {
                isSuspect = true;
                reason = "네이버 미검색";
            } else if (naverTitle != null) {
                String dbNorm    = place.getTitle().replaceAll("[\\s\\-_()（）·]", "").toLowerCase();
                String naverNorm = naverTitle.replaceAll("[\\s\\-_()（）·]", "").toLowerCase();

                if (dbNorm.contains(naverNorm) && !dbNorm.equals(naverNorm)) {
                    // Naver 결과가 DB보다 짧음 → 숫자 접미사 미포함 추정
                    isSuspect = true;
                    reason = "Naver 매칭 이름이 DB보다 짧음 (숫자 접미사 미포함 추정)";
                } else {
                    // 핵심 이름(후행 숫자/점 제거)이 Naver 제목에 없으면 다른 업체로 추정
                    // 예: "준하우스3" → core="준하우스" → "홍대 준 게스트하우스"에 미포함 → suspect
                    String coreNorm = place.getTitle().replaceAll("[0-9.]+$", "").strip()
                            .replaceAll("[\\s\\-_()（）·]", "").toLowerCase();
                    if (!coreNorm.isEmpty() && !naverNorm.contains(coreNorm)) {
                        isSuspect = true;
                        reason = "Naver 매칭 업체명에 핵심 이름 미포함 (다른 업체 추정)";
                    }
                }
            }

            if (isSuspect) flagged++; else clean++;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", place.getId());
            row.put("title", place.getTitle());
            row.put("address", place.getAddress());
            row.put("naverFound", result.isActive());
            row.put("naverTitle", naverTitle);
            row.put("suspect", isSuspect);
            row.put("reason", reason);
            results.add(row);

            log.info("[숫자접미사검증] id={} '{}' → Naver='{}' → {}",
                    place.getId(), place.getTitle(), naverTitle,
                    isSuspect ? "⚠️ SUSPECT" : "✅ OK");

            try { Thread.sleep(200); } catch (InterruptedException ie) {
                Thread.currentThread().interrupt(); break;
            }
        }

        log.info("===== 숫자 접미사 스캔 완료: 의심 {}건 / 정상 {}건 =====", flagged, clean);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", suspects.size());
        summary.put("flagged", flagged);
        summary.put("clean", clean);
        summary.put("results", results);
        return summary;
    }

    /**
     * 특정 장소 단건 카카오 재검증 — dry-run (DB 변경 없음).
     */
    public Map<String, Object> reVerifyOne(Long id) {
        Place place = placeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("장소 없음: id=" + id));

        NaverLocalVerifyService.VerifyResult naver =
                naverLocalVerifyService.verify(place.getTitle(), place.getAddress());
        KakaoLocalVerifyService.VerifyResult kakao =
                kakaoLocalVerifyService.verify(place.getTitle(), place.getLatitude(), place.getLongitude());

        boolean kakaoValid = kakao.isActive() && kakao.kakaoId() != null;
        boolean keep = naver.isActive() || kakaoValid;
        log.info("[단건검증] id={} '{}' → 네이버:{} 카카오:{} → {}",
                id, place.getTitle(),
                naver.isActive() ? "✅" : "❌",
                kakaoValid ? "✅" : "❌(한도초과 or 미검색)",
                keep ? "✅ KEEP" : "❌ REJECT");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", place.getId());
        result.put("title", place.getTitle());
        result.put("address", place.getAddress());
        result.put("currentStatus", place.getStatus().name());
        result.put("verdict", keep ? "KEEP" : "REJECT");
        result.put("naverFound", naver.isActive());
        result.put("kakaoFound", kakaoValid);
        result.put("kakaoId", kakao.kakaoId());
        result.put("kakaoPlaceUrl", kakao.placeUrl());
        return result;
    }
}
