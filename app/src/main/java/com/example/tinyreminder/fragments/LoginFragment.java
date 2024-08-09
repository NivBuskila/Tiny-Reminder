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
import com.example.tinyreminder.models.User;
import com.example.tinyreminder.utils.DatabaseManager;
import java.util.Arrays;
import java.util.List;

public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";

    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            this::onSignInResult
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
                User user = new User();
                user.setId(userId);
                user.setName(firebaseUser.getDisplayName());
                user.setEmail(firebaseUser.getEmail());
                user.setPhoneNumber(firebaseUser.getPhoneNumber());

                DatabaseManager dbManager = new DatabaseManager();
                dbManager.createUser(user, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User created successfully");
                        checkAndCreateUserAvatar(firebaseUser);
                        ((MainActivity) requireActivity()).navigateToProfile();
                    } else {
                        Log.e(TAG, "Failed to create user", task.getException());
                        Toast.makeText(requireContext(), "Failed to create user", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } else {
            handleSignInError(response);
        }
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

    private void checkAndCreateUserAvatar(FirebaseUser firebaseUser) {
        String userId = firebaseUser.getUid();
        String displayName = firebaseUser.getDisplayName();
        String email = firebaseUser.getEmail();
        String phoneNumber = firebaseUser.getPhoneNumber();

        DatabaseManager dbManager = new DatabaseManager();
        User user = new User(userId, displayName, email, phoneNumber);

        dbManager.createOrUpdateUser(user, task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "User created/updated successfully");
                AvatarUtils.loadAvatarData(userId, displayName, (initials, color) -> {
                    if (initials == null || color == 0) {
                        String newInitials = AvatarUtils.getInitials(displayName);
                        int newColor = AvatarUtils.getRandomColor();
                        AvatarUtils.saveAvatarData(userId, newInitials, newColor);
                    }
                });
            } else {
                Log.e(TAG, "Failed to create/update user", task.getException());
            }
        });
    }
}