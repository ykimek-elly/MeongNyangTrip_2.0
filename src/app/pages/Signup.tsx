import React, { useState, useEffect, useRef } from 'react';
import { motion } from 'motion/react';
import { useAppStore } from '../store/useAppStore';
import { authApi } from '../api/authApi';
import { ArrowLeft, Mail, Eye, EyeOff, Leaf, Phone, CheckCircle2, RefreshCw } from 'lucide-react';

interface SignupProps {
  onNavigate: (page: string) => void;
}

export function Signup({ onNavigate }: SignupProps) {
  const login = useAppStore(state => state.login);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [nickname, setNickname] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [agreeTerms, setAgreeTerms] = useState(false);

  // 휴대폰 인증
  const [phone, setPhone] = useState('');
  const [smsCode, setSmsCode] = useState('');
  const [smsSent, setSmsSent] = useState(false);
  const [smsVerified, setSmsVerified] = useState(false);
  const [smsError, setSmsError] = useState('');
  const [smsSending, setSmsSending] = useState(false);
  const [smsVerifying, setSmsVerifying] = useState(false);
  const [timeLeft, setTimeLeft] = useState(0);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    return () => { if (timerRef.current) clearInterval(timerRef.current); };
  }, []);

  const formatTime = (s: number) => `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;

  const formatPhone = (v: string) => {
    const digits = v.replace(/\D/g, '').slice(0, 11);
    if (digits.length <= 3) return digits;
    if (digits.length <= 7) return `${digits.slice(0, 3)}-${digits.slice(3)}`;
    return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`;
  };

  const startTimer = () => {
    if (timerRef.current) clearInterval(timerRef.current);
    setTimeLeft(180);
    timerRef.current = setInterval(() => {
      setTimeLeft(t => { if (t <= 1) { clearInterval(timerRef.current!); return 0; } return t - 1; });
    }, 1000);
  };

  const handleSendSms = async () => {
    const digits = phone.replace(/\D/g, '');
    if (digits.length < 10) { setSmsError('올바른 휴대폰 번호를 입력해주세요.'); return; }
    setSmsError(''); setSmsSending(true);
    try {
      await authApi.sendSmsCode(digits);
      setSmsSent(true); setSmsVerified(false); setSmsCode('');
      startTimer();
    } catch {
      setSmsError('인증번호 발송에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setSmsSending(false);
    }
  };

  const handleVerifySms = async () => {
    if (smsCode.length !== 6) { setSmsError('6자리 인증번호를 입력해주세요.'); return; }
    setSmsError(''); setSmsVerifying(true);
    try {
      const ok = await authApi.verifySmsCode(phone.replace(/\D/g, ''), smsCode);
      if (ok) { setSmsVerified(true); if (timerRef.current) clearInterval(timerRef.current); }
      else setSmsError('인증번호가 올바르지 않습니다.');
    } catch {
      setSmsError('인증 확인에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setSmsVerifying(false);
    }
  };

  const isValid = email.trim() && password.length >= 6 && nickname.trim() && smsVerified && agreeTerms;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isValid) return;
    setError('');
    setIsLoading(true);
    try {
      const res = await authApi.signup(email, password, nickname, phone.replace(/\D/g, ''));
      localStorage.setItem('accessToken', res.token);
      login(res.nickname, res.email, res.userId, res.profileImage);
      onNavigate('onboarding');
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg || '회원가입에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleSocialLogin = (provider: string) => {
    if (!smsVerified) return;
    sessionStorage.setItem('pending_phone', phone.replace(/\D/g, ''));
    const apiHost = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1').replace('/api/v1', '');
    window.location.href = `${apiHost}/oauth2/authorization/${provider}`;
  };

  return (
    <div className="min-h-screen bg-white flex flex-col">
      {/* 헤더 */}
      <header className="px-5 py-4 flex items-center bg-white sticky top-0 z-10">
        <button onClick={() => onNavigate('login')} className="p-2 -ml-2 text-gray-600 hover:bg-gray-100 rounded-full">
          <ArrowLeft size={24} />
        </button>
        <div className="flex-1" />
      </header>

      <main className="flex-1 px-6 py-4 pb-24">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="space-y-8"
        >
          {/* 타이틀 */}
          <div>
            <div className="w-14 h-14 bg-primary/10 rounded-2xl flex items-center justify-center mb-5">
              <Leaf size={28} className="text-primary" />
            </div>
            <h2 className="text-2xl font-bold text-gray-900 mb-2">간편 가입하기</h2>
            <p className="text-sm text-gray-500 leading-relaxed">
              30초면 완료! 가입 후 반려동물 등록까지<br />한 번에 진행할 수 있어요.
            </p>
          </div>

          {/* STEP 1: 휴대폰 인증 */}
          <div className={`rounded-2xl border p-4 transition-colors ${smsVerified ? 'border-green-200 bg-green-50/50' : 'border-gray-200 bg-gray-50/50'}`}>
            <p className="text-xs font-bold text-gray-500 uppercase tracking-wide mb-3">
              STEP 1 · 휴대폰 인증
            </p>

            <div>
              <div className="flex gap-2">
                <div className="relative flex-1">
                  <Phone size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" />
                  <input
                    type="tel"
                    value={phone}
                    onChange={(e) => { setPhone(formatPhone(e.target.value)); setSmsVerified(false); setSmsSent(false); setSmsError(''); }}
                    placeholder="010-0000-0000"
                    disabled={smsVerified}
                    className="w-full pl-10 pr-3 py-3.5 bg-white border border-gray-200 rounded-2xl focus:border-primary outline-none transition-colors text-sm disabled:opacity-60"
                  />
                </div>
                <button
                  type="button"
                  onClick={handleSendSms}
                  disabled={smsSending || smsVerified}
                  className="shrink-0 px-4 py-3.5 rounded-2xl text-sm font-bold transition-spring active:scale-[0.97] disabled:opacity-50 disabled:cursor-not-allowed bg-gray-800 text-white hover:bg-gray-700 flex items-center gap-1.5"
                >
                  {smsSending ? <RefreshCw size={14} className="animate-spin" /> : smsSent && !smsVerified ? <><RefreshCw size={13} />재발송</> : '발송'}
                </button>
              </div>

              {smsSent && !smsVerified && (
                <motion.div initial={{ opacity: 0, y: -6 }} animate={{ opacity: 1, y: 0 }} className="mt-2 flex gap-2">
                  <div className="relative flex-1">
                    <input
                      type="text"
                      value={smsCode}
                      onChange={(e) => { setSmsCode(e.target.value.replace(/\D/g, '').slice(0, 6)); setSmsError(''); }}
                      placeholder="인증번호 6자리"
                      inputMode="numeric"
                      className="w-full px-4 py-3.5 bg-white border border-gray-200 rounded-2xl focus:border-primary outline-none transition-colors text-sm tracking-widest"
                    />
                    {timeLeft > 0 ? (
                      <span className="absolute right-4 top-1/2 -translate-y-1/2 text-xs font-mono text-destructive tabular-nums">{formatTime(timeLeft)}</span>
                    ) : (
                      <span className="absolute right-4 top-1/2 -translate-y-1/2 text-xs text-gray-400">만료</span>
                    )}
                  </div>
                  <button
                    type="button"
                    onClick={handleVerifySms}
                    disabled={smsVerifying || smsCode.length !== 6 || timeLeft === 0}
                    className="shrink-0 px-4 py-3.5 rounded-2xl text-sm font-bold bg-primary text-white transition-spring active:scale-[0.97] hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {smsVerifying ? <RefreshCw size={14} className="animate-spin" /> : '확인'}
                  </button>
                </motion.div>
              )}

              {smsError && <p className="text-xs text-destructive mt-1.5 ml-1">{smsError}</p>}
              {smsSent && !smsVerified && (
                <p className="text-xs text-gray-400 mt-1.5 ml-1">
                  {import.meta.env.VITE_USE_MOCK === 'true' ? '테스트 인증번호: 123456' : `${phone}으로 인증번호를 발송했습니다.`}
                </p>
              )}
              {smsVerified && (
                <p className="flex items-center gap-1.5 text-sm font-bold text-green-600 mt-1">
                  <CheckCircle2 size={16} /> {phone} 인증 완료
                </p>
              )}
            </div>
          </div>

          {/* STEP 2: 가입 방법 선택 (인증 후 활성화) */}
          <div className={`space-y-3 transition-opacity duration-300 ${smsVerified ? 'opacity-100' : 'opacity-40 pointer-events-none'}`}>
            <p className="text-xs font-bold text-gray-500 uppercase tracking-wide">STEP 2 · 가입 방법 선택</p>

            {/* 소셜 로그인 */}
            <button
              onClick={() => handleSocialLogin('google')}
              className="w-full flex items-center justify-center gap-3 py-3.5 border border-gray-200 rounded-2xl bg-white hover:bg-gray-50 active:scale-[0.98] transition-all"
            >
              <svg width="20" height="20" viewBox="0 0 48 48">
                <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
                <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
                <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
                <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
              </svg>
              <span className="font-bold text-sm text-gray-700">Google로 계속하기</span>
            </button>

            <button
              onClick={() => handleSocialLogin('kakao')}
              className="w-full flex items-center justify-center gap-3 py-3.5 border border-yellow-300 rounded-2xl bg-[#FEE500] hover:bg-[#FDD800] active:scale-[0.98] transition-all"
            >
              <svg width="20" height="20" viewBox="0 0 48 48">
                <path fill="#3C1E1E" d="M24 4C12.95 4 4 11.16 4 20c0 5.6 3.6 10.5 9.09 13.37l-2.32 8.55c-.2.75.64 1.35 1.3.92L20.55 37c1.12.15 2.27.24 3.45.24 11.05 0 20-7.16 20-16S35.05 4 24 4z"/>
              </svg>
              <span className="font-bold text-sm text-gray-900">카카오로 계속하기</span>
            </button>

            <div className="flex items-center gap-4">
              <div className="flex-1 h-px bg-gray-200" />
              <span className="text-xs text-gray-400">또는 이메일로 가입</span>
              <div className="flex-1 h-px bg-gray-200" />
            </div>

            {/* 이메일 가입 폼 */}
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-bold text-gray-700 mb-1.5">이메일</label>
                <div className="relative">
                  <Mail size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" />
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="example@email.com"
                    className="w-full pl-11 pr-4 py-3.5 bg-gray-50 border border-gray-200 rounded-2xl focus:bg-white focus:border-primary outline-none transition-colors text-sm"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-bold text-gray-700 mb-1.5">비밀번호</label>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="6자리 이상 입력해주세요"
                    className="w-full px-4 py-3.5 bg-gray-50 border border-gray-200 rounded-2xl focus:bg-white focus:border-primary outline-none transition-colors text-sm pr-12"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                  >
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
                {password.length > 0 && password.length < 6 && (
                  <p className="text-xs text-destructive mt-1 ml-1">6자리 이상 입력해주세요</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-bold text-gray-700 mb-1.5">닉네임</label>
                <input
                  type="text"
                  value={nickname}
                  onChange={(e) => setNickname(e.target.value)}
                  placeholder="활동할 닉네임을 입력해주세요"
                  className="w-full px-4 py-3.5 bg-gray-50 border border-gray-200 rounded-2xl focus:bg-white focus:border-primary outline-none transition-colors text-sm"
                  maxLength={20}
                />
              </div>

              {/* 약관 동의 */}
              <label className="flex items-start gap-3 cursor-pointer mt-2 p-3 bg-gray-50 rounded-2xl">
                <input
                  type="checkbox"
                  checked={agreeTerms}
                  onChange={(e) => setAgreeTerms(e.target.checked)}
                  className="w-5 h-5 mt-0.5 rounded border-gray-300 text-primary accent-[var(--primary)] shrink-0"
                />
                <span className="text-xs text-gray-600 leading-relaxed">
                  <span className="font-bold text-gray-800">이용약관</span> 및 <span className="font-bold text-gray-800">개인정보처리방침</span>에 동의합니다
                </span>
              </label>

              {error && <p className="text-red-500 text-sm px-1">{error}</p>}

              <button
                type="submit"
                disabled={!isValid || isLoading}
                className={`w-full py-4 rounded-2xl font-bold shadow-md transition-spring active:scale-[0.98] hover:scale-[1.02] mt-2 ${
                  isValid && !isLoading
                    ? 'bg-primary text-white hover:bg-primary/90'
                    : 'bg-gray-200 text-gray-400 cursor-not-allowed shadow-none'
                }`}
              >
                {isLoading ? '가입 중...' : '가입하기'}
              </button>
            </form>
          </div>

          {/* 로그인 링크 */}
          <div className="text-center text-sm text-gray-400">
            이미 계정이 있으신가요?{' '}
            <button onClick={() => onNavigate('login')} className="font-bold text-primary hover:underline">
              로그인
            </button>
          </div>
        </motion.div>
      </main>
    </div>
  );
}