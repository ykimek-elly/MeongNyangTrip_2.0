package com.team.meongnyang.ai;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.place.repository.PlaceRepository;
import com.team.meongnyang.user.entity.Dog;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.user.repository.DogRepository;
import com.team.meongnyang.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DummyDataInitializer implements CommandLineRunner {

  private final UserRepository userRepository;
  private final DogRepository dogRepository;
  private final PlaceRepository placeRepository;

  @Override
  public void run(String... args) {

    // 이미 들어있으면 중복 삽입 방지
    if (userRepository.count() > 0 || placeRepository.count() > 0) {
      return;
    }

    // =========================
    // USER
    // =========================
    User user1 = User.builder()
            .email("user1@test.com")
            .password("12345678")
            .nickname("멍집사1")
            .role(User.Role.USER)
            .status(User.Status.ACTIVE)
            .build();

    User user2 = User.builder()
            .email("user2@test.com")
            .password("12345678")
            .nickname("멍집사2")
            .role(User.Role.USER)
            .status(User.Status.ACTIVE)
            .build();

    User admin = User.builder()
            .email("admin@test.com")
            .password("admin1234")
            .nickname("관리자")
            .role(User.Role.ADMIN)
            .status(User.Status.ACTIVE)
            .build();

    userRepository.save(user1);
    userRepository.save(user2);
    userRepository.save(admin);

    // =========================
    // DOG
    // =========================
    Dog dog1 = Dog.builder()
            .user(user1)
            .dogName("콩이")
            .dogSize(Dog.DogSize.SMALL)
            .dogBreed("말티즈")
            .personality("사람을 좋아하고 활발함")
            .preferredPlace("공원")
            .build();

    Dog dog2 = Dog.builder()
            .user(user2)
            .dogName("보리")
            .dogSize(Dog.DogSize.LARGE)
            .dogBreed("골든리트리버")
            .personality("온순하고 산책을 좋아함")
            .preferredPlace("넓은 야외")
            .build();

    dogRepository.save(dog1);
    dogRepository.save(dog2);

    // =========================
    // PLACE
    // =========================
    Place place1 = Place.builder()
            .title("양평 반려견 공원")
            .description("잔디밭과 산책로가 넓어 반려견과 산책하기 좋은 공원")
            .address("경기도 양평군 양평읍 공원로 10")
            .latitude(37.4912)
            .longitude(127.4873)
            .category("PLACE")
            .rating(0.0)
            .reviewCount(0)
            .imageUrl("https://example.com/park.jpg")
            .phone("031-111-2222")
            .tags("[\"대형견가능\",\"주차가능\",\"야외\",\"산책로\"]")
            .build();

    Place place2 = Place.builder()
            .title("멍냥 브런치 카페")
            .description("반려견 동반이 가능한 실내 카페")
            .address("경기도 여주시 세종로 20")
            .latitude(37.2981)
            .longitude(127.6375)
            .category("DINING")
            .rating(0.0)
            .reviewCount(0)
            .imageUrl("https://example.com/cafe.jpg")
            .phone("031-333-4444")
            .tags("[\"소형견가능\",\"중형견가능\",\"실내가능\",\"주차가능\"]")
            .build();

    Place place3 = Place.builder()
            .title("댕댕 스테이 펜션")
            .description("반려견 전용 운동장이 있는 숙소")
            .address("강원도 홍천군 서면 여행길 33")
            .latitude(37.6911)
            .longitude(127.6932)
            .category("STAY")
            .rating(0.0)
            .reviewCount(0)
            .imageUrl("https://example.com/stay.jpg")
            .phone("033-555-6666")
            .tags("[\"대형견가능\",\"운동장\",\"숙박\",\"주차가능\"]")
            .build();

    placeRepository.save(place1);
    placeRepository.save(place2);
    placeRepository.save(place3);

  }
}
