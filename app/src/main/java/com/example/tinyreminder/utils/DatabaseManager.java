package com.example.tinyreminder.utils;

import androidx.annotation.NonNull;
import com.example.tinyreminder.models.Family;
import com.example.tinyreminder.models.RelationshipToChildren;
import com.example.tinyreminder.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class DatabaseManager {
    private DatabaseReference mDatabase;

    public DatabaseManager() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public void createUser(User user, final OnCompleteListener<Void> listener) {
        String userId = mDatabase.child("users").push().getKey();
        user.setId(userId);
        mDatabase.child("users").child(userId).setValue(user)
                .addOnCompleteListener(listener);
    }

    public void updateUserProfile(String userId, Map<String, Object> updates, final OnCompleteListener<Void> listener) {
        mDatabase.child("users").child(userId).updateChildren(updates)
                .addOnCompleteListener(listener);
    }
    public String createNewFamily(String familyName) {
        String familyId = mDatabase.child("families").push().getKey();
        Family family = new Family(familyId, familyName);
        mDatabase.child("families").child(familyId).setValue(family);
        return familyId;
    }

    public void addUserToFamily(String userId, String familyId, RelationshipToChildren relationship,
                                final OnCompleteListener<Void> listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("/users/" + userId + "/familyId", familyId);
        updates.put("/users/" + userId + "/relationshipToChildren", relationship.name());
        updates.put("/families/" + familyId + "/memberIds/" + userId, true);

        mDatabase.updateChildren(updates).addOnCompleteListener(listener);
    }

    public void getUserData(String userId, final ValueEventListener listener) {
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(listener);
    }

    public void getFamilyData(String familyId, final ValueEventListener listener) {
        mDatabase.child("families").child(familyId).addListenerForSingleValueEvent(listener);
    }

    public void updateUserRelationship(String userId, String relationship,
                                       final OnCompleteListener<Void> listener) {
        mDatabase.child("users").child(userId).child("relationshipToChildren")
                .setValue(relationship)
                .addOnCompleteListener(listener);
    }

    public void getFamilyMembers(String familyId, final ValueEventListener listener) {
        mDatabase.child("families").child(familyId).child("memberIds").addValueEventListener(listener);
    }

    public void getMemberData(String userId, final ValueEventListener listener) {
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(listener);
    }

    public void updateMemberLocation(String userId, double latitude, double longitude) {
        Map<String, Object> locationUpdates = new HashMap<>();
        locationUpdates.put("latitude", latitude);
        locationUpdates.put("longitude", longitude);
        mDatabase.child("locations").child(userId).setValue(locationUpdates);
    }
    public void createOrUpdateUser(User user, final OnCompleteListener<Void> listener) {
        mDatabase.child("users").child(user.getId()).setValue(user)
                .addOnCompleteListener(listener);
    }
}