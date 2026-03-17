# 멍냥트립 2.0 - AI 개발 가이드라인

## 핵심 규칙

* 모든 색상은 CSS 변수 기반 Tailwind 클래스를 사용할 것 (`text-primary`, `bg-primary` 등). 하드코딩 금지.
* 컴포넌트 작성 시 반드시 `/src/imports/fe-dev-guidelines.md`의 디자인 시스템 섹션을 참조할 것.
* 새로운 컴포넌트/패턴 추가 또는 기존 디자인 토큰 변경 시, `/src/imports/fe-dev-guidelines.md`의 디자인 시스템 섹션도 반드시 동기화 업데이트할 것.
* 레이아웃은 flexbox/grid 기반. absolute positioning은 최소화.
* 파일 크기를 작게 유지하고, 헬퍼 함수와 컴포넌트는 별도 파일로 분리.
* 아이콘은 lucide-react만 사용.
* 모바일 퍼스트 (max-width: 600px 컨테이너 기준).

## 디자인 시스템 동기화 규칙 (Auto-Update)

아래 상황 발생 시 `/src/imports/fe-dev-guidelines.md`의 "디자인 시스템" 섹션을 **반드시 함께 업데이트**:

1. `theme.css`의 CSS 변수가 추가/변경/삭제될 때
2. 새로운 공통 컴포넌트 패턴이 추가될 때
3. 기존 컴포넌트의 스타일 규칙이 변경될 때
4. 새로운 페이지/화면이 추가될 때 (라우팅 정보 업데이트)
5. 타이포그래피, 간격, 둥글기 등 디자인 토큰이 변경될 때

이렇게 하면 디자인 시스템 문서가 항상 최신 상태를 유지합니다.
