import api from './axios';

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
};
