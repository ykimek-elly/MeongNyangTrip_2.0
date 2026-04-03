package com.team.meongnyang.security.oauth2;

import com.team.meongnyang.security.JwtUtil;
import com.team.meongnyang.security.RefreshTokenService;
import com.team.meongnyang.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Value("${oauth2.redirect-uri:https://meongnyangtrip.duckdns.org/oauth2/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        String email        = user.getEmail() != null ? user.getEmail() : "";
        String profileImage = user.getProfileImage() != null ? user.getProfileImage() : "";

        String accessToken  = jwtUtil.generateToken(email, user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(email);
        refreshTokenService.save(email, refreshToken);

        String region       = user.getRegion() != null ? user.getRegion() : "";
        Integer activityRadius = user.getActivityRadius() != null ? user.getActivityRadius() : 15;

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", accessToken)
                .queryParam("refreshToken", refreshToken)
                .queryParam("userId", user.getUserId())
                .queryParam("nickname", URLEncoder.encode(user.getNickname(), StandardCharsets.UTF_8))
                .queryParam("email", URLEncoder.encode(email, StandardCharsets.UTF_8))
                .queryParam("profileImage", URLEncoder.encode(profileImage, StandardCharsets.UTF_8))
                .queryParam("isNewUser", principal.isNewUser())
                .queryParam("region", URLEncoder.encode(region, StandardCharsets.UTF_8))
                .queryParam("activityRadius", activityRadius)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
