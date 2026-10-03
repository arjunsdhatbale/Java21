package com.main.security;

import com.main.user.User;
import com.main.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String clientRegistrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("Processing OAuth2 login from provider: [{}]", clientRegistrationId);

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String givenName = oAuth2User.getAttribute("given_name");
        String familyName = oAuth2User.getAttribute("family_name");
        String picture = oAuth2User.getAttribute("picture");
        String providerId = oAuth2User.getAttribute("sub");

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Email not provided by OAuth2 identity provider");
        }

        String safeFirst = givenName != null && !givenName.isBlank()
                ? givenName.trim()
                : (name != null && !name.isBlank() ? name.trim() : "Google");

        String safeLast = familyName != null && !familyName.isBlank()
                ? familyName.trim()
                : "User";

        Optional<User> existingUserOpt = userRepository.findByEmail(email);

        if (existingUserOpt.isEmpty()) {
            log.warn("OAuth2 login rejected: Google email [{}] is not registered in the system.", email);
            throw new OAuth2AuthenticationException(
                    new org.springframework.security.oauth2.core.OAuth2Error("user_not_registered"),
                    "Your Google account (" + email + ") is not registered. Please sign up first."
            );
        }

        User user = existingUserOpt.get();

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            log.warn("OAuth2 login rejected: User [{}] is in status: {}", email, user.getStatus());
            throw new OAuth2AuthenticationException(
                    new org.springframework.security.oauth2.core.OAuth2Error("account_disabled"),
                    "Your account is " + user.getStatus() + ". Please contact administrator."
            );
        }

        log.info("Existing registered user authenticated via Google OAuth2: [{}]", email);
        user.setProvider(User.AuthProvider.GOOGLE);
        user.setProviderId(providerId);
        if (picture != null) {
            user.setAvatarUrl(picture);
        }
        user = userRepository.save(user);

        String authority = "ROLE_" + (user.getRole() != null ? user.getRole().name() : "USER");
        return new CustomOAuth2User(oAuth2User, user.getId(), user.getEmail(), authority);
    }
}
