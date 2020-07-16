package com.foltran.mermaid.repositories;

import com.foltran.mermaid.model.location.Location;

import com.foltran.mermaid.model.profile.TripGridRow;
import com.foltran.mermaid.ui.recommendation_feed.RecommendationFeedViewModel;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class LocationRepository {

    private int NUM_TO_DOWNLOAD_PER_REQUEST = 2;
    private static LocationRepository instance;

    private int curRecommendationDocIndex = 0;

    private RecommendationFeedViewModel model;

    private List<Double> userRatingVector;
    private List<String> locationQueryNames;

    public static LocationRepository getInstance(){
        if (instance == null){
            instance = new LocationRepository();
        }
        return instance;
    }

    private Utils.RepoCallback locationCallback = new Utils.RepoCallback() {

        @Override
        public void getUserInfoCallback(DocumentSnapshot document) {
            List<Double> ratings = new ArrayList<>((List<Double>) document.get("ratings"));

            setUserRatingVector(ratings);   // cache rating vector
            getUserFeed(ratings, curRecommendationDocIndex);
        }

        @Override
        public void getUserRecommendedLocationsCallback(List<Location> result){
            updateFeed(result, locationQueryNames);
        }

        @Override
        public void getLocationCallback(Location location){
            model.addToFeed(location, userRatingVector);
        }

        @Override
        public void getMapLocationCallback(Location location, int positionInMap, int mapPositionInTrip){}

        @Override
        public void getLocationRatingCallback(List<Location> result){
            updateFeed(result, locationQueryNames);
        }
        @Override
        public void getPostObjectCallback(TripGridRow row){}

        @Override
        public void getUserMapPreviewCallback(StorageReference imgRef){}

    };

    private void setUserRatingVector(List<Double> ratingVector){
        this.userRatingVector = new ArrayList<>(ratingVector);
    }


    /**
     * Based on the current user, this method asks for the locations
     * that compose the recommendation feed
     */
    public void requestRecommendationFeedLocations(RecommendationFeedViewModel model, final List<String> locationQueryNames){

        this.model = model;
        this.locationQueryNames = locationQueryNames;

        // use ratingVector that's cached
        if (userRatingVector != null) {

            // if there's a query (i.e. search result) no need to retrieve feed
            if (locationQueryNames != null){
                getQueryResult();
            }
            else {
                getUserFeed(userRatingVector, 0);
            }
            return;
        }

        // retrieve rating vector and proceed to get feed
        Utils.getUserInfo(locationCallback);

    }

    public void getQueryResult(){

        for (final String location : locationQueryNames) {
            Utils.getLocationRating(locationCallback, location, userRatingVector);
        }
    }

    public void getUserFeed(List<Double> userRatingVector, int docIndex){

        // cache user rating vector
        if (userRatingVector != null && this.userRatingVector == null) {
            this.userRatingVector = new ArrayList<>(userRatingVector);
        }

        Utils.getUserRecommendedLocations(locationCallback, docIndex);

    }


    public void updateFeed(final List<Location> queries, List<String> locationQueryNames){

        // no more locations left on buffer
        if (queries == null || queries.size() == 0){
            getUserFeed(null, curRecommendationDocIndex + 1);
            curRecommendationDocIndex ++;
            return;
        }

        // update buffer of locations
        model.updateFeedBuffer(queries, NUM_TO_DOWNLOAD_PER_REQUEST);

        final Object[] queryLocations = queries.subList(0, Math.min(queries.size(), NUM_TO_DOWNLOAD_PER_REQUEST)).toArray();

        for (int i = 0; i < queryLocations.length; i++){

            final Location curLocation = (Location) queryLocations[i];
            Utils.getLocation(locationCallback, curLocation);
        }
    }
}
