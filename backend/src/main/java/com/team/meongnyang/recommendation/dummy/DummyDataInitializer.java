package com.team.meongnyang.recommendation.dummy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DummyDataInitializer implements CommandLineRunner {

  private static final String PLACE_SAMPLE_CLASSPATH = "recommendation/dummy/places_entity_sample.json";
  private static final Path PLACE_SAMPLE_SOURCE_PATH = Path.of(
          "src", "main", "java", "com", "team", "meongnyang", "recommendation", "dummy", "places_entity_sample.json"
  );

  private final UserRepository userRepository;
  private final PetRepository petRepository;
  private final PlaceRepository placeRepository;
  private final WishlistRepository wishlistRepository;
  private final ReviewRepository reviewRepository;
  private final CheckInRepository checkInRepository;
  private final AiResponseLogRepository aiResponseLogRepository;
  private final ObjectMapper objectMapper;

  private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  @Transactional
  public void run(String... args) {
    log.warn("[더미 로더] 기존 데이터를 비우고 테스트용 더미 데이터를 적재합니다.");

    clearAllData();

    List<User> users = seedUsers();
    seedPets(users);
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

  private List<User> seedUsers() {
    User seoulUser = userRepository.save(
            User.builder()
                    .email("scheduler-test@meongnyang.com")
                    .password("12345678")
                    .nickname("더미테스터")
                    .phoneNumber("01097465454")
                    .notificationEnabled(true)
                    .role(User.Role.USER)
                    .status(User.Status.ACTIVE)
                    .latitude(37.5665)
                    .longitude(126.9780)
                    .build()
    );

    User suwonUser = userRepository.save(
            User.builder()
                    .email("scheduler-test-suwon@meongnyang.com")
                    .password("12345678")
                    .nickname("더미테스터수원")
                    .phoneNumber("01097465454")
                    .notificationEnabled(true)
                    .role(User.Role.USER)
                    .status(User.Status.ACTIVE)
                    .latitude(37.2636)
                    .longitude(127.0286)
                    .build()
    );

    return List.of(seoulUser, suwonUser);
  }

  private void seedPets(List<User> users) {
    petRepository.save(
            Pet.builder()
                    .user(users.get(0))
                    .petName("몽실이")
                    .petType(Pet.PetType.강아지)
                    .petBreed("말티즈")
                    .petGender(Pet.PetGender.남아)
                    .petSize(Pet.PetSize.SMALL)
                    .petAge(4)
                    .petWeight(new BigDecimal("4.20"))
                    .petActivity(Pet.PetActivity.NORMAL)
                    .personality("실내와 실외 산책을 모두 무난하게 좋아하는 아이입니다.")
                    .preferredPlace("공원")
                    .isRepresentative(true)
                    .build()
    );

    petRepository.save(
            Pet.builder()
                    .user(users.get(1))
                    .petName("보리")
                    .petType(Pet.PetType.강아지)
                    .petBreed("푸들")
                    .petGender(Pet.PetGender.여아)
                    .petSize(Pet.PetSize.SMALL)
                    .petAge(3)
                    .petWeight(new BigDecimal("3.80"))
                    .petActivity(Pet.PetActivity.NORMAL)
                    .personality("도심 산책과 카페 방문을 좋아하는 활발한 아이입니다.")
                    .preferredPlace("카페")
                    .isRepresentative(true)
                    .build()
    );
  }

  private void seedPlaces() {
    List<PlaceSeed> placeSeeds = new ArrayList<>(loadPlaceSeeds());
    placeSeeds.addAll(buildSuwonTestPlaceSeeds());

    for (PlaceSeed placeSeed : placeSeeds) {
      savePlace(placeSeed);
    }
  }

  private List<PlaceSeed> loadPlaceSeeds() {
    try (InputStream inputStream = openPlaceSeedStream()) {
      List<Map<String, Object>> rawPlaces = objectMapper.readValue(inputStream, new TypeReference<>() {});
      List<PlaceSeed> placeSeeds = new ArrayList<>();

      for (Map<String, Object> rawPlace : rawPlaces) {
        if (!rawPlace.containsKey("contentId")) {
          continue;
        }
        placeSeeds.add(objectMapper.convertValue(rawPlace, PlaceSeed.class));
      }

      return placeSeeds;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load place dummy data from places_entity_sample.json", e);
    }
  }

  private InputStream openPlaceSeedStream() throws IOException {
    ClassPathResource resource = new ClassPathResource(PLACE_SAMPLE_CLASSPATH);
    if (resource.exists()) {
      return resource.getInputStream();
    }

    if (Files.exists(PLACE_SAMPLE_SOURCE_PATH)) {
      return Files.newInputStream(PLACE_SAMPLE_SOURCE_PATH);
    }

    throw new IOException("places_entity_sample.json not found in classpath or source path");
  }

  private List<PlaceSeed> buildSuwonTestPlaceSeeds() {
    return List.of(
            new PlaceSeed(null, "dummy-suwon-001", null, 37.2851, 127.0151, 0, true, "ACTIVE", null,
                    "수원 산책공원 1", "반려동물과 걷기 좋은 넓은 공원입니다.", "경기도 수원시 팔달구 테스트로 11", null,
                    "PLACE", 4.6, 20, "https://example.com/dummy/place-001.jpg", "031-100-0001", null,
                    "N", null, "야외 산책 가능, 목줄 착용 필수", "공원 산책로와 잔디 공간이 넓어 반려동물과 가볍게 걷기 좋은 장소입니다.",
                    "공원,산책,야외,반려동물동반", 3.8, 25, "산책,넓어,추천", null, null,
                    "목줄 착용 필수, 배변 처리 필수", null, null,
                    LocalDateTime.of(2026, 3, 1, 10, 0), LocalDateTime.of(2026, 3, 21, 9, 0)),
            new PlaceSeed(null, "dummy-suwon-002", null, 37.2861, 127.0161, 0, true, "ACTIVE", null,
                    "수원 산책공원 2", "아침 산책에 적합한 조용한 코스입니다.", "경기도 수원시 팔달구 테스트로 12", null,
                    "PLACE", 4.4, 12, "https://example.com/dummy/place-002.jpg", "031-100-0002", null,
                    "N", null, "야외 산책 가능", "주거지와 가까워 짧은 산책 코스로 활용하기 좋은 조용한 공원입니다.",
                    "공원,산책,야외,조용함", 3.5, 18, "조용,산책,힐링", null, null,
                    "목줄 착용 필수", null, null,
                    LocalDateTime.of(2026, 3, 1, 10, 5), LocalDateTime.of(2026, 3, 21, 9, 5)),
            new PlaceSeed(null, "dummy-suwon-003", null, 37.2811, 127.0411, 0, true, "ACTIVE", null,
                    "반려 카페 1", "실내 좌석이 넉넉한 반려동물 동반 카페입니다.", "경기도 수원시 영통구 테스트로 21", null,
                    "DINING", 4.7, 31, "https://example.com/dummy/place-003.jpg", "031-100-0003", "https://example.com/dummy/cafe-001",
                    "Y", null, "실내 동반 가능", "테이블 간격이 넓고 반려견과 함께 쉬기 좋은 카페형 공간입니다.",
                    "실내,카페,반려동물동반", 4.1, 41, "친절,아늑,추천", null, "물그릇 제공",
                    "리드줄 착용 권장", "10:00-21:00", null,
                    LocalDateTime.of(2026, 3, 1, 10, 10), LocalDateTime.of(2026, 3, 21, 9, 10)),
            new PlaceSeed(null, "dummy-suwon-004", null, 37.2821, 127.0421, 0, true, "ACTIVE", null,
                    "반려 카페 2", "가벼운 휴식에 적합한 카페입니다.", "경기도 수원시 영통구 테스트로 22", null,
                    "DINING", 4.3, 14, "https://example.com/dummy/place-004.jpg", "031-100-0004", null,
                    "Y", null, "실내 동반 가능", "짧게 머물며 커피와 디저트를 즐기기 좋은 반려동물 동반 카페입니다.",
                    "실내,카페,디저트", 3.7, 16, "디저트,아늑", null, null,
                    "리드줄 착용 권장", "11:00-20:00", null,
                    LocalDateTime.of(2026, 3, 1, 10, 15), LocalDateTime.of(2026, 3, 21, 9, 15)),
            new PlaceSeed(null, "dummy-suwon-005", null, 37.2571, 127.0281, 0, true, "ACTIVE", null,
                    "실내 플레이존", "비 오는 날 방문하기 좋은 실내 공간입니다.", "경기도 수원시 권선구 테스트로 31", null,
                    "PLACE", 4.5, 22, "https://example.com/dummy/place-005.jpg", "031-100-0005", null,
                    "Y", null, "실내 동반 가능", "우천 시에도 반려동물과 함께 시간을 보내기 좋은 실내 놀이 공간입니다.",
                    "실내,놀이,반려동물동반", 4.0, 27, "실내,편해,추천", null, "놀이 매트",
                    "공격성 있는 반려동물 입장 제한", "10:00-19:00", "안전 수칙 준수",
                    LocalDateTime.of(2026, 3, 1, 10, 20), LocalDateTime.of(2026, 3, 21, 9, 20)),
            new PlaceSeed(null, "dummy-suwon-006", null, 37.2871, 127.0611, 0, true, "ACTIVE", null,
                    "호수 산책길", "호수 주변을 따라 걷기 좋은 장소입니다.", "경기도 수원시 영통구 테스트로 32", null,
                    "PLACE", 4.8, 37, "https://example.com/dummy/place-006.jpg", "031-100-0006", null,
                    "N", null, "야외 산책 가능", "호수 주변으로 산책로가 이어져 있어 반려동물과 길게 걷기 좋습니다.",
                    "호수,산책,야외", 4.2, 48, "뷰,산책,탁트인", null, null,
                    "목줄 착용 필수", null, null,
                    LocalDateTime.of(2026, 3, 1, 10, 25), LocalDateTime.of(2026, 3, 21, 9, 25)),
            new PlaceSeed(null, "dummy-suwon-007", null, 37.2691, 126.9511, 0, true, "ACTIVE", null,
                    "반려 스테이 1", "조용하게 쉬기 좋은 숙소입니다.", "경기도 수원시 권선구 테스트로 41", null,
                    "STAY", 4.6, 19, "https://example.com/dummy/place-007.jpg", "031-100-0007", "https://example.com/dummy/stay-001",
                    "Y", "소형견 2마리", "객실 내 동반 가능", "반려동물과 함께 편하게 쉬기 좋은 소규모 스테이입니다.",
                    "숙소,실내,반려동물동반", 4.1, 23, "청결,편안,재방문", null, "펫 침대,식기",
                    "추가 요금 발생 가능", "체크인 15:00 / 체크아웃 11:00", "사전 예약 필수",
                    LocalDateTime.of(2026, 3, 1, 10, 30), LocalDateTime.of(2026, 3, 21, 9, 30)),
            new PlaceSeed(null, "dummy-suwon-008", null, 37.3031, 127.0121, 0, true, "ACTIVE", null,
                    "반려 스테이 2", "가볍게 머물기 좋은 반려 숙소입니다.", "경기도 수원시 장안구 테스트로 42", null,
                    "STAY", 4.2, 11, "https://example.com/dummy/place-008.jpg", "031-100-0008", null,
                    "Y", "소형견 1마리", "객실 내 동반 가능", "단기 숙박 수요에 맞는 반려동물 동반 숙소입니다.",
                    "숙소,실내,단기숙박", 3.6, 13, "가성비,편안", null, "펫 식기",
                    "사전 문의 권장", "체크인 16:00 / 체크아웃 11:00", null,
                    LocalDateTime.of(2026, 3, 1, 10, 35), LocalDateTime.of(2026, 3, 21, 9, 35)),
            new PlaceSeed(null, "dummy-suwon-009", null, 37.3011, 127.0441, 0, true, "ACTIVE", null,
                    "실내 문화공간", "날씨가 좋지 않을 때 방문하기 좋은 조용한 공간입니다.", "경기도 수원시 장안구 테스트로 51", null,
                    "PLACE", 4.1, 9, "https://example.com/dummy/place-009.jpg", "031-100-0009", null,
                    "Y", null, "일부 공간 동반 가능", "전시와 휴식을 함께 즐길 수 있는 실내형 문화 공간입니다.",
                    "실내,문화,조용함", 3.4, 10, "조용,실내", null, null,
                    "공간별 동반 가능 여부 상이", "09:00-18:00", "전시 구역별 안내 준수",
                    LocalDateTime.of(2026, 3, 1, 10, 40), LocalDateTime.of(2026, 3, 21, 9, 40)),
            new PlaceSeed(null, "dummy-suwon-010", null, 37.2841, 127.0191, 0, true, "ACTIVE", null,
                    "가벼운 산책로", "짧게 산책하기 좋은 동네 코스입니다.", "경기도 수원시 팔달구 테스트로 52", null,
                    "PLACE", 4.0, 8, "https://example.com/dummy/place-010.jpg", "031-100-0010", null,
                    "N", null, "야외 산책 가능", "부담 없이 짧게 돌기 좋은 도심형 산책 코스입니다.",
                    "산책,야외,가벼운코스", 3.2, 9, "접근성,산책", null, null,
                    "목줄 착용 필수", null, null,
                    LocalDateTime.of(2026, 3, 1, 10, 45), LocalDateTime.of(2026, 3, 21, 9, 45))
    );
  }

  private Place savePlace(PlaceSeed placeSeed) {
    Point geom = geometryFactory.createPoint(new Coordinate(placeSeed.longitude(), placeSeed.latitude()));
    geom.setSRID(4326);

    Place savedPlace = placeRepository.save(
            Place.builder()
                    .contentId(placeSeed.contentId())
                    .kakaoId(placeSeed.kakaoId())
                    .version(placeSeed.version())
                    .isVerified(placeSeed.isVerified() != null ? placeSeed.isVerified() : true)
                    .status(placeSeed.status() != null ? PlaceStatus.valueOf(placeSeed.status()) : PlaceStatus.ACTIVE)
                    .pendingReason(placeSeed.pendingReason())
                    .title(placeSeed.title())
                    .description(placeSeed.description())
                    .address(placeSeed.address())
                    .addr2(placeSeed.addr2())
                    .latitude(placeSeed.latitude())
                    .longitude(placeSeed.longitude())
                    .geom(geom)
                    .category(placeSeed.category())
                    .rating(placeSeed.rating() != null ? placeSeed.rating() : 0.0)
                    .reviewCount(placeSeed.reviewCount() != null ? placeSeed.reviewCount() : 0)
                    .imageUrl(placeSeed.imageUrl())
                    .phone(placeSeed.phone())
                    .tags(placeSeed.tags())
                    .overview(placeSeed.overview())
                    .chkPetInside(placeSeed.chkPetInside())
                    .accomCountPet(placeSeed.accomCountPet())
                    .petTurnAdroose(placeSeed.petTurnAdroose())
                    .homepage(placeSeed.homepage())
                    .aiRating(placeSeed.aiRating())
                    .blogCount(placeSeed.blogCount())
                    .blogPositiveTags(placeSeed.blogPositiveTags())
                    .blogNegativeTags(placeSeed.blogNegativeTags())
                    .petFacility(placeSeed.petFacility())
                    .petPolicy(placeSeed.petPolicy())
                    .operatingHours(placeSeed.operatingHours())
                    .operationPolicy(placeSeed.operationPolicy())
                    .build()
    );

    updatePlaceMetadata(savedPlace.getId(), placeSeed.longitude(), placeSeed.latitude(), placeSeed.createdAt(), placeSeed.updatedAt());
    return savedPlace;
  }

  private void updatePlaceMetadata(Long placeId, Double longitude, Double latitude, LocalDateTime createdAt, LocalDateTime updatedAt) {
    entityManager.createNativeQuery("""
            UPDATE places
            SET geom = ST_SetSRID(ST_MakePoint(:lng, :lat), 4326),
                created_at = COALESCE(:createdAt, created_at),
                updated_at = COALESCE(:updatedAt, updated_at)
            WHERE id = :placeId
            """)
            .setParameter("lng", longitude)
            .setParameter("lat", latitude)
            .setParameter("createdAt", createdAt)
            .setParameter("updatedAt", updatedAt)
            .setParameter("placeId", placeId)
            .executeUpdate();
  }

  private record PlaceSeed(
          Long id,
          String contentId,
          String kakaoId,
          Double latitude,
          Double longitude,
          Integer version,
          Boolean isVerified,
          String status,
          String pendingReason,
          String title,
          String description,
          String address,
          String addr2,
          String category,
          Double rating,
          Integer reviewCount,
          String imageUrl,
          String phone,
          String homepage,
          String chkPetInside,
          String accomCountPet,
          String petTurnAdroose,
          String overview,
          String tags,
          Double aiRating,
          Integer blogCount,
          String blogPositiveTags,
          String blogNegativeTags,
          String petFacility,
          String petPolicy,
          String operatingHours,
          String operationPolicy,
          LocalDateTime createdAt,
          LocalDateTime updatedAt
  ) {
  }
}
