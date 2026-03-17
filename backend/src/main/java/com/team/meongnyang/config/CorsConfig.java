package com.team.meongnyang.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * CORS 설정.
 * 프론트엔드(localhost:5173) 및 운영 도메인의 요청을 허용한다.
 *
 * @see docs/specs/core-setup.md 3.2장 참조
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
            "http://localhost:5173",        // 프론트엔드 로컬 (Vite 기본 포트)
            "http://localhost:3000",        // 프론트엔드 로컬 (대체)
            "http://54.180.22.22",          // EC2 프론트엔드
            "http://54.180.22.22:8080",     // EC2 Swagger
            "https://meongnyangtrip.com"    // 운영 도메인
            "https://meongnyangtrip.duckdns.org"  // 임시 도메인

        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return new CorsFilter(source);
    }
}