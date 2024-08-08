package com.example.tinyreminder.models;

import java.util.HashMap;
import java.util.Map;

public class Family {
    public String id;
    public String name;
    public Map<String, Boolean> memberIds;

    public Family() {
        // Default constructor required for calls to DataSnapshot.getValue(Family.class)
    }

    public Family(String id, String name) {
        this.id = id;
        this.name = name;
        this.memberIds = new HashMap<>();
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Boolean> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(Map<String, Boolean> memberIds) {
        this.memberIds = memberIds;
    }

    public void addMember(String userId) {
        if (this.memberIds == null) {
            this.memberIds = new HashMap<>();
        }
        this.memberIds.put(userId, true);
    }

    public void removeMember(String userId) {
        if (this.memberIds != null) {
            this.memberIds.remove(userId);
        }
    }
}