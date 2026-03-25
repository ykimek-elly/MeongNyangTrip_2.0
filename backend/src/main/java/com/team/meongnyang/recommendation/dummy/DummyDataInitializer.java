package com.team.meongnyang.recommendation.dummy;

import com.team.meongnyang.checkin.repository.CheckInRepository;
import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.place.entity.PlaceStatus;
import com.team.meongnyang.place.repository.PlaceRepository;
import com.team.meongnyang.recommendation.log.repository.AiResponseLogRepository;
import com.team.meongnyang.review.repository.ReviewRepository;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.user.repository.PetRepository;
import com.team.meongnyang.user.repository.UserRepository;
import com.team.meongnyang.wishlist.repository.WishlistRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DummyDataInitializer implements CommandLineRunner {

  private final UserRepository userRepository;
  private final PetRepository petRepository;
  private final PlaceRepository placeRepository;
  private final WishlistRepository wishlistRepository;
  private final ReviewRepository reviewRepository;
  private final CheckInRepository checkInRepository;
  private final AiResponseLogRepository aiResponseLogRepository;

  private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  @Transactional
  public void run(String... args) {
    log.warn("[더미 로더] 기존 데이터를 비우고 스케줄러 테스트용 최소 더미 데이터를 적재합니다.");

    clearAllData();

    User testUser = seedUser();
    seedPet(testUser);
    seedPlaces();

    log.info("[더미 로더] 적재 완료 users={}, pets={}, places={}",
            userRepository.count(),
            petRepository.count(),
            placeRepository.count());
  }

  private void clearAllData() {
    wishlistRepository.deleteAllInBatch();
    reviewRepository.deleteAllInBatch();
    checkInRepository.deleteAllInBatch();
    petRepository.deleteAllInBatch();
    aiResponseLogRepository.deleteAllInBatch();
    userRepository.deleteAllInBatch();
    placeRepository.deleteAllInBatch();
  }

  private User seedUser() {
    return userRepository.save(
            User.builder()
                    .email("scheduler-test@meongnyang.com")
                    .password("12345678")
                    .nickname("알림테스트")
                    .phoneNumber("01097465454")
                    .notificationEnabled(true)
                    .role(User.Role.USER)
                    .status(User.Status.ACTIVE)
                    .latitude(37.27)
                    .longitude(127.0)
                    .build()
    );
  }

  private Pet seedPet(User user) {
    return petRepository.save(
            Pet.builder()
                    .user(user)
                    .petName("몽실")
                    .petType(Pet.PetType.강아지)
                    .petBreed("말티즈")
                    .petGender(Pet.PetGender.여아)
                    .petSize(Pet.PetSize.SMALL)
                    .petAge(4)
                    .petWeight(new BigDecimal("4.20"))
                    .petActivity(Pet.PetActivity.NORMAL)
                    .personality("실내와 야외 산책을 모두 무난하게 좋아하는 편입니다.")
                    .preferredPlace("공원")
                    .isRepresentative(true)
                    .build()
    );
  }

  private void seedPlaces() {
    List<PlaceSeed> places = List.of(
            new PlaceSeed("dummy-test-place-001", "수원 산책공원 1", "반려동물과 걷기 좋은 넓은 공원입니다.", "경기도 수원시 팔달구 테스트로 11", 37.2851, 127.0151, "PLACE", "https://example.com/dummy/place-001.jpg", "031-100-0001", "[\"공원\",\"산책\",\"야외\",\"반려동물동반\"]"),
            new PlaceSeed("dummy-test-place-002", "수원 산책공원 2", "아침 산책에 적합한 조용한 코스입니다.", "경기도 수원시 팔달구 테스트로 12", 37.2861, 127.0161, "PLACE", "https://example.com/dummy/place-002.jpg", "031-100-0002", "[\"공원\",\"산책\",\"야외\"]"),
            new PlaceSeed("dummy-test-place-003", "반려 카페 1", "실내 좌석이 넉넉한 반려동물 동반 카페입니다.", "경기도 수원시 영통구 테스트로 21", 37.2811, 127.0411, "DINING", "https://example.com/dummy/place-003.jpg", "031-100-0003", "[\"실내\",\"카페\",\"반려동물동반\"]"),
            new PlaceSeed("dummy-test-place-004", "반려 카페 2", "가벼운 휴식에 적합한 카페입니다.", "경기도 수원시 영통구 테스트로 22", 37.2821, 127.0421, "DINING", "https://example.com/dummy/place-004.jpg", "031-100-0004", "[\"실내\",\"카페\"]"),
            new PlaceSeed("dummy-test-place-005", "실내 플레이존", "비 오는 날 방문하기 좋은 실내 공간입니다.", "경기도 수원시 권선구 테스트로 31", 37.2571, 127.0281, "PLACE", "https://example.com/dummy/place-005.jpg", "031-100-0005", "[\"실내\",\"놀이\",\"반려동물동반\"]"),
            new PlaceSeed("dummy-test-place-006", "호수 산책길", "호수 주변을 따라 걷기 좋은 장소입니다.", "경기도 수원시 영통구 테스트로 32", 37.2871, 127.0611, "PLACE", "https://example.com/dummy/place-006.jpg", "031-100-0006", "[\"호수\",\"산책\",\"야외\"]"),
            new PlaceSeed("dummy-test-place-007", "반려 스테이 1", "조용하게 쉬기 좋은 숙소입니다.", "경기도 수원시 권선구 테스트로 41", 37.2691, 126.9511, "STAY", "https://example.com/dummy/place-007.jpg", "031-100-0007", "[\"숙소\",\"실내\",\"반려동물동반\"]"),
            new PlaceSeed("dummy-test-place-008", "반려 스테이 2", "가볍게 머물기 좋은 반려 숙소입니다.", "경기도 수원시 장안구 테스트로 42", 37.3031, 127.0121, "STAY", "https://example.com/dummy/place-008.jpg", "031-100-0008", "[\"숙소\",\"실내\"]"),
            new PlaceSeed("dummy-test-place-009", "실내 문화공간", "날씨가 좋지 않을 때 방문하기 좋은 조용한 공간입니다.", "경기도 수원시 장안구 테스트로 51", 37.3011, 127.0441, "PLACE", "https://example.com/dummy/place-009.jpg", "031-100-0009", "[\"실내\",\"문화\",\"조용함\"]"),
            new PlaceSeed("dummy-test-place-010", "가벼운 산책로", "짧게 산책하기 좋은 동네 코스입니다.", "경기도 수원시 팔달구 테스트로 52", 37.2841, 127.0191, "PLACE", "https://example.com/dummy/place-010.jpg", "031-100-0010", "[\"산책\",\"야외\",\"가벼운코스\"]")
    );

    for (PlaceSeed placeSeed : places) {
      savePlace(placeSeed);
    }
  }

  private Place savePlace(PlaceSeed placeSeed) {
    Point geom = geometryFactory.createPoint(new Coordinate(placeSeed.longitude(), placeSeed.latitude()));

    Place savedPlace = placeRepository.save(
            Place.builder()
                    .contentId(placeSeed.contentId())
                    .title(placeSeed.title())
                    .description(placeSeed.description())
                    .address(placeSeed.address())
                    .latitude(placeSeed.latitude())
                    .longitude(placeSeed.longitude())
                    .geom(geom)
                    .category(placeSeed.category())
                    .isVerified(true)
                    .rating(4.6)
                    .reviewCount(20)
                    .imageUrl(placeSeed.imageUrl())
                    .phone(placeSeed.phone())
                    .tags(placeSeed.tags())
                    .status(PlaceStatus.ACTIVE)
                    .overview(placeSeed.description())
                    .chkPetInside(placeSeed.tags().contains("실내") ? "Y" : "N")
                    .build()
    );

    updateGeom(savedPlace.getId(), placeSeed.longitude(), placeSeed.latitude());
    return savedPlace;
  }

  private void updateGeom(Long placeId, double longitude, double latitude) {
    entityManager.createNativeQuery("""
            UPDATE places
            SET geom = ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)
            WHERE id = :placeId
            """)
            .setParameter("lng", longitude)
            .setParameter("lat", latitude)
            .setParameter("placeId", placeId)
            .executeUpdate();
  }

  private record PlaceSeed(
          String contentId,
          String title,
          String description,
          String address,
          double latitude,
          double longitude,
          String category,
          String imageUrl,
          String phone,
          String tags
  ) {
  }
}
