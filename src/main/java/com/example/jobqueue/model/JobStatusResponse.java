package com.example.jobqueue.model;

public class JobStatusResponse {

    private String jobId;
    private JobStatus status;
    private String result;
    private String errorMessage;
    private int attempts;

    public JobStatusResponse() {
    }

    public JobStatusResponse(String jobId, JobStatus status, String result, String errorMessage, int attempts) {
        this.jobId = jobId;
        this.status = status;
        this.result = result;
        this.errorMessage = errorMessage;
        this.attempts = attempts;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }
}
