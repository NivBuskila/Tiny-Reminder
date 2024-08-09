package com.example.tinyreminder.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.tinyreminder.MainActivity;
import com.example.tinyreminder.R;
import com.example.tinyreminder.models.User;
import com.example.tinyreminder.utils.AvatarUtils;
import com.example.tinyreminder.utils.DatabaseManager;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.List;

public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";

    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            this::onSignInResult
    );

    private DatabaseManager dbManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbManager = new DatabaseManager();
        signIn();
    }

    private void signIn() {
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        Intent signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .setTheme(R.style.LoginTheme)
                .setLogo(R.drawable.ic_logo)
                .setTosAndPrivacyPolicyUrls("https://example.com/terms", "https://example.com/privacy")
                .build();
        signInLauncher.launch(signInIntent);
    }

    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        IdpResponse response = result.getIdpResponse();
        if (result.getResultCode() == Activity.RESULT_OK) {
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser != null) {
                String userId = firebaseUser.getUid();
                checkExistingUser(userId, firebaseUser);
            }
        } else {
            handleSignInError(response);
        }
    }

    private void checkExistingUser(String userId, FirebaseUser firebaseUser) {
        dbManager.getUserData(userId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    User existingUser = dataSnapshot.getValue(User.class);
                    updateExistingUser(existingUser, firebaseUser);
                } else {
                    createNewUser(userId, firebaseUser);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error checking existing user", databaseError.toException());
                Toast.makeText(requireContext(), "Error during sign in", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateExistingUser(User existingUser, FirebaseUser firebaseUser) {
        existingUser.setName(firebaseUser.getDisplayName());
        existingUser.setEmail(firebaseUser.getEmail());
        if (existingUser.getPhoneNumber() == null || existingUser.getPhoneNumber().isEmpty()) {
            existingUser.setPhoneNumber(firebaseUser.getPhoneNumber());
        }

        dbManager.updateUserProfile(existingUser.getId(), existingUser.toMap(), task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "User updated successfully");
                checkAndCreateUserAvatar(existingUser.getId(), existingUser.getName());
                ((MainActivity) requireActivity()).navigateToProfile();
            } else {
                Log.e(TAG, "Failed to update user", task.getException());
                Toast.makeText(requireContext(), "Failed to update user", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createNewUser(String userId, FirebaseUser firebaseUser) {
        User newUser = new User(
                userId,
                firebaseUser.getDisplayName(),
                firebaseUser.getEmail(),
                firebaseUser.getPhoneNumber()
        );

        dbManager.createUser(newUser, task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "User created successfully");
                checkAndCreateUserAvatar(userId, newUser.getName());
                ((MainActivity) requireActivity()).navigateToProfile();
            } else {
                Log.e(TAG, "Failed to create user", task.getException());
                Toast.makeText(requireContext(), "Failed to create user", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleSignInError(IdpResponse response) {
        if (response == null) {
            Log.d(TAG, "Sign in cancelled by user");
            Toast.makeText(requireContext(), "Sign in cancelled", Toast.LENGTH_SHORT).show();
        } else if (response.getError() != null) {
            Log.e(TAG, "Sign in error: ", response.getError());
            Toast.makeText(requireContext(), "Sign in failed: " + response.getError().getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void checkAndCreateUserAvatar(String userId, String displayName) {
        AvatarUtils.loadAvatarData(userId, displayName, (initials, color) -> {
            if (initials == null || color == 0) {
                String newInitials = AvatarUtils.getInitials(displayName);
                int newColor = AvatarUtils.getRandomColor();
                AvatarUtils.saveAvatarData(userId, newInitials, newColor);
            }
        });
    }
}