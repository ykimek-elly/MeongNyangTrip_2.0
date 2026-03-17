# 🐾 [멍냥트립 2.0] Core Environment & Cross-Check Specification

> **문서 유형**: 코어 인프라 명세서  
> **최종 수정**: 2026-03-09  
> **관련 문서**: [FE 개발 지침서](../../src/imports/fe-dev-guidelines.md), [Guidelines](../../guidelines/Guidelines.md)

본 문서는 **멍냥트립 2.0** 프로젝트의 **프론트엔드(React/Vite)** 와 **백엔드(Spring Boot 3.5.11)** 간 통신 규격, 인프라 설정, 보안 정책, 그리고 파트 간 교차 검증(Cross-Check) 절차를 정의합니다.

---

## 목차

1. [아키텍처 공통 응답 규격 (ApiResponse)](#1--아키텍처-공통-응답-규격-apiresponse)
2. [JDK 21 가상 스레드 활성화](#2--jdk-21-가상-스레드virtual-threads-활성화)
3. [보안 및 API 통신 (CORS & JWT 규격)](#3--보안-및-api-통신-cors--jwt-규격)
4. [스쿼드 교차 검증 매뉴얼](#4--스쿼드-교차-검증cross-check-매뉴얼)
5. [프로젝트 구조 및 로컬 DB 인프라 (Docker)](#5--프로젝트-구조-및-로컬-db-인프라-docker)

---

## 1. 🏗️ 아키텍처 공통 응답 규격 (ApiResponse)

프론트엔드(React)와 백엔드(Spring Boot 3.5.11) 간의 통신 시 **파편화된 구조를 방지**하기 위해, 모든 REST API 응답은 아래의 `ApiResponse<T>` 포맷으로 통일한다.

### 1.1 Java 구현체

```java
// src/main/java/com/team/meongnyang/common/ApiResponse.java
public record ApiResponse<T>(
    int status,
    String message,
    T data
) {
    // 성공 응답 (200 OK)
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }
    
    // 에러 응답 (GlobalExceptionHandler 연동용)
    public static <T> ApiResponse<T> error(int status, String message) {
        return new ApiResponse<>(status, message, null);
    }
}
```

### 1.2 응답 예시

#### ✅ 성공 응답

```json
{
  "status": 200,
  "message": "장소 목록 조회 성공",
  "data": [
    { "id": 1, "title": "서울숲 반려동물 구역", "rating": 4.9 },
    { "id": 2, "title": "남양주 물의정원", "rating": 4.8 }
  ]
}
```

#### ❌ 에러 응답

```json
{
  "status": 404,
  "message": "해당 장소를 찾을 수 없습니다.",
  "data": null
}
```

### 1.3 프론트엔드 타입 정의

```typescript
// src/app/types/api.ts
interface ApiResponse<T> {
  status: number;
  message: string;
  data: T | null;
}
```

> **💡 규칙**: 프론트엔드에서는 반드시 `response.data.data`로 실제 데이터에 접근한다.  
> (Axios 응답의 `data` → `ApiResponse`의 `data` 필드)

---

## 2. ⚡ JDK 21 가상 스레드(Virtual Threads) 활성화

톰캣(Tomcat)의 기본 스레드 풀 한계를 극복하고, **대량의 위치 기반(PostGIS) 동시 요청**을 처리하기 위해 Spring Boot 3.5.11의 가상 스레드를 기본으로 활성화한다.

### 2.1 설정

```yaml
# src/main/resources/application.yml
spring:
  threads:
    virtual:
      enabled: true  # JDK 21 Virtual Threads 활성화 (아키텍트 파트 설정)
```

### 2.2 왜 가상 스레드인가?

| 항목 | 기존 플랫폼 스레드 | 가상 스레드 (JDK 21) |
|------|-------------------|---------------------|
| **스레드 생성 비용** | 높음 (~1MB 메모리) | 매우 낮음 (~수 KB) |
| **동시 처리 수** | 수백 개 제한 | 수만 개 이상 |
| **I/O 블로킹 시** | 스레드 점유 → 병목 | 자동 양보 → 효율적 |
| **적합한 사용처** | CPU 집약 작업 | DB/API 호출 등 I/O 작업 |

> **⚠️ 주의**: `synchronized` 블록 내에서 I/O 호출 시 **핀닝(Pinning)** 현상이 발생할 수 있으므로, `ReentrantLock`으로 대체해야 한다.

### 2.3 멍냥트립에서의 활용 시나리오

- **장소 리스트 대량 조회**: PostGIS 기반 근거리 장소 검색 시 DB I/O 대기
- **AI 산책 가이드**: Spring AI(Gemini) API 호출 대기
- **실시간 톡**: WebSocket 연결 다수 유지

---

## 3. 🛡️ 보안 및 API 통신 (CORS & JWT 규격)

프론트엔드 파트(Axios)와 보안 파트(Spring Security) 간의 **교차 검증**을 위한 통신 규격이다.

### 3.1 인증 규격 요약

| 항목 | 값 |
|------|-----|
| **인증 방식** | JWT (JSON Web Token) |
| **Token 전달 헤더** | `Authorization: Bearer <token>` |
| **Token 저장소 (FE)** | `localStorage` (`accessToken` 키) |
| **CORS 허용 Origin** | `http://localhost:5173`, `https://meongnyangtrip.com` |
| **허용 Methods** | `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS` |
| **허용 Headers** | `Authorization`, `Content-Type` |
| **Credentials** | `true` (쿠키 및 토큰 인증 허용) |

### 3.2 백엔드 — CORS & Security 설정

```java
// src/main/java/com/team/meongnyang/config/SecurityConfig.java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of(
        "http://localhost:5173",        // 프론트엔드 로컬 (Vite 기본 포트)
        "https://meongnyangtrip.com"    // 운영 도메인
    ));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    configuration.setAllowCredentials(true); // 쿠키 및 토큰 인증 허용
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

### 3.3 프론트엔드 — Axios Interceptor 설정

```typescript
// src/app/api/axios.ts
import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL, // .env 파일에서 관리
  withCredentials: true,
});

// Request Interceptor: 모든 요청에 JWT 토큰 자동 첨부
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response Interceptor: 공통 에러 처리 (선택)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // 토큰 만료 시 로그인 페이지로 리다이렉트
      localStorage.removeItem('accessToken');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
```

### 3.4 환경 변수 설정

```bash
# .env (프론트엔드 루트)
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

```bash
# .env.production
VITE_API_BASE_URL=https://api.meongnyangtrip.com/api/v1
```

### 3.5 통신 흐름도

```
┌──────────────────┐     Authorization: Bearer <JWT>     ┌──────────────────────┐
│                  │  ─────────────────────────────────►  │                      │
│  React (Vite)    │         POST /api/v1/places          │  Spring Boot 3.5.11     │
│  localhost:5173  │                                      │  localhost:8080       │
│                  │  ◄─────────────────────────────────  │                      │
│  Axios + JWT     │   ApiResponse<T> { status, msg, data }│  Spring Security     │
│                  │                                      │  + JWT Filter         │
└──────────────────┘                                      └──────────────────────┘
```

---

## 4. 🔍 스쿼드 교차 검증(Cross-Check) 매뉴얼

각 초기 설정이 완료된 후, **파트장들은 다음의 시나리오를 통해 타 파트의 코드를 검증**해야 한다.

### 4.1 [네트워크 검증] Front ↔ Security

> **검증 대상**: 프론트엔드 Axios ↔ Spring Security JWT 인증

| 단계 | 상세 내용 |
|------|----------|
| **1** | 프론트엔드에서 `POST /api/v1/auth/login` API 호출 |
| **2** | 응답에서 JWT 토큰 수신 및 `localStorage`에 저장 확인 |
| **3** | 저장된 토큰을 `Authorization: Bearer <token>` 헤더에 담아 `GET /api/v1/mypage` 호출 |
| **4** | **200(OK)** 응답이 돌아오는지 확인 (401 Unauthorized가 아닌지) |
| **5** | **Postman**과 **브라우저** 양측에서 교차 검증 |

**✅ 성공 기준**: 두 환경 모두에서 `200 OK` + `ApiResponse` 규격 응답 수신

**❌ 실패 시 체크리스트**:
- CORS 설정에 `http://localhost:5173`이 포함되어 있는가?
- `withCredentials: true` 설정이 양측에 반영되어 있는가?
- JWT 토큰이 올바르게 생성/파싱되는가?

---

### 4.2 [예외 처리 검증] Data ↔ Architect

> **검증 대상**: DB 에러 발생 시 `GlobalExceptionHandler`의 정상 동작

| 단계 | 상세 내용 |
|------|----------|
| **1** | 의도적으로 DB 쿼리 에러를 발생시킨다 (잘못된 SQL, 존재하지 않는 테이블 조회 등) |
| **2** | `GlobalExceptionHandler`가 이를 캐치하는지 확인 |
| **3** | 프론트엔드에 전달되는 응답이 `ApiResponse` 규격을 준수하는지 검증 |

**✅ 성공 기준**: 아래 JSON이 정확히 내려와야 한다.

```json
{
  "status": 500,
  "message": "데이터베이스 오류 발생",
  "data": null
}
```

**❌ 실패 시 체크리스트**:
- `GlobalExceptionHandler`에 `@RestControllerAdvice`가 선언되어 있는가?
- `DataAccessException` 등 DB 예외를 잡는 핸들러가 등록되어 있는가?
- 응답이 HTML(Whitelabel Error Page)로 내려오지 않는가?

---

### 4.3 [성능 검증] BE ↔ QA

> **검증 대상**: 가상 스레드 적용 전후 성능 비교

| 단계 | 상세 내용 |
|------|----------|
| **1** | `spring.threads.virtual.enabled=false`로 설정 후 부하 테스트 실행 |
| **2** | `spring.threads.virtual.enabled=true`로 변경 후 동일 부하 테스트 실행 |
| **3** | 테스트 도구: **JMeter** 또는 **k6** |
| **4** | 테스트 시나리오: 장소 리스트 대량 조회 (`GET /api/v1/places?lat=...&lng=...`) |

**📊 측정 지표**:

| 지표 | 설명 |
|------|------|
| **평균 응답 시간** (Avg Response Time) | 요청 ~ 응답까지 평균 시간 (ms) |
| **처리량** (Throughput) | 초당 처리 건수 (req/s) |
| **에러율** (Error Rate) | 실패 응답 비율 (%) |
| **스레드 병목** | 스레드 풀 대기 큐 발생 여부 |

**✅ 성공 기준**: 가상 스레드 적용 후 **평균 응답 시간 개선** 및 **스레드 병목 해소**

---

### 4.4 [Zero-Legacy 코드 검증] BE ↔ 아키텍트

> **검증 대상**: 기존 JSP/Java 1.8 레거시 코드의 잔재 유입 여부 (PR 리뷰 단계)

| 단계 | 상세 내용 |
|------|----------|
| **1** | 백엔드 파트원이 `/backend` 하위에 신규 API 코드를 작성 후 PR(Pull Request) 생성 |
| **2** | 아키텍트 파트는 해당 코드가 기존 레거시 코드의 복사-붙여넣기인지 엄격히 검열 |
| **3** | 파편화된 파라미터(`@RequestParam` 남발)나 기존 MyBatis XML 로직이 남아있다면 즉각 반려(Reject) |

**✅ 성공 기준**: 모든 코드가 `docs/specs/`의 명세에 맞춰 **100% 순수 창조(Rewrite from scratch)** 되었을 때만 머지(Merge) 승인.

**❌ 절대 금지 사항**:
- 기존 코드의 Copy & Paste
- 레거시 DTO/Entity 혼용 구조 답습

---

## 5. 📂 프로젝트 구조 및 로컬 DB 인프라 (Docker)

본 프로젝트는 프론트엔드와 백엔드를 **단일 레포지토리에서 관리하는 모노레포(Monorepo) 방식**을 채택한다.

### 5.1 디렉토리 구조

```
MeongNyangTrip_2.0/
├── /                          # 프론트엔드 (React + Vite)
│   ├── src/
│   ├── package.json
│   └── vite.config.ts
├── /backend                   # 백엔드 (Spring Boot 3.5.11, JDK 21) — 완전 신규
│   ├── src/main/java/
│   ├── src/main/resources/
│   ├── build.gradle
│   └── docker-compose.yml     # 로컬 DB 환경 정의
├── /docs                      # 명세서, 설계 문서
└── README.md
```

### 5.2 로컬 DB 환경 — Docker Compose (강제)

> **⚠️ 절대 원칙**: 팀원 간 완벽히 동일한 DB 환경 구성을 위해, **로컬에 개별 설치를 금지**하며 반드시 Docker Compose를 사용해 일괄 구동한다.

```yaml
# /backend/docker-compose.yml
version: '3.8'
services:
  postgres:
    image: postgis/postgis:16-3.4
    container_name: meongnyang-db
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: meongnyang
      POSTGRES_USER: meongnyang
      POSTGRES_PASSWORD: meongnyang1234
    volumes:
      - pgdata:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    container_name: meongnyang-redis
    ports:
      - "6379:6379"

volumes:
  pgdata:
```

| 컨테이너 | 이미지 | 용도 |
|----------|--------|------|
| **PostgreSQL + PostGIS** | `postgis/postgis:16-3.4` | 공간 쿼리 및 위치 기반 서비스 |
| **Redis** | `redis:7-alpine` | 세션 관리 및 AI 추천 데이터 캐싱 |

### 5.3 로컬 환경 실행 명령어

```bash
# 1. DB 컨테이너 실행
cd backend
docker compose up -d

# 2. 백엔드 실행 (별도 터미널)
./gradlew bootRun

# 3. 프론트엔드 실행 (별도 터미널)
cd ..  # 프로젝트 루트로 이동
npm run dev
```

---

## 부록: 파트별 기술 스택 매핑

| 파트 | 담당 영역 | 주요 기술 |
|------|----------|----------|
| **프론트엔드** | UI/UX, 상태 관리, API 연동 | React, Vite, Tailwind CSS v4, Zustand, Axios |
| **백엔드** | REST API, 비즈니스 로직 | Spring Boot 3.5.11, JPA, PostGIS, JDK 21 |
| **보안** | 인증/인가, 토큰 관리 | Spring Security, JWT, OAuth2.0 |
| **AI** | 초개인화 추천, 산책 가이드 | Spring AI, Gemini API |
| **인프라** | 배포, 모니터링, 파일 저장 | AWS (EC2, S3, RDS), Docker |
| **데이터** | DB 설계, 공간 쿼리 | PostgreSQL, PostGIS |

---

> 📌 **본 명세서는 모든 파트가 공동으로 참조하는 기반 규격 문서입니다.**  
> 변경 사항 발생 시 반드시 팀 전체에 공유하고, 이 문서를 업데이트해야 합니다.
