package com.example.tinyreminder.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.tinyreminder.R;
import com.example.tinyreminder.utils.DatabaseManager;
import com.example.tinyreminder.utils.LocationCache;
import com.example.tinyreminder.utils.NotificationHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;

public class LocationUpdateService extends Service {

    private static final String TAG = "LocationUpdateService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "LocationChannel";
    private static final String CHANNEL_NAME = "Location Updates";
    private static final int LOCATION_UPDATE_INTERVAL = 10000; // 10 seconds
    private static final int FASTEST_UPDATE_INTERVAL = 5000; // 5 seconds
    private static final float SIGNIFICANT_DISTANCE = 100; // 100 meters

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DatabaseManager dbManager;
    private LocationCache locationCache;
    private NotificationHelper notificationHelper;
    private Location lastReportedLocation;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: Service created");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        dbManager = new DatabaseManager();
        locationCache = new LocationCache();
        notificationHelper = new NotificationHelper(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: Service started");
        startForeground(NOTIFICATION_ID, buildNotification());
        requestLocationUpdates();
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("TinyReminder is active")
                .setContentText("Tracking your location")
                .setSmallIcon(R.drawable.ic_logo)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        return builder.build();
    }

    private void requestLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(LOCATION_UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_UPDATE_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.d(TAG, "onLocationResult: Location result is null");
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    Log.d(TAG, "onLocationResult: New location received: " + location.getLatitude() + ", " + location.getLongitude());
                    updateLocationInFirebase(location);
                }
            }
        };

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "requestLocationUpdates: Requesting location updates");
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
        } else {
            Log.e(TAG, "requestLocationUpdates: Location permission not granted");
        }
    }

    private void updateLocationInFirebase(Location location) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userId != null) {
            Log.d(TAG, "updateLocationInFirebase: Updating location for user: " + userId);
            dbManager.updateMemberLocation(userId, location.getLatitude(), location.getLongitude())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "updateLocationInFirebase: Location updated successfully");
                        locationCache.clearCache();
                        if (isSignificantMove(location)) {
                            Log.d(TAG, "updateLocationInFirebase: Significant move detected");
                            notificationHelper.showLocationUpdateNotification("Your location has been updated.");
                            lastReportedLocation = location;
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "updateLocationInFirebase: Failed to update location", e);
                        locationCache.addLocation(location);
                    });
        } else {
            Log.e(TAG, "updateLocationInFirebase: User ID is null");
        }
    }

    private boolean isSignificantMove(Location newLocation) {
        if (lastReportedLocation == null) {
            Log.d(TAG, "isSignificantMove: First location update");
            return true;
        }
        float distance = newLocation.distanceTo(lastReportedLocation);
        Log.d(TAG, "isSignificantMove: Distance moved: " + distance + " meters");
        return distance > SIGNIFICANT_DISTANCE;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Service is being destroyed");
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            Log.d(TAG, "onDestroy: Location updates removed");
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}