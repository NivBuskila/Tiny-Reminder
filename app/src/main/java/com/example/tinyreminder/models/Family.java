package com.example.tinyreminder.models;

import java.util.HashMap;
import java.util.Map;

public class Family {
    public String id; // Family ID
    public String name; // Family name
    public Map<String, Boolean> memberIds; // Map of member IDs with their presence status
    public Map<String, Boolean> adminIds; // Map of admin IDs with their presence status
    public Map<String, Map<String, Double>> memberLocations; // Map of member locations (latitude, longitude)

    public Family() {
        // Default constructor required for calls to DataSnapshot.getValue(Family.class)
    }

    public Family(String id, String name, String creatorId) {
        this.id = id; // Initialize family ID
        this.name = name; // Initialize family name
        this.memberIds = new HashMap<>(); // Initialize member IDs map
        this.adminIds = new HashMap<>(); // Initialize admin IDs map
        this.memberLocations = new HashMap<>(); // Initialize member locations map
        this.memberIds.put(creatorId, true); // Add creator as a member
        this.adminIds.put(creatorId, true); // Add creator as an admin
    }

    // Getters and setters for admin IDs and member locations

    public Map<String, Boolean> getAdminIds() {
        return adminIds; // Get the map of admin IDs
    }

    public Map<String, Map<String, Double>> getMemberLocations() {
        return memberLocations; // Get the map of member locations
    }

    public void setMemberLocations(Map<String, Map<String, Double>> memberLocations) {
        this.memberLocations = memberLocations; // Set the map of member locations
    }

    public void setAdminIds(Map<String, Boolean> adminIds) {
        this.adminIds = adminIds; // Set the map of admin IDs
    }

    // Method to add an admin by user ID
    public void addAdmin(String userId) {
        if (this.adminIds == null) {
            this.adminIds = new HashMap<>(); // Initialize admin IDs map if null
        }
        this.adminIds.put(userId, true); // Add user as an admin
    }

    // Method to remove an admin by user ID
    public void removeAdmin(String userId) {
        if (this.adminIds != null) {
            this.adminIds.remove(userId); // Remove user from admin list
        }
    }

    // Method to check if a user is an admin
    public boolean isAdmin(String userId) {
        return this.adminIds != null && this.adminIds.containsKey(userId); // Return true if user is an admin
    }

    // Getters for family name and ID
    public String getName() {
        return name; // Get the family name
    }

    public String getId() {
        return id; // Get the family ID
    }
}
