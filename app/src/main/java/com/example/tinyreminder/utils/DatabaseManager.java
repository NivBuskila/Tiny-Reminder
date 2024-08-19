    package com.example.tinyreminder.utils;

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

        public DatabaseManager() {
            mDatabase = FirebaseDatabase.getInstance().getReference();
            mStorage = FirebaseStorage.getInstance().getReference();
        }

        public void createUser(User user, final OnCompleteListener<Void> listener) {
            mDatabase.child("users").child(user.getId()).setValue(user)
                    .addOnCompleteListener(listener);
        }

        public void updateUserProfile(String userId, Map<String, Object> updates, final OnCompleteListener<Void> listener) {
            mDatabase.child("users").child(userId).updateChildren(updates)
                    .addOnCompleteListener(listener);
        }

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

        public void checkFamilyExists(String familyId, final ValueEventListener listener) {
            mDatabase.child("families").child(familyId).addListenerForSingleValueEvent(listener);
        }

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

        public void getUserByPhoneNumber(String phoneNumber, final ValueEventListener listener) {
            mDatabase.child("users").orderByChild("phoneNumber").equalTo(phoneNumber).addListenerForSingleValueEvent(listener);
        }

        public void addAdminToFamily(String userId, String familyId, final OnCompleteListener<Void> listener) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("/families/" + familyId + "/adminIds/" + userId, true);

            mDatabase.updateChildren(updates).addOnCompleteListener(listener);
        }

        public void removeAdminFromFamily(String userId, String familyId, final OnCompleteListener<Void> listener) {
            mDatabase.child("families").child(familyId).child("adminIds").child(userId).removeValue()
                    .addOnCompleteListener(listener);
        }

        public void createParkingEvent(ParkingEvent event, OnCompleteListener<Void> listener) {
            String key = mDatabase.child("parkingEvents").push().getKey();
            event.setId(key);
            mDatabase.child("parkingEvents").child(key).setValue(event.toMap())
                    .addOnCompleteListener(listener);
        }

        public void updateParkingEventStatus(String eventId, String status, OnCompleteListener<Void> listener) {
            mDatabase.child("parkingEvents").child(eventId).child("status").setValue(status)
                    .addOnCompleteListener(listener);
        }

        public void getParkingEvent(String eventId, ValueEventListener listener) {
            mDatabase.child("parkingEvents").child(eventId).addListenerForSingleValueEvent(listener);
        }

        public void deleteFamily(String familyId, OnCompleteListener<Void> listener) {
            mDatabase.child("families").child(familyId).removeValue().addOnCompleteListener(listener);
        }

        public void getUserData(String userId, final ValueEventListener listener) {
            mDatabase.child("users").child(userId).addListenerForSingleValueEvent(listener);
        }

        public void getFamilyData(String familyId, final ValueEventListener listener) {
            mDatabase.child("families").child(familyId).addListenerForSingleValueEvent(listener);
        }

        public void getFamilyMembers(String familyId, final ValueEventListener listener) {
            mDatabase.child("families").child(familyId).child("memberIds").addValueEventListener(listener);
        }

        public void getMemberData(String userId, final ValueEventListener listener) {
            mDatabase.child("users").child(userId).addListenerForSingleValueEvent(listener);
        }

        public Task<Void> updateMemberLocation(String userId, String familyId, double latitude, double longitude) {
            Map<String, Object> locationUpdates = new HashMap<>();
            locationUpdates.put("latitude", latitude);
            locationUpdates.put("longitude", longitude);

            if (familyId != null) {
                return mDatabase.child("families").child(familyId).child("memberLocations").child(userId).setValue(locationUpdates);
            } else {
                return Tasks.forException(new Exception("User is not in a family"));
            }
        }




        public void createOrUpdateUser(User user, final OnCompleteListener<Void> listener) {
            mDatabase.child("users").child(user.getId()).setValue(user)
                    .addOnCompleteListener(listener);
        }
        public Task<Void> updateUserAlertStatus(String userId, boolean isAlerted) {
            return mDatabase.child("users").child(userId).child("isAlerted").setValue(isAlerted);
        }
        public void checkIfUserIsAdmin(String userId, String familyId, final ValueEventListener listener) {
            mDatabase.child("families").child(familyId).child("adminIds").child(userId)
                    .addListenerForSingleValueEvent(listener);
        }

        public void checkUserHasPhoneNumber(String userId, final ValueEventListener listener) {
            mDatabase.child("users").child(userId).child("phoneNumber").addListenerForSingleValueEvent(listener);
        }

        public void removeUserFromFamily(String userId, String familyId, final OnCompleteListener<Void> listener) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("/users/" + userId + "/familyId", null);
            updates.put("/families/" + familyId + "/memberIds/" + userId, null);
            updates.put("/families/" + familyId + "/adminIds/" + userId, null);

            mDatabase.updateChildren(updates).addOnCompleteListener(listener);
        }

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




        public Task<Void> setUserStatus(String userId, String status) {
            return mDatabase.child("users").child(userId).child("status").setValue(status);
        }

        public void getUserStatus(String userId, final ValueEventListener listener) {
            mDatabase.child("users").child(userId).child("status").addValueEventListener(listener);
        }

        public void setLastCheckInTime(String userId, long timestamp) {
            mDatabase.child("users").child(userId).child("lastCheckIn").setValue(timestamp);
        }

        public void getLastCheckInTime(String userId, final ValueEventListener listener) {
            mDatabase.child("users").child(userId).child("lastCheckIn").addListenerForSingleValueEvent(listener);
        }
        public void getRealtimeLocationsForFamily(String familyId, ValueEventListener listener) {
            mDatabase.child("families").child(familyId).child("memberLocations").addValueEventListener(listener);
        }

        public void getUsersByPhoneNumber(String phoneNumber, ValueEventListener listener) {
            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
            usersRef.orderByChild("phoneNumber").equalTo(phoneNumber).addListenerForSingleValueEvent(listener);
        }

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

        public void getFcmToken(String userId, ValueEventListener listener) {
            mDatabase.child("users").child(userId).child("fcmToken").addListenerForSingleValueEvent(listener);
        }

        public void setNotificationResponseFlag(String userId, int notificationId, boolean hasResponded) {
            mDatabase.child("users").child(userId).child("notifications").child(String.valueOf(notificationId)).child("hasResponded").setValue(hasResponded);
        }

        public void getNotificationResponseFlag(String userId, int notificationId, ValueEventListener listener) {
            mDatabase.child("users").child(userId).child("notifications").child(String.valueOf(notificationId)).child("hasResponded").addListenerForSingleValueEvent(listener);
        }

        public Task<Void> deleteParkingEvent(String eventId) {
            return mDatabase.child("parkingEvents").child(eventId).removeValue();
        }
    }