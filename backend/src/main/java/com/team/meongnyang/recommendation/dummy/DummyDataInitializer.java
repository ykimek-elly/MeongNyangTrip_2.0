package com.team.meongnyang.recommendation.dummy;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.place.repository.PlaceRepository;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.user.repository.PetRepository;
import com.team.meongnyang.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DummyDataInitializer implements CommandLineRunner {

  private final UserRepository userRepository;
  private final PetRepository petRepository;
  private final PlaceRepository placeRepository;

  @Override
  public void run(String... args) {
    if (userRepository.findByEmail("testsingle@test.com").isEmpty()) {

      User singleUser = User.builder()
              .email("testsingle@test.com")
              .password("12345678")
              .nickname("singleTester")
              .role(User.Role.USER)
              .status(User.Status.ACTIVE)
              .build();

      userRepository.save(singleUser);
    }

    User singleUser = userRepository.findByEmail("testsingle@test.com").orElseThrow();

    if (petRepository.findAll().stream().noneMatch(p -> p.getUser().getUserId().equals(singleUser.getUserId()))) {

      Pet singlePet = Pet.builder()
              .user(singleUser)
              .petName("두부")
              .petType(Pet.PetType.values()[0])
              .petBreed("푸들")
              .petGender(Pet.PetGender.values()[0])
              .petSize(Pet.PetSize.SMALL)
              .petAge(4)
              .petActivity(Pet.PetActivity.NORMAL)
              .personality("조용하지만 산책을 좋아함")
              .preferredPlace("실내카페")
              .build();

      petRepository.save(singlePet);
    }

    if (true) {

      placeRepository.save(
              Place.builder()
                      .title("광교 테스트 공원")
                      .description("산책 테스트용 공원")
                      .address("경기도 수원시 영통구 광교 101")
                      .latitude(37.2862)
                      .longitude(127.0515)
                      .category("PLACE")
                      .rating(4.7)
                      .reviewCount(120)
                      .imageUrl("https://example.com/test1.jpg")
                      .phone("031-100-1001")
                      .tags("[\"산책로\",\"야외\",\"주차가능\"]")
                      .build()
      );

      placeRepository.save(
              Place.builder()
                      .title("광교 테스트 카페")
                      .description("반려견 실내 동반 카페")
                      .address("경기도 수원시 영통구 광교 102")
                      .latitude(37.2841)
                      .longitude(127.0487)
                      .category("DINING")
                      .rating(4.4)
                      .reviewCount(90)
                      .imageUrl("https://example.com/test2.jpg")
                      .phone("031-100-1002")
                      .tags("[\"실내가능\",\"소형견가능\"]")
                      .build()
      );

      placeRepository.save(
              Place.builder()
                      .title("행궁동 테스트 식당")
                      .description("테라스 좌석 가능")
                      .address("경기도 수원시 팔달구 행궁동 88")
                      .latitude(37.2801)
                      .longitude(127.0145)
                      .category("DINING")
                      .rating(4.2)
                      .reviewCount(77)
                      .imageUrl("https://example.com/test3.jpg")
                      .phone("031-100-1003")
                      .tags("[\"테라스\",\"주차가능\"]")
                      .build()
      );

      placeRepository.save(
              Place.builder()
                      .title("수원 테스트 스테이")
                      .description("숙박 테스트용")
                      .address("경기도 수원시 권선구 권선동 77")
                      .latitude(37.2575)
                      .longitude(127.0231)
                      .category("STAY")
                      .rating(4.5)
                      .reviewCount(55)
                      .imageUrl("https://example.com/test4.jpg")
                      .phone("031-100-1004")
                      .tags("[\"숙박\",\"중형견가능\"]")
                      .build()
      );

      placeRepository.save(
              Place.builder()
                      .title("인계동 테스트 플레이스")
                      .description("도심 산책 장소")
                      .address("경기도 수원시 팔달구 인계동 45")
                      .latitude(37.2657)
                      .longitude(127.0301)
                      .category("PLACE")
                      .rating(4.3)
                      .reviewCount(64)
                      .imageUrl("https://example.com/test5.jpg")
                      .phone("031-100-1005")
                      .tags("[\"도심\",\"산책가능\"]")
                      .build()
      );
    }
  }
}
