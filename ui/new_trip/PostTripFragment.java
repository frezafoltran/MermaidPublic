package com.foltran.mermaid.ui.new_trip;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.foltran.mermaid.MainActivity;
import com.foltran.mermaid.R;
import com.foltran.mermaid.model.trip.FeedCard;
import com.foltran.mermaid.model.trip.TripCardWrapper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class PostTripFragment extends Fragment {

    private ImageView tripMainPic;
    private TextView tripTitle;
    private CardView postTripBtn;
    private ProgressBar postTripLoading;

    private String postFirebaseCollection = "trip_posts";
    String usersCollection = "users";

    private MainActivity activity;
    private Context context;

    TripCardWrapper workingTrip;

    private String postID = UUID.randomUUID().toString();
    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageReference = storage.getReference();

    final FirebaseFirestore db = FirebaseFirestore.getInstance();
    final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    Boolean postedNonImagePost = false;
    Boolean updatedUserPost = false;
    String postedCompletionMsg = "posted";

    private NewTripViewModel model;

    public PostTripFragment(){}


    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        model = ViewModelProviders.of(requireActivity()).get(NewTripViewModel.class);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.new_trip_post, container, false);

        activity = (MainActivity) getActivity();
        context = getContext();
        postTripBtn = root.findViewById(R.id.post_trip_btn);
        tripMainPic = root.findViewById(R.id.trip_main_pic_post);
        tripTitle = root.findViewById(R.id.trip_title);
        postTripLoading = root.findViewById(R.id.post_loading);

        postTripBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postTripBtn.setVisibility(View.GONE);
                postTripLoading.setVisibility(View.VISIBLE);
                postTrip();
            }
        });

        handleTripChanges();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        handleTripChanges();
    }

    private void handleTripChanges(){

        workingTrip = model.getWorkingTrip().getValue();
        FeedCard feedCard = (FeedCard) workingTrip.getCardAt(0);
        if (feedCard.getUris().size() > 0) {
            Glide.with(context).load(feedCard.getUriAt(0)).into(tripMainPic);
        }
        if (workingTrip.getTripTitle() != null && workingTrip.getTripTitle().length() > 0) {
            tripTitle.setText(workingTrip.getTripTitle());
        }

    }

    private void postTrip(){

        if (workingTrip == null){
            Toast.makeText(context, "First add content to your post", Toast.LENGTH_LONG).show();
            return;
        }

        final Map<String, Object> data = new HashMap<>();

        data.put("title", workingTrip.getTripTitle());
        data.put("cards", workingTrip.getSerializableCardsWithoutUris());
        //data.put("trip", workingTrip);
        data.put("author", user.getEmail());
        data.put("timestamp", Calendar.getInstance().getTime().toString());

        Thread postTripThread = new Thread() {
            @Override
            public void run() {
                uploadNonImagePostContent(data);
            }
        };
        postTripThread.start();

        Thread updateUserTripsThread = new Thread() {
            @Override
            public void run() {
                updateUserTripPosts();
            }
        };
        updateUserTripsThread.start();

        // save images from feedcard
        FeedCard feedCard = (FeedCard) workingTrip.getCardAt(0);
        List<Uri> cardUris = feedCard.getUris();
        int numImages = cardUris.size();

        for (int i = 0; i < cardUris.size(); i++){
            uploadImage(cardUris.get(i), "img_" + i, "" + i + " out of " + numImages);
        }

    }

    private void uploadNonImagePostContent(Map<String, Object> data){
        db.collection(postFirebaseCollection)
                .document(postID)
                .set(data)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        if (updatedUserPost) {
                            Toast.makeText(context, postedCompletionMsg, Toast.LENGTH_SHORT).show();
                        }
                        postedNonImagePost = true;
                    }
                });
    }

    private void updateUserTripPosts(){

        CollectionReference collectionReference = db
                .collection(usersCollection);
        DocumentReference docRef = collectionReference.document(user.getEmail());

        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {

                        List<String> userPosts = (List<String>) document.get("posts");

                        if (userPosts == null) userPosts = new ArrayList<>();

                        userPosts.add(postID);
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("posts", userPosts);
                        db.collection(usersCollection).document(user.getEmail())
                                .set(userData, SetOptions.merge());

                        if (postedNonImagePost) {
                            Toast.makeText(context, postedCompletionMsg, Toast.LENGTH_SHORT).show();
                        }
                        updatedUserPost = true;

                    } else {
                        //Log.d(TAG, "No such document");
                    }
                } else {
                    //Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }

    private void uploadImage(Uri filePath, String imageLabel, final String outOfProgress) {

        Bitmap bitmap;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), filePath);
        }
        catch (IOException e){
            Toast.makeText(context, "Failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        if(filePath != null) {
            final ProgressDialog progressDialog = new ProgressDialog(getContext());
            progressDialog.setTitle("Uploading...");
            progressDialog.show();

            postTripLoading.setVisibility(View.GONE);
            postTripBtn.setVisibility(View.VISIBLE);

            StorageReference ref = storageReference.child("images/tripPosts/" + postID + "/" + imageLabel);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream);
            byte[] byteArray = stream.toByteArray();

            ref.putBytes(byteArray)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss();
                            Toast.makeText(context, postedCompletionMsg, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(context, "Failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
                                    .getTotalByteCount());
                            progressDialog.setMessage("Uploaded "+(int)progress+"% (" + outOfProgress + ")");
                        }
                    });

        }
    }
}
