import React, { useState } from 'react';
import { motion } from 'motion/react';
import { ArrowLeft, Smartphone, User, Lock, CheckCircle } from 'lucide-react';
import { authApi } from '../api/authApi';

interface FindPasswordProps {
  onNavigate: (page: string) => void;
}

export function FindPassword({ onNavigate }: FindPasswordProps) {
  const [id, setId] = useState('');
  const [phone, setPhone] = useState('');
  const [isSent, setIsSent] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleFind = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !phone) return;
    setIsLoading(true);
    setError(null);
    try {
      await authApi.resetPassword(id, phone);
      setIsSent(true);
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
        <h1 className="text-lg font-bold ml-2">비밀번호 찾기</h1>
      </header>

      <main className="flex-1 p-6">
        {!isSent ? (
          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ type: 'spring', damping: 28, stiffness: 320, delay: 0.1 }}
          >
            <div className="mb-8">
              <h2 className="text-2xl font-bold text-gray-900 mb-2">비밀번호를 잊으셨나요?</h2>
              <p className="text-gray-500 text-sm">가입한 아이디와 휴대폰 번호를 입력하시면<br/>임시 비밀번호를 발송해드립니다.</p>
            </div>

            <form onSubmit={handleFind} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">아이디</label>
                <div className="relative">
                  <User className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
                  <input 
                    type="text" 
                    value={id}
                    onChange={(e) => setId(e.target.value)}
                    placeholder="아이디 입력"
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
                disabled={!id || !phone || isLoading}
                className={`w-full py-4 rounded-2xl font-bold text-lg shadow-md transition-spring mt-8 ${
                  id && phone && !isLoading
                    ? 'bg-primary text-white hover:bg-primary/90 hover:scale-[1.02] active:scale-[0.97]'
                    : 'bg-gray-200 text-gray-400 cursor-not-allowed'
                }`}
              >
                {isLoading ? '발송 중...' : '임시 비밀번호 발송'}
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
            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-6">
              <CheckCircle className="text-green-500" size={32} />
            </div>
            <h2 className="text-xl font-bold text-gray-900 mb-2">발송 완료!</h2>
            <p className="text-gray-500 mb-8">
              입력하신 휴대폰 번호로<br/>
              임시 비밀번호를 보내드렸어요.
            </p>
            
            <button
              onClick={() => onNavigate('login')}
              className="w-full py-4 bg-primary text-white font-bold rounded-2xl shadow-md hover:bg-primary/90 transition-spring hover:scale-[1.02] active:scale-[0.97]"
            >
              로그인하러 가기
            </button>
          </motion.div>
        )}
      </main>
    </motion.div>
  );
}