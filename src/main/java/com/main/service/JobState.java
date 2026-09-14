package com.main.service;

import com.main.model.dto.RowError;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class JobState {

    public enum JobStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }

    private final String jobId;
    private volatile JobStatus status;
    private final AtomicInteger totalRows = new AtomicInteger(0);
    private final AtomicInteger processedRows = new AtomicInteger(0);
    private final AtomicInteger successfulRows = new AtomicInteger(0);
    private final List<RowError> failedRows = new CopyOnWriteArrayList<>();
    private volatile String errorMessage;
    private final LocalDateTime createdAt;
    private volatile LocalDateTime startedAt;
    private volatile LocalDateTime completedAt;

    public JobState(String jobId) {
        this.jobId = jobId;
        this.status = JobStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public void markStarted() {
        this.status = JobStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    public void markCompleted() {
        this.status = JobStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void markFailed(String message) {
        this.status = JobStatus.FAILED;
        this.errorMessage = message;
        this.completedAt = LocalDateTime.now();
    }

    public void addFailedRow(RowError rowError) {
        this.failedRows.add(rowError);
    }

    public void recordBatchResult(int batchTotal, int inserted, int failed) {
        this.totalRows.addAndGet(batchTotal);
        this.processedRows.addAndGet(batchTotal);
        this.successfulRows.addAndGet(inserted);
    }

    public int getTotalRows() {
        return totalRows.get();
    }

    public int getProcessedRows() {
        return processedRows.get();
    }

    public int getSuccessfulRows() {
        return successfulRows.get();
    }

    public List<RowError> getFailedRows() {
        return Collections.unmodifiableList(failedRows);
    }
}
