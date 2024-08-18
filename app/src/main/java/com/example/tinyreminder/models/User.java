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
        private double latitude;
        private double longitude;
        private Map<String, Object> avatar;
        private String status;

        private String profilePictureUrl;
        private String fcmToken;
        private Map<String, Boolean> notifications;

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
        public User(String id, String name, String email, String phoneNumber, String avatarUrl, String familyId, double latitude, double longitude) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.phoneNumber = phoneNumber;
            this.avatarUrl = avatarUrl;
            this.familyId = familyId;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public boolean hasPhoneNumber() {
            return phoneNumber != null && !phoneNumber.isEmpty();
        }

        // Getters and setters
        public String getId() {
            return id;
        }

        public String getFcmToken() {
            return fcmToken;
        }

        public void setFcmToken(String fcmToken) {
            this.fcmToken = fcmToken;
        }

        public Map<String, Boolean> getNotifications() {
            return notifications;
        }

        public void setNotifications(Map<String, Boolean> notifications) {
            this.notifications = notifications;
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
        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
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

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
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
            result.put("status", status);
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
                    ", status='" + status + '\'' +
                    '}';
        }
    }