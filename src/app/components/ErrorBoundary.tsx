import React from 'react';
import { useRouteError, isRouteErrorResponse, useNavigate } from 'react-router';
import { AlertTriangle, Home, RotateCcw } from 'lucide-react';

/** 라우트 에러 폴백 UI — React Router errorElement용 */
export function RouteErrorFallback() {
  const error = useRouteError();
  const navigate = useNavigate();

  let title = '문제가 발생했어요';
  let description = '일시적인 오류가 발생했습니다.\n잠시 후 다시 시도해 주세요.';

  if (isRouteErrorResponse(error)) {
    if (error.status === 404) {
      title = '페이지를 찾을 수 없어요';
      description = '요청하신 페이지가 존재하지 않거나\n주소가 변경되었을 수 있습니다.';
    } else {
      title = `오류 ${error.status}`;
      description = error.statusText || '알 수 없는 오류가 발생했습니다.';
    }
  }

  return (
    <div className="bg-background min-h-screen flex justify-center font-sans text-foreground">
      <div className="w-full max-w-[600px] min-h-screen bg-white flex flex-col items-center justify-center px-6 text-center">
        {/* 에러 아이콘 */}
        <div className="w-20 h-20 bg-destructive/10 rounded-full flex items-center justify-center mb-6">
          <AlertTriangle size={40} className="text-destructive" />
        </div>

        <h2 className="text-xl font-bold text-gray-800 mb-2">{title}</h2>
        <p className="text-sm text-gray-500 mb-8 leading-relaxed whitespace-pre-line">
          {description}
        </p>

        <div className="flex gap-3">
          <button
            onClick={() => window.location.reload()}
            className="flex items-center gap-2 bg-gray-100 text-gray-600 px-5 py-3 rounded-2xl font-bold text-sm hover:bg-gray-200 active:scale-[0.97] transition-spring"
          >
            <RotateCcw size={16} />
            새로고침
          </button>
          <button
            onClick={() => navigate('/')}
            className="flex items-center gap-2 bg-primary text-white px-5 py-3 rounded-2xl font-bold text-sm shadow-md hover:bg-primary/90 active:scale-[0.97] transition-spring"
          >
            <Home size={16} />
            홈으로
          </button>
        </div>

        {/* 개발 모드에서만 에러 상세 표시 */}
        {import.meta.env.DEV && error instanceof Error && (
          <details className="mt-8 w-full text-left">
            <summary className="text-xs text-gray-400 cursor-pointer hover:text-gray-600">
              개발자용 에러 상세
            </summary>
            <pre className="mt-2 p-3 bg-gray-50 rounded-xl text-xs text-destructive overflow-x-auto max-h-[200px] border border-gray-100">
              {error.message}
              {'\n\n'}
              {error.stack}
            </pre>
          </details>
        )}
      </div>
    </div>
  );
}

/**
 * 전역 Error Boundary (클래스 컴포넌트)
 * — React Router가 잡지 못하는 렌더 에러 대비
 */
interface Props {
  children: React.ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class GlobalErrorBoundary extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    // TODO: [로깅] 에러 리포팅 서비스(Sentry 등)로 전송
    console.error('[GlobalErrorBoundary]', error, errorInfo);
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null });
    window.location.href = '/';
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className="bg-background min-h-screen flex justify-center font-sans text-foreground">
          <div className="w-full max-w-[600px] min-h-screen bg-white flex flex-col items-center justify-center px-6 text-center">
            <div className="w-20 h-20 bg-destructive/10 rounded-full flex items-center justify-center mb-6">
              <AlertTriangle size={40} className="text-destructive" />
            </div>

            <h2 className="text-xl font-bold text-gray-800 mb-2">앗, 예상치 못한 오류예요</h2>
            <p className="text-sm text-gray-500 mb-8 leading-relaxed">
              앱에서 문제가 발생했습니다.<br />
              아래 버튼을 눌러 다시 시도해 주세요.
            </p>

            <div className="flex gap-3">
              <button
                onClick={() => window.location.reload()}
                className="flex items-center gap-2 bg-gray-100 text-gray-600 px-5 py-3 rounded-2xl font-bold text-sm hover:bg-gray-200 active:scale-[0.97] transition-spring"
              >
                <RotateCcw size={16} />
                새로고침
              </button>
              <button
                onClick={this.handleReset}
                className="flex items-center gap-2 bg-primary text-white px-5 py-3 rounded-2xl font-bold text-sm shadow-md hover:bg-primary/90 active:scale-[0.97] transition-spring"
              >
                <Home size={16} />
                홈으로
              </button>
            </div>

            {/* 개발 모드에서만 에러 상세 표시 */}
            {import.meta.env.DEV && this.state.error && (
              <details className="mt-8 w-full text-left">
                <summary className="text-xs text-gray-400 cursor-pointer hover:text-gray-600">
                  개발자용 에러 상세
                </summary>
                <pre className="mt-2 p-3 bg-gray-50 rounded-xl text-xs text-destructive overflow-x-auto max-h-[200px] border border-gray-100">
                  {this.state.error.message}
                  {'\n\n'}
                  {this.state.error.stack}
                </pre>
              </details>
            )}
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
