package com.foltran.mermaid.ui.recommendation_feed;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.foltran.mermaid.MainActivity;
import com.foltran.mermaid.model.location.Location;
import com.foltran.mermaid.repositories.LocationRepository;
import com.foltran.mermaid.storage.images.LocalImageUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class RecommendationFeedViewModel extends ViewModel {

    private MutableLiveData<List<Location>> mRecommendationFeedLocations;
    private MutableLiveData<List<Location>> mRecommendationFeedLocationsBuffer;

    // 0: no update pending, 1: update occurring, 2: update finished, needs to update feed
    private MutableLiveData<Integer> recalculatingRecommendations;

    private LocationRepository mLocationRepo;

    /**
     *
     * @return a boolean that's true if feed is cached and false if it's being built
     */
    public boolean init(){

        if (mRecommendationFeedLocations != null) return true;
        mLocationRepo = LocationRepository.getInstance();

        mRecommendationFeedLocations = new MutableLiveData<>();
        mRecommendationFeedLocations.setValue(new ArrayList<Location>());

        mRecommendationFeedLocationsBuffer = new MutableLiveData<>();
        mRecommendationFeedLocationsBuffer.setValue(new ArrayList<Location>());

        mLocationRepo.requestRecommendationFeedLocations(this, null);

        return false;
    }

    /**
     * Retrieves the locations to be displayed in feed again. If optionalLocations is passed as
     * non-null it uses those values instead
     *
     * @param optionalLocations Array of Location objects, usually result of a search
     */
    public void resetRecommendationFeed(List<String> optionalLocations){

        mRecommendationFeedLocationsBuffer.setValue(new ArrayList<Location>());
        mRecommendationFeedLocations.setValue(new ArrayList<Location>());

        mLocationRepo.requestRecommendationFeedLocations(this, optionalLocations);
    }


    public LiveData<List<Location>> getRecommendationFeedLocations(){
        return mRecommendationFeedLocations;
    }

    public void addLocationToFeed(Location location){
        List<Location> cur = mRecommendationFeedLocations.getValue();
        cur.add(location);
        mRecommendationFeedLocations.postValue(cur);
    }

    public void setRecalculatingRecommendations(Integer code){
        if (recalculatingRecommendations == null) {
            recalculatingRecommendations = new MutableLiveData<>();
        }
        recalculatingRecommendations.postValue(code);
    }

    public LiveData<Integer> getRecalculatingRecommendations(){

        if (recalculatingRecommendations == null){
            recalculatingRecommendations = new MutableLiveData<>();
            recalculatingRecommendations.setValue(0);
        }
        return recalculatingRecommendations;
    }


    public void updateFeedBuffer(List<Location> queries, int numToDownloadPerRequest){

        if (queries.size() > numToDownloadPerRequest) {
            mRecommendationFeedLocationsBuffer.postValue(new ArrayList<>(
                    queries.subList(numToDownloadPerRequest, queries.size())
            ));
        }
        else if (mRecommendationFeedLocationsBuffer != null){
            mRecommendationFeedLocationsBuffer.postValue(new ArrayList<Location>());
        }
    }

    /**
     *
     * @param location
     * @param userRatingVector
     */
    public void addToFeed(Location location, List<Double> userRatingVector){

        //calculate distance fom user to location and store in Location object
        android.location.Location userLocation = MainActivity.getUserLocation();
        if (userLocation != null){
            Double dist = LocalImageUtil.getGeoCoordDist(
                    userLocation.getLongitude(), location.getLon(),
                    userLocation.getLatitude(), location.getLat());
            location.setDistToUser(dist.intValue());
        }

        if (mRecommendationFeedLocations.getValue() == null){
            mRecommendationFeedLocations.setValue(new ArrayList<Location>());
        }

        int numLocations = mRecommendationFeedLocations.getValue().size();

        // if last view is null (loading symbol), delete that to make room for valid location view
        if (numLocations >= 1 &&
                mRecommendationFeedLocations.getValue().get(numLocations - 1) == null){

            List<Location> cur = mRecommendationFeedLocations.getValue();
            cur.remove(numLocations - 1);
            mRecommendationFeedLocations.postValue(cur);
        }

        // adds top matches that are displayed directly on feed based on user rating vector
        location.addTopMatchRatingsWithUser(userRatingVector);

        // adds the location view to feed array
        addLocationToFeed(location);
    }

    public void updateFeed(){
        mLocationRepo.updateFeed(mRecommendationFeedLocationsBuffer.getValue(), null);
    }

    public void filterByPopulation(int maxPopulation){

        List<Location> locations = mRecommendationFeedLocations.getValue();
        Iterator<Location> itr = locations.iterator();
        while (itr.hasNext()) {
            Location cur = itr.next();
            if (cur.getPopulation() > maxPopulation) {
                itr.remove();
            }
        }
        mRecommendationFeedLocations.setValue(locations);
    }

    public void sortFeedBySimilarity(){
        Collections.sort(mRecommendationFeedLocations.getValue(), new SortBySimilarity());
    }

    public void sortFeedByDistance(){
        Collections.sort(mRecommendationFeedLocations.getValue(), new SortByDistance());
    }

    static class SortBySimilarity implements Comparator<Location> {

        public int compare(Location a, Location b){

            double diff = a.getSimilarity() - b.getSimilarity();

            if (diff == 0) return 0;
            else if (diff > 0) return -1;
            else return 1;
        }
    }

    static class SortByDistance implements Comparator<Location>{

        public int compare(Location a, Location b){

            int diff = a.getDistToUser() - b.getDistToUser();

            if (diff == 0) return 0;
            else if (diff > 0) return 1;
            else return -1;
        }
    }

}
