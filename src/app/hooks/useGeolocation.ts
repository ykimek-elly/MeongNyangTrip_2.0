declare global {
  interface Window {
    kakao: any;
  }
}

import { useState, useCallback } from 'react';

interface GeolocationState {
  lat: number | null;
  lng: number | null;
  address: string | null;
  error: string | null;
  isLoading: boolean;
}

export function useGeolocation() {
  const [state, setState] = useState<GeolocationState>({
    lat: null,
    lng: null,
    address: null,
    error: null,
    isLoading: false,
  });

  const getLocation = useCallback(() => {
    setState((prev) => ({ ...prev, isLoading: true, error: null }));

    if (!navigator.geolocation) {
      setState((prev) => ({
        ...prev,
        isLoading: false,
        error: "이 브라우저에서는 위치 기능을 지원하지 않습니다.",
      }));
      return;
    }

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        const { latitude, longitude } = position.coords;
        try {
          // 카카오맵 Geocoder를 이용해 좌표를 주소로 변환
          const address = await reverseGeocode(latitude, longitude);
          setState({
            lat: latitude,
            lng: longitude,
            address,
            error: null,
            isLoading: false,
          });
        } catch (error) {
          setState({
            lat: latitude,
            lng: longitude,
            address: null, // 주소 변환 실패해도 좌표는 저장
            error: "주소를 가져오는데 실패했습니다.",
            isLoading: false,
          });
        }
      },
      (error) => {
        let errorMessage = "위치 정보를 가져오는데 실패했습니다.";
        if (error.code === 1) errorMessage = "위치 정보 접근이 거부되었습니다. 설정에서 권한을 허용해주세요.";
        else if (error.code === 2) errorMessage = "위치 정보를 사용할 수 없습니다.";
        else if (error.code === 3) errorMessage = "위치 정보 요청 시간이 초과되었습니다.";
        
        setState((prev) => ({
          ...prev,
          isLoading: false,
          error: errorMessage,
        }));
      },
{ enableHighAccuracy: false, timeout: 10000, maximumAge: 60000 }
    );
  }, []);

  return { ...state, getLocation };
}

// 카카오맵 역지오코딩 (좌표 -> 주소) 헬퍼 함수
const reverseGeocode = (lat: number, lng: number): Promise<string> => {
  return new Promise((resolve, reject) => {
    if (!window.kakao || !window.kakao.maps || !window.kakao.maps.services) {
      reject("Kakao map API is not loaded");
      return;
    }

    const geocoder = new window.kakao.maps.services.Geocoder();
    geocoder.coord2RegionCode(lng, lat, (result: any, status: any) => {
      if (status === window.kakao.maps.services.Status.OK) {
        // 행정동 단위 주소 추출
        const addressMatch = result.find((res: any) => res.region_type === 'H');
        resolve(addressMatch ? addressMatch.address_name : result[0].address_name);
      } else {
        reject("Failed to reverse geocode");
      }
    });
  });
};
