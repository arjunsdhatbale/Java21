package com.main.user;

import java.util.List;

public interface UserService {
    UserResponseDto createUser(UserRequestDto dto);
    UserResponseDto getUserById(Long id);
    List<UserResponseDto> getAllUsers() throws Throwable;
    UserResponseDto updateUser(Long id, UserRequestDto dto);
    void deleteUser(Long id);
    List<UserResponseDto> search(String keyword);
}
