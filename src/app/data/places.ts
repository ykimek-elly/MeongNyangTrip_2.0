import { PlaceDto } from '../api/types';

/**
 * 프론트엔드 내에서 사용하는 장소 타입.
 * 기존의 더미데이터 프로퍼티(cat, loc, img 등) 대신 백엔드의 PlaceDto 규격을 따릅니다.
 */
export type Place = PlaceDto;

// 기존 코드와의 호환성을 위해 빈 배열을 남겨둠 (추후 각 페이지는 API 호출로 전환)
export const places: Place[] = [];