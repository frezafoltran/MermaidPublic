package com.foltran.mermaid.database.requests;

import android.os.AsyncTask;

import com.foltran.mermaid.MainActivity;
import com.foltran.mermaid.R;
import com.foltran.mermaid.ui.profile.ProfileFragment;
import com.foltran.mermaid.ui.profile.TripStylePreferencesFragment;
import com.foltran.mermaid.ui.recommendation_feed.RecommendationFeedFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class UpdateUserInfo extends AsyncTask<Object, Void, String> {

    String usersCollection = "users";

    private MainActivity activity;
    private ProfileFragment profileFragment;
    private TripStylePreferencesFragment tripStylePreferencesFragment;


    public UpdateUserInfo(MainActivity activity,
                          ProfileFragment profileFragment, TripStylePreferencesFragment tripStylePreferencesFragment){
        this.activity = activity;
        this.profileFragment = profileFragment;
        this.tripStylePreferencesFragment = tripStylePreferencesFragment;
    }

    @Override
    public String doInBackground(Object... params){

        String username = (String) params[0];
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        Map<String, Object> data = new HashMap<>();
        List<Double> ratingVector = new ArrayList<>();

        if (username.length() > 0) {

            data.put("username", username);
            db.collection(usersCollection).document(user.getEmail())
                    .set(data, SetOptions.merge());
        }

        if (params.length > 1){

            //((MainActivity) activity).setIsRecommendationBeingUpdated(true);
            ratingVector = (List<Double>) params[1];
            data.put("ratings", ratingVector);
            db.collection(usersCollection).document(user.getEmail())
                    .set(data, SetOptions.merge());

        }

        StringBuilder ratingStr = new StringBuilder();
        for (int i = 0; i < ratingVector.size(); i++){

            ratingStr.append(ratingVector.get(i));
            if (i < ratingVector.size() - 1) {
                ratingStr.append(",");
            }
        }

        if (profileFragment != null){
            profileFragment.callBackRecalculateRecommendationFeed(ratingVector);
        }
        else if (tripStylePreferencesFragment != null){
            tripStylePreferencesFragment.callBackRecalculateRecommendationFeed(ratingVector);
        }


        return ratingStr.toString();
    }

    @Override
    public void onPostExecute(String result){
        super.onPostExecute(result);

    }
}
