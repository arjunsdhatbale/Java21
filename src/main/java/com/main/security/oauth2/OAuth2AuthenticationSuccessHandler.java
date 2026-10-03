package com.main.security.oauth2;

import com.main.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;

    @Value("${app.oauth2.authorized-redirect-url:http://localhost:4200/oauth2/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        if (response.isCommitted()) {
            log.warn("Response has already been committed. Unable to redirect.");
            return;
        }

        String username;
        String email = "";
        List<String> roles;

        if (authentication.getPrincipal() instanceof CustomOAuth2User customUser) {
            username = customUser.getName() != null ? customUser.getName() : customUser.getEmail();
            email = customUser.getEmail();
            roles = customUser.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
        } else {
            username = authentication.getName();
            roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
        }

        String token = tokenProvider.generateToken(username, roles, email);
        log.info("OAuth2 login successful for user: [{}]. Issuing JWT token and redirecting to Angular frontend.", username);

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", token)
                .queryParam("username", URLEncoder.encode(username, StandardCharsets.UTF_8))
                .queryParam("email", URLEncoder.encode(email, StandardCharsets.UTF_8))
                .queryParam("roles", String.join(",", roles))
                .queryParam("provider", "google")
                .build().toUriString();

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
