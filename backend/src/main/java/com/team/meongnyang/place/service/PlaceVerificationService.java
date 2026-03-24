package com.team.meongnyang.place.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 네이버 + 카카오 이중 교차검증 서비스 (파이프라인 2단계).
 *
 * [검증 기준]
 *   - Kakao: 장소명 유사도 + 좌표 500m 이내 일치 → 좌표 보정 + kakaoId 확보 + placeUrl 확보
 *   - Naver: 장소명 유사도 + 지역 컨텍스트 일치 → 폐업 여부 교차 검증
 *
 * [저장 결정]
 *   - 둘 다 통과 (BOTH)   → 확정 저장 (Kakao 좌표 + Kakao place_url)
 *   - Kakao만 통과 (KAKAO) → 확정 저장 (Kakao 좌표 + Kakao place_url)
 *   - Naver만 통과 (NAVER) → 확정 저장 (원본 좌표, placeUrl 없음)
 *   - 둘 다 실패 (REJECTED) → 폐업/오류 의심 → 저장 제외
 *
 * [dedup]
 *   kakaoId를 반환 → 배치 서비스에서 중복 저장 방지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceVerificationService {

    private final KakaoLocalVerifyService kakaoVerifyService;
    private final NaverLocalVerifyService naverVerifyService;

    /**
     * 통합 검증 결과.
     *
     * @param confirmed  저장 가능 여부 (false이면 배치에서 제외)
     * @param source     검증 통과 출처: "BOTH" | "KAKAO" | "NAVER" | "REJECTED"
     * @param placeUrl   Kakao에서 취득한 상세 웹페이지 URL (없으면 null)
     * @param lat        최종 위도 (Kakao 보정 좌표 우선)
     * @param lng        최종 경도 (Kakao 보정 좌표 우선)
     * @param kakaoId    카카오 장소 ID (중복 dedup 키, null 가능)
     */
    public record VerificationResult(
            boolean confirmed,
            String source,
            String placeUrl,
            double lat,
            double lng,
            String kakaoId
    ) {}

    /**
     * 네이버 + 카카오 이중 교차검증 실행.
     *
     * @param title   장소명
     * @param address 공공데이터 주소 (지역 컨텍스트 추출용)
     * @param lat     공공데이터 위도
     * @param lng     공공데이터 경도
     * @return VerificationResult
     */
    public VerificationResult verify(String title, String address, double lat, double lng) {

        // 1. Kakao 검증
        KakaoLocalVerifyService.VerifyResult kakao =
                kakaoVerifyService.verify(title, lat, lng);

        // 2. Naver 검증
        NaverLocalVerifyService.VerifyResult naver =
                naverVerifyService.verify(title, address);

        boolean kakaoOk = kakao.isActive();
        boolean naverOk = naver.isActive();

        if (kakaoOk && naverOk) {
            log.debug("[검증-BOTH] '{}'  kakaoId={}", title, kakao.kakaoId());
            return new VerificationResult(
                    true, "BOTH",
                    kakao.placeUrl(),
                    kakao.lat(), kakao.lng(),
                    kakao.kakaoId()
            );
        } else if (kakaoOk) {
            log.debug("[검증-KAKAO_ONLY] '{}'  kakaoId={}", title, kakao.kakaoId());
            return new VerificationResult(
                    true, "KAKAO",
                    kakao.placeUrl(),
                    kakao.lat(), kakao.lng(),
                    kakao.kakaoId()
            );
        } else if (naverOk) {
            log.debug("[검증-NAVER_ONLY] '{}'", title);
            return new VerificationResult(
                    true, "NAVER",
                    null,
                    lat, lng,
                    null
            );
        } else {
            log.debug("[검증-REJECTED] '{}' — 네이버·카카오 모두 미검색, 폐기", title);
            return new VerificationResult(false, "REJECTED", null, lat, lng, null);
        }
    }
}
