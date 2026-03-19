// MapSearch와 같이 쓰이는 근처 감성 스팟 범용 Mock Data
import { PlaceDto } from '../api/types';

export const NEARBY_SPOTS: PlaceDto[] = [
  {
    id: 2,
    title: '서울숲 반려동물 구역',
    address: '서울특별시 성동구 뚝섬로 273',
    latitude: 37.5444,
    longitude: 127.0374,
    imageUrl: 'https://images.unsplash.com/photo-1597633425046-08f5110420b5?w=600&q=80',
    category: 'PLACE',
    rating: 4.9,
    aiRating: 4.9,
  } as any,
  {
    id: 3,
    title: '남양주 물의정원',
    address: '경기도 남양주시 조안면 북한강로 398',
    latitude: 37.6389,
    longitude: 127.3185,
    imageUrl: 'https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=600&q=80',
    category: 'PLACE',
    rating: 4.8,
    aiRating: 4.8,
  } as any,
  {
    id: 4,
    title: '하늘공원 산책로',
    address: '서울특별시 마포구 상암동 하늘공원로 95',
    latitude: 37.5703,
    longitude: 126.8940,
    imageUrl: 'https://images.unsplash.com/photo-1571570776991-b3b4d4982a0d?w=600&q=80',
    category: 'PLACE',
    rating: 4.7,
    aiRating: 4.7,
  } as any,
];
