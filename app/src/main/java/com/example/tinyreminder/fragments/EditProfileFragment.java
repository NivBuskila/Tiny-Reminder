package com.example.tinyreminder.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.tinyreminder.R;
import com.example.tinyreminder.models.FamilyMember;
import com.example.tinyreminder.models.RelationshipToChildren;
import com.example.tinyreminder.utils.AvatarUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileFragment extends Fragment {

    private CircleImageView editProfileImage;
    private MaterialButton changePhotoButton;
    private TextInputEditText editName;
    private TextInputEditText editEmail;
    private TextInputEditText editPhone;
    private Spinner relationshipSpinner;
    private MaterialButton saveProfileButton;
    private DatabaseReference mDatabase;

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
        mDatabase = FirebaseDatabase.getInstance().getReference();
        initViews();
        setupButtons();
    }

    private void findViews(View view) {
        editProfileImage = view.findViewById(R.id.edit_profile_image);
        changePhotoButton = view.findViewById(R.id.change_photo_button);
        editName = view.findViewById(R.id.edit_name);
        editEmail = view.findViewById(R.id.edit_email);
        editPhone = view.findViewById(R.id.edit_phone);
        relationshipSpinner = view.findViewById(R.id.relationship_spinner);
        saveProfileButton = view.findViewById(R.id.save_profile_button);
    }

    private void initViews() {
        setupRelationshipSpinner();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            loadFamilyMemberData(user.getUid());
        }
    }

    private void setupRelationshipSpinner() {
        if (relationshipSpinner != null && getContext() != null) {
            ArrayAdapter<RelationshipToChildren> adapter = new ArrayAdapter<>(
                    getContext(),
                    android.R.layout.simple_spinner_item,
                    RelationshipToChildren.values()
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            relationshipSpinner.setAdapter(adapter);
        }
    }

    private void loadFamilyMemberData(String uid) {
        mDatabase.child("families").child(uid).child("members").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                FamilyMember familyMember = dataSnapshot.getValue(FamilyMember.class);
                if (familyMember != null) {
                    updateUI(familyMember);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(FamilyMember familyMember) {
        loadAndDisplayAvatar(familyMember.getId());
        editName.setText(familyMember.getName());
        setRelationshipSpinnerSelection(familyMember.getRelationshipToUser());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            editEmail.setText(user.getEmail());
            editPhone.setText(user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
        }
    }

    private void setRelationshipSpinnerSelection(String relationshipString) {
        RelationshipToChildren relationship = RelationshipToChildren.fromString(relationshipString);
        int spinnerPosition = ((ArrayAdapter<RelationshipToChildren>) relationshipSpinner.getAdapter())
                .getPosition(relationship);
        relationshipSpinner.setSelection(spinnerPosition);
    }

    private void loadAndDisplayAvatar(String uid) {
        AvatarUtils.loadAvatarData(uid, new AvatarUtils.OnAvatarDataLoadedListener() {
            @Override
            public void onAvatarDataLoaded(String initials, int color) {
                if (initials != null && color != 0) {
                    editProfileImage.setImageBitmap(AvatarUtils.createAvatarBitmap(initials, color, 200));
                } else {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        String newInitials = AvatarUtils.getInitials(user.getDisplayName());
                        int newColor = AvatarUtils.getRandomColor();
                        editProfileImage.setImageBitmap(AvatarUtils.createAvatarBitmap(newInitials, newColor, 200));
                        AvatarUtils.saveAvatarData(uid, newInitials, newColor);
                    }
                }
            }
        });
    }

    private void setupButtons() {
        changePhotoButton.setOnClickListener(v -> changePhoto());
        saveProfileButton.setOnClickListener(v -> saveProfileChanges());
    }

    private void changePhoto() {
        // TODO: Implement photo change functionality
        Toast.makeText(getContext(), "Photo change functionality not implemented yet", Toast.LENGTH_SHORT).show();
    }

    private void saveProfileChanges() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String newName = editName.getText().toString().trim();
            String newEmail = editEmail.getText().toString().trim();
            String newPhone = editPhone.getText().toString().trim();
            RelationshipToChildren newRelationship = (RelationshipToChildren) relationshipSpinner.getSelectedItem();

            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            updateEmailAndPhone(user, newName, newEmail, newPhone, newRelationship);
                        } else {
                            Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void updateEmailAndPhone(FirebaseUser user, String newName, String newEmail, String newPhone, RelationshipToChildren newRelationship) {
        user.updateEmail(newEmail)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveFamilyMemberToDatabase(user.getUid(), newName, newEmail, newPhone, newRelationship);
                    } else {
                        Toast.makeText(getContext(), "Failed to update email", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateEmail(FirebaseUser user, String newName, String newEmail, String newPhone, RelationshipToChildren newRelationship) {
        user.updateEmail(newEmail)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveFamilyMemberToDatabase(user.getUid(), newName, newEmail, newPhone, newRelationship);
                    } else {
                        Toast.makeText(getContext(), "Failed to update email", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveFamilyMemberToDatabase(String uid, String name, String email, String phone, RelationshipToChildren relationship) {
        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("name", name);
        userUpdates.put("email", email);
        userUpdates.put("phoneNumber", phone);
        userUpdates.put("relationshipToChildren", relationship.name());

        mDatabase.child("users").child(uid).updateChildren(userUpdates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to save user data to database", Toast.LENGTH_SHORT).show();
                });
    }
}