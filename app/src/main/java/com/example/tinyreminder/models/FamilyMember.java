package com.example.tinyreminder.models;

public class FamilyMember {
    private String id;
    private String name;
    private String avatarUrl;
    private String relationshipToUser;

    public FamilyMember() {
        // Default constructor required for calls to DataSnapshot.getValue(FamilyMember.class)
    }

    public FamilyMember(String id, String name, String avatarUrl, String relationshipToUser) {
        this.id = id;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.relationshipToUser = relationshipToUser;
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

    public String getRelationshipToUser() {
        return relationshipToUser;
    }

    public void setRelationshipToUser(String relationshipToUser) {
        this.relationshipToUser = relationshipToUser;
    }

    // Helper method to get the enum version of the relationship
    public RelationshipToChildren getRelationshipToUserEnum() {
        return RelationshipToChildren.valueOf(relationshipToUser);
    }

    // Helper method to set the relationship using the enum
    public void setRelationshipToUserEnum(RelationshipToChildren relationshipToUser) {
        this.relationshipToUser = relationshipToUser.name();
    }
}