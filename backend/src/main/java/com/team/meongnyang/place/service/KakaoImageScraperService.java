package com.team.meongnyang.place.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 카카오 공식 이미지 검색 API 서비스 (KakaoImageScraperService 대체).
 * 기존 카카오맵 HTML 파싱(Jsoup) 방식이 SPA 및 동적 웹으로 인해 차단되므로,
 * 공식 검색 API(v2/search/image)를 사용하여 대표 이미지를 확보합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoImageScraperService {

    /**
     * 장소명과 주소로 카카오 이미지 검색 API를 호출하여 가장 정확한 이미지 1개의 URL을 반환한다.
     * 
     * @param title   검색할 장소명
     * @param address 검색할 주소
     * @return 추출된 이미지 URL, 없거나 실패 시 null
     */
    public String scrapeImage(String title, String address) {
        log.debug("[Kakao Image Search] Disabled by user request to maintain image quality. Skipping image search for '{} {}^{}'", title, address);
        return null;
    }
}
