package com.example.tinyreminder.utils;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.tinyreminder.MainActivity;
import com.example.tinyreminder.R;
import com.example.tinyreminder.models.User;
import com.example.tinyreminder.receivers.NotificationActionReceiver;
import com.example.tinyreminder.receivers.NotificationTimeoutReceiver;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "ParkingReminders";
    private static final String CHANNEL_NAME = "Parking Reminders";
    private static final String CHANNEL_DESCRIPTION = "Notifications for parking reminders";
    private static final long NOTIFICATION_TIMEOUT = 60 * 1000; // 1 minute in milliseconds

    /**
     * Sends a notification to the user to remind them about a parking event.
     * The notification includes options to confirm or cancel the event.
     */
    public static void sendParkingNotification(Context context, String userId, String eventId) {
        createNotificationChannel(context); // Ensure the notification channel is created
        Log.d(TAG, "Preparing to send parking notification for user: " + userId);
        int notificationId = (int) System.currentTimeMillis(); // Generate a unique notification ID

        // Create intent for the "Confirm" action in the notification
        Intent confirmIntent = new Intent(context, NotificationActionReceiver.class);
        confirmIntent.setAction("ACTION_CONFIRM");
        confirmIntent.putExtra("userId", userId);
        confirmIntent.putExtra("eventId", eventId);
        confirmIntent.putExtra("notificationId", notificationId);
        PendingIntent confirmPendingIntent = PendingIntent.getBroadcast(context, notificationId * 2, confirmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Create intent for the "Cancel" action in the notification
        Intent cancelIntent = new Intent(context, NotificationActionReceiver.class);
        cancelIntent.setAction("ACTION_CANCEL");
        cancelIntent.putExtra("userId", userId);
        cancelIntent.putExtra("eventId", eventId);
        cancelIntent.putExtra("notificationId", notificationId);
        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(context, notificationId * 2 + 1, cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Set notification icon
                .setContentTitle("Parking Reminder")
                .setContentText("Is the child still in the car?")
                .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority for urgent notifications
                .setCategory(NotificationCompat.CATEGORY_ALARM) // Alarm category for critical alerts
                .setAutoCancel(true) // Auto-cancel the notification when tapped
                .addAction(R.drawable.ic_check, "Child is present", confirmPendingIntent) // "Confirm" action
                .addAction(R.drawable.ic_close, "Child is not present", cancelPendingIntent); // "Cancel" action

        // Display the notification if the permission is granted
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(notificationId, builder.build());
            setNotificationTimeout(context, userId, eventId, notificationId); // Set timeout for the notification
        } else {
            Log.e(TAG, "Notification permission not granted");
        }
    }

    /**
     * Sets a timeout for the notification, triggering a timeout action if the user does not respond within the specified time.
     */
    private static void setNotificationTimeout(Context context, String userId, String eventId, int notificationId) {
        Intent intent = new Intent(context, NotificationTimeoutReceiver.class);
        intent.putExtra("userId", userId);
        intent.putExtra("eventId", eventId);
        intent.putExtra("notificationId", notificationId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, notificationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            // Set the exact alarm based on the Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + NOTIFICATION_TIMEOUT, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + NOTIFICATION_TIMEOUT, pendingIntent);
            }
        }
    }

    /**
     * Sends a notification to all family members except the specified user.
     * This is used to alert family members about potential issues.
     */
    public static void sendFamilyNotificationExceptUser(Context context, String familyId, String excludeUserId) {
        DatabaseManager dbManager = new DatabaseManager(context);
        dbManager.getFamilyMembersWithValueEventListener(familyId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Iterate through all family members
                for (DataSnapshot memberSnapshot : dataSnapshot.getChildren()) {
                    String memberId = memberSnapshot.getKey();
                    if (memberId != null && !memberId.equals(excludeUserId)) {
                        sendNotificationToMember(context, memberId, "Family Alert",
                                "A family member may have left a child in the car!");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to get family members: " + databaseError.getMessage());
            }
        });
    }

    /**
     * Sends a notification to a specific family member.
     */
    public static void sendNotificationToMember(Context context, String memberId, String title, String message) {
        DatabaseManager dbManager = new DatabaseManager(context);
        dbManager.getUserData(memberId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (user != null) {
                    showNotification(context, user.getId(), title, message);
                    dbManager.updateUserAlertStatus(user.getId(), true)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "Alert status updated for user: " + user.getId());
                                } else {
                                    Log.e(TAG, "Failed to update alert status for user: " + user.getId(), task.getException());
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error fetching user data: " + databaseError.getMessage());
            }
        });
    }

    /**
     * Displays a notification to a user.
     */
    private static void showNotification(Context context, String userId, String title, String message) {
        createNotificationChannel(context);
        int notificationId = (int) System.currentTimeMillis(); // Generate a unique notification ID

        // Create intent to open the main activity when the notification is tapped
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("userId", userId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Build and show the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true); // Auto-cancel the notification when tapped

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "Notification sent to user: " + userId + " with title: " + title);
        } else {
            Log.e(TAG, "Notification permission not granted for user: " + userId);
        }
    }

    /**
     * Sends a notification to a specific user.
     */
    private static void sendNotification(Context context, User user, String title, String message) {
        createNotificationChannel(context);
        int notificationId = (int) System.currentTimeMillis(); // Generate a unique notification ID

        // Create intent to open the main activity when the notification is tapped
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("userId", user.getId());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Build and show the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true); // Auto-cancel the notification when tapped

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "Notification sent to user: " + user.getId() + " with title: " + title);
        } else {
            Log.e(TAG, "Notification permission not granted for user: " + user.getId());
        }
    }

    /**
     * Creates the notification channel required for Android 8.0 (Oreo) and above.
     */
    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            } else {
                Log.e(TAG, "NotificationManager is null, can't create notification channel");
            }
        }
    }
}
