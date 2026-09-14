package com.main.service;

import com.main.model.dto.JobStatusResponse;
import com.main.model.dto.RowError;
import com.main.model.dto.UploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

public interface UserBulkUploadService {
    /** Validates the upload, stages the file, registers a job, and kicks off async processing. */
    UploadResponse uploadExcel(MultipartFile file);

    JobStatusResponse getJobStatus(String jobId);

    List<RowError> getFailedRows(String jobId);

    /**
     * The actual background worker. Public (and called through the Spring proxy,
     * never via `this.`) so that @Async takes effect — see UserBulkUploadServiceImpl
     * for why self-invocation would otherwise silently run synchronously.
     */
    void processFileAsync(String jobId, File stagedFile);
}
