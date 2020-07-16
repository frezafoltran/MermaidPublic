package com.foltran.mermaid.storage.images;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.UUID;

public class FirebaseUploadImage extends Worker {

    private String TAG = "------ FirebaseUploadImage: ";
    private Context mContext;

    public FirebaseUploadImage(
            @NonNull Context context,
            @NonNull WorkerParameters params) {

        super(context, params);
        mContext = context;
    }

    @Override
    public Result doWork() {

        String filePath =
                getInputData().getString("IMAGE_URI");

        if (filePath != null) {

            try {

                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageReference = storage.getReference();

                // Defining the child of storageReference
                StorageReference ref = storageReference.child(
                        "images/" + UUID.randomUUID().toString());


                ref.putFile(Uri.parse(filePath)).addOnSuccessListener(
                    new OnSuccessListener<UploadTask.TaskSnapshot>() {

                        @Override
                        public void onSuccess(
                                UploadTask.TaskSnapshot taskSnapshot) {

                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    // Run your task here
                                    Toast.makeText(mContext, "Image uploaded", Toast.LENGTH_SHORT).show();
                                }
                            });
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                // Run your task here
                                Toast.makeText(mContext, "Upload Failed", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
            catch (Throwable e){
                Log.e(TAG, e.toString());
                return Result.failure();
            }

        }

        return Result.success();
    }
}
