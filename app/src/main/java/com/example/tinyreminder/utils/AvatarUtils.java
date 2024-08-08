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

    public static String getInitials(@NonNull String fullName) {
        String[] names = fullName.split(" ");
        StringBuilder initials = new StringBuilder();
        for (String name : names) {
            if (!name.isEmpty()) {
                initials.append(name.charAt(0));
                if (initials.length() == 2) break;
            }
        }
        return initials.toString().toUpperCase();
    }

    public static int getRandomColor() {
        Random rnd = new Random();
        return Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
    }

    public static Bitmap createAvatarBitmap(String initials, int color, int size) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(color);
        canvas.drawRect(0, 0, size, size, backgroundPaint);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(size / 2f);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);

        float xPos = size / 2f;
        float yPos = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2);

        canvas.drawText(initials, xPos, yPos, textPaint);

        return bitmap;
    }

    public static void saveAvatarData(String userId, String initials, int color) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot save avatar data: userId is null or empty");
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        Map<String, Object> avatarData = new HashMap<>();
        avatarData.put("initials", initials);
        avatarData.put("color", color);
        userRef.child("avatar").setValue(avatarData);
    }

    public interface OnAvatarDataLoadedListener {
        void onAvatarDataLoaded(String initials, int color);
    }

    public static void loadAvatarData(String userId, OnAvatarDataLoadedListener listener) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot load avatar data: userId is null or empty");
            listener.onAvatarDataLoaded(null, 0);
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        userRef.child("avatar").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String initials = dataSnapshot.child("initials").getValue(String.class);
                    Integer color = dataSnapshot.child("color").getValue(Integer.class);
                    listener.onAvatarDataLoaded(initials, color != null ? color : 0);
                } else {
                    listener.onAvatarDataLoaded(null, 0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading avatar data: " + databaseError.getMessage());
                listener.onAvatarDataLoaded(null, 0);
            }
        });
    }
}