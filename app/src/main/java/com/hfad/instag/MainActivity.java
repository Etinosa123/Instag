package com.hfad.instag;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.hfad.instag.adapter.PostAdapter;
import com.hfad.instag.model.Post;
import com.hfad.instag.model.Users;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth auth;
    private Toolbar toolbar;
    private FirebaseFirestore store;
    private RecyclerView recyclerView;
    private FloatingActionButton fab;
    private List<Post> list;
    private PostAdapter adapter;
    private Query query;
    private ListenerRegistration listenerRegistration;
    private  List<Users> usersList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        recyclerView = findViewById(R.id.rec);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        list = new ArrayList<>();
        usersList = new ArrayList<>();
        adapter = new PostAdapter(MainActivity.this,list, usersList);
        recyclerView.setAdapter(adapter);

        fab = findViewById(R.id.floatingActionButton2);
        auth = FirebaseAuth.getInstance();
        store = FirebaseFirestore.getInstance();
        getSupportActionBar().setTitle("Instag");

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddPostActivity.class));
            }
        });

        if (auth.getCurrentUser() != null){
            //if user has seen all the posts
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    boolean isBottom = !recyclerView.canScrollVertically(1);
                    if (isBottom){
                        Toast.makeText(MainActivity.this, "Reached Bottom", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            query = store.collection("Posts").orderBy("time", Query.Direction.DESCENDING);
            //listener registration is an interface and it will remove the listener when we get all the data
            listenerRegistration = query.addSnapshotListener(MainActivity.this, new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                    //this snapshot listener will give us all the data inside that post
                    for(DocumentChange doc: value.getDocumentChanges()){
                        if (doc.getType() == DocumentChange.Type.ADDED){
                            String postId = doc.getDocument().getId();

                            //getting data and adding it to our post.class
                            Post post = doc.getDocument().toObject(Post.class).withId(postId);

                            String postUserId = doc.getDocument().getString("user");
                            store.collection("Users").document(postUserId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()){
                                     Users users = task.getResult().toObject(Users.class);
                                     usersList.add(users);
                                     list.add(post);
                                     adapter.notifyDataSetChanged();
                                    }else{
                                        Toast.makeText(MainActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });

                        }else {
                            adapter.notifyDataSetChanged();
                        }
                    }
                    //preventing the query from running continusly
                    listenerRegistration.remove();

                }
            });
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.profile_menu){
            startActivity(new Intent(MainActivity.this, SetUpActivity.class));
        }else if (item.getItemId() ==R.id.sign_out_menu){
            auth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }
        return true;
    }
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null){
            startActivity(new Intent(MainActivity.this, RegistrationActivity.class));
            finish();
        }else{
            String currentUserId = auth.getCurrentUser().getUid();
            store.collection("Users").document(currentUserId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()){
                        if (!task.getResult().exists()){
                            startActivity(new Intent(MainActivity.this, SetUpActivity.class));
                            finish();
                        }
                    }
                }
            });
        }
    }
}