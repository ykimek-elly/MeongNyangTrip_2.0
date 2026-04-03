package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.exception.BusinessException;
import com.team.meongnyang.exception.ErrorCode;
import com.team.meongnyang.recommendation.log.RecommendationLogContext;
import com.team.meongnyang.security.oauth2.OAuth2UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RecommendationAuthenticationService {

  /**
   * 현재 인증 객체에서 추천 대상 사용자의 이메일을 추출한다.
   * JWT 로그인 사용자는 UserDetails#getUsername()을,
   * OAuth 로그인 사용자는 OAuth2UserPrincipal 내부 User 엔티티의 이메일을 사용한다.
   *
   * @param authentication 스프링 시큐리티 인증 객체
   * @return 현재 로그인 사용자의 이메일
   * @throws BusinessException 인증 정보가 없거나 지원하지 않는 principal 타입인 경우
   */
  public String getAuthenticatedUserEmail(Authentication authentication) {
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw unauthorized();
    }

    Object principal = authentication.getPrincipal();

    if (principal instanceof OAuth2UserPrincipal oAuth2UserPrincipal) {
      String email = oAuth2UserPrincipal.getUser().getEmail();
      log.debug("[RecommendationAuthenticationService] OAuth 로그인 사용자 이메일 추출 완료 email={}", email);
      return email;
    }

    if (principal instanceof UserDetails userDetails) {
      String email = userDetails.getUsername();
      log.debug("[RecommendationAuthenticationService] 시큐리티 로그인 사용자 이메일 추출 완료 email={}", email);
      return email;
    }

    log.warn("[추천 파이프라인] 인증 principal 타입 미지원 type={}, batchExecutionId={}",
            principal == null ? "null" : principal.getClass().getName(),
            RecommendationLogContext.batchExecutionId());
    throw unauthorized();
  }

  /**
   * 인증되지 않은 요청에 대한 공통 예외를 생성한다.
   *
   * @return 401 Unauthorized 비즈니스 예외
   */
  private BusinessException unauthorized() {
    log.warn("[추천 파이프라인] 인증되지 않은 요청 batchExecutionId={}",
            RecommendationLogContext.batchExecutionId());
    return new BusinessException(ErrorCode.UNAUTHORIZED);
  }
}
