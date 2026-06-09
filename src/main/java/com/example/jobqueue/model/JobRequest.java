package com.example.jobqueue.model;

import java.util.HashMap;
import java.util.Map;

public class JobRequest {

    private Map<String, Object> payload = new HashMap<>();

    public JobRequest() {
    }

    public JobRequest(Map<String, Object> payload) {
        this.payload = payload;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}
