package com.main.service;


import com.main.model.dto.UserRequestDto;
import com.main.model.dto.UserResponseDto;

import java.util.List;

public interface UserService {
    UserResponseDto createUser(UserRequestDto dto);
    UserResponseDto getUserById(Long id);
    List<UserResponseDto> getAllUsers();
    UserResponseDto updateUser(Long id, UserRequestDto dto);
    void deleteUser(Long id);

}
