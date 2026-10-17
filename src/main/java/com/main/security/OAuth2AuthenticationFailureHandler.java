package com.main.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.oauth2.authorized-redirect-url:http://localhost:4200/oauth2/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        log.warn("OAuth2 Authentication failed: {}", exception.getMessage());

        // Determine specific error code
        String errorCode = "oauth2_error";
        if (exception instanceof OAuth2AuthenticationException oauthEx) {
            OAuth2Error oauthError = oauthEx.getError();
            if (oauthError != null) {
                errorCode = oauthError.getErrorCode(); // e.g. "user_not_registered", "account_disabled"
            }
        }

        String safeMessage = exception.getMessage() != null
                ? URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8)
                : URLEncoder.encode("OAuth2 authentication failed.", StandardCharsets.UTF_8);

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("error", errorCode)
                .queryParam("message", safeMessage)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
