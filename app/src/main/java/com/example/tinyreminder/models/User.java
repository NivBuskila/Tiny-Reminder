package com.example.tinyreminder.models;

import androidx.annotation.NonNull;
import java.util.HashMap;
import java.util.Map;

public class User {
    private String id; // User ID
    private String name; // User's name
    private String email; // User's email
    private String phoneNumber; // User's phone number
    private String avatarUrl; // URL to user's avatar
    private String familyId; // ID of the family the user belongs to
    private double latitude; // User's last known latitude
    private double longitude; // User's last known longitude
    private Map<String, Object> avatar; // Additional avatar details
    private String status; // User's current status
    private boolean isAlerted; // Whether the user has been alerted
    private String profilePictureUrl; // URL to user's profile picture
    private String fcmToken; // Firebase Cloud Messaging token for push notifications
    private Map<String, Boolean> notifications; // Map of notification preferences

    // Default constructor required for calls to DataSnapshot.getValue(User.class)
    public User() {}

    // Constructor with basic info
    public User(String id, String name, String email, String phoneNumber) {
        this.id = id; // Initialize user ID
        this.name = name; // Initialize user name
        this.email = email; // Initialize user email
        this.phoneNumber = phoneNumber; // Initialize user phone number
    }

    // Full constructor with additional info
    public User(String id, String name, String email, String phoneNumber, String avatarUrl, String familyId, double latitude, double longitude) {
        this.id = id; // Initialize user ID
        this.name = name; // Initialize user name
        this.email = email; // Initialize user email
        this.phoneNumber = phoneNumber; // Initialize user phone number
        this.avatarUrl = avatarUrl; // Initialize avatar URL
        this.familyId = familyId; // Initialize family ID
        this.latitude = latitude; // Initialize latitude
        this.longitude = longitude; // Initialize longitude
    }

    // Getters and setters for user properties

    public String getId() {
        return id; // Get the user ID
    }

    public void setId(String id) {
        this.id = id; // Set the user ID
    }

    public String getName() {
        return name; // Get the user's name
    }

    public void setName(String name) {
        this.name = name; // Set the user's name
    }

    public String getEmail() {
        return email; // Get the user's email
    }

    public void setEmail(String email) {
        this.email = email; // Set the user's email
    }

    public String getPhoneNumber() {
        return phoneNumber; // Get the user's phone number
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber; // Set the user's phone number
    }

    public String getAvatarUrl() {
        return avatarUrl; // Get the URL of the user's avatar
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl; // Set the URL of the user's avatar
    }

    public String getFamilyId() {
        return familyId; // Get the family ID
    }

    public void setFamilyId(String familyId) {
        this.familyId = familyId; // Set the family ID
    }

    public double getLatitude() {
        return latitude; // Get the user's last known latitude
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude; // Set the user's latitude
    }

    public double getLongitude() {
        return longitude; // Get the user's last known longitude
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude; // Set the user's longitude
    }

    public Map<String, Object> getAvatar() {
        return avatar; // Get additional avatar details
    }

    public void setAvatar(Map<String, Object> avatar) {
        this.avatar = avatar; // Set additional avatar details
    }

    public String getStatus() {
        return status; // Get the user's status
    }

    public void setStatus(String status) {
        this.status = status; // Set the user's status
    }

    public boolean isAlerted() {
        return isAlerted; // Check if the user has been alerted
    }

    public void setAlerted(boolean alerted) {
        isAlerted = alerted; // Set the alerted status
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl; // Get the URL of the user's profile picture
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl; // Set the URL of the user's profile picture
    }

    public String getFcmToken() {
        return fcmToken; // Get the Firebase Cloud Messaging token
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken; // Set the Firebase Cloud Messaging token
    }

    public Map<String, Boolean> getNotifications() {
        return notifications; // Get the notification preferences
    }

    public void setNotifications(Map<String, Boolean> notifications) {
        this.notifications = notifications; // Set the notification preferences
    }

    public boolean hasPhoneNumber() {
        return phoneNumber != null && !phoneNumber.isEmpty(); // Check if the user has a phone number
    }

    // Method to convert the object to a map for Firebase database
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("id", id); // Add user ID to the map
        result.put("name", name); // Add name to the map
        result.put("email", email); // Add email to the map
        result.put("phoneNumber", phoneNumber); // Add phone number to the map
        result.put("avatarUrl", avatarUrl); // Add avatar URL to the map
        result.put("familyId", familyId); // Add family ID to the map
        result.put("profilePictureUrl", profilePictureUrl); // Add profile picture URL to the map
        result.put("status", status); // Add status to the map
        result.put("isAlerted", isAlerted); // Add alerted status to the map
        return result; // Return the map representation of the object
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
                ", status='" + status + '\'' +
                '}';
    }
}
