package com.hfad.instag;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.hfad.instag.adapter.CommentsAdapter;
import com.hfad.instag.model.Comments;
import com.hfad.instag.model.Users;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentsActivity extends AppCompatActivity {
    private EditText commentEdit;
    private Button addCommentBtn;
    private RecyclerView commentRecyclerView;
    private FirebaseFirestore store;
    private String post_id;
    private String currentUserId;
    private FirebaseAuth auth;
    private CommentsAdapter adapter;
    private List<Comments> list;
    private List<Users> usersList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        store = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser().getUid();
        post_id = getIntent().getStringExtra("postid");

        list = new ArrayList<>();
        usersList = new ArrayList<>();
        adapter = new CommentsAdapter(CommentsActivity.this, list, usersList);

        commentEdit = findViewById(R.id.commentEdittext);
        addCommentBtn = findViewById(R.id.addComment);
        commentRecyclerView = findViewById(R.id.rec_comment);

        commentRecyclerView.setHasFixedSize(true);
        commentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentRecyclerView.setAdapter(adapter);

        //retrieving comments from firestore
        store.collection("Posts/" + post_id+ "/Comments").addSnapshotListener(CommentsActivity.this,new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                for (DocumentChange dc: value.getDocumentChanges()){
                    if (dc.getType() == DocumentChange.Type.ADDED){
                        Comments comments = dc.getDocument().toObject(Comments.class);
                        String userId = dc.getDocument().getString("user");
                        store.collection("Users").document(userId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()){
                                    Users users = task.getResult().toObject(Users.class);
                                    usersList.add(users);
                                    list.add(comments);
                                    adapter.notifyDataSetChanged();
                                }else {
                                    Toast.makeText(CommentsActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                    }else {
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        });

        //adding comments
        addCommentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String comment = commentEdit.getText().toString();
                if (!comment.isEmpty()){
                    Map<String, Object>commentsMap = new HashMap<>();
                    commentsMap.put("comments", comment);
                    commentsMap.put("timestamp", FieldValue.serverTimestamp());
                    commentsMap.put("user", currentUserId);
                    store.collection("Posts/" + post_id+ "/Comments").add(commentsMap).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentReference> task) {
                            if (task.isSuccessful()){
                                Toast.makeText(CommentsActivity.this, "Comment Added !!!", Toast.LENGTH_SHORT).show();
                            }else{
                                Toast.makeText(CommentsActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();

                            }
                        }
                    });
                }else {
                    Toast.makeText(CommentsActivity.this, "Please Write Comment", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}