package com.hfad.instag.model;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.Exclude;

public class PostId {
    //this field will not be stored in cloud firestore
    @Exclude
    public  String PostId;

    public <T extends PostId> T withId(@NonNull final  String id){
        this.PostId = id;
        //class will hold id and must have one parameter
        return (T)this;
    }

}
