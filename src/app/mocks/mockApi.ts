import api from '../api/axios';
import { PlaceDto } from '../api/types';
import { NEARBY_SPOTS } from '../data/places-mock';

/**
 * 프론트엔드 단독 테스트를 위한 axios 인터셉터 Mocking 설정.
 * (Vite 환경 변수 VITE_USE_MOCK === 'true' 일 때 동작하도록 권장)
 * 실제 API 서버가 없더라도 임시 데이터를 응답하여 UI 개발을 진행할 수 있게 돕습니다.
 */
export const setupMockApi = () => {
  // 환경변수가 없거나 MOCK을 안 쓴다면 그냥 리턴
  if (import.meta.env.VITE_USE_MOCK !== 'true') return;

  console.log('🚧 axios Mocking is ENABLED 🚧');

  // response 인터셉터 가로채기 (실제 네트워크 에러가 났을 때 Mock 데이터를 던져줌)
  api.interceptors.response.use(
    (response) => response, // 성공하면 그냥 넘김 (실제 백엔드가 살아있는 경우)
    (error) => {
      const config = error.config;
      
      // /places 요청 실패 시 Mock Data 반환
      if (config.url.includes('/places')) {
        console.warn(`[Mock API] Intercepted failed request to ${config.url}. Returning Mock Data.`);
        return Promise.resolve({
          data: {
            status: 200,
            message: 'SUCCESS (MOCK)',
            data: NEARBY_SPOTS as PlaceDto[],
          },
          status: 200,
          statusText: 'OK',
          headers: {},
          config,
        });
      }

      return Promise.reject(error);
    }
  );
};
