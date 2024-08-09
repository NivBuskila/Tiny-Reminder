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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        initializeViews(view);
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbManager = new DatabaseManager();
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            Log.d(TAG, "Loading user data for userId: " + userId);
            dbManager.getUserData(userId, new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        Log.d(TAG, "User data loaded successfully");
                        updateUIWithUserData(user);
                        if (user.getFamilyId() != null && !user.getFamilyId().isEmpty()) {
                            loadFamilyData(user.getFamilyId());
                        } else {
                            showCreateJoinFamilyButton();
                        }
                    } else {
                        Log.e(TAG, "User data is null");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to load user data: " + error.getMessage());
                }
            });
        } else {
            Log.e(TAG, "Current user is null");
        }
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

        Log.d(TAG, "Updating UI with user data: " + user.getId());

        profileName.setText(user.getName());
        profileEmail.setText(user.getEmail());
        profilePhone.setText(user.getPhoneNumber());

        String userId = user.getId();
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID is null or empty");
            return;
        }

        loadAndDisplayAvatar(userId, user.getName());
    }

    private void loadAndDisplayAvatar(String userId, String name) {
        AvatarUtils.loadAvatarData(userId, name, (initials, color) -> {
            if (initials != null && color != 0) {
                profileImage.setImageBitmap(AvatarUtils.createAvatarBitmap(initials, color, 200));
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
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Family Selection")
                .setMessage("Do you want to create a new family or join an existing one?")
                .setPositiveButton("Create New", (dialog, which) -> createNewFamily())
                .setNegativeButton("Join Existing", (dialog, which) -> showJoinFamilyDialog())
                .setCancelable(false)
                .show();
    }

    private void createNewFamily() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Create New Family");
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Enter family name");
        builder.setView(input);
        builder.setPositiveButton("Create", (dialog, which) -> {
            String familyName = input.getText().toString().trim();
            if (!familyName.isEmpty()) {
                String familyId = dbManager.createNewFamily(familyName);
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    dbManager.addUserToFamily(currentUser.getUid(), familyId, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Family created successfully", Toast.LENGTH_SHORT).show();
                            loadUserData();
                        } else {
                            Toast.makeText(getContext(), "Failed to create family", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                Toast.makeText(getContext(), "Family name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showJoinFamilyDialog() {
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
    }

    private void joinFamily(String familyId) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            dbManager.addUserToFamily(currentUser.getUid(), familyId, task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(getContext(), "Joined family successfully", Toast.LENGTH_SHORT).show();
                    loadUserData();
                } else {
                    Toast.makeText(getContext(), "Failed to join family", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void signOut() {
        mAuth.signOut();
        ((MainActivity) requireActivity()).navigateToLogin();
    }
}