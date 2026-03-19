package com.team.meongnyang.place.entity;

/**
 * 장소 상태 Enum.
 * 파이프라인 Stage2 카카오 검증 결과에 따라 분류됨.
 */
public enum PlaceStatus {
    /** 정상 노출 — 검증 통과 또는 관리자 승인 */
    ACTIVE,
    /** 관리자 검토 대기 — 카카오 상호명 유사도 50~79% */
    PENDING,
    /** 폐기 — 유사도 50% 미만 또는 관리자 거절 */
    REJECTED
}
