package com.example.tinyreminder.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.example.tinyreminder.services.LocationUpdateService;
import com.example.tinyreminder.services.ParkingDetectionService;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Check if the received broadcast is for device boot completion
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Create intents for the services that need to be started after boot
            Intent locationServiceIntent = new Intent(context, LocationUpdateService.class);
            Intent parkingDetectionServiceIntent = new Intent(context, ParkingDetectionService.class);

            // Check the Android version to determine how to start the services
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // For Android O and above, use startForegroundService to start the services in the foreground
                context.startForegroundService(locationServiceIntent);
                context.startForegroundService(parkingDetectionServiceIntent);
            } else {
                // For earlier versions, use startService to start the services in the background
                context.startService(locationServiceIntent);
                context.startService(parkingDetectionServiceIntent);
            }
        }
    }
}
