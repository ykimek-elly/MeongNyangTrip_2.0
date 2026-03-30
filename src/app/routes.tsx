import React, { useRef } from 'react';
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
import { AdminLogin } from './pages/AdminLogin';
import { TeamPage } from './pages/TeamPage';
import { DMList } from './pages/DMList';
import { DMDetail } from './pages/DMDetail';
import { OAuthCallback } from './pages/OAuthCallback';
import { NotFound } from './pages/NotFound';
import { Onboarding } from './pages/Onboarding';
import { EditProfile } from './pages/EditProfile';
import { RouteErrorFallback } from './components/ErrorBoundary';
import { BottomNav } from './components/BottomNav';
import { AIChat } from './components/AIChat';
import { ScrollButtons } from './components/ScrollButtons';
import { useAppStore } from './store/useAppStore';
import { Leaf, Sun } from 'lucide-react';

/** 공통 네비게이션 핸들러 — 페이지 이동 로직 통합 */
function createNavigateHandler(navigate: ReturnType<typeof useNavigate>, isLoggedIn: boolean) {
  return (page: string, params?: any) => {
    switch (page) {
      case 'back':
        if (window.history.length > 1) { navigate(-1); return; }
        navigate('/');
        return;
      case 'home': navigate('/'); break;
      case 'list': {
        const search = params ? new URLSearchParams(params).toString() : '';
        navigate(`/list${search ? `?${search}` : ''}`);
        break;
      }
      case 'detail': navigate(`/detail/${params.id}`); break;
      case 'map': {
        const placeId = params?.placeId;
        navigate(`/map${placeId ? `?placeId=${placeId}` : ''}`);
        break;
      }
      case 'lounge': navigate('/lounge'); break;
      case 'login': navigate('/login'); break;
      case 'signup': navigate('/signup'); break;
      case 'find-id': navigate('/find-id'); break;
      case 'find-password': navigate('/find-password'); break;
      case 'onboarding': navigate('/onboarding'); break;
      case 'mypage':
      case 'wish':
        if (!isLoggedIn) {
          alert("로그인이 필요합니다.");
          navigate('/login');
        } else {
          navigate('/mypage');
        }
        break;
      case 'ai-walk-guide': navigate('/ai-walk-guide'); break;
      case 'visit-checkin': navigate('/visit-checkin'); break;
      case 'edit-profile': navigate('/edit-profile'); break;
      case 'admin': navigate('/admin'); break;
      case 'admin-login': navigate('/admin/login'); break;
      case 'team': navigate('/team'); break;
      case 'dm': navigate('/dm'); break;
      case 'dm-detail': navigate(`/dm/${encodeURIComponent(params?.partner ?? '')}`); break;
      default: navigate('/');
    }
    window.scrollTo(0, 0);
  };
}

/** 헤더/GNB 숨김 대상 페이지 목록 */
const HIDDEN_HEADER_PAGES = ['login', 'signup', 'detail', 'list', 'find-id', 'find-password', 'ai-walk-guide', 'visit-checkin', 'admin', 'admin/login', 'onboarding', 'edit-profile', 'team', 'oauth2/callback', 'dm'];
const HIDDEN_NAV_PAGES = ['login', 'signup', 'detail', 'find-id', 'find-password', 'visit-checkin', 'admin', 'admin/login', 'onboarding', 'edit-profile', 'team', 'oauth2/callback', 'dm'];

/** 루트 레이아웃 — 헤더, GNB, AI챗 표시 제어 */
function RootAdapter() {
  const navigate = useNavigate();
  const location = useLocation();
  const { isLoggedIn, username } = useAppStore();

  const handleNavigate = createNavigateHandler(navigate, isLoggedIn);

  const currentPath = location.pathname.substring(1) || 'home';

  /* detail/:id, dm/:partner 경로도 숨김 처리 */
  const showHeader = !HIDDEN_HEADER_PAGES.includes(currentPath) && !currentPath.startsWith('detail') && !currentPath.startsWith('dm');
  const showBottomNav = !HIDDEN_NAV_PAGES.includes(currentPath) && !currentPath.startsWith('detail') && !currentPath.startsWith('dm');
  const showAIChat = showBottomNav;
  const contentRef = useRef<HTMLElement>(null);
  const showScrollButtons = currentPath !== 'map' && currentPath !== 'list';

  return (
    <div className="bg-background h-screen flex justify-center font-sans text-foreground">
      <div className="w-full max-w-[600px] h-full bg-white shadow-[0_0_30px_rgba(227,99,148,0.1)] relative overflow-hidden flex flex-col">
        
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
              <div className="flex items-center gap-1">
                <Sun size={14} className="text-orange-400 fill-orange-400" />
                <span className="text-sm font-bold text-gray-800">23°C</span>
                <span className="text-[10px] text-primary font-medium tracking-tight ml-1 -order-1">산책하기 좋은 날씨예요!</span>
              </div>

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

        <main
          ref={contentRef}
          className={`flex-1 flex flex-col overflow-x-hidden relative min-h-0 ${currentPath === 'map' || currentPath === 'list' ? 'overflow-hidden pt-0' : 'overflow-y-auto pt-2.5'}`}
          style={{ paddingBottom: showBottomNav ? 'calc(80px + env(safe-area-inset-bottom, 0px))' : '0px' }}
        >
          <Outlet context={{ onNavigate: handleNavigate }} />
        </main>

        {showScrollButtons && <ScrollButtons scrollRef={contentRef} withNav={showBottomNav} />}
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

/** 관리자 전용 라우트 — isAdmin 아닐 경우 /admin/login으로 리다이렉트 */
function AdminRoute() {
  const { isAdmin } = useAppStore();
  const navigate = useNavigate();
  React.useEffect(() => {
    if (!isAdmin) navigate('/admin/login', { replace: true });
  }, [isAdmin, navigate]);
  if (!isAdmin) return null;
  return withNavigation(AdminDashboard)({});
}

/** DM 상세 페이지 — partner 파라미터 전달 */
const WrappedDMDetail = withNavigation(DMDetail);
function DMDetailWrapper() {
  const { partner } = useParams();
  return <WrappedDMDetail partner={decodeURIComponent(partner || '')} />;
}

/** 지도 페이지 — placeId 쿼리 파라미터 전달 */
const WrappedMapSearch = withNavigation(MapSearch);
function MapWrapper() {
  const [searchParams] = useSearchParams();
  const placeId = searchParams.get('placeId');
  return <WrappedMapSearch initialPlaceId={placeId ? Number(placeId) : undefined} />;
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
      { path: "map", Component: MapWrapper },
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
      { path: "admin", Component: AdminRoute },
      { path: "admin/login", Component: withNavigation(AdminLogin) },
      { path: "team",  Component: withNavigation(TeamPage) },
      { path: "dm", Component: withNavigation(DMList) },
      { path: "dm/:partner", Component: DMDetailWrapper },
      { path: "oauth2/callback", Component: OAuthCallback },
      { path: "*", Component: withNavigation(NotFound) },
    ],
  },
]);