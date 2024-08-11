package com.example.tinyreminder.fragments;

import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tinyreminder.MainActivity;
import com.example.tinyreminder.R;
import com.example.tinyreminder.adapters.FamilyMemberAdapter;
import com.example.tinyreminder.models.FamilyMember;
import com.example.tinyreminder.models.User;
import com.example.tinyreminder.utils.DatabaseManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FamilyFragment extends Fragment implements FamilyMemberAdapter.OnMemberClickListener {
    private static final String TAG = "FamilyFragment";

    private RecyclerView familyMembersList;
    private FamilyMemberAdapter adapter;
    private DatabaseManager dbManager;
    private TextView noMembersTextView;
    private FloatingActionButton addMemberButton;
    private FloatingActionButton removeMemberButton;
    private boolean isCurrentUserAdmin = false;
    private String currentFamilyId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_family, container, false);
        familyMembersList = view.findViewById(R.id.family_members_list);
        noMembersTextView = view.findViewById(R.id.no_members_text);
        addMemberButton = view.findViewById(R.id.add_family_member_button);
        removeMemberButton = view.findViewById(R.id.remove_family_member_button);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbManager = new DatabaseManager();
        setupRecyclerView();
        setupButtons();
        loadFamilyMembers();
    }

    private void setupRecyclerView() {
        adapter = new FamilyMemberAdapter(new ArrayList<>(), this);
        familyMembersList.setAdapter(adapter);
        familyMembersList.setLayoutManager(new LinearLayoutManager(getContext()));
        familyMembersList.setHasFixedSize(true);
    }

    private void setupButtons() {
        addMemberButton.setOnClickListener(v -> showAddMemberDialog());
        removeMemberButton.setOnClickListener(v -> showRemoveMemberDialog());
    }

    private void loadFamilyMembers() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        dbManager.getUserData(currentUser.getUid(), new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (user != null && user.getFamilyId() != null) {
                    currentFamilyId = user.getFamilyId();
                    fetchFamilyMembers(currentFamilyId);
                    checkAdminStatus(currentUser.getUid(), currentFamilyId);
                } else {
                    showNoMembersMessage();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "loadUser:onCancelled", databaseError.toException());
                showNoMembersMessage();
            }
        });
    }

    private void fetchFamilyMembers(String familyId) {
        dbManager.getFamilyMembers(familyId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<FamilyMember> members = new ArrayList<>();
                for (DataSnapshot memberSnapshot : dataSnapshot.getChildren()) {
                    String memberId = memberSnapshot.getKey();
                    if (memberId != null) {
                        fetchMemberDetails(memberId, members);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "loadFamilyMembers:onCancelled", databaseError.toException());
                showNoMembersMessage();
            }
        });
    }

    private void fetchMemberDetails(String memberId, final List<FamilyMember> members) {
        dbManager.getMemberData(memberId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (user != null) {
                    dbManager.checkIfUserIsAdmin(user.getId(), currentFamilyId, new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot adminSnapshot) {
                            boolean isAdmin = adminSnapshot.exists() && adminSnapshot.getValue(Boolean.class);
                            String role = isAdmin ? "Manager" : "Member";
                            FamilyMember member = new FamilyMember(user.getId(), user.getName(), role);
                            member.setProfilePictureUrl(user.getProfilePictureUrl());
                            members.add(member);
                            updateUI(members);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.w(TAG, "checkIfUserIsAdmin:onCancelled", databaseError.toException());
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "loadMemberDetails:onCancelled", databaseError.toException());
            }
        });
    }

    private void updateUI(List<FamilyMember> members) {
        if (members.isEmpty()) {
            showNoMembersMessage();
        } else {
            adapter.updateMembers(members);
            familyMembersList.setVisibility(View.VISIBLE);
            noMembersTextView.setVisibility(View.GONE);
        }
        updateUIForAdminStatus();
    }

    private void showNoMembersMessage() {
        familyMembersList.setVisibility(View.GONE);
        noMembersTextView.setVisibility(View.VISIBLE);
    }

    private void checkAdminStatus(String userId, String familyId) {
        dbManager.checkIfUserIsAdmin(userId, familyId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                isCurrentUserAdmin = dataSnapshot.exists() && dataSnapshot.getValue(Boolean.class);
                updateUIForAdminStatus();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "checkAdminStatus:onCancelled", databaseError.toException());
            }
        });
    }

    private void updateUIForAdminStatus() {
        if (isCurrentUserAdmin) {
            addMemberButton.setVisibility(View.VISIBLE);
            removeMemberButton.setVisibility(View.VISIBLE);
        } else {
            addMemberButton.setVisibility(View.GONE);
            removeMemberButton.setVisibility(View.GONE);
        }
    }

    private void showAddMemberDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add Family Member");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        input.setHint("Enter phone number");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String phoneNumber = input.getText().toString().trim();
            if (!phoneNumber.isEmpty()) {
                addMemberByPhoneNumber(phoneNumber);
            } else {
                Toast.makeText(getContext(), "Phone number cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void addMemberByPhoneNumber(String phoneNumber) {
        dbManager.getUserByPhoneNumber(phoneNumber, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        User user = userSnapshot.getValue(User.class);
                        if (user != null) {
                            addUserToFamily(user);
                            return;
                        }
                    }
                } else {
                    Toast.makeText(getContext(), "User not found with this phone number", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addUserToFamily(User user) {
        if (user.getFamilyId() != null && !user.getFamilyId().isEmpty()) {
            Toast.makeText(getContext(), "User is already in a family", Toast.LENGTH_SHORT).show();
            return;
        }

        dbManager.addUserToFamily(user.getId(), currentFamilyId, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "User added to family successfully", Toast.LENGTH_SHORT).show();
                loadFamilyMembers();
            } else {
                Toast.makeText(getContext(), "Failed to add user to family", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRemoveMemberDialog() {
        List<FamilyMember> members = adapter.getMembers();
        if (members.isEmpty()) {
            Toast.makeText(getContext(), "No members to remove", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] memberNames = members.stream().map(FamilyMember::getName).toArray(String[]::new);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Remove Family Member")
                .setItems(memberNames, (dialog, which) -> {
                    FamilyMember selectedMember = members.get(which);
                    removeMemberFromFamily(selectedMember);
                });

        builder.show();
    }

    private void removeMemberFromFamily(FamilyMember member) {
        dbManager.removeUserFromFamily(member.getId(), currentFamilyId, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), member.getName() + " removed from family", Toast.LENGTH_SHORT).show();
                checkAndDeleteEmptyFamily();
                loadFamilyMembers();
            } else {
                Toast.makeText(getContext(), "Failed to remove " + member.getName() + " from family", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAndDeleteEmptyFamily() {
        dbManager.getFamilyMembers(currentFamilyId, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists() || dataSnapshot.getChildrenCount() == 0) {
                    // Family has no members, delete it
                    dbManager.deleteFamily(currentFamilyId, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Family deleted as it has no members left", Toast.LENGTH_SHORT).show();
                            navigateToProfileScreen();
                        } else {
                            Toast.makeText(getContext(), "Failed to delete empty family", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            private void navigateToProfileScreen() {
                // Navigate to the profile screen
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToProfile();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "checkAndDeleteEmptyFamily:onCancelled", databaseError.toException());
            }
        });
    }

    @Override
    public void onMemberClick(FamilyMember member) {
        showMemberOptionsDialog(member);
    }

    private void showMemberOptionsDialog(FamilyMember member) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(member.getName());

        List<String> options = new ArrayList<>();
        options.add("View Location");

        if (isCurrentUserAdmin) {
            options.add("Make Admin");
            options.add("Remove Admin");
            options.add("Remove from Family");
        }

        String[] optionsArray = options.toArray(new String[0]);

        builder.setItems(optionsArray, (dialog, which) -> {
            switch (optionsArray[which]) {
                case "View Location":
                    navigateToMapFragment(member);
                    break;
                case "Make Admin":
                    makeAdmin(member);
                    break;
                case "Remove Admin":
                    removeAdmin(member);
                    break;
                case "Remove from Family":
                    removeMemberFromFamily(member);
                    break;
            }
        });
        builder.show();
    }

    private void navigateToMapFragment(FamilyMember member) {
        // TODO: Implement navigation to MapFragment
        Toast.makeText(getContext(), "Navigating to map for " + member.getName(), Toast.LENGTH_SHORT).show();
    }

    private void makeAdmin(FamilyMember member) {
        if (currentFamilyId == null) return;
        dbManager.addAdminToFamily(member.getId(), currentFamilyId, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), member.getName() + " is now an admin", Toast.LENGTH_SHORT).show();
                loadFamilyMembers(); // Reload to reflect changes
            } else {
                Toast.makeText(getContext(), "Failed to make " + member.getName() + " an admin", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeAdmin(FamilyMember member) {
        if (currentFamilyId == null) return;
        dbManager.removeAdminFromFamily(member.getId(), currentFamilyId, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), member.getName() + " is no longer an admin", Toast.LENGTH_SHORT).show();
                loadFamilyMembers(); // Reload to reflect changes
            } else {
                Toast.makeText(getContext(), "Failed to remove " + member.getName() + " as an admin", Toast.LENGTH_SHORT).show();
            }
        });
    }
}