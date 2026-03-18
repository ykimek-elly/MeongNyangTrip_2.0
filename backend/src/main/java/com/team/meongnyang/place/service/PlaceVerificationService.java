package com.team.meongnyang.place.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 네이버 + 카카오 이중 교차검증 서비스 (파이프라인 2단계).
 *
 * [검증 기준]
 *   - Kakao: 장소명 유사도 + 좌표 500m 이내 일치 → 좌표 보정 + kakaoId 확보
 *   - Naver: 장소명 유사도 + 지역 컨텍스트 일치 → 대표 이미지 확보
 *
 * [저장 결정]
 *   - 둘 다 통과 (BOTH)   → 확정 저장 (Kakao 좌표 + Naver 이미지)
 *   - Kakao만 통과 (KAKAO) → 확정 저장 (Kakao 좌표, 이미지 없음)
 *   - Naver만 통과 (NAVER) → 확정 저장 (원본 좌표 + Naver 이미지)
 *   - 둘 다 실패 (REJECTED) → 폐업/오류 의심 → 저장 제외
 *
 * [dedup]
 *   kakaoId를 반환 → 배치 서비스에서 중복 저장 방지 (KTO + KCISA 교차)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceVerificationService {

    private final KakaoLocalVerifyService kakaoVerifyService;
    private final NaverLocalImageService naverImageService;

    /**
     * 통합 검증 결과.
     *
     * @param confirmed  저장 가능 여부 (false이면 배치에서 제외)
     * @param source     검증 통과 출처: "BOTH" | "KAKAO" | "NAVER" | "REJECTED"
     * @param imageUrl   Naver에서 취득한 대표 이미지 URL (없으면 null)
     * @param lat        최종 위도 (Kakao 보정 좌표 우선)
     * @param lng        최종 경도 (Kakao 보정 좌표 우선)
     * @param kakaoId    카카오 장소 ID (중복 dedup 키, null 가능)
     */
    public record VerificationResult(
            boolean confirmed,
            String source,
            String imageUrl,
            double lat,
            double lng,
            String kakaoId
    ) {}

    /**
     * 네이버 + 카카오 이중 교차검증 실행.
     *
     * 각 포털 검증 결과를 조합해 저장 여부와 최적 좌표/이미지를 결정한다.
     *
     * @param title   장소명
     * @param address 공공데이터 주소 (지역 컨텍스트 추출용)
     * @param lat     공공데이터 위도
     * @param lng     공공데이터 경도
     * @return VerificationResult
     */
    public VerificationResult verify(String title, String address, double lat, double lng) {

        // 1. Kakao 검증: 이름 유사도 + 500m 반경 좌표 일치
        KakaoLocalVerifyService.VerifyResult kakao =
                kakaoVerifyService.verify(title, lat, lng);

        // 2. Naver 검증: 이름 유사도 + 지역 컨텍스트 + 이미지 취득
        NaverLocalImageService.VerifyResult naver =
                naverImageService.verifyAndFetchImage(title, address);

        boolean kakaoOk = kakao.isActive();
        boolean naverOk = naver.isActive();

        if (kakaoOk && naverOk) {
            log.debug("[검증-BOTH] '{}'  kakaoId={}", title, kakao.kakaoId());
            return new VerificationResult(
                    true, "BOTH",
                    naver.imageUrl(),
                    kakao.lat(), kakao.lng(),
                    kakao.kakaoId()
            );
        } else if (kakaoOk) {
            log.debug("[검증-KAKAO_ONLY] '{}'  kakaoId={}", title, kakao.kakaoId());
            return new VerificationResult(
                    true, "KAKAO",
                    null,
                    kakao.lat(), kakao.lng(),
                    kakao.kakaoId()
            );
        } else if (naverOk) {
            log.debug("[검증-NAVER_ONLY] '{}'", title);
            return new VerificationResult(
                    true, "NAVER",
                    naver.imageUrl(),
                    lat, lng,
                    null
            );
        } else {
            log.debug("[검증-REJECTED] '{}' — 네이버·카카오 모두 미검색, 폐기", title);
            return new VerificationResult(false, "REJECTED", null, lat, lng, null);
        }
    }
}
