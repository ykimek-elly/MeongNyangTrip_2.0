package com.team.meongnyang.recommendation.dummy;

import com.team.meongnyang.checkin.entity.CheckIn;
import com.team.meongnyang.checkin.repository.CheckInRepository;
import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.place.entity.PlaceStatus;
import com.team.meongnyang.place.repository.PlaceRepository;
import com.team.meongnyang.recommendation.log.entity.AiResponseLog;
import com.team.meongnyang.recommendation.log.repository.AiResponseLogRepository;
import com.team.meongnyang.review.entity.Review;
import com.team.meongnyang.review.repository.ReviewRepository;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.user.repository.PetRepository;
import com.team.meongnyang.user.repository.UserRepository;
import com.team.meongnyang.wishlist.entity.Wishlist;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    log.warn("[더미 로더] 기존 데이터를 모두 삭제하고 새 더미 데이터를 적재합니다.");

    clearAllData();

    Map<String, User> users = seedUsers();
    Map<String, Pet> pets = seedPets(users);
    Map<String, Place> places = seedPlaces();

    seedWishlists(users, places);
    seedReviews(users, places);
    seedCheckIns(users);
    seedAiLogs(users, pets, places);

    log.info("[더미 로더] 적재 완료 users={}, pets={}, places={}, wishlists={}, reviews={}, checkIns={}, aiLogs={}",
            userRepository.count(),
            petRepository.count(),
            placeRepository.count(),
            wishlistRepository.count(),
            reviewRepository.count(),
            checkInRepository.count(),
            aiResponseLogRepository.count());
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

  private Map<String, User> seedUsers() {
    Map<String, User> users = new LinkedHashMap<>();

    users.put("mina", saveUser("mina@meongnyang.com", "미나", "01090010001"));
    users.put("jisu", saveUser("jisu@meongnyang.com", "지수", "01090010002"));
    users.put("taeho", saveUser("taeho@meongnyang.com", "태호", "01090010003"));
    users.put("yuri", saveUser("yuri@meongnyang.com", "유리", "01090010004"));
    users.put("sean", saveUser("sean@meongnyang.com", "시언", "01090010005"));

    return users;
  }

  private Map<String, Pet> seedPets(Map<String, User> users) {
    Map<String, Pet> pets = new LinkedHashMap<>();

    pets.put("mina-pet", savePet(
            users.get("mina"),
            "호두",
            Pet.PetType.강아지,
            "비숑프리제",
            Pet.PetGender.남아,
            Pet.PetSize.SMALL,
            4,
            "5.10",
            Pet.PetActivity.NORMAL,
            "차분하고 낯가림이 조금 있지만 실내에서는 금방 편해져요",
            "실내카페"
    ));

    pets.put("jisu-pet", savePet(
            users.get("jisu"),
            "마루",
            Pet.PetType.강아지,
            "보더콜리",
            Pet.PetGender.남아,
            Pet.PetSize.MEDIUM,
            3,
            "14.20",
            Pet.PetActivity.HIGH,
            "활발하고 바깥 냄새 맡는 걸 정말 좋아해요",
            "산책로"
    ));

    pets.put("taeho-pet", savePet(
            users.get("taeho"),
            "구름",
            Pet.PetType.고양이,
            "코리안숏헤어",
            Pet.PetGender.여아,
            Pet.PetSize.SMALL,
            5,
            "4.30",
            Pet.PetActivity.LOW,
            "조용하고 예민한 편이라 복잡한 공간은 금방 지쳐요",
            "전시"
    ));

    pets.put("yuri-pet", savePet(
            users.get("yuri"),
            "라떼",
            Pet.PetType.강아지,
            "말티푸",
            Pet.PetGender.여아,
            Pet.PetSize.SMALL,
            7,
            "3.90",
            Pet.PetActivity.LOW,
            "사교적이지만 쉬는 시간을 충분히 가져야 안정돼요",
            "실내"
    ));

    pets.put("sean-pet", savePet(
            users.get("sean"),
            "단추",
            Pet.PetType.강아지,
            "웰시코기",
            Pet.PetGender.남아,
            Pet.PetSize.MEDIUM,
            6,
            "12.60",
            Pet.PetActivity.HIGH,
            "명랑하고 에너지가 많아서 걷는 코스를 특히 좋아해요",
            "공원"
    ));

    return pets;
  }

  private Map<String, Place> seedPlaces() {
    Map<String, Place> places = new LinkedHashMap<>();

    places.put("hwaseong-trail", savePlace(
            "dummy-v2-place-001",
            "수원 화성 성곽 산책길",
            "성곽 주변을 천천히 걷기 좋은 반려 산책 코스예요.",
            "경기도 수원시 팔달구 장안동 334-3",
            37.2872,
            127.0194,
            "PLACE",
            "https://example.com/dummy/hwaseong-trail.jpg",
            "031-201-0001",
            "[\"산책로\",\"공원\",\"야외\",\"반려동물동반\"]"
    ));

    places.put("haenggung-brunch", savePlace(
            "dummy-v2-place-002",
            "행궁동 멍냥 브런치 라운지",
            "실내 좌석이 넓고 반려동물과 함께 쉬기 좋은 브런치 카페예요.",
            "경기도 수원시 팔달구 신풍동 112-8",
            37.2825,
            127.0152,
            "DINING",
            "https://example.com/dummy/haenggung-brunch.jpg",
            "031-201-0002",
            "[\"실내\",\"카페\",\"브런치\",\"반려동물동반\"]"
    ));

    places.put("museum-gallery", savePlace(
            "dummy-v2-place-003",
            "수원 반려생활 전시관",
            "실내 관람 동선이 편하고 조용한 분위기의 반려 문화 전시 공간이에요.",
            "경기도 수원시 영통구 이의동 1285",
            37.3008,
            127.0456,
            "PLACE",
            "https://example.com/dummy/museum-gallery.jpg",
            "031-201-0003",
            "[\"전시\",\"실내\",\"관람\",\"조용함\"]"
    ));

    places.put("lake-park", savePlace(
            "dummy-v2-place-004",
            "광교 호수 반려 산책공원",
            "호수 주변 산책 동선이 길고 시야가 탁 트여 활동량 높은 아이에게 잘 맞아요.",
            "경기도 수원시 영통구 하동 1024",
            37.2864,
            127.0607,
            "PLACE",
            "https://example.com/dummy/lake-park.jpg",
            "031-201-0004",
            "[\"공원\",\"산책\",\"야외\",\"수변\"]"
    ));

    places.put("quiet-stay", savePlace(
            "dummy-v2-place-005",
            "수원 포레스트 펫 스테이",
            "객실 간 간격이 여유롭고 실내 휴식이 편한 반려동물 동반 숙소예요.",
            "경기도 수원시 권선구 금곡동 671",
            37.2698,
            126.9505,
            "STAY",
            "https://example.com/dummy/quiet-stay.jpg",
            "031-201-0005",
            "[\"숙소\",\"실내\",\"휴식\",\"반려동물동반\"]"
    ));

    places.put("gallery-cafe", savePlace(
            "dummy-v2-place-006",
            "북수원 갤러리 카페 온유",
            "작은 전시와 카페 이용을 함께 할 수 있는 실내형 공간이에요.",
            "경기도 수원시 장안구 조원동 742-5",
            37.3041,
            127.0104,
            "DINING",
            "https://example.com/dummy/gallery-cafe.jpg",
            "031-201-0006",
            "[\"실내\",\"전시\",\"카페\",\"관람\"]"
    ));

    places.put("indoor-play", savePlace(
            "dummy-v2-place-007",
            "권선 펫 플레이룸",
            "비 오는 날에도 실내에서 가볍게 움직일 수 있는 반려 놀이 공간이에요.",
            "경기도 수원시 권선구 권선동 1041-7",
            37.2574,
            127.0308,
            "PLACE",
            "https://example.com/dummy/indoor-play.jpg",
            "031-201-0007",
            "[\"실내\",\"놀이\",\"반려동물동반\"]"
    ));

    return places;
  }

  private void seedWishlists(Map<String, User> users, Map<String, Place> places) {
    wishlistRepository.saveAll(List.of(
            Wishlist.builder().user(users.get("mina")).place(places.get("haenggung-brunch")).build(),
            Wishlist.builder().user(users.get("jisu")).place(places.get("lake-park")).build(),
            Wishlist.builder().user(users.get("taeho")).place(places.get("museum-gallery")).build(),
            Wishlist.builder().user(users.get("yuri")).place(places.get("gallery-cafe")).build(),
            Wishlist.builder().user(users.get("sean")).place(places.get("hwaseong-trail")).build()
    ));
  }

  private void seedReviews(Map<String, User> users, Map<String, Place> places) {
    reviewRepository.saveAll(List.of(
            Review.builder().user(users.get("mina")).place(places.get("haenggung-brunch")).content("좌석 간격이 넓어서 아이가 금방 편해졌어요. 실내 선호 아이에게 잘 맞아요.").rating(4.8).imageUrl(null).build(),
            Review.builder().user(users.get("jisu")).place(places.get("lake-park")).content("동선이 길어서 에너지 많은 아이가 충분히 걷기 좋아요.").rating(4.9).imageUrl(null).build(),
            Review.builder().user(users.get("taeho")).place(places.get("museum-gallery")).content("실내 관람 동선이 조용해서 예민한 아이도 비교적 안정적이었어요.").rating(4.7).imageUrl(null).build(),
            Review.builder().user(users.get("yuri")).place(places.get("gallery-cafe")).content("잠깐 쉬어가기 좋고 공간 분위기가 차분해요.").rating(4.6).imageUrl(null).build(),
            Review.builder().user(users.get("sean")).place(places.get("hwaseong-trail")).content("산책 길이 적당히 길고 풍경이 좋아서 재방문 의사 있어요.").rating(4.8).imageUrl(null).build()
    ));
  }

  private void seedCheckIns(Map<String, User> users) {
    checkInRepository.saveAll(List.of(
            CheckIn.builder().user(users.get("mina")).placeName("행궁동 멍냥 브런치 라운지").latitude(37.2825).longitude(127.0152).photoUrl("https://example.com/dummy/checkin-1.jpg").badgeName("첫 체크인").build(),
            CheckIn.builder().user(users.get("jisu")).placeName("광교 호수 반려 산책공원").latitude(37.2864).longitude(127.0607).photoUrl("https://example.com/dummy/checkin-2.jpg").badgeName("산책왕").build(),
            CheckIn.builder().user(users.get("taeho")).placeName("수원 반려생활 전시관").latitude(37.3008).longitude(127.0456).photoUrl("https://example.com/dummy/checkin-3.jpg").badgeName("문화탐방").build()
    ));
  }

  private void seedAiLogs(Map<String, User> users, Map<String, Pet> pets, Map<String, Place> places) {
    aiResponseLogRepository.saveAll(List.of(
            AiResponseLog.builder()
                    .userId(users.get("mina").getUserId())
                    .dogId(pets.get("mina-pet").getPetId())
                    .modelName("dummy-loader")
                    .prompt("실내 선호와 차분한 성향을 고려한 더미 추천 로그")
                    .recommendedPlaces(places.get("haenggung-brunch").getTitle())
                    .ragContext("dummy")
                    .responseText("실내 선호를 반영해 오래 머물기 편한 장소를 우선 추천했습니다.")
                    .fallbackUsed(false)
                    .cacheHit(false)
                    .latencyMs(120L)
                    .build(),
            AiResponseLog.builder()
                    .userId(users.get("jisu").getUserId())
                    .dogId(pets.get("jisu-pet").getPetId())
                    .modelName("dummy-loader")
                    .prompt("산책 선호와 높은 활동량을 고려한 더미 추천 로그")
                    .recommendedPlaces(places.get("lake-park").getTitle())
                    .ragContext("dummy")
                    .responseText("활동량이 높아 산책 동선이 긴 장소를 우선 추천했습니다.")
                    .fallbackUsed(false)
                    .cacheHit(false)
                    .latencyMs(135L)
                    .build()
    ));
  }

  private User saveUser(String email, String nickname, String phoneNumber) {
    return userRepository.save(
            User.builder()
                    .email(email)
                    .password("12345678")
                    .nickname(nickname)
                    .phoneNumber(phoneNumber)
                    .notificationEnabled(true)
                    .role(User.Role.USER)
                    .status(User.Status.ACTIVE)
                    .build()
    );
  }

  private Pet savePet(
          User user,
          String petName,
          Pet.PetType petType,
          String petBreed,
          Pet.PetGender petGender,
          Pet.PetSize petSize,
          int petAge,
          String petWeight,
          Pet.PetActivity petActivity,
          String personality,
          String preferredPlace
  ) {
    return petRepository.save(
            Pet.builder()
                    .user(user)
                    .petName(petName)
                    .petType(petType)
                    .petBreed(petBreed)
                    .petGender(petGender)
                    .petSize(petSize)
                    .petAge(petAge)
                    .petWeight(new BigDecimal(petWeight))
                    .petActivity(petActivity)
                    .personality(personality)
                    .preferredPlace(preferredPlace)
                    .isRepresentative(true)
                    .build()
    );
  }

  private Place savePlace(
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
    Point geom = geometryFactory.createPoint(new Coordinate(longitude, latitude));

    Place savedPlace = placeRepository.save(
            Place.builder()
                    .contentId(contentId)
                    .title(title)
                    .description(description)
                    .address(address)
                    .latitude(latitude)
                    .longitude(longitude)
                    .geom(geom)
                    .category(category)
                    .isVerified(true)
                    .rating(4.6)
                    .reviewCount(40)
                    .imageUrl(imageUrl)
                    .phone(phone)
                    .tags(tags)
                    .status(PlaceStatus.ACTIVE)
                    .overview(description + " 더미 로더 v2에서 생성한 테스트 장소입니다.")
                    .chkPetInside(tags.contains("실내") ? "Y" : "N")
                    .build()
    );

    updateGeom(savedPlace.getId(), longitude, latitude);
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
}
