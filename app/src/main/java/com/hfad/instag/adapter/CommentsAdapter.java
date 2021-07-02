package com.hfad.instag.adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.hfad.instag.R;
import com.hfad.instag.model.Comments;
import com.hfad.instag.model.Users;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentsViewHolder> {
    private Activity context;
    private List<Comments> list;
    private List<Users>usersList;

    public CommentsAdapter(Activity context, List<Comments> list, List<Users> usersList) {
        this.context = context;
        this.list = list;
        this.usersList = usersList;
    }

    @NonNull
    @Override
    public CommentsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.each_comment,parent, false);
        return new CommentsViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentsViewHolder holder, int position) {
        Comments comments = list.get(position);

        holder.setComment(comments.getComments());

        Users user = usersList.get(position);
        holder.setmUsername(user.getName());
        holder.setCircleImageView(user.getImage());
    }


    @Override
    public int getItemCount() {
        return list.size();
    }

    public  class CommentsViewHolder extends RecyclerView.ViewHolder{
        TextView  comments, mUsername;
        CircleImageView circleImageView;
        View view;
        public CommentsViewHolder(@NonNull View itemView) {
            super(itemView);
            view = itemView;
        }

        public void setComment(String comment) {
            comments = view.findViewById(R.id.comment_tv);
            comments.setText(comment);
        }
        public void setmUsername(String username) {
            mUsername = view.findViewById(R.id.comment_user);
            mUsername.setText(username);
        }
        public void setCircleImageView(String profilePic){
            circleImageView = view.findViewById(R.id.comment_profile_pic);
            Glide.with(context).load(profilePic).into(circleImageView);
        }

    }
}
