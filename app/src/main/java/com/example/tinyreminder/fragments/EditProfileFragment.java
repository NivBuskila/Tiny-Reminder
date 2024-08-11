package com.example.tinyreminder.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    private CircleImageView editProfileImage;
    private MaterialButton changePhotoButton;
    private TextInputEditText editName;
    private TextInputEditText editEmail;
    private TextInputEditText editPhone;
    private MaterialButton saveProfileButton;
    private DatabaseManager dbManager;
    private String originalName;
    private String userId;
    private Uri imageUri;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);
        findViews(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbManager = new DatabaseManager();
        initViews();
        setupButtons();
    }

    private void findViews(View view) {
        editProfileImage = view.findViewById(R.id.edit_profile_image);
        changePhotoButton = view.findViewById(R.id.change_photo_button);
        editName = view.findViewById(R.id.edit_name);
        editEmail = view.findViewById(R.id.edit_email);
        editPhone = view.findViewById(R.id.edit_phone);
        saveProfileButton = view.findViewById(R.id.save_profile_button);
    }

    private void initViews() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            loadUserData(userId);
        }
    }

    private void loadUserData(String uid) {
        if (uid == null || uid.isEmpty()) {
            Log.e(TAG, "User ID is null or empty");
            return;
        }

        dbManager.getUserData(uid, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
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
                Log.e(TAG, "Failed to load user data: " + databaseError.getMessage());
                Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(User user) {
        loadAndDisplayAvatar(user.getId(), user.getName(), user.getProfilePictureUrl());
        editName.setText(user.getName());
        editEmail.setText(user.getEmail());
        editPhone.setText(user.getPhoneNumber());
    }

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

    private void setupButtons() {
        changePhotoButton.setOnClickListener(v -> openImagePicker());
        saveProfileButton.setOnClickListener(v -> saveProfileChanges());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            Glide.with(this).load(imageUri).circleCrop().into(editProfileImage);
        }
    }

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
                        if (task.isSuccessful()) {
                            updateEmailAndPhone(user, newName, newEmail, newPhone);
                        } else {
                            Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void updateEmailAndPhone(FirebaseUser user, String newName, String newEmail, String newPhone) {
        if (newEmail == null || newEmail.trim().isEmpty()) {
            saveUserToDatabase(user.getUid(), newName, user.getEmail(), newPhone);
        } else {
            user.updateEmail(newEmail)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            saveUserToDatabase(user.getUid(), newName, newEmail, newPhone);
                        } else {
                            Toast.makeText(getContext(), "Failed to update email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void saveUserToDatabase(String uid, String name, String email, String phone) {
        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("id", uid);
        userUpdates.put("name", name);
        if (email != null && !email.trim().isEmpty()) {
            userUpdates.put("email", email);
        }
        if (phone != null && !phone.trim().isEmpty()) {
            userUpdates.put("phoneNumber", phone);
        }

        if (imageUri != null) {
            uploadProfilePicture(uid, userUpdates);
        } else {
            updateUserProfile(uid, userUpdates);
        }
    }

    private void uploadProfilePicture(String uid, Map<String, Object> userUpdates) {
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
        dbManager.updateUserProfile(uid, userUpdates, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                if (!userUpdates.get("name").equals(originalName)) {
                    updateAvatarIfNameChanged(uid, (String) userUpdates.get("name"));
                }
                getParentFragmentManager().popBackStack();
            } else {
                Toast.makeText(getContext(), "Failed to save user data to database", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateAvatarIfNameChanged(String uid, String newName) {
        AvatarUtils.loadAvatarData(uid, newName, (oldInitials, oldColor) -> {
            String newInitials = AvatarUtils.getInitials(newName);
            if (!newInitials.equals(oldInitials)) {
                int newColor = AvatarUtils.getRandomColor();
                AvatarUtils.saveAvatarData(uid, newInitials, newColor);
            }
        });
    }
}