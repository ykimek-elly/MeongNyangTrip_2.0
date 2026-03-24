import api from './axios';

export interface BatchStatsDto {
  total: number;
  active: number;
  pending: number;
  rejected: number;
}

export interface PendingPlaceDto {
  id: number;
  title: string;
  address: string;
  addr2: string | null;
  latitude: number;
  longitude: number;
  category: string;
  imageUrl: string | null;
  phone: string | null;
  homepage: string | null;
  pendingReason: string | null;  // JSON: {similarity, sourceTitle, kakaoTitle, kakaoLat, kakaoLng}
  kakaoMapUrl: string;
}

/**
 * 관리자 배치 API.
 * 백엔드 PlaceBatchController와 매핑.
 * POST /api/v1/admin/batch/*
 *
 * 파이프라인 V3 실행 순서:
 *   1. /collect         — KTO + KCISA 동시 수집 (raw, 검증 없이 임시 저장)
 *   2. /dedup           — 소스간 중복 제거 (제목 유사도 + 좌표 50m 이내)
 *   3. /verify          — 카카오 + 네이버 교차검증 → ACTIVE/PENDING/REJECTED
 *   4. /validate-images — Gemini Vision 이미지 검증 (부적합 → null)
 *   5. /recalculate-ai-rating — AI 별점 확정
 */
export const adminApi = {
  /** GET /api/v1/admin/batch/stats — DB 장소 상태별 현황 */
  getBatchStats: async (): Promise<BatchStatsDto> => {
    const { data } = await api.get<BatchStatsDto>('/admin/batch/stats');
    return data;
  },

  // ── 파이프라인 V3 메인 ────────────────────────────────────────────────────
  /** STEP 1 — KTO + KCISA 공공데이터 동시 수집 (raw 임시 저장, 검증 전) */
  runCollectBatch: () => api.post('/admin/batch/collect'),

  /** STEP 2 — 소스간 중복 제거 (제목 유사도 ≥ 90% + 좌표 50m 이내 → 하나만 유지) */
  runDedupBatch: () => api.post('/admin/batch/dedup'),

  /** STEP 3 — 카카오 + 네이버 교차검증 → 유사도 기준 ACTIVE/PENDING/REJECTED 분류 */
  runVerifyBatch: () => api.post('/admin/batch/verify'),

  /** STEP 4 — Gemini Vision 이미지 적합성 검증 (부적합 → imageUrl null화) */
  runValidateImagesBatch: () => api.post('/admin/batch/validate-images'),

  /** STEP 5 — AI 별점(aiRating) 재계산 */
  runAiRatingBatch: () => api.post('/admin/batch/recalculate-ai-rating'),

  // ── 유틸리티 (개별 실행) ──────────────────────────────────────────────────
  /** 전체 ACTIVE 장소 교차검증 재실행 + 폐업 의심 REJECTED 처리 */
  runReVerifyAllBatch: () => api.post('/admin/batch/re-verify-all'),

  /** 전체 AI 별점 강제 재계산 (blogCount 포함) */
  runAiRatingAllBatch: () => api.post('/admin/batch/recalculate-ai-rating-all'),

  // ── 장소 전체 관리 ────────────────────────────────────────────────────────
  /** 전체 ACTIVE 장소 조회 — 관리자 수정용 */
  getAllActivePlaces: () =>
    api.get<PendingPlaceDto[]>('/admin/places').then(r => r.data),

  /** 장소 필드 수정 */
  editPlace: (id: number, data: { title?: string; address?: string; phone?: string; homepage?: string; imageUrl?: string }) =>
    api.patch<PendingPlaceDto>(`/admin/places/${id}/edit`, data).then(r => r.data),

  // ── 장소 검토 큐 ──────────────────────────────────────────────────────────
  /** 보류 장소 목록 조회 (유사도 50~79%) */
  getPendingPlaces: () =>
    api.get<PendingPlaceDto[]>('/admin/places/pending').then(r => r.data),

  /** 거절 장소 목록 조회 — 관리자 수동 복구용 */
  getRejectedPlaces: () =>
    api.get<PendingPlaceDto[]>('/admin/places/rejected').then(r => r.data),

  /** 이미지 없는 장소 목록 조회 */
  getNoImagePlaces: () =>
    api.get<PendingPlaceDto[]>('/admin/places/no-image').then(r => r.data),

  /** 이미지 URL 수동 등록 */
  updatePlaceImage: (id: number, imageUrl: string) =>
    api.patch(`/admin/places/${id}/image`, { imageUrl }),

  /** 승인 — 좌표 수정 옵션 포함 */
  approvePlace: (id: number, coords?: { lat: number; lng: number }) =>
    api.post<PendingPlaceDto>(`/admin/places/${id}/approve`, coords ?? {}).then(r => r.data),

  /** 거절 */
  rejectPlace: (id: number) =>
    api.post(`/admin/places/${id}/reject`),

  /** 수동 수정 후 승인 */
  manualApprovePlace: (id: number, data: { title?: string; address?: string; lat?: number; lng?: number }) =>
    api.put<PendingPlaceDto>(`/admin/places/${id}/manual`, data).then(r => r.data),

  /** 장소 영구 삭제 (중복 제거용) */
  deletePlace: (id: number) =>
    api.delete(`/admin/places/${id}`),

  /** AI 보강 미리보기 — DB 저장 없이 Naver 분석 + aiRating 계산 */
  analyzePlacePreview: (data: {
    title: string;
    address: string;
    phone?: string;
    homepage?: string;
    imageUrl?: string;
    description?: string;
  }) => api.post<{
    lat: number | null;
    lng: number | null;
    geocodeSuccess: boolean;
    aiRating: number;
    blogCount: number;
    blogPositiveTags: string | null;
    blogNegativeTags: string | null;
    naverVerified: boolean;
  }>('/admin/places/analyze', data).then(r => r.data),

  /** 신규 장소 수동 등록 — POST /api/v1/admin/places */
  createPlace: (data: {
    title: string;
    category: string;
    address: string;
    lat: number;
    lng: number;
    phone?: string;
    homepage?: string;
    imageUrl?: string;
    description?: string;
    aiRating?: number;
  }) => api.post<PendingPlaceDto>('/admin/places', data).then(r => r.data),
};
