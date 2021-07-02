package com.hfad.instag;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;

public class AddPostActivity extends AppCompatActivity {

    private Button addPostBtn;
    private EditText captionText;
    private ImageView postImage;
    private ProgressBar progressBar;
    private Uri postImageUri = null;
    private StorageReference reference;
    private FirebaseFirestore store;
    private FirebaseAuth auth;
    private String currentUserId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        addPostBtn = findViewById(R.id.savePostBtn);
        captionText = findViewById(R.id.captionpost);
        postImage = findViewById(R.id.postImage);
        progressBar = findViewById(R.id.postProgressBar);
        progressBar.setVisibility(View.INVISIBLE);
        Toolbar toolbar = findViewById(R.id.addposttoolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Add Posts");

        reference = FirebaseStorage.getInstance().getReference();
        store = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser().getUid();


        addPostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                String caption  = captionText.getText().toString();
                if (!caption.isEmpty() && postImageUri != null){
                    StorageReference postRef = reference.child("post_images").child(FieldValue.serverTimestamp().toString() + ".jpg");
                    postRef.putFile(postImageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            if (task.isSuccessful()){
                                postRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        //addig the posts to firebase
                                        HashMap<String, Object> map = new HashMap<>();
                                        map.put("image",uri.toString());
                                        map.put("user", currentUserId);
                                        map.put("caption", caption);
                                        map.put("time", FieldValue.serverTimestamp());

                                        store.collection("Posts").add(map).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentReference> task) {
                                                if(task.isSuccessful()){
                                                    progressBar.setVisibility(View.INVISIBLE);
                                                    Toast.makeText(AddPostActivity.this, "Post Added Successfully", Toast.LENGTH_SHORT).show();
                                                    startActivity(new Intent(AddPostActivity.this, MainActivity.class));
                                                    finish();
                                                }else{
                                                    progressBar.setVisibility(View.INVISIBLE);
                                                    Toast.makeText(AddPostActivity.this, task.getException().toString(), Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                    }
                                });
                            }else{
                                progressBar.setVisibility(View.INVISIBLE);
                                Toast.makeText(AddPostActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }else {
                    progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(AddPostActivity.this, "Please Add Image and Caption", Toast.LENGTH_SHORT).show();
                }
            }
        });
        postImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //taking and cropping the pic from users gallery
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setAspectRatio(3,2)
                        .setMinCropResultSize(512,512)
                        .start(AddPostActivity.this);
            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK){

                postImageUri = result.getUri();
                postImage.setImageURI(postImageUri);
            }else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE){
                Toast.makeText(this, result.getError().toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}