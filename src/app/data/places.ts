export interface Place {
  id: number;
  title: string;
  cat: 'place' | 'stay' | 'dining';
  loc: string;
  img: string;
  rating: number;
  reviewCount: number;
}

export const places: Place[] = [
  { 
    id: 1, 
    title: "멍스테이 글램핑", 
    cat: "stay", 
    loc: "경기 가평", 
    img: "https://images.unsplash.com/photo-1523987355523-c7b5b0dd90a7?w=600&q=80",
    rating: 4.8,
    reviewCount: 120
  },
  { 
    id: 2, 
    title: "서울숲 반려동물 구역", 
    cat: "place", 
    loc: "서울 성동구", 
    img: "https://images.unsplash.com/photo-1597633425046-08f5110420b5?w=600&q=80",
    rating: 4.9,
    reviewCount: 230
  },
  { 
    id: 3, 
    title: "남양주 물의정원", 
    cat: "place", 
    loc: "경기 남양주", 
    img: "https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=600&q=80",
    rating: 4.8,
    reviewCount: 85
  },
  { 
    id: 4, 
    title: "하늘공원 산책로", 
    cat: "place", 
    loc: "서울 마포구", 
    img: "https://images.unsplash.com/photo-1571570776991-b3b4d4982a0d?w=600&q=80",
    rating: 4.7,
    reviewCount: 210
  },
  { 
    id: 5, 
    title: "우드무드 카페", 
    cat: "dining", 
    loc: "경기 용인", 
    img: "https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=600&q=80",
    rating: 4.6,
    reviewCount: 150
  },
  { 
    id: 6, 
    title: "포레스트 리조트", 
    cat: "stay", 
    loc: "강원 홍천", 
    img: "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=600&q=80",
    rating: 4.9,
    reviewCount: 300
  },
  { 
    id: 7, 
    title: "해운대 반려 해변", 
    cat: "place", 
    loc: "부산 해운대", 
    img: "https://images.unsplash.com/photo-1770816605358-222129df8658?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxwZXQlMjBmcmllbmRseSUyMGJlYWNoJTIwZG9nJTIwb2NlYW58ZW58MXx8fHwxNzcyNjkzMjQ4fDA&ixlib=rb-4.1.0&q=80&w=1080&utm_source=figma&utm_medium=referral",
    rating: 4.7,
    reviewCount: 175
  },
  { 
    id: 8, 
    title: "테라스독 브런치", 
    cat: "dining", 
    loc: "서울 강남구", 
    img: "https://images.unsplash.com/photo-1764475493462-9083c3e59047?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxwZXQlMjBmcmllbmRseSUyMHJlc3RhdXJhbnQlMjB0ZXJyYWNlJTIwZG9nfGVufDF8fHx8MTc3MjY5MzI0OHww&ixlib=rb-4.1.0&q=80&w=1080&utm_source=figma&utm_medium=referral",
    rating: 4.5,
    reviewCount: 92
  },
];