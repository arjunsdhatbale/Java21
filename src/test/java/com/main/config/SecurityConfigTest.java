package com.main.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.main.model.dto.LoginRequest;
import com.main.model.dto.SignUpRequest;
import com.main.model.entity.User;
import com.main.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByEmail("testuser@example.com")) {
            User user = User.builder()
                    .firstName("Test")
                    .lastName("User")
                    .email("testuser@example.com")
                    .password(passwordEncoder.encode("SecurePass123"))
                    .phone("9876543210")
                    .role(User.UserRole.USER)
                    .status(User.UserStatus.ACTIVE)
                    .provider(User.AuthProvider.LOCAL)
                    .build();
            userRepository.save(user);
        }
    }

    @Test
    void testSignUp_createsNewUserSuccessfully() throws Exception {
        String uniqueEmail = "newbie" + System.currentTimeMillis() + "@example.com";
        SignUpRequest request = SignUpRequest.builder()
                .firstName("Alice")
                .lastName("Wonder")
                .email(uniqueEmail)
                .password("Secret123")
                .phone("9991112220")
                .role("USER")
                .build();

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(uniqueEmail));
    }

    @Test
    void testLogin_withRegisteredUser_authenticatesSuccessfully() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("testuser@example.com")
                .password("SecurePass123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.authenticated").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void testLogin_withUnregisteredUser_isRejected() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("completely_unregistered_ghost@example.com")
                .password("anyPassword123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Account not found")));
    }

    @Test
    void testLogin_withWrongPassword_isRejected() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("testuser@example.com")
                .password("WrongPassword999")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid password")));
    }

    @Test
    void testLogout_succeeds() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
