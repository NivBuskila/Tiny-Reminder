package com.example.tinyreminder.models;

public enum RelationshipToChildren {
    FATHER("Father"),
    MOTHER("Mother"),
    BROTHER("Brother"),
    SISTER("Sister"),
    GRANDFATHER("Grandfather"),
    GRANDMOTHER("Grandmother"),
    RELATIVE("Relative"),
    SELF("Self"),
    OTHER("Other");

    private final String displayName;

    RelationshipToChildren(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static RelationshipToChildren fromString(String text) {
        for (RelationshipToChildren r : RelationshipToChildren.values()) {
            if (r.name().equalsIgnoreCase(text) || r.displayName.equalsIgnoreCase(text)) {
                return r;
            }
        }
        return OTHER;
    }

    @Override
    public String toString() {
        return displayName;
    }
}