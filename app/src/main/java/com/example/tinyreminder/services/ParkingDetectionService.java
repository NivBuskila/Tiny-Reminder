package com.example.tinyreminder.services;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import com.google.android.gms.location.*;
import com.example.tinyreminder.utils.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import java.util.LinkedList;

public class ParkingDetectionService extends Service {
    private static final String TAG = "ParkingDetectionService";
    private static final float DRIVING_SPEED_THRESHOLD = 15f; // km/h
    private static final float PARKING_SPEED_THRESHOLD = 5f; // km/h
    private static final long PARKING_TIME_THRESHOLD = 120000; // 2 minutes in milliseconds
    private static final float MIN_DISTANCE_CHANGE = 10; // meters
    private static final int LOCATION_INTERVAL = 5000; // 5 seconds
    private static final float MS_TO_KMH = 3.6f;

    private enum VehicleState {
        PARKED,
        DRIVING,
        POTENTIAL_PARKING
    }

    private VehicleState currentState = VehicleState.PARKED;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location lastLocation;
    private long stationaryStartTime;
    private float lastSpeed = 0f;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ParkingDetectionService created");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createLocationCallback();
        startLocationUpdates();
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    processNewLocation(location);
                }
            }
        };
    }

    private void processNewLocation(Location location) {
        Log.d(TAG, "Received new location: " + location.getLatitude() + ", " + location.getLongitude());

        if (lastLocation == null) {
            lastLocation = location;
            return;
        }

        long timeDelta = (location.getTime() - lastLocation.getTime()) / 1000; // seconds
        float distance = lastLocation.distanceTo(location);
        float speed = (distance / timeDelta) * MS_TO_KMH; // km/h

        Log.d(TAG, "Current speed: " + speed + " km/h");
        Log.d(TAG, "Current state: " + currentState);

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

    private void sendParkingNotification() {
        String userId = getCurrentUserId();
        if (userId != null) {
            Log.d(TAG, "Sending parking notification for user: " + userId);
            NotificationHelper.sendParkingNotification(this, userId);
        } else {
            Log.e(TAG, "Failed to send notification: User ID is null");
        }
    }

    private String getCurrentUserId() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            return auth.getCurrentUser().getUid();
        }
        Log.w(TAG, "No current user found");
        return null;
    }

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
        fusedLocationClient.removeLocationUpdates(locationCallback);
        Log.d(TAG, "ParkingDetectionService destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}