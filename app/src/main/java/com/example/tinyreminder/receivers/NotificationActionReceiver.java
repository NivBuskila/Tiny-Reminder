package com.example.tinyreminder.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.tinyreminder.utils.DatabaseManager;
import com.example.tinyreminder.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class NotificationActionReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationActionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String userId = intent.getStringExtra("userId");
        int notificationId = intent.getIntExtra("notificationId", 0);

        if (userId == null || action == null) {
            Log.e(TAG, "Received null userId or action");
            return;
        }

        DatabaseManager dbManager = new DatabaseManager();

        if ("ACTION_CONFIRM".equals(action)) {
            dbManager.setUserStatus(userId, "OK");
        } else if ("ACTION_CANCEL".equals(action)) {
            dbManager.setUserStatus(userId, "ALERT");
        }

        dbManager.setLastCheckInTime(userId, System.currentTimeMillis());
        dbManager.setNotificationResponseFlag(userId, notificationId, true);

        // Cancel any pending notifications for this user
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
    }

}