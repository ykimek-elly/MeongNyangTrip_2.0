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

// Mock DM 데이터 — 인메모리
interface MockDmMessage { id: number; fromId: string; content: string; createdAt: string; isRead: boolean; }
const mockDmMessages: Record<string, MockDmMessage[]> = {
  '보리보리': [
    { id: 1, fromId: '보리보리', content: '혹시 다음에 같이 산책해요!', createdAt: '2026-03-05T11:00:00', isRead: false },
    { id: 2, fromId: '보리보리', content: '저도 강아지 키우는데 한강 자주 가요 ☺️', createdAt: '2026-03-05T11:02:00', isRead: false },
  ],
  '망고주스': [
    { id: 3, fromId: '망고주스', content: '반려동물 크기 제한 있나요?', createdAt: '2026-03-05T08:00:00', isRead: true },
    { id: 4, fromId: '망고주스', content: '글램핑장 사진도 찍어오셨나요? 참고하고 싶어서요!', createdAt: '2026-03-05T10:00:00', isRead: false },
  ],
  '코코넨네': [
    { id: 5, fromId: '코코넨네', content: '예약 어떻게 하셨어요?', createdAt: '2026-03-05T08:00:00', isRead: true },
  ],
};
let mockDmIdSeq = 100;

export const setupMockApi = () => {
  // 환경변수가 없거나 MOCK을 안 쓴다면 그냥 리턴
  const isMock = import.meta.env.VITE_USE_MOCK === 'true';
  if (!isMock) return;

  console.log('🚧 axios Mocking is ENABLED 🚧');

  // request 인터셉터 가로채기 (실제 네트워크를 타기 전에 아예 응답을 던져버림으로써 백엔드 서버가 아예 꺼져있어도 ERR_CONNECTION_REFUSED를 발생시키지 않음)
  api.interceptors.request.use((config) => {
    const url = config.url ?? '';

    // ── 장소 목록 (MOCK 해제 - 실제 DB 연동) ───────────────────
    /*
    if (!url.includes('/admin') && (url.includes('/places') || url.includes('/public-places'))) {
      console.warn(`[Mock API] Intercepted request to ${url}. Returning Mock Data instantly.`);
      config.adapter = async () => ({
        data: { status: 200, message: 'SUCCESS (MOCK)', data: NEARBY_SPOTS as PlaceDto[] },
        status: 200, statusText: 'OK', headers: {}, config,
      });
    }
    */

    // ── 찜하기 토글 POST /wishlists/:placeId (MOCK 해제) ──────
    /*
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
      const parts = url.split('/'); const placeId = Number(parts[parts.length - 2]);
      config.adapter = async () => ({
        data: { status: 200, message: 'SUCCESS (MOCK)', data: mockWishlistSet.has(placeId) },
        status: 200, statusText: 'OK', headers: {}, config,
      });
    }
    */

    // ── 신규 장소 AI 분석 미리보기 (MOCK 해제) ─────────────────
    /*
    else if (config.method === 'post' && url.includes('/admin/places/analyze')) {
      const body = typeof config.data === 'string' ? JSON.parse(config.data) : (config.data ?? {});
      const hasPhone    = !!body.phone;
      const hasHomepage = !!body.homepage;
      const hasImage    = !!body.imageUrl;
      const hasDesc     = (body.description ?? '').length >= 50;
      let aiRating = 2.0;
      if (hasImage && hasDesc) aiRating += 0.3;
      if (hasPhone && hasHomepage) aiRating += 0.3;
      aiRating += 0.4; // mock blog score
      aiRating = Math.round(aiRating * 10) / 10;
      config.adapter = async () => ({
        data: {
          lat: 37.5172, lng: 127.0473,
          geocodeSuccess: true,
          aiRating,
          blogCount: 23,
          blogPositiveTags: '분위기좋음,반려동물환영,청결',
          blogNegativeTags: null,
          naverVerified: true,
        },
        status: 200, statusText: 'OK', headers: {}, config,
      });
    }

    // ── 신규 장소 등록 POST /admin/places (mock) ──────────────
    else if (config.method === 'post' && /\/admin\/places$/.test(url)) {
      const body = typeof config.data === 'string' ? JSON.parse(config.data) : (config.data ?? {});
      config.adapter = async () => ({
        data: {
          id: Math.floor(Math.random() * 9000) + 1000,
          title: body.title ?? '새 장소',
          address: body.address ?? '',
          category: body.category ?? 'PLACE',
          latitude: body.lat ?? 37.5172,
          longitude: body.lng ?? 127.0473,
          imageUrl: body.imageUrl ?? null,
          phone: body.phone ?? null,
          homepage: body.homepage ?? null,
          pendingReason: null,
          kakaoMapUrl: '',
        },
        status: 200, statusText: 'OK', headers: {}, config,
      });
    }
    */

    // ── 휴대폰 중복확인 GET /auth/check-phone (MOCK 해제 - 실제 DB 연동)
    /*
    if (config.method === 'get' && url.includes('/auth/check-phone')) {
      config.adapter = async () => ({
        data: { status: 200, message: 'SUCCESS (MOCK)', data: { available: true } },
        status: 200, statusText: 'OK', headers: {}, config,
      });
    }
    */

    // ── SMS 인증번호 발송 POST /auth/sms/send (백엔드 미구현이므로 유지) ─────────────────
    if (config.method === 'post' && url.includes('/auth/sms/send')) {
      config.adapter = async () => ({
        data: { status: 200, message: 'SUCCESS (MOCK)', data: null },
        status: 200, statusText: 'OK', headers: {}, config,
      });
    }

    // ── SMS 인증번호 확인 POST /auth/sms/verify (백엔드 미구현이므로 유지) ───────────────
    else if (config.method === 'post' && url.includes('/auth/sms/verify')) {
      const body = typeof config.data === 'string' ? JSON.parse(config.data) : (config.data ?? {});
      const verified = body.code === '123456';
      config.adapter = async () => ({
        data: { status: 200, message: 'SUCCESS (MOCK)', data: { verified } },
        status: 200, statusText: 'OK', headers: {}, config,
      });
    }

    // ── 아이디 찾기 POST /auth/find-id (MOCK 해제 - 실제 DB 연동)
    /*
    else if (config.method === 'post' && url.includes('/auth/find-id')) {
      config.adapter = async () => ({
        data: { status: 200, message: 'SUCCESS (MOCK)', data: { email: 'user***@example.com' } },
        status: 200, statusText: 'OK', headers: {}, config,
      });
    }
    */

    // ── 임시 비밀번호 발송 POST /auth/reset-password (MOCK 해제 - 실제 DB 연동)
    /*
    else if (config.method === 'post' && url.includes('/auth/reset-password')) {
      config.adapter = async () => ({
        data: { status: 200, message: 'SUCCESS (MOCK)', data: null },
        status: 200, statusText: 'OK', headers: {}, config,
      });
    }
    */

    // ── DM: (MOCK 해제 - 실제 DB 연동) ─────────────────────────
    /*
    else if (config.method === 'patch' && /\/dms\/.+\/read$/.test(url)) {
      const partnerId = decodeURIComponent(url.split('/dms/')[1].replace('/read', ''));
      const msgs = mockDmMessages[partnerId];
      if (msgs) msgs.forEach(m => { m.isRead = true; });
      config.adapter = async () => ({
        data: { status: 200, message: 'SUCCESS (MOCK)', data: null },
        status: 200, statusText: 'OK', headers: {}, config,
      });
    }

    // ── DM: 메시지 전송 POST /dms/:partnerId ──────────────────
    else if (config.method === 'post' && /\/dms\/[^/]+$/.test(url) && !url.includes('/wishlists')) {
      const partnerId = decodeURIComponent(url.split('/dms/')[1]);
      const body = typeof config.data === 'string' ? JSON.parse(config.data) : config.data;
      const newMsg: MockDmMessage = {
        id: ++mockDmIdSeq,
        fromId: 'me',
        content: body.content,
        createdAt: new Date().toISOString(),
        isRead: true,
      };
      if (!mockDmMessages[partnerId]) mockDmMessages[partnerId] = [];
      mockDmMessages[partnerId].push(newMsg);
      config.adapter = async () => ({
        data: { status: 200, message: 'SUCCESS (MOCK)', data: newMsg },
        status: 200, statusText: 'OK', headers: {}, config,
      });
    }

    // ── DM: 메시지 목록 GET /dms/:partnerId ───────────────────
    else if (config.method === 'get' && /\/dms\/[^/]+$/.test(url)) {
      const partnerId = decodeURIComponent(url.split('/dms/')[1]);
      config.adapter = async () => ({
        data: { status: 200, message: 'SUCCESS (MOCK)', data: mockDmMessages[partnerId] ?? [] },
        status: 200, statusText: 'OK', headers: {}, config,
      });
    }

    // ── DM: 대화 목록 GET /dms ────────────────────────────────
    else if (config.method === 'get' && /\/dms$/.test(url)) {
      const list = Object.entries(mockDmMessages).map(([partnerId, msgs]) => {
        const last = msgs[msgs.length - 1];
        const partnerImgs: Record<string, string> = {
          '보리보리': 'https://images.unsplash.com/photo-1569163139394-de4e5f43e5ca?w=100&q=80',
          '망고주스': 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100&q=80',
          '코코넨네': 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&q=80',
        };
        return {
          partnerId,
          partnerImg: partnerImgs[partnerId] ?? '',
          lastMessage: last?.content ?? '',
          lastMessageAt: last?.createdAt ?? new Date().toISOString(),
          unreadCount: msgs.filter(m => !m.isRead).length,
        };
      }).sort((a, b) => new Date(b.lastMessageAt).getTime() - new Date(a.lastMessageAt).getTime());
      config.adapter = async () => ({
        data: { status: 200, message: 'SUCCESS (MOCK)', data: list },
        status: 200, statusText: 'OK', headers: {}, config,
      });
    }
    */

    return config;
  });
};
