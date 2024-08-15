package com.example.tinyreminder;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.tinyreminder.fragments.FamilyFragment;
import com.example.tinyreminder.fragments.LoginFragment;
import com.example.tinyreminder.fragments.MapFragment;
import com.example.tinyreminder.fragments.ProfileFragment;
import com.example.tinyreminder.services.LocationUpdateService;
import com.example.tinyreminder.services.ParkingDetectionService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.Manifest;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkLocationPermission();

        mAuth = FirebaseAuth.getInstance();

        setupBottomNavigation();
        setupBackPressedCallback();
        setupAuthStateListener();
        checkUserAuthState();
    }

    private void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_profile) {
                selectedFragment = new ProfileFragment();
            } else if (itemId == R.id.navigation_family) {
                selectedFragment = new FamilyFragment();
            } else if (itemId == R.id.navigation_map) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    selectedFragment = MapFragment.newInstance(true, currentUser.getUid());
                }
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationServices();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationServices();
            } else {
                Toast.makeText(this, "Location permission is necessary for the app to function properly", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startLocationServices() {
        Intent locationServiceIntent = new Intent(this, LocationUpdateService.class);
        startService(locationServiceIntent);

        Intent parkingDetectionServiceIntent = new Intent(this, ParkingDetectionService.class);
        startService(parkingDetectionServiceIntent);
    }

    private void setupBackPressedCallback() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    finish();
                }
            }
        });
    }

    private void setupAuthStateListener() {
        mAuthListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            Log.d(TAG, "onAuthStateChanged: " + (user != null ? user.getUid() : "null"));
            if (user != null) {
                navigateToProfile();
            } else {
                navigateToLogin();
            }
        };
    }

    private void checkUserAuthState() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User is signed in, navigating to profile");
            navigateToProfile();
        } else {
            Log.d(TAG, "User is not signed in, navigating to login");
            navigateToLogin();
        }
    }

    public void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }




    public void navigateToLogin() {
        Log.d(TAG, "Navigating to login");
        loadFragment(new LoginFragment());
        bottomNavigationView.setVisibility(View.GONE);
    }

    public void navigateToProfile() {
        Log.d(TAG, "MainActivity: navigateToProfile called");
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        // לוג נוסף לבדוק אם ה-Fragment הנוכחי הוא ה-ProfileFragment
        if (currentFragment instanceof ProfileFragment) {
            Log.d(TAG, "MainActivity: ProfileFragment is already displayed");
            return;
        }

        Fragment profileFragment = new ProfileFragment();
        loadFragment(profileFragment);
        Log.d(TAG, "MainActivity: after loadFragment call");

        bottomNavigationView.setVisibility(View.VISIBLE);
        bottomNavigationView.setSelectedItemId(R.id.navigation_profile);
    }



    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "MainActivity: onStart");
        mAuth.addAuthStateListener(mAuthListener);
        checkUserAuthState();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
}