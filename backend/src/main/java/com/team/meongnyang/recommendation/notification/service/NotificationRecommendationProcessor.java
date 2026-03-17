package com.team.meongnyang.recommendation.notification.service;

import com.team.meongnyang.recommendation.notification.dto.NotificationProcessResult;
import com.team.meongnyang.recommendation.notification.dto.RecommendationNotificationResult;
import com.team.meongnyang.recommendation.service.RecommendationPipelineService;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.user.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationRecommendationProcessor {

  private final PetRepository petRepository;
  private final RecommendationPipelineService recommendtationService;
  private final NotificationMessageBuilder messageBuilder;
  private static final double SUWON_LAT = 37.27;
  private static final double SUWON_LNG = 127.01;

  /**
   * 알림 대상 사용자 받기
   * 대표 반려견 조회
   * 추천 파이프라인 호출
   * topPlace, weatherType, aiMessage 같은 알림 재료 확보
   * 메시지 빌더 호출
   * 최종 발송 서비스 호출
   * @param user
   * @return
   */

  public NotificationProcessResult process(User user) {
    Long userId = user.getUserId();
    String email = user.getEmail();
    // Double latitude = user.getLatitude();<- 위치 필드명 맞게 수정
    // Double longitude = user.getLongitude();<- 위치 필드명 맞게 수정
    Double latitude = SUWON_LAT;
    Double longitude = SUWON_LNG;

    log.info("[알림 추천 처리 시작] userId={}, email={}", userId, email);

    Pet pet = petRepository.findFirstByUser(user).orElse(null);

    if (pet == null) {
      log.warn("[알림 추천 처리 실패] 유저의 반려동물 정보가 존재하지 않습니다. userId={}", userId);
      return NotificationProcessResult.fail(userId, email ,"반려견 정보 없음");
    }


    RecommendationNotificationResult result = recommendtationService.recommendForNotification(user, pet);
    // todo : 알림 변환
    // todo : NotificationMessageBuilder 로 문구 조립

    log.info("[알림 추천 Processor] 추천 생성 완료 userId={}, petId={}, messageLength={}",
            user.getUserId(),
            pet.getPetId(),
            result.getMessage() == null ? 0 : result.getMessage().length());


    return NotificationProcessResult.success(userId, email, result.getMessage());
  }
}
