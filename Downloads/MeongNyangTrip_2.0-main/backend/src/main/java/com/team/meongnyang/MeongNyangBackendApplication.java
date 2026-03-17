package com.team.meongnyang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class MeongNyangBackendApplication {

	// TODO (팀원 A - 인프라/보안): SecurityConfig 및 JWT 필터 생성 (현재 Security 비활성화 상태이므로 최우선 작업 요망)
	// ✅ (팀원 B - 코어/데이터): Place 엔티티 PostGIS POINT 컬럼, ST_DWithin 검색, Redis 캐싱 구현 완료 (2026-03-14)
	// TODO (팀원 C - AI/연동): application.yml에 Spring AI (Gemini) 설정 추가 및 WalkGuideService 개발

	public static void main(String[] args) {
		SpringApplication.run(MeongNyangBackendApplication.class, args);
	}

}
