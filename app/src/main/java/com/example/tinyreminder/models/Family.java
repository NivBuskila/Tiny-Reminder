package com.example.tinyreminder.models;

import java.util.HashMap;
import java.util.Map;

public class Family {
    public String id;
    public String name;
    public Map<String, Boolean> memberIds;
    public Map<String, Boolean> adminIds;

    public Family() {
        // Default constructor required for calls to DataSnapshot.getValue(Family.class)
    }

    public Family(String id, String name, String creatorId) {
        this.id = id;
        this.name = name;
        this.memberIds = new HashMap<>();
        this.adminIds = new HashMap<>();
        this.memberIds.put(creatorId, true);
        this.adminIds.put(creatorId, true);
    }

    // Existing getters and setters...

    public Map<String, Boolean> getAdminIds() {
        return adminIds;
    }

    public void setAdminIds(Map<String, Boolean> adminIds) {
        this.adminIds = adminIds;
    }

    public void addAdmin(String userId) {
        if (this.adminIds == null) {
            this.adminIds = new HashMap<>();
        }
        this.adminIds.put(userId, true);
    }

    public void removeAdmin(String userId) {
        if (this.adminIds != null) {
            this.adminIds.remove(userId);
        }
    }

    public boolean isAdmin(String userId) {
        return this.adminIds != null && this.adminIds.containsKey(userId);
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }


}