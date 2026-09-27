package com.main.service;

import com.main.mapper.UserMapper;
import com.main.model.dto.NotificationDto;
import com.main.model.dto.UserRequestDto;
import com.main.model.dto.UserResponseDto;
import com.main.model.entity.User;
import com.main.repo.UserRepository;
import com.main.shared.pagination.helper.CursorQueryHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CursorQueryHelper cursorQueryHelper;

    @Mock
    private EmailService emailService;

    @Mock
    private SmsService smsService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void testCreateUser_dispatchesWelcomeEmailWithFullProfileDetails() {
        UserRequestDto requestDto = UserRequestDto.builder()
                .firstName("Arjun")
                .lastName("Dhatbale")
                .email("arjun@example.com")
                .password("Secret@123")
                .phone("9876543210")
                .role("ADMIN")
                .build();

        User userEntity = User.builder()
                .id(1L)
                .firstName("Arjun")
                .lastName("Dhatbale")
                .email("arjun@example.com")
                .password("Secret@123")
                .phone("9876543210")
                .role(User.UserRole.ADMIN)
                .status(User.UserStatus.ACTIVE)
                .build();

        UserResponseDto responseDto = UserResponseDto.builder()
                .id(1L)
                .firstName("Arjun")
                .lastName("Dhatbale")
                .email("arjun@example.com")
                .phone("9876543210")
                .role("ADMIN")
                .build();

        when(userRepository.existsByEmail("arjun@example.com")).thenReturn(false);
        when(userMapper.toEntity(requestDto)).thenReturn(userEntity);
        when(userRepository.save(any(User.class))).thenReturn(userEntity);
        when(userMapper.toDto(userEntity)).thenReturn(responseDto);

        UserResponseDto result = userService.createUser(requestDto);

        assertNotNull(result);
        assertEquals("arjun@example.com", result.getEmail());

        // Verify welcome email dispatch with full user details
        verify(emailService, times(1)).sendWelcomeEmail(
                eq("arjun@example.com"),
                eq("Arjun"),
                eq("Dhatbale"),
                eq("9876543210"),
                eq("ADMIN")
        );

        // Verify SMS dispatch
        verify(smsService, times(1)).sendSms(
                eq("9876543210"),
                contains("Welcome to Java21 Project")
        );

        // Verify WebSocket notification
        verify(notificationService, times(1)).sendToUser(
                eq("arjun@example.com"),
                any(NotificationDto.class)
        );
    }
}
