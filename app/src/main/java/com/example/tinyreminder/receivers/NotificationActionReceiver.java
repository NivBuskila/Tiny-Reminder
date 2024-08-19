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

        // Extract the action, userId, eventId, and notificationId from the intent
        String action = intent.getAction();
        String userId = intent.getStringExtra("userId");
        String eventId = intent.getStringExtra("eventId");
        int notificationId = intent.getIntExtra("notificationId", 0);

        Log.d(TAG, "Action: " + action + ", UserId: " + userId + ", EventId: " + eventId + ", NotificationId: " + notificationId);

        // Check if any of the critical data is null and log an error if so
        if (userId == null || action == null || eventId == null) {
            Log.e(TAG, "Received null userId, action, or eventId");
            return;
        }

        // Initialize the DatabaseManager for interacting with the database
        DatabaseManager dbManager = new DatabaseManager(context);

        // Handle the actions for confirming or canceling the event
        if ("ACTION_CONFIRM".equals(action) || "ACTION_CANCEL".equals(action)) {
            String newStatus = action.equals("ACTION_CONFIRM") ? "CHILD_PRESENT" : "CHILD_NOT_PRESENT";
            dbManager.updateParkingEventStatus(eventId, newStatus, task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Parking event status updated to: " + newStatus);
                    // Update the user's status and proceed to delete the parking event
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

    // Method to update the user's status to "OK" after the event action is handled
    private void updateUserStatus(Context context, DatabaseManager dbManager, String userId, String eventId, int notificationId, String eventStatus) {
        dbManager.setUserStatus(userId, "OK").addOnCompleteListener(statusTask -> {
            if (statusTask.isSuccessful()) {
                Log.d(TAG, "User status updated to OK");
                // After updating the status, delete the parking event
                deleteParkingEvent(context, dbManager, eventId, notificationId, eventStatus);
            } else {
                Log.e(TAG, "Failed to update user status", statusTask.getException());
                showToast(context, "Failed to update user status. Please check the app.");
            }
        });
    }

    // Method to delete the parking event from the database after handling the action
    private void deleteParkingEvent(Context context, DatabaseManager dbManager, String eventId, int notificationId, String eventStatus) {
        dbManager.deleteParkingEvent(eventId).addOnCompleteListener(deleteTask -> {
            if (deleteTask.isSuccessful()) {
                Log.d(TAG, "Parking event deleted successfully");
                // Cancel the notification related to the event
                cancelNotification(context, notificationId);
                showToast(context, eventStatus.equals("CHILD_PRESENT") ? "Child is present" : "Child is not present");
                // Notify the FamilyFragment if necessary
                notifyFamilyFragment(context);
            } else {
                Log.e(TAG, "Failed to delete parking event", deleteTask.getException());
                showToast(context, "Failed to delete parking event. Please check the app.");
            }
        });
    }

    // Method to notify the FamilyFragment (implementation needed based on app's structure)
    private void notifyFamilyFragment(Context context) {
        // Implementation goes here
    }

    // Method to cancel the notification after the event has been handled
    private void cancelNotification(Context context, int notificationId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
        Log.d(TAG, "Notification cancelled: " + notificationId);
    }

    // Method to show a toast message on the UI thread
    private void showToast(Context context, String message) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
        Log.d(TAG, "Toast shown: " + message);
    }
}
