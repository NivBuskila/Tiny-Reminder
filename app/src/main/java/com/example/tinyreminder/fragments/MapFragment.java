package com.example.tinyreminder.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.tinyreminder.R;
import com.example.tinyreminder.models.User;
import com.example.tinyreminder.utils.AvatarUtils;
import com.example.tinyreminder.utils.DatabaseManager;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class MapFragment extends Fragment implements OnMapReadyCallback {
    private static final String TAG = "MapFragment";
    private static final String ARG_MEMBER_ID = "member_id";
    private static final int MARKER_SIZE = 120; // Size for the marker in pixels
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int MAP_PADDING = 100; // pixels

    private static final float MIN_DISTANCE_FOR_UPDATE = 50f; // 50 METERS
    private static final float DEFAULT_ZOOM = 15f;
    private static final float MAX_ZOOM = 18f;
    private boolean isNavigatedFromBottomNav = false;

    private long lastZoomAdjustment = 0;
    private static final long ZOOM_ADJUSTMENT_COOLDOWN = 10000; // 10 seconds
    private GoogleMap map;
    private String memberId;
    private String currentUserFamilyId;

    private DatabaseManager dbManager;
    private Map<String, Marker> markers = new HashMap<>();
    private Map<String, LatLng> lastKnownLocations = new HashMap<>();

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private FloatingActionButton btnMyLocation;

    public static MapFragment newInstance(String memberId) {
        MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MEMBER_ID, memberId);
        fragment.setArguments(args);
        return fragment;
    }
    public static MapFragment newInstance(boolean fromBottomNav, String memberId) {
        MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
        args.putBoolean("fromBottomNav", fromBottomNav);
        args.putString(ARG_MEMBER_ID, memberId);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            memberId = getArguments().getString(ARG_MEMBER_ID);
            isNavigatedFromBottomNav = getArguments().getBoolean("fromBottomNav", false);
        }
        if (memberId == null || memberId.isEmpty()) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                memberId = currentUser.getUid();
            }
        }

        dbManager = new DatabaseManager(requireContext());

        // קבל את ה-familyId של המשתמש הנוכחי
        dbManager.getUserData(memberId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User currentUser = snapshot.getValue(User.class);
                if (currentUser != null) {
                    currentUserFamilyId = currentUser.getFamilyId();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching current user data: ", error.toException());
            }
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        Log.d(TAG, "onCreate: Fragment created with member ID: " + memberId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Creating view for MapFragment");
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        btnMyLocation = view.findViewById(R.id.btn_my_location);
        btnMyLocation.setOnClickListener(v -> zoomToCurrentUser());
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "Error: Map fragment is null");
            Toast.makeText(getContext(), "Error loading map", Toast.LENGTH_SHORT).show();
        }
        return view;
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: Map is ready");
        map = googleMap;
        enableMyLocation();
        setupMapSettings();
        setupLocationListener();
        if (isNavigatedFromBottomNav) {
            zoomToCurrentUser();
        }
    }

    private void zoomToCurrentUser() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f));
                }
            });
        }
    }

    private void setupMapSettings() {
        if (map != null) {
            Log.d(TAG, "setupMapSettings: Configuring map settings");
            map.getUiSettings().setZoomControlsEnabled(true);
            map.getUiSettings().setMyLocationButtonEnabled(false); // Disable default button
            map.getUiSettings().setCompassEnabled(true);
            map.getUiSettings().setMapToolbarEnabled(false);
        } else {
            Log.e(TAG, "setupMapSettings: Map is null");
        }
    }

    private void addCurrentUserMarker(LatLng location) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        dbManager.getUserData(userId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    createOrUpdateMarker(user, location);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching current user data: ", error.toException());
            }
        });
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (map != null) {
                Log.d(TAG, "enableMyLocation: Enabling my location on map");
                map.setMyLocationEnabled(false); // Disable default blue dot

                // Get the last known location and add custom marker
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        addCurrentUserMarker(currentLocation);
                    }
                });

                // Set up location callback for updates
                locationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        if (locationResult == null) return;
                        for (Location location : locationResult.getLocations()) {
                            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            addCurrentUserMarker(currentLocation);
                        }
                    }
                };

                // Request location updates
                LocationRequest locationRequest = LocationRequest.create()
                        .setInterval(10000)
                        .setFastestInterval(5000)
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);

            } else {
                Log.e(TAG, "enableMyLocation: Map is null");
            }
        } else {
            Log.d(TAG, "enableMyLocation: Requesting location permission");
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: Location permission granted");
                enableMyLocation();
            } else {
                Log.w(TAG, "onRequestPermissionsResult: Location permission denied");
                Toast.makeText(getContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupLocationListener() {
        if (memberId == null || memberId.isEmpty()) {
            Log.e(TAG, "setupLocationListener: Member ID is null or empty");
            Toast.makeText(getContext(), "Error: Invalid member ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "setupLocationListener: Setting up listener for member ID: " + memberId);
        dbManager.getUserData(memberId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null && user.getFamilyId() != null && !user.getFamilyId().isEmpty()) {
                    Log.d(TAG, "onDataChange: User data retrieved. Family ID: " + user.getFamilyId());
                    setupRealtimeFamilyLocationUpdates(user.getFamilyId());
                    // Focus on the specific member if one is selected
                    if (!isNavigatedFromBottomNav) {
                        focusOnMember(memberId);
                    }
                } else {
                    Log.d(TAG, "onDataChange: User does not belong to a family");
                    showNoFamilyMessage();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "onCancelled: Error fetching user data: ", error.toException());
                Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void focusOnMember(String memberId) {
        Marker marker = markers.get(memberId);
        if (marker != null) {
            LatLng position = marker.getPosition();
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, DEFAULT_ZOOM));
        } else {
            Log.w(TAG, "focusOnMember: Marker not found for member ID: " + memberId);
        }
    }

    private void setupRealtimeFamilyLocationUpdates(String familyId) {
        dbManager.getRealtimeLocationsForFamily(familyId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot memberSnapshot : dataSnapshot.getChildren()) {
                    String memberId = memberSnapshot.getKey();
                    Double latitude = memberSnapshot.child("latitude").getValue(Double.class);
                    Double longitude = memberSnapshot.child("longitude").getValue(Double.class);
                    if (latitude != null && longitude != null) {
                        LatLng newLocation = new LatLng(latitude, longitude);
                        updateMemberMarker(memberId, newLocation);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error fetching family locations: ", databaseError.toException());
            }
        });
    }


    private float distanceBetween(LatLng point1, LatLng point2) {
        float[] results = new float[1];
        Location.distanceBetween(point1.latitude, point1.longitude, point2.latitude, point2.longitude, results);
        return results[0];
    }

    private void showNoFamilyMessage() {
        Toast.makeText(getContext(), "You are not part of a family. Please join or create a family first.", Toast.LENGTH_LONG).show();
    }


    private void updateMemberMarker(String memberId, LatLng location) {
        Log.d(TAG, "updateMemberMarker: Updating marker for member: " + memberId);
        dbManager.getUserData(memberId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null && user.getFamilyId() != null && user.getFamilyId().equals(currentUserFamilyId)) {
                    Log.d(TAG, "onDataChange: User belongs to the same family. Updating marker for member: " + memberId);

                    LatLng lastKnownLocation = markers.containsKey(memberId) ? markers.get(memberId).getPosition() : null;
                    if (lastKnownLocation == null || distanceBetween(lastKnownLocation, location) > MIN_DISTANCE_FOR_UPDATE) {
                        createOrUpdateMarker(user, location);

                    }
                } else {
                    Log.d(TAG, "onDataChange: User does not belong to the same family. Skipping update for member: " + memberId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "onCancelled: Error fetching user data for marker: ", error.toException());
            }
        });
    }


    private void createOrUpdateMarker(User user, LatLng location) {
        Log.d(TAG, "createOrUpdateMarker: Creating/Updating marker for user: " + user.getId());
        if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
            Log.d(TAG, "createOrUpdateMarker: Loading profile picture for user: " + user.getId());
            Glide.with(this)
                    .asBitmap()
                    .load(user.getProfilePictureUrl())
                    .circleCrop()
                    .into(new CustomTarget<Bitmap>(MARKER_SIZE, MARKER_SIZE) {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            Log.d(TAG, "onResourceReady: Profile picture loaded for user: " + user.getId());
                            Bitmap markerBitmap = createMarkerBitmapFromProfile(resource);
                            addOrUpdateMarkerOnMap(user.getId(), location, markerBitmap, user.getName());
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            Log.d(TAG, "onLoadCleared: Profile picture load cleared for user: " + user.getId());
                        }
                    });
        } else {
            Log.d(TAG, "createOrUpdateMarker: Creating avatar for user: " + user.getId());
            AvatarUtils.loadAvatarData(user.getId(), user.getName(), (initials, color) -> {
                if (initials != null && color != 0) {
                    Log.d(TAG, "Avatar created for user: " + user.getId());
                    Bitmap avatarBitmap = AvatarUtils.createAvatarBitmap(initials, color, MARKER_SIZE);
                    Bitmap markerBitmap = createMarkerBitmapFromProfile(avatarBitmap);
                    addOrUpdateMarkerOnMap(user.getId(), location, markerBitmap, user.getName());
                } else {
                    Log.w(TAG, "Failed to create avatar for user: " + user.getId());
                    // Use a default marker as fallback
                    addOrUpdateMarkerOnMap(user.getId(), location, null, user.getName());
                }
            });
        }
    }

    private Bitmap createMarkerBitmapFromProfile(Bitmap profile) {
        Log.d(TAG, "createMarkerBitmapFromProfile: Creating marker bitmap");
        Bitmap background = Bitmap.createBitmap(MARKER_SIZE, MARKER_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(background);

        // Draw a white circle for the background
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        canvas.drawCircle(MARKER_SIZE / 2f, MARKER_SIZE / 2f, MARKER_SIZE / 2f - 2, paint);

        // Draw the profile picture or avatar
        canvas.drawBitmap(profile, 2, 2, null);

        // Draw a small inverted triangle at the bottom to make it look like a pin
        Paint trianglePaint = new Paint();
        trianglePaint.setColor(Color.WHITE);
        trianglePaint.setStyle(Paint.Style.FILL);

        float triangleSize = MARKER_SIZE / 4f;
        float centerX = MARKER_SIZE / 2f;
        float bottomY = MARKER_SIZE;

        canvas.drawPath(new android.graphics.Path() {{
            moveTo(centerX, bottomY);
            lineTo(centerX - triangleSize / 2, bottomY - triangleSize);
            lineTo(centerX + triangleSize / 2, bottomY - triangleSize);
            close();
        }}, trianglePaint);

        Log.d(TAG, "createMarkerBitmapFromProfile: Marker bitmap created");
        return background;
    }

    private void addOrUpdateMarkerOnMap(String userId, LatLng location, Bitmap markerIcon, String title) {
        Log.d(TAG, "addOrUpdateMarkerOnMap: Adding/Updating marker for user: " + userId);
        if (map != null) {
            if (markers.containsKey(userId)) {
                // Update existing marker
                Marker marker = markers.get(userId);
                Log.d(TAG, "addOrUpdateMarkerOnMap: Updating existing marker for user: " + userId);
                marker.setPosition(location);
                if (markerIcon != null) {
                    marker.setIcon(BitmapDescriptorFactory.fromBitmap(markerIcon));
                }
            } else {
                // Add new marker
                Log.d(TAG, "addOrUpdateMarkerOnMap: Adding new marker for user: " + userId);
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(location)
                        .title(title);
                if (markerIcon != null) {
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(markerIcon));
                }
                Marker marker = map.addMarker(markerOptions);
                markers.put(userId, marker);
            }

            // If this is the focused member, ensure they're visible
            if (userId.equals(memberId) && !isNavigatedFromBottomNav) {
                focusOnMember(userId);
            }
        } else {
            Log.e(TAG, "addOrUpdateMarkerOnMap: Map is null");
        }
    }

    private void ensureFocusedMemberVisible(LatLng location) {
        Log.d(TAG, "ensureFocusedMemberVisible: Ensuring focused member is visible");
        if (map != null && !map.getProjection().getVisibleRegion().latLngBounds.contains(location)) {
            Log.d(TAG, "ensureFocusedMemberVisible: Animating camera to focused member");
            map.animateCamera(CameraUpdateFactory.newLatLng(location));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: Cleaning up resources");
        // Clean up resources
        for (Marker marker : markers.values()) {
            marker.remove();
        }
        markers.clear();

        // Remove location updates
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    // You might want to add these methods for better lifecycle management
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: MapFragment resumed");
        if (map != null) {
            setupLocationListener();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: MapFragment paused");
        // You might want to stop location updates here if you're using them
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: MapFragment destroyed");
        // Perform any final cleanup here
    }

    // Helper method to log the current state of markers
    private void logMarkerState() {
        Log.d(TAG, "Current marker state:");
        for (Map.Entry<String, Marker> entry : markers.entrySet()) {
            Log.d(TAG, "User ID: " + entry.getKey() + ", Position: " + entry.getValue().getPosition());
        }
    }
}