package com.example.tinyreminder.utils;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.example.tinyreminder.R;
import com.example.tinyreminder.receivers.NotificationTimeoutReceiver;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

public class NotificationHelper {
    private static final String CHANNEL_ID = "ParkingReminders";
    private static final String CHANNEL_NAME = "Parking Reminders";
    private static final String CHANNEL_DESCRIPTION = "Notifications for parking reminders";
    private static final long NOTIFICATION_TIMEOUT = 5 * 60 * 1000; // 5 minutes in milliseconds

    public static void sendParkingNotification(Context context, String userId) {
        createNotificationChannel(context);

        int notificationId = (int) System.currentTimeMillis();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Parking Reminder")
                .setContentText("Is the child still in the car?")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, builder.build());

        // Send FCM message to the user's device
        FirebaseMessaging.getInstance().send(new RemoteMessage.Builder(userId + "@fcm.googleapis.com")
                .setMessageId(Integer.toString(notificationId))
                .addData("title", "Parking Reminder")
                .addData("body", "Is the child still in the car?")
                .build());

        // Set up timeout for notification response
        setNotificationTimeout(context, userId, notificationId);
    }

    private static void setNotificationTimeout(Context context, String userId, int notificationId) {
        Intent intent = new Intent(context, NotificationTimeoutReceiver.class);
        intent.putExtra("userId", userId);
        intent.putExtra("notificationId", notificationId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + NOTIFICATION_TIMEOUT, pendingIntent);
    }

    public static void sendFamilyNotification(Context context, String familyId) {
        DatabaseManager dbManager = new DatabaseManager();
        dbManager.getFamilyMembers(familyId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot memberSnapshot : dataSnapshot.getChildren()) {
                    String memberId = memberSnapshot.getKey();
                    if (memberId != null) {
                        sendNotificationToMember(context, memberId);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("NotificationHelper", "Failed to get family members: " + databaseError.getMessage());
            }
        });
    }

    private static void sendNotificationToMember(Context context, String memberId) {
        createNotificationChannel(context);

        int notificationId = (int) System.currentTimeMillis();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Family Alert")
                .setContentText("A family member may have left a child in the car!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, builder.build());

        // Send FCM message to the family member's device
        FirebaseMessaging.getInstance().send(new RemoteMessage.Builder(memberId + "@fcm.googleapis.com")
                .setMessageId(Integer.toString(notificationId))
                .addData("title", "Family Alert")
                .addData("body", "A family member may have left a child in the car!")
                .build());
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(CHANNEL_DESCRIPTION);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}