import api from '../api/axios';
import { PlaceDto } from '../api/types';
import { NEARBY_SPOTS } from '../data/places-mock';

/**
 * 프론트엔드 단독 테스트를 위한 axios 인터셉터 Mocking 설정.
 * (Vite 환경 변수 VITE_USE_MOCK === 'true' 일 때 동작하도록 권장)
 * 실제 API 서버가 없더라도 임시 데이터를 응답하여 UI 개발을 진행할 수 있게 돕습니다.
 */
// Mock 찜 목록 — 인메모리 (새로고침 시 초기화)
const mockWishlistSet = new Set<number>();

export const setupMockApi = () => {
  // 환경변수가 없거나 MOCK을 안 쓴다면 그냥 리턴
  const isMock = import.meta.env.VITE_USE_MOCK === 'true';
  if (!isMock) return;

  console.log('🚧 axios Mocking is ENABLED 🚧');

  // request 인터셉터 가로채기 (실제 네트워크를 타기 전에 아예 응답을 던져버림으로써 백엔드 서버가 아예 꺼져있어도 ERR_CONNECTION_REFUSED를 발생시키지 않음)
  api.interceptors.request.use((config) => {
    const url = config.url ?? '';

    // ── 장소 목록 ──────────────────────────────────────────────
    if (url.includes('/places') || url.includes('/public-places')) {
      console.warn(`[Mock API] Intercepted request to ${url}. Returning Mock Data instantly.`);
      config.adapter = async () => ({
        data: { status: 200, message: 'SUCCESS (MOCK)', data: NEARBY_SPOTS as PlaceDto[] },
        status: 200, statusText: 'OK', headers: {}, config,
      });
    }

    // ── 찜하기 토글 POST /wishlists/:placeId ──────────────────
    else if (config.method === 'post' && /\/wishlists\/\d+$/.test(url)) {
      const placeId = Number(url.split('/').pop());
      const wishlisted = mockWishlistSet.has(placeId);
      if (wishlisted) mockWishlistSet.delete(placeId);
      else mockWishlistSet.add(placeId);
      config.adapter = async () => ({
        data: { status: 200, message: 'SUCCESS (MOCK)', data: { placeId, wishlisted: !wishlisted } },
        status: 200, statusText: 'OK', headers: {}, config,
      });
    }

    // ── 찜 목록 조회 GET /wishlists/my ────────────────────────
    else if (config.method === 'get' && url.includes('/wishlists/my')) {
      config.adapter = async () => ({
        data: { status: 200, message: 'SUCCESS (MOCK)', data: Array.from(mockWishlistSet).map(id => ({ placeId: id })) },
        status: 200, statusText: 'OK', headers: {}, config,
      });
    }

    // ── 찜 여부 확인 GET /wishlists/:placeId/status ───────────
    else if (config.method === 'get' && /\/wishlists\/\d+\/status$/.test(url)) {
      const placeId = Number(url.split('/').at(-2));
      config.adapter = async () => ({
        data: { status: 200, message: 'SUCCESS (MOCK)', data: mockWishlistSet.has(placeId) },
        status: 200, statusText: 'OK', headers: {}, config,
      });
    }

    return config;
  });
};
