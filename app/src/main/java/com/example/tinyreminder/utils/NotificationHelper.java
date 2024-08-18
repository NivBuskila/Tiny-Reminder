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
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.tinyreminder.MainActivity;
import com.example.tinyreminder.R;
import com.example.tinyreminder.receivers.NotificationActionReceiver;
import com.example.tinyreminder.receivers.NotificationTimeoutReceiver;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "ParkingReminders";
    private static final String CHANNEL_NAME = "Parking Reminders";
    private static final String CHANNEL_DESCRIPTION = "Notifications for parking reminders";
    private static final long NOTIFICATION_TIMEOUT = 60 * 1000; // 1 minutes in milliseconds

    public static void sendParkingNotification(Context context, String userId) {
        if (context == null || userId == null || userId.isEmpty()) {
            Log.e(TAG, "Invalid context or userId for sending parking notification");
            return;
        }

        DatabaseManager dbManager = new DatabaseManager();
        dbManager.getUserStatus(userId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String status = dataSnapshot.getValue(String.class);
                if (status == null || "PENDING".equals(status)) {
                    createNotificationChannel(context);
                    Log.d(TAG, "Preparing to send parking notification for user: " + userId);
                    int notificationId = (int) System.currentTimeMillis();

                    Intent confirmIntent = new Intent(context, NotificationActionReceiver.class);
                    confirmIntent.setAction("ACTION_CONFIRM");
                    confirmIntent.putExtra("userId", userId);
                    confirmIntent.putExtra("notificationId", notificationId);
                    PendingIntent confirmPendingIntent = PendingIntent.getBroadcast(context, 0, confirmIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                    Intent cancelIntent = new Intent(context, NotificationActionReceiver.class);
                    cancelIntent.setAction("ACTION_CANCEL");
                    cancelIntent.putExtra("userId", userId);
                    cancelIntent.putExtra("notificationId", notificationId);
                    PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(context, 1, cancelIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentTitle("Parking Reminder")
                            .setContentText("Is the child still in the car?")
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setCategory(NotificationCompat.CATEGORY_ALARM)
                            .setAutoCancel(true)
                            .addAction(R.drawable.ic_check, "Yes", confirmPendingIntent)
                            .addAction(R.drawable.ic_close, "No", cancelPendingIntent);

                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        Log.e(TAG, "Notification permission not granted");
                        return;
                    }
                    notificationManager.notify(notificationId, builder.build());

                    setNotificationTimeout(context, userId, notificationId);

                    // Update user status to PENDING
                    dbManager.setUserStatus(userId, "PENDING");
                } else {
                    Log.d(TAG, "User " + userId + " has already responded or is in " + status + " state. Skipping notification.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error fetching user status: " + databaseError.getMessage());
            }
        });
    }

    private static void setNotificationTimeout(Context context, String userId, int notificationId) {
        Intent intent = new Intent(context, NotificationTimeoutReceiver.class);
        intent.putExtra("userId", userId);
        intent.putExtra("notificationId", notificationId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, notificationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                            System.currentTimeMillis() + NOTIFICATION_TIMEOUT, pendingIntent);
                } else {
                    // Handle the case where the app doesn't have permission
                    Log.e(TAG, "App doesn't have permission to schedule exact alarms");
                    // You might want to use setAndAllowWhileIdle instead, which is inexact but doesn't require special permissions
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                            System.currentTimeMillis() + NOTIFICATION_TIMEOUT, pendingIntent);
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + NOTIFICATION_TIMEOUT, pendingIntent);
            }
        } else {
            Log.e(TAG, "AlarmManager is null, can't set notification timeout");
        }
    }

    public static void sendFamilyNotificationExceptUser(Context context, String familyId, String excludeUserId) {
        if (context == null || TextUtils.isEmpty(familyId) || TextUtils.isEmpty(excludeUserId)) {
            Log.e(TAG, "Invalid parameters for sendFamilyNotificationExceptUser");
            return;
        }

        DatabaseManager dbManager = new DatabaseManager();
        dbManager.getFamilyMembers(familyId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "Fetching family members for familyId: " + familyId);
                for (DataSnapshot memberSnapshot : dataSnapshot.getChildren()) {
                    String memberId = memberSnapshot.getKey();
                    if (memberId != null && !memberId.equals(excludeUserId)) {
                        Log.d(TAG, "Preparing to send notification to member: " + memberId);
                        sendNotificationToMember(context, memberId, "Family Alert",
                                "A family member may have left a child in the car!");
                    } else {
                        Log.d(TAG, "Skipping notification for excluded user: " + excludeUserId);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to get family members: " + databaseError.getMessage());
            }
        });
    }

    private static void checkUserStatusAndSendNotification(Context context, String memberId, String excludeUserId) {
        DatabaseManager dbManager = new DatabaseManager();
        dbManager.getUserStatus(memberId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String status = dataSnapshot.getValue(String.class);
                if (!"OK".equals(status) && !"ALERT".equals(status)) {
                    Log.d(TAG, "Sending notification to member: " + memberId + ", excluding user: " + excludeUserId);
                    sendNotificationToMember(context, memberId, "Family Alert",
                            "A family member may have left a child in the car!");
                } else {
                    Log.d(TAG, "Skipping notification for member: " + memberId + " with status: " + status);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to get user status: " + databaseError.getMessage());
            }
        });
    }



    public static void sendNotificationToMember(Context context, String memberId, String title, String message) {
        if (context == null || TextUtils.isEmpty(memberId) || TextUtils.isEmpty(title) || TextUtils.isEmpty(message)) {
            Log.e(TAG, "Invalid parameters for sendNotificationToMember");
            return;
        }

        DatabaseManager dbManager = new DatabaseManager();
        dbManager.getUserStatus(memberId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String status = dataSnapshot.getValue(String.class);
                if (!"OK".equals(status) && !"ALERT".equals(status)) {
                    createNotificationChannel(context);
                    int notificationId = (int) System.currentTimeMillis();

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentTitle(title)
                            .setContentText(message)
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setAutoCancel(true);

                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                        notificationManager.notify(notificationId, builder.build());
                        Log.d(TAG, "Notification sent to memberId: " + memberId + " with title: " + title);
                        sendFCMMessage(memberId, notificationId, title, message);
                    } else {
                        Log.w(TAG, "Notification permission not granted for memberId: " + memberId);
                    }
                } else {
                    Log.d(TAG, "Skipping notification for user " + memberId + " with status " + status);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error fetching user status: " + databaseError.getMessage());
            }
        });
    }

    private static void sendFCMMessage(String userId, int notificationId, String title, String body) {
        try {
            FirebaseMessaging.getInstance().send(new RemoteMessage.Builder(userId + "@fcm.googleapis.com")
                    .setMessageId(Integer.toString(notificationId))
                    .addData("title", title)
                    .addData("body", body)
                    .build());
            Log.d(TAG, "FCM message sent successfully to user: " + userId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send FCM message to user: " + userId, e);
        }
    }




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