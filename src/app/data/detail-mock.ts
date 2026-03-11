/**
 * TODO (팀원 D - FE): 백엔드 API 연동 후 삭제/대체할 더미 데이터 파일입니다.
 * 장소 상세 정보(DETAIL_EXTRA) 및 리뷰(MOCK_REVIEWS)를 임시로 보여주기 위해 쓰입니다.
 */
import { Dog, Car } from 'lucide-react';

export const MOCK_IMAGES = [
  "https://images.unsplash.com/photo-1523987355523-c7b5b0dd90a7?w=800&q=80",
  "https://images.unsplash.com/photo-1510798831971-661eb04b3739?w=800&q=80",
  "https://images.unsplash.com/photo-1534349762913-92c29654d68e?w=800&q=80",
  "https://images.unsplash.com/photo-1587595431973-160d0d94add1?w=800&q=80"
];

// 시설 정보 태그
export const AMENITIES = [
  { icon: Dog, label: "반려동물 동반" },
  { icon: Car, label: "주차가능" },
];

// 장소별 추가 정보
export const DETAIL_EXTRA: Record<number, {
  address: string;
  tags: string[];
  description: string;
  instagram?: string;
}> = {
  1: {
    address: "경기도 가평군 청평면 호명산로 123",
    tags: ["#글램핑", "#반려동물동반", "#자연"],
    description: "멍스테이 글램핑은 반려동물과 함께하는 특별한 글램핑 경험을 제공합니다. 넓은 운동장과 전용 독채에서 아이들이 마음껏 뛰어놀 수 있습니다.",
    instagram: "@mungstay_glamping",
  },
  2: {
    address: "서울특별시 성동구 뚝섬로 273",
    tags: ["#공원", "#산책", "#서울숲"],
    description: "서울숲 반려동물 구역은 도심 속 반려동물 전용 놀이터입니다. 넓은 잔디밭과 산책로가 조성되어 있습니다.",
    instagram: "@seoul_forest_pet",
  },
  3: {
    address: "경기도 남양주시 조안면 북한강로 398",
    tags: ["#자연", "#산책로", "#꽃"],
    description: "남양주 물의정원은 한강변에 위치한 아름다운 생태공원으로 반려동물 산책에 최적의 장소입니다.",
    instagram: "@water_garden_ny",
  },
  4: {
    address: "서울특별시 마포구 상암동 하늘공원로 95",
    tags: ["#공원", "#하늘공원", "#억새"],
    description: "하늘공원 산책로는 서울에서 가장 높은 공원으로, 반려동물과 함께 탁 트인 전망을 즐길 수 있습니다.",
    instagram: "@sky_park_seoul",
  },
  5: {
    address: "경기도 용인시 처인구 모현읍 왕산리 456",
    tags: ["#카페", "#브런치", "#테라스"],
    description: "우드무드 카페는 넓은 테라스에서 반려동물과 여유로운 시간을 보낼 수 있는 감성 카페입니다.",
    instagram: "@woodmood_cafe",
  },
  6: {
    address: "강원도 홍천군 서면 팔봉산로 789",
    tags: ["#리조트", "#자연", "#숲"],
    description: "포레스트 리조트는 울창한 숲 속에서 반려동물과 특별한 휴식을 즐길 수 있는 프리미엄 리조트입니다.",
    instagram: "@forest_resort_hc",
  },
  7: {
    address: "부산광역시 해운대구 해운대해변로 264",
    tags: ["#해변", "#바다", "#산책"],
    description: "해운대 반려 해변은 반려동물 전용 해변 구역으로, 모래사장에서 함께 뛰어놀 수 있습니다.",
    instagram: "@haeundae_pet_beach",
  },
  8: {
    address: "서울특별시 강남구 압구정로 234",
    tags: ["#브런치", "#테라스", "#강남"],
    description: "테라스독 브런치는 반려동물 동반 가능한 프리미엄 브런치 카페입니다.",
    instagram: "@terrace_dog_brunch",
  },
};

// 리뷰 목업 데이터
export interface Review {
  id: number;
  author: string;
  date: string;
  rating: number;
  content: string;
}

export const MOCK_REVIEWS: Review[] = [
  {
    id: 1,
    author: "멍냥이맘",
    date: "2025.07.08",
    rating: 5,
    content: "멍이가 정말대로 사장님이 친절하시 있는 무슨 도시가 너무 놀기 좋았습니다.",
  },
  {
    id: 2,
    author: "초코아빠",
    date: "2025.07.08",
    rating: 4,
    content: "주변 공간이 넓어서 멍에게 자유롭습니다. 대체적 만이 더했으면합니다.",
  },
];
