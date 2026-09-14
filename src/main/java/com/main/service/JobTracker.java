package com.main.service;

import com.main.model.dto.JobStatusResponse;
import com.main.model.dto.RowError;
import com.main.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JobTracker {

    private final Map<String, JobState> jobs = new ConcurrentHashMap<>();

    public void createJob(String jobId) {
        jobs.put(jobId, new JobState(jobId));
    }

    public JobState get(String jobId) {
        JobState job = jobs.get(jobId);
        if (job == null) {
            throw new ResourceNotFoundException("Bulk upload job", "jobId", jobId);
        }
        return job;
    }

    public JobStatusResponse toResponse(String jobId) {
        JobState job = get(jobId);
        return JobStatusResponse.builder()
                .jobId(job.getJobId())
                .status(job.getStatus().name())
                .totalRows(job.getTotalRows())
                .processedRows(job.getProcessedRows())
                .successfulRows(job.getSuccessfulRows())
                .failedRows(job.getFailedRows().size())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }

    public List<RowError> getFailedRows(String jobId) {
        JobState job = get(jobId);
        return job.getFailedRows();
    }
}
