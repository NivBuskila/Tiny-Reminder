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
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent locationServiceIntent = new Intent(context, LocationUpdateService.class);
            Intent parkingDetectionServiceIntent = new Intent(context, ParkingDetectionService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(locationServiceIntent);
                context.startForegroundService(parkingDetectionServiceIntent);
            } else {
                context.startService(locationServiceIntent);
                context.startService(parkingDetectionServiceIntent);
            }
        }
    }
}