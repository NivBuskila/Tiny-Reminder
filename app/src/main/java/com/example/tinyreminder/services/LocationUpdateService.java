package com.example.tinyreminder.services;

import static android.content.ContentValues.TAG;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.util.Log;

import com.example.tinyreminder.MainActivity;
import com.example.tinyreminder.R;
import com.example.tinyreminder.models.User;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.example.tinyreminder.utils.DatabaseManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class LocationUpdateService extends Service {

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "LocationServiceChannel";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DatabaseManager dbManager;
    private String familyId;

    @Override
    public void onCreate() {
        super.onCreate();
        // Create a notification channel for foreground service
        createNotificationChannel();
        // Build and start the notification
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        }
        // Initialize FusedLocationProviderClient for location updates
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Initialize DatabaseManager for database operations
        dbManager = new DatabaseManager(this);
        // Fetch the user's family ID from the database
        getUserFamilyId();

        // Define the callback to handle location updates
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                // For each location update, send it to Firebase
                for (Location location : locationResult.getLocations()) {
                    updateLocationInFirebase(location);
                }
            }
        };
    }

    // Method to retrieve the user's family ID from the database
    private void getUserFamilyId() {
        String userId = getUserId();
        if (userId != null) {
            dbManager.getUserData(userId, new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        familyId = user.getFamilyId(); // Store the family ID
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error fetching user data: ", error.toException());
                }
            });
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Check for location permissions and start requesting location updates
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            requestLocationUpdates();
        }
        return START_STICKY; // Ensure the service is restarted if terminated
    }

    // Method to request location updates from the fused location provider
    private void requestLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000); // 10 seconds
        locationRequest.setFastestInterval(5000); // Fastest interval of 5 seconds
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // High accuracy priority

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    // Method to update the user's location in Firebase
    private void updateLocationInFirebase(Location location) {
        String userId = getUserId();
        if (userId != null && familyId != null) {
            dbManager.updateMemberLocation(userId, familyId, location.getLatitude(), location.getLongitude())
                    .addOnSuccessListener(aVoid -> {
                        // Broadcast an intent to notify other components of the location update
                        Intent intent = new Intent("com.example.tinyreminder.FAMILY_STATUS_CHANGED");
                        sendBroadcast(intent);
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to update location", e));
        }
    }

    // Method to create the notification channel for the service
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop location updates when the service is destroyed
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // This service is not designed for binding
    }

    // Helper method to get the current user's ID
    private String getUserId() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            return auth.getCurrentUser().getUid();
        }
        return null;
    }
}
