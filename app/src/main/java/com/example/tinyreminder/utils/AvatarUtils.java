package com.example.tinyreminder.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AvatarUtils {
    private static final String TAG = "AvatarUtils";

    /**
     * Extracts the initials from a given full name.
     *
     * @param fullName The full name from which to extract initials.
     * @return The initials of the full name, or an empty string if the name is null or empty.
     */
    public static String getInitials(@NonNull String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return ""; // Return an empty string if fullName is null or empty
        }
        String[] names = fullName.split(" ");
        StringBuilder initials = new StringBuilder();
        for (String name : names) {
            if (!name.isEmpty()) {
                initials.append(name.charAt(0));
                if (initials.length() == 2) break; // Stop after getting two initials
            }
        }
        return initials.toString().toUpperCase(); // Convert initials to uppercase
    }

    /**
     * Generates a random color.
     *
     * @return A random color in the ARGB format.
     */
    public static int getRandomColor() {
        Random rnd = new Random();
        return Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)); // Generate a random color
    }

    /**
     * Creates a bitmap image representing the user's avatar with initials and a background color.
     *
     * @param initials The initials to display in the avatar.
     * @param color    The background color of the avatar.
     * @param size     The size of the avatar bitmap.
     * @return A Bitmap representing the avatar.
     */
    public static Bitmap createAvatarBitmap(String initials, int color, int size) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Draw background color
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(color);
        canvas.drawRect(0, 0, size, size, backgroundPaint);

        // Draw initials text
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(size / 2f);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Calculate the position to center the text
        float xPos = size / 2f;
        float yPos = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2);

        canvas.drawText(initials, xPos, yPos, textPaint); // Draw the initials on the canvas

        return bitmap; // Return the created bitmap
    }

    /**
     * Saves the avatar data (initials and color) to Firebase under the user's profile.
     *
     * @param userId   The ID of the user.
     * @param initials The initials to save.
     * @param color    The color to save.
     */
    public static void saveAvatarData(String userId, String initials, int color) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot save avatar data: userId is null or empty");
            return; // If the userId is invalid, log an error and return
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        Map<String, Object> avatarData = new HashMap<>();
        avatarData.put("initials", initials);
        avatarData.put("color", color);
        userRef.child("avatar").setValue(avatarData); // Save the avatar data under the user's profile
    }

    /**
     * Interface for loading avatar data asynchronously.
     */
    public interface OnAvatarDataLoadedListener {
        void onAvatarDataLoaded(String initials, int color);
    }

    /**
     * Loads the avatar data (initials and color) from Firebase.
     * If no avatar data is found, it creates a new one using the user's name.
     *
     * @param userId   The ID of the user.
     * @param name     The name of the user, used to generate initials if needed.
     * @param listener The listener to notify when the avatar data is loaded.
     */
    public static void loadAvatarData(String userId, String name, OnAvatarDataLoadedListener listener) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot load avatar data: userId is null or empty");
            listener.onAvatarDataLoaded(null, 0); // If userId is invalid, notify listener with null data
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        userRef.child("avatar").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String initials = dataSnapshot.child("initials").getValue(String.class);
                    Integer color = dataSnapshot.child("color").getValue(Integer.class);
                    if (initials != null && color != null) {
                        listener.onAvatarDataLoaded(initials, color); // Notify listener with the loaded data
                    } else {
                        createAndSaveNewAvatar(userId, name, listener); // Create and save new avatar if data is incomplete
                    }
                } else {
                    createAndSaveNewAvatar(userId, name, listener); // Create and save new avatar if no data exists
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading avatar data: " + databaseError.getMessage());
                listener.onAvatarDataLoaded(null, 0); // Notify listener of the error
            }
        });
    }

    /**
     * Creates a new avatar and saves it to Firebase, then notifies the listener.
     *
     * @param userId   The ID of the user.
     * @param name     The name of the user, used to generate initials.
     * @param listener The listener to notify when the avatar data is ready.
     */
    private static void createAndSaveNewAvatar(String userId, String name, OnAvatarDataLoadedListener listener) {
        String newInitials = name != null ? getInitials(name) : "";
        int newColor = getRandomColor();
        saveAvatarData(userId, newInitials, newColor); // Save the newly created avatar data
        listener.onAvatarDataLoaded(newInitials, newColor); // Notify the listener with the new data
    }
}
