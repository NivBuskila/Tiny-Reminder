    package com.example.tinyreminder.utils;

    import android.content.Context;
    import android.content.Intent;
    import android.net.Uri;
    import android.util.Log;

    import androidx.annotation.NonNull;

    import com.example.tinyreminder.models.Family;
    import com.example.tinyreminder.models.ParkingEvent;
    import com.example.tinyreminder.models.User;
    import com.google.android.gms.maps.model.LatLng;
    import com.google.android.gms.tasks.OnCompleteListener;
    import com.google.android.gms.tasks.Task;
    import com.google.android.gms.tasks.Tasks;
    import com.google.firebase.database.ChildEventListener;
    import com.google.firebase.database.DataSnapshot;
    import com.google.firebase.database.DatabaseError;
    import com.google.firebase.database.DatabaseReference;
    import com.google.firebase.database.FirebaseDatabase;
    import com.google.firebase.database.ValueEventListener;
    import com.google.firebase.storage.FirebaseStorage;
    import com.google.firebase.storage.StorageReference;
    import com.google.firebase.storage.UploadTask;

    import java.util.HashMap;
    import java.util.Map;
    import java.util.concurrent.atomic.AtomicLong;

    public class DatabaseManager {
        private static final String TAG = "DatabaseManager";
        private DatabaseReference mDatabase;
        private StorageReference mStorage;
        private Context context;

        /**
         * Constructor for DatabaseManager, initializes the Firebase database and storage references.
         *
         * @param context The application context.
         */
        public DatabaseManager(Context context) {
            this.context = context.getApplicationContext();
            mDatabase = FirebaseDatabase.getInstance().getReference();
            mStorage = FirebaseStorage.getInstance().getReference();
        }

        /**
         * Creates a new user in the Firebase database.
         *
         * @param user     The User object to be created.
         * @param listener Listener for the completion of the operation.
         */
        public void createUser(User user, final OnCompleteListener<Void> listener) {
            mDatabase.child("users").child(user.getId()).setValue(user)
                    .addOnCompleteListener(listener);
        }

        /**
         * Updates the user's profile in the Firebase database.
         *
         * @param userId   The ID of the user to update.
         * @param updates  A map of fields to update in the user's profile.
         * @param listener Listener for the completion of the operation.
         */
        public void updateUserProfile(String userId, Map<String, Object> updates, final OnCompleteListener<Void> listener) {
            mDatabase.child("users").child(userId).updateChildren(updates)
                    .addOnCompleteListener(listener);
        }

        /**
         * Uploads a profile picture to Firebase storage and retrieves the download URL.
         *
         * @param userId   The ID of the user whose profile picture is being uploaded.
         * @param imageUri The URI of the image file to upload.
         * @param listener Listener for the completion of the operation, returns the download URL.
         */
        public void uploadProfilePicture(String userId, Uri imageUri, OnCompleteListener<Uri> listener) {
            StorageReference profilePicRef = mStorage.child("profile_pictures/" + userId + ".jpg");

            UploadTask uploadTask = profilePicRef.putFile(imageUri);
            uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return profilePicRef.getDownloadUrl();
            }).addOnCompleteListener(listener);
        }

        /**
         * Creates a new family in the Firebase database.
         *
         * @param familyName The name of the new family.
         * @param creatorId  The ID of the user creating the family.
         * @param listener   Listener for the completion of the operation, returns the family ID.
         */
        public void createNewFamily(String familyName, String creatorId, OnCompleteListener<String> listener) {
            String familyId = mDatabase.child("families").push().getKey();
            Family family = new Family(familyId, familyName, creatorId);

            mDatabase.child("families").child(familyId).setValue(family)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            addUserToFamily(creatorId, familyId, innerTask -> {
                                if (innerTask.isSuccessful()) {
                                    listener.onComplete(Tasks.forResult(familyId));
                                } else {
                                    listener.onComplete(Tasks.forException(innerTask.getException()));
                                }
                            });
                        } else {
                            listener.onComplete(Tasks.forException(task.getException()));
                        }
                    });
        }

        /**
         * Checks if a family exists in the Firebase database.
         *
         * @param familyId The ID of the family to check.
         * @param listener Listener for the result of the check.
         */
        public void checkFamilyExists(String familyId, final ValueEventListener listener) {
            mDatabase.child("families").child(familyId).addListenerForSingleValueEvent(listener);
        }

        /**
         * Adds a user to an existing family in the Firebase database.
         *
         * @param userId   The ID of the user to add.
         * @param familyId The ID of the family to add the user to.
         * @param listener Listener for the completion of the operation.
         */
        public void addUserToFamily(String userId, String familyId, final OnCompleteListener<Void> listener) {
            mDatabase.child("families").child(familyId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("/users/" + userId + "/familyId", familyId);
                        updates.put("/families/" + familyId + "/memberIds/" + userId, true);

                        mDatabase.updateChildren(updates).addOnCompleteListener(listener);
                    } else {
                        listener.onComplete(Tasks.forException(new Exception("Family does not exist")));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    listener.onComplete(Tasks.forException(databaseError.toException()));
                }
            });
        }

        /**
         * Retrieves a user by their phone number from the Firebase database.
         *
         * @param phoneNumber The phone number to search for.
         * @param listener    Listener for the result of the search.
         */
        public void getUserByPhoneNumber(String phoneNumber, final ValueEventListener listener) {
            mDatabase.child("users").orderByChild("phoneNumber").equalTo(phoneNumber).addListenerForSingleValueEvent(listener);
        }

        /**
         * Adds a user as an admin to a family in the Firebase database.
         *
         * @param userId   The ID of the user to add as an admin.
         * @param familyId The ID of the family.
         * @param listener Listener for the completion of the operation.
         */
        public void addAdminToFamily(String userId, String familyId, final OnCompleteListener<Void> listener) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("/families/" + familyId + "/adminIds/" + userId, true);

            mDatabase.updateChildren(updates).addOnCompleteListener(listener);
        }

        /**
         * Removes a user as an admin from a family in the Firebase database.
         *
         * @param userId   The ID of the user to remove as an admin.
         * @param familyId The ID of the family.
         * @param listener Listener for the completion of the operation.
         */
        public void removeAdminFromFamily(String userId, String familyId, final OnCompleteListener<Void> listener) {
            mDatabase.child("families").child(familyId).child("adminIds").child(userId).removeValue()
                    .addOnCompleteListener(listener);
        }

        /**
         * Creates a new parking event in the Firebase database.
         *
         * @param event    The ParkingEvent object to create.
         * @param listener Listener for the completion of the operation.
         */
        public void createParkingEvent(ParkingEvent event, OnCompleteListener<Void> listener) {
            String key = mDatabase.child("parkingEvents").push().getKey();
            event.setId(key);
            mDatabase.child("parkingEvents").child(key).setValue(event.toMap())
                    .addOnCompleteListener(listener);
        }

        /**
         * Updates the status of a parking event in the Firebase database.
         *
         * @param eventId  The ID of the event to update.
         * @param status   The new status of the event.
         * @param listener Listener for the completion of the operation.
         */
        public void updateParkingEventStatus(String eventId, String status, OnCompleteListener<Void> listener) {
            mDatabase.child("parkingEvents").child(eventId).child("status").setValue(status)
                    .addOnCompleteListener(listener);
        }

        /**
         * Retrieves a parking event by its ID from the Firebase database.
         *
         * @param eventId  The ID of the parking event to retrieve.
         * @param listener Listener for the result of the retrieval.
         */
        public void getParkingEvent(String eventId, ValueEventListener listener) {
            mDatabase.child("parkingEvents").child(eventId).addListenerForSingleValueEvent(listener);
        }

        /**
         * Deletes a family from the Firebase database.
         *
         * @param familyId The ID of the family to delete.
         * @param listener Listener for the completion of the operation.
         */
        public void deleteFamily(String familyId, OnCompleteListener<Void> listener) {
            mDatabase.child("families").child(familyId).removeValue().addOnCompleteListener(listener);
        }

        /**
         * Retrieves user data by their ID from the Firebase database.
         *
         * @param userId   The ID of the user to retrieve.
         * @param listener Listener for the result of the retrieval.
         */
        public void getUserData(String userId, final ValueEventListener listener) {
            mDatabase.child("users").child(userId).addListenerForSingleValueEvent(listener);
        }

        /**
         * Retrieves family data by its ID from the Firebase database.
         *
         * @param familyId The ID of the family to retrieve.
         * @param listener Listener for the result of the retrieval.
         */
        public void getFamilyData(String familyId, final ValueEventListener listener) {
            mDatabase.child("families").child(familyId).addListenerForSingleValueEvent(listener);
        }

        /**
         * Retrieves family members from the Firebase database using a ChildEventListener.
         *
         * @param familyId The ID of the family whose members to retrieve.
         * @param listener Listener for child events on the family members.
         */
        public void getFamilyMembers(String familyId, ChildEventListener listener) {
            mDatabase.child("families").child(familyId).child("memberIds").addChildEventListener(listener);
        }

        /**
         * Retrieves family members from the Firebase database using a ValueEventListener.
         *
         * @param familyId The ID of the family whose members to retrieve.
         * @param listener Listener for the result of the retrieval.
         */
        public void getFamilyMembersWithValueEventListener(String familyId, ValueEventListener listener) {
            mDatabase.child("families").child(familyId).child("memberIds").addListenerForSingleValueEvent(listener);
        }

        /**
         * Retrieves family members from the Firebase database using a ChildEventListener.
         *
         * @param familyId The ID of the family whose members to retrieve.
         * @param listener Listener for child events on the family members.
         */
        public void getFamilyMembersWithChildEventListener(String familyId, ChildEventListener listener) {
            mDatabase.child("families").child(familyId).child("memberIds").addChildEventListener(listener);
        }

        /**
         * Retrieves member data from the Firebase database using a ChildEventListener.
         *
         * @param userId   The ID of the member to retrieve.
         * @param listener Listener for child events on the member data.
         */
        public void getMemberDataWithChildListener(String userId, ChildEventListener listener) {
            mDatabase.child("users").child(userId).addChildEventListener(listener);
        }

        /**
         * Retrieves member data from the Firebase database using a ValueEventListener.
         *
         * @param userId   The ID of the member to retrieve.
         * @param listener Listener for the result of the retrieval.
         */
        public void getMemberData(String userId, final ValueEventListener listener) {
            mDatabase.child("users").child(userId).addValueEventListener(listener);
        }

        /**
         * Updates the location of a family member in the Firebase database.
         *
         * @param userId    The ID of the user whose location to update.
         * @param familyId  The ID of the family.
         * @param latitude  The latitude of the new location.
         * @param longitude The longitude of the new location.
         * @return A Task representing the completion of the operation.
         */
        public Task<Void> updateMemberLocation(String userId, String familyId, double latitude, double longitude) {
            Map<String, Object> locationUpdates = new HashMap<>();
            locationUpdates.put("latitude", latitude);
            locationUpdates.put("longitude", longitude);

            if (familyId != null) {
                return mDatabase.child("families").child(familyId).child("memberLocations").child(userId).setValue(locationUpdates)
                        .addOnSuccessListener(aVoid -> {
                            Intent intent = new Intent("com.example.tinyreminder.FAMILY_STATUS_CHANGED");
                            context.sendBroadcast(intent);
                        });
            } else {
                return Tasks.forException(new Exception("User is not in a family"));
            }
        }






        /**
         * Creates or updates a user in the Firebase database.
         *
         * @param user     The User object to be created or updated.
         * @param listener Listener for the completion of the operation.
         */
        public void createOrUpdateUser(User user, final OnCompleteListener<Void> listener) {
            mDatabase.child("users").child(user.getId()).setValue(user)
                    .addOnCompleteListener(listener);
        }

        /**
         * Updates the alert status of a user in the Firebase database.
         *
         * @param userId    The ID of the user to update.
         * @param isAlerted The new alert status of the user.
         * @return A Task representing the completion of the operation.
         */
        public Task<Void> updateUserAlertStatus(String userId, boolean isAlerted) {
            return mDatabase.child("users").child(userId).child("isAlerted").setValue(isAlerted)
                    .addOnSuccessListener(aVoid -> {
                        Intent intent = new Intent("com.example.tinyreminder.FAMILY_STATUS_CHANGED");
                        context.sendBroadcast(intent);
                    });
        }

        /**
         * Checks if a user is an admin in a family.
         *
         * @param userId   The ID of the user.
         * @param familyId The ID of the family.
         * @param listener Listener for the result of the check.
         */
        public void checkIfUserIsAdmin(String userId, String familyId, final ValueEventListener listener) {
            mDatabase.child("families").child(familyId).child("adminIds").child(userId)
                    .addListenerForSingleValueEvent(listener);
        }

        /**
         * Checks if a user has a phone number associated with their profile.
         *
         * @param userId   The ID of the user.
         * @param listener Listener for the result of the check.
         */
        public void checkUserHasPhoneNumber(String userId, final ValueEventListener listener) {
            mDatabase.child("users").child(userId).child("phoneNumber").addListenerForSingleValueEvent(listener);
        }

        /**
         * Removes a user from a family in the Firebase database.
         *
         * @param userId   The ID of the user to remove.
         * @param familyId The ID of the family.
         * @param listener Listener for the completion of the operation.
         */
        public void removeUserFromFamily(String userId, String familyId, final OnCompleteListener<Void> listener) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("/users/" + userId + "/familyId", null);
            updates.put("/families/" + familyId + "/memberIds/" + userId, null);
            updates.put("/families/" + familyId + "/adminIds/" + userId, null);

            mDatabase.updateChildren(updates).addOnCompleteListener(listener);
        }

        /**
         * Retrieves the locations of all family members from the Firebase database.
         *
         * @param familyId The ID of the family.
         * @param listener Listener for the result of the retrieval, returns a map of user IDs to LatLng objects.
         */
        public void getLocationsForFamily(String familyId, OnCompleteListener<Map<String, LatLng>> listener) {
            mDatabase.child("families").child(familyId).child("memberIds")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            Map<String, LatLng> familyLocations = new HashMap<>();
                            final long memberCount = dataSnapshot.getChildrenCount();
                            final AtomicLong completedCount = new AtomicLong(0);

                            for (DataSnapshot memberSnapshot : dataSnapshot.getChildren()) {
                                String memberId = memberSnapshot.getKey();
                                if (memberId != null) {
                                    mDatabase.child("users").child(memberId).addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                            Double lat = userSnapshot.child("latitude").getValue(Double.class);
                                            Double lng = userSnapshot.child("longitude").getValue(Double.class);
                                            if (lat != null && lng != null) {
                                                familyLocations.put(memberId, new LatLng(lat, lng));
                                            }
                                            if (completedCount.incrementAndGet() == memberCount) {
                                                listener.onComplete(Tasks.forResult(familyLocations));
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                            listener.onComplete(Tasks.forException(databaseError.toException()));
                                        }
                                    });
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            listener.onComplete(Tasks.forException(databaseError.toException()));
                        }
                    });
        }

        /**
         * Sets the status of a user in the Firebase database.
         *
         * @param userId The ID of the user.
         * @param status The new status of the user.
         * @return A Task representing the completion of the operation.
         */
        public Task<Void> setUserStatus(String userId, String status) {
            return mDatabase.child("users").child(userId).child("status").setValue(status)
                    .addOnSuccessListener(aVoid -> {
                        Intent intent = new Intent("com.example.tinyreminder.FAMILY_STATUS_CHANGED");
                        context.sendBroadcast(intent);
                    });
        }

        /**
         * Retrieves the status of a user from the Firebase database.
         *
         * @param userId   The ID of the user.
         * @param listener Listener for the result of the retrieval.
         */
        public void getUserStatus(String userId, final ValueEventListener listener) {
            mDatabase.child("users").child(userId).child("status").addValueEventListener(listener);
        }

        /**
         * Sets the last check-in time of a user in the Firebase database.
         *
         * @param userId    The ID of the user.
         * @param timestamp The timestamp of the last check-in.
         */
        public void setLastCheckInTime(String userId, long timestamp) {
            mDatabase.child("users").child(userId).child("lastCheckIn").setValue(timestamp);
        }

        /**
         * Retrieves the last check-in time of a user from the Firebase database.
         *
         * @param userId   The ID of the user.
         * @param listener Listener for the result of the retrieval.
         */
        public void getLastCheckInTime(String userId, final ValueEventListener listener) {
            mDatabase.child("users").child(userId).child("lastCheckIn").addListenerForSingleValueEvent(listener);
        }

        /**
         * Retrieves the real-time locations of all family members from the Firebase database.
         *
         * @param familyId The ID of the family.
         * @param listener Listener for the real-time updates of family members' locations.
         */
        public void getRealtimeLocationsForFamily(String familyId, ValueEventListener listener) {
            mDatabase.child("families").child(familyId).child("memberLocations").addValueEventListener(listener);
        }

        /**
         * Retrieves users by their phone number from the Firebase database.
         *
         * @param phoneNumber The phone number to search for.
         * @param listener    Listener for the result of the search.
         */
        public void getUsersByPhoneNumber(String phoneNumber, ValueEventListener listener) {
            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
            usersRef.orderByChild("phoneNumber").equalTo(phoneNumber).addListenerForSingleValueEvent(listener);
        }

        /**
         * Saves the FCM token of a user in the Firebase database.
         *
         * @param userId The ID of the user.
         * @param token  The FCM token to save.
         */
        public void saveFcmToken(String userId, String token) {
            mDatabase.child("users").child(userId).child("fcmToken").setValue(token)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "FCM token saved successfully");
                        } else {
                            Log.e(TAG, "Failed to save FCM token", task.getException());
                        }
                    });
        }

        /**
         * Retrieves the FCM token of a user from the Firebase database.
         *
         * @param userId   The ID of the user.
         * @param listener Listener for the result of the retrieval.
         */
        public void getFcmToken(String userId, ValueEventListener listener) {
            mDatabase.child("users").child(userId).child("fcmToken").addListenerForSingleValueEvent(listener);
        }

        /**
         * Sets the response flag for a notification in the Firebase database.
         *
         * @param userId         The ID of the user.
         * @param notificationId The ID of the notification.
         * @param hasResponded   The response flag to set.
         */
        public void setNotificationResponseFlag(String userId, int notificationId, boolean hasResponded) {
            mDatabase.child("users").child(userId).child("notifications").child(String.valueOf(notificationId)).child("hasResponded").setValue(hasResponded);
        }

        /**
         * Retrieves the response flag for a notification from the Firebase database.
         *
         * @param userId         The ID of the user.
         * @param notificationId The ID of the notification.
         * @param listener       Listener for the result of the retrieval.
         */
        public void getNotificationResponseFlag(String userId, int notificationId, ValueEventListener listener) {
            mDatabase.child("users").child(userId).child("notifications").child(String.valueOf(notificationId)).child("hasResponded").addListenerForSingleValueEvent(listener);
        }

        /**
         * Deletes a parking event from the Firebase database.
         *
         * @param eventId The ID of the parking event to delete.
         * @return A Task representing the completion of the operation.
         */
        public Task<Void> deleteParkingEvent(String eventId) {
            return mDatabase.child("parkingEvents").child(eventId).removeValue();
        }
    }