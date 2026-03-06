# 🐾 MeongNyangTrip 2.0: FE 개발 지침 및 컴포넌트 설계서

본 문서는 **멍냥트립 2.0** 고도화 프로젝트의 프론트엔드 개발 표준 및 페이지별 상세 컴포넌트 아키텍처를 정의합니다. 기존 JSP/JS 레거시를 **React/Tailwind CSS** 기반의 모던 스택으로 전환하고, AI 초개인화 기능을 통합하는 것을 목표로 합니다.

> **동기화 규칙**: 이 문서의 디자인 시스템 섹션은 코드 변경 시 반드시 함께 업데이트합니다. (`Guidelines.md` 참조)

---

## 1. 기술 스택 및 개발 표준 (Tech Stack)

* **Framework**: React (Vite 기반)
* **Styling**: Tailwind CSS v4 (Utility-First, CSS 변수 기반)
* **Icons**: Lucide React
* **Animation**: Motion (motion/react)
* **Map SDK**: Kakao Map SDK
* **State Management**: Zustand (전역 상태 관리 완료 - `useAppStore.ts`, `useFeedStore.ts`)
* **Routing**: React Router v7 Data Mode (적용 완료)
* **Data Fetching**: Axios & React Query (서버 상태 캐싱 및 동기화)

---

## 2. 디자인 시스템 (Design System)

### 2.1 Color Palette

#### 브랜드 컬러 (Brand Colors)

| 토큰명 | CSS 변수 | HEX | Tailwind 클래스 | 용도 |
|---|---|---|---|---|
| Primary | `--brand-primary` / `--primary` | `#E36394` | `text-primary`, `bg-primary`, `border-primary` | 브랜드 아이덴티티, 주요 버튼, CTA, 탭 활성 상태 |
| Primary Alt | `--brand-primary-alt` | `#ea4c89` | `text-brand-primary-alt`, `bg-brand-primary-alt` | Primary 대체 (호버 등) |
| Secondary | `--brand-secondary` / `--secondary` | `#FF4D8D` | `text-secondary`, `bg-secondary` | 플로팅 버튼(FAB), 포인트 액션 |
| Point | `--brand-point` | `#FFB300` | `text-brand-point`, `bg-brand-point`, `fill-brand-point` | 별점, 알림 뱃지, 배너 강조 텍스트 |

#### 시스템 컬러 (System Colors)

| 토큰명 | CSS 변수 | 값 | Tailwind 클래스 | 용도 |
|---|---|---|---|---|
| Background | `--background` | `#F1F1F1` | `bg-background` | Body 배경 |
| Foreground | `--foreground` | `#2c3e50` | `text-foreground` | 기본 텍스트 |
| Card | `--card` | `#FFFFFF` | `bg-card` | 카드, 컨테이너 배경 |
| Card Foreground | `--card-foreground` | `#2c3e50` | `text-card-foreground` | 카드 내 텍스트 |
| Muted | `--muted` | `#ececf0` | `bg-muted` | 비활성 배경 |
| Muted Foreground | `--muted-foreground` | `#717182` | `text-muted-foreground` | 보조 텍스트 |
| Accent | `--accent` | `#e9ebef` | `bg-accent` | 강조 배경 |
| Destructive | `--destructive` | `#d4183d` | `text-destructive`, `bg-destructive` | 삭제, 에러 |
| Border | `--border` | `rgba(0,0,0,0.1)` | `border-border` | 기본 테두리 |
| Input BG | `--input-background` | `#f3f3f5` | `bg-input-background` | 입력 필드 배경 |

#### 하드코딩 색상 (레거시 - 마이그레이션 완료)

| HEX | 마이그레이션 결과 | 비고 |
|---|---|---|
| `#FFB300` | `--brand-point` (`#FFB300`)로 통합 완료 | `text-brand-point`, `fill-brand-point`, `bg-brand-point` 사용 |
| `#FF5252` | `--destructive` (`#d4183d`)로 통합 완료 | `fill-destructive`, `text-destructive` 사용 |
| `#E36394` (하드코딩) | `--primary` 클래스로 통합 완료 | `text-primary`, `bg-primary` 사용 |
| `pink-50`, `pink-500` (Tailwind 기본) | `primary/10`, `primary`로 통합 완료 | CSS 변수 기반 클래스 사용 |

#### 투명도 활용 패턴

```
bg-primary/10   → 연한 핑크 배경 (태그, 뱃지, 배너)
bg-primary/20   → 중간 핑크 배경 (호버 상태)
shadow-primary/20 → 핑크 그림자 (활성 버튼)
```

---

### 2.2 Typography

#### 폰트 패밀리
```css
font-family: 'Pretendard', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
```
* CDN: `https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/static/pretendard.min.css`
* 모든 폰트 import는 `/src/styles/fonts.css`에서만 관리

#### 폰트 가중치 (Font Weight)
| 토큰 | CSS 변수 | 값 | 용도 |
|---|---|---|---|
| Bold | `--font-weight-bold` | `700` | 제목, 강조 텍스트, 버튼 |
| Medium | `--font-weight-medium` | `500` | 본문, 라벨, 버튼 |
| Normal | `--font-weight-normal` | `400` | 입력 필드, 일반 텍스트 |

#### 타이포그래피 스케일 (Base: 16px)

##### HTML 요소 기본 스타일 (`theme.css` @layer base 정의)

아래 요소들은 `theme.css`에서 기본 스타일이 정의되어 있으며, Tailwind 유틸리티 클래스로 오버라이드 가능합니다.

| 요소 | 크기 | 가중치 | line-height | 사용 예 |
|---|---|---|---|---|
| h1 | `1.5rem` (24px) | Bold (700) | 1.5 | 페이지 대제목, 로고 타이틀 |
| h2 | `1.25rem` (20px) | Bold (700) | 1.5 | 섹션 제목 |
| h3 | `1.125rem` (18px) | Bold (700) | 1.5 | 카드 제목, 서브 섹션 |
| h4 | `1rem` (16px) | Medium (500) | 1.5 | 소제목 |
| h5 | (기본 없음) | - | - | Tailwind 클래스로 직접 지정 |
| h6 | (기본 없음) | - | - | Tailwind 클래스로 직접 지정 |
| body/label/button | `1rem` (16px) | Medium (500) | 1.5 | 기본 텍스트, 라벨, 버튼 |
| input | `1rem` (16px) | Normal (400) | 1.5 | 입력 필드 |

##### Tailwind 유틸리티 폰트 사이즈 스케일 (9단계)

코드에서 사용하는 모든 폰트 사이즈는 아래 9단계 중 하나여야 합니다.

| 레벨 | 크기 | Tailwind 클래스 | 가중치 | 용도 | 사용 빈도 |
|---|---|---|---|---|---|
| **display** | `24px` | `text-2xl` / h1 기본 | Bold | 페이지 대제목 | 낮음 |
| **headline** | `20px` | `text-xl` / h2 기본 | Bold | 섹션 제목, 프로필명 | 낮음 |
| **title** | `18px` | `text-lg` / h3 기본 | Bold | 카드 제목, 서브헤더, CTA 버튼 | 보통 |
| **body** | `16px` | `text-base` / h4·body 기본 | Medium/Normal | 본문, 버튼, 입력 필드 | 높음 |
| **body-compact** | `15px` | `text-[15px]` | Bold/Medium | 컴팩트 카드 제목, 리스트 아이템명, 랭킹 타이틀 | 높음 |
| **small** | `14px` | `text-sm` | Medium/Bold | 보조 텍스트, 필터 라벨, 컴팩트 UI | 높음 |
| **caption** | `12px` | `text-xs` | Medium | 태그, 타임스탬프, 메타 정보, 필터 탭 | 매우 높음 |
| **sub-caption** | `11px` | `text-[11px]` | Normal/Bold | 컴팩트 리스트 콘텐츠, 인라인 보조 텍스트 | 보통 |
| **micro** | `10px` | `text-[10px]` | Bold/Medium | 뱃지, GNB 라벨, 아이콘 옆 수치, 최소 라벨 | 높음 |

##### 폰트 사이즈 규칙

> **최소 폰트 사이즈: `10px` (`text-[10px]`)**
> * `9px` 이하 사이즈는 **사용 금지**. 가독성 및 접근성 기준 미달.
> * `text-[15px]`은 `text-base`(16px)와 `text-sm`(14px) 사이의 중간 사이즈로, 16px Bold가 시각적으로 과한 컴팩트 카드/리스트 컨텍스트에서 사용합니다.
> * `text-[11px]`은 `text-xs`(12px)와 `text-[10px]`(10px) 사이의 중간 사이즈로, 컴팩트 UI(관리자 대시보드, 피드 인라인 텍스트 등)에서만 사용합니다.
> * 위 9단계 외의 임의 사이즈(예: `text-[13px]`, `text-[17px]`)는 **사용 금지**.

##### 사이즈 선택 가이드

```
"이 텍스트는 어떤 사이즈를 써야 할까?"

→ 사용자가 반드시 읽어야 하는 핵심 정보?     → body (16px) 이상
→ 카드/리스트에서 16px Bold가 과해 보일 때?  → body-compact (15px)
→ 보조적이지만 읽히는 텍스트?               → small (14px) 또는 caption (12px)
→ 공간이 좁고 밀도 높은 리스트 내부?          → sub-caption (11px)
→ 라벨/뱃지/아이콘 옆 수치?                 → micro (10px)
```

> **주의**: Tailwind의 `text-2xl`, `font-bold`, `leading-none` 등 폰트 관련 유틸리티는 사용자가 명시적으로 요청하지 않는 한 사용하지 않습니다. `theme.css`의 기본 스타일을 우선합니다.

---

### 2.3 Spacing & Layout

#### 앱 컨테이너
```
max-width: 600px (모바일 퍼스트)
min-height: 100vh
background: white
shadow: 0 0 30px rgba(227,99,148,0.1)  → 핑크 톤 그림자
```

#### 주요 간격 패턴
| 용도 | 클래스 | 값 |
|---|---|---|
| 페이지 좌우 패딩 | `px-4` ~ `px-5` | 16px ~ 20px |
| 섹션 간 마진 | `mb-8` ~ `mb-10` | 32px ~ 40px |
| 카드 내부 패딩 | `p-3` ~ `p-6` | 12px ~ 24px |
| 리스트 아이템 간격 | `gap-3` ~ `gap-4` | 12px ~ 16px |
| 하단 네비 여백 | `pb-24` | 96px (하단 탭바 겹침 방지) |
| 하단 네비 높이 | `h-[70px]` | 70px |

---

### 2.4 Border Radius (둥글기)

| 용도 | 클래스 | 값 |
|---|---|---|
| 소형 버튼/뱃지 | `rounded-full` | 완전 원형 |
| 카드 | `rounded-2xl` ~ `rounded-3xl` | 16px ~ 24px |
| 히어로 배너 | `rounded-[2rem]` | 32px |
| 검색박스 | `rounded-[2rem]` | 32px |
| 입력 필드 | `rounded-2xl` | 16px |
| 이미지 썸네일 | `rounded-2xl` | 16px |
| 카테고리 아이콘 | `rounded-3xl` | 24px |
| 필터 탭 | `rounded-full` | 완전 원형 |
| 하단 네비 상단 | `rounded-t-3xl` | 24px |
| 시스템 기본 | `--radius: 0.625rem` | 10px |

---

### 2.5 Shadow (그림자)

| 용도 | 클래스 / 값 |
|---|---|
| 카드 기본 | `shadow-sm` 또는 `shadow-[0_2px_15px_rgba(0,0,0,0.03)]` |
| 검색박스 | `shadow-[0_10px_30px_rgba(0,0,0,0.04)]` |
| 히어로 배너 | `shadow-xl` |
| 활성 버튼 | `shadow-lg` 또는 `shadow-md shadow-primary/20` |
| 하단 네비 | `shadow-[0_-5px_20px_rgba(0,0,0,0.03)]` |
| 앱 컨테이너 | `shadow-[0_0_30px_rgba(227,99,148,0.1)]` |
| 하단 예약바 | `shadow-[0_-5px_20px_rgba(0,0,0,0.05)]` |

---

### 2.6 Animation & Transition

#### Motion (motion/react) 패턴
| 패턴 | 설정 | 사용처 |
|---|---|---|
| 페이지 진입 | `initial={{ opacity: 0, y: 20 }}` → `animate={{ opacity: 1, y: 0 }}` | Home, List 등 메인 페이지 |
| 페이지 퇴장 | `exit={{ opacity: 0, y: -20 }}` | 페이지 전환 |
| 슬라이드 진입 | `initial={{ opacity: 0, x: 50 }}` → `animate={{ opacity: 1, x: 0 }}` | Detail 페이지 |
| 로그인 진입 | `initial={{ opacity: 0, scale: 0.95 }}` → `animate={{ opacity: 1, scale: 1 }}` | Login |
| 바텀시트 | `initial={{ y: "100%" }}` → `animate={{ y: 0 }}`, `spring damping: 25, stiffness: 300` | AI Chat |
| 리스트 아이템 | `delay: idx * 0.05`, `duration: 0.3` | 카테고리 베스트 랭킹 |

#### Tailwind Transition 패턴
```
transition-all         → 범용
transition-colors      → 색상 변화 (호버)
transition-transform   → 스케일 변화
duration-200 ~ 700     → 0.2초 ~ 0.7초
active:scale-95        → 터치 피드백 (누를 때 축소)
active:scale-[0.98]    → 미세 터치 피드백
hover:scale-105        → 호버 확대
group-hover:scale-105  → 그룹 호버 확대
```

---

### 2.7 Component Patterns (컴포넌트 패턴)

#### 2.7.1 Button (버튼)

**Primary Button (CTA)**
```
bg-primary text-white font-bold py-3.5~4 rounded-xl~2xl
shadow-md hover:bg-primary/90 active:scale-[0.98] transition-all
```
사용처: 로그인, 예약하기, 검색

**Filter Tab Button (필터 탭)**
```
활성: bg-primary text-white shadow-md shadow-primary/20 rounded-full px-3~4 py-1.5 text-xs font-bold
비활성: bg-gray-100 text-gray-500 rounded-full px-3~4 py-1.5 text-xs font-medium
```
사용처: 카테고리 필터, 정렬 탭

**Pill Button (알약형 버튼)**
```
flex items-center gap-2 bg-{color}-50 border border-{color}-100 rounded-full pl-2.5 pr-4 py-2
hover:bg-{color}-100 active:scale-95 transition-all
아이콘: w-7 h-7 bg-{color}-100 rounded-full
텍스트: text-xs font-bold text-gray-700 whitespace-nowrap
```
사용처: 개인 맞춤 서비스 (AI 산책 가이드, 펫 케어 시스템, 방문 인증 센터)

**Ghost Button (텍스트 버튼)**
```
text-xs text-gray-400~500 hover:text-gray-600~800 cursor-pointer
```
사용처: 더보기, 뒤로가기(텍스트형)

**Icon Button**
```
p-2 text-gray-500 hover:bg-gray-100 rounded-full transition-colors
```
사용처: 헤더 액션 (뒤로가기, 공유, 찜)

#### 2.7.2 Card (카드)

**Place Card (리스트 뷰)**
```
flex items-center bg-white border border-gray-100 p-2.5 rounded-3xl shadow-sm
이미지: w-[90px] h-[90px] rounded-2xl object-cover
active:scale-[0.98] transition-transform cursor-pointer
```

**Place Card (그리드 뷰)**
```
bg-white border border-gray-100 p-2.5 rounded-3xl shadow-sm flex flex-col
이미지: w-full aspect-square rounded-2xl object-cover
active:scale-[0.98] transition-transform cursor-pointer
```

**Ranking Card (베스트)**
```
bg-white p-3 rounded-2xl shadow-[0_2px_15px_rgba(0,0,0,0.03)]
border border-gray-100 flex gap-4 cursor-pointer
hover:border-primary/30 transition-colors
이미지: w-[80px] h-[80px] rounded-xl
순위 뱃지: absolute top-2 left-2 bg-primary text-white w-6 h-6 rounded-md text-xs font-bold
```

**Recommendation Card (횡스크롤)**
```
min-w-[140px] w-[140px] cursor-pointer active:scale-95
이미지: w-full aspect-square rounded-2xl
순위 뱃지: absolute top-2 left-2 bg-primary text-white w-6 h-6 rounded-md
```

#### 2.7.3 Badge & Tag (뱃지/태그)

**Category Tag**
```
bg-primary/10 text-primary border border-primary/30 text-[10px] font-medium px-2 py-0.5 rounded-full
```

**Hot Badge**
```
bg-brand-point text-black text-[10px] font-bold px-2.5 py-1 rounded-full
```

**Rating**
```
Star 아이콘: text-brand-point fill-brand-point
텍스트: text-xs font-bold text-gray-800
리뷰 수: text-xs text-gray-400~500
```

#### 2.7.4 Input (입력 필드)

**Search Input**
```
bg-gray-50 rounded-2xl px-3 flex items-center gap-2
아이콘: text-gray-400 size={18}
input: bg-transparent outline-none text-gray-800 placeholder:text-gray-400 font-medium text-sm
```

**Form Input**
```
w-full p-4 bg-white border border-gray-200 rounded-2xl shadow-sm
outline-none focus:border-primary transition-colors
```

#### 2.7.5 Navigation (네비게이션)

**Header**
```
sticky top-0 z-50 bg-white border-b border-gray-100 px-5 py-4
로고: Leaf 아이콘 text-primary + 텍스트 text-lg font-bold text-gray-800
```

**Bottom Navigation**
```
fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[600px]
h-[70px] bg-white/95 backdrop-blur-md border-t border-gray-100
rounded-t-3xl z-[1000] shadow-[0_-5px_20px_rgba(0,0,0,0.03)]
활성 탭: text-primary font-bold strokeWidth={2.5}
비활성 탭: text-[#b0bec5] strokeWidth={2}
라벨: text-[10px]
탭: 홈, 목록보기, AI산책, 지도, 라운지, 마이 (6개)
```

**Detail Header**
```
sticky top-0 z-50 bg-white/90 backdrop-blur-md border-b border-gray-100 h-14
뒤로가기 + 타이틀 + 액션(찜, 공유)
```

#### 2.7.6 Category Icon (카테고리 아이콘)

```
w-14 h-14 bg-primary/10 text-primary rounded-3xl flex items-center justify-center
hover → bg-primary text-white scale-105
active → scale-95
라벨: text-xs font-bold text-gray-500
```

#### 2.7.7 Bottom Sheet & Modal

**AI Chat Bottom Sheet**
```
fixed inset-0 bg-black/50 z-[2000]
시트: w-full max-w-[600px] h-[85vh] bg-white rounded-t-3xl
spring animation: damping 25, stiffness 300
```

**Chat Bubble**
```
봇: bg-white text-gray-800 self-start rounded-bl-sm border border-gray-100 shadow-sm
사용자: bg-primary text-white self-end rounded-br-sm
max-w-[80%] p-3 px-4 rounded-2xl text-sm leading-relaxed
```

#### 2.7.8 Banner

**Hero Banner**
```
h-[280px] rounded-[2rem] overflow-hidden shadow-xl
이미지: object-cover transition-transform duration-700 group-hover:scale-105
오버레이: bg-gradient-to-t from-black/70 via-black/20 to-transparent
```

**Promo Banner (이벤트)**
```
bg-primary/10 rounded-3xl p-5 flex items-center justify-between
제목: font-bold text-primary
아이콘: text-primary opacity-50
```

#### 2.7.9 Room Selection Card

```
border rounded-2xl overflow-hidden cursor-pointer transition-all
선택됨: border-primary ring-1 ring-primary bg-primary/10
미선택: border-gray-200
이미지: w-32 h-32 object-cover
하단: bg-gray-50 border-t border-gray-100
```

#### 2.7.10 Info Block

**Facility Tag**
```
flex items-center gap-1.5 bg-gray-50 px-3 py-1.5 rounded-full border border-gray-100
아이콘: size={14} text-gray-400
텍스트: text-xs text-gray-600
```

**Info Alert**
```
bg-blue-50 text-blue-600 text-xs p-3 rounded-lg flex gap-2
```

**Policy List**
```
text-sm text-gray-600 space-y-1 list-disc list-inside bg-gray-50 p-4 rounded-xl
```

---

### 2.8 Icon System

* **라이브러리**: `lucide-react`
* **기본 크기**: 아이콘 용도에 따라 다름

| 용도 | 크기 | strokeWidth |
|---|---|---|
| 네비게이션 탭 | `size={22}` | 활성 2.5, 비활성 2 |
| 카테고리 아이콘 | `size={24}` | 기본 |
| 헤더 액션 | `size={22}` | 기본 |
| 인라인 (텍스트 옆) | `size={10~16}` | 기본 |
| 검색 | `size={18~20}` | 기본, CTA는 2.5 |
| 대형 (배너/빈 상태) | `size={40~48}` | 기본 |
| Pill 버튼 내 아이콘 | `size={14}` | 기본 |

---

## 3. 페이지별 상세 컴포넌트 설계서 (Component Architecture)

Atomic Design 패턴의 변형을 적용하여 페이지(Pages), 유기체(Organisms), 분자(Molecules), 원자(Atoms) 단위로 컴포넌트를 설계합니다.

### 3.1 공통 컴포넌트 (Common Components)
모든 페이지에서 재사용되는 핵심 UI 요소입니다.
* `TopNavigation`: 상단 뒤로가기, 타이틀, 액션 버튼(공유, 찜)
* `BottomNavigation`: 하단 고정 GNB (홈, 목록보기, AI산책, 지도, 라운지, 마이)
* `PlaceCard`: 장소 목록에서 사용되는 공통 카드 UI (썸네일, 카테고리 태그, 별점, 주소)
* `CategoryBestRanking`: 카테고리별 베스트 TOP 5 탭 및 랭킹 리스트
* `AIChat`: AI 멍냥 플래너 바텀시트 (FAB 트리거)
* `FloatingActionButton` (FAB): 글쓰기, 지도 내 위치 찾기 등 주요 액션 버튼

### 3.2 홈 화면 (Home)
* **Route**: `/` (state: `home`)
* **주요 기능**: 큐레이션 및 검색, 주요 서비스 진입점.
* **섹션 순서**:
  1. Hero Banner (큐레이션 배너)
  2. Search Box (지역/날짜 통합 검색)
  3. Category Nav (6개: AI 추천산책, 플레이스, 스테이, 다이닝, 지도, 라운지)
  4. Recommendations (이번 주말 추천, 횡스크롤)
  5. 개인 맞춤 서비스 (Pill 버튼 3개: AI 산책 가이드, 펫 케어 시스템, 방문 인증 센터)
  6. Category Best Ranking (카테고리별 베스트 TOP 5)
  7. Promo Banner (이벤트 배너)

### 3.3 목록 및 검색 결과 (List)
* **Route**: `/list` (state: `list`)
* **주요 기능**: 필터링 및 장소 탐색.
* **Organisms (유기체)**:
  * `FilterBar`: 카테고리 탭(전체, 플레이스, 스테이, 다이닝) 및 뷰 전환(리스트/그리드) 토글
  * `SortDropdown`: 최신순, 별점순, 리뷰순 정렬 드롭다운
  * `PlaceListContainer`: 검색 결과 출력 영역 (무한 스크롤 적용)
  * `EmptyState`: 검색 결과가 없을 때 노출되는 안내 화면

### 3.4 상세 페이지 (Detail)
* **Route**: `/detail/:id` (state: `detail`)
* **주요 기능**: 장소 상세 정보, 리뷰 작성, 지도 모달, 예약/찜하기.
* **Organisms (유기체)**:
  * `PlaceImageGallery`: 상단 메인 이미지 (4:3 비율)
  * `PlaceInfoBlock`: 장소명, 별점, 리뷰 수
  * `FacilityTags`: 주차, 대형견 가능 여부 등 시설 정보 태그
  * `PlaceDescription`: 해시태그 기반 소개 + 더보기/접기 토글
  * `AddressBar`: 주소 표시 + "지도보기" 버튼 (클릭 시 바텀시트 모달 → 주소 복사/카카오맵/네이버지도 외부 연결)
  * `InstagramSection`: 인스타그램 바로가기 버튼
  * `ReviewSection`: 별점 선택·텍스트 입력 인라인 리뷰 작성 폼 및 리뷰 리스트
  * `BottomReservationBar`: 하단 고정 "예약 / 문의하기" CTA 버튼
* **데이터**: 장소별 상세 정보(주소, 태그, 설명, 인스타)는 `/src/app/data/detail-mock.ts`의 `DETAIL_EXTRA`로 분리

### 3.5 멍냥지도 (Map)
* **Route**: `/map` (state: `map`)
* **주요 기능**: 내 주변 탐색, 카테고리 필터, 마커 인터랙션.
* **Organisms (유기체)**:
  * `MapContainer`: 카카오맵 SDK 렌더링 영역 (클러스터링 적용)
  * `MapSearchOverlay`: 상단 검색바 및 해시태그 퀵 필터
  * `MapBottomSheet`: 마커 클릭 시 하단에서 올라오는 장소 요약 카드
  * `LocationFab`: 현재 내 위치로 지도 중심 이동 버튼

### 3.6 라운지 (커뮤니티) (Lounge)
* **Route**: `/lounge` (state: `lounge`)
* **주요 기능**: 보호자 간 정보 공유, 피드 및 실시간 톡.
* **상태 관리**: `useFeedStore` (Zustand) - 게시글 CRUD, 좋아요 토글, 댓글/DM 관리 / `useFriendStore` - 친구 관리, 공유 기록
* **Organisms (유기체)**:
  * `LoungeTabs`: '피드' / '실시간 톡' 전환 스티키 탭
  * `FeedView`: 인스타그램 스타일 피드 카드, 좋아요 토글, 댓글 인라인 입력, 공유 버튼
  * `ShareSheet`: 인스타그램 스타일 바텀시트 (친구 그리드 선택, 검색, 친구 추가, 링크 복사, 메시지 전송)
  * `WalkTalkView`: 실시간 산책 톡 카드 리스트
  * `WriteModal`: 피드 쓰기 / 톡 쓰기 선택 바텀 시트

### 3.7 마이페이지 (MyPage)
* **Route**: `/mypage` (state: `mypage` / `wish`)
* **주요 기능**: 프로필 관리, 찜 목록, 로그아웃.

### 3.8 AI 산책 가이드 (신규) (AI-Guide)
* **Route**: `/ai-walk-guide` (state: `ai-walk-guide`)
* **주요 기능**: 나이/품종/날씨 기반 초개인화 산책 추천.
* **초기값 전략**: 로그인+펫 등록 → 스토어 `pet` 데이터(breed, size, activity)로 자동 세팅 / 비로그인 → 수동 선택 UI (크기·활동량)
* **Organisms (유기체)**:
  * `PetProfileSelector`: 등록된 반려동물 중 대상 선택 (로그인 시 프로필 카드 표시)
  * `GuestInputSection`: 비로그인 시 크기·활동량 수동 선택 폼
  * `WeatherWidget`: 현재 위치의 실시간 날씨 및 산책 지수
  * `AiCommentCard`: Spring AI(Gemini)가 생성한 맞춤형 산책 조언
  * `RecommendedRouteMap`: 추천 산책로 및 주변 휴지통/배변 봉투 위치 맵

### 3.9 펫 케어 시스템 (신규) (Senior-Care)
* **Route**: `/senior-pet` (state: `senior-pet`)
* **주요 기능**: 반려동물 의료 데이터 모니터링 및 알림.
* **초기값 전략**: 로그인+펫 등록 필수 — 비로그인/미등록 시 안내 화면(로그인 유도 또는 등록 유도) 표시. 스토어 `pet` 데이터(name, breed, age, gender, size, weight)로 프로필 카드 자동 세팅
* **Organisms (유기체)**:
  * `PetProfileCard`: 스토어 연동 반려동물 프로필 (이름, 품종, 나이, 성별, 크기, 체중)
  * `HealthMetrics`: 체중·활동량·식욕·수면 건강 지표 그리드
  * `DailyChecklist`: 약 복용·케어·건강 체크 토글 리스트
  * `MedicationReminder`: 복약 알림 목록
  * `UpcomingSchedule`: 검진·접종 예정 일정
  * `HealthChart`: 예방접종 및 건강 검진 주기 시각화 타임라인
  * `AlertHistory`: 카카오 알림톡으로 수신된 건강 알림 내역 리스트
  * `HospitalRecommendCard`: PostGIS 기반 내 주변 5km 이내 최적 동물병원 추천

### 3.10 방문 인증 센터 (신규) (Verify)
* **Route**: `/verify` (state: `visit-checkin`)
* **주요 기능**: 장소 방문 신뢰도 확보를 위한 GPS 기반 인증.
* **Organisms (유기체)**:
  * `GpsCheckModule`: 현재 위치와 장소 좌표(반경 50m 이내) 대조 로직
  * `CameraUploader`: 실시간 사진 촬영 및 업로드 컴포넌트
  * `VerifiedBadge`: 인증 완료 시 부여되는 신뢰도 뱃지 UI

### 3.11 인증 화면 (Auth)
* **Login** (state: `login`): 아이디/비밀번호 입력, 소셜 로그인(예정)
* **Signup** (state: `signup`): 간편 가입 (이메일+비밀번호+닉네임 1단계 또는 Google/카카오 소셜 로그인) → 가입 후 온보딩으로 자동 이동
* **Onboarding** (state: `onboarding`): 4단계 온보딩 플로우 (웰컴 → 반려동물 등록 → 취향 선택 → 완료 축하). 모든 단계 스킵 가능. `PetProfileForm` 재사용, `hasCompletedOnboarding` 플래그로 1회만 표시
* **EditProfile** (state: `edit-profile`): 회원정보 수정 페이지 — 닉네임/이메일 변경, 비밀번호 변경, 계정 삭제(회원 탈퇴). 헤더에서 `{username}님` 클릭으로 진입. `updateProfile` 액션 사용
* **FindId** (state: `find-id`): 아이디 찾기
* **FindPassword** (state: `find-password`): 비밀번호 찾기
* 공통: 하단 네비게이션 숨김, 중앙 정렬 레이아웃

### 3.12 관리자 대시보드 (신규) (Admin)
* **Route**: `/admin` (state: `admin`)
* **주요 기능**: 피드 게시글/좋아요/댓글/DM 통계 모니터링 및 관리 (숨김/삭제/신고 처리)
* **진입점**: 마이페이지 > 관리자 대시보드 버튼
* **Organisms (유기체)**:
  * `OverviewTab`: 전체 통계 카드, 인기 게시글, 사용자 활동 요약
  * `PostsTab`: 게시글 목록 (검색/정렬), 좋아요/댓글/DM 상세, 숨김/삭제 관리
  * `CommentsTab`: 전체 댓글 타임라인
  * `DMsTab`: 전체 DM 목록 및 읽음 처리
* **UI 특징**: 다크 헤더, 하단 GNB 숨김, 전용 탭 네비게이션

### 3.13 404 페이지 (NotFound)
* **Route**: `*` (모든 미매칭 경로)
* **주요 기능**: 잘못된 URL 접근 시 안내 및 홈으로 이동 버튼 제공

---

## 4. 파일 구조 (File Structure)

```
src/
├── app/
│   ├── App.tsx                    # 메인 앱 (라우팅, 상태 관리)
│   ├── index.css                  # 앱 스타일 (scrollbar-hide 등)
│   ├── components/
│   │   ├── AIChat.tsx             # AI 채팅 바텀시트
│   │   ├── BottomNav.tsx          # 하단 네비게이션
│   │   ├── ErrorBoundary.tsx      # 전역 에러 바운더리 (RouteErrorFallback + GlobalErrorBoundary)
│   │   ├── PetProfileForm.tsx     # 반려동물 등록/수정 2단계 바텀시트 폼
│   │   ├── ShareSheet.tsx         # 인스타그램 스타일 공유 바텀시트
│   │   ├── CategoryBestRanking.tsx # 카테고리별 베스트 랭킹
│   │   ├── DatePickerPopup.tsx    # 날짜 선택 팝업
│   │   ├── figma/                 # Figma 연동 컴포넌트
│   │   └── ui/                    # shadcn/ui 기반 공통 UI
│   ├── data/
│   │   ├── places.ts              # 장소 목업 데이터
│   │   ├── detail-mock.ts         # 상세 페이지 목업 데이터
│   │   └── pet-care-helpers.ts    # 펫 케어 동적 데이터 생성 헬퍼 (체크리스트, 건강지표, 복약, 팁)
│   ├── store/
│   │   ├── useAppStore.ts          # 앱 전역 상태 (로그인, 찜, 경로, 반려동물 CRUD)
│   │   ├── useFeedStore.ts         # 피드 상태 (게시글, 좋아요, 댓글, DM)
│   │   └── useFriendStore.ts       # 친구 관리 상태 (친구 목록, 추천 친구, 공유 기록)
│   └── pages/
│       ├── Home.tsx               # 홈 화면
│       ├── List.tsx               # 목록 화면
│       ├── Detail.tsx             # 상세 화면
│       ├── MapSearch.tsx          # 지도 화면
│       ├── Lounge.tsx             # 라운지 화면
│       ├── MyPage.tsx             # 마이페이지
│       ├── AIWalkGuide.tsx        # AI 산책 가이드
│       ├── SeniorPetDashboard.tsx # 펫 케어 시스템 대시보드
│       ├── VisitCheckIn.tsx       # 방문 인증 센터
│       ├── AdminDashboard.tsx     # 관리자 대시보드
│       ├── Onboarding.tsx         # 온보딩 플로우 (회원가입 후)
│       ├── NotFound.tsx           # 404 페이지
│       ├── Login.tsx              # 로그인
│       ├── Signup.tsx             # 간편 회원가입 (이메일/소셜)
│       ├── EditProfile.tsx        # 회원정보 수정 (닉네임/이메일/비밀번호)
│       ├── FindId.tsx             # 아이디 찾기
│       └── FindPassword.tsx       # 비밀번호 찾기
├── imports/
│   └── fe-dev-guidelines.md       # 이 문서
└── styles/
    ├── fonts.css                  # 폰트 CDN import (Pretendard)
    ├── index.css                  # 스타일 import 통합
    ├── tailwind.css               # Tailwind 설정
    └── theme.css                  # 디자인 토큰 (CSS 변수 정의)
```

---

## 5. 프론트엔드 상태 관리 및 API 연동 원칙

1. **API 명세(Contract) 우선주의**: 백엔드(팀원 B, C)와 협의하여 노션에 Request/Response 규격을 확정한 후, 프론트엔드는 Mock Service Worker (MSW) 등을 활용해 UI 개발을 병렬로 진행합니다.
2. **낙관적 업데이트 (Optimistic UI)**: 라운지의 '좋아요', '찜하기' 등의 인터랙션은 사용자 경험을 위해 서버 응답을 기다리지 않고 UI를 먼저 변경한 뒤, 실패 시 롤백합니다.
3. **컴포넌트 지연 로딩 (Lazy Loading)**: 지도(`MapContainer`)나 용량이 큰 외부 라이브러리는 `React.lazy` 및 `Suspense`를 활용하여 초기 로딩 속도를 최적화합니다.

---

## 6. 현재 네비게이션 구조 (Navigation Map)

React Router v7 Data Mode를 통해 `src/app/routes.ts`에서 라우팅을 중앙 관리합니다.

```
Header 표시 + Bottom Nav 표시 (RootAdapter):
  홈(/) → 목록(/list) → 지도(/map) → 라운지(/lounge) → 마이페이지(/mypage)

Header/Bottom Nav 숨김:
  로그인(/login) → 회원가입(/signup) → 온보딩(/onboarding) → 아이디찾기(/find-id) → 비밀번호찾기(/find-password)
  상세(/detail/:id) → AI 산책 가이드(/ai-walk-guide) → 펫 케어 시스템(/senior-pet) → 방문 인증(/visit-checkin)
  회원정보 수정(/edit-profile) → 관리자 대시보드(/admin)
```

---

*최종 업데이트: 3단계 완료 후 (Zustand 상태 관리 통합, React Router 라우팅, Kakao Map SDK 연동)*