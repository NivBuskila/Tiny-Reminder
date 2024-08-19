package com.example.tinyreminder.models;

import java.util.HashMap;
import java.util.Map;

public class ParkingEvent {
    private String id;
    private String userId;
    private long timestamp;
    private String status; // "PENDING", "CONFIRMED", "CANCELLED", "ALERTED"
    private double latitude;
    private double longitude;

    public ParkingEvent() {
        // Default constructor required for calls to DataSnapshot.getValue(ParkingEvent.class)
    }

    public ParkingEvent(String userId, long timestamp, double latitude, double longitude) {
        this.userId = userId;
        this.timestamp = timestamp;
        this.status = "PENDING";
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and setters


    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getStatus() {
        return status;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("timestamp", timestamp);
        result.put("status", status);
        result.put("latitude", latitude);
        result.put("longitude", longitude);
        return result;
    }
}