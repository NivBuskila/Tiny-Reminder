package com.example.tinyreminder.services;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.Nullable;
import com.google.android.gms.location.*;
import com.example.tinyreminder.utils.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;

public class ParkingDetectionService extends Service {
    private static final float PARKING_SPEED_THRESHOLD = 5f; // km/h
    private static final long PARKING_TIME_THRESHOLD = 60000; // 1 minute in milliseconds
    private static final float TRIP_START_SPEED_THRESHOLD = 10f; // km/h

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location lastLocation;
    private long stationaryStartTime;
    private boolean isOnTrip = false;

    @Override
    public void onCreate() {
        super.onCreate();
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
        if (lastLocation == null) {
            lastLocation = location;
            stationaryStartTime = System.currentTimeMillis();
            return;
        }

        float speed = location.getSpeed() * 3.6f; // Convert m/s to km/h

        if (speed < PARKING_SPEED_THRESHOLD) {
            if (isOnTrip && System.currentTimeMillis() - stationaryStartTime > PARKING_TIME_THRESHOLD) {
                // Trip ended
                isOnTrip = false;
                sendParkingNotification();
            }
        } else if (speed > TRIP_START_SPEED_THRESHOLD) {
            if (!isOnTrip) {
                // Trip started
                isOnTrip = true;
                // You might want to log or handle trip start here
            }
            stationaryStartTime = System.currentTimeMillis();
        }

        lastLocation = location;
    }

    private void sendParkingNotification() {
        String userId = getCurrentUserId();
        if (userId != null) {
            NotificationHelper.sendParkingNotification(this, userId);
        }
    }

    private String getCurrentUserId() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            return auth.getCurrentUser().getUid();
        }
        return null;
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(10000)
                .setFastestInterval(5000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}