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

public class GetUserRecommendations extends AsyncTask<String, Void, String> {

    RecommendationFeedFragment recommendationFeedFragment;
    int docIndex;

    public GetUserRecommendations(RecommendationFeedFragment recommendationFeedFragment,
                                  int docIndex){

        this.recommendationFeedFragment = recommendationFeedFragment;
        this.docIndex = docIndex;
    }

    @Override
    public String doInBackground(String... params){

        String curUser = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference doc = db.collection("location_recommendations_by_user").document(curUser + "__" + docIndex);

        doc.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                if (task.isSuccessful()){
                    DocumentSnapshot result = task.getResult();


                    List<HashMap<String, Double>> recommendationss = (List<HashMap<String, Double>>) result.get("recommendations");

                    List<Location> queriess = new ArrayList<>();
                    for (HashMap<String, Double> cur : recommendationss){

                        for (String location : cur.keySet()){
                            String queryName = location + ", usa";
                            queriess.add(new Location(queryName.toLowerCase(), cur.get(location)));
                        }
                    }


                    if (recommendationFeedFragment != null){

                        List<HashMap<String, Double>> recommendations = (List<HashMap<String, Double>>) result.get("recommendations");

                        List<Location> queries = new ArrayList<>();
                        for (HashMap<String, Double> cur : recommendations){

                            for (String location : cur.keySet()){
                                String queryName = location + ", usa";
                                queries.add(new Location(queryName.toLowerCase(), cur.get(location)));
                            }
                        }

                        //recommendationFeedFragment.updateFeed(queries);
                    }


                }
                else{
                    //TODO show error message in profile page
                }
            }
        });

        return null;
    }
}
