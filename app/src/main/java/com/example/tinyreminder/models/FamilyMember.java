package com.example.tinyreminder.models;

public class FamilyMember {
    private String id; // Member ID
    private String name; // Member name
    private String role; // Member role in the family (e.g., "Member", "Manager")
    private ResponseStatus responseStatus; // Member's current response status
    private String profilePictureUrl; // URL of the member's profile picture

    // Enum representing the possible response statuses for a family member
    public enum ResponseStatus {
        OK,      // GREEN - Answered / no alert sent
        PENDING, // ORANGE - Waiting for OK
        ALERT    // RED - Didn't answer after 5 minutes
    }

    public FamilyMember() {
        // Default constructor required for calls to DataSnapshot.getValue(FamilyMember.class)
    }

    public FamilyMember(String id, String name, String role) {
        this.id = id; // Initialize member ID
        this.name = name; // Initialize member name
        this.role = role; // Initialize member role
        this.responseStatus = ResponseStatus.OK; // Default status
    }

    // Getters and setters for member properties

    public String getId() {
        return id; // Get the member ID
    }

    public void setId(String id) {
        this.id = id; // Set the member ID
    }

    public String getName() {
        return name; // Get the member name
    }

    public void setName(String name) {
        this.name = name; // Set the member name
    }

    public String getRole() {
        return role; // Get the member role
    }

    public void setRole(String role) {
        this.role = role; // Set the member role
    }

    public ResponseStatus getResponseStatus() {
        return responseStatus; // Get the member's current response status
    }

    public void setResponseStatus(ResponseStatus responseStatus) {
        this.responseStatus = responseStatus; // Set the member's response status
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl; // Get the URL of the member's profile picture
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl; // Set the URL of the member's profile picture
    }
}
