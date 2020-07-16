package com.foltran.mermaid.database.requests;

import android.os.AsyncTask;


import androidx.annotation.NonNull;

import com.foltran.mermaid.model.location.Location;
import com.foltran.mermaid.ui.recommendation_feed.RecommendationFeedFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


import java.util.List;
import java.util.Map;

public class GetLocationFirestoreTask extends AsyncTask<Object, Void, List<String>> {

    public List<String> imagePaths;
    public RecommendationFeedFragment recommendationFeedFragment;
    public String locationCollection = "_USA_location_profiles";

    public GetLocationFirestoreTask(RecommendationFeedFragment recommendationFeedFragment){
        this.recommendationFeedFragment = recommendationFeedFragment;
    }

    @Override
    protected List<String> doInBackground(final Object... queryLocations){

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference collectionReference = db
                .collection(locationCollection);

        for (int i = 0; i < queryLocations.length; i++){

            final Location curLocation = (Location) queryLocations[i];
            DocumentReference docRef = collectionReference.document(curLocation.getQueryName());

            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {

                            curLocation.addFeedAttrs(document);
                            if (recommendationFeedFragment != null) {
                                //recommendationFeedFragment.addToFeed(curLocation);
                            }
                            curLocation.addFirebaseAttrs(document);
                            //Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        } else {
                            //Log.d(TAG, "No such document");
                        }
                    } else {
                        //Log.d(TAG, "get failed with ", task.getException());
                    }
                }
            });
        }

        return imagePaths;
    }

    @Override
    protected void onPostExecute(List<String> result){
        super.onPostExecute(result);
    }

}