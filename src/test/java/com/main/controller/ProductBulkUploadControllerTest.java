package com.main.controller;

import com.main.model.dto.JobStatusResponse;
import com.main.model.dto.RowError;
import com.main.model.dto.UploadResponse;
import com.main.service.ProductBulkUploadService;
import com.main.service.ProductService;
import com.main.service.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
class ProductBulkUploadControllerTest {

    @Mock
    private ProductService productService;

    @Mock
    private ProductServiceImpl productServiceImpl;

    @Mock
    private ProductBulkUploadService productBulkUploadService;

    @InjectMocks
    private ProductController productController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController).build();
    }

    @Test
    void testUploadProducts_returnsAccepted() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "products.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dummy content".getBytes()
        );

        UploadResponse response = UploadResponse.builder()
                .jobId("job-prod-123")
                .message("Upload accepted. Processing has started.")
                .statusUrl("/api/v1/products/bulk-upload/status/job-prod-123")
                .build();

        when(productBulkUploadService.uploadExcel(any())).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/products/upload").file(file))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").value("job-prod-123"))
                .andExpect(jsonPath("$.data.statusUrl").value("/api/v1/products/bulk-upload/status/job-prod-123"));
    }

    @Test
    void testGetUploadStatus_returnsStatus() throws Exception {
        JobStatusResponse response = JobStatusResponse.builder()
                .jobId("job-prod-123")
                .status("COMPLETED")
                .totalRows(15)
                .processedRows(15)
                .successfulRows(12)
                .failedRows(3)
                .build();

        when(productBulkUploadService.getJobStatus(eq("job-prod-123"))).thenReturn(response);

        mockMvc.perform(get("/api/v1/products/bulk-upload/status/job-prod-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").value("job-prod-123"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.successfulRows").value(12))
                .andExpect(jsonPath("$.data.failedRows").value(3));
    }

    @Test
    void testGetFailedRows_returnsErrors() throws Exception {
        RowError error1 = RowError.builder()
                .rowNumber(3)
                .email("Wireless Mouse")
                .identifier("Wireless Mouse")
                .reason("Price must be greater than 0")
                .build();

        when(productBulkUploadService.getFailedRows(eq("job-prod-123"))).thenReturn(List.of(error1));

        mockMvc.perform(get("/api/v1/products/bulk-upload/failed-rows/job-prod-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].rowNumber").value(3))
                .andExpect(jsonPath("$.data[0].identifier").value("Wireless Mouse"))
                .andExpect(jsonPath("$.data[0].reason").value("Price must be greater than 0"));
    }
}
