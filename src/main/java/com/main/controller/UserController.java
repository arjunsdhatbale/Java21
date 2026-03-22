package com.main.controller;

import com.main.model.dto.UserRequestDto;
import com.main.model.dto.UserResponseDto;
import com.main.service.UserService;
import com.main.service.UserServiceImpl;
import com.main.shared.pagination.annotation.CursorPaginated;
import com.main.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final UserServiceImpl userServiceImpl;
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponseDto>> createUser(@Valid @RequestBody UserRequestDto dto) {
        logger.info("Request received to create user.");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully", userService.createUser(dto)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserById(@PathVariable Long id) {
        logger.info("Request receive to get user by id : {}.",id);
        return ResponseEntity.ok(ApiResponse.success("User fetched successfully", userService.getUserById(id)));
    }

    @GetMapping
    @CursorPaginated(defaultSize = 2, sortField = "createdAt", sortDir = "DESC")
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> getAllUsers() throws Throwable {
        logger.info("Request received to get all users.");
        return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", userService.getAllUsers()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateUser(@PathVariable Long id,
                                                                   @Valid @RequestBody UserRequestDto dto) {
        logger.info("Request received to update user by id : {}.", id);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", userService.updateUser(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        logger.info("Request received to delete user by id : {}. ", id);
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> searchUsers(@RequestParam String keyword) {
        logger.info("Request received to seaarch user by keyword : {}.", keyword);
        return ResponseEntity.ok(ApiResponse.success("Search results", userServiceImpl.search(keyword)));
    }
}
