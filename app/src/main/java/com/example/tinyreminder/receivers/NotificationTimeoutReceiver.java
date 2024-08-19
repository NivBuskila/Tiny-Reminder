    package com.example.tinyreminder.receivers;

    import android.content.BroadcastReceiver;
    import android.content.Context;
    import android.content.Intent;
    import android.util.Log;
    import androidx.annotation.NonNull;

    import com.example.tinyreminder.models.ParkingEvent;
    import com.example.tinyreminder.utils.NotificationHelper;
    import com.example.tinyreminder.utils.DatabaseManager;
    import com.example.tinyreminder.models.User;
    import com.google.firebase.database.DataSnapshot;
    import com.google.firebase.database.DatabaseError;
    import com.google.firebase.database.ValueEventListener;

    public class NotificationTimeoutReceiver extends BroadcastReceiver {
        private static final String TAG = "NotificationTimeoutReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            String userId = intent.getStringExtra("userId");
            String eventId = intent.getStringExtra("eventId");

            if (userId == null || eventId == null) {
                Log.e(TAG, "Received null userId or eventId");
                return;
            }

            DatabaseManager dbManager = new DatabaseManager(context);
            dbManager.getParkingEvent(eventId, new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    ParkingEvent event = dataSnapshot.getValue(ParkingEvent.class);
                    if (event != null && "PENDING".equals(event.getStatus())) {
                        updateToAlertStatus(context, dbManager, userId, eventId);
                    } else {
                        Log.d(TAG, "Parking event " + eventId + " is not pending. Status: " + (event != null ? event.getStatus() : "null"));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Error fetching parking event: " + databaseError.getMessage());
                }
            });
        }

        private void updateToAlertStatus(Context context, DatabaseManager dbManager, String userId, String eventId) {
            dbManager.updateParkingEventStatus(eventId, "ALERT", task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Parking event status updated to ALERT");
                    dbManager.setUserStatus(userId, "ALERT").addOnCompleteListener(statusTask -> {
                        if (statusTask.isSuccessful()) {
                            Log.d(TAG, "User status updated to ALERT");
                            notifyFamilyMembers(context, dbManager, userId);
                        } else {
                            Log.e(TAG, "Failed to update user status", statusTask.getException());
                        }
                    });
                } else {
                    Log.e(TAG, "Failed to update parking event status", task.getException());
                }
            });
        }

        private void notifyFamilyMembers(Context context, DatabaseManager dbManager, String userId) {
            dbManager.getUserData(userId, new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null && user.getFamilyId() != null) {
                        NotificationHelper.sendFamilyNotificationExceptUser(context, user.getFamilyId(), userId);
                    } else {
                        Log.e(TAG, "User or family ID is null");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Error fetching user data: " + databaseError.getMessage());
                }
            });
        }
    }