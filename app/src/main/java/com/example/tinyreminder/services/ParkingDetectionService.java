package com.example.tinyreminder.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.tinyreminder.MainActivity;
import com.example.tinyreminder.R;
import com.example.tinyreminder.models.ParkingEvent;
import com.example.tinyreminder.utils.DatabaseManager;
import com.example.tinyreminder.utils.NotificationHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;

public class ParkingDetectionService extends Service {
    private static final String TAG = "ParkingDetectionService";
    private static final float DRIVING_SPEED_THRESHOLD = 15f; // km/h
    private static final float PARKING_SPEED_THRESHOLD = 5f; // km/h
    private static final long PARKING_TIME_THRESHOLD = 120000; // 2 minutes in milliseconds
    private static final float MIN_DISTANCE_CHANGE = 10; // meters
    private static final int LOCATION_INTERVAL = 5000; // 5 seconds
    private static final float MS_TO_KMH = 3.6f; // Conversion from m/s to km/h
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "LocationServiceChannel";

    // Enum to represent the state of the vehicle
    private enum VehicleState {
        PARKED,
        DRIVING,
        POTENTIAL_PARKING
    }

    private VehicleState currentState = VehicleState.PARKED; // Initial state
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location lastLocation;
    private long stationaryStartTime;
    private float lastSpeed = 0f;

    private DatabaseManager dbManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ParkingDetectionService onCreate called");

        // Create a notification channel for foreground service
        createNotificationChannel();
        dbManager = new DatabaseManager(this);

        // Build the notification and start the service in the foreground
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        }

        // Initialize the fused location provider and start location updates
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createLocationCallback();
        startLocationUpdates();
    }

    // Method to create a notification channel
    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Location Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    // Method to build the notification for the foreground service
    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Tracking")
                .setContentText("Tracking your location")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .build();
    }

    // Method to create a location callback to handle location updates
    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                // Process each new location update
                for (Location location : locationResult.getLocations()) {
                    processNewLocation(location);
                }
            }
        };
    }

    // Method to process a new location update
    private void processNewLocation(Location location) {
        Log.d(TAG, "Received new location: " + location.getLatitude() + ", " + location.getLongitude());

        if (lastLocation == null) {
            lastLocation = location;
            return;
        }

        // Calculate the speed based on the distance and time between the last and current location
        long timeDelta = (location.getTime() - lastLocation.getTime()) / 1000; // seconds
        float distance = lastLocation.distanceTo(location);
        float speed = (distance / timeDelta) * MS_TO_KMH; // km/h

        Log.d(TAG, "Current speed: " + speed + " km/h");
        Log.d(TAG, "Current state: " + currentState);

        // Update the state of the vehicle based on the speed
        switch (currentState) {
            case PARKED:
                if (speed > DRIVING_SPEED_THRESHOLD) {
                    currentState = VehicleState.DRIVING;
                    Log.d(TAG, "State changed to DRIVING");
                }
                break;

            case DRIVING:
                if (speed < PARKING_SPEED_THRESHOLD) {
                    currentState = VehicleState.POTENTIAL_PARKING;
                    stationaryStartTime = System.currentTimeMillis();
                    Log.d(TAG, "State changed to POTENTIAL_PARKING");
                }
                break;

            case POTENTIAL_PARKING:
                if (speed > DRIVING_SPEED_THRESHOLD) {
                    currentState = VehicleState.DRIVING;
                    Log.d(TAG, "State changed back to DRIVING");
                } else if (isParkingDetected(location, speed)) {
                    currentState = VehicleState.PARKED;
                    sendParkingNotification();
                    Log.d(TAG, "Parking detected! State changed to PARKED");
                }
                break;
        }

        lastLocation = location;
        lastSpeed = speed;
    }

    // Method to check if the parking conditions are met
    private boolean isParkingDetected(Location location, float speed) {
        long currentTime = System.currentTimeMillis();
        long stationaryDuration = currentTime - stationaryStartTime;
        float distanceMoved = lastLocation.distanceTo(location);

        boolean isSpeedLow = speed < PARKING_SPEED_THRESHOLD;
        boolean isStationaryLongEnough = stationaryDuration > PARKING_TIME_THRESHOLD;
        boolean hasNotMovedMuch = distanceMoved < MIN_DISTANCE_CHANGE;

        Log.d(TAG, "Checking parking conditions:");
        Log.d(TAG, "  Speed: " + speed + " km/h (Threshold: " + PARKING_SPEED_THRESHOLD + " km/h)");
        Log.d(TAG, "  Stationary duration: " + stationaryDuration + " ms (Threshold: " + PARKING_TIME_THRESHOLD + " ms)");
        Log.d(TAG, "  Distance moved: " + distanceMoved + " meters (Threshold: " + MIN_DISTANCE_CHANGE + " meters)");

        return isSpeedLow && isStationaryLongEnough && hasNotMovedMuch;
    }

    // Method to send a parking notification and create a parking event in the database
    private void sendParkingNotification() {
        String userId = getCurrentUserId();
        if (userId != null) {
            Log.d(TAG, "Creating parking event for user: " + userId);
            ParkingEvent parkingEvent = new ParkingEvent(userId, System.currentTimeMillis(), lastLocation.getLatitude(), lastLocation.getLongitude());
            dbManager.createParkingEvent(parkingEvent, task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Parking event created successfully");
                    // Update user status to PENDING
                    dbManager.setUserStatus(userId, "PENDING").addOnCompleteListener(statusTask -> {
                        if (statusTask.isSuccessful()) {
                            Log.d(TAG, "User status updated to PENDING");
                            NotificationHelper.sendParkingNotification(this, userId, parkingEvent.getId());
                        } else {
                            Log.e(TAG, "Failed to update user status", statusTask.getException());
                        }
                    });
                } else {
                    Log.e(TAG, "Failed to create parking event", task.getException());
                }
            });
        } else {
            Log.e(TAG, "Failed to create parking event: User ID is null");
        }
    }

    // Helper method to get the current user's ID
    private String getCurrentUserId() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            return auth.getCurrentUser().getUid();
        }
        Log.w(TAG, "No current user found");
        return null;
    }

    // Method to start location updates
    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_INTERVAL)
                .setMinUpdateIntervalMillis(LOCATION_INTERVAL)
                .build();
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());

            Log.d(TAG, "Location updates started");
        } catch (SecurityException e) {
            Log.e(TAG, "Error starting location updates", e);
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop location updates when the service is destroyed
        fusedLocationClient.removeLocationUpdates(locationCallback);
        Log.d(TAG, "ParkingDetectionService destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // This service is not designed for binding
    }
}
