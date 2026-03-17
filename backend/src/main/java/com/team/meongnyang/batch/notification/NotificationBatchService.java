package com.team.meongnyang.batch.notification;

import com.team.meongnyang.recommendation.service.RecommendationOrchestratorService;
import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 일일 추천 알림 배치 서비스.
 *
 * 스케줄러에서 호출되며,
 * 발송 대상 사용자 조회, 추천 생성, 알림 발송, 결과 기록을 순차적으로 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationBatchService {

  private final UserRepository userRepository;
  private final RecommendationOrchestratorService recommendationOrchestratorService;

  /**
   * 일일 추천 알림 배치 흐름을 실행한다.
   */
  public void process() {
    log.info("[배치] 알림 발송 대상 사용자 조회 시작");

    // 1. 알림 수신 동의 사용자 조회
    List<Long> targetUserIds  = findNotificationTargetUserIds();
    // 2. 사용자별 추천 생성
    // 3. 알림 발송
    // 4. 발송 성공/실패 로그 저장

    log.info("[배치] 알림 발송 대상 사용자 조회 완료");
  }

  /**
  * 알림 수신 동의 사용자를 조회한다.
  *
  * 현재는 mock 데이터로 사용자 ID 목록을 반환하며,
  * 이후 알림 수신 여부, 전화번호 존재 여부 등을 조건으로 DB 조회로 확장한다.*
  * @return 알림 발송 대상 사용자 ID 목록
   *
   * todo : phone, 알림 수신
  */
  private List<Long> findNotificationTargetUserIds() {
    return List.of(1L, 2L, 3L);
  }


}
