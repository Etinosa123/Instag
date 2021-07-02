package com.hfad.instag.adapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.hfad.instag.CommentsActivity;
import com.hfad.instag.R;
import com.hfad.instag.model.Post;
import com.hfad.instag.model.Users;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Post>list;
    private Activity context;
    private FirebaseFirestore store;
    private FirebaseAuth auth;
    private List<Users>usersList;

    public PostAdapter(Activity context, List<Post> list, List<Users> usersList) {
        this.list = list;
        this.context = context;
        this.usersList = usersList;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.each_post,parent, false);
        store = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        return  new PostViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = list.get(position);
        holder.setPostPic(post.getImage());
        holder.setPostCaption(post.getCaption());

        long milliseconds = post.getTime().getTime();
        String date = DateFormat.format("MM/dd/yyyy", new Date(milliseconds)).toString();
        holder.setPostDate(date);

        //String userId = post.getUser();
        //store.collection("Users").document(userId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
         //   @Override
           // public void onComplete(@NonNull Task<DocumentSnapshot> task) {
             //   if (task.isSuccessful()){

        String username = usersList.get(position).getName(); //task.getResult().getString("name");
        String image = usersList.get(position).getImage(); //task.getResult().getString("image");

        holder.setProfilePic(image);
        holder.setPostUsername(username);

          //      }else{
            //        Toast.makeText(context, task.getException().toString(), Toast.LENGTH_SHORT).show();
              //  }
          //  }
        //});
        //likes button
        String postId = post.PostId;
        String currentUserId = auth.getCurrentUser().getUid();
        holder.likePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                store.collection("Posts/" + postId + "/Likes").document(currentUserId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (!task.getResult().exists()){
                    Map<String, Object> likesMap = new HashMap<>();
                    likesMap.put("timestamp", FieldValue.serverTimestamp());
                    //if user clicks on this, it will changed to the liked button and added to the Likes subcollection

                       store.collection("Posts/"+ postId+ "/Likes").document(currentUserId).set(likesMap);

                        }else{
                            //to delete from the likes subcollection
                            store.collection("Posts/"+ postId + "/Likes").document(currentUserId).delete();
                        }
                    }
                });
            }
        });
        //like color change

        store.collection("Posts/"+ postId + "/Likes").document(currentUserId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                //checking first if error is null
                if (error == null){
                    if (value.exists()){
                       //if there is value, when clicked, change to red or
                        holder.likePic.setImageDrawable(context.getDrawable(R.drawable.after_liked));
                    }else {
                        //or to normal color
                        holder.likePic.setImageDrawable(context.getDrawable(R.drawable.before_liked));

                    }
                }
            }
        });

        //likes count
        store.collection("Posts/"+ postId + "/Likes").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error == null){
                    if (!value.isEmpty()){
                        int count = value.size();
                        holder.setPostLikes(count);
                    }else {
                        holder.setPostLikes(0);
                    }
                }
            }
        });

        //comment implementation
        holder.commentsPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent commentIntent = new Intent(context, CommentsActivity.class);
                commentIntent.putExtra("postid",postId);
                context.startActivity(commentIntent);
            }
        });

        //implementing deleting posts
        if (currentUserId.equals(post.getUser())){
            holder.deleteBtn.setVisibility(View.VISIBLE);
            holder.deleteBtn.setClickable(true);
            holder.deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(context);
                    alert.setTitle("Delete?")
                            .setMessage("Are you sure?")
                            .setNegativeButton("No", null)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    store.collection("Posts/"+ postId + "/Comments").get()
                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                    for(QueryDocumentSnapshot snapshot: task.getResult()){
                                                        store.collection("Posts/"+ postId + "/Comments").document(snapshot.getId()).delete();
                                                    }
                                                }
                                            });
                                    store.collection("Posts/"+ postId + "/Likes").get()
                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                    for(QueryDocumentSnapshot snapshot: task.getResult()){
                                                        store.collection("Posts/"+ postId + "/Likes").document(snapshot.getId()).delete();
                                                    }
                                                }
                                            });
                                    store.collection("Posts").document(postId).delete();
                                    list.remove(position);
                                    notifyDataSetChanged();
                                }
                            });
                    alert.show();
                }
            });
        }

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class PostViewHolder extends RecyclerView.ViewHolder{
        ImageView postPic, commentsPic,likePic;
        CircleImageView profilePic;
        TextView postUsername, postDate, postCaption,postLikes;
        ImageButton deleteBtn;
        View view;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            view = itemView;

            commentsPic = view.findViewById(R.id.commentsPost);
            likePic = view.findViewById(R.id.like_btn);
            deleteBtn = view.findViewById(R.id.deleteBtn);
        }

        public void setPostLikes(int count) {
            postLikes = view.findViewById(R.id.likeCountTv);
            postLikes.setText(count + " Likes");
        }

        public void setPostPic(String urlPost){
            postPic = view.findViewById(R.id.userPost);
            Glide.with(context).load(urlPost).into(postPic);
        }

        public void setProfilePic(String urlProfilePic) {
            profilePic = view.findViewById(R.id.profile_pic);
            Glide.with(context).load(urlProfilePic).into(profilePic);
        }
        public void setPostUsername(String username) {
            postUsername = view.findViewById(R.id.username);
            postUsername.setText(username);
        }
        public void setPostDate(String date) {
            postDate = view.findViewById(R.id.date_tv);
            postDate.setText(date);
        }
        public void setPostCaption(String caption) {
            postCaption = view.findViewById(R.id.captionTv);
            postCaption.setText(caption);
        }

    }
}
