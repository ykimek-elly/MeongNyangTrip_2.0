# MeongNyangTrip AI Engine
### 임시 README.md

## 프로젝트 소개

반려견 정보, 사용자 정보, 실시간 날씨, 장소 데이터, RAG 검색 결과를 기반으로 개인 맞춤 추천을 생성하는 AI 추천 시스템

## 기술 스택

- Spring Boot
- PostgreSQL + PostGIS
- Redis
- Gemini
- Spring AI

## 추천 흐름

1. 사용자 조회
2. 반려견 조회
3. 날씨 조회
4. 장소 후보 조회
5. 점수 계산
6. RAG 검색
7. Gemini 응답 생성
8. 로그 저장

## 담당 역할

- 추천 오케스트레이션
- 점수 계산 로직
- Weather Cache
- RAG 검색 구조 설계