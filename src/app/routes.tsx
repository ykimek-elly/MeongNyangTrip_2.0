import React from 'react';
import { createBrowserRouter, Outlet, useNavigate, useLocation, useParams, useSearchParams } from 'react-router';
import { Home } from './pages/Home';
import { List } from './pages/List';
import { Detail } from './pages/Detail';
import { Login } from './pages/Login';
import { Signup } from './pages/Signup';
import { FindId } from './pages/FindId';
import { FindPassword } from './pages/FindPassword';
import { MapSearch } from './pages/MapSearch';
import { Lounge } from './pages/Lounge';
import { MyPage } from './pages/MyPage';
import { AIWalkGuide } from './pages/AIWalkGuide';
import { VisitCheckIn } from './pages/VisitCheckIn';
import { AdminDashboard } from './pages/AdminDashboard';
import { TeamPage } from './pages/TeamPage';
import { OAuthCallback } from './pages/OAuthCallback';
import { NotFound } from './pages/NotFound';
import { Onboarding } from './pages/Onboarding';
import { EditProfile } from './pages/EditProfile';
import { RouteErrorFallback } from './components/ErrorBoundary';
import { BottomNav } from './components/BottomNav';
import { AIChat } from './components/AIChat';
import { useAppStore } from './store/useAppStore';
import { Leaf } from 'lucide-react';

/** 공통 네비게이션 핸들러 — 페이지 이동 로직 통합 */
function createNavigateHandler(navigate: (path: string) => void, isLoggedIn: boolean) {
  return (page: string, params?: any) => {
    switch (page) {
      case 'home': navigate('/'); break;
      case 'list': {
        const search = params ? new URLSearchParams(params).toString() : '';
        navigate(`/list${search ? `?${search}` : ''}`);
        break;
      }
      case 'detail': navigate(`/detail/${params.id}`); break;
      case 'map': navigate('/map'); break;
      case 'lounge': navigate('/lounge'); break;
      case 'login': navigate('/login'); break;
      case 'signup': navigate('/signup'); break;
      case 'find-id': navigate('/find-id'); break;
      case 'find-password': navigate('/find-password'); break;
      case 'onboarding': navigate('/onboarding'); break;
      case 'mypage':
      case 'wish':
        if (!isLoggedIn) {
          alert("로그인이 필요합니다. (테스트: 아무 아이디나 입력)");
          navigate('/login');
        } else {
          navigate('/mypage');
        }
        break;
      case 'ai-walk-guide': navigate('/ai-walk-guide'); break;
      case 'visit-checkin': navigate('/visit-checkin'); break;
      case 'edit-profile': navigate('/edit-profile'); break;
      case 'admin': navigate('/admin'); break;
      case 'team': navigate('/team'); break;
      default: navigate('/');
    }
    window.scrollTo(0, 0);
  };
}

/** 헤더/GNB 숨김 대상 페이지 목록 */
const HIDDEN_HEADER_PAGES = ['login', 'signup', 'detail', 'list', 'find-id', 'find-password', 'ai-walk-guide', 'visit-checkin', 'admin', 'onboarding', 'edit-profile', 'team', 'oauth2/callback'];
const HIDDEN_NAV_PAGES = ['login', 'signup', 'detail', 'find-id', 'find-password', 'ai-walk-guide', 'visit-checkin', 'admin', 'onboarding', 'edit-profile', 'team', 'oauth2/callback'];

/** 루트 레이아웃 — 헤더, GNB, AI챗 표시 제어 */
function RootAdapter() {
  const navigate = useNavigate();
  const location = useLocation();
  const { isLoggedIn, username } = useAppStore();

  const handleNavigate = createNavigateHandler(navigate, isLoggedIn);

  const currentPath = location.pathname.substring(1) || 'home';

  /* detail/:id 경로도 숨김 처리 */
  const showHeader = !HIDDEN_HEADER_PAGES.includes(currentPath) && !currentPath.startsWith('detail');
  const showBottomNav = !HIDDEN_NAV_PAGES.includes(currentPath) && !currentPath.startsWith('detail');
  const showAIChat = showBottomNav;

  return (
    <div className="bg-background min-h-screen flex justify-center font-sans text-foreground">
      <div className="w-full max-w-[600px] min-h-screen bg-white shadow-[0_0_30px_rgba(227,99,148,0.1)] relative overflow-hidden flex flex-col">
        
        {/* 상단 헤더 — 메인 탭에서만 표시 */}
        {showHeader && (
          <header className="flex justify-between items-center bg-white sticky top-0 z-50 border-b border-gray-100 flex-shrink-0 px-[20px] py-[8px] m-[0px]">
            <div 
              className="flex items-center gap-2 cursor-pointer" 
              onClick={() => navigate('/')}
              aria-label="홈으로 이동"
            >
              <Leaf className="text-primary" size={24} />
              <h1 className="text-lg font-bold tracking-tight text-gray-800">멍냥트립</h1>
            </div>
            
            <div className="flex items-center gap-3">
              {/* 날씨 섹션 — WEATHER_API_KEY 연동 전까지 숨김 (팀원C 연동 후 복구) */}

              <div className="flex items-center gap-2">
                {isLoggedIn && (
                  <span 
                    className="text-xs font-bold text-primary cursor-pointer hover:underline active:opacity-70 transition-opacity"
                    onClick={() => navigate('/edit-profile')}
                  >
                    {username}님
                  </span>
                )}
                <button 
                  className="text-xs font-bold text-gray-600 bg-gray-100 px-3 py-1.5 rounded-full hover:bg-gray-200 transition-colors"
                  onClick={() => isLoggedIn ? (useAppStore.getState().logout(), navigate('/')) : navigate('/login')}
                >
                  {isLoggedIn ? '로그아웃' : '로그인'}
                </button>
              </div>
            </div>
          </header>
        )}

        <main className={`flex-1 flex flex-col overflow-y-auto overflow-x-hidden relative ${currentPath === 'map' ? 'pt-0' : 'pt-2.5'}`}>
          <Outlet context={{ onNavigate: handleNavigate }} />
        </main>

        {showAIChat && <AIChat />}

        {showBottomNav && (
          <BottomNav 
            activeTab={currentPath === 'wish' ? 'mypage' : currentPath} 
            onNavigate={handleNavigate} 
          />
        )}
      </div>
    </div>
  );
}

/** onNavigate 주입 HOC */
function withNavigation(Component: any) {
  return function WrappedComponent(props: any) {
    const navigate = useNavigate();
    const { isLoggedIn } = useAppStore();
    const handleNavigate = createNavigateHandler(navigate, isLoggedIn);
    return <Component {...props} onNavigate={handleNavigate} />;
  };
}

/** 목록 페이지 — URL 파라미터를 initialParams로 전달 */
const WrappedList = withNavigation(List);
function ListWrapper() {
  const [searchParams] = useSearchParams();
  const params = Object.fromEntries(searchParams.entries());
  return <WrappedList initialParams={Object.keys(params).length > 0 ? params : null} />;
}

/** 상세 페이지 — URL에서 id 추출 후 전달 */
const WrappedDetail = withNavigation(Detail);
function DetailWrapper() {
  const { id } = useParams();
  return <WrappedDetail id={Number(id)} />;
}

/** 라우터 설정 */
export const router = createBrowserRouter([
  {
    path: "/",
    Component: RootAdapter,
    ErrorBoundary: RouteErrorFallback,
    children: [
      { index: true, Component: withNavigation(Home) },
      { path: "list", Component: ListWrapper },
      { path: "detail/:id", Component: DetailWrapper },
      { path: "map", Component: withNavigation(MapSearch) },
      { path: "lounge", Component: withNavigation(Lounge) },
      { path: "mypage", Component: withNavigation(MyPage) },
      { path: "login", Component: withNavigation(Login) },
      { path: "signup", Component: withNavigation(Signup) },
      { path: "find-id", Component: withNavigation(FindId) },
      { path: "find-password", Component: withNavigation(FindPassword) },
      { path: "onboarding", Component: withNavigation(Onboarding) },
      { path: "ai-walk-guide", Component: withNavigation(AIWalkGuide) },
      { path: "visit-checkin", Component: withNavigation(VisitCheckIn) },
      { path: "edit-profile", Component: withNavigation(EditProfile) },
      { path: "admin", Component: withNavigation(AdminDashboard) },
      { path: "team",  Component: withNavigation(TeamPage) },
      { path: "oauth2/callback", Component: OAuthCallback },
      { path: "*", Component: withNavigation(NotFound) },
    ],
  },
]);