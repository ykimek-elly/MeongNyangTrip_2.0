package com.team.meongnyang.place.service;

import com.team.meongnyang.place.dto.PlaceRequestDto;
import com.team.meongnyang.place.dto.PlaceResponseDto;
import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 장소(Place) 비즈니스 로직 서비스.
 * Controller와 Repository 사이에서 비즈니스 규칙을 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    private final PlaceRepository placeRepository;

    /**
     * 위치 기반 근거리 장소 검색 (PostGIS ST_DWithin).
     * 결과는 Redis에 1시간 캐싱. 배치 실행 시 자동 무효화.
     */
    @Cacheable(value = "places", key = "#lat + '_' + #lng + '_' + #radius + '_' + (#category ?: 'ALL')")
    public List<PlaceResponseDto> getPlacesNearby(double lat, double lng, int radius, String category) {
        List<Place> places = category != null
                ? placeRepository.findNearbyByCategory(lat, lng, radius, category, 50)
                : placeRepository.findNearby(lat, lng, radius, 50);
        return places.stream().map(PlaceResponseDto::from).toList();
    }

    /** 장소 목록 조회 (카테고리/키워드 필터 — 위치 정보 없을 때 fallback) */
    public List<PlaceResponseDto> getPlaces(String category, String keyword) {
        List<Place> places;

        if (category != null && keyword != null) {
            places = placeRepository.findByCategoryAndTitleContainingIgnoreCase(category, keyword);
        } else if (category != null) {
            places = placeRepository.findByCategory(category);
        } else if (keyword != null) {
            places = placeRepository.findByTitleContainingIgnoreCase(keyword);
        } else {
            places = placeRepository.findAll();
        }

        return places.stream()
            .map(PlaceResponseDto::from)
            .toList();
    }

    /** 장소 상세 조회 (6시간 캐싱) */
    @Cacheable(value = "places:detail", key = "#id")
    public PlaceResponseDto getPlace(Long id) {
        Place place = placeRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("해당 장소를 찾을 수 없습니다. (id: " + id + ")"));
        return PlaceResponseDto.from(place);
    }

    /** 장소 등록 */
    @Transactional
    public PlaceResponseDto createPlace(PlaceRequestDto request) {
        Place place = Place.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .address(request.getAddress())
            .latitude(request.getLatitude())
            .longitude(request.getLongitude())
            .category(request.getCategory())
            .imageUrl(request.getImageUrl())
            .phone(request.getPhone())
            .tags(request.getTags())
            .build();

        Place saved = placeRepository.save(place);
        return PlaceResponseDto.from(saved);
    }

    /** 장소 수정 (상세 캐시 무효화) */
    @Transactional
    @CacheEvict(value = "places:detail", key = "#id")
    public PlaceResponseDto updatePlace(Long id, PlaceRequestDto request) {
        Place place = placeRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("해당 장소를 찾을 수 없습니다. (id: " + id + ")"));

        place.update(
            request.getTitle(),
            request.getDescription(),
            request.getAddress(),
            request.getLatitude(),
            request.getLongitude(),
            request.getCategory(),
            request.getImageUrl(),
            request.getPhone(),
            request.getTags()
        );

        return PlaceResponseDto.from(place);
    }

    /** 장소 삭제 (캐시 전체 무효화) */
    @Transactional
    @CacheEvict(value = {"places", "places:detail"}, allEntries = true)
    public void deletePlace(Long id) {
        if (!placeRepository.existsById(id)) {
            throw new NoSuchElementException("해당 장소를 찾을 수 없습니다. (id: " + id + ")");
        }
        placeRepository.deleteById(id);
    }
}
