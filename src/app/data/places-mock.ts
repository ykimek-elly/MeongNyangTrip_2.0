// MapSearch와 같이 쓰이는 근처 감성 스팟 범용 Mock Data
import { PlaceDto } from '../api/types';

export const NEARBY_SPOTS: PlaceDto[] = [
  {
    id: 2,
    name: '서울숲 반려동물 구역',
    address: '서울특별시 성동구 뚝섬로 273',
    lat: 37.5665,
    lng: 126.9780,
    imageUrl: 'https://images.unsplash.com/photo-1597633425046-08f5110420b5?w=600&q=80',
    category: 'PARK',
    // UI에서 임시로 사용할 확장 프로퍼티들 (API Dto 외)
    tag: '햇살맛집',
    desc: '따스한 햇살이 가득한 반려동물 전용 구역',
    rating: 4.9,
    distance: '0.8km',
  } as any, // DTO 확장을 위해 any 단언
  {
    id: 3,
    name: '남양주 물의정원',
    address: '경기도 남양주시 조안면 북한강로 398',
    lat: 37.5650,
    lng: 126.9800,
    imageUrl: 'https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=600&q=80',
    category: 'PARK',
    tag: '조용한산책',
    desc: '사람이 적고 한적한 힐링 스팟',
    rating: 4.8,
    distance: '1.2km',
  } as any,
  {
    id: 4,
    name: '하늘공원 산책로',
    address: '서울특별시 마포구 상암동 하늘공원로 95',
    lat: 37.5680,
    lng: 126.9750,
    imageUrl: 'https://images.unsplash.com/photo-1571570776991-b3b4d4982a0d?w=600&q=80',
    category: 'RUN',
    tag: '뛰뛰가능',
    desc: '목줄 없이 자유롭게 뛰노는 운동장',
    rating: 4.7,
    distance: '2.5km',
  } as any,
];
