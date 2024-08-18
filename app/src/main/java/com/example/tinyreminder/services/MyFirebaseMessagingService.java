package com.example.tinyreminder.services;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.example.tinyreminder.utils.NotificationHelper;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMessagingService";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String targetUserId = remoteMessage.getData().get("targetUserId");

        if (targetUserId != null && !targetUserId.equals(currentUserId)) {
            Log.d(TAG, "Ignoring message intended for different user");
            return;
        }
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if the message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleDataMessage(remoteMessage.getData());
        }

        // Check if the message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            handleNotificationMessage(remoteMessage.getNotification());
        }
    }

    private void handleDataMessage(Map<String, String> data) {
        String notificationType = data.get("notification_type");
        if ("parking_reminder".equals(notificationType)) {
            handleParkingReminder(data);
        } else {
            Log.d(TAG, "Unknown notification type: " + notificationType);
        }
    }

    private void handleParkingReminder(Map<String, String> data) {
        String userId = data.get("userId");
        String latitude = data.get("latitude");
        String longitude = data.get("longitude");

        if (userId != null && latitude != null && longitude != null) {
            showParkingReminderNotification(userId, latitude, longitude);
        } else {
            Log.e(TAG, "Invalid parking reminder data received");
        }
    }

    private void showParkingReminderNotification(String userId, String latitude, String longitude) {
        String title = "Parking Reminder";
        String message = "Did you remember to check your car? Location: " + latitude + ", " + longitude;

        // Check if it's the current user
        if (userId.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            NotificationHelper.sendParkingNotification(this, userId);
        } else {
            Log.d(TAG, "Received parking reminder for different user: " + userId);
        }
    }

    private void handleNotificationMessage(RemoteMessage.Notification notification) {
        String title = notification.getTitle();
        String body = notification.getBody();

        // Show notification only if it's the current user
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        NotificationHelper.sendNotificationToMember(this, currentUserId, title, body);
    }

    private void showNotification(String title, String body) {
        // Use NotificationHelper to show a general notification
        NotificationHelper.sendNotificationToMember(this, FirebaseAuth.getInstance().getCurrentUser().getUid(), title, body);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("fcmToken");
            databaseReference.setValue(token)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Token saved to database successfully");
                        } else {
                            Log.e(TAG, "Failed to save token to database", task.getException());
                        }
                    });
        } else {
            Log.w(TAG, "No user is currently signed in, token not saved");
        }
    }
}
