package com.team.meongnyang;

import com.team.meongnyang.place.entity.PlaceStatus;
import com.team.meongnyang.place.repository.PlaceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableAsync
public class MeongNyangBackendApplication {

	// TODO (팀원 A - 인프라/보안): SecurityConfig 및 JWT 필터 생성 (현재 Security 비활성화 상태이므로 최우선 작업 요망)
	// ✅ (팀원 B - 코어/데이터): Place 엔티티 PostGIS POINT 컬럼, ST_DWithin 검색, Redis 캐싱 구현 완료 (2026-03-14)
	// TODO (팀원 C - AI/연동): application.yml에 Spring AI (Gemini) 설정 추가 및 WalkGuideService 개발

	public static void main(String[] args) {
		SpringApplication.run(MeongNyangBackendApplication.class, args);
	}

	@Bean
	ApplicationRunner placeStatsLogger(PlaceRepository placeRepository, CacheManager cacheManager) {
		return args -> {
			// 시작 시 Redis 캐시 초기화 — 재시작 후 stale 캐시 방지
			cacheManager.getCacheNames().forEach(name -> {
				var cache = cacheManager.getCache(name);
				if (cache != null) cache.clear();
			});
			log.info("[Cache] 시작 시 전체 캐시 초기화 완료");

			long active   = placeRepository.countByStatus(PlaceStatus.ACTIVE);
			long pending  = placeRepository.countByStatus(PlaceStatus.PENDING);
			long rejected = placeRepository.countByStatus(PlaceStatus.REJECTED);
			long total    = placeRepository.count();
			log.info("┌─────────────────────────────────────────┐");
			log.info("│  [DB 장소 현황]  총 {}건", String.format("%-24s", total + "건") + "│");
			log.info("│  ACTIVE   : {}건", String.format("%-30s", active)   + "│");
			log.info("│  PENDING  : {}건", String.format("%-30s", pending)  + "│");
			log.info("│  REJECTED : {}건", String.format("%-30s", rejected) + "│");
			log.info("└─────────────────────────────────────────┘");
		};
	}
}
