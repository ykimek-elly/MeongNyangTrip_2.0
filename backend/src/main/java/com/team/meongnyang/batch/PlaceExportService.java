package com.team.meongnyang.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 오프라인 AI 보강을 위한 데이터 내보내기/가져오기 서비스 (파이프라인 3, 4단계).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceExportService {

    private final PlaceRepository placeRepository;
    private final ObjectMapper objectMapper;

    private static final String EXPORT_DIR = "exports";
    private static final String EXPORT_FILE = "place_enrich_input.json";
    private static final String IMPORT_FILE = "place_enrich_output.json";

    /**
     * [3단계] AI 보강용 JSON 내보내기.
     * 검증된(isVerified=true) 장소 중 설명이 부족하거나 보강이 필요한 데이터를 추출합니다.
     */
    @Transactional(readOnly = true)
    public String exportToEnrichJson() throws IOException {
        List<Place> targets = placeRepository.findByIsVerified(true);
        log.info("===== AI 보강용 데이터 추출 시작: {}건 =====", targets.size());

        List<Map<String, Object>> exportData = targets.stream()
                .map(p -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", p.getId());
                    map.put("title", p.getTitle());
                    map.put("address", p.getAddress());
                    map.put("category", p.getCategory());
                    map.put("current_overview", p.getOverview() != null ? p.getOverview() : "");
                    map.put("pet_info", String.format("실내:%s / 안내:%s", p.getChkPetInside(), p.getPetTurnAdroose()));
                    map.put("homepage", p.getHomepage() != null ? p.getHomepage() : "");
                    return map;
                })
                .collect(Collectors.toList());

        Path path = Paths.get(EXPORT_DIR);
        if (!Files.exists(path)) Files.createDirectories(path);

        File file = new File(EXPORT_DIR, EXPORT_FILE);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, exportData);

        log.info("===== 추출 완료: {} =====", file.getAbsolutePath());
        return file.getAbsolutePath();
    }

    /**
     * [4단계] AI 보강 완료된 JSON 가져오기.
     * 외부에서 생성된 place_enrich_output.json 파일을 읽어 DB를 업데이트합니다.
     */
    @SuppressWarnings("unchecked")
    @Transactional
    public int importFromEnrichedJson() throws IOException {
        File file = new File(EXPORT_DIR, IMPORT_FILE);
        if (!file.exists()) {
            throw new IOException("가져올 파일이 없습니다: " + file.getAbsolutePath());
        }

        List<Map<String, Object>> data = objectMapper.readValue(file, List.class);
        log.info("===== AI 보강 데이터 가져오기 시작: {}건 =====", data.size());

        int updatedCount = 0;
        for (Map<String, Object> item : data) {
            Object idObj = item.get("id");
            if (idObj == null) continue;
            
            Long id = Long.valueOf(idObj.toString());
            String overview = (String) item.get("overview");
            String petFacility = (String) item.get("pet_facility");
            String petPolicy = (String) item.get("pet_policy");
            String operatingHours = (String) item.get("operating_hours");
            String operationPolicy = (String) item.get("operation_policy");

            placeRepository.findById(id).ifPresent(place -> {
                place.updateEnrichedData(overview, petFacility, petPolicy, operatingHours, operationPolicy);
                log.debug("[보강수신] id={} | overview_len={}", id, overview != null ? overview.length() : 0);
            });
            updatedCount++;
        }

        log.info("===== 가져오기 완료: {}건 업데이트 =====", updatedCount);
        return updatedCount;
    }
}
