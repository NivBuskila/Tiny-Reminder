package com.example.tinyreminder.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
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
        int notificationId = intent.getIntExtra("notificationId", 0);

        if (userId == null) {
            Log.e(TAG, "Received null userId");
            return;
        }

        DatabaseManager dbManager = new DatabaseManager();
        dbManager.getUserStatus(userId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String status = dataSnapshot.getValue(String.class);
                if (status == null || "PENDING".equals(status)) {
                    sendNotificationToFamilyMembers(context, userId);
                } else {
                    Log.d(TAG, "User " + userId + " has already responded. Status: " + status);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error fetching user status: " + databaseError.getMessage());
            }
        });
    }

    private void sendNotificationToFamilyMembers(Context context, String userId) {
        DatabaseManager dbManager = new DatabaseManager();
        dbManager.getUserData(userId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (user != null && user.getFamilyId() != null) {
                    Log.d(TAG, "Sending family notification except user: " + userId);
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