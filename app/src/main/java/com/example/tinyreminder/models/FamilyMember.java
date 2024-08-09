package com.example.tinyreminder.models;

public class FamilyMember {
    private String id;
    private String name;
    private String avatarUrl;

    public FamilyMember() {
        // Default constructor required for calls to DataSnapshot.getValue(FamilyMember.class)
    }

    public FamilyMember(String id, String name, String avatarUrl) {
        this.id = id;
        this.name = name;
        this.avatarUrl = avatarUrl;
    }

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

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}