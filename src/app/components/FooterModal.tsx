import React from 'react';
import { X } from 'lucide-react';

type ModalType = 'terms' | 'privacy' | 'support';

interface FooterModalProps {
  type: ModalType;
  onClose: () => void;
}

/**
 * 로그인 페이지 하단 모달 (이용약관 / 개인정보처리방침 / 고객센터).
 * lazy import로 분리 — 초기 번들에서 제외, 클릭 시에만 로드.
 */
export default function FooterModal({ type, onClose }: FooterModalProps) {
  const content: Record<ModalType, { title: string; body: React.ReactNode }> = {
    terms: {
      title: '이용약관',
      body: (
        <div className="space-y-4 text-sm text-gray-600 leading-relaxed">
          <section>
            <h3 className="font-bold text-gray-800 mb-1">제1조 (목적)</h3>
            <p>본 약관은 멍냥트립(이하 "서비스")이 제공하는 반려동물 동반 여행 정보 서비스의 이용 조건 및 절차에 관한 사항을 규정합니다.</p>
          </section>
          <section>
            <h3 className="font-bold text-gray-800 mb-1">제2조 (회원가입)</h3>
            <p>서비스 이용을 위해 회원가입이 필요하며, 만 14세 이상만 가입 가능합니다. 소셜 로그인(Google, Kakao)을 통한 간편 가입을 지원합니다.</p>
          </section>
          <section>
            <h3 className="font-bold text-gray-800 mb-1">제3조 (서비스 이용)</h3>
            <p>회원은 서비스를 통해 반려동물 동반 가능 장소 검색, 리뷰 작성, 여행 코스 저장 등의 기능을 이용할 수 있습니다.</p>
          </section>
          <section>
            <h3 className="font-bold text-gray-800 mb-1">제4조 (금지 행위)</h3>
            <p>타인의 정보 도용, 허위 정보 게재, 서비스 운영 방해 등의 행위는 금지되며 위반 시 이용이 제한될 수 있습니다.</p>
          </section>
          <section>
            <h3 className="font-bold text-gray-800 mb-1">제5조 (책임 제한)</h3>
            <p>서비스는 장소 정보의 정확성을 위해 노력하나, 실제 운영 상황과 다를 수 있습니다. 방문 전 해당 업체에 직접 확인을 권장합니다.</p>
          </section>
          <p className="text-xs text-gray-400 mt-4">시행일: 2025년 1월 1일</p>
        </div>
      ),
    },
    privacy: {
      title: '개인정보처리방침',
      body: (
        <div className="space-y-4 text-sm text-gray-600 leading-relaxed">
          <section>
            <h3 className="font-bold text-gray-800 mb-1">수집하는 개인정보</h3>
            <p>이메일 주소, 닉네임, 프로필 사진(소셜 로그인 시), 반려동물 정보(선택)</p>
          </section>
          <section>
            <h3 className="font-bold text-gray-800 mb-1">수집 목적</h3>
            <p>회원 식별 및 서비스 제공, 반려동물 맞춤 장소 추천, AI 산책 가이드 개인화</p>
          </section>
          <section>
            <h3 className="font-bold text-gray-800 mb-1">보유 및 이용 기간</h3>
            <p>회원 탈퇴 시까지 보유하며, 탈퇴 후 30일 이내 파기합니다. 단, 관계 법령에 따라 일부 정보는 일정 기간 보존될 수 있습니다.</p>
          </section>
          <section>
            <h3 className="font-bold text-gray-800 mb-1">제3자 제공</h3>
            <p>이용자의 동의 없이 개인정보를 제3자에게 제공하지 않습니다. 단, 법령에 의한 경우는 예외입니다.</p>
          </section>
          <section>
            <h3 className="font-bold text-gray-800 mb-1">이용자의 권리</h3>
            <p>이용자는 언제든지 개인정보 열람, 수정, 삭제를 요청할 수 있으며 고객센터를 통해 처리됩니다.</p>
          </section>
          <p className="text-xs text-gray-400 mt-4">시행일: 2025년 1월 1일</p>
        </div>
      ),
    },
    support: {
      title: '고객센터',
      body: (
        <div className="space-y-5 text-sm text-gray-600">
          <div className="bg-primary/5 rounded-2xl p-4 space-y-2">
            <p className="font-bold text-gray-800">이메일 문의</p>
            <p className="text-primary font-medium">support@meongnyangtrip.com</p>
            <p className="text-xs text-gray-400">평일 09:00 ~ 18:00 (주말·공휴일 제외)</p>
          </div>
          <div className="space-y-3">
            <p className="font-bold text-gray-800">자주 묻는 질문</p>
            <div className="space-y-2">
              {[
                { q: '반려동물 정보는 어떻게 수정하나요?', a: '마이페이지 > 반려동물 관리에서 수정할 수 있습니다.' },
                { q: '소셜 로그인 계정을 변경하고 싶어요.', a: '현재 계정 탈퇴 후 새 계정으로 재가입이 필요합니다.' },
                { q: '장소 정보가 잘못되었어요.', a: '장소 상세 페이지에서 오류 신고 버튼을 이용해주세요.' },
              ].map(({ q, a }) => (
                <div key={q} className="bg-gray-50 rounded-xl p-3">
                  <p className="font-medium text-gray-700 mb-1">Q. {q}</p>
                  <p className="text-gray-500 text-xs">A. {a}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      ),
    },
  };

  const { title, body } = content[type];

  return (
    // motion.div → CSS 애니메이션으로 교체
    <div className="fixed inset-0 z-50 flex items-end justify-center" onClick={onClose}>
      <div className="absolute inset-0 bg-black/40" />
      <div
        className="relative w-full max-w-[600px] bg-white rounded-t-3xl max-h-[75vh] flex flex-col animate-slide-up"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
          <h2 className="text-base font-bold text-gray-900">{title}</h2>
          <button onClick={onClose} className="p-1 text-gray-400 hover:text-gray-600">
            <X size={20} />
          </button>
        </div>
        <div className="overflow-y-auto px-6 py-5">{body}</div>
      </div>
    </div>
  );
}
