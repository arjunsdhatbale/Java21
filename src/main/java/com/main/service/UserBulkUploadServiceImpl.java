package com.main.service;



 import com.main.model.dto.*;
 import com.main.model.entity.User;
 import com.main.repo.UserBulkRepository;
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
import java.util.ArrayList;
 import java.util.HashSet;
import java.util.List;
 import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserBulkUploadServiceImpl implements UserBulkUploadService {

    private final JobTracker jobTracker;
    private final UserBulkRepository userBulkRepository;
    private final Validator validator;
   // private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;

    // Self-reference obtained lazily. Calling processFileAsync via `this.` from
    // inside this same bean would bypass Spring's @Async proxy entirely and run
    // synchronously on the caller's thread — a classic self-invocation gotcha.
    // Injecting the interface (not the concrete class) back into itself gets us
    // the proxied bean instead, so @Async actually takes effect.
    private final UserBulkUploadService userBulkUploadService;

    public UserBulkUploadServiceImpl(JobTracker jobTracker,
                                     UserBulkRepository userBulkRepository,
                                     Validator validator,
                                   //  PasswordEncoder passwordEncoder,
                                     TransactionTemplate transactionTemplate,
                                     @Lazy UserBulkUploadService userBulkUploadService) {
        this.jobTracker = jobTracker;
        this.userBulkRepository = userBulkRepository;
        this.validator = validator;
       // this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = transactionTemplate;
        this.userBulkUploadService = userBulkUploadService;
    }

    @Override
    public UploadResponse uploadExcel(MultipartFile file) {
        validateFile(file);

        String jobId = UUID.randomUUID().toString();
        File stagedFile = stageToTempFile(file);
        jobTracker.createJob(jobId);

        // Kick off the background worker through the proxy (userBulkUploadService), not `this`.
        userBulkUploadService.processFileAsync(jobId, stagedFile);

        return UploadResponse.builder()
                .jobId(jobId)
                .message("Upload accepted. Processing has started.")
                .statusUrl("/api/users/bulk-upload/status/" + jobId)
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

        // Emails already inserted from earlier batches in *this* file, so we
        // catch duplicates that span batches, not just duplicates within one
        // batch of 1000.
        Set<String> emailsSeenInFile = new HashSet<>();

        try {
            ExcelStreamProcessor.process(stagedFile,
                    batch -> processBatch(job, batch, emailsSeenInFile));
            job.markCompleted();
        } catch (Exception ex) {
            log.error("Bulk upload job {} failed", jobId, ex);
            job.markFailed("Processing failed: " + ex.getMessage());
        } finally {
            deleteQuietly(stagedFile);
        }
    }

    /**
     * Validates, deduplicates, and inserts one batch (~1000 rows) of parsed
     * Excel rows. Runs inside its own transaction so a failure in one batch
     * only rolls back that batch, not the whole job.
     */
    private void processBatch(JobState job, List<ExcelRowRecord> batch, Set<String> emailsSeenInFile) {
        List<ExcelRowRecord> validRows = new ArrayList<>();

        // 1. Field-level validation (@NotBlank, @Email, @Pattern on UserRequestDto)
        for (ExcelRowRecord row : batch) {
            Set<ConstraintViolation<UserRequestDto>> violations = validator.validate(row.getDto());
            if (!violations.isEmpty()) {
                String reason = violations.stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.joining("; "));
                job.addFailedRow(RowError.builder()
                        .rowNumber(row.getRowNumber())
                        .email(row.getDto().getEmail())
                        .reason(reason)
                        .build());
            } else {
                validRows.add(row);
            }
        }

        // 2. Duplicate detection: within this batch, within the file so far,
        // and against what's already in the database — in that order, so we
        // don't waste a DB round trip on rows we can already reject in memory.
        Set<String> candidateEmails = validRows.stream()
                .map(r -> r.getDto().getEmail().toLowerCase())
                .collect(Collectors.toSet());
        Set<String> existingInDb = userBulkRepository.findExistingEmails(candidateEmails);

        List<ExcelRowRecord> insertableRows = new ArrayList<>();
        for (ExcelRowRecord row : validRows) {
            String email = row.getDto().getEmail().toLowerCase();
            if (emailsSeenInFile.contains(email)) {
                job.addFailedRow(RowError.builder()
                        .rowNumber(row.getRowNumber()).email(email)
                        .reason("Duplicate email within uploaded file").build());
            } else if (existingInDb.contains(email)) {
                job.addFailedRow(RowError.builder()
                        .rowNumber(row.getRowNumber()).email(email)
                        .reason("Email already exists in the system").build());
            } else {
                emailsSeenInFile.add(email);
                insertableRows.add(row);
            }
        }

        // 3. Map to entities and insert in one batch, in its own transaction.
        int inserted = 0;
        if (!insertableRows.isEmpty()) {
            List<User> entities = insertableRows.stream()
                    .map(r -> toEntity(r.getDto()))
                    .collect(Collectors.toList());
            try {
                Boolean success = transactionTemplate.execute(status -> {
                    userBulkRepository.batchInsert(entities);
                    return true;
                });
                inserted = Boolean.TRUE.equals(success) ? entities.size() : 0;
            } catch (Exception ex) {
                log.error("Batch insert failed for job {}", job.getJobId(), ex);
                for (ExcelRowRecord row : insertableRows) {
                    job.addFailedRow(RowError.builder()
                            .rowNumber(row.getRowNumber())
                            .email(row.getDto().getEmail())
                            .reason("Insert failed: " + ex.getMessage())
                            .build());
                }
                inserted = 0;
            }
        }

        int failedInBatch = batch.size() - inserted;
        job.recordBatchResult(batch.size(), inserted, failedInBatch);
    }

    private User toEntity(UserRequestDto dto) {
        User.UserRole role = parseRole(dto.getRole());
        String rawPassword = (dto.getPassword() == null || dto.getPassword().isBlank())
                ? UUID.randomUUID().toString()
                : dto.getPassword();

        return User.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail().toLowerCase())
                //.password(passwordEncoder.encode(rawPassword))
                .password(rawPassword)
                .phone(dto.getPhone())
                .role(role)
                .status(User.UserStatus.ACTIVE)
                .build();
    }

    private User.UserRole parseRole(String role) {
        if (role == null || role.isBlank()) {
            return User.UserRole.USER;
        }
        try {
            return User.UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return User.UserRole.USER;
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
            File temp = File.createTempFile("bulk-upload-", ".xlsx");
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
