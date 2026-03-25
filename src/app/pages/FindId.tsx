import React, { useState } from 'react';
import { motion } from 'motion/react';
import { ArrowLeft, Smartphone, User } from 'lucide-react';
import { authApi } from '../api/authApi';

interface FindIdProps {
  onNavigate: (page: string) => void;
}

export function FindId({ onNavigate }: FindIdProps) {
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [result, setResult] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleFind = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name || !phone) return;
    setIsLoading(true);
    setError(null);
    try {
      const email = await authApi.findId(name, phone);
      setResult(email);
    } catch {
      setError('입력하신 정보와 일치하는 계정을 찾을 수 없습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ type: 'spring', damping: 28, stiffness: 320 }}
      className="min-h-screen bg-white flex flex-col"
    >
      <header className="px-5 py-4 flex items-center bg-white border-b border-gray-100 sticky top-0 z-10">
        <button
          onClick={() => onNavigate('login')}
          className="p-2 -ml-2 text-gray-600 hover:bg-gray-100 rounded-full transition-spring hover:scale-[1.1] active:scale-[0.9]"
          aria-label="로그인 화면으로 이동"
        >
          <ArrowLeft size={24} />
        </button>
        <h1 className="text-lg font-bold ml-2">아이디 찾기</h1>
      </header>

      <main className="flex-1 p-6">
        {!result ? (
          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ type: 'spring', damping: 28, stiffness: 320, delay: 0.1 }}
          >
            <div className="mb-8">
              <h2 className="text-2xl font-bold text-gray-900 mb-2">가입 시 등록한<br/>정보를 입력해주세요.</h2>
            </div>

            <form onSubmit={handleFind} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">이름</label>
                <div className="relative">
                  <User className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
                  <input 
                    type="text" 
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="이름 입력"
                    className="w-full pl-12 p-4 bg-gray-50 border border-gray-200 rounded-2xl focus:bg-white focus:border-primary focus:shadow-[0_0_0_3px_rgba(227,99,148,0.1)] outline-none transition-spring"
                  />
                </div>
              </div>
              
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">휴대폰 번호</label>
                <div className="relative">
                  <Smartphone className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
                  <input 
                    type="tel" 
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    placeholder="010-0000-0000"
                    className="w-full pl-12 p-4 bg-gray-50 border border-gray-200 rounded-2xl focus:bg-white focus:border-primary focus:shadow-[0_0_0_3px_rgba(227,99,148,0.1)] outline-none transition-spring"
                  />
                </div>
              </div>

              {error && (
                <p className="text-sm text-red-500 text-center">{error}</p>
              )}
              <button
                type="submit"
                disabled={!name || !phone || isLoading}
                className={`w-full py-4 rounded-2xl font-bold text-lg shadow-md transition-spring mt-8 ${
                  name && phone && !isLoading
                    ? 'bg-primary text-white hover:bg-primary/90 hover:scale-[1.02] active:scale-[0.97]'
                    : 'bg-gray-200 text-gray-400 cursor-not-allowed'
                }`}
              >
                {isLoading ? '확인 중...' : '아이디 찾기'}
              </button>
            </form>
          </motion.div>
        ) : (
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ type: 'spring', damping: 25, stiffness: 350 }}
            className="text-center pt-10"
          >
            <div className="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-6">
              <User className="text-primary" size={32} />
            </div>
            <h2 className="text-xl font-bold text-gray-900 mb-2">회원님의 아이디를 찾았어요!</h2>
            <p className="text-gray-500 mb-8">
              가입하신 이메일은<br/>
              <span className="text-primary font-bold text-lg">{result}</span> 입니다.
            </p>
            
            <div className="space-y-3">
              <button
                onClick={() => onNavigate('login')}
                className="w-full py-4 bg-primary text-white font-bold rounded-2xl shadow-md hover:bg-primary/90 transition-spring hover:scale-[1.02] active:scale-[0.97]"
              >
                로그인하러 가기
              </button>
              <button
                onClick={() => onNavigate('find-password')}
                className="w-full py-4 bg-gray-100 text-gray-600 font-bold rounded-2xl hover:bg-gray-200 transition-spring hover:scale-[1.02] active:scale-[0.97]"
              >
                비밀번호 찾기
              </button>
            </div>
          </motion.div>
        )}
      </main>
    </motion.div>
  );
}