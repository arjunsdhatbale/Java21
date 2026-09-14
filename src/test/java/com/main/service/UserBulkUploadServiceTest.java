package com.main.service;

import com.main.model.dto.*;
import com.main.model.entity.User;
import com.main.repo.UserBulkRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserBulkUploadServiceTest {

    private JobTracker jobTracker;
    private UserBulkRepository userBulkRepository;
    private Validator validator;
    private TransactionTemplate transactionTemplate;
    private UserBulkUploadService proxyService;
    private UserBulkUploadServiceImpl service;

    @BeforeEach
    void setUp() {
        jobTracker = new JobTracker();
        userBulkRepository = mock(UserBulkRepository.class);
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
        transactionTemplate = new TransactionTemplate(txManager) {
            @Override
            public <T> T execute(org.springframework.transaction.support.TransactionCallback<T> action) {
                return action.doInTransaction(mock(org.springframework.transaction.TransactionStatus.class));
            }
        };
        proxyService = mock(UserBulkUploadService.class);
        service = new UserBulkUploadServiceImpl(
                jobTracker,
                userBulkRepository,
                validator,
                transactionTemplate,
                proxyService
        );
    }

    @Test
    void testExcelStreamProcessor_andAsyncProcessing(@TempDir Path tempDir) throws Exception {
        // 1. Create a sample .xlsx workbook
        File excelFile = tempDir.resolve("test_users.xlsx").toFile();
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(excelFile)) {
            Sheet sheet = workbook.createSheet("Users");

            // Header
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("First Name");
            header.createCell(1).setCellValue("Last Name");
            header.createCell(2).setCellValue("Email");
            header.createCell(3).setCellValue("Password");
            header.createCell(4).setCellValue("Phone");
            header.createCell(5).setCellValue("Role");

            // Row 1: Valid user
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("John");
            row1.createCell(1).setCellValue("Doe");
            row1.createCell(2).setCellValue("john.doe@example.com");
            row1.createCell(3).setCellValue("Secret123");
            row1.createCell(4).setCellValue("1234567890");
            row1.createCell(5).setCellValue("USER");

            // Row 2: Invalid phone (not 10 digits)
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("Jane");
            row2.createCell(1).setCellValue("Smith");
            row2.createCell(2).setCellValue("jane.smith@example.com");
            row2.createCell(3).setCellValue("Secret123");
            row2.createCell(4).setCellValue("123"); // invalid phone
            row2.createCell(5).setCellValue("ADMIN");

            // Row 3: Duplicate email of row 1 in same file
            Row row3 = sheet.createRow(3);
            row3.createCell(0).setCellValue("Johnny");
            row3.createCell(1).setCellValue("Duplicate");
            row3.createCell(2).setCellValue("john.doe@example.com");
            row3.createCell(3).setCellValue("Secret123");
            row3.createCell(4).setCellValue("9876543210");
            row3.createCell(5).setCellValue("USER");

            workbook.write(fos);
        }

        // 2. Test ExcelStreamProcessor directly
        List<ExcelRowRecord> records = new ArrayList<>();
        ExcelStreamProcessor.process(excelFile, records::addAll);
        assertEquals(3, records.size());
        assertEquals("John", records.get(0).getDto().getFirstName());
        assertEquals("1234567890", records.get(0).getDto().getPhone());

        // 3. Test service.processFileAsync with job tracking
        String jobId = "test-job-123";
        jobTracker.createJob(jobId);

        when(userBulkRepository.findExistingEmails(any())).thenReturn(Collections.emptySet());

        service.processFileAsync(jobId, excelFile);

        JobStatusResponse status = jobTracker.toResponse(jobId);
        assertEquals(JobState.JobStatus.COMPLETED.name(), status.getStatus());
        assertEquals(3, status.getTotalRows());
        assertEquals(1, status.getSuccessfulRows());
        assertEquals(2, status.getFailedRows());

        List<RowError> failedRows = jobTracker.getFailedRows(jobId);
        assertEquals(2, failedRows.size());

        // Check that row 2 failed due to phone validation (1-indexed row 3 in excel sheet)
        assertTrue(failedRows.stream().anyMatch(e -> e.getRowNumber() == 3 && e.getReason().contains("phone")));

        // Check that row 3 failed due to duplicate email in file (1-indexed row 4 in excel sheet)
        assertTrue(failedRows.stream().anyMatch(e -> e.getRowNumber() == 4 && e.getReason().contains("Duplicate email")));

        // Verify batch insert was called for the 1 valid row
        verify(userBulkRepository, times(1)).batchInsert(argThat(list -> list.size() == 1));
    }

    @Test
    void testUploadExcel_createsJobAndInvokesProxy() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[10]
        );

        UploadResponse response = service.uploadExcel(file);
        assertNotNull(response.getJobId());
        assertTrue(response.getStatusUrl().contains(response.getJobId()));

        JobStatusResponse jobStatus = service.getJobStatus(response.getJobId());
        assertEquals(JobState.JobStatus.PENDING.name(), jobStatus.getStatus());

        verify(proxyService, times(1)).processFileAsync(eq(response.getJobId()), any());
    }
}