package com.main.controller;

import com.main.model.dto.JobStatusResponse;
import com.main.model.dto.RowError;
import com.main.model.dto.UploadResponse;
import com.main.service.UserBulkUploadService;
import com.main.service.UserService;
import com.main.service.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserBulkUploadControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserServiceImpl userServiceImpl;

    @Mock
    private UserBulkUploadService userBulkUploadService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

    @Test
    void testUploadUsers_returnsAccepted() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dummy content".getBytes()
        );

        UploadResponse response = UploadResponse.builder()
                .jobId("job-123")
                .message("Upload accepted. Processing has started.")
                .statusUrl("/api/v1/users/bulk-upload/status/job-123")
                .build();

        when(userBulkUploadService.uploadExcel(any())).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/users/upload").file(file))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").value("job-123"))
                .andExpect(jsonPath("$.data.statusUrl").value("/api/v1/users/bulk-upload/status/job-123"));
    }

    @Test
    void testGetUploadStatus_returnsStatus() throws Exception {
        JobStatusResponse response = JobStatusResponse.builder()
                .jobId("job-123")
                .status("COMPLETED")
                .totalRows(10)
                .processedRows(10)
                .successfulRows(8)
                .failedRows(2)
                .build();

        when(userBulkUploadService.getJobStatus(eq("job-123"))).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/bulk-upload/status/job-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").value("job-123"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.successfulRows").value(8))
                .andExpect(jsonPath("$.data.failedRows").value(2));
    }

    @Test
    void testGetFailedRows_returnsErrors() throws Exception {
        RowError error1 = RowError.builder()
                .rowNumber(2)
                .email("bad@email.com")
                .reason("Invalid phone number")
                .build();

        when(userBulkUploadService.getFailedRows(eq("job-123"))).thenReturn(List.of(error1));

        mockMvc.perform(get("/api/v1/users/bulk-upload/failed-rows/job-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].rowNumber").value(2))
                .andExpect(jsonPath("$.data[0].reason").value("Invalid phone number"));
    }
}