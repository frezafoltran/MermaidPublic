package com.foltran.mermaid.database.requests;

import android.os.AsyncTask;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.foltran.mermaid.model.location.Location;
import com.foltran.mermaid.repositories.LocationRepository;
import com.foltran.mermaid.ui.profile.ProfileFragment;
import com.foltran.mermaid.ui.recommendation_feed.RecommendationFeedFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GetUserInfo extends AsyncTask<String, Void, List<Double>> {

    ProfileFragment profileFragment;
    RecommendationFeedFragment recommendationFeedFragment;
    List<Double> ratings;


    public GetUserInfo(ProfileFragment profileFragment, RecommendationFeedFragment recommendationFeedFragment){
        this.profileFragment = profileFragment;
        this.recommendationFeedFragment = recommendationFeedFragment;
    }

    @Override
    public List<Double> doInBackground(String... params){

        String curUser = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference doc = db.collection("users").document(curUser);

        doc.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                if (task.isSuccessful()){
                    DocumentSnapshot result = task.getResult();

                    ratings = new ArrayList<>((List<Double>) result.get("ratings"));


                    if (profileFragment != null) {
                        //profileFragment.userInfoCallback(result);
                    }
                    else if (recommendationFeedFragment != null){

                        //recommendationFeedFragment.getUserFeed((List<Double>) result.get("ratings"), 0);
                    }



                }
                else{
                    //TODO show error message in profile page
                }
            }
        });

        return ratings;
    }
}
