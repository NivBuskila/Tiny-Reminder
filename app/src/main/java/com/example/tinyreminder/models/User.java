package com.example.tinyreminder.models;

import androidx.annotation.NonNull;
import java.util.HashMap;
import java.util.Map;

public class User {
    private String id;
    private String name;
    private String email;
    private String phoneNumber;
    private String avatarUrl;
    private String familyId;
    private Map<String, Object> avatar;

    private String profilePictureUrl;

    // Default constructor required for calls to DataSnapshot.getValue(User.class)
    public User() {}

    // Constructor with basic info
    public User(String id, String name, String email, String phoneNumber) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    // Full constructor
    public User(String id, String name, String email, String phoneNumber, String avatarUrl, String familyId) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.avatarUrl = avatarUrl;
        this.familyId = familyId;
    }

    public boolean hasPhoneNumber() {
        return phoneNumber != null && !phoneNumber.isEmpty();
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }
    public Map<String, Object> getAvatar() {
        return avatar;
    }

    public void setAvatar(Map<String, Object> avatar) {
        this.avatar = avatar;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getFamilyId() {
        return familyId;
    }

    public void setFamilyId(String familyId) {
        this.familyId = familyId;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("name", name);
        result.put("email", email);
        result.put("phoneNumber", phoneNumber);
        result.put("avatarUrl", avatarUrl);
        result.put("familyId", familyId);
        result.put("profilePictureUrl", profilePictureUrl);
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", familyId='" + familyId + '\'' +
                '}';
    }
}