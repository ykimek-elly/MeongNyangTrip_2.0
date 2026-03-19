import api from './axios';

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
  pendingReason: string | null;  // JSON: {similarity, sourceTitle, kakaoTitle, kakaoLat, kakaoLng}
  kakaoMapUrl: string;
}

/**
 * 관리자 배치 API.
 * 백엔드 PlaceBatchController와 매핑.
 * POST /api/v1/admin/batch/*
 */
export const adminApi = {
  /** KTO 공공데이터 수집 + 네이버+카카오 이중검증 저장 */
  runPlacesBatch: () => api.post('/admin/batch/places'),

  /** 문화시설(KCISA) 수집 + 이중검증 + kakaoId dedup */
  runCultureBatch: () => api.post('/admin/batch/culture'),

  /** imageUrl null/빈값 장소 이미지 보강 */
  runEnrichImagesBatch: () => api.post('/admin/batch/enrich-images'),

  /** AI 별점(aiRating) 재계산 */
  runAiRatingBatch: () => api.post('/admin/batch/recalculate-ai-rating'),

  /** 깨진 이미지(SNS CDN / 뉴스) 초기화 + 재보강 */
  runFixBrokenImagesBatch: () => api.post('/admin/batch/fix-broken-images'),

  // ── 장소 검토 큐 ──────────────────────────────────────────────────────────
  /** 보류 장소 목록 조회 (유사도 50~79%) */
  getPendingPlaces: () =>
    api.get<PendingPlaceDto[]>('/admin/places/pending').then(r => r.data),

  /** 승인 — 좌표 수정 옵션 포함 */
  approvePlace: (id: number, coords?: { lat: number; lng: number }) =>
    api.post<PendingPlaceDto>(`/admin/places/${id}/approve`, coords ?? {}).then(r => r.data),

  /** 거절 */
  rejectPlace: (id: number) =>
    api.post(`/admin/places/${id}/reject`),

  /** 수동 수정 후 승인 */
  manualApprovePlace: (id: number, data: { title?: string; address?: string; lat?: number; lng?: number }) =>
    api.put<PendingPlaceDto>(`/admin/places/${id}/manual`, data).then(r => r.data),
};
