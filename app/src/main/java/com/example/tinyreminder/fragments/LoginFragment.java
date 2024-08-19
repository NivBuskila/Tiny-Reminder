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
import com.example.tinyreminder.databinding.FragmentLoginBinding;
import com.example.tinyreminder.models.User;
import com.example.tinyreminder.utils.AvatarUtils;
import com.example.tinyreminder.utils.DatabaseManager;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.List;

public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";

    private FragmentLoginBinding binding;  // Binding for the login fragment layout
    private FirebaseAuth firebaseAuth;  // Firebase authentication instance
    private FirebaseAuth.AuthStateListener authStateListener;  // Listener for authentication state changes
    private DatabaseManager dbManager;  // Database manager for user data
    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            this::onSignInResult
    );

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Set up an AuthStateListener to handle state changes
        authStateListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                navigateToProfileScreen();
            }
        };

        // Initialize the DatabaseManager
        dbManager = new DatabaseManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.loginButton.setOnClickListener(v -> signIn());
    }

    @Override
    public void onStart() {
        super.onStart();
        // Add the AuthStateListener when the fragment starts
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Remove the AuthStateListener when the fragment stops
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }

    private void signIn() {
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build()
        );

        Intent signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .setTheme(R.style.LoginTheme)
                .setLogo(R.drawable.ic_logo)  // Set the logo for the login screen
                .setTosAndPrivacyPolicyUrls(
                        "https://example.com/terms",  // Link to Terms of Service
                        "https://example.com/privacy"  // Link to Privacy Policy
                )
                .build();
        signInLauncher.launch(signInIntent);
    }

    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        Log.d(TAG, "LoginFragment: onSignInResult called");
        IdpResponse response = result.getIdpResponse();
        if (result.getResultCode() == Activity.RESULT_OK) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            Log.d(TAG, "LoginFragment: signInWithCredential:success, user: " + (user != null ? user.getUid() : "null"));
            if (user != null) {
                Log.d(TAG, "LoginFragment: Attempting to navigate to profile");
                checkExistingUser(user.getUid(), user);
            }
        } else {
            Log.w(TAG, "LoginFragment: signInWithCredential:failure", response != null ? response.getError() : null);
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
        if (existingUser == null) {
            Log.e(TAG, "updateExistingUser: existingUser is null");
            return;
        }
        existingUser.setName(firebaseUser.getDisplayName());
        existingUser.setEmail(firebaseUser.getEmail());
        if (existingUser.getPhoneNumber() == null || existingUser.getPhoneNumber().isEmpty()) {
            existingUser.setPhoneNumber(firebaseUser.getPhoneNumber());
        }

        dbManager.updateUserProfile(existingUser.getId(), existingUser.toMap(), task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "User updated successfully");
                checkAndCreateUserAvatar(existingUser.getId(), existingUser.getName());
                navigateToProfileScreen();
            } else {
                Log.e(TAG, "Failed to update user", task.getException());
                Toast.makeText(requireContext(), "Failed to update user", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void createNewUser(String userId, FirebaseUser firebaseUser) {
        final String displayName = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() :
                (firebaseUser.getEmail() != null ? firebaseUser.getEmail().split("@")[0] : "User");

        User newUser = new User(
                userId,
                displayName,
                firebaseUser.getEmail(),
                firebaseUser.getPhoneNumber()
        );

        dbManager.createUser(newUser, task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "User created successfully in database");
                updateUserDisplayName(firebaseUser, displayName);
                createAndSaveUserAvatar(userId, displayName);
                navigateToProfileScreen();
            } else {
                Log.e(TAG, "Failed to create user in database", task.getException());
                Toast.makeText(requireContext(), "Failed to create user", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createAndSaveUserAvatar(String userId, String displayName) {
        String initials = AvatarUtils.getInitials(displayName);
        int color = AvatarUtils.getRandomColor();
        AvatarUtils.saveAvatarData(userId, initials, color);
    }

    private void updateUserDisplayName(FirebaseUser firebaseUser, String displayName) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build();

        firebaseUser.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User display name updated.");
                    } else {
                        Log.e(TAG, "Failed to update user display name", task.getException());
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

    private void navigateToProfileScreen() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).loadFragment(new ProfileFragment());
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up the binding to avoid memory leaks
        binding = null;
    }
}
