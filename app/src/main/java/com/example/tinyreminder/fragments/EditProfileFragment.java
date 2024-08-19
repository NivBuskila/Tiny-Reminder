package com.example.tinyreminder.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.tinyreminder.R;
import com.example.tinyreminder.models.User;
import com.example.tinyreminder.utils.AvatarUtils;
import com.example.tinyreminder.utils.DatabaseManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileFragment extends Fragment {

    private static final String TAG = "EditProfileFragment";
    private static final int PICK_IMAGE_REQUEST = 1;

    // UI elements for profile editing
    private CircleImageView editProfileImage;
    private MaterialButton changePhotoButton;
    private TextInputEditText editName;
    private TextInputEditText editEmail;
    private TextInputEditText editPhone;
    private MaterialButton saveProfileButton;

    // Database manager instance for accessing user data
    private DatabaseManager dbManager;

    // Variables to hold original user data
    private String originalName;
    private String userId;
    private Uri imageUri;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment and initialize views
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);
        findViews(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Initialize the database manager and UI elements
        dbManager = new DatabaseManager(requireContext());
        initViews();
        setupButtons();
    }

    // Method to find and initialize all UI elements in the layout
    private void findViews(View view) {
        editProfileImage = view.findViewById(R.id.edit_profile_image);
        changePhotoButton = view.findViewById(R.id.change_photo_button);
        editName = view.findViewById(R.id.edit_name);
        editEmail = view.findViewById(R.id.edit_email);
        editPhone = view.findViewById(R.id.edit_phone);
        saveProfileButton = view.findViewById(R.id.save_profile_button);
    }

    // Method to initialize views with current user data
    private void initViews() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            loadUserData(userId);
        }
    }

    // Method to load user data from the database using the user ID
    private void loadUserData(String uid) {
        if (uid == null || uid.isEmpty()) {
            Log.e(TAG, "User ID is null or empty");
            return;
        }

        dbManager.getUserData(uid, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Retrieve the user data from the snapshot
                User user = dataSnapshot.getValue(User.class);
                if (user != null) {
                    user.setId(uid);
                    updateUI(user);
                    originalName = user.getName();
                } else {
                    Log.e(TAG, "User data is null");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle the error when data loading is cancelled
                Log.e(TAG, "Failed to load user data: " + databaseError.getMessage());
                Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Method to update the UI with the retrieved user data
    private void updateUI(User user) {
        loadAndDisplayAvatar(user.getId(), user.getName(), user.getProfilePictureUrl());
        editName.setText(user.getName());
        editEmail.setText(user.getEmail());
        editPhone.setText(user.getPhoneNumber());
    }

    // Method to load and display the user's avatar or a default avatar if none is available
    private void loadAndDisplayAvatar(String uid, String name, String profilePictureUrl) {
        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
            Glide.with(this)
                    .load(profilePictureUrl)
                    .circleCrop()
                    .into(editProfileImage);
        } else {
            AvatarUtils.loadAvatarData(uid, name, (initials, color) -> {
                if (initials != null && color != 0) {
                    editProfileImage.setImageBitmap(AvatarUtils.createAvatarBitmap(initials, color, 200));
                }
            });
        }
    }

    // Method to set up button click listeners
    private void setupButtons() {
        changePhotoButton.setOnClickListener(v -> openImagePicker());
        saveProfileButton.setOnClickListener(v -> saveProfileChanges());
    }

    // Method to open the image picker to select a new profile picture
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Handle the result of the image picker
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            Glide.with(this).load(imageUri).circleCrop().into(editProfileImage);
        }
    }

    // Method to save the profile changes made by the user
    private void saveProfileChanges() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String newName = editName.getText().toString().trim();
            String newEmail = editEmail.getText().toString().trim();
            String newPhone = editPhone.getText().toString().trim();

            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        // Check if profile update was successful
                        if (task.isSuccessful()) {
                            checkPhoneNumberUniqueness(user, newName, newEmail, newPhone);
                        } else {
                            Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void checkPhoneNumberUniqueness(FirebaseUser user, String newName, String newEmail, String newPhone) {
        // Check if the phone number is empty, if so, update email and phone directly
        if (newPhone.isEmpty()) {
            updateEmailAndPhone(user, newName, newEmail, newPhone);
            return;
        }

        // Query the database to check if the phone number is already in use
        dbManager.getUsersByPhoneNumber(newPhone, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean isUnique = true;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    User existingUser = snapshot.getValue(User.class);
                    if (existingUser != null && !existingUser.getId().equals(user.getUid())) {
                        isUnique = false;
                        break;
                    }
                }

                // If the phone number is unique, proceed to update, otherwise, show an error message
                if (isUnique) {
                    updateEmailAndPhone(user, newName, newEmail, newPhone);
                } else {
                    Toast.makeText(getContext(), "Phone number is already in use", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Failed to check phone number: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmailAndPhone(FirebaseUser user, String newName, String newEmail, String newPhone) {
        // Check if the email is empty, if so, save user data directly, otherwise prompt for reauthentication
        if (newEmail == null || newEmail.trim().isEmpty()) {
            saveUserToDatabase(user.getUid(), newName, user.getEmail(), newPhone);
        } else {
            promptForPasswordAndReauthenticate(user, newEmail, newName, newPhone);
        }
    }

    private void saveUserToDatabase(String uid, String name, String email, String phone) {
        // Prepare the data to be updated in the database
        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("id", uid);
        userUpdates.put("name", name);
        if (email != null && !email.trim().isEmpty()) {
            userUpdates.put("email", email);
        }
        if (phone != null && !phone.trim().isEmpty()) {
            userUpdates.put("phoneNumber", phone);
        }

        // Check if there's an imageUri, if so, upload the profile picture first, otherwise update the profile directly
        if (imageUri != null) {
            uploadProfilePicture(uid, userUpdates);
        } else {
            updateUserProfile(uid, userUpdates);
        }
    }

    private void uploadProfilePicture(String uid, Map<String, Object> userUpdates) {
        // Upload the selected profile picture to the database
        dbManager.uploadProfilePicture(uid, imageUri, task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                userUpdates.put("profilePictureUrl", downloadUri.toString());
                updateUserProfile(uid, userUpdates);
            } else {
                Toast.makeText(getContext(), "Failed to upload profile picture", Toast.LENGTH_SHORT).show();
                updateUserProfile(uid, userUpdates);
            }
        });
    }

    private void updateUserProfile(String uid, Map<String, Object> userUpdates) {
        // Update the user profile in the database with the provided data
        dbManager.updateUserProfile(uid, userUpdates, task -> {
            if (task.isSuccessful()) {
                Context context = getContext();
                if (context != null) {
                    Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                }
                // If the name has changed, update the avatar accordingly
                if (!userUpdates.get("name").equals(originalName)) {
                    updateAvatarIfNameChanged(uid, (String) userUpdates.get("name"));
                }
                // Navigate back to the previous screen
                getParentFragmentManager().popBackStack();
            } else {
                Context context = getContext();
                if (context != null) {
                    Toast.makeText(context, "Failed to save user data to database", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void navigateToProfile() {
        // Navigate to the ProfileFragment when called
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new ProfileFragment())
                .addToBackStack(null)
                .commit();
    }

    private void promptForPasswordAndReauthenticate(FirebaseUser user, String newEmail, String newName, String newPhone) {
        // Show a dialog to prompt the user for their password for reauthentication
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Reauthenticate");

        final EditText passwordInput = new EditText(getContext());
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setHint("Enter your current password");

        builder.setView(passwordInput);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String password = passwordInput.getText().toString().trim();
            if (!password.isEmpty()) {
                reauthenticateAndChangeEmail(user, newEmail, newName, newPhone, password);
            } else {
                Toast.makeText(getContext(), "Password cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void reauthenticateAndChangeEmail(FirebaseUser user, String newEmail, String newName, String newPhone, String password) {
        // Reauthenticate the user with their password before updating the email
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);

        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                user.updateEmail(newEmail).addOnCompleteListener(emailTask -> {
                    if (emailTask.isSuccessful()) {
                        saveUserToDatabase(user.getUid(), newName, newEmail, newPhone);
                    } else {
                        Toast.makeText(getContext(), "Failed to update email: " + emailTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(getContext(), "Reauthentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateAvatarIfNameChanged(String uid, String newName) {
        // Update the avatar if the user's name has changed
        AvatarUtils.loadAvatarData(uid, newName, (oldInitials, oldColor) -> {
            String newInitials = AvatarUtils.getInitials(newName);
            if (!newInitials.equals(oldInitials)) {
                int newColor = AvatarUtils.getRandomColor();
                AvatarUtils.saveAvatarData(uid, newInitials, newColor);
            }
        });
    }
}