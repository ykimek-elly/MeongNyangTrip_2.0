package com.team.meongnyang.security.oauth2;

import com.team.meongnyang.user.entity.User;
import com.team.meongnyang.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * 소셜 로그인 사용자 정보 처리.
 * Google/Kakao에서 받은 사용자 정보로 DB에 신규 가입 또는 기존 유저 조회.
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @SuppressWarnings("unchecked")
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String providerId;
        String email;
        String nickname;
        String profileImage;

        if ("google".equals(registrationId)) {
            providerId   = (String) attributes.get("sub");
            email        = (String) attributes.get("email");
            nickname     = (String) attributes.get("name");
            profileImage = (String) attributes.get("picture");

        } else if ("kakao".equals(registrationId)) {
            providerId = String.valueOf(attributes.get("id"));

            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            Map<String, Object> profile = kakaoAccount != null
                    ? (Map<String, Object>) kakaoAccount.get("profile")
                    : null;

            email        = kakaoAccount != null ? (String) kakaoAccount.get("email")        : null;
            nickname     = profile      != null ? (String) profile.get("nickname")          : null;
            profileImage = profile      != null ? (String) profile.get("profile_image_url") : null;

        } else {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인 제공자: " + registrationId);
        }

        if (email == null || email.isBlank()) {
            email = providerId + "@" + registrationId + ".oauth";
        }
        if (nickname == null || nickname.isBlank()) {
            nickname = registrationId + "User";
        }

        String finalEmail        = email;
        String finalNickname     = resolveUniqueNickname(nickname);
        String finalProfileImage = profileImage;
        String provider          = registrationId.toUpperCase();

        boolean[] isNew = {false};
        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> userRepository.findByEmail(finalEmail)
                        .orElseGet(() -> {
                            isNew[0] = true;
                            return userRepository.save(User.builder()
                                    .email(finalEmail)
                                    .password("")
                                    .nickname(finalNickname)
                                    .profileImage(finalProfileImage)
                                    .provider(provider)
                                    .providerId(providerId)
                                    .build());
                        }));

        return new OAuth2UserPrincipal(user, attributes, isNew[0]);
    }

    private String resolveUniqueNickname(String base) {
        String trimmed = base.length() > 16 ? base.substring(0, 16) : base;
        if (!userRepository.existsByNickname(trimmed)) return trimmed;
        String candidate = trimmed + UUID.randomUUID().toString().substring(0, 4);
        return candidate.length() > 20 ? candidate.substring(0, 20) : candidate;
    }
}
