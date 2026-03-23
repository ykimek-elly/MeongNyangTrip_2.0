import api from './axios';
import type { ApiResponse } from './types';

/** 대화 목록 단건 (DM 목록 화면용) */
export interface DmConversationDto {
  partnerId: string;
  partnerImg: string;
  lastMessage: string;
  lastMessageAt: string;
  unreadCount: number;
}

/** 메시지 단건 */
export interface DmMessageDto {
  id: number;
  fromId: string;
  content: string;
  createdAt: string;
  isRead: boolean;
}

/**
 * DM API 서비스.
 * 백엔드 DmController 엔드포인트와 매핑된다.
 *
 * GET  /api/v1/dms                    → 내 대화 목록 + 읽지 않은 메시지 수
 * GET  /api/v1/dms/{partnerId}        → 특정 상대와의 메시지 목록
 * POST /api/v1/dms/{partnerId}        → 메시지 전송
 * PATCH /api/v1/dms/{partnerId}/read  → 해당 대화 전체 읽음 처리
 */
export const dmApi = {
  /** GET /api/v1/dms — 내 대화 목록 */
  getConversations: async (): Promise<DmConversationDto[]> => {
    const { data } = await api.get<ApiResponse<DmConversationDto[]>>('/dms');
    return data.data ?? [];
  },

  /** GET /api/v1/dms/{partnerId} — 메시지 목록 */
  getMessages: async (partnerId: string): Promise<DmMessageDto[]> => {
    const { data } = await api.get<ApiResponse<DmMessageDto[]>>(
      `/dms/${encodeURIComponent(partnerId)}`
    );
    return data.data ?? [];
  },

  /** POST /api/v1/dms/{partnerId} — 메시지 전송 */
  sendMessage: async (partnerId: string, content: string): Promise<DmMessageDto> => {
    const { data } = await api.post<ApiResponse<DmMessageDto>>(
      `/dms/${encodeURIComponent(partnerId)}`,
      { content }
    );
    if (!data.data) throw new Error('메시지 전송에 실패했습니다.');
    return data.data;
  },

  /** PATCH /api/v1/dms/{partnerId}/read — 전체 읽음 처리 */
  markAllRead: async (partnerId: string): Promise<void> => {
    await api.patch(`/dms/${encodeURIComponent(partnerId)}/read`);
  },
};
