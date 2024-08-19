package com.example.tinyreminder.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.google.android.gms.tasks.Task;

import android.widget.Toast;

import com.example.tinyreminder.utils.DatabaseManager;

public class NotificationActionReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationActionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "NotificationActionReceiver onReceive called");

        String action = intent.getAction();
        String userId = intent.getStringExtra("userId");
        String eventId = intent.getStringExtra("eventId");
        int notificationId = intent.getIntExtra("notificationId", 0);

        Log.d(TAG, "Action: " + action + ", UserId: " + userId + ", EventId: " + eventId + ", NotificationId: " + notificationId);

        if (userId == null || action == null || eventId == null) {
            Log.e(TAG, "Received null userId, action, or eventId");
            return;
        }

        DatabaseManager dbManager = new DatabaseManager(context);

        if ("ACTION_CONFIRM".equals(action) || "ACTION_CANCEL".equals(action)) {
            String newStatus = action.equals("ACTION_CONFIRM") ? "CHILD_PRESENT" : "CHILD_NOT_PRESENT";
            dbManager.updateParkingEventStatus(eventId, newStatus, task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Parking event status updated to: " + newStatus);
                    updateUserStatus(context, dbManager, userId, eventId, notificationId, newStatus);
                } else {
                    Log.e(TAG, "Failed to update parking event status", task.getException());
                    showToast(context, "Failed to update event status. Please try again.");
                }
            });
        } else {
            Log.w(TAG, "Received unknown action: " + action);
        }
    }


    private void updateUserStatus(Context context, DatabaseManager dbManager, String userId, String eventId, int notificationId, String eventStatus) {
        dbManager.setUserStatus(userId, "OK").addOnCompleteListener(statusTask -> {
            if (statusTask.isSuccessful()) {
                Log.d(TAG, "User status updated to OK");
                deleteParkingEvent(context, dbManager, eventId, notificationId, eventStatus);
            } else {
                Log.e(TAG, "Failed to update user status", statusTask.getException());
                showToast(context, "Failed to update user status. Please check the app.");
            }
        });
    }

    private void deleteParkingEvent(Context context, DatabaseManager dbManager, String eventId, int notificationId, String eventStatus) {
        dbManager.deleteParkingEvent(eventId).addOnCompleteListener(deleteTask -> {
            if (deleteTask.isSuccessful()) {
                Log.d(TAG, "Parking event deleted successfully");
                cancelNotification(context, notificationId);
                showToast(context, eventStatus.equals("CHILD_PRESENT") ? "Child is present" : "Child is not present");
                notifyFamilyFragment(context);
            } else {
                Log.e(TAG, "Failed to delete parking event", deleteTask.getException());
                showToast(context, "Failed to delete parking event. Please check the app.");
            }
        });
    }

    private void notifyFamilyFragment(Context context) {
    }

    private void cancelNotification(Context context, int notificationId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
        Log.d(TAG, "Notification cancelled: " + notificationId);
    }

    private void showToast(Context context, String message) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
        Log.d(TAG, "Toast shown: " + message);
    }
}
