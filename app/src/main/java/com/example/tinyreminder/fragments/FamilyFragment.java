package com.example.tinyreminder.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tinyreminder.R;
import com.example.tinyreminder.adapters.FamilyMemberAdapter;
import com.example.tinyreminder.models.FamilyMember;
import com.example.tinyreminder.models.User;
import com.example.tinyreminder.utils.DatabaseManager;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_family, container, false);
        familyMembersList = view.findViewById(R.id.family_members_list);
        noMembersTextView = view.findViewById(R.id.no_members_text);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbManager = new DatabaseManager();
        setupRecyclerView();
        loadFamilyMembers();
    }

    private void setupRecyclerView() {
        adapter = new FamilyMemberAdapter(new ArrayList<>(), this);
        familyMembersList.setAdapter(adapter);
        familyMembersList.setLayoutManager(new LinearLayoutManager(getContext()));
        familyMembersList.setHasFixedSize(true);
    }

    private void loadFamilyMembers() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        dbManager.getUserData(currentUser.getUid(), new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (user != null && user.getFamilyId() != null) {
                    fetchFamilyMembers(user.getFamilyId());
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
                    FamilyMember member = new FamilyMember(user.getId(), user.getName());
                    members.add(member);
                    updateUI(members);
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
    }

    private void showNoMembersMessage() {
        familyMembersList.setVisibility(View.GONE);
        noMembersTextView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onMemberClick(FamilyMember member) {
        MapFragment mapFragment = MapFragment.newInstance(member.getId());
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, mapFragment)
                .addToBackStack(null)
                .commit();
    }
}