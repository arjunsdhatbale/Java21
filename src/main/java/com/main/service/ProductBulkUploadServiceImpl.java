package com.main.service;

import com.main.model.dto.*;
import com.main.model.entity.Product;
import com.main.repo.ProductBulkRepository;
import com.main.shared.exception.InvalidFileException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductBulkUploadServiceImpl implements ProductBulkUploadService {

    private final JobTracker jobTracker;
    private final ProductBulkRepository productBulkRepository;
    private final Validator validator;
    private final TransactionTemplate transactionTemplate;
    private final ProductBulkUploadService productBulkUploadService;

    public ProductBulkUploadServiceImpl(JobTracker jobTracker,
                                        ProductBulkRepository productBulkRepository,
                                        Validator validator,
                                        TransactionTemplate transactionTemplate,
                                        @Lazy ProductBulkUploadService productBulkUploadService) {
        this.jobTracker = jobTracker;
        this.productBulkRepository = productBulkRepository;
        this.validator = validator;
        this.transactionTemplate = transactionTemplate;
        this.productBulkUploadService = productBulkUploadService;
    }

    @Override
    public UploadResponse uploadExcel(MultipartFile file) {
        validateFile(file);

        String jobId = UUID.randomUUID().toString();
        File stagedFile = stageToTempFile(file);
        jobTracker.createJob(jobId);

        productBulkUploadService.processFileAsync(jobId, stagedFile);

        return UploadResponse.builder()
                .jobId(jobId)
                .message("Upload accepted. Processing has started.")
                .statusUrl("/api/v1/products/bulk-upload/status/" + jobId)
                .build();
    }

    @Override
    public JobStatusResponse getJobStatus(String jobId) {
        return jobTracker.toResponse(jobId);
    }

    @Override
    public List<RowError> getFailedRows(String jobId) {
        return jobTracker.getFailedRows(jobId);
    }

    @Override
    @Async("bulkUploadExecutor")
    public void processFileAsync(String jobId, File stagedFile) {
        JobState job = jobTracker.get(jobId);
        job.markStarted();

        Set<String> namesSeenInFile = new HashSet<>();

        try {
            ProductExcelStreamProcessor.process(stagedFile,
                    batch -> processBatch(job, batch, namesSeenInFile));
            job.markCompleted();
        } catch (Exception ex) {
            log.error("Bulk upload job {} failed", jobId, ex);
            job.markFailed("Processing failed: " + ex.getMessage());
        } finally {
            deleteQuietly(stagedFile);
        }
    }

    private void processBatch(JobState job, List<ProductExcelRowRecord> batch, Set<String> namesSeenInFile) {
        List<ProductExcelRowRecord> validRows = new ArrayList<>();

        // 1. Field-level validation (@NotBlank, @NotNull, @DecimalMin, @Min on ProductRequestDto)
        for (ProductExcelRowRecord row : batch) {
            Set<ConstraintViolation<ProductRequestDto>> violations = validator.validate(row.getDto());
            if (!violations.isEmpty()) {
                String reason = violations.stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.joining("; "));
                job.addFailedRow(RowError.builder()
                        .rowNumber(row.getRowNumber())
                        .email(row.getDto().getName())
                        .identifier(row.getDto().getName())
                        .reason(reason)
                        .build());
            } else {
                validRows.add(row);
            }
        }

        // 2. Duplicate product name detection
        Set<String> candidateNames = validRows.stream()
                .map(r -> r.getDto().getName().toLowerCase())
                .collect(Collectors.toSet());
        Set<String> existingInDb = productBulkRepository.findExistingProductNames(candidateNames);

        List<ProductExcelRowRecord> insertableRows = new ArrayList<>();
        for (ProductExcelRowRecord row : validRows) {
            String name = row.getDto().getName().toLowerCase();
            if (namesSeenInFile.contains(name)) {
                job.addFailedRow(RowError.builder()
                        .rowNumber(row.getRowNumber())
                        .email(row.getDto().getName())
                        .identifier(row.getDto().getName())
                        .reason("Duplicate product name within uploaded file")
                        .build());
            } else if (existingInDb.contains(name)) {
                job.addFailedRow(RowError.builder()
                        .rowNumber(row.getRowNumber())
                        .email(row.getDto().getName())
                        .identifier(row.getDto().getName())
                        .reason("Product name already exists in the system")
                        .build());
            } else {
                namesSeenInFile.add(name);
                insertableRows.add(row);
            }
        }

        // 3. Batch insert in own transaction
        int inserted = 0;
        if (!insertableRows.isEmpty()) {
            List<Product> entities = insertableRows.stream()
                    .map(r -> toEntity(r.getDto()))
                    .collect(Collectors.toList());
            try {
                Boolean success = transactionTemplate.execute(status -> {
                    productBulkRepository.batchInsert(entities);
                    return true;
                });
                inserted = Boolean.TRUE.equals(success) ? entities.size() : 0;
            } catch (Exception ex) {
                log.error("Batch insert failed for job {}", job.getJobId(), ex);
                for (ProductExcelRowRecord row : insertableRows) {
                    job.addFailedRow(RowError.builder()
                            .rowNumber(row.getRowNumber())
                            .email(row.getDto().getName())
                            .identifier(row.getDto().getName())
                            .reason("Insert failed: " + ex.getMessage())
                            .build());
                }
                inserted = 0;
            }
        }

        int failedInBatch = batch.size() - inserted;
        job.recordBatchResult(batch.size(), inserted, failedInBatch);
    }

    private Product toEntity(ProductRequestDto dto) {
        Product.ProductStatus status = parseStatus(dto.getStatus());
        return Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .stock(dto.getStock())
                .category(dto.getCategory())
                .imageUrl(dto.getImageUrl())
                .status(status)
                .build();
    }

    private Product.ProductStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return Product.ProductStatus.ACTIVE;
        }
        try {
            return Product.ProductStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Product.ProductStatus.ACTIVE;
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Uploaded file is empty");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".xlsx")) {
            throw new InvalidFileException("Only .xlsx files are supported");
        }
    }

    private File stageToTempFile(MultipartFile file) {
        try {
            File temp = File.createTempFile("bulk-upload-products-", ".xlsx");
            file.transferTo(temp);
            return temp;
        } catch (IOException ex) {
            throw new InvalidFileException("Could not read uploaded file: " + ex.getMessage());
        }
    }

    private void deleteQuietly(File file) {
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException ex) {
            log.warn("Could not delete temp file {}", file.getAbsolutePath());
        }
    }
}
