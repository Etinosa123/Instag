package com.hfad.instag;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public class SetUpActivity extends AppCompatActivity {

    private CircleImageView circleImageView;
    private FirebaseAuth auth;
    private EditText profileName;
    private Button saveBtn;
    private Uri imageUri;
    private Toolbar toolbar;
    private StorageReference storageReference;
    private FirebaseFirestore store;
    private String Uid;
    private ProgressBar progressBar;
    private boolean isPhotoSelected = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_up);

        //initializing views
        toolbar = findViewById(R.id.set_up_profile_name);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Profile");

        storageReference = FirebaseStorage.getInstance().getReference();
        store = FirebaseFirestore.getInstance();


        auth = FirebaseAuth.getInstance();

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        circleImageView = findViewById(R.id.circleImageView);
        auth = FirebaseAuth.getInstance();
        profileName = findViewById(R.id.profilename);
        saveBtn = findViewById(R.id.save);
        Uid = auth.getCurrentUser().getUid();

        // retrieving profile data
        store.collection("Users").document(Uid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    if(task.getResult().exists()){
                        String name = task.getResult().getString("name");
                        String mImageUri = task.getResult().getString("image");
                        profileName.setText(name);
                        imageUri = Uri.parse(mImageUri);
                        Glide.with(SetUpActivity.this).load(mImageUri).into(circleImageView);
                    }
                }
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                StorageReference imageRef = storageReference.child("Profile_pics").child(Uid + ".jpg");

                String name = profileName.getText().toString();
                if (isPhotoSelected) {
                    if (!name.isEmpty() && imageUri != null) {
                        //creating a child profile pic and the user id with pic
                        imageRef.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                if (task.isSuccessful()) {
                                    imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            saveToFirestore(task, name, uri);
                                        }
                                    });
                                } else {
                                    progressBar.setVisibility(View.INVISIBLE);
                                    Toast.makeText(SetUpActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } else {
                        progressBar.setVisibility(View.INVISIBLE);
                        Toast.makeText(SetUpActivity.this, "Please Select Picture and Enter Name", Toast.LENGTH_SHORT).show();
                    }
                }else {
                    saveToFirestore(null,name,imageUri);
                }
            }
        });


        circleImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

                    //checking if permission is not granted
                    if(ContextCompat.checkSelfPermission(SetUpActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(SetUpActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                    }else{
                        //taking and cropping the pic from users gallery
                        CropImage.activity()
                                .setGuidelines(CropImageView.Guidelines.ON)
                                .setAspectRatio(1,1)
                                .start(SetUpActivity.this);
                    }
                }
            }
        });
    }

    private void saveToFirestore(Task<UploadTask.TaskSnapshot> task, String name, Uri downloadUri) {


        //uploading picture uri and profile name
        HashMap<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("image", downloadUri.toString());

        store.collection("Users").document(Uid).set(map).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()){
                    progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(SetUpActivity.this, "Profile Settings Saved", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SetUpActivity.this, MainActivity.class));
                    finish();
                }else {
                    progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(SetUpActivity.this, task.getException().toString(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            //storing data into the result
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK){
                imageUri = result.getUri();
                circleImageView.setImageURI(imageUri);
                //if user has selected the image
                isPhotoSelected = true;
            }else if(resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE){
                Toast.makeText(this, result.getError().getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}