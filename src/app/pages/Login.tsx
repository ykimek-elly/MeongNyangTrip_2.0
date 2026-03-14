import React, { useState } from 'react';
import { Leaf } from 'lucide-react';
import { useAppStore } from '../store/useAppStore';
import { authApi } from '../api/authApi';

import { motion } from 'motion/react';

interface LoginProps {
  onNavigate: (page: string) => void;
}

export function Login({ onNavigate }: LoginProps) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const login = useAppStore(state => state.login);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !password.trim()) return setError('이메일과 비밀번호를 입력해주세요.');
    setError('');
    setIsLoading(true);
    try {
      const res = await authApi.login(email, password);
      localStorage.setItem('accessToken', res.token);
      login(res.nickname, res.email, res.userId);
      onNavigate('home');
    } catch {
      setError('이메일 또는 비밀번호가 올바르지 않습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      className="min-h-screen bg-white flex flex-col justify-center px-6"
    >
      <div className="text-center mb-10">
        <div className="inline-flex items-center justify-center w-20 h-20 bg-primary/10 rounded-full mb-6">
          <Leaf size={40} className="text-primary" />
        </div>
        <h2 className="text-3xl font-bold text-gray-900 mb-2">멍냥트립</h2>
        <p className="text-gray-500">반려동물과 함께하는 자연 여행</p>
      </div>

      <form onSubmit={handleSubmit} className="w-full max-w-sm mx-auto space-y-3">
        <input
          type="email"
          placeholder="이메일"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="w-full p-4 bg-white border border-gray-200 rounded-2xl shadow-sm outline-none focus:border-primary transition-colors"
        />
        <input
          type="password"
          placeholder="비밀번호"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className="w-full p-4 bg-white border border-gray-200 rounded-2xl shadow-sm outline-none focus:border-primary transition-colors mb-4"
        />
        {error && <p className="text-red-500 text-sm px-1">{error}</p>}

        <button
          type="submit"
          disabled={isLoading}
          className="w-full bg-primary text-white font-bold py-4 rounded-2xl shadow-md hover:bg-primary/90 active:scale-[0.98] transition-all mt-4 disabled:opacity-60"
        >
          {isLoading ? '로그인 중...' : '로그인'}
        </button>
      </form>

      <div className="mt-8 text-center flex justify-center text-sm text-gray-500 space-x-4">
        <span onClick={() => onNavigate('find-id')} className="cursor-pointer hover:text-gray-800">아이디 찾기</span>
        <span className="text-gray-300">|</span>
        <span onClick={() => onNavigate('find-password')} className="cursor-pointer hover:text-gray-800">비밀번호 찾기</span>
        <span className="text-gray-300">|</span>
        <span onClick={() => onNavigate('signup')} className="cursor-pointer hover:text-gray-800">회원가입</span>
      </div>
      
      <button 
        onClick={() => onNavigate('home')} 
        className="mt-12 text-gray-400 text-sm underline decoration-gray-300 underline-offset-4"
      >
        둘러보기
      </button>
    </motion.div>
  );
}
