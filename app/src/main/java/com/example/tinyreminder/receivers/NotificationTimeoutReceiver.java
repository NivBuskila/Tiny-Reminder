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

        // Check if the notification has been responded to
        if (!hasUserRespondedToNotification(userId, notificationId)) {
            // If not, send notification to family members
            DatabaseManager dbManager = new DatabaseManager();
            dbManager.getUserData(userId, new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null && user.getFamilyId() != null) {
                        NotificationHelper.sendFamilyNotification(context, user.getFamilyId());
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

    private boolean hasUserRespondedToNotification(String userId, int notificationId) {
        // TODO: Implement method to check if user has responded to the notification
        // This could involve checking a flag in the database
        return false;
    }
}