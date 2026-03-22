package com.main.service;



import com.main.mapper.UserMapper;
import com.main.model.dto.UserRequestDto;
import com.main.model.dto.UserResponseDto;
import com.main.model.entity.User;
import com.main.repo.UserRepository;
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



    @Override
    public UserResponseDto createUser(UserRequestDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException("Email already exists: " + dto.getEmail());
        }
        User user = userMapper.toEntity(dto);
        user.setPassword(dto.getPassword());
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    public UserResponseDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toDto(user);
    }

    @Override
    public List<UserResponseDto> getAllUsers() throws Throwable {
        return  cursorQueryHelper.findWithCursor(User.class, null)
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