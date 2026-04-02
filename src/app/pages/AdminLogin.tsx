import React, { useState } from 'react';
import { motion } from 'motion/react';
import { useAppStore } from '../store/useAppStore';
import { authApi } from '../api/authApi';
import { Shield, Mail, Eye, EyeOff, Leaf, LogIn } from 'lucide-react';

interface AdminLoginProps {
  onNavigate: (page: string) => void;
}

export function AdminLogin({ onNavigate }: AdminLoginProps) {
  const login = useAppStore(state => state.login);
  const [email, setEmail]           = useState('');
  const [password, setPassword]     = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError]           = useState('');
  const [isLoading, setIsLoading]   = useState(false);

  const isValid = email.trim() && password.length >= 6;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isValid) return;
    setError('');
    setIsLoading(true);
    try {
      const res = await authApi.login(email, password);
      const isAdmin = res.role === 'ADMIN';
      if (!isAdmin) {
        setError('관리자 계정이 아닙니다.');
        return;
      }
      localStorage.setItem('accessToken', res.token);
      login(res.nickname, res.email, res.userId, res.profileImage, true);
      onNavigate('admin');
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg || '로그인에 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-950 flex flex-col items-center justify-center px-6">
      <motion.div
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-[380px]"
      >
        {/* 로고 */}
        <div className="text-center mb-10">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-primary/20 rounded-2xl mb-4">
            <Shield size={32} className="text-primary" />
          </div>
          <div className="flex items-center justify-center gap-2 mb-1">
            <Leaf size={18} className="text-primary" />
            <span className="text-white font-bold text-lg tracking-tight">멍냥트립</span>
          </div>
          <p className="text-gray-400 text-sm">관리자 포털</p>
        </div>

        {/* 폼 */}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-bold text-gray-400 mb-1.5 uppercase tracking-wider">
              관리자 이메일
            </label>
            <div className="relative">
              <Mail size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-500" />
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="admin@meongtrip.com"
                className="w-full pl-10 pr-4 py-3.5 bg-gray-800 border border-gray-700 rounded-xl text-white text-sm placeholder-gray-600 outline-none focus:border-primary transition-spring"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-gray-400 mb-1.5 uppercase tracking-wider">
              비밀번호
            </label>
            <div className="relative">
              <input
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full px-4 py-3.5 bg-gray-800 border border-gray-700 rounded-xl text-white text-sm placeholder-gray-600 outline-none focus:border-primary transition-spring pr-11"
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3.5 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-300"
              >
                {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
          </div>

          {error && (
            <div className="flex items-center gap-2 bg-red-500/10 border border-red-500/30 rounded-xl px-3.5 py-2.5">
              <Shield size={14} className="text-red-400 shrink-0" />
              <p className="text-red-400 text-xs">{error}</p>
            </div>
          )}

          <button
            type="submit"
            disabled={!isValid || isLoading}
            className={`w-full py-3.5 rounded-xl font-bold text-sm flex items-center justify-center gap-2 transition-spring mt-2 ${
              isValid && !isLoading
                ? 'bg-primary text-white hover:bg-primary/90 active:scale-[0.98]'
                : 'bg-gray-700 text-gray-500 cursor-not-allowed'
            }`}
          >
            <LogIn size={16} />
            {isLoading ? '인증 중...' : '관리자 로그인'}
          </button>
        </form>

        {/* 구분선 */}
        <div className="mt-8 pt-6 border-t border-gray-800 text-center">
          <button
            onClick={() => onNavigate('home')}
            className="text-xs text-gray-600 hover:text-gray-400 transition-spring"
          >
            ← 일반 사용자 페이지로 돌아가기
          </button>
        </div>
      </motion.div>
    </div>
  );
}
