package com.foltran.mermaid.repositories;

import androidx.annotation.NonNull;

import com.foltran.mermaid.model.location.Location;
import com.foltran.mermaid.model.profile.TripGridRow;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {

    static FirebaseFirestore db = FirebaseFirestore.getInstance();
    static FirebaseStorage storage = FirebaseStorage.getInstance();
    static StorageReference storageReference = storage.getReference();

    static private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    static public String locationCollection = "_USA_location_profiles";
    static public String postCollection = "trip_posts";

    static CollectionReference collectionReference = db.collection(locationCollection);
    static CollectionReference postCollectionReference = db.collection(postCollection);

    static TripGridRow curTripGridRow;
    static int gridTripColCount = 0;

    public interface RepoCallback {

        void getUserInfoCallback(DocumentSnapshot document);

        void getUserRecommendedLocationsCallback(List<Location> result);

        void getLocationCallback(Location location);

        void getMapLocationCallback(Location location, int positionInMap, int mapPositionInTrip);

        void getLocationRatingCallback(List<Location> result);

        void getPostObjectCallback(TripGridRow row);

        void getUserMapPreviewCallback(StorageReference imgRef);
    }

    public static void getUserInfo(final RepoCallback repoCallback) {

        String curUser = user.getEmail();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference doc = db.collection("users").document(curUser);

        doc.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                if (task.isSuccessful()){
                    DocumentSnapshot result = task.getResult();

                    repoCallback.getUserInfoCallback(result);
                }
                else{
                    //TODO show error message in profile page
                }
            }
        });
    }


    public static void getUserRecommendedLocations(final RepoCallback repoCallback, int docIndex){

        String curUser = user.getEmail();
        DocumentReference doc = db.collection("location_recommendations_by_user").document(curUser + "__" + docIndex);

        doc.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                if (task.isSuccessful()){
                    DocumentSnapshot result = task.getResult();


                    List<HashMap<String, Double>> recommendationss = (List<HashMap<String, Double>>) result.get("recommendations");

                    List<Location> queries = new ArrayList<>();
                    for (HashMap<String, Double> cur : recommendationss){

                        for (String location : cur.keySet()){
                            String queryName = location + ", usa";
                            queries.add(new Location(queryName.toLowerCase(), cur.get(location)));
                        }
                    }

                    repoCallback.getUserRecommendedLocationsCallback(queries);

                }
                else{
                    //TODO show error message in profile page
                }
            }
        });
    }

    public static void getLocation(final RepoCallback repoCallback, final Location location){

        DocumentReference docRef = collectionReference.document(location.getQueryName());

        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {

                        location.addFeedAttrs(document);
                        repoCallback.getLocationCallback(location);
                        location.addFirebaseAttrs(document);
                    }
                }
            }
        });
    }

    public static void getMapLocation(final RepoCallback repoCallback,
                                      final Location location, final int positionInMap, final int mapPositionInTrip){

        DocumentReference docRef = collectionReference.document(location.getQueryName());

        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {

                        location.addFeedAttrs(document);
                        repoCallback.getMapLocationCallback(location, positionInMap, mapPositionInTrip);
                        location.addFirebaseAttrs(document);
                    }
                }
            }
        });
    }

    public static void getLocationRating(final RepoCallback repoCallback, final String queryName, final List<Double> userRatingVector){

        DocumentReference doc = db.collection("_USA_location_ratings").document(queryName);
        doc.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                if (task.isSuccessful()) {
                    DocumentSnapshot result = task.getResult();


                    List<Double> locationRating = (List<Double>) result.get("ratings");

                    List<Location> queries = new ArrayList<>();

                    String completeQueryName = queryName + ", usa";
                    queries.add(new Location(completeQueryName.toLowerCase(), getSimilarity(locationRating, userRatingVector)));

                    repoCallback.getLocationRatingCallback(queries);
                    //updateFeed(queries, locationQueryNames);

                } else {
                    //TODO show error message in profile page
                }
            }
        });
    }

    public static void getPostObject(final RepoCallback repoCallback, final List<String> userPosts, final int curUserPostIndex){

        if (curUserPostIndex >= userPosts.size()) return;

        if (gridTripColCount == 0) {
            curTripGridRow = new TripGridRow();
        }

        final Boolean isLastPost = curUserPostIndex == userPosts.size() - 1 ? true : false;

        final String postId = userPosts.get(curUserPostIndex);
        DocumentReference docRef = postCollectionReference.document(postId);

        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {

                        String curTitle = (String) document.get("title");

                        Map<String, Object> feedCard = ((List<Map<String, Object>>)document.get("cards")).get(0);

                        Long numImages = (Long) feedCard.get("numCards");
                        List<String> curImages = new ArrayList<>();
                        for (int k = 0; k < numImages.intValue(); k++){
                            curImages.add(postId + "/img_" + k);
                        }

                        curTripGridRow.addEntry(curImages, curTitle, document);


                        gridTripColCount ++;
                        if (gridTripColCount == 2 || isLastPost) {
                            //profileFeed.add(curTripGridRow);
                            repoCallback.getPostObjectCallback(curTripGridRow);
                            gridTripColCount = 0;
                        }

                        getPostObject(repoCallback, userPosts, curUserPostIndex + 1);

                    }
                }
            }
        });
    }

    static private Double dotProduct(List v1, List v2){

        if (v1.size() != v2.size()) return Double.valueOf(-1);

        Double out = Double.valueOf(0);
        for (int i = 0; i < v1.size(); i++){

            if (v1.get(i) instanceof Long && v2.get(i) instanceof  Long){
                out += ((Long) v1.get(i)).doubleValue() * ((Long) v2.get(i)).doubleValue();
            }
            else if (v1.get(i) instanceof Long){
                out += ((Long) v1.get(i)).doubleValue() * (Double) v2.get(i);
            }
            else if (v2.get(i) instanceof Long){
                out += ((Long) v2.get(i)).doubleValue() * (Double) v1.get(i);
            }
            else{
                out += ((Double) v1.get(i)) * ((Double) v2.get(i));
            }
        }
        return out;
    }

    static private double getSimilarity(List<Double> locationRating, List<Double> userRatingVector){

        if (userRatingVector == null || locationRating == null
                || userRatingVector.size() != locationRating.size()) return -1;

        double dotProductVal = dotProduct(locationRating, userRatingVector);

        double p1 = dotProduct(locationRating, locationRating);
        double p2 = dotProduct(userRatingVector, userRatingVector);

        return dotProductVal / (Math.sqrt(p1) * Math.sqrt(p2));

    }

    public static void getUserMapPreview(final RepoCallback repoCallback) {

        final StorageReference imgRef = storageReference.child("images/map_screenshots/"
                + user.getEmail() + "/map_preview");

        final long ONE_MEGABYTE = 1024 * 1024;

        imgRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                repoCallback.getUserMapPreviewCallback(imgRef);
            }
        });
    }

}
