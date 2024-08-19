package com.example.tinyreminder.fragments;

import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.tinyreminder.MainActivity;
import com.example.tinyreminder.R;
import com.example.tinyreminder.models.Family;
import com.example.tinyreminder.models.User;
import com.example.tinyreminder.utils.AvatarUtils;
import com.example.tinyreminder.utils.DatabaseManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";

    private CircleImageView profileImage;
    private TextView profileName, profileEmail, profilePhone, familyName, familyCode;
    private Button editProfileButton, familyButton, createJoinFamilyButton, logoutButton;
    private DatabaseManager dbManager;
    private FirebaseAuth mAuth;
    private User currentUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "ProfileFragment: onCreateView");
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        initializeViews(view);
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "ProfileFragment: onCreate");
        dbManager = new DatabaseManager(requireContext());
        mAuth = FirebaseAuth.getInstance();
    }
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "ProfileFragment: onResume");
        loadUserData();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "ProfileFragment: onViewCreated");
        setupListeners();
        loadUserData();
    }

    private void initializeViews(View view) {
        profileImage = view.findViewById(R.id.profile_image);
        profileName = view.findViewById(R.id.profile_name);
        profileEmail = view.findViewById(R.id.profile_email);
        profilePhone = view.findViewById(R.id.profile_phone);
        familyName = view.findViewById(R.id.family_name);
        familyCode = view.findViewById(R.id.family_code);
        editProfileButton = view.findViewById(R.id.edit_profile_button);
        familyButton = view.findViewById(R.id.family_button);
        createJoinFamilyButton = view.findViewById(R.id.create_join_family_button);
        logoutButton = view.findViewById(R.id.logout_button);
    }

    private void setupListeners() {
        editProfileButton.setOnClickListener(v -> navigateToEditProfile());
        familyButton.setOnClickListener(v -> navigateToFamilyScreen());
        createJoinFamilyButton.setOnClickListener(v -> showFamilySelectionDialog());
        logoutButton.setOnClickListener(v -> signOut());
    }

    private void loadUserData() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            String userId = firebaseUser.getUid();
            Log.d(TAG, "Loading user data for userId: " + userId);
            dbManager.getUserData(userId, new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        user.setId(userId);
                        currentUser = user;
                        Log.d(TAG, "User data loaded successfully: " + user.toString());
                        updateUIWithUserData(user);
                        if (user.getFamilyId() != null && !user.getFamilyId().isEmpty()) {
                            loadFamilyData(user.getFamilyId());
                        } else {
                            showCreateJoinFamilyButton();
                        }
                    } else {
                        Log.e(TAG, "User data is null");
                        createUserDataIfNotExists(firebaseUser);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to load user data: " + error.getMessage());
                    Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.e(TAG, "Current user is null");
            Toast.makeText(getContext(), "No user logged in", Toast.LENGTH_SHORT).show();
        }
    }

    private void createUserDataIfNotExists(FirebaseUser firebaseUser) {
        User newUser = new User(
                firebaseUser.getUid(),
                firebaseUser.getDisplayName(),
                firebaseUser.getEmail(),
                firebaseUser.getPhoneNumber()
        );
        dbManager.createUser(newUser, task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "User data created successfully");
                currentUser = newUser;
                updateUIWithUserData(newUser);
            } else {
                Log.e(TAG, "Failed to create user data", task.getException());
                Toast.makeText(getContext(), "Failed to create user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFamilyData(String familyId) {
        if (familyId == null || familyId.isEmpty()) {
            Log.e(TAG, "Family ID is null or empty");
            showCreateJoinFamilyButton();
            return;
        }

        dbManager.getFamilyData(familyId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Family family = snapshot.getValue(Family.class);
                if (family != null) {
                    updateUIWithFamilyData(family);
                } else {
                    Log.e(TAG, "Family data is null");
                    showCreateJoinFamilyButton();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load family data: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to load family data", Toast.LENGTH_SHORT).show();
                showCreateJoinFamilyButton();
            }
        });
    }

    private void updateUIWithUserData(User user) {
        if (user == null) {
            Log.e(TAG, "User object is null");
            return;
        }

        Log.d(TAG, "Updating UI with user data: " + user.toString());

        profileName.setText(user.getName() != null ? user.getName() : "N/A");
        profileEmail.setText(user.getEmail() != null ? user.getEmail() : "N/A");
        profilePhone.setText(user.getPhoneNumber() != null ? user.getPhoneNumber() : "N/A");

        String userId = user.getId();
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID is null or empty");
            return;
        }

        if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getProfilePictureUrl())
                    .circleCrop()
                    .into(profileImage);
        } else {
            loadAndDisplayAvatar(userId, user.getName());
        }
        updatePhoneNumberRelatedUI(user.hasPhoneNumber());
    }

    private void loadAndDisplayAvatar(String userId, String name) {
        Log.d(TAG, "Loading avatar for user: " + userId + ", name: " + name);
        AvatarUtils.loadAvatarData(userId, name, (initials, color) -> {
            if (initials != null && color != 0) {
                Log.d(TAG, "Avatar loaded successfully. Initials: " + initials + ", Color: " + color);
                profileImage.setImageBitmap(AvatarUtils.createAvatarBitmap(initials, color, 200));
            } else {
                Log.e(TAG, "Failed to load avatar data");
                profileImage.setImageResource(R.drawable.default_avatar);
            }
        });
    }

    private void updateUIWithFamilyData(Family family) {
        familyName.setText(getString(R.string.family_name, family.getName()));
        familyCode.setText(getString(R.string.family_code, family.getId()));
        familyButton.setVisibility(View.VISIBLE);
        createJoinFamilyButton.setVisibility(View.GONE);
    }

    private void showCreateJoinFamilyButton() {
        familyButton.setVisibility(View.GONE);
        createJoinFamilyButton.setVisibility(View.VISIBLE);
    }

    private void navigateToEditProfile() {
        ((MainActivity) requireActivity()).loadFragment(new EditProfileFragment());
    }

    private void navigateToFamilyScreen() {
        ((MainActivity) requireActivity()).loadFragment(new FamilyFragment());
    }

    private void showFamilySelectionDialog() {
        if (currentUser != null && currentUser.hasPhoneNumber()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Family Selection")
                    .setMessage("Do you want to create a new family or join an existing one?")
                    .setPositiveButton("Create New", (dialog, which) -> createNewFamily())
                    .setNegativeButton("Join Existing", (dialog, which) -> showJoinFamilyDialog())
                    .setCancelable(false)
                    .show();
        } else {
            showPhoneNumberRequiredDialog();
        }
    }

    private void showPhoneNumberRequiredDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Phone Number Required")
                .setMessage("You need to add a phone number to your profile before creating or joining a family.")
                .setPositiveButton("Add Phone Number", (dialog, which) -> navigateToEditProfile())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void createNewFamily() {
        if (currentUser != null && currentUser.hasPhoneNumber()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Create New Family");
            final EditText input = new EditText(getContext());
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setHint("Enter family name");
            builder.setView(input);
            builder.setPositiveButton("Create", (dialog, which) -> {
                String familyName = input.getText().toString().trim();
                if (!familyName.isEmpty()) {
                    FirebaseUser currentFirebaseUser = mAuth.getCurrentUser();
                    if (currentFirebaseUser != null) {
                        dbManager.createNewFamily(familyName, currentFirebaseUser.getUid(), task -> {
                            if (task.isSuccessful()) {
                                String familyId = task.getResult();
                                Toast.makeText(getContext(), "Family created successfully", Toast.LENGTH_SHORT).show();
                                loadUserData();
                            } else {
                                Toast.makeText(getContext(), "Failed to create family: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    Toast.makeText(getContext(), "Family name cannot be empty", Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
        } else {
            showPhoneNumberRequiredDialog();
        }
    }

    private void showJoinFamilyDialog() {
        if (currentUser != null && currentUser.hasPhoneNumber()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Join Family");
            final EditText input = new EditText(getContext());
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setHint("Enter family code");
            builder.setView(input);
            builder.setPositiveButton("Join", (dialog, which) -> {
                String familyId = input.getText().toString().trim();
                if (!familyId.isEmpty()) {
                    joinFamily(familyId);
                } else {
                    Toast.makeText(getContext(), "Family code cannot be empty", Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
        } else {
            showPhoneNumberRequiredDialog();
        }
    }

    private void joinFamily(String familyId) {
        FirebaseUser currentFirebaseUser = mAuth.getCurrentUser();
        if (currentFirebaseUser != null) {
            dbManager.checkFamilyExists(familyId, new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // Family exists, proceed with joining
                        dbManager.addUserToFamily(currentFirebaseUser.getUid(), familyId, task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(getContext(), "Joined family successfully", Toast.LENGTH_SHORT).show();
                                loadUserData();
                            } else {
                                Toast.makeText(getContext(), "Failed to join family", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        // Family does not exist
                        Toast.makeText(getContext(), "Family does not exist", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(getContext(), "Error checking family: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updatePhoneNumberRelatedUI(boolean hasPhoneNumber) {
        if (hasPhoneNumber) {
            createJoinFamilyButton.setEnabled(true);
            // You might want to show a message that the user can now create or join a family
        } else {
            createJoinFamilyButton.setEnabled(false);
            // You might want to show a message that the user needs to add a phone number
        }
    }

    private void signOut() {
        mAuth.signOut();
        ((MainActivity) requireActivity()).navigateToLogin();
    }
}