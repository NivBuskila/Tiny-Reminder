package com.example.tinyreminder.models;

import java.util.HashMap;
import java.util.Map;

public class ParkingEvent {
    private String id; // Unique ID for the parking event
    private String userId; // ID of the user who created the event
    private long timestamp; // Timestamp when the event was created
    private String status; // Current status of the event: "PENDING", "CONFIRMED", "CANCELLED", "ALERTED"
    private double latitude; // Latitude of the parking location
    private double longitude; // Longitude of the parking location

    public ParkingEvent() {
        // Default constructor required for calls to DataSnapshot.getValue(ParkingEvent.class)
    }

    public ParkingEvent(String userId, long timestamp, double latitude, double longitude) {
        this.userId = userId; // Initialize user ID
        this.timestamp = timestamp; // Initialize timestamp
        this.status = "PENDING"; // Default status
        this.latitude = latitude; // Initialize latitude
        this.longitude = longitude; // Initialize longitude
    }

    // Getters and setters for parking event properties

    public String getId() {
        return id; // Get the event ID
    }

    public void setId(String id) {
        this.id = id; // Set the event ID
    }

    public String getUserId() {
        return userId; // Get the user ID
    }

    public void setUserId(String userId) {
        this.userId = userId; // Set the user ID
    }

    public String getStatus() {
        return status; // Get the event status
    }

    // Method to convert the object to a map for Firebase database
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("userId", userId); // Add user ID to the map
        result.put("timestamp", timestamp); // Add timestamp to the map
        result.put("status", status); // Add status to the map
        result.put("latitude", latitude); // Add latitude to the map
        result.put("longitude", longitude); // Add longitude to the map
        return result; // Return the map representation of the object
    }
}
