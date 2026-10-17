package com.main.user;

import com.main.notification.NotificationDto;
import com.main.notification.NotificationService;
import com.main.notification.NotificationType;
import com.main.notification.EmailService;
import com.main.notification.SmsService;
import com.main.shared.SearchService;
import com.main.shared.exception.BusinessException;
import com.main.shared.exception.ResourceNotFoundException;
import com.main.shared.pagination.helper.CursorQueryHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService, SearchService<UserResponseDto> {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CursorQueryHelper cursorQueryHelper;
    private final EmailService emailService;
    private final SmsService smsService;
    private final NotificationService notificationService;

    @Override
    public UserResponseDto createUser(UserRequestDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException("Email already exists: " + dto.getEmail());
        }
        User user = userMapper.toEntity(dto);
        user.setPassword(dto.getPassword());
        User savedUser = userRepository.save(user);

        // Async welcome email & SMS notification with complete user profile
        String roleStr = savedUser.getRole() != null ? savedUser.getRole().name() : "USER";
        emailService.sendWelcomeEmail(
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getPhone(),
                roleStr
        );
        if (savedUser.getPhone() != null && !savedUser.getPhone().trim().isEmpty()) {
            smsService.sendSms(savedUser.getPhone(), "Welcome to Java21 Project, " + savedUser.getFirstName() + "!");
        }

        // WebSocket notification
        notificationService.sendToUser(savedUser.getEmail(),
                NotificationDto.of(
                        savedUser.getEmail(),
                        "Welcome to Java21 Project",
                        "You have been added to my Java21 project! Your account has been registered successfully.",
                        NotificationType.WELCOME
                ));

        return userMapper.toDto(savedUser);
    }

    @Override
    public UserResponseDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toDto(user);
    }

    @Override
    public List<UserResponseDto> getAllUsers() throws Throwable {
        return cursorQueryHelper.findWithCursor(User.class, null)
                .stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Override
    public UserResponseDto updateUser(Long id, UserRequestDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhone(dto.getPhone());
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            user.setPassword(dto.getPassword());
        }
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setStatus(User.UserStatus.INACTIVE);
        userRepository.save(user);
    }

    @Override
    public List<UserResponseDto> search(String keyword) {
        return userRepository.searchUsers(keyword)
                .stream()
                .map(userMapper::toDto)
                .toList();
    }
}
