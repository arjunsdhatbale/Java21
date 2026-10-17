package com.main.security;

import com.main.user.User;
import com.main.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles successful OAuth2 / OpenID Connect authentication.
 * Extracts profile attributes from the authenticated principal, verifies that the user
 * is registered in the database, generates an internal application JWT,
 * and redirects to the Angular frontend.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final com.main.notification.EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${app.oauth2.authorized-redirect-url:http://localhost:4200/oauth2/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        if (response.isCommitted()) {
            log.warn("OAuth2 response has already been committed. Aborting redirect.");
            return;
        }

        OAuth2Profile profile = resolveProfile(authentication);

        // Check if user initiated OAuth2 in SIGNUP mode (via session)
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        String oauth2Mode = session != null ? (String) session.getAttribute("oauth2_mode") : null;
        boolean isSignupMode = "signup".equalsIgnoreCase(oauth2Mode);
        if (session != null) {
            session.removeAttribute("oauth2_mode");
        }

        String emailToCheck = profile.email().trim().toLowerCase();
        Optional<User> dbUserOpt = userRepository.findByEmail(emailToCheck);

        User dbUser;

        if (isSignupMode) {
            // ══════════════════════════════════════════════════════════════════════
            // OAUTH2 SIGN-UP MODE: Create user in database if not already present
            // ══════════════════════════════════════════════════════════════════════
            if (dbUserOpt.isPresent()) {
                dbUser = dbUserOpt.get();
                log.info("OAuth2 Sign-Up: User with email [{}] is already registered. Proceeding to sign-in.", emailToCheck);
            } else {
                log.info("OAuth2 Sign-Up: Registering new user in database with email [{}] from Google account.", emailToCheck);
                User newUser = User.builder()
                        .firstName(profile.firstName().isBlank() ? "User" : profile.firstName())
                        .lastName(profile.lastName().isBlank() ? "Google" : profile.lastName())
                        .email(emailToCheck)
                        // A secure random BCrypt hash satisfies the non-null password constraint in MySQL
                        .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .phone("NOT_PROVIDED")
                        .role(User.UserRole.USER)
                        .status(User.UserStatus.ACTIVE)
                        .provider(User.AuthProvider.GOOGLE)
                        .providerId(profile.providerId().isBlank() ? null : profile.providerId())
                        .avatarUrl(profile.avatarUrl().isBlank() ? null : profile.avatarUrl())
                        .build();

                dbUser = userRepository.save(newUser);
                log.info("New OAuth2 user created in database successfully with ID: [{}]", dbUser.getId());

                // Send welcome email async
                try {
                    emailService.sendWelcomeEmail(
                            dbUser.getEmail(),
                            dbUser.getFirstName(),
                            dbUser.getLastName(),
                            dbUser.getPhone(),
                            dbUser.getRole().name()
                    );
                } catch (Exception ex) {
                    log.warn("Could not dispatch welcome email to {}: {}", dbUser.getEmail(), ex.getMessage());
                }
            }
        } else {
            // ══════════════════════════════════════════════════════════════════════
            // OAUTH2 SIGN-IN MODE: User MUST already exist in database!
            // ══════════════════════════════════════════════════════════════════════
            if (dbUserOpt.isEmpty()) {
                log.warn("OAuth2 Sign-In REJECTED: Email [{}] is NOT registered in the database.", emailToCheck);
                clearAuthenticationAttributes(request);

                String failureRedirectUrl = UriComponentsBuilder.fromUriString(redirectUri)
                        .queryParam("error", "user_not_registered")
                        .queryParam("email", URLEncoder.encode(emailToCheck, StandardCharsets.UTF_8))
                        .queryParam("message", URLEncoder.encode("Your Google account (" + emailToCheck + ") is not registered. Please sign up first.", StandardCharsets.UTF_8))
                        .build().toUriString();

                getRedirectStrategy().sendRedirect(request, response, failureRedirectUrl);
                return;
            }

            dbUser = dbUserOpt.get();
        }

        // Account status check
        if (dbUser.getStatus() != User.UserStatus.ACTIVE) {
            log.warn("OAuth2 Sign-In REJECTED: User [{}] is in status: {}", emailToCheck, dbUser.getStatus());
            clearAuthenticationAttributes(request);

            String failureRedirectUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("error", "account_disabled")
                    .queryParam("message", URLEncoder.encode("Your account is " + dbUser.getStatus() + ". Please contact administrator.", StandardCharsets.UTF_8))
                    .build().toUriString();

            getRedirectStrategy().sendRedirect(request, response, failureRedirectUrl);
            return;
        }

        String finalFirstName = (dbUser.getFirstName() != null && !dbUser.getFirstName().isBlank())
                ? dbUser.getFirstName()
                : profile.firstName();
        String finalLastName = (dbUser.getLastName() != null && !dbUser.getLastName().isBlank())
                ? dbUser.getLastName()
                : profile.lastName();

        OAuth2Profile finalProfile = new OAuth2Profile(
                profile.username(),
                profile.email(),
                finalFirstName,
                finalLastName,
                profile.avatarUrl(),
                profile.providerId()
        );

        String userRole = "ROLE_" + (dbUser.getRole() != null ? dbUser.getRole().name() : "USER");
        List<String> roles = List.of(userRole);

        String jwtToken = tokenProvider.generateToken(finalProfile.username(), roles, finalProfile.email());
        log.info("OAuth2 authentication successful for user: [{}] (role: {}, firstName: {}). Issuing JWT.",
                finalProfile.username(), userRole, finalProfile.firstName());

        String targetUrl = buildRedirectUrl(jwtToken, finalProfile, roles);

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /**
     * Resolves user profile information cleanly across OIDC, custom OAuth2, and generic principals.
     */
    private OAuth2Profile resolveProfile(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof OidcUser oidcUser) {
            return fromOidcUser(oidcUser);
        }

        if (principal instanceof OAuth2User oauth2User) {
            return fromOAuth2User(oauth2User);
        }

        String fallback = Optional.ofNullable(authentication.getName()).orElse("User");
        return new OAuth2Profile(fallback, fallback, "", "", "", "");
    }

    private OAuth2Profile fromOidcUser(OidcUser oidcUser) {
        Map<String, Object> claims = oidcUser.getClaims();

        String email = Optional.ofNullable(oidcUser.getEmail())
                .filter(s -> !s.isBlank())
                .orElseGet(() -> getString(claims, "email"));

        String firstName = Optional.ofNullable(oidcUser.getGivenName())
                .filter(s -> !s.isBlank())
                .orElseGet(() -> getString(claims, "given_name"));

        String lastName = Optional.ofNullable(oidcUser.getFamilyName())
                .filter(s -> !s.isBlank())
                .orElseGet(() -> getString(claims, "family_name"));

        String username = Optional.ofNullable(oidcUser.getFullName())
                .filter(s -> !s.isBlank())
                .orElseGet(() -> !email.isBlank() ? email : firstName);

        String avatarUrl = Optional.ofNullable(oidcUser.getPicture())
                .filter(s -> !s.isBlank())
                .orElseGet(() -> getString(claims, "picture"));

        String providerId = Optional.ofNullable(oidcUser.getSubject())
                .filter(s -> !s.isBlank())
                .orElseGet(() -> getString(claims, "sub"));

        return normalize(username, email, firstName, lastName, avatarUrl, providerId);
    }

    private OAuth2Profile fromOAuth2User(OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();

        String email = getString(attributes, "email");
        String firstName = getString(attributes, "given_name");
        String lastName = getString(attributes, "family_name");
        String username = getString(attributes, "name");
        String avatarUrl = getString(attributes, "picture");
        String providerId = getString(attributes, "sub");
        if (providerId.isBlank()) {
            providerId = getString(attributes, "id");
        }

        return normalize(username, email, firstName, lastName, avatarUrl, providerId);
    }

    /**
     * Guarantees fallback values so user-facing names are clean and never null or blank.
     */
    private OAuth2Profile normalize(String username, String email, String firstName, String lastName, String avatarUrl, String providerId) {
        String cleanEmail = Optional.ofNullable(email).map(String::trim).orElse("");
        String cleanFirst = Optional.ofNullable(firstName).map(String::trim).orElse("");
        String cleanLast = Optional.ofNullable(lastName).map(String::trim).orElse("");
        String cleanUser = Optional.ofNullable(username).map(String::trim).orElse("");
        String cleanAvatar = Optional.ofNullable(avatarUrl).map(String::trim).orElse("");
        String cleanProviderId = Optional.ofNullable(providerId).map(String::trim).orElse("");

        if (cleanFirst.isBlank()) {
            if (!cleanUser.isBlank() && !cleanUser.contains("@")) {
                cleanFirst = cleanUser.split("\\s+")[0];
            } else if (!cleanEmail.isBlank()) {
                cleanFirst = cleanEmail.split("@")[0];
            } else {
                cleanFirst = "User";
            }
        }

        if (cleanUser.isBlank()) {
            cleanUser = !cleanEmail.isBlank() ? cleanEmail : cleanFirst;
        }

        return new OAuth2Profile(cleanUser, cleanEmail, cleanFirst, cleanLast, cleanAvatar, cleanProviderId);
    }

    private String getString(Map<String, Object> attributes, String key) {
        if (attributes == null || !attributes.containsKey(key)) {
            return "";
        }
        Object value = attributes.get(key);
        return value != null ? value.toString().trim() : "";
    }

    private List<String> extractRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    private String buildRedirectUrl(String jwtToken, OAuth2Profile profile, List<String> roles) {
        return UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", jwtToken)
                .queryParam("username", profile.username())
                .queryParam("email", profile.email())
                .queryParam("firstName", profile.firstName())
                .queryParam("lastName", profile.lastName())
                .queryParam("roles", String.join(",", roles))
                .queryParam("provider", "google")
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
    }

    /**
     * Immutable representation of an authenticated OAuth2 user identity.
     */
    private record OAuth2Profile(String username, String email, String firstName, String lastName, String avatarUrl, String providerId) {}
}

