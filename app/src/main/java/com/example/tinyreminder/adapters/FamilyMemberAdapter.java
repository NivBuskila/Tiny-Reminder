package com.example.tinyreminder.adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tinyreminder.R;
import com.example.tinyreminder.models.FamilyMember;
import com.example.tinyreminder.utils.AvatarUtils;

import java.util.List;

public class FamilyMemberAdapter extends RecyclerView.Adapter<FamilyMemberAdapter.ViewHolder> {
    private List<FamilyMember> members;
    private OnMemberClickListener listener;

    public FamilyMemberAdapter(List<FamilyMember> members, OnMemberClickListener listener) {
        this.members = members;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_family_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FamilyMember member = members.get(position);
        holder.bind(member, listener);
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    public void updateMembers(List<FamilyMember> newMembers) {
        members.clear();
        members.addAll(newMembers);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView roleTextView;
        ImageView avatarImageView;
        ImageView statusIndicator;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.member_name);
            roleTextView = itemView.findViewById(R.id.member_role);
            avatarImageView = itemView.findViewById(R.id.member_avatar);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
        }

        void bind(FamilyMember member, OnMemberClickListener listener) {
            nameTextView.setText(member.getName());
            roleTextView.setText(member.getRole());

            // Load profile picture or avatar
            if (member.getProfilePictureUrl() != null && !member.getProfilePictureUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(member.getProfilePictureUrl())
                        .circleCrop()
                        .into(avatarImageView);
            } else {
                AvatarUtils.loadAvatarData(member.getId(), member.getName(), (initials, color) -> {
                    if (initials != null && color != 0) {
                        Bitmap avatarBitmap = AvatarUtils.createAvatarBitmap(initials, color, 200);
                        avatarImageView.setImageBitmap(avatarBitmap);
                    } else {
                        avatarImageView.setImageResource(R.drawable.default_avatar);
                    }
                });
            }

            switch (member.getResponseStatus()) {
                case OK:
                    statusIndicator.setImageResource(R.drawable.ic_status_green);
                    break;
                case PENDING:
                    statusIndicator.setImageResource(R.drawable.ic_status_orange);
                    break;
                case ALERT:
                    statusIndicator.setImageResource(R.drawable.ic_status_red);
                    break;
            }

            itemView.setOnClickListener(v -> listener.onMemberClick(member));
        }
    }

    public interface OnMemberClickListener {
        void onMemberClick(FamilyMember member);
    }
}