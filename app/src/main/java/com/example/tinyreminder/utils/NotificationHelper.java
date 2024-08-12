package com.example.tinyreminder.utils;

import android.content.Context;

import com.example.tinyreminder.services.NotificationService;

public class NotificationHelper {
    private static final int FAMILY_UPDATE_NOTIFICATION_ID = 1;
    private static final int LOCATION_UPDATE_NOTIFICATION_ID = 2;

    private NotificationService notificationService;

    public NotificationHelper(Context context) {
        notificationService = new NotificationService(context);
    }

    public void showFamilyUpdateNotification(String message) {
        notificationService.showNotification("Family Update", message, FAMILY_UPDATE_NOTIFICATION_ID);
    }

    public void showLocationUpdateNotification(String message) {
        notificationService.showNotification("Location Update", message, LOCATION_UPDATE_NOTIFICATION_ID);
    }
}