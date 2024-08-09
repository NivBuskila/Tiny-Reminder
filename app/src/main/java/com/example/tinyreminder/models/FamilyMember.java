package com.example.tinyreminder.models;

public class FamilyMember {
    private String id;
    private String name;
    private String role;
    private ResponseStatus responseStatus;

    public enum ResponseStatus {
        OK,      // GREEN - Answered / no alert sent
        PENDING, // ORANGE - Waiting for OK
        ALERT    // RED - Dosnt answered after 5 min
    }

    public FamilyMember() {
        // Default constructor required for calls to DataSnapshot.getValue(FamilyMember.class)
    }

    public FamilyMember(String id, String name, String role) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.responseStatus = ResponseStatus.OK; // Default status
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public ResponseStatus getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(ResponseStatus responseStatus) {
        this.responseStatus = responseStatus;
    }
}