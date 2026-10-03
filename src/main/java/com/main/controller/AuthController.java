package com.main.controller;

import com.main.model.dto.LoginRequest;
import com.main.model.dto.LoginResponse;
import com.main.model.dto.SignUpRequest;
import com.main.model.dto.UserResponseDto;
import com.main.model.entity.User;
import com.main.repo.UserRepository;
import com.main.security.jwt.JwtTokenProvider;
import com.main.service.EmailService;
import com.main.shared.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final EmailService emailService;

    /**
     * REST endpoint for user registration / signup.
     * Prevents unregistered sign-in by allowing users to create their account in the database first.
     */
    @PostMapping({"/register", "/signup"})
    public ResponseEntity<ApiResponse<UserResponseDto>> register(@Valid @RequestBody SignUpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("REST signup attempt for email: [{}]", email);

        if (userRepository.existsByEmail(email)) {
            log.warn("Signup rejected: Email [{}] is already registered.", email);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("An account with email '" + email + "' already exists. Please sign in.", "Email already exists"));
        }

        User.UserRole role = User.UserRole.USER;
        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                role = User.UserRole.valueOf(request.getRole().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                role = User.UserRole.USER;
            }
        }

        String safePhone = request.getPhone() != null && !request.getPhone().isBlank()
                ? request.getPhone().trim()
                : "NOT_PROVIDED";

        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword().trim()))
                .phone(safePhone)
                .role(role)
                .status(User.UserStatus.ACTIVE)
                .provider(User.AuthProvider.LOCAL)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered successfully in database with ID: [{}] and email: [{}]", savedUser.getId(), savedUser.getEmail());

        // Dispatch async welcome email with registration confirmation
        try {
            emailService.sendWelcomeEmail(
                    savedUser.getEmail(),
                    savedUser.getFirstName(),
                    savedUser.getLastName(),
                    savedUser.getPhone(),
                    savedUser.getRole().name()
            );
        } catch (Exception ex) {
            log.warn("Could not dispatch welcome email to {}: {}", savedUser.getEmail(), ex.getMessage());
        }

        UserResponseDto responseData = UserResponseDto.builder()
                .id(savedUser.getId())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .email(savedUser.getEmail())
                .phone(savedUser.getPhone())
                .role(savedUser.getRole().name())
                .status(savedUser.getStatus().name())
                .createdAt(savedUser.getCreatedAt())
                .updatedAt(savedUser.getUpdatedAt())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account registered successfully! You can now sign in with your credentials.", responseData));
    }

    /**
     * REST endpoint for user login.
     * Strictly verifies that the user is registered in the database before granting access.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {

        String query = loginRequest.getUsername().trim();
        log.info("REST login attempt for user: [{}]", query);

        // Find user by email or by "user@example.com" if query was "user"
        Optional<User> userOpt = userRepository.findByEmail(query.toLowerCase());
        if (userOpt.isEmpty() && "user".equalsIgnoreCase(query)) {
            userOpt = userRepository.findByEmail("user@example.com");
        }

        if (userOpt.isEmpty()) {
            log.warn("Login rejected: User [{}] is NOT registered in the database.", query);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Account not found for '" + query + "'. Please sign up first.", "User not registered"));
        }

        User user = userOpt.get();

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            log.warn("Login rejected: User [{}] is in status: {}", user.getEmail(), user.getStatus());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Your account is " + user.getStatus() + ". Please contact administrator.", "Account disabled"));
        }

        if (!passwordMatches(loginRequest.getPassword().trim(), user.getPassword())) {
            log.warn("Login rejected: Invalid password for user [{}]", user.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid password. Please check your credentials.", "Bad credentials"));
        }

        // Automatic password upgrade to BCrypt if stored as legacy plaintext
        if (user.getPassword() != null && !user.getPassword().startsWith("$2")) {
            user.setPassword(passwordEncoder.encode(loginRequest.getPassword().trim()));
            userRepository.save(user);
        }

        String role = "ROLE_" + (user.getRole() != null ? user.getRole().name() : "USER");
        List<String> roles = Collections.singletonList(role);
        String jwtToken = tokenProvider.generateToken(user.getEmail(), roles, user.getEmail());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of(new SimpleGrantedAuthority(role)));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        LoginResponse responseData = LoginResponse.builder()
                .username(user.getFirstName() + " " + user.getLastName())
                .roles(roles)
                .token(jwtToken)
                .authenticated(true)
                .build();

        log.info("Login successful for user: [{}] ({}) with role: {}", user.getEmail(), responseData.getUsername(), role);
        return ResponseEntity.ok(ApiResponse.success("Login successful", responseData));
    }

    /**
     * REST endpoint for user logout.
     * Clears SecurityContextHolder and invalidates the active HTTP session.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "Anonymous";

        log.info("REST logout requested for user: [{}]", username);

        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", username));
    }

    /**
     * REST endpoint to check the currently authenticated user session.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<LoginResponse>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User is not authenticated", "Anonymous session"));
        }

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        LoginResponse response = LoginResponse.builder()
                .username(authentication.getName())
                .roles(roles)
                .authenticated(true)
                .build();

        return ResponseEntity.ok(ApiResponse.success("Authenticated user profile retrieved", response));
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (storedPassword == null || rawPassword == null) return false;
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return storedPassword.equals(rawPassword);
    }
}
