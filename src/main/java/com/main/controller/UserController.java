package com.main.controller;

import com.main.model.dto.JobStatusResponse;
import com.main.model.dto.RowError;
import com.main.model.dto.UploadResponse;
import com.main.model.dto.UserRequestDto;
import com.main.model.dto.UserResponseDto;
import com.main.service.UserBulkUploadService;
import com.main.service.UserService;
import com.main.service.UserServiceImpl;
import com.main.shared.pagination.annotation.CursorPaginated;
import com.main.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final UserServiceImpl userServiceImpl;
    private final UserBulkUploadService userBulkUploadService;
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
    @CursorPaginated(defaultSize = 50, sortField = "createdAt", sortDir = "DESC")
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
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadResponse>> uploadUsers(
            @RequestParam("file") MultipartFile file) {
        logger.info("Request received to upload excel file.");
        UploadResponse response = userBulkUploadService.uploadExcel(file);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)   // 202
                .body(ApiResponse.success("File accepted for processing", response));

    }

    @GetMapping(value = {"/bulk-upload/status/{jobId}", "/upload/status/{jobId}"})
    public ResponseEntity<ApiResponse<JobStatusResponse>> getUploadStatus(@PathVariable String jobId) {
        logger.info("Request received to get bulk upload status for jobId: {}.", jobId);
        JobStatusResponse response = userBulkUploadService.getJobStatus(jobId);
        return ResponseEntity.ok(ApiResponse.success("Job status fetched successfully", response));
    }

    @GetMapping(value = {"/bulk-upload/failed-rows/{jobId}", "/upload/failed-rows/{jobId}"})
    public ResponseEntity<ApiResponse<List<RowError>>> getFailedRows(@PathVariable String jobId) {
        logger.info("Request received to get failed rows for jobId: {}.", jobId);
        List<RowError> failedRows = userBulkUploadService.getFailedRows(jobId);
        return ResponseEntity.ok(ApiResponse.success("Failed rows fetched successfully", failedRows));
    }
}
