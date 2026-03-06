import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export interface PetInfo {
  name: string;
  type: string;
  breed: string;        // 품종 (예: 푸들, 코리안숏헤어)
  gender: '남아' | '여아';
  size: 'SMALL' | 'MEDIUM' | 'LARGE';
  activity: 'LOW' | 'NORMAL' | 'HIGH';
  age: number;
  weight?: number;      // 체중 (kg) — 펫 케어용
}

export interface SavedRoute {
  id: string;
  date: string;
  weather: string;
  temperature: number;
  bestTime: string;
  routes: Array<{ name: string; distance: string; type: string }>;
}

interface AppState {
  isLoggedIn: boolean;
  username: string;
  email: string;           // 회원 이메일
  pet: PetInfo | null;
  hasCompletedOnboarding: boolean;  // 온보딩 완료 여부
  wishlist: number[];
  savedRoutes: SavedRoute[];
  login: (username: string, email?: string) => void;
  logout: () => void;
  updateProfile: (data: { username?: string; email?: string }) => void;  // 회원정보 수정
  completeOnboarding: () => void;
  registerPet: (pet: PetInfo) => void;
  updatePet: (pet: Partial<PetInfo>) => void;
  removePet: () => void;
  toggleWishlist: (id: number) => void;
  clearWishlist: () => void;
  addSavedRoute: (route: SavedRoute) => void;
  removeSavedRoute: (id: string) => void;
}

export const useAppStore = create<AppState>()(
  persist(
    (set) => ({
      isLoggedIn: false,
      username: '게스트',
      email: '',
      pet: null,
      hasCompletedOnboarding: false,
      wishlist: [],
      savedRoutes: [],

      // TODO: [DB 연동] POST /api/auth/login → Spring Security JWT 토큰 기반 인증으로 전환
      login: (username, email) => set({ 
        isLoggedIn: true, 
        username,
        email: email || '',
      }),

      // TODO: [DB 연동] POST /api/auth/logout → JWT 토큰 블랙리스트(Redis) 처리 + 클라이언트 토큰 삭제
      logout: () => set({ isLoggedIn: false, username: '게스트', email: '', pet: null, hasCompletedOnboarding: false, wishlist: [] }),

      // TODO: [DB 연동] PUT /api/users/profile → Spring Boot JPA users 테이블 UPDATE (PostgreSQL)
      updateProfile: (data) => set((state) => ({
        username: data.username ?? state.username,
        email: data.email ?? state.email,
      })),

      // TODO: [DB 연동] 온보딩 완료 플래그 서버 저장
      completeOnboarding: () => set({ hasCompletedOnboarding: true }),

      // TODO: [DB 연동] POST /api/pets → Spring Boot JPA pets 테이블 INSERT (PostgreSQL)
      registerPet: (pet) => set({ pet }),

      // TODO: [DB 연동] PUT /api/pets/{id} → Spring Boot JPA pets 테이블 UPDATE (PostgreSQL)
      updatePet: (partial) => set((state) => ({
        pet: state.pet ? { ...state.pet, ...partial } : null
      })),

      // TODO: [DB 연동] DELETE /api/pets/{id} → Spring Boot JPA pets 테이블 DELETE (PostgreSQL)
      removePet: () => set({ pet: null }),

      // TODO: [DB 연동] POST|DELETE /api/wishlists/{placeId} → Spring Boot JPA wishlists 테이블 UPSERT/DELETE + 낙관적 업데이트
      toggleWishlist: (id) => set((state) => {
        const isWished = state.wishlist.includes(id);
        if (isWished) {
          return { wishlist: state.wishlist.filter(wId => wId !== id) };
        } else {
          return { wishlist: [...state.wishlist, id] };
        }
      }),

      // TODO: [DB 연동] DELETE /api/wishlists → Spring Boot JPA wishlists 전체 삭제 (PostgreSQL)
      clearWishlist: () => set({ wishlist: [] }),

      // TODO: [DB 연동] POST /api/saved-routes → Spring Boot JPA saved_routes 테이블 INSERT (PostgreSQL)
      addSavedRoute: (route) => set((state) => ({ savedRoutes: [route, ...state.savedRoutes] })),

      // TODO: [DB 연동] DELETE /api/saved-routes/{id} → Spring Boot JPA saved_routes 테이블 DELETE (PostgreSQL)
      removeSavedRoute: (id) => set((state) => ({ savedRoutes: state.savedRoutes.filter(r => r.id !== id) })),
    }),
    {
      // TODO: [DB 연동] persist 미들웨어 제거 → WebSocket(STOMP) 실시간 동기화로 대체
      name: 'meongnyang-storage',
    }
  )
);