package com.example.tinyreminder.models;

import androidx.annotation.NonNull;

public class User {
    private String id;
    private String name;
    private String email;
    private String phoneNumber;
    private String avatarUrl;
    private String familyId;
    private String relationshipToChildren;

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
    public User(String id, String name, String email, String phoneNumber, String avatarUrl, String familyId, String relationshipToChildren) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.avatarUrl = avatarUrl;
        this.familyId = familyId;
        this.relationshipToChildren = relationshipToChildren;
    }

    // Constructor with RelationshipToChildren enum
    public User(String id, String name, String email, String phoneNumber, String avatarUrl, String familyId, RelationshipToChildren relationshipToChildren) {
        this(id, name, email, phoneNumber, avatarUrl, familyId, relationshipToChildren.name());
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

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getFamilyId() {
        return familyId;
    }

    public void setFamilyId(String familyId) {
        this.familyId = familyId;
    }

    public String getRelationshipToChildren() {
        return relationshipToChildren;
    }

    public void setRelationshipToChildren(String relationshipToChildren) {
        this.relationshipToChildren = relationshipToChildren;
    }

    // Helper methods for RelationshipToChildren enum
    public RelationshipToChildren getRelationshipToChildrenEnum() {
        return relationshipToChildren != null ? RelationshipToChildren.valueOf(relationshipToChildren) : null;
    }

    public void setRelationshipToChildrenEnum(RelationshipToChildren relationshipToChildren) {
        this.relationshipToChildren = relationshipToChildren.name();
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
                ", relationshipToChildren='" + relationshipToChildren + '\'' +
                '}';
    }
}