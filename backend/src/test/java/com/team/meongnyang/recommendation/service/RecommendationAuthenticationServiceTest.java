package com.team.meongnyang.recommendation.service;

import com.team.meongnyang.exception.BusinessException;
import com.team.meongnyang.exception.ErrorCode;
import com.team.meongnyang.security.oauth2.OAuth2UserPrincipal;
import com.team.meongnyang.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecommendationAuthenticationServiceTest {

    private final RecommendationAuthenticationService authenticationService =
            new RecommendationAuthenticationService();

    @Test
    @DisplayName("JWT 로그인 principal 이면 username 이메일을 반환한다")
    void getAuthenticatedUserEmail_withUserDetails() {
        UserDetails principal = org.springframework.security.core.userdetails.User.builder()
                .username("jwt-user@example.com")
                .password("encoded-password")
                .roles("USER")
                .build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        String email = authenticationService.getAuthenticatedUserEmail(authentication);

        assertThat(email).isEqualTo("jwt-user@example.com");
    }

    @Test
    @DisplayName("OAuth 로그인 principal 이면 User 엔티티 이메일을 반환한다")
    void getAuthenticatedUserEmail_withOAuth2Principal() {
        User user = User.builder()
                .userId(1L)
                .email("oauth-user@example.com")
                .nickname("oauthUser")
                .build();
        OAuth2UserPrincipal principal = new OAuth2UserPrincipal(user, Map.of("sub", "oauth-id"));
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        String email = authenticationService.getAuthenticatedUserEmail(authentication);

        assertThat(email).isEqualTo("oauth-user@example.com");
    }

    @Test
    @DisplayName("비로그인 요청이면 UNAUTHORIZED 예외를 던진다")
    void getAuthenticatedUserEmail_withoutAuthentication() {
        Authentication authentication = new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

        assertThatThrownBy(() -> authenticationService.getAuthenticatedUserEmail(authentication))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}
