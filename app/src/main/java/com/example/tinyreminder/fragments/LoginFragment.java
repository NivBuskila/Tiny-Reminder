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
import com.example.tinyreminder.utils.AvatarUtils;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                checkAndCreateUserAvatar(user);
                ((MainActivity) requireActivity()).navigateToProfile();
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

    private void checkAndCreateUserAvatar(FirebaseUser user) {
        AvatarUtils.loadAvatarData(user.getUid(), (initials, color) -> {
            if (initials == null || color == 0) {
                String newInitials = AvatarUtils.getInitials(user.getDisplayName());
                int newColor = AvatarUtils.getRandomColor();
                AvatarUtils.saveAvatarData(user.getUid(), newInitials, newColor);
            }
        });
    }
}