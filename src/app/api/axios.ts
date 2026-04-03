import axios from 'axios';

/**
 * Axios 인스턴스 — 백엔드 API 통신용.
 * Access Token 만료(401) 시 Refresh Token으로 자동 재발급 후 원래 요청 재시도.
 */
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

/** 토큰 첨부 제외 경로 — 인증 불필요 public 엔드포인트 */
const PUBLIC_PATHS = ['/auth/login', '/auth/signup', '/auth/refresh', '/auth/find-id', '/auth/reset-password', '/auth/check-phone', '/auth/sms'];

/** Request Interceptor: 모든 요청에 Access Token 자동 첨부 (public 경로 제외) */
api.interceptors.request.use((config) => {
  const url = config.url ?? '';
  const isPublic = PUBLIC_PATHS.some(path => url.includes(path));
  if (!isPublic) {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

/** 토큰 갱신 중 중복 요청 방지 플래그 */
let isRefreshing = false;
let pendingRequests: Array<(token: string) => void> = [];

function onRefreshed(newToken: string) {
  pendingRequests.forEach(cb => cb(newToken));
  pendingRequests = [];
}

/** Response Interceptor: 401 시 Refresh Token으로 재발급 시도 */
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // /auth/refresh 자체가 401이면 무한루프 방지 — 바로 로그아웃
    if (error.response?.status === 401 && originalRequest.url?.includes('/auth/refresh')) {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      window.location.href = '/login';
      return Promise.reject(error);
    }

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      const refreshToken = localStorage.getItem('refreshToken');
      if (!refreshToken) {
        localStorage.removeItem('accessToken');
        window.location.href = '/login';
        return Promise.reject(error);
      }

      // 이미 갱신 중이면 완료될 때까지 대기
      if (isRefreshing) {
        return new Promise((resolve) => {
          pendingRequests.push((newToken: string) => {
            originalRequest.headers.Authorization = `Bearer ${newToken}`;
            resolve(api(originalRequest));
          });
        });
      }

      isRefreshing = true;

      try {
        const { data } = await api.post('/auth/refresh', { refreshToken });
        const newAccessToken  = data.token;
        const newRefreshToken = data.refreshToken;

        localStorage.setItem('accessToken', newAccessToken);
        localStorage.setItem('refreshToken', newRefreshToken);

        onRefreshed(newAccessToken);
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return api(originalRequest);

      } catch {
        // refresh 실패 → 로그아웃
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        window.location.href = '/login';
        return Promise.reject(error);

      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default api;
