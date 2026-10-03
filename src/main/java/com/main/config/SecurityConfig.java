package com.main.config;

import com.main.security.jwt.JwtAuthenticationFilter;
import com.main.security.oauth2.CustomOAuth2UserService;
import com.main.security.oauth2.OAuth2AuthenticationSuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;
    private final com.main.security.oauth2.OAuth2AuthenticationFailureHandler oauth2FailureHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${spring.security.user.name:user}")
    private String defaultUsername;

    @Value("${spring.security.user.password:password}")
    private String defaultPassword;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails user = User.builder()
                .username(defaultUsername)
                .password(passwordEncoder.encode(defaultPassword))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Pure REST API - Disable CSRF
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            // Pure REST API - Disable HTML Form Login and Default Logout Filter
            .formLogin(form -> form.disable())
            .logout(logout -> logout.disable())
            // Return JSON 401 instead of redirecting to HTML login page
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("""
                        {"success":false,"message":"Unauthorized - authentication required","data":null}
                        """);
                })
            )
            .authorizeHttpRequests(auth -> auth
                // Allow REST Auth endpoints (login, logout, me)
                .requestMatchers("/api/v1/auth/**").permitAll()
                // Allow OAuth2 authorization endpoints and redirects
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                // Allow OpenAPI Swagger Documentation & Actuator
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html", "/actuator/**").permitAll()
                // Allow WebSocket endpoints & interactive tester
                .requestMatchers("/ws/**", "/ws-notifications/**", "/websocket-test.html").permitAll()
                // Keep business REST APIs accessible for the Angular frontend & external clients
                .requestMatchers("/api/**").permitAll()
                // Any other request requires authentication
                .anyRequest().authenticated()
            )
            // Configure Google OAuth2 Social Login
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                .successHandler(oauth2SuccessHandler)
                .failureHandler(oauth2FailureHandler)
            )
            .httpBasic(Customizer.withDefaults());

        // Add stateless JWT filter before UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
